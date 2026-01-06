package au.smap.fieldTask.external.handler;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.odk.collect.android.injection.config.AppDependencyModule;
import org.odk.collect.android.support.CollectHelpers;
import org.odk.collect.openrosa.http.HttpCredentials;
import org.odk.collect.openrosa.http.OpenRosaHttpInterface;
import org.odk.collect.android.utilities.WebCredentialsUtils;
import org.robolectric.RobolectricTestRunner;

import java.net.URI;

/**
 * Tests for SmapRemoteDataHandlerLookup
 */
@RunWith(RobolectricTestRunner.class)
public class SmapRemoteDataHandlerLookupTest {

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
    public void testHandlerCreation() {
        SmapRemoteDataHandlerLookup handler = new SmapRemoteDataHandlerLookup();
        assertNotNull(handler);
    }

    @Test
    public void testRemoteLookup() throws Exception {
        when(httpInterface.getRequest(any(URI.class), any(String.class), any(HttpCredentials.class), any()))
                .thenReturn("{\"data\":[{\"id\":\"1\",\"name\":\"Test\"}]}");

        SmapRemoteDataHandlerLookup handler = new SmapRemoteDataHandlerLookup();
        assertNotNull(handler);
    }
}
