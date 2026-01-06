package au.smap.fieldTask.formmanagement;

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
import java.util.ArrayList;

/**
 * Tests for MultiFormDownloaderSmap
 */
@RunWith(RobolectricTestRunner.class)
public class MultiFormDownloaderSmapTest {

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
    public void testCreation() {
        MultiFormDownloaderSmap downloader = new MultiFormDownloaderSmap();
        assertNotNull(downloader);
    }

    @Test
    public void testEmptyFormList() {
        MultiFormDownloaderSmap downloader = new MultiFormDownloaderSmap();
        ArrayList<ServerFormDetailsSmap> forms = new ArrayList<>();

        HashMap<ServerFormDetailsSmap, String> result = downloader.downloadForms(forms);

        assertNotNull(result);
    }
}
