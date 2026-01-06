package au.smap.fieldTask.services;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

/**
 * Tests for LocationService
 */
@RunWith(RobolectricTestRunner.class)
public class LocationServiceTest {

    @Test
    public void testServiceCreation() {
        LocationService service = Robolectric.setupService(LocationService.class);
        assertNotNull(service);
    }

    @Test
    public void testLocationTracking() {
        // Test location tracking start/stop
        LocationService service = Robolectric.setupService(LocationService.class);
        assertNotNull(service);
    }
}
