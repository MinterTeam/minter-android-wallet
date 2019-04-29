/*
 * Copyright (C) by MinterTeam. 2019
 * @link <a href="https://github.com/MinterTeam">Org Github</a>
 * @link <a href="https://github.com/edwardstock">Maintainer Github</a>
 *
 * The MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package network.minter.bipwallet.tests.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.support.test.espresso.IdlingRegistry;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.intent.Intents;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.runner.AndroidJUnitRunner;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import network.minter.bipwallet.R;
import network.minter.bipwallet.auth.ui.AuthActivity;
import network.minter.bipwallet.home.ui.HomeActivity;
import network.minter.bipwallet.internal.system.testing.CallbackIdlingResource;
import network.minter.bipwallet.tests.internal.TestWallet;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.Intents.times;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static android.support.test.espresso.matcher.ViewMatchers.hasErrorText;
import static android.support.test.espresso.matcher.ViewMatchers.isEnabled;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static junit.framework.Assert.assertNotNull;
import static network.minter.bipwallet.tests.ui.actions.BottomNavigationExAction.selectCurrentItem;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;

@LargeTest
//@RunWith(AndroidJUnitRunner.class)
//@RunWith(AndroidJUnit4.class)
public class AdvancedModeTest {

    private CallbackIdlingResource mAuthWaitIdlingRes = new CallbackIdlingResource();

    @Rule
    public ActivityTestRule<AuthActivity> mActivityTestRule = new ActivityTestRule<>(AuthActivity.class);

    public AdvancedModeTest() {
    }

    @Before
    public void setUp() {
        Intents.init();
        TestWallet.app().storage().deleteAll();
        TestWallet.app().secretStorage().destroy();


        mAuthWaitIdlingRes.setIdleState(false);
        IdlingRegistry.getInstance().register(mAuthWaitIdlingRes);
        mActivityTestRule.getActivity().getAuthFragment().registerIdling(mAuthWaitIdlingRes);
    }

    @After
    public void unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(mAuthWaitIdlingRes);
        Intents.release();
    }

    @Test
    public void advancedAuthTest() throws Throwable {
        // STEP 1 - creating new mnemonic
        // wait for fragments
        ViewInteraction advanceModeButton = onView(withId(R.id.action_advanced_mode));
        // click on advanced mode
        advanceModeButton.perform(click());

        ViewInteraction seedInput = onView(withId(R.id.input_seed));

        // paste to seed invalid text
        seedInput.perform(replaceText("WTF"), closeSoftKeyboard());

        ViewInteraction advancedActivateButton = onView(allOf(withId(R.id.action_activate), withText(R.string.btn_activate)));

        // try to login with invalid seed
        advancedActivateButton.perform(click());

        // check invalid seed error
        seedInput.check(matches(hasErrorText("Phrase is not valid")));

        ViewInteraction generateMnemonicButton = onView(allOf(withId(R.id.action_generate), withText(R.string.btn_generate_address)));
        // click on generate new seed
        generateMnemonicButton.perform(click());

        ViewInteraction appCompatTextView = onView(allOf(withId(R.id.action_copy), withText(R.string.btn_copy)));
        appCompatTextView.perform(scrollTo(), click());
        final ClipboardManager[] clipboardManager = new ClipboardManager[1];
        mActivityTestRule.runOnUiThread(() -> {
            clipboardManager[0] = ((ClipboardManager) mActivityTestRule.getActivity().getSystemService(Context.CLIPBOARD_SERVICE));
        });

        ClipData copiedMnemonic = clipboardManager[0].getPrimaryClip();
        assertNotNull("ClipData is null!", copiedMnemonic);
        CharSequence copiedMnemonicText = copiedMnemonic.getItemAt(0).getText();
        assertNotNull("Copied mnemonic is null", copiedMnemonicText);

        ViewInteraction mnemonicTextView = onView(withId(R.id.mnemonic));
        mnemonicTextView.check(matches(withText(copiedMnemonicText.toString())));


        ViewInteraction launchWalletButton = onView(allOf(withId(R.id.action), withText(R.string.btn_launch_wallet)));
        launchWalletButton.check(matches(not(isEnabled())));

        ViewInteraction switchEnableLaunching = onView(allOf(withId(R.id.switch_save_mnemonic)));
        switchEnableLaunching.perform(scrollTo(), click());

        launchWalletButton.check(matches(isEnabled()));
        launchWalletButton.perform(scrollTo(), click());

        // we are on home activity
        intended(hasComponent(HomeActivity.class.getName()));

        // logging out
        ViewInteraction bottomNavigation = onView(withId(R.id.navigation_bottom));

        bottomNavigation.perform(selectCurrentItem(3));

        ViewInteraction logoutButton = onView(withId(R.id.logout));
        logoutButton.perform(click());


        //STEP 2 - auth by newly generated mnemonic
        // wait for fragments
        waitSeconds(3);
        // click on advanced mode
        advanceModeButton.perform(click());

        // trying to activate without empty seed
        advancedActivateButton.perform(scrollTo(), click());
        // seeing empty phrase error
        seedInput.check(matches(hasErrorText("Empty phrase")));
        // paste to seed valid generated mnemonic
        seedInput.perform(replaceText(copiedMnemonicText.toString()), closeSoftKeyboard());
        // activating again with valid seed
        advancedActivateButton.perform(scrollTo(), click());

        // check we are at home activity
        intended(hasComponent(HomeActivity.class.getName()), times(2));

        // logging out
        bottomNavigation.perform(selectCurrentItem(3));
        logoutButton.perform(click());

        // wait for fragments
        waitSeconds(3);
    }

    private void waitSeconds(int seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
