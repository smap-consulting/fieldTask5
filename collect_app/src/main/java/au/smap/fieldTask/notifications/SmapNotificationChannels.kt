package au.smap.fieldTask.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import timber.log.Timber

/**
 * Notification channels for Smap-specific notifications.
 * Android 8.0 (API 26)+ requires notification channels.
 *
 * smap - Manages notification channels for task updates and location tracking
 */
object SmapNotificationChannels {

    /**
     * Channel for task update notifications from server (FCM)
     */
    const val TASK_UPDATES_CHANNEL_ID = "smap_task_updates"

    /**
     * Channel for location tracking foreground service notification
     */
    const val LOCATION_TRACKING_CHANNEL_ID = "smap_location_tracking"

    /**
     * Create all Smap notification channels.
     * Safe to call on all API levels - checks Build.VERSION internally.
     *
     * @param context Application context
     */
    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createTaskUpdatesChannel(context)
            createLocationTrackingChannel(context)
            Timber.i("Smap notification channels created")
        } else {
            Timber.d("Notification channels not required for API < 26")
        }
    }

    /**
     * Create channel for task update notifications from server.
     * Default importance - shows in notification drawer with sound/vibration.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createTaskUpdatesChannel(context: Context) {
        val channel = NotificationChannel(
            TASK_UPDATES_CHANNEL_ID,
            "Task Updates",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications when new tasks are available from the server"
            enableVibration(true)
            setShowBadge(true)
        }

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager?.createNotificationChannel(channel)
        Timber.d("Created task updates notification channel")
    }

    /**
     * Create channel for location tracking foreground service.
     * Low importance - shows in notification drawer without sound/vibration.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createLocationTrackingChannel(context: Context) {
        val channel = NotificationChannel(
            LOCATION_TRACKING_CHANNEL_ID,
            "Location Tracking",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows when location tracking is active"
            enableVibration(false)
            setShowBadge(false)
        }

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager?.createNotificationChannel(channel)
        Timber.d("Created location tracking notification channel")
    }
}
