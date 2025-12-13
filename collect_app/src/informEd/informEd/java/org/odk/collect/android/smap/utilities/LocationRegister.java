package org.odk.collect.android.smap.utilities;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.preference.PreferenceManager;

import org.odk.collect.android.database.TraceUtilities;
import org.odk.collect.settings.keys.ProjectKeys;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import timber.log.Timber;

public class LocationRegister {
    public void register(Context context, Location location) {
        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(ProjectKeys.KEY_SMAP_USER_LOCATION, false)) {
            TraceUtilities.insertPoint(location);
            Timber.i("+++++ Insert Point");
            LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent("locationChanged"));  // update map
        }
    }
}
