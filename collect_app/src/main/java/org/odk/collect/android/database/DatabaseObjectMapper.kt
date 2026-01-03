package org.odk.collect.android.database

import android.content.ContentValues
import android.database.Cursor
import android.provider.BaseColumns
import androidx.core.database.getLongOrNull
import org.odk.collect.android.database.forms.DatabaseFormColumns
import org.odk.collect.android.database.instances.DatabaseInstanceColumns
import org.odk.collect.androidshared.utils.PathUtils.getAbsoluteFilePath
import org.odk.collect.forms.Form
import org.odk.collect.forms.instances.Instance
import org.odk.collect.shared.PathUtils.getRelativeFilePath
import java.lang.Boolean

object DatabaseObjectMapper {

    @JvmStatic
    fun getValuesFromForm(form: Form, formsPath: String): ContentValues {
        val formFilePath = getRelativeFilePath(formsPath, form.formFilePath)
        val formMediaPath = form.formMediaPath?.let { getRelativeFilePath(formsPath, it) }

        val values = ContentValues()
        values.put(BaseColumns._ID, form.dbId)
        values.put(DatabaseFormColumns.DISPLAY_NAME, form.displayName)
        values.put(DatabaseFormColumns.DESCRIPTION, form.description)
        values.put(DatabaseFormColumns.JR_FORM_ID, form.formId)
        values.put(DatabaseFormColumns.JR_VERSION, form.version)
        values.put(DatabaseFormColumns.FORM_FILE_PATH, formFilePath)
        values.put(DatabaseFormColumns.SUBMISSION_URI, form.submissionUri)
        values.put(DatabaseFormColumns.BASE64_RSA_PUBLIC_KEY, form.basE64RSAPublicKey)
        values.put(DatabaseFormColumns.MD5_HASH, form.mD5Hash)
        values.put(DatabaseFormColumns.FORM_MEDIA_PATH, formMediaPath)
        values.put(DatabaseFormColumns.LANGUAGE, form.language)
        values.put(DatabaseFormColumns.AUTO_SEND, form.autoSend)
        values.put(DatabaseFormColumns.DATE, form.date)
        values.put(DatabaseFormColumns.AUTO_DELETE, form.autoDelete)
        values.put(DatabaseFormColumns.GEOMETRY_XPATH, form.geometryXpath)
        values.put(DatabaseFormColumns.LAST_DETECTED_ATTACHMENTS_UPDATE_DATE, form.lastDetectedAttachmentsUpdateDate)
        values.put(DatabaseFormColumns.USES_ENTITIES, Boolean.toString(form.usesEntities()))
        // Smap-specific columns
        values.put(DatabaseFormColumns.PROJECT, form.project)
        values.put(DatabaseFormColumns.TASKS_ONLY, form.tasksOnly)
        values.put(DatabaseFormColumns.READ_ONLY, form.readOnly)
        values.put(DatabaseFormColumns.SEARCH_LOCAL_DATA, form.searchLocalData)
        values.put(DatabaseFormColumns.SOURCE, form.source)
        return values
    }

