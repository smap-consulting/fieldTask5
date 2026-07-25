package org.odk.collect.android.smap.utilities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.preference.PreferenceManager;

import org.odk.collect.android.database.TraceUtilities;
import org.odk.collect.settings.keys.ProjectKeys;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
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

    /*
     * Disable permissions concerned with background location
     */
    public void set(org.odk.collect.shared.settings.Settings settings, String sendLocation) {
        settings.save(ProjectKeys.KEY_SMAP_USER_LOCATION, false);
        settings.save(ProjectKeys.KEY_SMAP_OVERRIDE_LOCATION, true);
    }

    // Check that the installation is good
    public void isValidInstallation(Context context) {
    }

    // Return true if a numeric pulldata record index of 0 should list all matching
    // values.  Legacy behaviour, retained only for variants whose forms rely on it.
    public static boolean pulldataZeroIsList() {
        return false;
    }
}
