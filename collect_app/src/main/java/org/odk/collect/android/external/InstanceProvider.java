/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.odk.collect.android.external;

import static org.odk.collect.android.database.DatabaseObjectMapper.getInstanceFromValues;
import static org.odk.collect.android.database.instances.DatabaseInstanceColumns._ID;
import static org.odk.collect.android.external.InstancesContract.CONTENT_ITEM_TYPE;
import static org.odk.collect.android.external.InstancesContract.CONTENT_TYPE;
import static org.odk.collect.android.external.InstancesContract.getUri;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;

import androidx.annotation.NonNull;

import org.odk.collect.android.analytics.AnalyticsEvents;
import org.odk.collect.android.analytics.AnalyticsUtils;
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.dao.CursorLoaderFactory;
import org.odk.collect.android.database.instances.DatabaseInstanceColumns;
import org.odk.collect.android.database.instances.DatabaseInstancesRepository;
import org.odk.collect.android.injection.DaggerUtils;
import org.odk.collect.android.instancemanagement.InstanceDeleter;
import org.odk.collect.android.storage.StoragePathProvider;
import org.odk.collect.android.utilities.ContentUriHelper;
import org.odk.collect.android.utilities.FormsRepositoryProvider;
import org.odk.collect.android.utilities.InstancesRepositoryProvider;
import org.odk.collect.forms.instances.Instance;
import org.odk.collect.projects.ProjectsRepository;
import org.odk.collect.settings.SettingsProvider;

import java.io.File;

import javax.inject.Inject;

public class InstanceProvider extends ContentProvider {

    private static final int INSTANCES = 1;
    private static final int INSTANCE_ID = 2;
    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    @Inject
    InstancesRepositoryProvider instancesRepositoryProvider;

    @Inject
    FormsRepositoryProvider formsRepositoryProvider;

    @Inject
    StoragePathProvider storagePathProvider;

    @Inject
    ProjectsRepository projectsRepository;

    @Inject
    SettingsProvider settingsProvider;

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        DaggerUtils.getComponent(getContext()).inject(this);

        String projectId = getProjectId(uri);

        // We only want to log external calls to the content provider
        if (uri.getQueryParameter(CursorLoaderFactory.INTERNAL_QUERY_PARAM) == null) {
            logServerEvent(projectId, AnalyticsEvents.INSTANCE_PROVIDER_QUERY);
        }

