package au.smap.fieldTask.formmanagement;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/**
 * Tests for LocalDataManagerSmap
 */
@RunWith(RobolectricTestRunner.class)
public class LocalDataManagerSmapTest {

    @Test
    public void testCreation() {
        LocalDataManagerSmap manager = new LocalDataManagerSmap();
        assertNotNull(manager);
    }
}
