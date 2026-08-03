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
import androidx.work.BackoffPolicy
import org.odk.collect.async.Scheduler
import org.odk.collect.async.TaskSpec
import org.odk.collect.settings.SettingsProvider
import timber.log.Timber
import java.util.function.Supplier

/**
 * Downloads the offline map layers assigned to this user in the background.
 *
 * Scheduled with a wifi constraint so that large layer files are never pulled over mobile data
 * without the user asking.  Returning false leaves the work to be retried, and because the
 * downloader resumes from a part downloaded file nothing already transferred is lost.
 */
class OfflineLayerTaskSpec : TaskSpec {

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
            val downloader = OfflineLayerDownloader.create()
            val layers = OfflineLayerDownloader.getManifest(settingsProvider(context))
            downloader.downloadAll(layers) { isStopped() }
        }
    }

    override fun onException(exception: Throwable) {
        Timber.e(exception, "Offline layer download failed")
    }

    private fun settingsProvider(context: Context): SettingsProvider =
        org.odk.collect.android.injection.DaggerUtils.getComponent(context).settingsProvider()

    companion object {
        const val TAG = "offlineLayerDownload"

        /**
         * Ask for the assigned layers to be downloaded when the device next has wifi.  Any
         * previously scheduled request with the same tag is replaced.
         */
        @JvmStatic
        fun schedule(scheduler: Scheduler) {
            scheduler.networkDeferred(
                TAG,
                OfflineLayerTaskSpec(),
                emptyMap(),
                Scheduler.NetworkType.WIFI
            )
        }

        /**
         * Stop waiting for wifi, used when the server no longer assigns any layers
         */
        @JvmStatic
        fun cancel(scheduler: Scheduler) {
            scheduler.cancelDeferred(TAG)
        }
    }
}
