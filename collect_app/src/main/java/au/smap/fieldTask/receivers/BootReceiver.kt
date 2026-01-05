package au.smap.fieldTask.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.preference.PreferenceManager
import au.smap.fieldTask.services.LocationService
import org.odk.collect.settings.keys.ProjectKeys
import timber.log.Timber

/**
 * BroadcastReceiver that restarts LocationService after device reboot.
 * Ensures location tracking continues if user has enabled it.
 *
 * smap - New for fieldTask5 to fix missing service restart
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Timber.i("Boot completed, checking if location service should start")

            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            val locationEnabled = sharedPreferences.getBoolean(ProjectKeys.KEY_SMAP_ENABLE_GEOFENCE, false)

            if (locationEnabled) {
                Timber.i("Location tracking enabled, starting LocationService")
                startLocationService(context)
            } else {
                Timber.i("Location tracking disabled, not starting LocationService")
            }
        }
    }

    /**
     * Start LocationService with proper handling for Android O+ (API 26+)
     */
    private fun startLocationService(context: Context) {
        val serviceIntent = Intent(context, LocationService::class.java)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Android O+ requires startForegroundService
                context.startForegroundService(serviceIntent)
                Timber.i("Started LocationService as foreground service")
            } else {
                context.startService(serviceIntent)
                Timber.i("Started LocationService")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to start LocationService after boot")
        }
    }
}
