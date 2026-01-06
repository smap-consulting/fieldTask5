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
 * Instrumented tests for SmapLoginActivity
 */
@RunWith(AndroidJUnit4.class)
public class SmapLoginActivityTest {

    @Rule
    public ActivityScenarioRule<SmapLoginActivity> rule =
            new ActivityScenarioRule<>(SmapLoginActivity.class);

    @Test
    public void testActivityLaunches() {
        // Test that login activity displays correctly
        onView(withId(R.id.username_edit)).check(matches(isDisplayed()));
        onView(withId(R.id.password_edit)).check(matches(isDisplayed()));
    }

    @Test
    public void testLoginButton() {
        // Test login button is displayed
        onView(withId(R.id.login_button)).check(matches(isDisplayed()));
    }
}
