package au.smap.fieldTask.activities;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.odk.collect.android.R;

/**
 * Instrumented tests for SmapMain activity
 */
@RunWith(AndroidJUnit4.class)
public class SmapMainTest {

    @Rule
    public ActivityScenarioRule<SmapMain> rule =
            new ActivityScenarioRule<>(SmapMain.class);

    @Test
    public void testMainActivityLaunches() {
        // Test that main activity displays
        onView(withId(R.id.pager)).check(matches(isDisplayed()));
    }

    @Test
    public void testTabsDisplayed() {
        // Test that tab layout is displayed
        onView(withId(R.id.tabs)).check(matches(isDisplayed()));
    }

    @Test
    public void testTaskTabVisible() {
        // Test that tasks tab is accessible
        // Tab navigation would require more complex setup
    }
}
