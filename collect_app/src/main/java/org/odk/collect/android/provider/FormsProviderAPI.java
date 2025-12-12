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

import org.odk.collect.android.database.forms.DatabaseFormColumns;
import org.odk.collect.android.external.FormsContract;

/**
 * Compatibility class for Smap code that expects the old FormsProviderAPI interface.
 * This class wraps the new fieldTask5 FormsContract and DatabaseFormColumns classes.
 */
public final class FormsProviderAPI {
    public static final String AUTHORITY = FormsContract.AUTHORITY;

    private FormsProviderAPI() {
    }

    /**
     * Columns for the Forms table.
     * This provides backward compatibility by exposing the column names from DatabaseFormColumns.
     */
    public static final class FormsColumns implements BaseColumns {
        private FormsColumns() {
        }

        /**
         * The content:// style URL for accessing Forms.
         * Note: This creates a URI without projectId for backward compatibility.
         * Smap code should migrate to using FormsContract.getUri(projectId) when possible.
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/forms");

        /**
         * The content:// style URL for accessing the newest versions of Forms.
         */
        public static final Uri CONTENT_NEWEST_FORMS_BY_FORMID_URI = Uri.parse("content://" + AUTHORITY + "/newest_forms_by_form_id");

        public static final String CONTENT_TYPE = FormsContract.CONTENT_TYPE;
        public static final String CONTENT_ITEM_TYPE = FormsContract.CONTENT_ITEM_TYPE;

        // Column names from DatabaseFormColumns
        public static final String DISPLAY_NAME = DatabaseFormColumns.DISPLAY_NAME;
        public static final String DESCRIPTION = DatabaseFormColumns.DESCRIPTION;
        public static final String JR_FORM_ID = DatabaseFormColumns.JR_FORM_ID;
        public static final String JR_VERSION = DatabaseFormColumns.JR_VERSION;
        public static final String FORM_FILE_PATH = DatabaseFormColumns.FORM_FILE_PATH;
        public static final String SUBMISSION_URI = DatabaseFormColumns.SUBMISSION_URI;
        public static final String BASE64_RSA_PUBLIC_KEY = DatabaseFormColumns.BASE64_RSA_PUBLIC_KEY;
        public static final String AUTO_DELETE = DatabaseFormColumns.AUTO_DELETE;
        public static final String AUTO_SEND = DatabaseFormColumns.AUTO_SEND;
        public static final String GEOMETRY_XPATH = DatabaseFormColumns.GEOMETRY_XPATH;
        public static final String MD5_HASH = DatabaseFormColumns.MD5_HASH;
        public static final String DATE = DatabaseFormColumns.DATE;
        public static final String JRCACHE_FILE_PATH = DatabaseFormColumns.JRCACHE_FILE_PATH;
        public static final String FORM_MEDIA_PATH = DatabaseFormColumns.FORM_MEDIA_PATH;
        public static final String LANGUAGE = DatabaseFormColumns.LANGUAGE;
        public static final String DELETED_DATE = DatabaseFormColumns.DELETED_DATE;
        public static final String LAST_DETECTED_FORM_VERSION_HASH = DatabaseFormColumns.LAST_DETECTED_FORM_VERSION_HASH;
        public static final String LAST_DETECTED_ATTACHMENTS_UPDATE_DATE = DatabaseFormColumns.LAST_DETECTED_ATTACHMENTS_UPDATE_DATE;
        public static final String USES_ENTITIES = DatabaseFormColumns.USES_ENTITIES;

        // Smap-specific columns
        public static final String PROJECT = DatabaseFormColumns.PROJECT;
        public static final String TASKS_ONLY = DatabaseFormColumns.TASKS_ONLY;
        public static final String READ_ONLY = DatabaseFormColumns.READ_ONLY;
        public static final String SEARCH_LOCAL_DATA = DatabaseFormColumns.SEARCH_LOCAL_DATA;
        public static final String SOURCE = DatabaseFormColumns.SOURCE;

        // Legacy columns
        public static final String DISPLAY_SUBTEXT = DatabaseFormColumns.DISPLAY_SUBTEXT;
    }
}
