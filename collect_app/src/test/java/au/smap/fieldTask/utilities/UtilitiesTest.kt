package au.smap.fieldTask.utilities

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A fresh install carries a default user name and server, and registering those put every
 * download of the app into the device table under one shared name.  Suppressing that must not
 * catch a device somebody has actually configured, because the only symptom would be that the
 * device quietly stops refreshing.
 */
class UtilitiesTest {

    private val defaultUser = "gplay"
    private val defaultServer = "sg.smap.com.au"

    @Test
    fun `an install still on both defaults is unconfigured`() {
        assertTrue(Utilities.isUnconfigured(defaultUser, defaultServer, defaultUser, defaultServer))
    }

    @Test
    fun `a user who has logged in is configured, even on the default server`() {
        assertFalse(Utilities.isUnconfigured("fieldworker", defaultServer, defaultUser, defaultServer))
    }

    @Test
    fun `a user who has changed server is configured, even under the default name`() {
        assertFalse(Utilities.isUnconfigured(defaultUser, "survey.client.org", defaultUser, defaultServer))
    }

    @Test
    fun `a flavour shipping no default name detects nothing`() {
        // ljstracker ships an empty default_username, so there is nothing to compare against
        assertFalse(Utilities.isUnconfigured("", "https://ljss.smap.au", "", "ljss.smap.au"))
        assertFalse(Utilities.isUnconfigured(null, null, null, null))
    }

    @Test
    fun `an unset user name is not mistaken for the default`() {
        assertFalse(Utilities.isUnconfigured(null, defaultServer, defaultUser, defaultServer))
    }
}
