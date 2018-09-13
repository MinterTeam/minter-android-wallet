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

package network.minter.bipwallet.ui.actions;

import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.support.test.espresso.matcher.ViewMatchers;
import android.view.View;

import com.ittianyu.bottomnavigationviewex.BottomNavigationViewEx;

import org.hamcrest.Matcher;

import static android.support.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
public class BottomNavigationExAction implements ViewAction {
    private final static int ACT_SELECT = 0;

    private int mItemPos;
    private int mAction;

    private BottomNavigationExAction(int action) {
        mAction = action;
    }

    public static BottomNavigationExAction selectCurrentItem(int item) {
        return new BottomNavigationExAction(ACT_SELECT).setSelectItem(item);
    }

    @Override
    public Matcher<View> getConstraints() {
        return withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE);
    }

    @Override
    public String getDescription() {
        return "Bottom Navigation Ex (custom)";
    }

    @Override
    public void perform(UiController uiController, View view) {
        final BottomNavigationViewEx nv = ((BottomNavigationViewEx) view);
        switch (mAction) {
            case ACT_SELECT:
                nv.setCurrentItem(mItemPos);
                break;
        }

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private BottomNavigationExAction setSelectItem(int item) {
        mItemPos = item;
        return this;
    }
}
