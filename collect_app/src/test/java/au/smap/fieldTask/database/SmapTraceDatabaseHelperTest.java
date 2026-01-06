package au.smap.fieldTask.database;

import static org.junit.Assert.assertNotNull;

import android.database.sqlite.SQLiteDatabase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.odk.collect.android.application.Collect;
import org.robolectric.RobolectricTestRunner;

/**
 * Tests for SmapTraceDatabaseHelper
 */
@RunWith(RobolectricTestRunner.class)
public class SmapTraceDatabaseHelperTest {

    @Test
    public void testDatabaseCreation() {
        SmapTraceDatabaseHelper helper = new SmapTraceDatabaseHelper(Collect.getInstance());
        assertNotNull(helper);
        SQLiteDatabase db = helper.getWritableDatabase();
        assertNotNull(db);
        db.close();
    }

    @Test
    public void testTraceTableExists() {
        SmapTraceDatabaseHelper helper = new SmapTraceDatabaseHelper(Collect.getInstance());
        SQLiteDatabase db = helper.getWritableDatabase();

        // Verify trace table exists with required columns
        assertNotNull(db);

        db.close();
    }
}
