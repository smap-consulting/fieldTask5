/*
 * Copyright (C) 2017 University of Washington
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

package org.odk.collect.android.application;

import static org.odk.collect.settings.keys.MetaKeys.KEY_GOOGLE_BUG_154855417_FIXED;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;
import org.odk.collect.android.dynamicpreload.ExternalDataManager;
import org.odk.collect.android.injection.DaggerUtils;
import org.odk.collect.android.injection.config.AppDependencyComponent;
import org.odk.collect.android.injection.config.CollectDrawDependencyModule;
import org.odk.collect.android.injection.config.CollectGeoDependencyModule;
import org.odk.collect.android.injection.config.CollectGoogleMapsDependencyModule;
import org.odk.collect.android.injection.config.CollectOsmDroidDependencyModule;
import org.odk.collect.android.injection.config.CollectProjectsDependencyModule;
import org.odk.collect.android.injection.config.CollectSelfieCameraDependencyModule;
import org.odk.collect.android.injection.config.DaggerAppDependencyComponent;
import org.odk.collect.android.storage.StoragePathProvider;
import org.odk.collect.android.utilities.CollectStrictMode;
import org.odk.collect.android.utilities.FormsRepositoryProvider;
import org.odk.collect.android.utilities.LocaleHelper;
import org.odk.collect.androidshared.data.AppState;
import org.odk.collect.androidshared.data.StateStore;
import org.odk.collect.androidshared.system.ExternalFilesUtils;
import org.odk.collect.androidshared.utils.UniqueIdGenerator;
import org.odk.collect.async.Scheduler;
import org.odk.collect.async.network.NetworkStateProvider;
import org.odk.collect.audiorecorder.AudioRecorderDependencyComponent;
import org.odk.collect.audiorecorder.AudioRecorderDependencyComponentProvider;
import org.odk.collect.audiorecorder.AudioRecorderDependencyModule;
import org.odk.collect.audiorecorder.DaggerAudioRecorderDependencyComponent;
import org.odk.collect.crashhandler.CrashHandler;
import org.odk.collect.draw.DaggerDrawDependencyComponent;
import org.odk.collect.draw.DrawDependencyComponent;
import org.odk.collect.draw.DrawDependencyComponentProvider;
import org.odk.collect.entities.DaggerEntitiesDependencyComponent;
import org.odk.collect.entities.EntitiesDependencyComponent;
import org.odk.collect.entities.EntitiesDependencyComponentProvider;
import org.odk.collect.entities.EntitiesDependencyModule;
import org.odk.collect.entities.storage.EntitiesRepository;
import org.odk.collect.forms.Form;
import org.odk.collect.geo.DaggerGeoDependencyComponent;
import org.odk.collect.geo.GeoDependencyComponent;
import org.odk.collect.geo.GeoDependencyComponentProvider;
import org.odk.collect.googlemaps.DaggerGoogleMapsDependencyComponent;
import org.odk.collect.googlemaps.GoogleMapsDependencyComponent;
import org.odk.collect.googlemaps.GoogleMapsDependencyComponentProvider;
import org.odk.collect.location.DaggerLocationDependencyComponent;
import org.odk.collect.location.LocationClient;
import org.odk.collect.location.LocationDependencyComponent;
import org.odk.collect.location.LocationDependencyComponentProvider;
import org.odk.collect.location.LocationDependencyModule;
import org.odk.collect.maps.layers.ReferenceLayerRepository;
import org.odk.collect.osmdroid.DaggerOsmDroidDependencyComponent;
import org.odk.collect.osmdroid.OsmDroidDependencyComponent;
import org.odk.collect.osmdroid.OsmDroidDependencyComponentProvider;
import org.odk.collect.projects.DaggerProjectsDependencyComponent;
import org.odk.collect.projects.ProjectsDependencyComponent;
import org.odk.collect.projects.ProjectsDependencyComponentProvider;
import org.odk.collect.qrcode.mlkit.MlKitBarcodeScannerViewFactory;
import org.odk.collect.selfiecamera.DaggerSelfieCameraDependencyComponent;
import org.odk.collect.selfiecamera.SelfieCameraDependencyComponent;
import org.odk.collect.selfiecamera.SelfieCameraDependencyComponentProvider;
import org.odk.collect.settings.SettingsProvider;
import org.odk.collect.settings.keys.ProjectKeys;
import org.odk.collect.shared.injection.ObjectProvider;
import org.odk.collect.shared.injection.ObjectProviderHost;
import org.odk.collect.shared.injection.SupplierObjectProvider;
import org.odk.collect.shared.settings.Settings;
import org.odk.collect.shared.strings.Md5;
import org.odk.collect.strings.localization.LocalizedApplication;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Stack;

import au.smap.fieldTask.loaders.GeofenceEntry;
import au.smap.fieldTask.external.handler.SmapRemoteDataItem;
import au.smap.fieldTask.models.FormLaunchDetail;
import au.smap.fieldTask.models.FormRestartDetails;

public class Collect extends Application implements
        LocalizedApplication,
        AudioRecorderDependencyComponentProvider,
        ProjectsDependencyComponentProvider,
        GeoDependencyComponentProvider,
        OsmDroidDependencyComponentProvider,
        StateStore,
        ObjectProviderHost,
        EntitiesDependencyComponentProvider,
        SelfieCameraDependencyComponentProvider,
        GoogleMapsDependencyComponentProvider,
        DrawDependencyComponentProvider,
        LocationDependencyComponentProvider {

    public static String defaultSysLanguage;
    private static Collect singleton;

    private final AppState appState = new AppState();
    private final SupplierObjectProvider objectProvider = new SupplierObjectProvider();

    private ExternalDataManager externalDataManager;
    private AppDependencyComponent applicationComponent;

    private AudioRecorderDependencyComponent audioRecorderDependencyComponent;
    private ProjectsDependencyComponent projectsDependencyComponent;
    private GeoDependencyComponent geoDependencyComponent;
    private OsmDroidDependencyComponent osmDroidDependencyComponent;
    private EntitiesDependencyComponent entitiesDependencyComponent;
    private SelfieCameraDependencyComponent selfieCameraDependencyComponent;
    private GoogleMapsDependencyComponent googleMapsDependencyComponent;
    private DrawDependencyComponent drawDependencyComponent;

    // Smap-specific fields
    private Location location = null;
    private Location savedLocation = null;
    private ArrayList<GeofenceEntry> geofences = new ArrayList<GeofenceEntry>();
    private boolean tasksDownloading = false;
    private org.odk.collect.android.activities.FormFillingActivity formFillingActivity = null;
    private HashMap<String, SmapRemoteDataItem> remoteCache = null;
    private int remoteCalls;
    private Stack<FormLaunchDetail> formStack = new Stack<>();
    private HashMap<String, String> compoundAddresses = new HashMap<>();
    private FormRestartDetails mRestartDetails;
    private String formId;
    private String searchLocalData;

    /**
     * @deprecated we shouldn't have to reference a static singleton of the application. Code doing this
     * should either have a {@link Context} instance passed to it (or have any references removed if
     * possible).
     */
    @Deprecated
    public static Collect getInstance() {
        return singleton;
    }

    public ExternalDataManager getExternalDataManager() {
        return externalDataManager;
    }

    public void setExternalDataManager(ExternalDataManager externalDataManager) {
        this.externalDataManager = externalDataManager;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        singleton = this;

        CrashHandler.install(this).launchApp(
                () -> ExternalFilesUtils.testExternalFilesAccess(this),
                () -> {
                    setupDagger();
                    DaggerUtils.getComponent(this).inject(this);

                    applicationComponent.applicationInitializer().initialize();
                    fixGoogleBug154855417();
                    CollectStrictMode.enable();
                    MlKitBarcodeScannerViewFactory.init(this);
                }
        );
    }

    private void setupDagger() {
        applicationComponent = DaggerAppDependencyComponent.builder()
                .application(this)
                .build();

        audioRecorderDependencyComponent = DaggerAudioRecorderDependencyComponent.builder()
                .application(this)
                .dependencyModule(new AudioRecorderDependencyModule() {
                    @Override
                    public @NotNull UniqueIdGenerator providesUniqueIdGenerator() {
                        return applicationComponent.uniqueIdGenerator();
                    }
                })
                .build();

        projectsDependencyComponent = DaggerProjectsDependencyComponent.builder()
                .projectsDependencyModule(new CollectProjectsDependencyModule(applicationComponent))
                .build();

        selfieCameraDependencyComponent = DaggerSelfieCameraDependencyComponent.builder()
                .selfieCameraDependencyModule(new CollectSelfieCameraDependencyModule(applicationComponent))
                .build();

        drawDependencyComponent = DaggerDrawDependencyComponent.builder()
                .drawDependencyModule(new CollectDrawDependencyModule(applicationComponent))
                .build();

        // Mapbox dependencies
        objectProvider.addSupplier(SettingsProvider.class, applicationComponent::settingsProvider);
        objectProvider.addSupplier(NetworkStateProvider.class, applicationComponent::networkStateProvider);
        objectProvider.addSupplier(ReferenceLayerRepository.class, applicationComponent::referenceLayerRepository);
        objectProvider.addSupplier(LocationClient.class, applicationComponent::locationClient);
    }

    @NotNull
    @Override
    public AudioRecorderDependencyComponent getAudioRecorderDependencyComponent() {
        return audioRecorderDependencyComponent;
    }

    @NotNull
    @Override
    public ProjectsDependencyComponent getProjectsDependencyComponent() {
        return projectsDependencyComponent;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        //noinspection deprecation
        defaultSysLanguage = newConfig.locale.getLanguage();
    }

    @Nullable
    public AppDependencyComponent getComponent() {
        return applicationComponent;
    }

    public void setComponent(AppDependencyComponent applicationComponent) {
        this.applicationComponent = applicationComponent;
        applicationComponent.inject(this);
    }

    /**
     * Gets a unique, privacy-preserving identifier for a form based on its id and version.
     *
     * @param formId      id of a form
     * @param formVersion version of a form
     * @return md5 hash of the form title, a space, the form ID
     */
    public static String getFormIdentifierHash(String formId, String formVersion) {
        Form form = new FormsRepositoryProvider(Collect.getInstance()).create().getLatestByFormIdAndVersion(formId, formVersion);

        String formTitle = form != null ? form.getDisplayName() : "";

        String formIdentifier = formTitle + " " + formId;
        return Md5.getMd5Hash(new ByteArrayInputStream(formIdentifier.getBytes()));
    }

    // https://issuetracker.google.com/issues/154855417
    private void fixGoogleBug154855417() {
        try {
            Settings metaSharedPreferences = applicationComponent.settingsProvider().getMetaSettings();

            boolean hasFixedGoogleBug154855417 = metaSharedPreferences.getBoolean(KEY_GOOGLE_BUG_154855417_FIXED);

            if (!hasFixedGoogleBug154855417) {
                File corruptedZoomTables = new File(getFilesDir(), "ZoomTables.data");
                corruptedZoomTables.delete();

                metaSharedPreferences.save(KEY_GOOGLE_BUG_154855417_FIXED, true);
            }
        } catch (Exception ignored) {
            // ignored
        }
    }

    @NotNull
    @Override
    public Locale getLocale() {
        if (this.applicationComponent != null) {
            return LocaleHelper.getLocale(applicationComponent.settingsProvider().getUnprotectedSettings().getString(ProjectKeys.KEY_APP_LANGUAGE));
        } else {
            return getResources().getConfiguration().locale;
        }
    }

    @NotNull
    @Override
    public AppState getState() {
        return appState;
    }

    @NonNull
    @Override
    public GeoDependencyComponent getGeoDependencyComponent() {
        if (geoDependencyComponent == null) {
            geoDependencyComponent = DaggerGeoDependencyComponent.builder()
                    .application(this)
                    .geoDependencyModule(new CollectGeoDependencyModule(applicationComponent))
                    .build();
        }

        return geoDependencyComponent;
    }

    @NonNull
    @Override
    public OsmDroidDependencyComponent getOsmDroidDependencyComponent() {
        if (osmDroidDependencyComponent == null) {
            osmDroidDependencyComponent = DaggerOsmDroidDependencyComponent.builder()
                    .osmDroidDependencyModule(new CollectOsmDroidDependencyModule(applicationComponent))
                    .build();
        }

        return osmDroidDependencyComponent;
    }

    @NonNull
    @Override
    public ObjectProvider getObjectProvider() {
        return objectProvider;
    }

    @NonNull
    @Override
    public EntitiesDependencyComponent getEntitiesDependencyComponent() {
        if (entitiesDependencyComponent == null) {
            entitiesDependencyComponent = DaggerEntitiesDependencyComponent.builder()
                    .entitiesDependencyModule(new EntitiesDependencyModule() {
                        @NonNull
                        @Override
                        public EntitiesRepository providesEntitiesRepository() {
                            String projectId = applicationComponent.currentProjectProvider().requireCurrentProject().getUuid();
                            return applicationComponent.entitiesRepositoryProvider().create(projectId);
                        }

                        @NonNull
                        @Override
                        public Scheduler providesScheduler() {
                            return applicationComponent.scheduler();
                        }
                    })
                    .build();
        }

        return entitiesDependencyComponent;
    }

    @NonNull
    @Override
    public SelfieCameraDependencyComponent getSelfieCameraDependencyComponent() {
        return selfieCameraDependencyComponent;
    }

    @NonNull
    @Override
    public GoogleMapsDependencyComponent getGoogleMapsDependencyComponent() {
        if (googleMapsDependencyComponent == null) {
            googleMapsDependencyComponent = DaggerGoogleMapsDependencyComponent.builder()
                    .googleMapsDependencyModule(new CollectGoogleMapsDependencyModule(applicationComponent))
                    .build();
        }

        return googleMapsDependencyComponent;
    }

    @NonNull
    @Override
    public DrawDependencyComponent getDrawDependencyComponent() {
        return drawDependencyComponent;
    }

    @Override
    public @NotNull LocationDependencyComponent getLocationDependencyComponent() {
        return DaggerLocationDependencyComponent.builder()
                .locationDependencyModule(new LocationDependencyModule() {
                    @Override
                    public @NotNull UniqueIdGenerator providesUniqueIdGenerator() {
                        return applicationComponent.uniqueIdGenerator();
                    }
                })
                .build();
    }

    // Begin Smap-specific methods
    public void setFormId(String v) {
        formId = v;
    }

    public String getFormId() {
        return formId;
    }

    public void setSearchLocalData(String v) {
        searchLocalData = v;
    }

    public String getSearchLocalData() {
        return searchLocalData;
    }

    public void setLocation(Location l) {
        location = l;
    }

    public Location getLocation() {
        return location;
    }

    public void setSavedLocation(Location l) {
        savedLocation = l;
    }

    public Location getSavedLocation() {
        return savedLocation;
    }

    public void setGeofences(ArrayList<GeofenceEntry> geofences) {
        this.geofences = geofences;
    }

    public ArrayList<GeofenceEntry> getGeofences() {
        return geofences;
    }

    public void setDownloading(boolean v) {
        tasksDownloading = v;
    }

    public boolean isDownloading() {
        return tasksDownloading;
    }

    // Set form filling activity
    public void setFormFillingActivity(org.odk.collect.android.activities.FormFillingActivity activity) {
        formFillingActivity = activity;
    }

    public org.odk.collect.android.activities.FormFillingActivity getFormFillingActivity() {
        return formFillingActivity;
    }

    public void clearRemoteServiceCaches() {
        remoteCache = new HashMap<String, SmapRemoteDataItem>();
    }

    public void initRemoteServiceCaches() {
        if (remoteCache == null) {
            remoteCache = new HashMap<String, SmapRemoteDataItem>();
        } else {
            ArrayList<String> expired = new ArrayList<String>();
            for (String key : remoteCache.keySet()) {
                SmapRemoteDataItem item = remoteCache.get(key);
                if (item.perSubmission) {
                    expired.add(key);
                }
            }
            if (expired.size() > 0) {
                for (String key : expired) {
                    remoteCache.remove(key);
                }
            }
        }
        remoteCalls = 0;
    }

    public String getRemoteData(String key) {
        SmapRemoteDataItem item = remoteCache.get(key);
        if (item != null) {
            return item.data;
        } else {
            return null;
        }
    }

    public void setRemoteItem(SmapRemoteDataItem item) {
        if (item.data == null) {
            // There was a network error
            remoteCache.remove(item.key);
        } else {
            remoteCache.put(item.key, item);
        }
    }

    public void startRemoteCall() {
        remoteCalls++;
    }

    public void endRemoteCall() {
        remoteCalls--;
    }

    public boolean inRemoteCall() {
        return remoteCalls > 0;
    }

    public void setFormRestartDetails(FormRestartDetails restartDetails) {
        mRestartDetails = restartDetails;
    }

    public FormRestartDetails getFormRestartDetails() {
        return mRestartDetails;
    }

    /*
     * Push a FormLaunchDetail to the stack
     * this form should then be launched by SmapMain
     */
    public void pushToFormStack(FormLaunchDetail fld) {
        formStack.push(fld);
    }

    /*
     * Pop a FormLaunchDetails from the stack
     */
    public FormLaunchDetail popFromFormStack() {
        if (formStack.empty()) {
            return null;
        } else {
            return formStack.pop();
        }
    }

    public void clearCompoundAddresses() {
        compoundAddresses = new HashMap<String, String>();
    }

    public void putCompoundAddress(String qName, String address) {
        compoundAddresses.put(qName, address);
    }

    public String getCompoundAddress(String qName) {
        return compoundAddresses.get(qName);
    }
    // End Smap-specific methods

    /**
     * Predicate that tests whether a directory path might refer to an
     * ODK Tables instance data directory (e.g., for media attachments).
     */
    public static boolean isODKTablesInstanceDataDirectory(File directory) {
        /*
         * Special check to prevent deletion of files that
         * could be in use by ODK Tables.
         */
        String dirPath = directory.getAbsolutePath();
        StoragePathProvider storagePathProvider = new StoragePathProvider();
        if (dirPath.startsWith(storagePathProvider.getStorageRootDirPath())) {
            dirPath = dirPath.substring(storagePathProvider.getStorageRootDirPath().length());
            String[] parts = dirPath.split(File.separatorChar == '\\' ? "\\\\" : File.separator);
            // [appName, instances, tableId, instanceId ]
            if (parts.length == 4 && parts[1].equals("instances")) {
                return true;
            }
        }
        return false;
    }
}
