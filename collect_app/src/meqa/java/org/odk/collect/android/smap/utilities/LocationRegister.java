package org.odk.collect.android.smap.utilities;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;

import org.odk.collect.android.BuildConfig;
import org.odk.collect.android.R;
import org.odk.collect.android.fragments.SmapTaskMapFragment;
import org.odk.collect.android.listeners.PermissionListener;

import au.smap.fieldTask.activities.SmapMain;
import au.smap.fieldTask.permissions.PermissionsProvider;
import org.odk.collect.settings.keys.ProjectKeys;

import java.io.File;
import java.io.IOException;

import timber.log.Timber;

public class LocationRegister {

    public boolean locationEnabled() {
        return false;
    }

    public boolean taskLocationEnabled() {
        return false;
    }

    public void register(Context context, Location location) {
        // Do nothing
    }

    public int getMessageId() {
        return org.odk.collect.android.R.string.smap_request_foreground_location_permission;
    }
    /*
     * Disable permissions concerned with background location
     */
    public void set(SharedPreferences.Editor editor, String sendLocation) {
        editor.putBoolean(ProjectKeys.KEY_SMAP_USER_LOCATION, false);
        editor.putBoolean(ProjectKeys.KEY_SMAP_OVERRIDE_LOCATION, true);
    }

    // Start foreground location recording
    public void locationStart(Activity currentActivity, PermissionsProvider permissionsProvider) {
        permissionsProvider.requestLocationPermissions(currentActivity, new PermissionListener() {
            @Override
            public void granted() {
                ((SmapMain) currentActivity).startLocationService();
            }

            @Override
            public void denied() {
            }
        });
    }

    // Check that the installation is good
    public void isValidInstallation(Context context) throws Exception {

        /*
         * Look for evidence of rooting on the file path
         */
        File suApk = new File("/system/app/Superuser.apk");
        File suBin = new File("/system/bin/su");
        File suBin2 = new File("/system/binx/su");

        if(suApk.exists() || suBin.exists() || suBin2.exists()) {
            throw new Exception(context.getString(R.string.smap_compromised));
        }

        /*
         * Attempt to execute a super user command
         */
        Process p =  null;
        try {
            p = Runtime.getRuntime().exec("su");
            if(p != null ) {
                throw new Exception(context.getString(R.string.smap_compromised));
            }
        } catch(IOException e) {
            // OK
        } finally {
            if(p != null) {
                try {
                    p.destroy();
                } catch (Exception e) {
                    Timber.e(e);
                }
            }
        }

        /*
         * Check for the busy box package
         */
        String bbName = "stericson.busybox";
        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo pi = pm.getPackageInfo(bbName, PackageManager.GET_ACTIVITIES);
            if(pi != null ) {
                throw new Exception(context.getString(R.string.smap_compromised));
            }
        } catch(Exception e) {
            // OK
        }

        /*
         * Check for running on an emulator
         */
        if (!BuildConfig.BUILD_TYPE.equals("debug")) {

            if(Build.FINGERPRINT.startsWith("google/sdk_gphone_")
                    && Build.FINGERPRINT.endsWith(":user/release-keys")
                    && Build.MANUFACTURER.equals("Google") && Build.PRODUCT.startsWith("sdk_gphone") && Build.BRAND.equals("google")
                    && Build.MODEL.startsWith("sdk_gphone")
                    || Build.FINGERPRINT.startsWith("generic")
                    || Build.FINGERPRINT.startsWith("unknown")
                    || Build.HARDWARE.contains("goldfish")
                    || Build.HARDWARE.contains("ranchu")
                    || Build.MODEL.contains("google_sdk")
                    || Build.MODEL.contains("Emulator")
                    || Build.MODEL.contains("Android SDK built for x86")
                    || Build.MANUFACTURER.contains("Genymotion")
                    || Build.HOST.equals("Build2") //MSI App Player
                    || Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")
                    || Build.PRODUCT.contains("sdk_google")
                    || Build.PRODUCT.equals("google_sdk")
                    || Build.PRODUCT.contains("sdk")
                    || Build.PRODUCT.contains("sdk_x86")
                    || Build.PRODUCT.contains("vbox86p")
                    || Build.PRODUCT.contains("emulator")
                    || Build.PRODUCT.contains("simulator")) {
                throw new Exception(context.getString(R.string.smap_emulator));
            }
        }
    }

    public static boolean defaultForceToken() {
        return false;   // For google play
        //return true;  // For download
    }
}
