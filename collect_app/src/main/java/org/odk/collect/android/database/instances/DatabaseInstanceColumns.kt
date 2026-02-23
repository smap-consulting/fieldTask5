package org.odk.collect.android.database.instances

import android.provider.BaseColumns

object DatabaseInstanceColumns : BaseColumns {

    // instance column names
    const val DISPLAY_NAME = "displayName"
    const val SUBMISSION_URI = "submissionUri"
    const val INSTANCE_FILE_PATH = "instanceFilePath"
    const val JR_FORM_ID = "jrFormId"
    const val JR_VERSION = "jrVersion"
    const val STATUS = "status"
    const val CAN_EDIT_WHEN_COMPLETE = "canEditWhenComplete" // the only reason why a finalized form should not be opened for review is that it is encrypted
    const val LAST_STATUS_CHANGE_DATE = "date"
    const val FINALIZATION_DATE = "finalizationDate"
    const val DELETED_DATE = "deletedDate"
    const val GEOMETRY = "geometry"
    const val GEOMETRY_TYPE = "geometryType"
    const val CAN_DELETE_BEFORE_SEND = "canDeleteBeforeSend"
    const val EDIT_OF = "editOf"
    const val EDIT_NUMBER = "editNumber"

    // Smap-specific columns
    const val SOURCE = "source" // Source of the instance
    const val FORM_PATH = "formPath" // Path to the form for this instance
    const val ACT_LON = "actLon" // Actual longitude task was completed
    const val ACT_LAT = "actLat" // Actual latitude task was completed
    const val SCHED_LON = "schedLon" // Scheduled longitude for task
    const val SCHED_LAT = "schedLat" // Scheduled latitude for task
    const val T_TITLE = "tTitle" // Task title
    const val T_TASK_TYPE = "tTaskType" // Task type case || task
    const val T_SCHED_START = "tSchedStart" // Scheduled Start
    const val T_SCHED_FINISH = "tSchedFinish" // Scheduled Finish
    const val T_ACT_START = "tActStart" // Actual Start
    const val T_ACT_FINISH = "tActFinish" // Actual Finish
    const val T_ADDRESS = "tAddress" // Address of task
    const val T_IS_SYNC = "tIsSync" // Set if the instance has been synced
    const val T_ASS_ID = "tTaskId" // Task Id
    const val T_TASK_STATUS = "tAssStatus" // Assignment Status
    const val T_TASK_COMMENT = "tComment" // Task comment
    const val T_REPEAT = "tRepeat" // Task can be completed multiple times
    const val T_UPDATEID = "tUpdateId" // The unique identifier of the instance to be updated
    const val T_LOCATION_TRIGGER = "tLocationTrigger" // An NFC UID or Geofence that will trigger the task
    const val T_SURVEY_NOTES = "tSurveyNotes" // Any notes added to the assessment outside of the form itself
    const val T_UPDATED = "tUpdated" // Record the number of times the instance is updated
    const val UUID = "uuid" // Universally unique identifier
    const val T_SHOW_DIST = "tShowDist" // Distance at which task will be shown, 0 for always show
    const val T_HIDE = "tHide" // Set true if task is to be hidden from view
    const val PHONE = "phone" // Phone number of task
    const val T_TASK_SRV_ID = "tTaskSrvId" // Server task ID from myassignments response
}
