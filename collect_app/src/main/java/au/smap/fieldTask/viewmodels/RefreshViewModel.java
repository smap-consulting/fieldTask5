package au.smap.fieldTask.viewmodels;

import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.odk.collect.android.R;
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.listeners.InstanceUploaderListener;

import java.util.HashMap;
import java.util.Map;

import au.smap.fieldTask.formmanagement.ServerFormDetailsSmap;
import au.smap.fieldTask.listeners.DownloadFormsTaskListenerSmap;
import au.smap.fieldTask.listeners.TaskDownloaderListener;
import au.smap.fieldTask.tasks.DownloadTasksTask;

/**
 * smap - Owns a running task refresh (DownloadTasksTask) so it survives activity recreation
 * (rotation, screen lock). Progress, running state and the final result are exposed as LiveData
 * for the activity to observe; because the ViewModel (not the activity) is the task's listener,
 * callbacks always land on a live object and the UI simply re-attaches on recreation.
 */
public class RefreshViewModel extends ViewModel
        implements TaskDownloaderListener, InstanceUploaderListener, DownloadFormsTaskListenerSmap {

    private final MutableLiveData<String> progress = new MutableLiveData<>();
    private final MutableLiveData<Boolean> running = new MutableLiveData<>(false);
    private final MutableLiveData<Consumable<HashMap<String, String>>> result = new MutableLiveData<>();

    private DownloadTasksTask downloadTask;
    private boolean manual;     // true for a user-initiated refresh (shows progress + result), false for silent auto-refresh

    public LiveData<String> getProgress() {
        return progress;
    }

    public LiveData<Boolean> getRunning() {
        return running;
    }

    public LiveData<Consumable<HashMap<String, String>>> getResult() {
        return result;
    }

    public boolean isManualRefresh() {
        return manual;
    }

    public boolean isRunning() {
        return Boolean.TRUE.equals(running.getValue());
    }

    /**
     * Start a refresh. No-op if one is already running.
     */
    public void startRefresh(boolean manual, String initialMessage) {
        if (isRunning()) {
            return;
        }
        this.manual = manual;
        running.setValue(true);
        progress.setValue(initialMessage);
        downloadTask = new DownloadTasksTask();
        downloadTask.setDownloaderListener(this, Collect.getInstance());
        downloadTask.execute();
    }

    public void cancel() {
        if (downloadTask != null) {
            downloadTask.cancel(true);
        }
    }

    // TaskDownloaderListener - the overall refresh
    @Override
    public void progressUpdate(String p) {
        progress.postValue(p);
    }

    @Override
    public void taskDownloadingComplete(HashMap<String, String> res) {
        downloadTask = null;
        running.postValue(false);
        result.postValue(new Consumable<>(res));
    }

    // DownloadFormsTaskListenerSmap - form downloads within the refresh
    @Override
    public void formsDownloadingComplete(Map<ServerFormDetailsSmap, String> r) {
        // reported through the overall result
    }

    @Override
    public void progressUpdate(String currentFile, int p, int total) {
        progress.postValue(Collect.getInstance().getString(R.string.smap_checking_file,
                currentFile, String.valueOf(p), String.valueOf(total)));
    }

    @Override
    public void formsDownloadingCancelled() {
        // reported through the overall result
    }

    // InstanceUploaderListener - completed-form uploads within the refresh
    @Override
    public void uploadingComplete(HashMap<String, String> r) {
        // reported through the overall result
    }

    @Override
    public void progressUpdate(int p, int total) {
        progress.postValue(Collect.getInstance().getString(
                org.odk.collect.strings.R.string.sending_items, String.valueOf(p), String.valueOf(total)));
    }

    @Override
    public void authRequest(Uri url, HashMap<String, String> doneSoFar) {
        // not used for smap token/basic auth
    }
}
