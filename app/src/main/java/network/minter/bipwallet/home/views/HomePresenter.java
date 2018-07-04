/*
 * Copyright (C) 2018 by MinterTeam
 * @link https://github.com/MinterTeam
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

package network.minter.bipwallet.home.views;

import android.view.MenuItem;

import com.arellomobile.mvp.InjectViewState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import network.minter.bipwallet.R;
import network.minter.bipwallet.home.HomeModule;
import network.minter.bipwallet.home.HomeTabFragment;
import network.minter.bipwallet.home.HomeTabsClasses;
import network.minter.bipwallet.internal.mvp.MvpBasePresenter;

/**
 * MinterWallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@InjectViewState
public class HomePresenter extends MvpBasePresenter<HomeModule.HomeView> {


    private final HashMap<Integer, Integer> mClientBottomIdPositionMap = new HashMap<Integer, Integer>() {{
        put(R.id.bottom_coins, 0);
        put(R.id.bottom_send, 1);
        put(R.id.bottom_receive, 2);
        put(R.id.bottom_settings, 3);
    }};

    @Inject @HomeTabsClasses
    List<Class<? extends network.minter.bipwallet.home.HomeTabFragment>> tabsClasses;

    private int mLastPosition = 0;

    @Inject
    public HomePresenter() {

    }

    public void onPageSelected(int position) {
        mLastPosition = position;
    }

    public int getBottomIdByPosition(int position) {
        for (Map.Entry<Integer, Integer> item : mClientBottomIdPositionMap.entrySet()) {
            if (item.getValue() == position) {
                return item.getKey();
            }
        }

        return 0;
    }

    public int getTabPosition(String name) {
        int position = 0;
        for (Class<? extends HomeTabFragment> cls : tabsClasses) {
            if (cls.getName().equals(name)) {
                break;
            }
            position++;
        }

        return position;
    }

    public void onNavigationBottomItemSelected(MenuItem item) {

    }

    public int getBottomPositionById(int itemId) {
        return mClientBottomIdPositionMap.get(itemId);
    }
}
