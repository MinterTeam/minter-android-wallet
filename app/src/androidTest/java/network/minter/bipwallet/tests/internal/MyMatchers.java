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

package network.minter.bipwallet.tests.internal;

import android.support.annotation.RestrictTo;
import android.support.annotation.StringRes;
import android.support.design.widget.TextInputLayout;
import android.view.View;
import android.view.ViewParent;

import junit.framework.AssertionFailedError;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import network.minter.bipwallet.internal.Wallet;
import network.minter.bipwallet.internal.system.StringUtil;
import timber.log.Timber;

/**
 * minter-android-wallet. 2019
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
@RestrictTo(RestrictTo.Scope.TESTS)
public final class MyMatchers {

    public static Matcher<View> withInputLayoutError(@StringRes int expectedErrorTextRes) {
        return new TypeSafeMatcher<View>() {
            @Override
            public boolean matchesSafely(View view) {
                TextInputLayout inputLayout = findInParentTree(TextInputLayout.class, view);
                if (inputLayout == null) {
                    throw new AssertionFailedError(String.format("View with id %s does not have a parent TextInputLayout", view.getContext().getResources().getResourceEntryName(view.getId())));
                }

                CharSequence inputError = inputLayout.getError();
                Timber.d("Input error: %s", inputError);
                return StringUtil.safeCompare(inputLayout, inputError, expectedErrorTextRes);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(String.format("Input error is: '%s'", TestWallet.app().res().getString(expectedErrorTextRes)));
            }
        };
    }

    public static Matcher<View> withInputLayoutError(final String expectedErrorText) {
        return new TypeSafeMatcher<View>() {

            @Override
            public boolean matchesSafely(View view) {
                TextInputLayout inputLayout = findInParentTree(TextInputLayout.class, view);
                if (inputLayout == null) {
                    throw new AssertionFailedError(String.format("View with id %s does not have a parent TextInputLayout", view.getContext().getResources().getResourceEntryName(view.getId())));
                }

                CharSequence inputError = inputLayout.getError();
                Timber.d("Input error: %s", inputError);
                return StringUtil.safeCompare(inputError, expectedErrorText);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(String.format("Input error is: '%s'", expectedErrorText));
            }
        };
    }

    public static Matcher<View> withInputLayoutHint(@StringRes int expectedHintTextRes) {
        return new TypeSafeMatcher<View>() {

            @Override
            public boolean matchesSafely(View view) {
                TextInputLayout inputLayout = findInParentTree(TextInputLayout.class, view);
                if (inputLayout == null) {
                    throw new AssertionFailedError(String.format("View with id %s does not have a parent TextInputLayout", view.getContext().getResources().getResourceEntryName(view.getId())));
                }

                CharSequence inputHint = inputLayout.getHint();
                Timber.d("Input hint: %s", inputHint);
                return StringUtil.safeCompare(inputLayout, inputHint, expectedHintTextRes);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(String.format("Input hint is: '%s'", Wallet.app().res().getString(expectedHintTextRes)));
            }
        };
    }

    public static Matcher<View> withInputLayoutHint(final String expectedHintText) {
        return new TypeSafeMatcher<View>() {

            @Override
            public boolean matchesSafely(View view) {
                TextInputLayout inputLayout = findInParentTree(TextInputLayout.class, view);
                if (inputLayout == null) {
                    throw new AssertionFailedError(String.format("View with id %s does not have a parent TextInputLayout", view.getContext().getResources().getResourceEntryName(view.getId())));
                }

                CharSequence inputHint = inputLayout.getHint();
                Timber.d("Input hint: %s", inputHint);
                return StringUtil.safeCompare(inputHint, expectedHintText);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(String.format("Input hint is: '%s'", expectedHintText));
            }
        };
    }

    private static <T extends View> T findInParentTree(Class<T> cls, View view) {
        if (view == null || cls == null) {
            return null;
        }

        ViewParent t = view.getParent();
        if (t == null) {
            return null;
        }

        if (cls.isInstance(t)) {
            return cls.cast(t);
        }

        while (t.getParent() != null) {
            t = t.getParent();
            if (cls.isInstance(t)) {
                return cls.cast(t);
            }
        }
        return null;
    }
}
