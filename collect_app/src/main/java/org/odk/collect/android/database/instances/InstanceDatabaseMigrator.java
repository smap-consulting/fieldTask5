package org.odk.collect.android.database.instances;

import static android.provider.BaseColumns._ID;
import static org.odk.collect.android.database.DatabaseConstants.INSTANCES_TABLE_NAME;
import static org.odk.collect.android.database.instances.DatabaseInstanceColumns.CAN_DELETE_BEFORE_SEND;
import static org.odk.collect.android.database.instances.DatabaseInstanceColumns.CAN_EDIT_WHEN_COMPLETE;
import static org.odk.collect.android.database.instances.DatabaseInstanceColumns.DELETED_DATE;
import static org.odk.collect.android.database.instances.DatabaseInstanceColumns.DISPLAY_NAME;
import static org.odk.collect.android.database.instances.DatabaseInstanceColumns.EDIT_NUMBER;
import static org.odk.collect.android.database.instances.DatabaseInstanceColumns.EDIT_OF;
import static org.odk.collect.android.database.instances.DatabaseInstanceColumns.FINALIZATION_DATE;
import static org.odk.collect.android.database.instances.DatabaseInstanceColumns.GEOMETRY;
import static org.odk.collect.android.database.instances.DatabaseInstanceColumns.GEOMETRY_TYPE;
import static org.odk.collect.android.database.instances.DatabaseInstanceColumns.INSTANCE_FILE_PATH;
import static org.odk.collect.android.database.instances.DatabaseInstanceColumns.JR_FORM_ID;
import static org.odk.collect.android.database.instances.DatabaseInstanceColumns.JR_VERSION;
import static org.odk.collect.android.database.instances.DatabaseInstanceColumns.LAST_STATUS_CHANGE_DATE;
import static org.odk.collect.android.database.instances.DatabaseInstanceColumns.STATUS;
import static org.odk.collect.android.database.instances.DatabaseInstanceColumns.SUBMISSION_URI;
import static org.odk.collect.android.database.instances.DatabaseInstanceColumns.SOURCE;
import static org.odk.collect.android.database.instances.DatabaseInstanceColumns.FORM_PATH;
import static org.odk.collect.android.database.instances.DatabaseInstanceColumns.ACT_LON;
import static org.odk.collect.android.database.instances.DatabaseInstanceColumns.ACT_LAT;
import static org.odk.collect.android.database.instances.DatabaseInstanceColumns.SCHED_LON;
import static org.odk.collect.android.database.instances.DatabaseInstanceColumns.SCHED_LAT;
import static org.odk.collect.android.database.instances.DatabaseInstanceColumns.T_TITLE;
import static org.odk.collect.android.database.instances.DatabaseInstanceColumns.T_TASK_TYPE;
import static org.odk.collect.android.database.instances.DatabaseInstanceColumns.T_SCHED_START;
import static org.odk.collect.android.database.instances.DatabaseInstanceColumns.T_SCHED_FINISH;
import static org.odk.collect.android.database.instances.DatabaseInstanceColumns.T_ACT_START;
import static org.odk.collect.android.database.instances.DatabaseInstanceColumns.T_ACT_FINISH;
import static org.odk.collect.android.database.instances.DatabaseInstanceColumns.T_ADDRESS;
import static org.odk.collect.android.database.instances.DatabaseInstanceColumns.T_IS_SYNC;
import static org.odk.collect.android.database.instances.DatabaseInstanceColumns.T_ASS_ID;
import static org.odk.collect.android.database.instances.DatabaseInstanceColumns.T_TASK_STATUS;
import static org.odk.collect.android.database.instances.DatabaseInstanceColumns.T_TASK_COMMENT;
import static org.odk.collect.android.database.instances.DatabaseInstanceColumns.T_REPEAT;
import static org.odk.collect.android.database.instances.DatabaseInstanceColumns.T_UPDATEID;
import static org.odk.collect.android.database.instances.DatabaseInstanceColumns.T_LOCATION_TRIGGER;
import static org.odk.collect.android.database.instances.DatabaseInstanceColumns.T_SURVEY_NOTES;
import static org.odk.collect.android.database.instances.DatabaseInstanceColumns.T_UPDATED;
import static org.odk.collect.android.database.instances.DatabaseInstanceColumns.UUID;
import static org.odk.collect.android.database.instances.DatabaseInstanceColumns.T_SHOW_DIST;
import static org.odk.collect.android.database.instances.DatabaseInstanceColumns.T_HIDE;
import static org.odk.collect.android.database.instances.DatabaseInstanceColumns.PHONE;
import static org.odk.collect.db.sqlite.SQLiteDatabaseExt.addColumn;
import static org.odk.collect.db.sqlite.SQLiteDatabaseExt.doesColumnExist;

