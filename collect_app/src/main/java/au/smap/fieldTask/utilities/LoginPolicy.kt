package au.smap.fieldTask.utilities

/**
 * smap - decides whether the login screen has to be shown.
 *
 * Both SplashScreenActivity and SmapMain need this answer and previously each held its own copy
 * of the rule, so a fix to one could miss the other.
 */
object LoginPolicy {

    /** Log in on every launch */
    const val ALWAYS = 0

    private const val MILLIS_PER_DAY = 24L * 3600L * 1000L

    /**
     * @param pwPolicy 0 to log in every launch, greater than 0 for the number of days between
     *                 logins, anything else to never re-prompt
     * @param lastLogin epoch millis of the last successful login, 0 when there has not been one
     * @param now current epoch millis
     * @param url stored server url
     * @param user stored user name
     * @param password stored password
     */
    @JvmStatic
    fun loginRequired(
        pwPolicy: Int,
        lastLogin: Long,
        now: Long,
        url: String?,
        user: String?,
        password: String?
    ): Boolean {
        // Nobody has logged in on this device yet. The credentials cannot be used for this test
        // because reading them falls back to the build defaults, so they are never empty and a
        // new install would otherwise open the main screen against the default server.
        if (lastLogin <= 0L) {
            return true
        }

        // Credentials cleared by hand in settings
        if (url.isNullOrBlank() || user.isNullOrBlank() || password.isNullOrBlank()) {
            return true
        }

        if (pwPolicy == ALWAYS) {
            return true
        }

        // Long arithmetic - a policy of 25 days or more overflows an int
        return pwPolicy > 0 && now - lastLogin > pwPolicy * MILLIS_PER_DAY
    }
}
