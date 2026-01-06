package au.smap.fieldTask.tasks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import au.smap.fieldTask.listeners.SmapLoginListener;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.odk.collect.android.injection.config.AppDependencyModule;
import org.odk.collect.android.support.CollectHelpers;
import org.odk.collect.openrosa.http.HttpCredentials;
import org.odk.collect.openrosa.http.OpenRosaHttpInterface;
import org.odk.collect.android.utilities.WebCredentialsUtils;
import org.robolectric.RobolectricTestRunner;

import java.net.URI;

@RunWith(RobolectricTestRunner.class)
public class SmapLoginTaskTest {

    private OpenRosaHttpInterface httpInterface;
    private WebCredentialsUtils webCredentialsUtils;
    private SmapLoginListener listener;

    @Before
    public void setup() {
        httpInterface = mock(OpenRosaHttpInterface.class);
        webCredentialsUtils = mock(WebCredentialsUtils.class);
        listener = mock(SmapLoginListener.class);

        CollectHelpers.overrideAppDependencyModule(new AppDependencyModule() {
            @Override
            public OpenRosaHttpInterface provideHttpInterface() {
                return httpInterface;
            }

            @Override
            public WebCredentialsUtils providesWebCredentials() {
                return webCredentialsUtils;
            }
        });
    }

    @Test
    public void testSuccessfulLoginWithPassword() throws Exception {
        when(httpInterface.loginRequest(any(URI.class), any(), any(HttpCredentials.class)))
                .thenReturn("success");

        SmapLoginTask task = new SmapLoginTask();
        task.setListener(listener);

        String result = task.doInBackground("false", "https://server.com", "user", "pass", "");

        assertEquals("success", result);

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        ArgumentCaptor<HttpCredentials> credsCaptor = ArgumentCaptor.forClass(HttpCredentials.class);
        verify(httpInterface).loginRequest(uriCaptor.capture(), eq(null), credsCaptor.capture());

        assertTrue(uriCaptor.getValue().toString().endsWith("/login"));
        assertEquals("user", credsCaptor.getValue().getUsername());
        assertEquals("pass", credsCaptor.getValue().getPassword());
    }

    @Test
    public void testSuccessfulLoginWithToken() throws Exception {
        when(httpInterface.loginRequest(any(URI.class), any(), any(HttpCredentials.class)))
                .thenReturn("success");

        SmapLoginTask task = new SmapLoginTask();
        task.setListener(listener);

        String result = task.doInBackground("true", "https://server.com", "user", "", "token123");

        assertEquals("success", result);

        ArgumentCaptor<HttpCredentials> credsCaptor = ArgumentCaptor.forClass(HttpCredentials.class);
        verify(httpInterface).loginRequest(any(URI.class), eq(null), credsCaptor.capture());

        assertTrue(credsCaptor.getValue().isUseToken());
        assertEquals("token123", credsCaptor.getValue().getToken());
    }

    @Test
    public void testFailedAuthentication() throws Exception {
        when(httpInterface.loginRequest(any(URI.class), any(), any(HttpCredentials.class)))
                .thenReturn("error: 401 Unauthorized");

        SmapLoginTask task = new SmapLoginTask();
        task.setListener(listener);

        String result = task.doInBackground("false", "https://server.com", "user", "wrongpass", "");

        assertTrue(result.contains("error"));
    }

    @Test
    public void testNetworkError() throws Exception {
        when(httpInterface.loginRequest(any(URI.class), any(), any(HttpCredentials.class)))
                .thenThrow(new java.net.UnknownHostException("Unknown host"));

        SmapLoginTask task = new SmapLoginTask();
        task.setListener(listener);

        String result = task.doInBackground("false", "https://server.com", "user", "pass", "");

        assertTrue(result.startsWith("error:"));
        assertTrue(result.contains("Unknown host"));
    }

    @Test
    public void testListenerCallback() {
        SmapLoginTask task = new SmapLoginTask();
        task.setListener(listener);

        task.onPostExecute("success");

        verify(listener).loginComplete("success");
    }

    @Test
    public void testNullListener() {
        // Should not throw exception
        SmapLoginTask task = new SmapLoginTask();
        task.onPostExecute("success");
    }
}