import android.database.sqlite.SQLiteDatabase;

import org.odk.collect.db.sqlite.DatabaseMigrator;
import org.odk.collect.db.sqlite.SQLiteDatabaseExt;
import org.odk.collect.db.sqlite.SQLiteUtils;
import org.odk.collect.forms.instances.Instance;

import java.util.Arrays;
import java.util.List;

import timber.log.Timber;

public class InstanceDatabaseMigrator implements DatabaseMigrator {
    private static final String[] COLUMN_NAMES_V5 = {_ID, DISPLAY_NAME, SUBMISSION_URI, CAN_EDIT_WHEN_COMPLETE,
            INSTANCE_FILE_PATH, JR_FORM_ID, JR_VERSION, STATUS, LAST_STATUS_CHANGE_DATE, DELETED_DATE};

    private static final String[] COLUMN_NAMES_V6 = {_ID, DISPLAY_NAME, SUBMISSION_URI,
            CAN_EDIT_WHEN_COMPLETE, INSTANCE_FILE_PATH, JR_FORM_ID, JR_VERSION, STATUS,
            LAST_STATUS_CHANGE_DATE, DELETED_DATE, GEOMETRY, GEOMETRY_TYPE};

    public void onCreate(SQLiteDatabase db) {
        createInstancesTableV11(db);
    }

    @SuppressWarnings({"checkstyle:FallThrough"})
    public void onUpgrade(SQLiteDatabase db, int oldVersion) {
        Timber.w("Instances db upgrade from version: %s", oldVersion);
        switch (oldVersion) {
            case 1:
                upgradeToVersion2(db);
            case 2:
                upgradeToVersion3(db);
            case 3:
                upgradeToVersion4(db);
            case 4:
                upgradeToVersion5(db);
            case 5:
                upgradeToVersion6(db, INSTANCES_TABLE_NAME);
            case 6:
                upgradeToVersion7(db);
            case 7:
                upgradeToVersion8(db);
            case 8:
                upgradeToVersion9(db);
            case 9:
                upgradeToVersion10(db);
            case 10:
                upgradeToVersion11(db);
            case 11:
            case 12:
            case 13:
            case 14:
            case 15:
            case 16:
            case 17:
            case 18:
            case 19:
            case 20:
            case 21:
            case 22:
            case 23:
            case 24:
            case 25:
            case 26:
                // smap - Versions 11-26 used by fieldTask4 which may not have ODK Collect columns
                // Ensure all ODK Collect columns exist that may be missing in fieldTask4
                ensureOdkColumnsExist(db);
                Timber.i("Upgrading from fieldTask4 database version %s", oldVersion);
            case 27:
                // Current version - no upgrade needed
        }
    }

