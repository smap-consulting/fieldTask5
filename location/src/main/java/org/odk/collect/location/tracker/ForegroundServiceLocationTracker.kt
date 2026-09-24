package org.odk.collect.location.tracker

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.StateFlow
import org.odk.collect.androidshared.data.getState
import org.odk.collect.androidshared.ui.ReturnToAppActivity
import org.odk.collect.androidshared.utils.UniqueIdGenerator
import org.odk.collect.location.Location
import org.odk.collect.location.LocationClient
import org.odk.collect.location.LocationClientProvider
import org.odk.collect.location.LocationDependencyComponentProvider
import org.odk.collect.strings.localization.getLocalizedString
import timber.log.Timber
import javax.inject.Inject

private const val LOCATION_KEY = "location"

class ForegroundServiceLocationTracker(private val application: Application) : LocationTracker {

    override fun getLocation(): StateFlow<Location?> {
        return application.getState().getFlow(LOCATION_KEY, null)
    }

    override fun start(retainMockAccuracy: Boolean, updateInterval: Long?, notification: Boolean) {
        val intent = Intent(application, LocationTrackerService::class.java).also { intent ->
            intent.putExtra(LocationTrackerService.EXTRA_RETAIN_MOCK_ACCURACY, retainMockAccuracy)
            intent.putExtra(LocationTrackerService.EXTRA_NOTIFICATION, notification)
            updateInterval?.let {
                intent.putExtra(LocationTrackerService.EXTRA_UPDATE_INTERVAL, it)
            }
        }

        // smap - onResume can run while the OS still sees the app as background (e.g. behind
        // the lock screen), and API 31+ then throws BackgroundServiceStartNotAllowedException
        try {
            if (notification) {
                application.startForegroundService(intent)
            } else {
                application.startService(intent)
            }
        } catch (e: IllegalStateException) {
            Timber.w(e, "Location tracker service not started: app in background")
        }
    }

    override fun stop() {
        application.stopService(Intent(application, LocationTrackerService::class.java))
    }
}

class LocationTrackerService : Service(), LocationClient.LocationClientListener {

    @Inject
    lateinit var uniqueIdGenerator: UniqueIdGenerator

    private val locationClient: LocationClient by lazy {
        LocationClientProvider.getClient(application)
    }

    override fun onCreate() {
        super.onCreate()
        val provider = applicationContext as LocationDependencyComponentProvider
        provider.locationDependencyComponent.inject(this)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // smap - runtime location permission must be held before startForeground() with
        // FOREGROUND_SERVICE_TYPE_LOCATION.  On API 34+ that call throws SecurityException
        // without it, so fall back to the 2-arg form and stop if the permission is missing.
        val hasLocationPermission =
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (intent?.getBooleanExtra(EXTRA_NOTIFICATION, true) != false) {
            setupNotificationChannel()

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && hasLocationPermission) {
                    startForeground(uniqueIdGenerator.getInt(NOTIFICATION_IDENTIFIER), createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
                } else {
                    startForeground(uniqueIdGenerator.getInt(NOTIFICATION_IDENTIFIER), createNotification())
                }
            } catch (e: Exception) { // smap
                stopSelf()
                return START_NOT_STICKY
            }
        }

        if (!hasLocationPermission) { // smap
            stopSelf()
            return START_NOT_STICKY
        }

        locationClient.setRetainMockAccuracy(
            intent?.getBooleanExtra(
                EXTRA_RETAIN_MOCK_ACCURACY,
                false
            ) ?: false
        )

        if (intent?.hasExtra(EXTRA_UPDATE_INTERVAL) == true) {
            val interval = intent.getLongExtra(EXTRA_UPDATE_INTERVAL, -1)
            locationClient.setUpdateInterval(interval)
        }

        locationClient.start(this)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        locationClient.stop()
        application.getState().setFlow(LOCATION_KEY, null)
    }

    override fun onClientStart() {
        locationClient.requestLocationUpdates {
            application.getState().setFlow(
                LOCATION_KEY,
                Location(it.latitude, it.longitude, it.altitude, it.accuracy)
            )
        }
    }

    override fun onClientStartFailure() {
        // Ignored
    }

    override fun onClientStop() {
        // Ignored
    }

    private fun createNotification(): Notification {
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL)
            .setSmallIcon(org.odk.collect.icons.R.drawable.ic_notification_small)
            .setContentTitle(getLocalizedString(org.odk.collect.strings.R.string.location_tracking_notification_title))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(createNotificationIntent())

        return notification
            .build()
    }

    private fun createNotificationIntent() =
        PendingIntent.getActivity(
            this,
            0,
            Intent(this, ReturnToAppActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

    private fun setupNotificationChannel() {
        val notificationChannel = NotificationChannel(
            NOTIFICATION_CHANNEL,
            getLocalizedString(org.odk.collect.strings.R.string.location_tracking_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )

        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
            notificationChannel
        )
    }

    companion object {
        const val EXTRA_RETAIN_MOCK_ACCURACY = "retain_mock_accuracy"
        const val EXTRA_UPDATE_INTERVAL = "update_interval"
        const val EXTRA_NOTIFICATION = "notification"

        private const val NOTIFICATION_IDENTIFIER = "location_tracking"
        private const val NOTIFICATION_CHANNEL = "location_tracking"
    }
}
