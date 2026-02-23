/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.odk.collect.android.provider;

import android.net.Uri;
import android.provider.BaseColumns;

import org.odk.collect.android.database.instances.DatabaseInstanceColumns;
import org.odk.collect.android.external.InstancesContract;

/**
 * Compatibility class for Smap code that expects the old InstanceProviderAPI interface.
 * This class wraps the new fieldTask5 InstancesContract and DatabaseInstanceColumns classes.
 */
public final class InstanceProviderAPI {
    public static final String AUTHORITY = InstancesContract.AUTHORITY;

    // This class cannot be instantiated
    private InstanceProviderAPI() {
    }

    /**
     * Notes table (instances)
     */
    public static final class InstanceColumns implements BaseColumns {
        // This class cannot be instantiated
        private InstanceColumns() {
        }

        /**
         * The content:// style URL for accessing instances.
         * Note: This creates a URI without projectId for backward compatibility.
         * Smap code should migrate to using InstancesContract.getUri(projectId) when possible.
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/instances");

        public static final String CONTENT_TYPE = InstancesContract.CONTENT_TYPE;
        public static final String CONTENT_ITEM_TYPE = InstancesContract.CONTENT_ITEM_TYPE;

        // Column names from DatabaseInstanceColumns
        public static final String DISPLAY_NAME = DatabaseInstanceColumns.DISPLAY_NAME;
        public static final String SUBMISSION_URI = DatabaseInstanceColumns.SUBMISSION_URI;
        public static final String INSTANCE_FILE_PATH = DatabaseInstanceColumns.INSTANCE_FILE_PATH;
        public static final String JR_FORM_ID = DatabaseInstanceColumns.JR_FORM_ID;
        public static final String JR_VERSION = DatabaseInstanceColumns.JR_VERSION;
        public static final String STATUS = DatabaseInstanceColumns.STATUS;
        public static final String CAN_EDIT_WHEN_COMPLETE = DatabaseInstanceColumns.CAN_EDIT_WHEN_COMPLETE;
        public static final String LAST_STATUS_CHANGE_DATE = DatabaseInstanceColumns.LAST_STATUS_CHANGE_DATE;
        public static final String FINALIZATION_DATE = DatabaseInstanceColumns.FINALIZATION_DATE;
        public static final String DELETED_DATE = DatabaseInstanceColumns.DELETED_DATE;
        public static final String GEOMETRY = DatabaseInstanceColumns.GEOMETRY;
        public static final String GEOMETRY_TYPE = DatabaseInstanceColumns.GEOMETRY_TYPE;
        public static final String CAN_DELETE_BEFORE_SEND = DatabaseInstanceColumns.CAN_DELETE_BEFORE_SEND;
        public static final String EDIT_OF = DatabaseInstanceColumns.EDIT_OF;
        public static final String EDIT_NUMBER = DatabaseInstanceColumns.EDIT_NUMBER;

        // Smap-specific columns
        public static final String SOURCE = DatabaseInstanceColumns.SOURCE;
        public static final String FORM_PATH = DatabaseInstanceColumns.FORM_PATH;
        public static final String ACT_LON = DatabaseInstanceColumns.ACT_LON;
        public static final String ACT_LAT = DatabaseInstanceColumns.ACT_LAT;
        public static final String SCHED_LON = DatabaseInstanceColumns.SCHED_LON;
        public static final String SCHED_LAT = DatabaseInstanceColumns.SCHED_LAT;
        public static final String T_TITLE = DatabaseInstanceColumns.T_TITLE;
        public static final String T_TASK_TYPE = DatabaseInstanceColumns.T_TASK_TYPE;
        public static final String T_SCHED_START = DatabaseInstanceColumns.T_SCHED_START;
        public static final String T_SCHED_FINISH = DatabaseInstanceColumns.T_SCHED_FINISH;
        public static final String T_ACT_START = DatabaseInstanceColumns.T_ACT_START;
        public static final String T_ACT_FINISH = DatabaseInstanceColumns.T_ACT_FINISH;
        public static final String T_ADDRESS = DatabaseInstanceColumns.T_ADDRESS;
        public static final String T_IS_SYNC = DatabaseInstanceColumns.T_IS_SYNC;
        public static final String T_ASS_ID = DatabaseInstanceColumns.T_ASS_ID;
        public static final String T_TASK_STATUS = DatabaseInstanceColumns.T_TASK_STATUS;
        public static final String T_TASK_COMMENT = DatabaseInstanceColumns.T_TASK_COMMENT;
        public static final String T_REPEAT = DatabaseInstanceColumns.T_REPEAT;
        public static final String T_UPDATEID = DatabaseInstanceColumns.T_UPDATEID;
        public static final String T_LOCATION_TRIGGER = DatabaseInstanceColumns.T_LOCATION_TRIGGER;
        public static final String T_SURVEY_NOTES = DatabaseInstanceColumns.T_SURVEY_NOTES;
        public static final String T_UPDATED = DatabaseInstanceColumns.T_UPDATED;
        public static final String UUID = DatabaseInstanceColumns.UUID;
        public static final String T_SHOW_DIST = DatabaseInstanceColumns.T_SHOW_DIST;
        public static final String T_HIDE = DatabaseInstanceColumns.T_HIDE;
        public static final String PHONE = DatabaseInstanceColumns.PHONE;
        public static final String T_TASK_SRV_ID = DatabaseInstanceColumns.T_TASK_SRV_ID;
    }
}