    // smap - Ensure all ODK Collect columns exist for fieldTask4 upgrades
    private void ensureOdkColumnsExist(SQLiteDatabase db) {
        // Columns from ODK Collect v6
        if (!doesColumnExist(db, INSTANCES_TABLE_NAME, GEOMETRY)) {
            Timber.i("Adding missing geometry column");
            addColumn(db, INSTANCES_TABLE_NAME, GEOMETRY, "text");
        }
        if (!doesColumnExist(db, INSTANCES_TABLE_NAME, GEOMETRY_TYPE)) {
            Timber.i("Adding missing geometryType column");
            addColumn(db, INSTANCES_TABLE_NAME, GEOMETRY_TYPE, "text");
        }
        // Columns from ODK Collect v8
        if (!doesColumnExist(db, INSTANCES_TABLE_NAME, CAN_DELETE_BEFORE_SEND)) {
            Timber.i("Adding missing canDeleteBeforeSend column");
            addColumn(db, INSTANCES_TABLE_NAME, CAN_DELETE_BEFORE_SEND, "text");
        }
        // Columns from ODK Collect v9
        if (!doesColumnExist(db, INSTANCES_TABLE_NAME, EDIT_OF)) {
            Timber.i("Adding missing editOf column");
            db.execSQL("ALTER TABLE " + INSTANCES_TABLE_NAME + " ADD COLUMN " + EDIT_OF + " integer REFERENCES " + INSTANCES_TABLE_NAME + "(" + _ID + ") CHECK (" + EDIT_OF + " != " + _ID + ")");
        }
        if (!doesColumnExist(db, INSTANCES_TABLE_NAME, EDIT_NUMBER)) {
            Timber.i("Adding missing editNumber column");
            db.execSQL("ALTER TABLE " + INSTANCES_TABLE_NAME + " ADD COLUMN " + EDIT_NUMBER + " integer CHECK ((" + EDIT_OF + " IS NULL AND " + EDIT_NUMBER + " IS NULL) OR + (" + EDIT_OF + " IS NOT NULL AND + " + EDIT_NUMBER + " IS NOT NULL))");
        }
        // Columns from ODK Collect v10
        if (!doesColumnExist(db, INSTANCES_TABLE_NAME, FINALIZATION_DATE)) {
            Timber.i("Adding missing finalizationDate column");
            addColumn(db, INSTANCES_TABLE_NAME, FINALIZATION_DATE, "date");
        }
    }

    private void upgradeToVersion2(SQLiteDatabase db) {
        if (!doesColumnExist(db, INSTANCES_TABLE_NAME, CAN_EDIT_WHEN_COMPLETE)) {
            addColumn(db, INSTANCES_TABLE_NAME, CAN_EDIT_WHEN_COMPLETE, "text");

            db.execSQL("UPDATE " + INSTANCES_TABLE_NAME + " SET "
                    + CAN_EDIT_WHEN_COMPLETE + " = '" + true
                    + "' WHERE " + STATUS + " IS NOT NULL AND "
                    + STATUS + " != '" + Instance.STATUS_INCOMPLETE
                    + "'");
        }
    }

    private void upgradeToVersion3(SQLiteDatabase db) {
        addColumn(db, INSTANCES_TABLE_NAME, JR_VERSION, "text");
    }

    private void upgradeToVersion4(SQLiteDatabase db) {
        addColumn(db, INSTANCES_TABLE_NAME, DELETED_DATE, "date");
    }

    /**
     * Upgrade to version 5. Prior versions of the instances table included a {@code displaySubtext}
     * column which was redundant with the {@link DatabaseInstanceColumns#STATUS} and
     * {@link DatabaseInstanceColumns#LAST_STATUS_CHANGE_DATE} columns and included
     * unlocalized text. Version 5 removes this column.
     */
    private void upgradeToVersion5(SQLiteDatabase db) {
        String temporaryTableName = INSTANCES_TABLE_NAME + "_tmp";

        // onDowngrade in Collect v1.22 always failed to clean up the temporary table so remove it now.
        // Going from v1.23 to v1.22 and back to v1.23 will result in instance status information
        // being lost.
        SQLiteUtils.dropTable(db, temporaryTableName);

        createInstancesTableV5(db, temporaryTableName);
        dropObsoleteColumns(db, COLUMN_NAMES_V5, temporaryTableName);
    }

    /**
     * Use the existing temporary table with the provided name to only keep the given relevant
     * columns, dropping all others.
     *
     * NOTE: the temporary table with the name provided is dropped.
     *
     * The move and copy strategy is used to overcome the fact that SQLITE does not directly support
     * removing a column. See https://sqlite.org/lang_altertable.html
     *
     * @param db                    the database to operate on
     * @param relevantColumns       the columns relevant to the current version
     * @param temporaryTableName    the name of the temporary table to use and then drop
     */
    private void dropObsoleteColumns(SQLiteDatabase db, String[] relevantColumns, String temporaryTableName) {
        List<String> columns = SQLiteDatabaseExt.getColumnNames(db, INSTANCES_TABLE_NAME);
        columns.retainAll(Arrays.asList(relevantColumns));
        String[] columnsToKeep = columns.toArray(new String[0]);

        SQLiteUtils.copyRows(db, INSTANCES_TABLE_NAME, columnsToKeep, temporaryTableName);
        SQLiteUtils.dropTable(db, INSTANCES_TABLE_NAME);
        SQLiteUtils.renameTable(db, temporaryTableName, INSTANCES_TABLE_NAME);
    }

