/*
 * Copyright (C) 2026 Smap Consulting Pty Ltd
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
package au.smap.fieldTask.tasks

import android.content.Context
import android.os.Environment
import androidx.work.BackoffPolicy
import org.odk.collect.async.Scheduler
import org.odk.collect.async.TaskSpec
import timber.log.Timber
import java.util.function.Supplier

/**
 * Refreshes tasks and forms from the server, off the thread the server notification arrived on.
 *
 * A refresh downloads assignments, forms, manifests and media, which on a slow connection takes
 * longer than the twenty seconds or so Firebase allows onMessageReceived before it may stop the
 * process.  A device woken out of doze, which is now the common case, has the least time of all.
 * Run as scheduled work instead, so the sync gets a full execution window and survives the
 * process being killed.
 *
 * Scheduled by tag, so several notifications arriving together collapse into one refresh in the
 * same way Firebase collapses the messages themselves.
 */
class RefreshTaskSpec : TaskSpec {

    /*
     * The refresh reports its own outcome through notifications and reads whatever the server
     * holds at the time it runs, so there is nothing a retry of this particular request would
     * recover that the next one will not.
     */
    override val maxRetries: Int? = null
    override val backoffPolicy: BackoffPolicy = BackoffPolicy.EXPONENTIAL
    override val backoffDelay: Long = 60_000

    override fun getTask(
        context: Context,
        inputData: Map<String, String>,
        isLastUniqueExecution: Boolean,
        isStopped: (() -> Boolean)
    ): Supplier<Boolean> {
        return Supplier {
            // Checked here rather than when the notification arrived, as that may be a while ago
            if (Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED) {
                Timber.w("External storage not mounted, skipping the refresh")
                return@Supplier true
            }

            Timber.i("Refreshing after a server notification")
            DownloadTasksTask().doInBackground()
            /*
             * Always complete.  doInBackground returns null when a refresh is already running,
             * which is not a failure as that refresh fetches the same data, and it handles its
             * own errors internally.  Returning false here would requeue for ever.
             */
            true
        }
    }

    override fun onException(exception: Throwable) {
        Timber.e(exception, "Refresh after a server notification failed")
    }

    companion object {
        const val TAG = "serverNotificationRefresh"

        /**
         * Ask for a refresh as soon as there is a network.  Any request already waiting under
         * the same tag is replaced, so a burst of notifications causes one refresh.
         */
        @JvmStatic
        fun schedule(scheduler: Scheduler) {
            scheduler.networkDeferred(TAG, RefreshTaskSpec(), emptyMap())
        }
    }
}
