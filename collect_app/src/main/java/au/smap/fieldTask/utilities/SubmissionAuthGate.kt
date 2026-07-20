package au.smap.fieldTask.utilities

import org.odk.collect.settings.keys.ProjectKeys
import org.odk.collect.shared.settings.Settings
import timber.log.Timber

/**
 * smap - Time boxed circuit breaker for submission authentication failures.
 *
 * Without this, credentials the server rejects cost one HEAD request per completed instance
 * per sync cycle, forever. A device holding a few hundred completed surveys and syncing on
 * every push, app open and form finalisation gets through a lot of data sending nothing.
 *
 * Once an auth failure is recorded, automatic submission is suppressed until the window
 * expires. The window is bounded so a password changed on the server is picked up without
 * the user doing anything. A manual refresh or a fresh login clears it immediately.
 */
class SubmissionAuthGate(private val settings: Settings) {

    /**
     * True if automatic submission should be skipped. Callers handling a user initiated
     * (manual) request must not consult this - the user is always allowed to try.
     */
    fun isBlocked(): Boolean {
        val failedAt = settings.getLong(ProjectKeys.KEY_SMAP_SUBMISSION_AUTH_FAILED_AT)
        if (failedAt <= 0) {
            return false
        }

        val elapsed = System.currentTimeMillis() - failedAt
        // Negative elapsed means the clock moved backwards; treat as expired rather than
        // blocking submission for up to a whole window.
        if (elapsed < 0 || elapsed >= RETRY_WINDOW_MS) {
            clear()
            return false
        }

        Timber.i("Submission auth gate blocking auto send, %d ms since auth failure", elapsed)
        return true
    }

    fun recordAuthFailure() {
        Timber.w("Server rejected credentials on submission - suppressing auto send for %d ms", RETRY_WINDOW_MS)
        settings.save(ProjectKeys.KEY_SMAP_SUBMISSION_AUTH_FAILED_AT, System.currentTimeMillis())
    }

    fun clear() {
        if (settings.getLong(ProjectKeys.KEY_SMAP_SUBMISSION_AUTH_FAILED_AT) > 0) {
            settings.save(ProjectKeys.KEY_SMAP_SUBMISSION_AUTH_FAILED_AT, 0L)
        }
    }

    companion object {
        /**
         * How long auto send stays suppressed after an auth failure. Long enough that a
         * stuck device costs at most a handful of probes a day, short enough that a password
         * corrected on the server recovers without user action.
         */
        const val RETRY_WINDOW_MS = 60 * 60 * 1000L // 1 hour
    }
}
