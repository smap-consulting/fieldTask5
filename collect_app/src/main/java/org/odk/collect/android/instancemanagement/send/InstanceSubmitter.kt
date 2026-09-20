package org.odk.collect.android.instancemanagement.send

import au.smap.fieldTask.utilities.SubmissionAuthGate
import org.odk.collect.android.instancemanagement.InstanceDeleter
import org.odk.collect.android.projects.ProjectDependencyModule
import org.odk.collect.android.utilities.InstanceAutoDeleteChecker
import org.odk.collect.forms.FormsRepository
import org.odk.collect.forms.instances.Instance
import org.odk.collect.forms.instances.InstancesRepository
import org.odk.collect.metadata.PropertyManager
import org.odk.collect.metadata.PropertyManager.Companion.PROPMGR_DEVICE_ID
import org.odk.collect.projects.ProjectDependencyFactory
import org.odk.collect.settings.keys.ProjectKeys
import org.odk.collect.shared.settings.Settings
import timber.log.Timber

class InstanceSubmitter(
    private val instanceUploader: InstanceUploader,
    private val projectDependencyFactory: ProjectDependencyFactory<ProjectDependencyModule>,
    private val propertyManager: PropertyManager
) {

    fun submitInstances(
        projectId: String,
        toUpload: List<Instance>,
        referrer: String = "",
        overrideURL: String? = null,
        cancelAfterAuthException: Boolean = false,
        externalDeleteAfterUpload: Boolean? = null,
        defaultSuccessMessage: String? = null,
        ensureActive: () -> Unit = {},
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }
    ): List<InstanceUploadResult> {
        val projectDependencyModule = projectDependencyFactory.create(projectId)
        val formsRepository = projectDependencyModule.formsRepository
        val instancesRepository = projectDependencyModule.instancesRepository
        val generalSettings = projectDependencyModule.generalSettings

        val uploadResults = mutableListOf<InstanceUploadResult>()
        val deviceId = propertyManager.getSingularProperty(PROPMGR_DEVICE_ID)

        val sortedInstances = toUpload.sortedBy { it.finalizationDate }

        // smap - probe authentication once for the batch rather than discovering the same
        // rejection once per instance, and trip the circuit breaker so later runs back off.
        if (sortedInstances.isNotEmpty()) {
            val authGate = SubmissionAuthGate(generalSettings)
            try {
                instanceUploader.checkSubmissionAuth(projectId, sortedInstances.first(), deviceId, overrideURL)
                authGate.clear()
            } catch (e: FormUploadAuthRequestedException) {
                authGate.recordAuthFailure()
                // Report the failure for each instance without touching the network or the
                // database - nothing was attempted, so nothing is marked as failed.
                return sortedInstances.map { InstanceUploadResult.Error(it, e) }
            } catch (e: FormUploadException) {
                // Not an auth problem. Fall through and let each instance report its own failure.
                Timber.d(e)
            }
        }

        for ((index, instance) in sortedInstances.withIndex()) {
            ensureActive()
            onProgress( index + 1, sortedInstances.size)

            try {
                val resultMessage = instanceUploader.uploadOneSubmission(projectId, instance, deviceId, overrideURL, referrer)
                uploadResults.add(InstanceUploadResult.Success(instance, resultMessage ?: defaultSuccessMessage))

                deleteInstance(instance, formsRepository, instancesRepository, generalSettings, externalDeleteAfterUpload)
            } catch (e: FormUploadException) {
                Timber.d(e)
                uploadResults.add(InstanceUploadResult.Error(instance, e))

                if (e is FormUploadAuthRequestedException && cancelAfterAuthException) {
                    break
                }
            }
        }

        return uploadResults
    }

    private fun deleteInstance(
        instance: Instance,
        formsRepository: FormsRepository,
        instancesRepository: InstancesRepository,
        generalSettings: Settings,
        externalDeleteAfterUpload: Boolean?
    ) {
        // If the submission was successful, delete the instance if either the app-level
        // delete preference is set or the form definition requests auto-deletion.
        // TODO: this could take some time so might be better to do in a separate process,
        // perhaps another worker. It also feels like this could fail and if so should be
        // communicated to the user. Maybe successful delete should also be communicated?
        val isFormAutoDeleteOptionEnabled = externalDeleteAfterUpload ?: generalSettings.getBoolean(ProjectKeys.KEY_DELETE_AFTER_SEND)

        if (InstanceAutoDeleteChecker.shouldInstanceBeDeleted(formsRepository, isFormAutoDeleteOptionEnabled, instance)) {
            InstanceDeleter(
                instancesRepository,
                formsRepository
            ).delete(instance.dbId)
        }
    }
}
