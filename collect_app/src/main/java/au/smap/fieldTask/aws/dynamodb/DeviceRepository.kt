package au.smap.fieldTask.aws.dynamodb

import au.smap.fieldTask.aws.config.AWSConfiguration
import au.smap.fieldTask.aws.credentials.CognitoCredentialsProvider
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for DynamoDB device registration operations.
 * Provides clean abstraction over AWS SDK for device CRUD operations.
 *
 * smap - Replaces fieldTask4's direct DynamoDBMapper usage with Repository pattern.
 */
@Singleton
class DeviceRepository @Inject constructor(
    private val credentialsProvider: CognitoCredentialsProvider
) {

    private val dynamoDBClient: AmazonDynamoDBClient by lazy {
        AmazonDynamoDBClient(credentialsProvider.credentialsProvider).apply {
            setRegion(com.amazonaws.regions.Region.getRegion(AWSConfiguration.DYNAMODB_REGION))
        }
    }

    private val mapper: DynamoDBMapper by lazy {
        DynamoDBMapper.builder()
            .dynamoDBClient(dynamoDBClient)
            .build()
    }

    /**
     * Save device registration to DynamoDB.
     * Uses exponential backoff retry for network failures.
     *
     * @param device Device registration data
     * @return Result.success if saved, Result.failure on error
     */
    suspend fun saveDevice(device: DevicesDO): Result<Unit> = withContext(Dispatchers.IO) {
        Timber.i("Saving device to DynamoDB: token=%s, server=%s, user=%s",
            device.registrationId.take(10) + "...",  // Log only first 10 chars of token
            device.smapServer,
            device.userIdent
        )

        var lastException: Exception? = null
        var attempt = 0
        val maxAttempts = 3

        while (attempt < maxAttempts) {
            try {
                mapper.save(device)
                Timber.i("Successfully saved device registration to DynamoDB")
                return@withContext Result.success(Unit)
            } catch (e: Exception) {
                lastException = e
                attempt++

                if (attempt < maxAttempts) {
                    val delayMs = (1000L * (1 shl (attempt - 1)))  // Exponential backoff: 1s, 2s, 4s
                    Timber.w(e, "Failed to save device (attempt %d/%d), retrying in %dms",
                        attempt, maxAttempts, delayMs)
                    delay(delayMs)
                } else {
                    Timber.e(e, "Failed to save device after %d attempts", maxAttempts)
                }
            }
        }

        Result.failure(lastException ?: Exception("Failed to save device registration"))
    }

    /**
     * Get device registration by FCM token.
     *
     * @param registrationId Firebase Cloud Messaging registration token
     * @return Result.success with device if found, Result.failure on error
     */
    suspend fun getDevice(registrationId: String): Result<DevicesDO?> = withContext(Dispatchers.IO) {
        Timber.d("Getting device from DynamoDB: token=%s...", registrationId.take(10))

        try {
            val device = mapper.load(DevicesDO::class.java, registrationId)
            Timber.d("Device found: %s", device != null)
            Result.success(device)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get device from DynamoDB")
            Result.failure(e)
        }
    }

    /**
     * Delete device registration.
     *
     * @param registrationId Firebase Cloud Messaging registration token
     * @return Result.success if deleted, Result.failure on error
     */
    suspend fun deleteDevice(registrationId: String): Result<Unit> = withContext(Dispatchers.IO) {
        Timber.i("Deleting device from DynamoDB: token=%s...", registrationId.take(10))

        try {
            // Load the device first (required by DynamoDBMapper.delete)
            val device = mapper.load(DevicesDO::class.java, registrationId)
            if (device != null) {
                mapper.delete(device)
                Timber.i("Successfully deleted device registration")
            } else {
                Timber.w("Device not found, nothing to delete")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete device from DynamoDB")
            Result.failure(e)
        }
    }
}
