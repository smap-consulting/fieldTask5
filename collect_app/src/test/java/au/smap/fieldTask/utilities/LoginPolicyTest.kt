package au.smap.fieldTask.utilities

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.Test

class LoginPolicyTest {

    private val now = 1_700_000_000_000L
    private val day = 24L * 3600L * 1000L

    private fun required(
        pwPolicy: Int = -1,
        lastLogin: Long = now - day,
        url: String? = "https://example.org",
        user: String? = "neil",
        password: String? = "pw"
    ) = LoginPolicy.loginRequired(pwPolicy, lastLogin, now, url, user, password)

    @Test
    fun `a device that has never logged in must log in`() {
        // The credentials fall back to the build defaults, so they look configured
        assertThat(required(lastLogin = 0), equalTo(true))
    }

    @Test
    fun `defaulted credentials do not count as a login`() {
        assertThat(
            required(lastLogin = 0, url = "https://sg.smap.com.au", user = "gplay", password = "gplay"),
            equalTo(true)
        )
    }

    @Test
    fun `no re-prompt once logged in and the policy is never`() {
        assertThat(required(pwPolicy = -1), equalTo(false))
    }

    @Test
    fun `policy of zero asks every launch`() {
        assertThat(required(pwPolicy = LoginPolicy.ALWAYS), equalTo(true))
    }

    @Test
    fun `periodic policy asks only once the period has passed`() {
        assertThat(required(pwPolicy = 7, lastLogin = now - 6 * day), equalTo(false))
        assertThat(required(pwPolicy = 7, lastLogin = now - 8 * day), equalTo(true))
    }

    @Test
    fun `a long period does not overflow`() {
        // 30 * 24 * 3600 * 1000 exceeds Integer.MAX_VALUE, which used to make the
        // comparison negative and force a login on every launch
        assertThat(required(pwPolicy = 30, lastLogin = now - day), equalTo(false))
        assertThat(required(pwPolicy = 30, lastLogin = now - 31 * day), equalTo(true))
    }

    @Test
    fun `credentials cleared by hand force a login`() {
        assertThat(required(url = ""), equalTo(true))
        assertThat(required(user = ""), equalTo(true))
        assertThat(required(password = ""), equalTo(true))
        assertThat(required(password = null), equalTo(true))
    }
}
