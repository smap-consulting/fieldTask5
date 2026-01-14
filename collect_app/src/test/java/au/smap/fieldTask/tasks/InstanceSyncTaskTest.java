package au.smap.fieldTask.tasks;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/**
 * Tests for InstanceSyncTask
 */
@RunWith(RobolectricTestRunner.class)
public class InstanceSyncTaskTest {

    @Test
    public void testTaskCreation() {
        InstanceSyncTask task = new InstanceSyncTask();
        assertNotNull(task);
    }
}
