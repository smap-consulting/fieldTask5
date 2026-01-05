package au.smap.fieldTask.aws.credentials

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials
import timber.log.Timber
import java.util.Date
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * Thread-safe cache for AWS Cognito credentials.
 * Caches credentials in memory and optionally persists to SharedPreferences.
 *
 * AWS SDK v3 doesn't provide built-in credential caching (unlike v2's CognitoCachingCredentialsProvider),
 * so we implement our own.
 */
class CredentialsCache(private val context: Context) {

    private val lock = ReentrantReadWriteLock()
    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    // In-memory cache
    private var cachedCredentials: AwsSessionCredentials? = null
    private var expirationTime: Date? = null
    private var identityId: String? = null

    companion object {
        private const val PREF_ACCESS_KEY = "aws_access_key"
        private const val PREF_SECRET_KEY = "aws_secret_key"
        private const val PREF_SESSION_TOKEN = "aws_session_token"
        private const val PREF_EXPIRATION = "aws_expiration"
        private const val PREF_IDENTITY_ID = "aws_identity_id"

        // Buffer time before expiration to refresh credentials (5 minutes)
        private const val EXPIRATION_BUFFER_MS = 5 * 60 * 1000L
    }

    /**
     * Get cached credentials if available and not expired
     */
    fun getCredentials(): AwsSessionCredentials? = lock.read {
        if (cachedCredentials == null) {
            loadFromPreferences()
        }

        if (isExpired()) {
            Timber.d("Cached credentials expired")
            return@read null
        }

        cachedCredentials
    }

    /**
     * Cache credentials in memory and persist to SharedPreferences
     */
    fun cacheCredentials(
        credentials: AwsSessionCredentials,
        expirationDate: Date,
        cognitoIdentityId: String
    ) = lock.write {
        Timber.d("Caching AWS credentials, expires: %s", expirationDate)

        cachedCredentials = credentials
        expirationTime = expirationDate
        identityId = cognitoIdentityId

        // Persist to SharedPreferences for survival across app restarts
        prefs.edit().apply {
            putString(PREF_ACCESS_KEY, credentials.accessKeyId())
            putString(PREF_SECRET_KEY, credentials.secretAccessKey())
            putString(PREF_SESSION_TOKEN, credentials.sessionToken())
            putLong(PREF_EXPIRATION, expirationDate.time)
            putString(PREF_IDENTITY_ID, cognitoIdentityId)
            apply()
        }
    }

    /**
     * Get cached Cognito identity ID
     */
    fun getIdentityId(): String? = lock.read {
        if (identityId == null) {
            loadFromPreferences()
        }
        identityId
    }

    /**
     * Check if cached credentials are expired (with buffer time)
     */
    fun isExpired(): Boolean = lock.read {
        val expiration = expirationTime ?: return@read true
        val now = Date()
        val expirationWithBuffer = Date(expiration.time - EXPIRATION_BUFFER_MS)

        now.after(expirationWithBuffer)
    }

    /**
     * Clear all cached credentials
     */
    fun clear() = lock.write {
        Timber.d("Clearing cached AWS credentials")
        cachedCredentials = null
        expirationTime = null
        identityId = null

        prefs.edit().apply {
            remove(PREF_ACCESS_KEY)
            remove(PREF_SECRET_KEY)
            remove(PREF_SESSION_TOKEN)
            remove(PREF_EXPIRATION)
            remove(PREF_IDENTITY_ID)
            apply()
        }
    }

    /**
     * Load credentials from SharedPreferences into memory
     */
    private fun loadFromPreferences() {
        val accessKey = prefs.getString(PREF_ACCESS_KEY, null)
        val secretKey = prefs.getString(PREF_SECRET_KEY, null)
        val sessionToken = prefs.getString(PREF_SESSION_TOKEN, null)
        val expiration = prefs.getLong(PREF_EXPIRATION, 0L)
        val cognitoId = prefs.getString(PREF_IDENTITY_ID, null)

        if (accessKey != null && secretKey != null && sessionToken != null && expiration > 0) {
            cachedCredentials = AwsSessionCredentials.create(accessKey, secretKey, sessionToken)
            expirationTime = Date(expiration)
            identityId = cognitoId
            Timber.d("Loaded cached credentials from SharedPreferences")
        } else {
            Timber.d("No valid cached credentials in SharedPreferences")
        }
    }
}
