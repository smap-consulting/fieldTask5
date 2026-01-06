package au.smap.fieldTask.tasks;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.odk.collect.android.injection.config.AppDependencyModule;
import org.odk.collect.android.support.CollectHelpers;
import org.odk.collect.openrosa.http.HttpCredentials;
import org.odk.collect.openrosa.http.OpenRosaHttpInterface;
import org.odk.collect.android.utilities.WebCredentialsUtils;
import org.odk.collect.openrosa.http.OpenRosaHttpResponse;
import org.robolectric.RobolectricTestRunner;

import java.io.ByteArrayInputStream;
import java.net.URI;

@RunWith(RobolectricTestRunner.class)
public class SmapChangeOrganisationTaskTest {

    private OpenRosaHttpInterface httpInterface;
    private WebCredentialsUtils webCredentialsUtils;
    private HttpCredentials credentials;

    @Before
    public void setup() {
        httpInterface = mock(OpenRosaHttpInterface.class);
        webCredentialsUtils = mock(WebCredentialsUtils.class);
        credentials = mock(HttpCredentials.class);

        when(webCredentialsUtils.getCredentials(any(URI.class))).thenReturn(credentials);

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
    public void testSuccessfulOrganisationChange() throws Exception {
        OpenRosaHttpResponse mockResponse = mock(OpenRosaHttpResponse.class);
        when(mockResponse.getInputStream()).thenReturn(new ByteArrayInputStream("{}".getBytes()));

        when(httpInterface.executeGetRequest(any(URI.class), any(), any(HttpCredentials.class)))
                .thenReturn(mockResponse);
        when(httpInterface.getRequest(any(URI.class), eq("application/json"), any(HttpCredentials.class), anyMap()))
                .thenReturn("success");

        SmapChangeOrganisationTask task = new SmapChangeOrganisationTask();
        String result = task.doInBackground("https://server.com", "TestOrg");

        assertNotNull(result);
        verify(httpInterface).executeGetRequest(any(URI.class), any(), any(HttpCredentials.class));
        verify(httpInterface).getRequest(any(URI.class), eq("application/json"), any(HttpCredentials.class), anyMap());
    }

    @Test
    public void testOrganisationNameEncoding() throws Exception {
        OpenRosaHttpResponse mockResponse = mock(OpenRosaHttpResponse.class);
        when(mockResponse.getInputStream()).thenReturn(new ByteArrayInputStream("{}".getBytes()));

        when(httpInterface.executeGetRequest(any(URI.class), any(), any(HttpCredentials.class)))
                .thenReturn(mockResponse);
        when(httpInterface.getRequest(any(URI.class), eq("application/json"), any(HttpCredentials.class), anyMap()))
                .thenReturn("success");

        SmapChangeOrganisationTask task = new SmapChangeOrganisationTask();

        // Test with special characters that need encoding
        task.doInBackground("https://server.com", "Test Org & Co");

        verify(httpInterface).executeGetRequest(any(URI.class), any(), any(HttpCredentials.class));
    }

    @Test
    public void testNetworkError() throws Exception {
        when(httpInterface.executeGetRequest(any(URI.class), any(), any(HttpCredentials.class)))
                .thenThrow(new java.net.UnknownHostException("Network error"));

        SmapChangeOrganisationTask task = new SmapChangeOrganisationTask();
        String result = task.doInBackground("https://server.com", "TestOrg");

        // Should handle exception gracefully
        assertNotNull(result);
    }
}
