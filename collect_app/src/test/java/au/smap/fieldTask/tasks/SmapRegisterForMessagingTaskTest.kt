package au.smap.fieldTask.tasks

import au.smap.fieldTask.aws.services.DeviceRegistrationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

/**
 * The caller records the device as registered from the onSuccess callback, so whether that
 * callback runs decides whether a failed registration is ever retried.  Before this was
 * fixed the caller saved the registration up front, a failure looked like a success, and
 * the device never registered again for the life of the install.
 */
class SmapRegisterForMessagingTaskTest {

    private val token = "fcm-token-value"
    private val server = "survey.example.org"
    private val username = "fieldworker"

    private fun taskFor(result: Result<Unit>): SmapRegisterForMessagingTask {
        val service = mock<DeviceRegistrationService> {
            onBlocking { registerDevice(any(), any(), any()) } doReturn result
        }
        val task = SmapRegisterForMessagingTask(service)
        // Unconfined runs the coroutine on this thread, so execute() has finished on return
        task.scope = CoroutineScope(Dispatchers.Unconfined)
        return task
    }

    @Test
    fun `registration reaching DynamoDB runs the callback`() {
        var registered = false

        taskFor(Result.success(Unit)).execute(token, server, username) { registered = true }

        assertTrue("The device registered, so the caller must be told to record it", registered)
    }

    @Test
    fun `failed registration does not run the callback`() {
        var registered = false

        taskFor(Result.failure(RuntimeException("no network"))).execute(token, server, username) {
            registered = true
        }

        assertFalse(
            "Recording a failed registration would stop it ever being retried",
            registered
        )
    }

    @Test
    fun `a caller that does not care about the outcome is still supported`() {
        // The three argument form is used elsewhere and must not throw
        taskFor(Result.success(Unit)).execute(token, server, username)
    }
}
