package org.odk.collect.android.instancemanagement

import au.smap.fieldTask.utilities.SubmissionAuthGate
import org.odk.collect.analytics.Analytics
import org.odk.collect.android.analytics.AnalyticsEvents
import org.odk.collect.android.application.Collect
import org.odk.collect.android.upload.FormUploadAuthRequestedException
import org.odk.collect.android.upload.FormUploadException
import org.odk.collect.android.upload.InstanceServerUploader
import org.odk.collect.android.utilities.FormsRepositoryProvider
import org.odk.collect.android.utilities.InstanceAutoDeleteChecker
import org.odk.collect.android.utilities.InstancesRepositoryProvider
import org.odk.collect.android.utilities.WebCredentialsUtils
import org.odk.collect.forms.FormsRepository
import org.odk.collect.forms.instances.Instance
import org.odk.collect.forms.instances.InstancesRepository
import org.odk.collect.metadata.PropertyManager
import org.odk.collect.metadata.PropertyManager.Companion.PROPMGR_DEVICE_ID
import org.odk.collect.openrosa.http.OpenRosaHttpInterface
import org.odk.collect.settings.keys.ProjectKeys
import org.odk.collect.shared.settings.Settings
import timber.log.Timber

class InstanceSubmitter(
    private val formsRepository: FormsRepository,
    private val generalSettings: Settings,
    private val propertyManager: PropertyManager,
    private val httpInterface: OpenRosaHttpInterface,
    private val instancesRepository: InstancesRepository
) {

    fun submitInstances(toUpload: List<Instance>): Map<Instance, FormUploadException?> {
        val result = mutableMapOf<Instance, FormUploadException?>()
        val deviceId = propertyManager.getSingularProperty(PROPMGR_DEVICE_ID)

        val uploader = setUpODKUploader()
        val ordered = toUpload.sortedBy { it.finalizationDate }

        // smap - probe authentication once for the batch rather than discovering the same
        // rejection once per instance, and trip the circuit breaker so later runs back off.
        if (ordered.isNotEmpty()) {
            val authGate = SubmissionAuthGate(generalSettings)
            try {
                uploader.checkSubmissionAuth(uploader.getUrlToSubmitTo(ordered.first(), deviceId, null, null))
                authGate.clear()
            } catch (e: FormUploadAuthRequestedException) {
                authGate.recordAuthFailure()
                // Report the failure for each instance without touching the network or the
                // database - nothing was attempted, so nothing is marked as failed.
                ordered.forEach { result[it] = e }
                return result
            } catch (e: FormUploadException) {
                // Not an auth problem. Fall through and let each instance report its own failure.
                Timber.d(e)
            }
        }

        for (instance in ordered) {
            try {
                val destinationUrl = uploader.getUrlToSubmitTo(instance, deviceId, null, null)
                uploader.uploadOneSubmission(instance, destinationUrl)
                result[instance] = null

                deleteInstance(instance)
                logUploadedForm(instance)
            } catch (e: FormUploadException) {
                Timber.d(e)
                result[instance] = e
            }
        }
        return result
    }

    private fun setUpODKUploader(): InstanceServerUploader {
        return InstanceServerUploader(
            httpInterface,
            WebCredentialsUtils(generalSettings),
            generalSettings,
            instancesRepository
        )
    }

    private fun deleteInstance(instance: Instance) {
        // If the submission was successful, delete the instance if either the app-level
        // delete preference is set or the form definition requests auto-deletion.
        // TODO: this could take some time so might be better to do in a separate process,
        // perhaps another worker. It also feels like this could fail and if so should be
        // communicated to the user. Maybe successful delete should also be communicated?
        if (InstanceAutoDeleteChecker.shouldInstanceBeDeleted(formsRepository, generalSettings.getBoolean(ProjectKeys.KEY_DELETE_AFTER_SEND), instance)) {
            InstanceDeleter(
                InstancesRepositoryProvider(Collect.getInstance()).create(),
                FormsRepositoryProvider(Collect.getInstance()).create()
            ).delete(instance.dbId)
        }
    }

    private fun logUploadedForm(instance: Instance) {
        val value = Collect.getFormIdentifierHash(instance.formId, instance.formVersion)

        Analytics.log(AnalyticsEvents.SUBMISSION, "HTTP auto", value)
    }
}
