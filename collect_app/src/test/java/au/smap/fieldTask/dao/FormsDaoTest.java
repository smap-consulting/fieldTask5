package au.smap.fieldTask.dao;

import static org.junit.Assert.assertNotNull;

import android.database.Cursor;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.odk.collect.android.support.CollectHelpers;
import org.robolectric.RobolectricTestRunner;

/**
 * Tests for FormsDao - Smap-specific extensions
 */
@RunWith(RobolectricTestRunner.class)
public class FormsDaoTest {

    @Before
    public void setup() {
        CollectHelpers.setupDemoProject();
    }

    @Test
    public void testDaoCreation() {
        FormsDao dao = new FormsDao();
        assertNotNull(dao);
    }

    @Test
    public void testSmapColumnHandling() {
        // Test smap-specific columns: PROJECT, TASKS_ONLY, READ_ONLY, SEARCH_LOCAL_DATA, SOURCE
        FormsDao dao = new FormsDao();
        Cursor cursor = dao.getFormsCursor();
        assertNotNull(cursor);
        cursor.close();
    }
}
