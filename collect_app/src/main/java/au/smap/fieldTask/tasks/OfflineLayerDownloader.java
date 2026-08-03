/*
 * Copyright (C) 2026 Smap Consulting Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package au.smap.fieldTask.tasks;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.odk.collect.android.injection.DaggerUtils;
import org.odk.collect.android.storage.StoragePathProvider;
import org.odk.collect.android.storage.StorageSubdirectory;
import org.odk.collect.android.utilities.WebCredentialsUtils;
import org.odk.collect.openrosa.http.HttpCredentialsInterface;
import org.odk.collect.openrosa.http.HttpGetResult;
import org.odk.collect.openrosa.http.OpenRosaHttpInterface;
import org.odk.collect.maps.layers.ServerLayers;
import org.odk.collect.settings.SettingsProvider;
import org.odk.collect.settings.keys.ProjectKeys;
import org.odk.collect.shared.strings.Md5;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import au.smap.fieldTask.models.OfflineLayer;
import timber.log.Timber;

/**
 * Downloads the offline map layers that the server has assigned to this user.
 *
 * These files are large and devices are often on poor connections, so a download that is
 * interrupted is resumed from where it stopped using a byte range request rather than being
 * started again.  A layer whose checksum already matches the file on the device is skipped.
 *
 * Layers are written to a directory that only the server manages.  Anything in there that the
 * server no longer assigns to the user is deleted, so unassigning a layer removes it from the
 * device on the next refresh.
 */
public class OfflineLayerDownloader {

    // Layers supplied by the server are kept apart from any that the user added themselves.
    // The picker uses the same constant to tell the two apart.
    public static final String SERVER_LAYER_DIR = ServerLayers.DIR;

    private static final String PART_SUFFIX = ".part";

    private final OpenRosaHttpInterface httpInterface;
    private final WebCredentialsUtils webCredentialsUtils;
    private final StoragePathProvider storagePathProvider;
    private final SettingsProvider settingsProvider;

    public OfflineLayerDownloader(OpenRosaHttpInterface httpInterface,
                                  WebCredentialsUtils webCredentialsUtils,
                                  StoragePathProvider storagePathProvider,
                                  SettingsProvider settingsProvider) {
        this.httpInterface = httpInterface;
        this.webCredentialsUtils = webCredentialsUtils;
        this.storagePathProvider = storagePathProvider;
        this.settingsProvider = settingsProvider;
    }

    /**
     * Save the list of layers returned by the server so that a download can be started later,
     * either by the background job or by the user from the layer picker.
     */
    public static void saveManifest(SettingsProvider settingsProvider, List<OfflineLayer> layers) {
        String json = layers == null ? "" : new Gson().toJson(layers);
        settingsProvider.getUnprotectedSettings().save(ProjectKeys.KEY_SMAP_OFFLINE_LAYERS, json);
    }

