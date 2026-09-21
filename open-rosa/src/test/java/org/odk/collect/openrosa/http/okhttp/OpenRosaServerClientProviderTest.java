package org.odk.collect.openrosa.http.okhttp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.odk.collect.openrosa.support.MockWebServerHelper.buildRequest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.odk.collect.openrosa.http.HttpCredentials;
import org.odk.collect.openrosa.http.OpenRosaConstants;
import org.odk.collect.openrosa.support.MockWebServerRule;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import okhttp3.Dns;
import okhttp3.Request;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public abstract class OpenRosaServerClientProviderTest {

    protected abstract OpenRosaServerClientProvider buildSubject();

    // smap - a subject whose client resolves every hostname to the local MockWebServer, so that a
    // request can carry a public hostname without a real public server
    protected abstract OpenRosaServerClientProvider buildSubject(Dns dns);

    private OpenRosaServerClientProvider subject;

    @Rule
    public MockWebServerRule mockWebServerRule = new MockWebServerRule();

    @Before
    public void setup() {
        subject = buildSubject();
    }

    @Test
    public void sendsOpenRosaHeaders() throws Exception {
        MockWebServer mockWebServer = mockWebServerRule.start();
        enqueueSuccess(mockWebServer);

        OpenRosaServerClient client = subject.get("http", "Android", null);
        client.makeRequest(buildRequest(mockWebServer, ""), new Date());

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getHeader(OpenRosaConstants.VERSION_HEADER), equalTo("1.0"));
    }

    @Test
    public void sendsDateHeader() throws Exception {
        MockWebServer mockWebServer = mockWebServerRule.start();
        enqueueSuccess(mockWebServer);

        Date currentTime = new Date();

        OpenRosaServerClient client = subject.get("http", "Android", null);
        client.makeRequest(buildRequest(mockWebServer, ""), currentTime);

        RecordedRequest request = mockWebServer.takeRequest();

        SimpleDateFormat dateFormatGmt = new SimpleDateFormat("E, dd MMM yyyy hh:mm:ss zz", Locale.US);
        dateFormatGmt.setTimeZone(TimeZone.getTimeZone("GMT"));
        assertThat(request.getHeader("Date"), equalTo(dateFormatGmt.format(currentTime)));
    }

    @Test
    public void sendsAcceptsGzipHeader() throws Exception {
        MockWebServer mockWebServer = mockWebServerRule.start();
        enqueueSuccess(mockWebServer);

        OpenRosaServerClient client = subject.get("http", "Android", null);
        client.makeRequest(buildRequest(mockWebServer, ""), new Date());

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getHeader("Accept-Encoding"), equalTo("gzip"));
    }

    @Test
    // smap - upstream asserted this for every http host. It now holds only for public hosts,
    // so the request is given a public hostname rather than the MockWebServer's loopback one.
    public void withCredentials_whenBasicChallengeReceived_whenHttpAndPublicHost_doesNotRetryWithCredentials() throws Exception {
        MockWebServer mockWebServer = mockWebServerRule.start();
        enqueueBasicChallenge(mockWebServer);
        enqueueSuccess(mockWebServer);

        OpenRosaServerClient client = buildSubject(loopbackDns())
                .get("http", "Android", new HttpCredentials("user", "pass"));
        client.makeRequest(buildPublicHostRequest(mockWebServer), new Date());

        assertThat(mockWebServer.getRequestCount(), equalTo(1));
    }

    @Test
    // smap - a development server on the local network is plain http, so basic auth is answered there
    public void withCredentials_whenBasicChallengeReceived_whenHttpAndPrivateHost_retriesWithCredentials() throws Exception {
        MockWebServer mockWebServer = mockWebServerRule.start();
        enqueueBasicChallenge(mockWebServer);
        enqueueSuccess(mockWebServer);

        OpenRosaServerClient client = subject.get("http", "Android", new HttpCredentials("user", "pass"));
        client.makeRequest(buildRequest(mockWebServer, ""), new Date());

        assertThat(mockWebServer.getRequestCount(), equalTo(2));
        mockWebServer.takeRequest();
        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getHeader("Authorization"), equalTo("Basic dXNlcjpwYXNz"));
    }

    @Test
    public void withCredentials_whenBasicChallengeReceived_whenHttps_retriesWithCredentials() throws Exception {
        MockWebServer mockWebServer = mockWebServerRule.start();

        enqueueBasicChallenge(mockWebServer);
        enqueueSuccess(mockWebServer);

        OpenRosaServerClient client = subject.get("https", "Android", new HttpCredentials("user", "pass"));
        client.makeRequest(buildRequest(mockWebServer, ""), new Date());

        assertThat(mockWebServer.getRequestCount(), equalTo(2));
        mockWebServer.takeRequest();
        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getHeader("Authorization"), equalTo("Basic dXNlcjpwYXNz"));
    }

    @Test
    public void withCredentials_whenDigestChallengeReceived_retriesWithCredentials() throws Exception {
        MockWebServer mockWebServer = mockWebServerRule.start();
        enqueueDigestChallenge(mockWebServer);
        enqueueSuccess(mockWebServer);

        OpenRosaServerClient client = subject.get("http", "Android", new HttpCredentials("user", "pass"));
        client.makeRequest(buildRequest(mockWebServer, ""), new Date());

        assertThat(mockWebServer.getRequestCount(), equalTo(2));
        mockWebServer.takeRequest();
        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getHeader("Authorization"), startsWith("Digest"));
    }

    @Test
    public void withCredentials_whenDigestChallengeReceived_whenHttps_retriesWithCredentials() throws Exception {
        MockWebServer mockWebServer = mockWebServerRule.start();

        enqueueDigestChallenge(mockWebServer);
        enqueueSuccess(mockWebServer);

        OpenRosaServerClient client = subject.get("https", "Android", new HttpCredentials("user", "pass"));
        client.makeRequest(buildRequest(mockWebServer, ""), new Date());

        assertThat(mockWebServer.getRequestCount(), equalTo(2));
        mockWebServer.takeRequest();
        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getHeader("Authorization"), startsWith("Digest"));
    }

    @Test
    public void withCredentials_onceBasicChallenged_whenHttps_proactivelySendsCredentials() throws Exception {
        MockWebServer mockWebServer = mockWebServerRule.start();

        enqueueBasicChallenge(mockWebServer);
        enqueueSuccess(mockWebServer);
        enqueueSuccess(mockWebServer);

        OpenRosaServerClient client = subject.get("https", "Android", new HttpCredentials("user", "pass"));
        client.makeRequest(buildRequest(mockWebServer, ""), new Date());
        client.makeRequest(buildRequest(mockWebServer, "/different"), new Date());

        assertThat(mockWebServer.getRequestCount(), equalTo(3));
        mockWebServer.takeRequest();
        mockWebServer.takeRequest();
        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getHeader("Authorization"), equalTo("Basic dXNlcjpwYXNz"));
    }

    @Test
    public void withCredentials_onceDigestChallenged_proactivelySendsCredentials() throws Exception {
        MockWebServer mockWebServer = mockWebServerRule.start();

        enqueueDigestChallenge(mockWebServer);
        enqueueSuccess(mockWebServer);
        enqueueSuccess(mockWebServer);

        OpenRosaServerClient client = subject.get("http", "Android", new HttpCredentials("user", "pass"));
        client.makeRequest(buildRequest(mockWebServer, ""), new Date());
        client.makeRequest(buildRequest(mockWebServer, "/different"), new Date());

        assertThat(mockWebServer.getRequestCount(), equalTo(3));
        mockWebServer.takeRequest();
        mockWebServer.takeRequest();
        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getHeader("Authorization"), startsWith("Digest"));
    }

    @Test
    public void withCredentials_onceDigestChallenged_whenHttps_proactivelySendsCredentials() throws Exception {
        MockWebServer mockWebServer = mockWebServerRule.start();

        enqueueDigestChallenge(mockWebServer);
        enqueueSuccess(mockWebServer);
        enqueueSuccess(mockWebServer);

        OpenRosaServerClient client = subject.get("https", "Android", new HttpCredentials("user", "pass"));
        client.makeRequest(buildRequest(mockWebServer, ""), new Date());
        client.makeRequest(buildRequest(mockWebServer, "/different"), new Date());

        assertThat(mockWebServer.getRequestCount(), equalTo(3));
        mockWebServer.takeRequest();
        mockWebServer.takeRequest();
        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getHeader("Authorization"), startsWith("Digest"));
    }

    @Test
    public void authenticationIsCachedBetweenInstances() throws Exception {
        MockWebServer mockWebServer = mockWebServerRule.start();

        enqueueDigestChallenge(mockWebServer);
        enqueueSuccess(mockWebServer);
        enqueueSuccess(mockWebServer);

        subject.get("http", "Android", new HttpCredentials("user", "pass")).makeRequest(buildRequest(mockWebServer, ""), new Date());
        subject.get("http", "Android", new HttpCredentials("user", "pass")).makeRequest(buildRequest(mockWebServer, "/different"), new Date());

        assertThat(mockWebServer.getRequestCount(), equalTo(3));
        mockWebServer.takeRequest();
        mockWebServer.takeRequest();
        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getHeader("Authorization"), startsWith("Digest"));
    }

    @Test
    public void whenUsingDifferentCredentials_authenticationIsNotCachedBetweenInstances() throws Exception {
        MockWebServer mockWebServer = mockWebServerRule.start();

        enqueueDigestChallenge(mockWebServer);
        enqueueSuccess(mockWebServer);
        enqueueDigestChallenge(mockWebServer);
        enqueueSuccess(mockWebServer);

        subject.get("http", "Android", new HttpCredentials("user", "pass")).makeRequest(buildRequest(mockWebServer, ""), new Date());
        subject.get("http", "Android", new HttpCredentials("new-user", "pass")).makeRequest(buildRequest(mockWebServer, "/different"), new Date());

        assertThat(mockWebServer.getRequestCount(), equalTo(4));
        mockWebServer.takeRequest();
        mockWebServer.takeRequest();
        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getHeader("Authorization"), equalTo(null));
    }

    @Test
    public void whenUsingNullAndThenNonNullCredentials_authenticationIsNotCachedBetweenInstances() throws Exception {
        MockWebServer mockWebServer = mockWebServerRule.start();

        enqueueSuccess(mockWebServer);
        enqueueDigestChallenge(mockWebServer);
        enqueueSuccess(mockWebServer);

        subject.get("http", "Android", null).makeRequest(buildRequest(mockWebServer, ""), new Date());
        subject.get("http", "Android", new HttpCredentials("new-user", "pass")).makeRequest(buildRequest(mockWebServer, "/different"), new Date());

        assertThat(mockWebServer.getRequestCount(), equalTo(3));
        mockWebServer.takeRequest();
        mockWebServer.takeRequest();
        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getHeader("Authorization"), notNullValue());
    }

    @Test
    public void whenConnectingToDifferentHosts_authenticationIsNotCachedBetweenInstances() throws Exception {
        MockWebServer host1 = mockWebServerRule.start();
        MockWebServer host2 = mockWebServerRule.start();

        enqueueDigestChallenge(host1);
        enqueueSuccess(host1);

        enqueueDigestChallenge(host2);
        enqueueSuccess(host2);

        subject.get("http", "Android", new HttpCredentials("user", "pass")).makeRequest(buildRequest(host1, ""), new Date());
        subject.get("http", "Android", new HttpCredentials("user", "pass")).makeRequest(buildRequest(host2, ""), new Date());

        assertThat(host2.getRequestCount(), equalTo(2));

        RecordedRequest request = host2.takeRequest();
        assertThat(request.getHeader("Authorization"), equalTo(null));
    }

    @Test
    public void whenUsingHttpsThenHttp_doesNotRespondToBasicAuthChallengesInSecondInstance() throws Exception {
        MockWebServer host = mockWebServerRule.start();

        enqueueDigestChallenge(host);
        enqueueSuccess(host);

        enqueueBasicChallenge(host);
        enqueueSuccess(host);

        // smap - the http leg uses a public hostname, since basic auth over http is now
        // answered for private hosts
        OpenRosaServerClientProvider provider = buildSubject(loopbackDns());
        provider.get("https", "Android", new HttpCredentials("user", "pass")).makeRequest(buildRequest(host, ""), new Date());
        provider.get("http", "Android", new HttpCredentials("user", "pass")).makeRequest(buildPublicHostRequest(host), new Date());

        assertThat(host.getRequestCount(), equalTo(3));

        host.takeRequest();
        host.takeRequest();
        RecordedRequest request = host.takeRequest();
        assertThat(request.getHeader("Authorization"), equalTo(null));
    }

    @Test
    public void whenLastRequestSetCookies_nextRequestDoesNotSendThem() throws Exception {
        MockWebServer mockWebServer = mockWebServerRule.start();

        mockWebServer.enqueue(new MockResponse()
                .addHeader("Set-Cookie", "blah=blah"));
        enqueueSuccess(mockWebServer);

        subject.get("http", "Android", new HttpCredentials("user", "pass")).makeRequest(buildRequest(mockWebServer, ""), new Date());
        subject.get("http", "Android", new HttpCredentials("user", "pass")).makeRequest(buildRequest(mockWebServer, ""), new Date());

        mockWebServer.takeRequest();
        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getHeader("Cookie"), isEmptyOrNullString());
    }

    // smap - resolves any hostname to the loopback address the MockWebServer listens on
    @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
    private static Dns loopbackDns() {
        return new Dns() {
            @Override
            public List<InetAddress> lookup(String hostname) throws UnknownHostException {
                return Collections.singletonList(InetAddress.getByName("127.0.0.1"));
            }
        };
    }

    // smap - a request to the MockWebServer that carries a public hostname
    private static Request buildPublicHostRequest(MockWebServer mockWebServer) {
        return new Request.Builder()
                .url("http://example.com:" + mockWebServer.getPort() + "/")
                .build();
    }

    protected void enqueueSuccess(MockWebServer mockWebServer) {
        mockWebServer.enqueue(new MockResponse());
    }

    private void enqueueBasicChallenge(MockWebServer mockWebServer) {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(401)
                .addHeader("WWW-Authenticate: Basic realm=\"protected area\"")
                .setBody("Please authenticate."));
    }

    private void enqueueDigestChallenge(MockWebServer mockWebServer) {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(401)
                .addHeader("WWW-Authenticate: Digest realm=\"ODK Aggregate\", qop=\"auth\", nonce=\"MTU2NTA4MjEzODI4OTpmMjc4MDM5N2YxZTJiNDRiNjNiYTBiMThiOWQ4ZTlkMg==\"")
                .setBody("Please authenticate."));
    }
}
