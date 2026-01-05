package au.smap.fieldTask.utilities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.annotation.RequiresApi
import timber.log.Timber

/**
 * Helper for managing battery optimization settings.
 * Ensures LocationService can run reliably in the background.
 *
 * smap - New for fieldTask5 to improve background service reliability
 */
object BatteryOptimizationHelper {

    /**
     * Check if the app is ignoring battery optimizations.
     * Apps that ignore battery optimizations can run more reliably in the background.
     *
     * @param context Application context
     * @return true if ignoring battery optimizations, false otherwise
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val packageName = context.packageName
            val isIgnoring = pm?.isIgnoringBatteryOptimizations(packageName) ?: false

            Timber.d("Battery optimization status: ignoring=$isIgnoring")
            return isIgnoring
        }

        // Battery optimization doesn't exist before Android M
        Timber.d("Battery optimization not applicable for API < 23")
        return true
    }

    /**
     * Request battery optimization exemption from the user.
     * Opens system settings where user can whitelist the app.
     *
     * IMPORTANT: Only call this when user has explicitly enabled location tracking.
     * Don't spam users with this request.
     *
     * @param activity Activity to launch settings from
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun requestBatteryOptimizationExemption(activity: Activity) {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${activity.packageName}")
            }

            if (intent.resolveActivity(activity.packageManager) != null) {
                Timber.i("Requesting battery optimization exemption")
                activity.startActivity(intent)
            } else {
                Timber.w("No activity found to handle battery optimization request")
                // Fallback: open battery optimization settings list
                openBatteryOptimizationSettings(activity)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to request battery optimization exemption")
            // Fallback: try opening settings
            openBatteryOptimizationSettings(activity)
        }
    }

    /**
     * Open battery optimization settings (shows list of all apps).
     * Fallback when direct exemption request fails.
     *
     * @param activity Activity to launch settings from
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun openBatteryOptimizationSettings(activity: Activity) {
        try {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)

            if (intent.resolveActivity(activity.packageManager) != null) {
                Timber.i("Opening battery optimization settings")
                activity.startActivity(intent)
            } else {
                Timber.w("No activity found to handle battery optimization settings")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to open battery optimization settings")
        }
    }

    /**
     * Check if battery optimization exemption request should be shown.
     * Only show if:
     * 1. Android M+ (battery optimization exists)
     * 2. App is not already ignoring battery optimizations
     * 3. Location tracking is enabled
     *
     * @param context Application context
     * @param locationTrackingEnabled Whether user has enabled location tracking
     * @return true if should show exemption request
     */
    fun shouldRequestBatteryOptimizationExemption(
        context: Context,
        locationTrackingEnabled: Boolean
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return false
        }

        if (!locationTrackingEnabled) {
            return false
        }

        val isIgnoring = isIgnoringBatteryOptimizations(context)
        val shouldRequest = !isIgnoring

        Timber.d("Should request battery optimization exemption: $shouldRequest")
        return shouldRequest
    }
}
