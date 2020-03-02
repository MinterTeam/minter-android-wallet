package network.minter.bipwallet.tests.ui;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.filters.LargeTest;
import androidx.test.runner.AndroidJUnit4;
import network.minter.bipwallet.R;
import network.minter.bipwallet.home.ui.HomeActivity;
import network.minter.bipwallet.tests.rules.ActivityTestRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.CoreMatchers.not;

/**
 * Created by Alexander Kolpakov (jquickapp@gmail.com) on 18-Apr-19
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class SignInActivityTest {

    @Rule
    public ActivityTestRule<SigninActivity> activityRule = new ActivityTestRule<>(SigninActivity.class);

    @Test
    public void testSignIn() {
        ViewInteraction actionButton = onView(withId(R.id.action));
        actionButton.check(ViewAssertions.matches(not(isEnabled())));

        ViewInteraction name = onView(withId(R.id.input_username));
        name.perform(typeText("123"), closeSoftKeyboard());

        // user name length from 5 to 16 (<5)
        actionButton.check(ViewAssertions.matches(not(isEnabled())));

        name.perform(clearText());
        name.perform(typeText("12345678901234567"), closeSoftKeyboard());

        name.perform(clearText());
        name.perform(typeText("bitvale"), closeSoftKeyboard());

        // user name length from 5 to 16 (>16)
        actionButton.check(ViewAssertions.matches(not(isEnabled())));

        ViewInteraction password = onView(withId(R.id.input_password));
        password.perform(typeText("1234"), closeSoftKeyboard());

        // password contains at leas 6 symbols (<6)
        actionButton.check(ViewAssertions.matches(not(isEnabled())));

        password.perform(typeText("56"), closeSoftKeyboard());

        // password contains at leas 6 symbols (==6)
        actionButton.check(ViewAssertions.matches(isEnabled()));

        password.perform(typeText("7"), closeSoftKeyboard());

        // password contains at leas 6 symbols (>6)
        actionButton.check(ViewAssertions.matches(isEnabled()));

        password.perform(clearText());
        password.perform(typeText("12444421"), closeSoftKeyboard());

        actionButton.perform(click());

        // home activity started
        intended(hasComponent(HomeActivity.class.getName()));
    }
}
