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

import android.os.Environment
import androidx.preference.PreferenceManager
import au.smap.fieldTask.notifications.SmapNotificationChannels
import au.smap.fieldTask.tasks.DownloadTasksTask
import au.smap.fieldTask.utilities.Utilities
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.odk.collect.android.R
import org.odk.collect.android.activities.NotificationActivity
import org.odk.collect.android.injection.DaggerUtils
import org.odk.collect.android.notifications.Notifier
import org.odk.collect.android.preferences.keys.ProjectKeys
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
    lateinit var notifier: Notifier

    override fun onDeletedMessages() {
        // No action needed for deleted messages
    }

    private fun deferDaggerInit() {
        DaggerUtils.getComponent(applicationContext).inject(this)
    }

    /**
     * Called when message is received from Firebase Cloud Messaging.
     * Downloads new tasks if auto-send is enabled, otherwise shows notification.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        Timber.i("Message received beginning refresh")
        deferDaggerInit()

        // Make sure SD card is ready, if not don't try to send
        if (Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED) {
            Timber.w("External storage not mounted, skipping notification handling")
            return
        }

        var automaticNotification = false
        if (Utilities.isFormAutoSendOptionEnabled()) {
            // Refresh - download new tasks from server
            Timber.i("Auto-send enabled, downloading tasks")
            val downloadTasksTask = DownloadTasksTask()
            downloadTasksTask.doInBackground()

            automaticNotification = true
        }

        // Show notification if automatic download not performed
        if (!automaticNotification) {
            Timber.i("Showing server changed notification")
            notifier.showNotification(
                null,
                NotificationActivity.NOTIFICATION_ID,
                R.string.app_name,
                getString(R.string.smap_server_changed),
                false
            )
        }
    }

    /**
     * Called when FCM token is refreshed.
     * Registers new token with server via DynamoDB.
     */
    override fun onNewToken(token: String) {
        Timber.i("Refreshed FCM token: %s", token)
        sendRegistrationToServer(token)
    }

    /**
     * Save FCM token to SharedPreferences and register with server.
     */
    private fun sendRegistrationToServer(token: String) {
        // Store the new token
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        sharedPreferences.edit().apply {
            putString(ProjectKeys.KEY_SMAP_REGISTRATION_ID, token)
            apply()
        }

        // Register with server via DynamoDB
        Utilities.updateServerRegistration(true)
    }
}
