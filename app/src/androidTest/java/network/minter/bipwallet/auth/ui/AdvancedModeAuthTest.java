/*
 * Copyright (C) by MinterTeam. 2018
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

package network.minter.bipwallet.auth.ui;


import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.support.test.espresso.ViewInteraction;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import com.squareup.rx2.idler.Rx2Idler;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.reactivex.plugins.RxJavaPlugins;
import network.minter.bipwallet.R;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasErrorText;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static junit.framework.Assert.assertNotNull;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class AdvancedModeAuthTest {

    @Rule
    public ActivityTestRule<AuthActivity> mActivityTestRule = new ActivityTestRule<>(AuthActivity.class);

    private static Matcher<View> childAtPosition(
            final Matcher<View> parentMatcher, final int position) {

        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("Child at position " + position + " in parent ");
                parentMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                ViewParent parent = view.getParent();
                return parent instanceof ViewGroup && parentMatcher.matches(parent)
                        && view.equals(((ViewGroup) parent).getChildAt(position));
            }
        };
    }

    @Before
    public void regIdlingResource() {
        RxJavaPlugins.setInitComputationSchedulerHandler(
                Rx2Idler.create("RxJava 2.x Computation Scheduler")
        );
        RxJavaPlugins.setInitIoSchedulerHandler(
                Rx2Idler.create("RxJava 2.x IO Scheduler"));
    }

    @Test
    public void authActivityTest() {
        // wait for fragments
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        ViewInteraction advanceModeButton = onView(
                allOf(withId(R.id.action_advanced_mode), withText("Advanced mode"), isDisplayed()));
        // click on advanced mode
        advanceModeButton.perform(click());

        ViewInteraction seedInput = onView(allOf(withId(R.id.input_seed)));

        // paste to seed invalid text
        seedInput.perform(replaceText("WTF"), closeSoftKeyboard());

        ViewInteraction advancedActivateButton = onView(allOf(withId(R.id.action_activate), withText("Activate")));

        // try to login with invalid seed
        advancedActivateButton.perform(click());

        // check invalid seed error
        seedInput.check(matches(hasErrorText("Phrase is not valid")));

        ViewInteraction generateMnemonicButton = onView(allOf(withId(R.id.action_generate), withText("Generate Address")));
        // click on generate new seed
        generateMnemonicButton.perform(click());

        ViewInteraction appCompatTextView = onView(allOf(withId(R.id.action_copy), withText("Copy")));
        appCompatTextView.perform(scrollTo(), click());

        ClipboardManager clipboardManager = ((ClipboardManager) mActivityTestRule.getActivity().getSystemService(Context.CLIPBOARD_SERVICE));
        ClipData copiedMnemonic = clipboardManager.getPrimaryClip();
        assertNotNull(copiedMnemonic);
        CharSequence copiedMnemonicText = copiedMnemonic.getItemAt(0).getText();
        assertNotNull(copiedMnemonicText);


        ViewInteraction switch_ = onView(
                allOf(withId(R.id.switch_save_mnemonic),
                        childAtPosition(
                                allOf(withId(R.id.row_save_mnemonic),
                                        childAtPosition(
                                                withClassName(is("android.support.constraint.ConstraintLayout")),
                                                7)),
                                1)));
        switch_.perform(scrollTo(), click());

        ViewInteraction walletButton4 = onView(
                allOf(withId(R.id.action), withText("Launch the wallet"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.ScrollView")),
                                        0),
                                9)));
        walletButton4.perform(scrollTo(), click());
    }
}
