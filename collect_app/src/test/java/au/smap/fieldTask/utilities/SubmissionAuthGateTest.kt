package au.smap.fieldTask.utilities

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.Test
import org.odk.collect.settings.keys.ProjectKeys
import org.odk.collect.shared.settings.InMemSettings

class SubmissionAuthGateTest {

    private val settings = InMemSettings()
    private val gate = SubmissionAuthGate(settings)

    @Test
    fun `is not blocked before any auth failure`() {
        assertThat(gate.isBlocked(), equalTo(false))
    }

    @Test
    fun `is blocked immediately after an auth failure`() {
        gate.recordAuthFailure()
        assertThat(gate.isBlocked(), equalTo(true))
    }

    @Test
    fun `is not blocked once the retry window has passed`() {
        settings.save(
            ProjectKeys.KEY_SMAP_SUBMISSION_AUTH_FAILED_AT,
            System.currentTimeMillis() - SubmissionAuthGate.RETRY_WINDOW_MS - 1
        )

        assertThat(gate.isBlocked(), equalTo(false))
    }

    @Test
    fun `unblocking after the window clears the stored failure`() {
        settings.save(
            ProjectKeys.KEY_SMAP_SUBMISSION_AUTH_FAILED_AT,
            System.currentTimeMillis() - SubmissionAuthGate.RETRY_WINDOW_MS - 1
        )
        gate.isBlocked()

        assertThat(settings.getLong(ProjectKeys.KEY_SMAP_SUBMISSION_AUTH_FAILED_AT), equalTo(0L))
    }

    @Test
    fun `clear unblocks immediately`() {
        gate.recordAuthFailure()
        gate.clear()

        assertThat(gate.isBlocked(), equalTo(false))
    }

    @Test
    fun `a clock moved backwards does not block`() {
        settings.save(
            ProjectKeys.KEY_SMAP_SUBMISSION_AUTH_FAILED_AT,
            System.currentTimeMillis() + SubmissionAuthGate.RETRY_WINDOW_MS
        )

        assertThat(gate.isBlocked(), equalTo(false))
    }
}
