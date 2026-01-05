package au.smap.fieldTask.tasks

import au.smap.fieldTask.aws.services.DeviceRegistrationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Background task for registering device FCM token with AWS DynamoDB.
 *
 * smap - Migrated from AsyncTask (deprecated) to Kotlin coroutines with DeviceRegistrationService.
 * Uses AWS SDK v3 instead of v2.
 */
class SmapRegisterForMessagingTask @Inject constructor(
    private val deviceRegistrationService: DeviceRegistrationService
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Register device FCM token with DynamoDB.
     * Launches coroutine in background scope.
     *
     * @param token Firebase Cloud Messaging registration token
     * @param server Smap server URL
     * @param username User identifier
     */
    fun execute(token: String, server: String, username: String) {
        Timber.i("================================================== Notifying server of messaging update")
        Timber.i("    token: %s", token)
        Timber.i("    server: %s", server)
        Timber.i("    user: %s", username)

        scope.launch {
            try {
                val result = deviceRegistrationService.registerDevice(token, server, username)

                result.onSuccess {
                    Timber.i("================================================== Notifying server of messaging update done")
                }.onFailure { error ->
                    Timber.e(error, "Failed to register device for messaging")
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception during device registration")
            }
        }
    }

    /**
     * Suspend function version for use within coroutines.
     * Returns Result for better error handling.
     *
     * @param token Firebase Cloud Messaging registration token
     * @param server Smap server URL
     * @param username User identifier
     * @return Result.success if registered, Result.failure on error
     */
    suspend fun registerDevice(token: String, server: String, username: String): Result<Unit> {
        Timber.i("================================================== Notifying server of messaging update (suspend)")
        Timber.i("    token: %s", token)
        Timber.i("    server: %s", server)
        Timber.i("    user: %s", username)

        val result = deviceRegistrationService.registerDevice(token, server, username)

        result.onSuccess {
            Timber.i("================================================== Notifying server of messaging update done")
        }.onFailure { error ->
            Timber.e(error, "Failed to register device for messaging")
        }

        return result
    }
}
