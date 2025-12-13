package org.odk.collect.android.smap.utilities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.preference.PreferenceManager;

import org.odk.collect.android.application.Collect;
import org.odk.collect.android.database.TraceUtilities;
import org.odk.collect.settings.keys.ProjectKeys;
import org.odk.collect.android.smap.tasks.SubmitLocationTask;

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
        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(ProjectKeys.KEY_SMAP_USER_LOCATION, false)) {

            // Save trace
            TraceUtilities.insertPoint(location);
            Timber.i("+++++ Insert Point");
            LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent("locationChanged"));  // update map

            // Attempt to send current location and trace immediately
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(Collect.getInstance());
            String server = sharedPreferences.getString(ProjectKeys.KEY_SERVER_URL, "");
            String latString = String.valueOf(location.getLatitude());
            String lonString = String.valueOf(location.getLongitude());
            SubmitLocationTask task = new SubmitLocationTask();
            task.execute(server, latString, lonString);
        }
    }

    public void set(SharedPreferences.Editor editor, String sendLocation) {
        /*
         * SAVE_LOCATION is used to store the setting so that it can be restored if overriden by EXIT
         */
        if(sendLocation == null || sendLocation.equals("off")) {
            editor.putBoolean(ProjectKeys.KEY_SMAP_USER_SAVE_LOCATION, false);
            editor.putBoolean(ProjectKeys.KEY_SMAP_USER_LOCATION, false);
            editor.putBoolean(ProjectKeys.KEY_SMAP_OVERRIDE_LOCATION, true);
        } else if(sendLocation.equals("on")) {
            editor.putBoolean(ProjectKeys.KEY_SMAP_USER_SAVE_LOCATION, true);
            editor.putBoolean(ProjectKeys.KEY_SMAP_USER_LOCATION, true);
            editor.putBoolean(ProjectKeys.KEY_SMAP_OVERRIDE_LOCATION, true);
        } else {
            editor.putBoolean(ProjectKeys.KEY_SMAP_OVERRIDE_LOCATION, false);
        }
    }
}
