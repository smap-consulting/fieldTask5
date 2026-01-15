package org.odk.collect.android.smap.utilities;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Build;

import org.odk.collect.android.R;
import au.smap.fieldTask.activities.SmapMain;
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.injection.DaggerUtils;
import au.smap.fieldTask.database.TraceUtilities;
import org.odk.collect.permissions.PermissionListener;
import org.odk.collect.permissions.PermissionsProvider;
import org.odk.collect.android.smap.tasks.SubmitLocationTask;
import org.odk.collect.settings.keys.ProjectKeys;
import org.odk.collect.shared.settings.Settings;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import timber.log.Timber;

/*
 * location Register
 * Records locations in a trace db file and automatically submits to the server in real time
 */
public class LocationRegister {

    public boolean locationEnabled() {
        return true;
    }

    public boolean taskLocationEnabled() {
        return true;
    }

    public void register(Context context, Location location) {
        Settings settings = DaggerUtils.getComponent(context).settingsProvider().getUnprotectedSettings();
        if (settings.getBoolean(ProjectKeys.KEY_SMAP_USER_LOCATION)) {

            // Save trace
            TraceUtilities.insertPoint(location);
            Timber.i("+++++ Insert Point");
            LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent("locationChanged"));  // update map

            // Attempt to send current location and trace immediately
            String server = settings.getString(ProjectKeys.KEY_SERVER_URL);
            String latString = String.valueOf(location.getLatitude());
            String lonString = String.valueOf(location.getLongitude());
            SubmitLocationTask task = new SubmitLocationTask();
            task.execute(server, latString, lonString);
        }
    }

    public int getMessageId() {
        return org.odk.collect.android.R.string.smap_request_foreground_location_permission;
    }

    public void set(org.odk.collect.shared.settings.Settings settings, String sendLocation) {
        /*
         * SAVE_LOCATION is used to store the setting so that it can be restored if overriden by EXIT
         */
        if(sendLocation == null || sendLocation.equals("off")) {
            settings.save(ProjectKeys.KEY_SMAP_USER_SAVE_LOCATION, false);
            settings.save(ProjectKeys.KEY_SMAP_USER_LOCATION, false);
            settings.save(ProjectKeys.KEY_SMAP_OVERRIDE_LOCATION, true);
        } else if(sendLocation.equals("on")) {
            settings.save(ProjectKeys.KEY_SMAP_USER_SAVE_LOCATION, true);
            settings.save(ProjectKeys.KEY_SMAP_USER_LOCATION, true);
            settings.save(ProjectKeys.KEY_SMAP_OVERRIDE_LOCATION, true);
        } else {
            settings.save(ProjectKeys.KEY_SMAP_OVERRIDE_LOCATION, false);
        }
    }

    public void locationStart(Activity currentActivity, PermissionsProvider permissionsProvider) {
        permissionsProvider.requestEnabledLocationPermissions(currentActivity, new PermissionListener() {
            @Override
            public void granted() {

                requestBackgroundLocationPermissions(currentActivity, new PermissionListener() {
                    @Override
                    public void granted() {
                        ((SmapMain) currentActivity).startLocationService();
                    }

                    @Override
                    public void denied() {
                        ((SmapMain) currentActivity).startLocationService();     // Start the service anyway it will only work when the app is in the foreground
                    }
                }, permissionsProvider);

            }

            @Override
            public void denied() {
            }
        });
    }

    private void requestBackgroundLocationPermissions(Activity activity, @NonNull PermissionListener action,
                                                      PermissionsProvider permissionsProvider) {
        if (Build.VERSION.SDK_INT >=  Build.VERSION_CODES.Q) {  // ACCESS_BACKGROUND_LOCATION added in API 29
            permissionsProvider.requestPermissions(activity, new PermissionListener() {
                @Override
                public void granted() {
                    action.granted();
                }

                @Override
                public void denied() {
                    // smap: showAdditionalExplanation is no longer accessible directly in fieldTask5
                    // Background location permission was denied, but we still grant the action
                    // since foreground location is sufficient
                    action.denied();
                }
            }, Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        } else {
            action.granted();
        }
    }

    // Check that the installation is good and not on a rooted device
    public void isValidInstallation(Context context) {
    }

    // Force the use of tokens to logon
    public static boolean defaultForceToken() {
        return false;
    }
}
