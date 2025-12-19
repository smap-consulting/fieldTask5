
package au.smap.fieldTask.database;

import static android.provider.BaseColumns._ID;

import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.odk.collect.android.application.Collect;
import org.odk.collect.android.storage.StoragePathProvider;
import org.odk.collect.android.storage.StorageSubdirectory;
import org.odk.collect.db.sqlite.AltDatabasePathContext;
import org.odk.collect.db.sqlite.SQLiteUtils;

import java.io.File;

import timber.log.Timber;


/**
 * This class helps open, create, and upgrade the database file.
 */
public class SmapTraceDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "trace.db";
    private static final String TABLE_NAME = "trace";

    static final int DATABASE_VERSION = 2;

    private static final String[] COLUMN_NAMES_V1 = {
            _ID,
            TraceProviderAPI.TraceColumns.LAT,
            TraceProviderAPI.TraceColumns.LON,
            TraceProviderAPI.TraceColumns.TIME,
    };

    private static final String[] COLUMN_NAMES_V2 = {
            _ID,
            TraceProviderAPI.TraceColumns.SOURCE,
            TraceProviderAPI.TraceColumns.LAT,
            TraceProviderAPI.TraceColumns.LON,
            TraceProviderAPI.TraceColumns.TIME,
            };



    static final String[] CURRENT_VERSION_COLUMN_NAMES = COLUMN_NAMES_V2;  // smap

    private static boolean isDatabaseBeingMigrated;

    public SmapTraceDatabaseHelper() {
        super(new AltDatabasePathContext(new StoragePathProvider().getDirPath(StorageSubdirectory.METADATA), Collect.getInstance()), DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static String getDatabasePath() {
        return new StoragePathProvider().getDirPath(StorageSubdirectory.METADATA) + File.separator + DATABASE_NAME;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createLatestVersion(db);
    }

    /**
     * Upgrades the database.
     *
     * When a new migration is added, a corresponding test case should be added to
     * InstancesDatabaseHelperTest by copying a real database into assets.
     */
    @SuppressWarnings({"checkstyle:FallThrough"})
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        try {
            Timber.i("Upgrading database from version %d to %d", oldVersion, newVersion);

            upgrade(db);

            Timber.i("Upgrading database from version %d to %d completed with success.", oldVersion, newVersion);
            isDatabaseBeingMigrated = false;
        } catch (SQLException e) {
            isDatabaseBeingMigrated = false;
            throw e;
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        try {
            isDatabaseBeingMigrated = false;
        } catch (SQLException e) {
            isDatabaseBeingMigrated = false;
            throw e;
        }
    }

    private void upgrade(SQLiteDatabase db) {
        if (!SQLiteUtils.doesColumnExist(db, TABLE_NAME, TraceProviderAPI.TraceColumns.SOURCE)) {
            SQLiteUtils.addColumn(db, TABLE_NAME, TraceProviderAPI.TraceColumns.SOURCE, "text");
        }
    }

    private static void createLatestVersion(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ("
                + _ID + " integer primary key, "
                + TraceProviderAPI.TraceColumns.SOURCE + " text, "
                + TraceProviderAPI.TraceColumns.LAT + " double not null, "
                + TraceProviderAPI.TraceColumns.LON + " double not null, "
                + TraceProviderAPI.TraceColumns.TIME + " long not null "
                + ");");
    }

    public static void databaseMigrationStarted() {
        isDatabaseBeingMigrated = true;
    }

    public static boolean isDatabaseBeingMigrated() {
        return isDatabaseBeingMigrated;
    }

    public static boolean databaseNeedsUpgrade() {
        boolean isDatabaseHelperOutOfDate = false;
        try {
            SQLiteDatabase db = SQLiteDatabase.openDatabase(SmapTraceDatabaseHelper.getDatabasePath(), null, SQLiteDatabase.OPEN_READONLY);
            isDatabaseHelperOutOfDate = SmapTraceDatabaseHelper.DATABASE_VERSION != db.getVersion();
            db.close();
        } catch (SQLException e) {
            Timber.i(e);
        }
        return isDatabaseHelperOutOfDate;
    }

    // smap
    public static void recreateDatabase() {

        try {
            SQLiteDatabase db = SQLiteDatabase.openDatabase(SmapTraceDatabaseHelper.getDatabasePath(), null, SQLiteDatabase.OPEN_READWRITE);
            SQLiteUtils.dropTable(db, TABLE_NAME);
            createLatestVersion(db);
            db.close();
        } catch (SQLException e) {
            Timber.i(e);
        }
    }

}