    private void upgradeToVersion6(SQLiteDatabase db, String name) {
        addColumn(db, name, GEOMETRY, "text");
        addColumn(db, name, GEOMETRY_TYPE, "text");
    }

    private void upgradeToVersion7(SQLiteDatabase db) {
        String temporaryTable = INSTANCES_TABLE_NAME + "_tmp";
        SQLiteUtils.renameTable(db, INSTANCES_TABLE_NAME, temporaryTable);
        createInstancesTableV7(db);
        SQLiteUtils.copyRows(db, temporaryTable, COLUMN_NAMES_V6, INSTANCES_TABLE_NAME);
        SQLiteUtils.dropTable(db, temporaryTable);
    }

    private void upgradeToVersion8(SQLiteDatabase db) {
        addColumn(db, INSTANCES_TABLE_NAME, CAN_DELETE_BEFORE_SEND, "text");
        db.execSQL("UPDATE " + INSTANCES_TABLE_NAME + " SET " + CAN_DELETE_BEFORE_SEND + " = 'true';");
    }

    private void upgradeToVersion9(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE " + INSTANCES_TABLE_NAME + " ADD COLUMN " + EDIT_OF + " integer REFERENCES " + INSTANCES_TABLE_NAME + "(" + _ID + ") CHECK (" + EDIT_OF + " != " + _ID + ")");
        db.execSQL("ALTER TABLE " + INSTANCES_TABLE_NAME + " ADD COLUMN " + EDIT_NUMBER + " integer CHECK ((" + EDIT_OF + " IS NULL AND " + EDIT_NUMBER + " IS NULL) OR + (" + EDIT_OF + " IS NOT NULL AND + " + EDIT_NUMBER + " IS NOT NULL))");
    }

    private void upgradeToVersion10(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE " + INSTANCES_TABLE_NAME + " ADD COLUMN " + FINALIZATION_DATE + " date");
        db.execSQL(
                "UPDATE " + INSTANCES_TABLE_NAME + " SET " + FINALIZATION_DATE + " = " + LAST_STATUS_CHANGE_DATE + " WHERE " + STATUS + " IN (?, ?, ?);",
                new Object[] {Instance.STATUS_COMPLETE, Instance.STATUS_SUBMITTED, Instance.STATUS_SUBMISSION_FAILED}
        );
    }

    private void upgradeToVersion11(SQLiteDatabase db) {
        // Add Smap-specific columns
        addColumn(db, INSTANCES_TABLE_NAME, SOURCE, "text");
        addColumn(db, INSTANCES_TABLE_NAME, FORM_PATH, "text");
        addColumn(db, INSTANCES_TABLE_NAME, ACT_LON, "double");
        addColumn(db, INSTANCES_TABLE_NAME, ACT_LAT, "double");
        addColumn(db, INSTANCES_TABLE_NAME, SCHED_LON, "double");
        addColumn(db, INSTANCES_TABLE_NAME, SCHED_LAT, "double");
        addColumn(db, INSTANCES_TABLE_NAME, T_TITLE, "text");
        addColumn(db, INSTANCES_TABLE_NAME, T_TASK_TYPE, "text");
        addColumn(db, INSTANCES_TABLE_NAME, T_SCHED_START, "long");
        addColumn(db, INSTANCES_TABLE_NAME, T_SCHED_FINISH, "long");
        addColumn(db, INSTANCES_TABLE_NAME, T_ACT_START, "long");
        addColumn(db, INSTANCES_TABLE_NAME, T_ACT_FINISH, "long");
        addColumn(db, INSTANCES_TABLE_NAME, T_ADDRESS, "text");
        addColumn(db, INSTANCES_TABLE_NAME, T_IS_SYNC, "text");
        addColumn(db, INSTANCES_TABLE_NAME, T_ASS_ID, "long");
        addColumn(db, INSTANCES_TABLE_NAME, T_TASK_STATUS, "text");
        addColumn(db, INSTANCES_TABLE_NAME, T_TASK_COMMENT, "text");
        addColumn(db, INSTANCES_TABLE_NAME, T_REPEAT, "integer");
        addColumn(db, INSTANCES_TABLE_NAME, T_UPDATEID, "text");
        addColumn(db, INSTANCES_TABLE_NAME, T_LOCATION_TRIGGER, "text");
        addColumn(db, INSTANCES_TABLE_NAME, T_SURVEY_NOTES, "text");
        addColumn(db, INSTANCES_TABLE_NAME, T_UPDATED, "integer");
        addColumn(db, INSTANCES_TABLE_NAME, UUID, "text");
        addColumn(db, INSTANCES_TABLE_NAME, T_SHOW_DIST, "integer");
        addColumn(db, INSTANCES_TABLE_NAME, T_HIDE, "integer");
        addColumn(db, INSTANCES_TABLE_NAME, PHONE, "text");
    }

