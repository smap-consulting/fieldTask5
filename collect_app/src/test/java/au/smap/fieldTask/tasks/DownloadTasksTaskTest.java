package au.smap.fieldTask.tasks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import au.smap.fieldTask.listeners.TaskDownloaderListener;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.injection.config.AppDependencyModule;
import org.odk.collect.android.support.CollectHelpers;
import org.odk.collect.android.utilities.WebCredentialsUtils;
import org.odk.collect.openrosa.http.HttpCredentials;
import org.odk.collect.openrosa.http.OpenRosaHttpInterface;
import org.robolectric.RobolectricTestRunner;

import java.net.URI;

/**
 * Tests for DownloadTasksTask
 * Note: Full integration testing requires mocking extensive server responses
 */
@RunWith(RobolectricTestRunner.class)
public class DownloadTasksTaskTest {

    private OpenRosaHttpInterface httpInterface;
    private WebCredentialsUtils webCredentialsUtils;
    private HttpCredentials credentials;
    private TaskDownloaderListener listener;
    private Context context;

    @Before
    public void setup() {
        context = Collect.getInstance();
        httpInterface = mock(OpenRosaHttpInterface.class);
        webCredentialsUtils = mock(WebCredentialsUtils.class);
        credentials = mock(HttpCredentials.class);
        listener = mock(TaskDownloaderListener.class);

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
        DownloadTasksTask task = new DownloadTasksTask();
        assertNotNull(task);
    }

    @Test
    public void testListenerSetting() {
        DownloadTasksTask task = new DownloadTasksTask();
        task.setDownloaderListener(listener);
        // Listener should be set without exceptions
    }

    @Test
    public void testEmptyServerResponse() throws Exception {
        when(httpInterface.getRequest(any(URI.class), any(String.class), any(HttpCredentials.class), any()))
                .thenReturn("{}");

        DownloadTasksTask task = new DownloadTasksTask();
        task.setDownloaderListener(listener);

        HashMap<String, String> result = task.doInBackground();

        assertNotNull(result);
    }

    @Test
    public void testNetworkErrorHandling() throws Exception {
        when(httpInterface.getRequest(any(URI.class), any(String.class), any(HttpCredentials.class), any()))
                .thenThrow(new java.net.UnknownHostException("Network error"));

        DownloadTasksTask task = new DownloadTasksTask();
        task.setDownloaderListener(listener);

        HashMap<String, String> result = task.doInBackground();

        assertNotNull(result);
        // Should handle error gracefully
    }
}
