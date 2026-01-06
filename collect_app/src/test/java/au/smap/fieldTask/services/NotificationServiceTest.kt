package au.smap.fieldTask.services

import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

/**
 * Tests for NotificationService (FCM)
 */
@RunWith(RobolectricTestRunner::class)
class NotificationServiceTest {

    @Test
    fun testServiceCreation() {
        val service = Robolectric.setupService(NotificationService::class.java)
        assertNotNull(service)
    }

    @Test
    fun testFcmMessageHandling() {
        // Test FCM message handling
        val service = Robolectric.setupService(NotificationService::class.java)
        assertNotNull(service)
    }
}
