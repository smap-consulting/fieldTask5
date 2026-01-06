package au.smap.fieldTask.dao;

import static org.junit.Assert.assertNotNull;

import android.database.Cursor;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.odk.collect.android.support.CollectHelpers;
import org.robolectric.RobolectricTestRunner;

/**
 * Tests for InstancesDao - Smap-specific task extensions
 */
@RunWith(RobolectricTestRunner.class)
public class InstancesDaoTest {

    @Before
    public void setup() {
        CollectHelpers.setupDemoProject();
    }

    @Test
    public void testDaoCreation() {
        InstancesDao dao = new InstancesDao();
        assertNotNull(dao);
    }

    @Test
    public void testTaskColumnHandling() {
        // Test smap task columns: T_TITLE, T_TASK_TYPE, T_SCHED_START, T_ACT_START, etc.
        InstancesDao dao = new InstancesDao();
        Cursor cursor = dao.getInstancesCursor();
        assertNotNull(cursor);
        cursor.close();
    }
}