    /**
     * The layers the server has assigned to this user.  Empty if none have been sent.
     */
    public static ArrayList<OfflineLayer> getManifest(SettingsProvider settingsProvider) {
        String json = settingsProvider.getUnprotectedSettings().getString(ProjectKeys.KEY_SMAP_OFFLINE_LAYERS);
        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            ArrayList<OfflineLayer> layers = new Gson().fromJson(json,
                    new TypeToken<ArrayList<OfflineLayer>>() {}.getType());
            return layers == null ? new ArrayList<>() : layers;
        } catch (Exception e) {
            Timber.e(e, "Invalid offline layer manifest");
            return new ArrayList<>();
        }
    }

    /**
     * The directory holding the layers supplied by the server.  It sits under the shared layers
     * directory, which is listed recursively, so the files show up in the layer picker without
     * any change to the repository.
     */
    public File getServerLayerDir() {
        File dir = new File(storagePathProvider.getOdkDirPath(StorageSubdirectory.SHARED_LAYERS),
                SERVER_LAYER_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    /**
     * True if the layer has been fully downloaded and matches the checksum from the server
     */
    public boolean isDownloaded(@NonNull OfflineLayer layer) {
        File f = new File(getServerLayerDir(), layer.fileName);
        return f.exists() && matches(f, layer.md5);
    }

    /**
     * How many bytes of a layer are already on the device.  Used to show progress when a
     * download has been interrupted.
     */
    public long getDownloadedSize(@NonNull OfflineLayer layer) {
        File complete = new File(getServerLayerDir(), layer.fileName);
        if (complete.exists()) {
            return complete.length();
        }
        File part = new File(getServerLayerDir(), layer.fileName + PART_SUFFIX);
        return part.exists() ? part.length() : 0;
    }

    /**
     * Download every layer in the manifest that is not already on the device and remove any
     * layer files that are no longer assigned.
     *
     * @return true if all the layers are now on the device
     */
    public boolean downloadAll(List<OfflineLayer> layers, Supplier isStopped) {

        boolean allDone = true;

        removeUnassigned(layers);

        if (layers != null) {
            for (OfflineLayer layer : layers) {
                if (isStopped != null && isStopped.get()) {
                    return false;
                }
                try {
                    if (!download(layer, isStopped)) {
                        allDone = false;
                    }
                } catch (Exception e) {
                    Timber.e(e, "Failed to download offline layer %s", layer.name);
                    allDone = false;
                }
            }
        }

        return allDone;
    }

    /**
     * Download a single layer, resuming a part downloaded file if there is one.
     *
     * @return true if the layer is on the device and matches the checksum from the server
     */
    public boolean download(@NonNull OfflineLayer layer, Supplier isStopped) throws Exception {

        File dir = getServerLayerDir();
        File target = new File(dir, layer.fileName);

        if (target.exists()) {
            if (matches(target, layer.md5)) {
                return true;            // Already have this version
            }
            // The layer has been replaced on the server, start again
            target.delete();
        }

        File part = new File(dir, layer.fileName + PART_SUFFIX);
        long have = part.exists() ? part.length() : 0;

        if (have > layer.size && layer.size > 0) {
            // The file on the server is smaller than what we have, our part file is stale
            part.delete();
            have = 0;
        }

        HashMap<String, String> headers = new HashMap<>();
        if (have > 0) {
            headers.put("Range", "bytes=" + have + "-");
        }

        URI uri = URI.create(layer.url);
        HttpCredentialsInterface credentials = webCredentialsUtils.getCredentials(uri);

        HttpGetResult result = httpInterface.executeGetRequest(uri, credentials, headers);

        // If the server ignored the range and sent the whole file, start from the beginning
        boolean append = have > 0 && result.getStatusCode() == 206;
        if (have > 0 && !append) {
            Timber.i("Server did not honour the range request for %s, starting again", layer.name);
            have = 0;
        }

        try (InputStream is = result.getInputStream();
             OutputStream os = new FileOutputStream(part, append)) {

            byte[] buffer = new byte[65536];
            int bytes;
            while ((bytes = is.read(buffer)) != -1) {
                if (isStopped != null && isStopped.get()) {
                    // Leave the part file in place, the next run resumes from here
                    return false;
                }
                os.write(buffer, 0, bytes);
            }
        }

        if (!matches(part, layer.md5)) {
            Timber.e("Checksum mismatch for offline layer %s, discarding", layer.name);
            part.delete();
            return false;
        }

        if (!part.renameTo(target)) {
            Timber.e("Failed to move downloaded layer into place: %s", layer.name);
            return false;
        }

        Timber.i("Downloaded offline layer %s", layer.name);
        selectIfNoneSelected(layer);
        return true;
    }

    /**
     * Make a newly downloaded layer the one shown on the map, but only when the user has not
     * chosen a layer themselves.  Without this a user who has never opened the layer picker
     * downloads their maps and sees no change.
     */
    private void selectIfNoneSelected(@NonNull OfflineLayer layer) {

        String current = settingsProvider.getUnprotectedSettings().getString(ProjectKeys.KEY_REFERENCE_LAYER);
        if (current != null && !current.trim().isEmpty()) {
            return;             // The user has already chosen a layer, leave it alone
        }

        // Layer ids are the path relative to the shared layers directory, see
        // DirectoryReferenceLayerRepository.getIdForFile
        String id = SERVER_LAYER_DIR + File.separator + layer.fileName;
        settingsProvider.getUnprotectedSettings().save(ProjectKeys.KEY_REFERENCE_LAYER, id);
        Timber.i("Selected offline layer %s as none was set", layer.name);
    }

    /**
     * Delete layer files that the server no longer assigns to this user
     */
    private void removeUnassigned(List<OfflineLayer> layers) {

        Set<String> keep = new HashSet<>();
        if (layers != null) {
            for (OfflineLayer layer : layers) {
                keep.add(layer.fileName);
                keep.add(layer.fileName + PART_SUFFIX);
            }
        }

        File[] existing = getServerLayerDir().listFiles();
        if (existing != null) {
            for (File f : existing) {
                if (f.isFile() && !keep.contains(f.getName())) {
                    Timber.i("Removing unassigned offline layer %s", f.getName());
                    clearIfSelected(f.getName());
                    f.delete();
                }
            }
        }
    }

    /**
     * Stop showing a layer that is being removed, otherwise the setting points at a file that
     * is no longer there
     */
    private void clearIfSelected(String fileName) {
        String id = SERVER_LAYER_DIR + File.separator + fileName;
        String current = settingsProvider.getUnprotectedSettings().getString(ProjectKeys.KEY_REFERENCE_LAYER);
        if (id.equals(current)) {
            settingsProvider.getUnprotectedSettings().save(ProjectKeys.KEY_REFERENCE_LAYER, null);
        }
    }

    private boolean matches(File f, String md5) {
        if (md5 == null) {
            return false;
        }
        String hash = Md5.getMd5Hash(f);
        return md5.equalsIgnoreCase(hash);
    }

    /**
     * Lets the caller stop a download that is part way through, for example when WorkManager
     * cancels the job.  Not using java.util.function.Supplier as that needs a higher min sdk.
     */
    public interface Supplier {
        boolean get();
    }

    /**
     * Build a downloader with the application dependencies
     */
    public static OfflineLayerDownloader create() {
        return new OfflineLayerDownloader(
                DaggerUtils.getComponent(org.odk.collect.android.application.Collect.getInstance()).openRosaHttpInterface(),
                DaggerUtils.getComponent(org.odk.collect.android.application.Collect.getInstance()).webCredentialsUtils(),
                DaggerUtils.getComponent(org.odk.collect.android.application.Collect.getInstance()).storagePathProvider(),
                DaggerUtils.getComponent(org.odk.collect.android.application.Collect.getInstance()).settingsProvider());
    }
}
