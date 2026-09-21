package org.odk.collect.openrosa.http.okhttp;

import androidx.annotation.NonNull;

import com.burgstaller.okhttp.AuthenticationCacheInterceptor;
import com.burgstaller.okhttp.CachingAuthenticatorDecorator;
import com.burgstaller.okhttp.DispatchingAuthenticator;
import com.burgstaller.okhttp.basic.BasicAuthenticator;
import com.burgstaller.okhttp.digest.CachingAuthenticator;
import com.burgstaller.okhttp.digest.Credentials;
import com.burgstaller.okhttp.digest.DigestAuthenticator;

import org.odk.collect.openrosa.http.HttpCredentialsInterface;
import org.odk.collect.openrosa.http.OpenRosaConstants;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import kotlin.Pair;
import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

public class OkHttpOpenRosaServerClientProvider implements OpenRosaServerClientProvider {

    private static final int CONNECTION_TIMEOUT = 30000;
    private static final int WRITE_CONNECTION_TIMEOUT = 180000; // smap: increased to 3 minutes for large media uploads on slow connections
    private static final int READ_CONNECTION_TIMEOUT = 60000; // it can take up to 27 seconds to spin up an Aggregate
    private static final String USER_AGENT_HEADER = "User-Agent";
    private static final String OPEN_ROSA_VERSION_HEADER = OpenRosaConstants.VERSION_HEADER;
    private static final String OPEN_ROSA_VERSION = "1.0";
    private static final String DATE_HEADER = "Date";
    private static final String TOKEN_HEADER = "x-api-key";  // smap - token authentication header

    private final OkHttpClient baseClient;
    private final String cacheDir;

    private final Map<Pair<String, HttpCredentialsInterface>, OkHttpOpenRosaServerClient> clients = new HashMap<>();

    public OkHttpOpenRosaServerClientProvider(@NonNull OkHttpClient baseClient, String cacheDir) {
        this.baseClient = baseClient;
        this.cacheDir = cacheDir;
    }

    public OkHttpOpenRosaServerClientProvider(String cacheDir) {
        this(new OkHttpClient(), cacheDir);
    }

    @Override
    public synchronized OpenRosaServerClient get(String scheme, String userAgent, @NonNull HttpCredentialsInterface credentials) {
        OkHttpOpenRosaServerClient existingClient = clients.get(new Pair<>(scheme, credentials));

        if (existingClient == null) {
            OkHttpOpenRosaServerClient newClient = createNewClient(scheme, userAgent, credentials);
            clients.put(new Pair<>(scheme, credentials), newClient);
            return newClient;
        } else {
            return existingClient;
        }
    }

    @NonNull
    private OkHttpOpenRosaServerClient createNewClient(String scheme, String userAgent, @NonNull HttpCredentialsInterface credentials) {
        OkHttpClient.Builder builder = baseClient.newBuilder()
                .connectTimeout(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)
                .writeTimeout(WRITE_CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)
                .readTimeout(READ_CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)
                .followRedirects(true);

        if (cacheDir != null && new File(cacheDir).exists()) {
            builder.cache(new Cache(
                    new File(cacheDir, "http_" + credentials.hashCode()),
                    50L * 1024L * 1024L // 50 MiB
            ));
        }

        if (credentials != null) {
            Credentials cred = new Credentials(credentials.getUsername(), credentials.getPassword());

            DispatchingAuthenticator.Builder daBuilder = new DispatchingAuthenticator.Builder();
            daBuilder.with("digest", new DigestAuthenticator(cred));
            if (scheme.equalsIgnoreCase("https")) {
                daBuilder.with("basic", new BasicAuthenticator(cred));
            } else {
                // smap - allow basic auth over plain http, but only to a server on the local network
                daBuilder.with("basic", new PrivateNetworkBasicAuthenticator(cred));
            }

            DispatchingAuthenticator authenticator = daBuilder.build();
            ConcurrentHashMap<String, CachingAuthenticator> authCache = new ConcurrentHashMap<>();
            builder.authenticator(new CachingAuthenticatorDecorator(authenticator, authCache))
                    .addInterceptor(new AuthenticationCacheInterceptor(authCache)).build();
        }

        // smap - pass credentials for token authentication
        return new OkHttpOpenRosaServerClient(builder.build(), userAgent, credentials);
    }

