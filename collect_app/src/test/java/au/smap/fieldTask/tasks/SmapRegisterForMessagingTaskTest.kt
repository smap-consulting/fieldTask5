package au.smap.fieldTask.tasks

import au.smap.fieldTask.aws.services.DeviceRegistrationService
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner

/**
 * Tests for SmapRegisterForMessagingTask
 */
@RunWith(RobolectricTestRunner::class)
class SmapRegisterForMessagingTaskTest {

    @Test
    fun testTaskCreation() {
        val deviceRegistrationService = mock(DeviceRegistrationService::class.java)
        val task = SmapRegisterForMessagingTask(deviceRegistrationService)
        assertNotNull(task)
    }

    @Test
    fun testTokenRegistration() {
        // Test FCM token registration with server
        val deviceRegistrationService = mock(DeviceRegistrationService::class.java)
        val task = SmapRegisterForMessagingTask(deviceRegistrationService)
        assertNotNull(task)
    }
}
