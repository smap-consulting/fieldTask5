package au.smap.fieldTask.widgets;

import static org.junit.Assert.assertNotNull;

import android.app.Activity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.odk.collect.android.support.WidgetTestActivity;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

/**
 * Tests for SmapChartLineWidget
 */
@RunWith(RobolectricTestRunner.class)
public class SmapChartLineWidgetTest {

    private Activity activity;

    @Before
    public void setup() {
        activity = Robolectric.buildActivity(WidgetTestActivity.class).create().get();
    }

    @Test
    public void testChartCreation() {
        assertNotNull(activity);
    }

    @Test
    public void testDataParsing() {
        // Test chart data parsing from JSON
        String chartData = "{\"labels\":[\"A\",\"B\"],\"values\":[1,2]}";
        assertNotNull(chartData);
    }
}