    @JvmStatic
    fun getFormFromCurrentCursorPosition(
        cursor: Cursor,
        formsPath: String,
        cachePath: String
    ): Form? {
        val idColumnIndex = cursor.getColumnIndex(BaseColumns._ID)
        val displayNameColumnIndex = cursor.getColumnIndex(DatabaseFormColumns.DISPLAY_NAME)
        val descriptionColumnIndex = cursor.getColumnIndex(DatabaseFormColumns.DESCRIPTION)
        val jrFormIdColumnIndex = cursor.getColumnIndex(DatabaseFormColumns.JR_FORM_ID)
        val jrVersionColumnIndex = cursor.getColumnIndex(DatabaseFormColumns.JR_VERSION)
        val formFilePathColumnIndex = cursor.getColumnIndex(DatabaseFormColumns.FORM_FILE_PATH)
        val submissionUriColumnIndex = cursor.getColumnIndex(DatabaseFormColumns.SUBMISSION_URI)
        val base64RSAPublicKeyColumnIndex =
            cursor.getColumnIndex(DatabaseFormColumns.BASE64_RSA_PUBLIC_KEY)
        val md5HashColumnIndex = cursor.getColumnIndex(DatabaseFormColumns.MD5_HASH)
        val dateColumnIndex = cursor.getColumnIndex(DatabaseFormColumns.DATE)
        val jrCacheFilePathColumnIndex =
            cursor.getColumnIndex(DatabaseFormColumns.JRCACHE_FILE_PATH)
        val formMediaPathColumnIndex = cursor.getColumnIndex(DatabaseFormColumns.FORM_MEDIA_PATH)
        val languageColumnIndex = cursor.getColumnIndex(DatabaseFormColumns.LANGUAGE)
        val autoSendColumnIndex = cursor.getColumnIndex(DatabaseFormColumns.AUTO_SEND)
        val autoDeleteColumnIndex = cursor.getColumnIndex(DatabaseFormColumns.AUTO_DELETE)
        val geometryXpathColumnIndex = cursor.getColumnIndex(DatabaseFormColumns.GEOMETRY_XPATH)
        val deletedDateColumnIndex = cursor.getColumnIndex(DatabaseFormColumns.DELETED_DATE)
        val lastDetectedAttachmentsUpdateDateColumnIndex = cursor.getColumnIndex(DatabaseFormColumns.LAST_DETECTED_ATTACHMENTS_UPDATE_DATE)
        val usesEntitiesColumnIndex = cursor.getColumnIndex(DatabaseFormColumns.USES_ENTITIES)
        // Smap-specific column indices
        val projectColumnIndex = cursor.getColumnIndex(DatabaseFormColumns.PROJECT)
        val tasksOnlyColumnIndex = cursor.getColumnIndex(DatabaseFormColumns.TASKS_ONLY)
        val readOnlyColumnIndex = cursor.getColumnIndex(DatabaseFormColumns.READ_ONLY)
        val searchLocalDataColumnIndex = cursor.getColumnIndex(DatabaseFormColumns.SEARCH_LOCAL_DATA)
        val sourceColumnIndex = cursor.getColumnIndex(DatabaseFormColumns.SOURCE)
        return Form.Builder()
            .dbId(cursor.getLong(idColumnIndex))
            .displayName(cursor.getString(displayNameColumnIndex))
            .description(cursor.getString(descriptionColumnIndex))
            .formId(cursor.getString(jrFormIdColumnIndex))
            .version(cursor.getString(jrVersionColumnIndex))
            .formFilePath(
                getAbsoluteFilePath(
                    formsPath,
                    cursor.getString(formFilePathColumnIndex)
                )
            )
            .submissionUri(cursor.getString(submissionUriColumnIndex))
            .base64RSAPublicKey(cursor.getString(base64RSAPublicKeyColumnIndex))
            .md5Hash(cursor.getString(md5HashColumnIndex))
            .date(cursor.getLong(dateColumnIndex))
            .jrCacheFilePath(
                getAbsoluteFilePath(
                    cachePath,
                    cursor.getString(jrCacheFilePathColumnIndex)
                )
            )
            .formMediaPath(
                getAbsoluteFilePath(
                    formsPath,
                    cursor.getString(formMediaPathColumnIndex)
                )
            )
            .language(cursor.getString(languageColumnIndex))
            .autoSend(cursor.getString(autoSendColumnIndex))
            .autoDelete(cursor.getString(autoDeleteColumnIndex))
            .geometryXpath(cursor.getString(geometryXpathColumnIndex))
            .deleted(!cursor.isNull(deletedDateColumnIndex))
            .lastDetectedAttachmentsUpdateDate(cursor.getLongOrNull(lastDetectedAttachmentsUpdateDateColumnIndex))
            .usesEntities(Boolean.valueOf(cursor.getString(usesEntitiesColumnIndex)))
            // Smap-specific columns
            .project(cursor.getString(projectColumnIndex))
            .tasksOnly(cursor.getString(tasksOnlyColumnIndex))
            .readOnly(cursor.getString(readOnlyColumnIndex))
            .searchLocalData(cursor.getString(searchLocalDataColumnIndex))
            .source(cursor.getString(sourceColumnIndex))
            .build()
    }