        Cursor c;
        switch (URI_MATCHER.match(uri)) {
            case INSTANCES:
                c = dbQuery(projectId, projection, selection, selectionArgs, sortOrder);
                break;

            case INSTANCE_ID:
                String id = String.valueOf(ContentUriHelper.getIdFromUri(uri));
                c = dbQuery(projectId, projection, _ID + "=?", new String[]{id}, null);
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        // Tell the cursor what uri to watch, so it knows when its source data changes
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    private Cursor dbQuery(String projectId, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return ((DatabaseInstancesRepository) instancesRepositoryProvider.create(projectId)).rawQuery(projection, selection, selectionArgs, sortOrder, null);
    }

    private int dbUpdate(String projectId, ContentValues values, String selection, String[] selectionArgs) {
        return ((DatabaseInstancesRepository) instancesRepositoryProvider.create(projectId)).rawUpdate(values, selection, selectionArgs);
    }

    @Override
    public String getType(@NonNull Uri uri) {
        switch (URI_MATCHER.match(uri)) {
            case INSTANCES:
                return CONTENT_TYPE;

            case INSTANCE_ID:
                return CONTENT_ITEM_TYPE;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues initialValues) {
        DaggerUtils.getComponent(getContext()).inject(this);

        String projectId = getProjectId(uri);
        logServerEvent(projectId, AnalyticsEvents.INSTANCE_PROVIDER_INSERT);

        // Validate the requested uri
        if (URI_MATCHER.match(uri) != INSTANCES) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        // smap - allow SUBMISSION_URI for internal task management
        // Original ODK code blocked this to prevent external apps from setting arbitrary submission URIs
        // if (initialValues.containsKey(DatabaseInstanceColumns.SUBMISSION_URI)) {
        //     throw new SecurityException();
        // }

        // smap - use rawInsert to preserve all ContentValues columns (task fields, source, etc.)
        // Original: Instance newInstance = instancesRepositoryProvider.create(projectId).save(getInstanceFromValues(initialValues));
        long newId = ((DatabaseInstancesRepository) instancesRepositoryProvider.create(projectId)).rawInsert(initialValues);
        return getUri(projectId, newId);
    }

    /**
     * This method removes the entry from the content provider, and also removes any associated
     * files.
     * files:  form.xml, [formmd5].formdef, formname-media {directory}
     */
    @Override
    public int delete(@NonNull Uri uri, String where, String[] whereArgs) {
        DaggerUtils.getComponent(getContext()).inject(this);

        String projectId = getProjectId(uri);
        logServerEvent(projectId, AnalyticsEvents.INSTANCE_PROVIDER_DELETE);

        int count;

        switch (URI_MATCHER.match(uri)) {
            case INSTANCES:
                try (Cursor cursor = dbQuery(projectId, new String[]{_ID}, where, whereArgs, null)) {
                    while (cursor.moveToNext()) {
                        long id = cursor.getLong(cursor.getColumnIndex(_ID));
                        new InstanceDeleter(instancesRepositoryProvider.create(projectId), formsRepositoryProvider.create(projectId)).delete(id);
                    }

                    count = cursor.getCount();
                }

                break;

            case INSTANCE_ID:
                long id = ContentUriHelper.getIdFromUri(uri);

                if (where == null) {
                    new InstanceDeleter(instancesRepositoryProvider.create(projectId), formsRepositoryProvider.create(projectId)).delete(id);
                } else {
                    try (Cursor cursor = dbQuery(projectId, new String[]{_ID}, where, whereArgs, null)) {
                        while (cursor.moveToNext()) {
                            if (cursor.getLong(cursor.getColumnIndex(_ID)) == id) {
                                new InstanceDeleter(instancesRepositoryProvider.create(), formsRepositoryProvider.create()).delete(id);
                                break;
                            }
                        }
                    }
                }

                count = 1;
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);

        return count;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String where, String[] whereArgs) {
        DaggerUtils.getComponent(getContext()).inject(this);

        String projectId = getProjectId(uri);
        logServerEvent(projectId, AnalyticsEvents.INSTANCE_PROVIDER_UPDATE);

        int count;
        switch (URI_MATCHER.match(uri)) {
            case INSTANCES:
                count = dbUpdate(projectId, values, where, whereArgs);
                break;

            case INSTANCE_ID:
                String id = String.valueOf(ContentUriHelper.getIdFromUri(uri));
                count = dbUpdate(projectId, values, _ID + "=?", new String[]{id});
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    private String getProjectId(@NonNull Uri uri) {
        String queryParam = uri.getQueryParameter("projectId");

        if (queryParam != null) {
            return queryParam;
        } else {
            return projectsRepository.getAll().get(0).getUuid();
        }
    }

    private void logServerEvent(String projectId, String event) {
        AnalyticsUtils.logServerEvent(event, settingsProvider.getUnprotectedSettings(projectId));
    }

    static {
        URI_MATCHER.addURI(InstancesContract.AUTHORITY, "instances", INSTANCES);
        URI_MATCHER.addURI(InstancesContract.AUTHORITY, "instances/#", INSTANCE_ID);
    }

    // smap
    public void deleteAllFilesInDirectory(File directory) {
        if (directory.exists()) {
            // do not delete the directory if it might be an
            // ODK Tables instance data directory. Let ODK Tables
            // manage the lifetimes of its filled-in form data
            // media attachments.
            if (directory.isDirectory() && !Collect.isODKTablesInstanceDataDirectory(directory)) {
                // delete all the files in the directory
                File[] files = directory.listFiles();
                if (files != null) {
                    for (File f : files) {
                        // should make this recursive if we get worried about
                        // the media directory containing directories
                        f.delete();
                    }
                }
            }
            directory.delete();
        }
    }
}
