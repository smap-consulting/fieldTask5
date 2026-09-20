package org.odk.collect.android.instancemanagement.send

import org.odk.collect.forms.instances.Instance

interface InstanceUploader {
    @Throws(FormUploadException::class)
    fun uploadOneSubmission(projectId: String, instance: Instance, deviceId: String?, overrideURL: String?, referrer: String): String?

    /**
     * smap - Confirms the server will accept our credentials before a batch is uploaded.
     *
     * Costs a single HEAD request. Throws [FormUploadAuthRequestedException] if the server
     * rejects the credentials, letting the caller abandon the whole batch instead of
     * discovering the same rejection once per instance. On success the resolved URL and the
     * accepted content length are cached, so instances submitting to this URL skip their own
     * HEAD request.
     */
    @Throws(FormUploadException::class)
    fun checkSubmissionAuth(projectId: String, instance: Instance, deviceId: String?, overrideURL: String?)
}