    private static class OkHttpOpenRosaServerClient implements OpenRosaServerClient {

        private final OkHttpClient client;
        private final String userAgent;
        private final HttpCredentialsInterface credentials;  // smap - for token auth

        OkHttpOpenRosaServerClient(OkHttpClient client, String userAgent, HttpCredentialsInterface credentials) {
            this.client = client;
            this.userAgent = userAgent;
            this.credentials = credentials;  // smap
        }

        @Override
        public Response makeRequest(Request request, Date currentTime) throws IOException {
            Request.Builder requestBuilder = request.newBuilder()
                    .addHeader(USER_AGENT_HEADER, userAgent)
                    .addHeader(OPEN_ROSA_VERSION_HEADER, OPEN_ROSA_VERSION)
                    .addHeader(DATE_HEADER, getHeaderDate(currentTime));

            // smap - add token header for token authentication
            if (credentials != null && credentials.getUseToken() && credentials.getAuthToken() != null) {
                requestBuilder.addHeader(TOKEN_HEADER, credentials.getAuthToken());
            }

            return client.newCall(requestBuilder.build()).execute();
        }

        private static String getHeaderDate(Date currentTime) {
            SimpleDateFormat dateFormatGmt = new SimpleDateFormat("E, dd MMM yyyy hh:mm:ss zz", Locale.US);
            dateFormatGmt.setTimeZone(TimeZone.getTimeZone("GMT"));
            return dateFormatGmt.format(currentTime);
        }
    }

    /**
     * smap - true if the host is a loopback, RFC1918 or link-local address, ie a server that can
     * only be reached from the local network. Hosts that would need DNS resolution are treated as
     * public, so a development server addressed by name (other than localhost or an mDNS .local
     * name) still needs https.
     */
    @SuppressWarnings("PMD.AvoidUsingHardCodedIP") // smap - the literals are what this check is for
    private static boolean isPrivateHost(String host) {
        if (host == null) {
            return false;
        }

        String h = host.toLowerCase(Locale.US);
        if (h.startsWith("[") && h.endsWith("]")) {
            h = h.substring(1, h.length() - 1);     // IPv6 literal
        }

        if (h.equals("localhost") || h.endsWith(".localhost") || h.endsWith(".local") || h.equals("::1")) {
            return true;
        }

        String[] parts = h.split("\\.");
        if (parts.length != 4) {
            return false;
        }

        int[] octets = new int[4];
        for (int i = 0; i < 4; i++) {
            try {
                octets[i] = Integer.parseInt(parts[i]);
            } catch (NumberFormatException e) {
                return false;
            }
            if (octets[i] < 0 || octets[i] > 255) {
                return false;
            }
        }

        return octets[0] == 127                                             // loopback
                || octets[0] == 10                                          // RFC1918
                || (octets[0] == 172 && octets[1] >= 16 && octets[1] <= 31) // RFC1918
                || (octets[0] == 192 && octets[1] == 168)                   // RFC1918
                || (octets[0] == 169 && octets[1] == 254);                  // link-local
    }

    /**
     * smap - basic auth over plain http, restricted to servers on the local network.
     *
     * Upstream ODK registers {@link BasicAuthenticator} only for https so that credentials are
     * never sent in the clear over the internet. That also blocks logging in to a development
     * server, which is typically plain http behind Apache. This keeps the upstream guarantee for
     * public hosts and answers the challenge only when the host is private, checked per request
     * because the provider is given the scheme but not the host.
     */
    private static class PrivateNetworkBasicAuthenticator implements CachingAuthenticator {

        private final BasicAuthenticator delegate;

        PrivateNetworkBasicAuthenticator(Credentials credentials) {
            this.delegate = new BasicAuthenticator(credentials);
        }

        @Override
        public Request authenticate(Route route, Response response) throws IOException {
            if (!isPrivateHost(response.request().url().host())) {
                return null;
            }
            return delegate.authenticate(route, response);
        }

        @Override
        public Request authenticateWithState(Route route, Request request) throws IOException {
            if (!isPrivateHost(request.url().host())) {
                return null;
            }
            return delegate.authenticateWithState(route, request);
        }
    }
}
