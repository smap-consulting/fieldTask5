package au.smap.fieldTask.aws.credentials

import android.content.Context
import au.smap.fieldTask.aws.config.AWSConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.cognitoidentity.CognitoIdentityClient
import software.amazon.awssdk.services.cognitoidentity.model.GetCredentialsForIdentityRequest
import software.amazon.awssdk.services.cognitoidentity.model.GetIdRequest
import timber.log.Timber
import java.util.Date
import java.util.concurrent.locks.ReentrantLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.withLock

/**
 * Provides AWS credentials via Amazon Cognito Identity Pool.
 * Manages unauthenticated (guest) credentials with automatic caching and refresh.
 *
 * smap - Replaces fieldTask4's IdentityManager with AWS SDK v3
 *
 * Thread-safe singleton managed by Dagger.
 */
@Singleton
class CognitoCredentialsProvider @Inject constructor(
    context: Context
) : AwsCredentialsProvider {

    private val cache = CredentialsCache(context)
    private val refreshLock = ReentrantLock()

    private val cognitoClient: CognitoIdentityClient = CognitoIdentityClient.builder()
        .region(AWSConfiguration.COGNITO_REGION)
        .build()

    /**
     * Get AWS credentials, fetching fresh ones if cache is expired.
     * Implements AwsCredentialsProvider interface for AWS SDK v3 clients.
     */
    override fun resolveCredentials(): AwsSessionCredentials {
        val cached = cache.getCredentials()
        if (cached != null && !cache.isExpired()) {
            Timber.d("Using cached AWS credentials")
            return cached
        }

        // Need to refresh - use synchronous blocking call
        // This is required by AwsCredentialsProvider interface
        Timber.i("AWS credentials expired or missing, refreshing synchronously")
        return refreshCredentialsBlocking()
    }

    /**
     * Get credentials asynchronously using Kotlin coroutines.
     * Preferred method for Android code.
     */
    suspend fun getCredentials(): AwsSessionCredentials {
        val cached = cache.getCredentials()
        if (cached != null && !cache.isExpired()) {
            Timber.d("Using cached AWS credentials")
            return cached
        }

        Timber.i("AWS credentials expired or missing, refreshing")
        return refresh()
    }

    /**
     * Force refresh of credentials from Cognito.
     * Suspend function for coroutine usage.
     */
    suspend fun refresh(): AwsSessionCredentials = withContext(Dispatchers.IO) {
        refreshLock.withLock {
            // Double-check: another thread may have refreshed while we waited for lock
            val cached = cache.getCredentials()
            if (cached != null && !cache.isExpired()) {
                Timber.d("Credentials refreshed by another thread")
                return@withContext cached
            }

            try {
                fetchCredentialsFromCognito()
            } catch (e: Exception) {
                Timber.e(e, "Failed to refresh AWS credentials")
                // Try to use cached credentials even if expired as fallback
                cache.getCredentials() ?: throw e
            }
        }
    }

    /**
     * Blocking refresh for AwsCredentialsProvider interface.
     * Used by AWS SDK clients internally.
     */
    private fun refreshCredentialsBlocking(): AwsSessionCredentials {
        return refreshLock.withLock {
            // Double-check cache
            val cached = cache.getCredentials()
            if (cached != null && !cache.isExpired()) {
                return@withLock cached
            }

            try {
                fetchCredentialsFromCognitoBlocking()
            } catch (e: Exception) {
                Timber.e(e, "Failed to refresh AWS credentials (blocking)")
                cache.getCredentials() ?: throw e
            }
        }
    }

    /**
     * Fetch fresh credentials from Cognito (coroutine version).
     */
    private suspend fun fetchCredentialsFromCognito(): AwsSessionCredentials = withContext(Dispatchers.IO) {
        Timber.i("Fetching credentials from Cognito Identity Pool: %s", AWSConfiguration.COGNITO_IDENTITY_POOL_ID)

        // Step 1: Get Cognito Identity ID (or reuse cached ID)
        val identityId = getOrCreateIdentityId()

        // Step 2: Get credentials for this identity
        val request = GetCredentialsForIdentityRequest.builder()
            .identityId(identityId)
            .build()

        val response = cognitoClient.getCredentialsForIdentity(request)
        val credentials = response.credentials()

        val awsCredentials = AwsSessionCredentials.create(
            credentials.accessKeyId(),
            credentials.secretKey(),
            credentials.sessionToken()
        )

        val expiration = Date(credentials.expiration().toEpochMilli())

        // Cache for future use
        cache.cacheCredentials(awsCredentials, expiration, identityId)

        Timber.i("Successfully fetched AWS credentials, expires: %s", expiration)
        awsCredentials
    }

    /**
     * Fetch fresh credentials from Cognito (blocking version).
     */
    private fun fetchCredentialsFromCognitoBlocking(): AwsSessionCredentials {
        Timber.i("Fetching credentials from Cognito Identity Pool (blocking): %s", AWSConfiguration.COGNITO_IDENTITY_POOL_ID)

        // Step 1: Get Cognito Identity ID (or reuse cached ID)
        val identityId = getOrCreateIdentityIdBlocking()

        // Step 2: Get credentials for this identity
        val request = GetCredentialsForIdentityRequest.builder()
            .identityId(identityId)
            .build()

        val response = cognitoClient.getCredentialsForIdentity(request)
        val credentials = response.credentials()

        val awsCredentials = AwsSessionCredentials.create(
            credentials.accessKeyId(),
            credentials.secretKey(),
            credentials.sessionToken()
        )

        val expiration = Date(credentials.expiration().toEpochMilli())

        // Cache for future use
        cache.cacheCredentials(awsCredentials, expiration, identityId)

        Timber.i("Successfully fetched AWS credentials (blocking), expires: %s", expiration)
        awsCredentials
    }

    /**
     * Get existing Cognito Identity ID or create a new one.
     */
    private suspend fun getOrCreateIdentityId(): String = withContext(Dispatchers.IO) {
        // Check cache first
        val cachedId = cache.getIdentityId()
        if (cachedId != null) {
            Timber.d("Using cached Cognito Identity ID: %s", cachedId)
            return@withContext cachedId
        }

        // Request new identity ID from Cognito
        Timber.i("Requesting new Cognito Identity ID")
        val request = GetIdRequest.builder()
            .identityPoolId(AWSConfiguration.COGNITO_IDENTITY_POOL_ID)
            .build()

        val response = cognitoClient.getId(request)
        val identityId = response.identityId()

        Timber.i("Received Cognito Identity ID: %s", identityId)
        identityId
    }

    /**
     * Get existing Cognito Identity ID or create a new one (blocking version).
     */
    private fun getOrCreateIdentityIdBlocking(): String {
        // Check cache first
        val cachedId = cache.getIdentityId()
        if (cachedId != null) {
            Timber.d("Using cached Cognito Identity ID (blocking): %s", cachedId)
            return cachedId
        }

        // Request new identity ID from Cognito
        Timber.i("Requesting new Cognito Identity ID (blocking)")
        val request = GetIdRequest.builder()
            .identityPoolId(AWSConfiguration.COGNITO_IDENTITY_POOL_ID)
            .build()

        val response = cognitoClient.getId(request)
        val identityId = response.identityId()

        Timber.i("Received Cognito Identity ID (blocking): %s", identityId)
        identityId
    }

    /**
     * Check if cached credentials are expired.
     */
    fun areCredentialsExpired(): Boolean = cache.isExpired()

    /**
     * Get cached Cognito Identity ID if available.
     */
    fun getCachedIdentityId(): String? = cache.getIdentityId()

    /**
     * Clear all cached credentials (for logout, etc.)
     */
    fun clearCredentials() {
        cache.clear()
    }
}
