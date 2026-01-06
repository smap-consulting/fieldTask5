/*
 * Copyright (C) 2017 Smap Consulting Pty Ltd
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

package au.smap.fieldTask.services

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.core.app.NotificationCompat
import au.smap.fieldTask.activities.NotificationActivity
import au.smap.fieldTask.tasks.DownloadTasksTask
import au.smap.fieldTask.utilities.Utilities
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.odk.collect.android.R
import org.odk.collect.android.application.Collect
import org.odk.collect.android.injection.DaggerUtils
import org.odk.collect.android.notifications.NotificationManagerNotifier
import org.odk.collect.settings.keys.ProjectKeys
import timber.log.Timber
import javax.inject.Inject

/**
 * Firebase Cloud Messaging service for receiving push notifications from server.
 * Responds to server notifications by downloading new tasks or showing notification.
 *
 * smap - Migrated from fieldTask4, updated for fieldTask5 architecture
 */
class NotificationService : FirebaseMessagingService() {

    @Inject
    lateinit var settingsProvider: org.odk.collect.settings.SettingsProvider

    override fun onCreate() {
        super.onCreate()
        DaggerUtils.getComponent(this).inject(this)
    }

    override fun onDeletedMessages() {
        // No action needed for deleted messages
    }

    /**
     * Called when message is received from Firebase Cloud Messaging.
     * Downloads new tasks if auto-send is enabled, otherwise shows notification.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        Timber.i("========================================")
        Timber.i("FCM MESSAGE RECEIVED!")
        Timber.i("From: ${message.from}")
        Timber.i("Data: ${message.data}")
        Timber.i("Notification: ${message.notification?.title} - ${message.notification?.body}")
        Timber.i("========================================")
        Timber.i("FCM message received, beginning refresh")

        // Make sure SD card is ready, if not don't try to send
        if (Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED) {
            Timber.w("External storage not mounted, skipping notification handling")
            return
        }

        // Check if auto-send is enabled (wifi or cellular) using fieldTask5 Settings architecture
        val settings = settingsProvider.getUnprotectedSettings()
        val autoSendOption = settings.getString(ProjectKeys.KEY_AUTOSEND) ?: "off"
        val isAutoSendEnabled = autoSendOption != "off"

        if (isAutoSendEnabled) {
            // Auto-send enabled: Refresh - download new tasks from server
            Timber.i("Auto-send enabled, downloading tasks")
            val downloadTasksTask = DownloadTasksTask()
            downloadTasksTask.doInBackground()
        } else {
            // Auto-send disabled: Show notification that server has changed
            Timber.i("Auto-send disabled, showing server changed notification")
            showServerChangedNotification()
        }
    }

    /**
     * Called when FCM token is refreshed.
     * Registers new token with server via DynamoDB.
     */
    override fun onNewToken(token: String) {
        Timber.i("========================================")
        Timber.i("NEW FCM TOKEN RECEIVED!")
        Timber.i("Token (first 30 chars): %s...", token.take(30))
        Timber.i("========================================")
        sendRegistrationToServer(token)
    }

    /**
     * Show notification that server has changed (when auto-send is disabled).
     */
    private fun showServerChangedNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create intent for when user taps notification
        val intent = Intent(this, NotificationActivity::class.java).apply {
            putExtra(NotificationActivity.NOTIFICATION_TITLE, getString(org.odk.collect.strings.R.string.app_name))
            putExtra(NotificationActivity.NOTIFICATION_MESSAGE, getString(R.string.smap_server_changed))
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(
            this,
            NotificationManagerNotifier.COLLECT_NOTIFICATION_CHANNEL
        )
            .setContentTitle(getString(org.odk.collect.strings.R.string.app_name))
            .setContentText(getString(R.string.smap_server_changed))
            .setSmallIcon(org.odk.collect.icons.R.drawable.ic_notification_small)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NotificationActivity.NOTIFICATION_ID, notification)
    }

    /**
     * Save FCM token to Settings and register with server.
     */
    private fun sendRegistrationToServer(token: String) {
        // Store the new token using fieldTask5 Settings architecture
        val settings = settingsProvider.getUnprotectedSettings()
        settings.save(ProjectKeys.KEY_SMAP_REGISTRATION_ID, token)

        Timber.i("FCM token saved to settings, triggering server registration")

        // Register with server via DynamoDB
        Utilities.updateServerRegistration(true)
    }
}
