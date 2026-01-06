package au.smap.fieldTask.aws.credentials

import android.content.Context
import au.smap.fieldTask.aws.config.AWSConfiguration
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides AWS credentials via Amazon Cognito Identity Pool.
 * Manages unauthenticated (guest) credentials with automatic caching and refresh.
 *
 * smap - Replaces fieldTask4's IdentityManager with AWS Android SDK v2.
 * Wraps CognitoCachingCredentialsProvider which handles caching automatically.
 *
 * Thread-safe singleton managed by Dagger.
 */
@Singleton
class CognitoCredentialsProvider @Inject constructor(
    context: Context
) {
    /**
     * AWS Android SDK v2's built-in credentials provider with caching.
     * This handles all credential fetching, caching, and refreshing automatically.
     */
    val credentialsProvider: CognitoCachingCredentialsProvider = CognitoCachingCredentialsProvider(
        context.applicationContext,
        AWSConfiguration.COGNITO_IDENTITY_POOL_ID,
        AWSConfiguration.COGNITO_REGION
    )

    init {
        Timber.i("Initialized CognitoCredentialsProvider with pool: %s",
            AWSConfiguration.COGNITO_IDENTITY_POOL_ID)
    }

    /**
     * Get AWS credentials (blocking call).
     * Will fetch from cache or refresh from Cognito as needed.
     *
     * @return AWS credentials
     */
    fun getCredentials(): com.amazonaws.auth.AWSSessionCredentials {
        return credentialsProvider.credentials as com.amazonaws.auth.AWSSessionCredentials
    }

    /**
     * Force refresh credentials from Cognito.
     * Useful when credentials may be invalid.
     */
    fun refresh() {
        Timber.i("Force refreshing AWS credentials")
        credentialsProvider.refresh()
    }

    /**
     * Get the Cognito Identity ID for this device.
     * This is stable across sessions and cached by the provider.
     *
     * @return Cognito Identity ID
     */
    fun getIdentityId(): String {
        return credentialsProvider.identityId
    }

    /**
     * Clear all cached credentials and identity.
     * Call this on logout or when switching users.
     */
    fun clearCredentials() {
        Timber.i("Clearing cached AWS credentials and identity")
        credentialsProvider.clear()
        credentialsProvider.clearCredentials()
    }

    /**
     * Check if credentials are cached (doesn't guarantee they're valid).
     *
     * @return true if credentials are in cache
     */
    fun hasCachedCredentials(): Boolean {
        return try {
            credentialsProvider.cachedIdentityId != null
        } catch (e: Exception) {
            Timber.w(e, "Error checking cached credentials")
            false
        }
    }
}
