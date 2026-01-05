package au.smap.fieldTask.aws.services

import au.smap.fieldTask.aws.dynamodb.DeviceRepository
import au.smap.fieldTask.aws.dynamodb.DevicesDO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for registering device FCM tokens with AWS DynamoDB.
 * Provides high-level API for device registration operations.
 *
 * smap - Replaces fieldTask4's SmapRegisterForMessagingTask AsyncTask with coroutines
 */
@Singleton
class DeviceRegistrationService @Inject constructor(
    private val deviceRepository: DeviceRepository
) {

    /**
     * Register device FCM token with DynamoDB.
     * This associates the Firebase Cloud Messaging token with the user and server.
     *
     * @param token Firebase Cloud Messaging registration token
     * @param server Smap server URL
     * @param username User identifier (username)
     * @return Result.success if registered, Result.failure on error
     */
    suspend fun registerDevice(
        token: String,
        server: String,
        username: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        Timber.i("Registering device: server=%s, user=%s", server, username)

        if (token.isBlank()) {
            Timber.e("Cannot register device: FCM token is blank")
            return@withContext Result.failure(IllegalArgumentException("FCM token is required"))
        }

        if (server.isBlank()) {
            Timber.e("Cannot register device: server URL is blank")
            return@withContext Result.failure(IllegalArgumentException("Server URL is required"))
        }

        if (username.isBlank()) {
            Timber.e("Cannot register device: username is blank")
            return@withContext Result.failure(IllegalArgumentException("Username is required"))
        }

        val device = DevicesDO(
            registrationId = token,
            smapServer = server,
            userIdent = username
        )

        val result = deviceRepository.saveDevice(device)

        result.onSuccess {
            Timber.i("Device registration successful")
        }.onFailure { error ->
            Timber.e(error, "Device registration failed")
        }

        result
    }

    /**
     * Get device registration by FCM token.
     *
     * @param token Firebase Cloud Messaging registration token
     * @return Result.success with device if found, Result.failure on error
     */
    suspend fun getDeviceRegistration(token: String): Result<DevicesDO?> {
        return deviceRepository.getDevice(token)
    }

    /**
     * Unregister device (delete from DynamoDB).
     *
     * @param token Firebase Cloud Messaging registration token
     * @return Result.success if deleted, Result.failure on error
     */
    suspend fun unregisterDevice(token: String): Result<Unit> {
        Timber.i("Unregistering device with token: %s...", token.take(10))
        return deviceRepository.deleteDevice(token)
    }
}
