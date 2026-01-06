package au.smap.fieldTask.loaders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/**
 * Tests for GeofenceEntry
 */
@RunWith(RobolectricTestRunner.class)
public class GeofenceEntryTest {

    @Test
    public void testGeofenceCreation() {
        GeofenceEntry entry = new GeofenceEntry();
        assertNotNull(entry);
    }

    @Test
    public void testGeofenceProperties() {
        GeofenceEntry entry = new GeofenceEntry();
        entry.lat = 12.345;
        entry.lon = 67.890;
        entry.radius = 100;

        assertEquals(12.345, entry.lat, 0.001);
        assertEquals(67.890, entry.lon, 0.001);
        assertEquals(100, entry.radius, 0.001);
    }
}
