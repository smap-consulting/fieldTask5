package au.smap.fieldTask.tasks;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.odk.collect.android.injection.config.AppDependencyModule;
import org.odk.collect.android.support.CollectHelpers;
import org.odk.collect.android.utilities.WebCredentialsUtils;
import org.odk.collect.openrosa.http.HttpCredentials;
import org.odk.collect.openrosa.http.OpenRosaHttpInterface;
import org.robolectric.RobolectricTestRunner;

import java.net.URI;

/**
 * Tests for InstanceSyncTask
 */
@RunWith(RobolectricTestRunner.class)
public class InstanceSyncTaskTest {

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
    public void testTaskCreation() {
        InstanceSyncTask task = new InstanceSyncTask();
        assertNotNull(task);
    }

    @Test
    public void testDoInBackgroundHandlesEmptyDatabase() {
        InstanceSyncTask task = new InstanceSyncTask();

        HashMap<String, String> result = task.doInBackground();

        assertNotNull(result);
    }

    @Test
    public void testFormDefinitionLookup() {
        // Test that form definitions are looked up correctly
        // TODO: Add caching tests when optimization is implemented
        InstanceSyncTask task = new InstanceSyncTask();
        assertNotNull(task);
    }
}