    private void createInstancesTableV5(SQLiteDatabase db, String name) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + name + " ("
                + _ID + " integer primary key, "
                + DISPLAY_NAME + " text not null, "
                + SUBMISSION_URI + " text, "
                + CAN_EDIT_WHEN_COMPLETE + " text, "
                + INSTANCE_FILE_PATH + " text not null, "
                + JR_FORM_ID + " text not null, "
                + JR_VERSION + " text, "
                + STATUS + " text not null, "
                + LAST_STATUS_CHANGE_DATE + " date not null, "
                + DELETED_DATE + " date );");
    }

    public void createInstancesTableV6(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + INSTANCES_TABLE_NAME + " ("
                + _ID + " integer primary key, "
                + DISPLAY_NAME + " text not null, "
                + SUBMISSION_URI + " text, "
                + CAN_EDIT_WHEN_COMPLETE + " text, "
                + INSTANCE_FILE_PATH + " text not null, "
                + JR_FORM_ID + " text not null, "
                + JR_VERSION + " text, "
                + STATUS + " text not null, "
                + LAST_STATUS_CHANGE_DATE + " date not null, "
                + DELETED_DATE + " date, "
                + GEOMETRY + " text, "
                + GEOMETRY_TYPE + " text);");
    }

    public void createInstancesTableV7(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + INSTANCES_TABLE_NAME + " ("
                + _ID + " integer primary key autoincrement, "
                + DISPLAY_NAME + " text not null, "
                + SUBMISSION_URI + " text, "
                + CAN_EDIT_WHEN_COMPLETE + " text, "
                + INSTANCE_FILE_PATH + " text not null, "
                + JR_FORM_ID + " text not null, "
                + JR_VERSION + " text, "
                + STATUS + " text not null, "
                + LAST_STATUS_CHANGE_DATE + " date not null, "
                + DELETED_DATE + " date, "
                + GEOMETRY + " text, "
                + GEOMETRY_TYPE + " text);");
    }

    public void createInstancesTableV8(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + INSTANCES_TABLE_NAME + " ("
                + _ID + " integer primary key autoincrement, "
                + DISPLAY_NAME + " text not null, "
                + SUBMISSION_URI + " text, "
                + CAN_EDIT_WHEN_COMPLETE + " text, "
                + CAN_DELETE_BEFORE_SEND + " text, "
                + INSTANCE_FILE_PATH + " text not null, "
                + JR_FORM_ID + " text not null, "
                + JR_VERSION + " text, "
                + STATUS + " text not null, "
                + LAST_STATUS_CHANGE_DATE + " date not null, "
                + DELETED_DATE + " date, "
                + GEOMETRY + " text, "
                + GEOMETRY_TYPE + " text);");
    }

    public void createInstancesTableV9(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + INSTANCES_TABLE_NAME + " ("
                + _ID + " integer primary key autoincrement, "
                + DISPLAY_NAME + " text not null, "
                + SUBMISSION_URI + " text, "
                + CAN_EDIT_WHEN_COMPLETE + " text, "
                + CAN_DELETE_BEFORE_SEND + " text, "
                + INSTANCE_FILE_PATH + " text not null, "
                + JR_FORM_ID + " text not null, "
                + JR_VERSION + " text, "
                + STATUS + " text not null, "
                + LAST_STATUS_CHANGE_DATE + " date not null, "
                + DELETED_DATE + " date, "
                + GEOMETRY + " text, "
                + GEOMETRY_TYPE + " text, "
                + EDIT_OF + " integer REFERENCES " + INSTANCES_TABLE_NAME + "(" + _ID + ") CHECK (" + EDIT_OF + " != " + _ID + "),"
                + EDIT_NUMBER + " integer CHECK ((" + EDIT_OF + " IS NULL AND " + EDIT_NUMBER + " IS NULL) OR + (" + EDIT_OF + " IS NOT NULL AND + " + EDIT_NUMBER + " IS NOT NULL))"
                + ");"
        );
    }

    public void createInstancesTableV10(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + INSTANCES_TABLE_NAME + " ("
                + _ID + " integer primary key autoincrement, "
                + DISPLAY_NAME + " text not null, "
                + SUBMISSION_URI + " text, "
                + CAN_EDIT_WHEN_COMPLETE + " text, "
                + CAN_DELETE_BEFORE_SEND + " text, "
                + INSTANCE_FILE_PATH + " text not null, "
                + JR_FORM_ID + " text not null, "
                + JR_VERSION + " text, "
                + STATUS + " text not null, "
                + LAST_STATUS_CHANGE_DATE + " date not null, "
                + FINALIZATION_DATE + " date, "
                + DELETED_DATE + " date, "
                + GEOMETRY + " text, "
                + GEOMETRY_TYPE + " text, "
                + EDIT_OF + " integer REFERENCES " + INSTANCES_TABLE_NAME + "(" + _ID + ") CHECK (" + EDIT_OF + " != " + _ID + "),"
                + EDIT_NUMBER + " integer CHECK ((" + EDIT_OF + " IS NULL AND " + EDIT_NUMBER + " IS NULL) OR + (" + EDIT_OF + " IS NOT NULL AND + " + EDIT_NUMBER + " IS NOT NULL))"
                + ");"
        );
    }

    public void createInstancesTableV11(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + INSTANCES_TABLE_NAME + " ("
                + _ID + " integer primary key autoincrement, "
                + DISPLAY_NAME + " text not null, "
                + SUBMISSION_URI + " text, "
                + CAN_EDIT_WHEN_COMPLETE + " text, "
                + CAN_DELETE_BEFORE_SEND + " text, "
                + INSTANCE_FILE_PATH + " text not null, "
                + JR_FORM_ID + " text not null, "
                + JR_VERSION + " text, "
                + STATUS + " text not null, "
                + LAST_STATUS_CHANGE_DATE + " date not null, "
                + FINALIZATION_DATE + " date, "
                + DELETED_DATE + " date, "
                + GEOMETRY + " text, "
                + GEOMETRY_TYPE + " text, "
                + EDIT_OF + " integer REFERENCES " + INSTANCES_TABLE_NAME + "(" + _ID + ") CHECK (" + EDIT_OF + " != " + _ID + "),"
                + EDIT_NUMBER + " integer CHECK ((" + EDIT_OF + " IS NULL AND " + EDIT_NUMBER + " IS NULL) OR + (" + EDIT_OF + " IS NOT NULL AND + " + EDIT_NUMBER + " IS NOT NULL)),"
                // Smap columns
                + SOURCE + " text, "
                + FORM_PATH + " text, "
                + ACT_LON + " double, "
                + ACT_LAT + " double, "
                + SCHED_LON + " double, "
                + SCHED_LAT + " double, "
                + T_TITLE + " text, "
                + T_TASK_TYPE + " text, "
                + T_SCHED_START + " long, "
                + T_SCHED_FINISH + " long, "
                + T_ACT_START + " long, "
                + T_ACT_FINISH + " long, "
                + T_ADDRESS + " text, "
                + T_IS_SYNC + " text, "
                + T_ASS_ID + " long, "
                + T_TASK_STATUS + " text, "
                + T_TASK_COMMENT + " text, "
                + T_REPEAT + " integer, "
                + T_UPDATEID + " text, "
                + T_LOCATION_TRIGGER + " text, "
                + T_SURVEY_NOTES + " text, "
                + T_UPDATED + " integer, "
                + UUID + " text, "
                + T_SHOW_DIST + " integer, "
                + T_HIDE + " integer, "
                + PHONE + " text"
                + ");"
        );
    }
}
