

package au.smap.fieldTask.providers;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.text.TextUtils;

import au.smap.fieldTask.database.SmapTraceDatabaseHelper;
import au.smap.fieldTask.database.TraceProviderAPI;
import au.smap.fieldTask.database.TraceProviderAPI.TraceColumns;

import timber.log.Timber;

/**
 * Content Provider for trace database
 *
 * New Architecture (Scoped Storage):
 * - No storage permissions needed - trace.db is in app-specific metadata directory
 * - AltDatabasePathContext automatically creates directories as needed
 * - No StorageInitializer required
 */
public class TraceProvider extends ContentProvider {

    private static final int TRACES = 1;
    private static final int TRACE_ID = 2;

    private static final String TABLE_NAME = "trace";

    private static final UriMatcher sUriMatcher;

    private static SmapTraceDatabaseHelper dbHelper;

    private synchronized SmapTraceDatabaseHelper getDbHelper() {
        // Check if database needs upgrade
        boolean databaseNeedsUpgrade = SmapTraceDatabaseHelper.databaseNeedsUpgrade();
        if (dbHelper == null || (databaseNeedsUpgrade && !SmapTraceDatabaseHelper.isDatabaseBeingMigrated())) {
            if (databaseNeedsUpgrade) {
                SmapTraceDatabaseHelper.databaseMigrationStarted();
            }
            recreateDatabaseHelper();
        }

        return dbHelper;
    }

    public static void recreateDatabaseHelper() {
        dbHelper = new SmapTraceDatabaseHelper();
    }

    @Override
    public boolean onCreate() {
        return true;
    }


    @Override
    public Cursor query(Uri uri, String[] projection, String where, String[] selectionArgs,
            String sortOrder) {

        SQLiteDatabase db = getDbHelper().getWritableDatabase();

        Cursor c = null;
        switch (sUriMatcher.match(uri)) {
            case TRACES:
                c = db.query(TABLE_NAME, projection, where, selectionArgs, null, null, sortOrder, null);
                break;

            case TRACE_ID:
                String instanceId = uri.getPathSegments().get(1);
                c = db.query(TABLE_NAME, projection, TraceColumns._ID + "=" + instanceId
                        + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), selectionArgs, null, null, sortOrder, null);

                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        // Tell the cursor what uri to watch, so it knows when its source data changes
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }


    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case TRACES:
                return TraceColumns.CONTENT_TYPE;

            case TRACE_ID:
                return TraceColumns.CONTENT_ITEM_TYPE;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        // Validate the requested uri
        if (sUriMatcher.match(uri) != TRACES) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        ContentValues values;
        if (initialValues != null) {
            values = new ContentValues(initialValues);
        } else {
            values = new ContentValues();
        }

        SQLiteDatabase db = getDbHelper().getWritableDatabase();
        long rowId = db.insert(TABLE_NAME, null, values);
        if (rowId > 0) {
            Uri traceUri = ContentUris.withAppendedId(TraceColumns.CONTENT_URI, rowId);
            getContext().getContentResolver().notifyChange(traceUri, null);
            return traceUri;
        }

        Timber.e(new SQLException("Failed to insert row into " + uri));
        return null;
    }


    /**
     * This method removes the entry from the content provider, and also removes any associated files.
     * files:  form.xml, [formmd5].formdef, formname-media {directory}
     */
    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        SQLiteDatabase db = getDbHelper().getWritableDatabase();
        int count;

        switch (sUriMatcher.match(uri)) {
            case TRACES:
                count = db.delete(TABLE_NAME, where, whereArgs);
                break;

            case TRACE_ID:
                String instanceId = uri.getPathSegments().get(1);
                count =
                    db.delete(TABLE_NAME,
                        TraceColumns._ID + "=" + instanceId
                                + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
                        whereArgs);
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        SQLiteDatabase db = getDbHelper().getWritableDatabase();

        int count;
        String status = null;
        switch (sUriMatcher.match(uri)) {
            case TRACES:
                count = db.update(TABLE_NAME, values, where, whereArgs);
                break;

            case TRACE_ID:
                String instanceId = uri.getPathSegments().get(1);

                count =
                    db.update(TABLE_NAME, values, TraceColumns._ID + "=" + instanceId
                            + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }


    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(TraceProviderAPI.AUTHORITY, "trace", TRACES);
        sUriMatcher.addURI(TraceProviderAPI.AUTHORITY, "trace/#", TRACE_ID);
    }
}