    @JvmStatic
    fun getInstanceFromValues(values: ContentValues): Instance? {
        return Instance.Builder()
            .dbId(values.getAsLong(BaseColumns._ID))
            .displayName(values.getAsString(DatabaseInstanceColumns.DISPLAY_NAME))
            .submissionUri(values.getAsString(DatabaseInstanceColumns.SUBMISSION_URI))
            .canEditWhenComplete(Boolean.parseBoolean(values.getAsString(DatabaseInstanceColumns.CAN_EDIT_WHEN_COMPLETE)))
            .instanceFilePath(values.getAsString(DatabaseInstanceColumns.INSTANCE_FILE_PATH))
            .formId(values.getAsString(DatabaseInstanceColumns.JR_FORM_ID))
            .formVersion(values.getAsString(DatabaseInstanceColumns.JR_VERSION))
            .status(values.getAsString(DatabaseInstanceColumns.STATUS))
            .lastStatusChangeDate(values.getAsLong(DatabaseInstanceColumns.LAST_STATUS_CHANGE_DATE))
            .finalizationDate(values.getAsLong(DatabaseInstanceColumns.FINALIZATION_DATE))
            .deletedDate(values.getAsLong(DatabaseInstanceColumns.DELETED_DATE))
            .geometry(values.getAsString(DatabaseInstanceColumns.GEOMETRY))
            .geometryType(values.getAsString(DatabaseInstanceColumns.GEOMETRY_TYPE))
            .editOf(values.getAsLong(DatabaseInstanceColumns.EDIT_OF))
            .editNumber(values.getAsLong(DatabaseInstanceColumns.EDIT_NUMBER))
            .build()
    }

    @JvmStatic
    fun getInstanceFromCurrentCursorPosition(cursor: Cursor, instancesPath: String): Instance? {
        val dbId = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID))
        val displayNameColumnIndex = cursor.getColumnIndex(DatabaseInstanceColumns.DISPLAY_NAME)
        val submissionUriColumnIndex = cursor.getColumnIndex(DatabaseInstanceColumns.SUBMISSION_URI)
        val canEditWhenCompleteIndex =
            cursor.getColumnIndex(DatabaseInstanceColumns.CAN_EDIT_WHEN_COMPLETE)
        val instanceFilePathIndex =
            cursor.getColumnIndex(DatabaseInstanceColumns.INSTANCE_FILE_PATH)
        val jrFormIdColumnIndex = cursor.getColumnIndex(DatabaseInstanceColumns.JR_FORM_ID)
        val jrVersionColumnIndex = cursor.getColumnIndex(DatabaseInstanceColumns.JR_VERSION)
        val statusColumnIndex = cursor.getColumnIndex(DatabaseInstanceColumns.STATUS)
        val lastStatusChangeDateColumnIndex =
            cursor.getColumnIndex(DatabaseInstanceColumns.LAST_STATUS_CHANGE_DATE)
        val finalizationDateColumnIndex =
            cursor.getColumnIndex(DatabaseInstanceColumns.FINALIZATION_DATE)
        val deletedDateColumnIndex = cursor.getColumnIndex(DatabaseInstanceColumns.DELETED_DATE)
        val geometryTypeColumnIndex = cursor.getColumnIndex(DatabaseInstanceColumns.GEOMETRY_TYPE)
        val geometryColumnIndex = cursor.getColumnIndex(DatabaseInstanceColumns.GEOMETRY)
        val databaseIdIndex = cursor.getColumnIndex(BaseColumns._ID)
        val canDeleteBeforeSendIndex =
            cursor.getColumnIndex(DatabaseInstanceColumns.CAN_DELETE_BEFORE_SEND)
        val editOfColumnIndex = cursor.getColumnIndex(DatabaseInstanceColumns.EDIT_OF)
        val editNumberColumnIndex = cursor.getColumnIndex(DatabaseInstanceColumns.EDIT_NUMBER)
        val sourceColumnIndex = cursor.getColumnIndex(DatabaseInstanceColumns.SOURCE)

        // smap fields
        val repeatColumnIndex = cursor.getColumnIndex(DatabaseInstanceColumns.T_REPEAT)
        val updateidColumnIndex = cursor.getColumnIndex(DatabaseInstanceColumns.T_UPDATEID)
        val locationTriggerColumnIndex = cursor.getColumnIndex(DatabaseInstanceColumns.T_LOCATION_TRIGGER)
        val surveyNotesColumnIndex = cursor.getColumnIndex(DatabaseInstanceColumns.T_SURVEY_NOTES)
        val taskTypeColumnIndex = cursor.getColumnIndex(DatabaseInstanceColumns.T_TASK_TYPE)
        val assignmentIdColumnIndex = cursor.getColumnIndex(DatabaseInstanceColumns.T_ASS_ID)
        val phoneColumnIndex = cursor.getColumnIndex(DatabaseInstanceColumns.PHONE)

        return Instance.Builder()
            .dbId(dbId)
            .displayName(cursor.getString(displayNameColumnIndex))
            .submissionUri(cursor.getString(submissionUriColumnIndex))
            .canEditWhenComplete(Boolean.valueOf(cursor.getString(canEditWhenCompleteIndex)))
            .instanceFilePath(
                getAbsoluteFilePath(
                    instancesPath,
                    cursor.getString(instanceFilePathIndex)
                )
            )
            .formId(cursor.getString(jrFormIdColumnIndex))
            .formVersion(cursor.getString(jrVersionColumnIndex))
            .status(cursor.getString(statusColumnIndex))
            .lastStatusChangeDate(cursor.getLong(lastStatusChangeDateColumnIndex))
            .finalizationDate(cursor.getLongOrNull(finalizationDateColumnIndex))
            .deletedDate(cursor.getLongOrNull(deletedDateColumnIndex))
            .geometryType(cursor.getString(geometryTypeColumnIndex))
            .geometry(cursor.getString(geometryColumnIndex))
            .dbId(cursor.getLong(databaseIdIndex))
            .canDeleteBeforeSend(cursor.getString(canDeleteBeforeSendIndex)?.let { Boolean.valueOf(it) } ?: true)
            .editOf(cursor.getLongOrNull(editOfColumnIndex))
            .editNumber(cursor.getLongOrNull(editNumberColumnIndex))
            .source(cursor.getString(sourceColumnIndex))
            // smap fields
            .repeat(cursor.getString(repeatColumnIndex) == "1")
            .updateid(cursor.getString(updateidColumnIndex))
            .location_trigger(cursor.getString(locationTriggerColumnIndex))
            .survey_notes(cursor.getString(surveyNotesColumnIndex))
            .isCase(cursor.getString(taskTypeColumnIndex) == "case")
            .assignment_id(cursor.getString(assignmentIdColumnIndex))
            .phone(cursor.getString(phoneColumnIndex))
            .build()
    }

    @JvmStatic
    fun getValuesFromInstance(instance: Instance, instancesPath: String): ContentValues {
        val values = ContentValues()
        values.put(BaseColumns._ID, instance.dbId)
        values.put(DatabaseInstanceColumns.DISPLAY_NAME, instance.displayName)
        values.put(DatabaseInstanceColumns.SUBMISSION_URI, instance.submissionUri)
        values.put(
            DatabaseInstanceColumns.CAN_EDIT_WHEN_COMPLETE,
            Boolean.toString(instance.canEditWhenComplete())
        )
        values.put(
            DatabaseInstanceColumns.INSTANCE_FILE_PATH,
            getRelativeFilePath(instancesPath, instance.instanceFilePath)
        )
        values.put(DatabaseInstanceColumns.JR_FORM_ID, instance.formId)
        values.put(DatabaseInstanceColumns.JR_VERSION, instance.formVersion)
        values.put(DatabaseInstanceColumns.STATUS, instance.status)
        values.put(DatabaseInstanceColumns.LAST_STATUS_CHANGE_DATE, instance.lastStatusChangeDate)
        values.put(DatabaseInstanceColumns.FINALIZATION_DATE, instance.finalizationDate)
        values.put(DatabaseInstanceColumns.DELETED_DATE, instance.deletedDate)
        values.put(DatabaseInstanceColumns.GEOMETRY, instance.geometry)
        values.put(DatabaseInstanceColumns.GEOMETRY_TYPE, instance.geometryType)
        values.put(
            DatabaseInstanceColumns.CAN_DELETE_BEFORE_SEND,
            Boolean.toString(instance.canDeleteBeforeSend())
        )
        values.put(DatabaseInstanceColumns.EDIT_OF, instance.editOf)
        values.put(DatabaseInstanceColumns.EDIT_NUMBER, instance.editNumber)
        values.put(DatabaseInstanceColumns.SOURCE, instance.source)

        // smap fields
        values.put(DatabaseInstanceColumns.T_REPEAT, if (instance.repeat) "1" else "0")
        values.put(DatabaseInstanceColumns.T_UPDATEID, instance.updateid)
        values.put(DatabaseInstanceColumns.T_LOCATION_TRIGGER, instance.locationTrigger)
        values.put(DatabaseInstanceColumns.T_SURVEY_NOTES, instance.surveyNotes)
        values.put(DatabaseInstanceColumns.T_TASK_TYPE, if (instance.isCase) "case" else "task")
        values.put(DatabaseInstanceColumns.T_ASS_ID, instance.assignmentId)
        values.put(DatabaseInstanceColumns.PHONE, instance.phone)

        return values
    }
}
