/*
 * Copyright (C) by MinterTeam. 2020
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

package network.minter.bipwallet.home;

import android.app.FragmentManager;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import androidx.lifecycle.LifecycleOwner;
import dagger.Module;
import dagger.Provides;
import network.minter.bipwallet.home.ui.HomeActivity;
import network.minter.bipwallet.internal.Wallet;
import network.minter.bipwallet.internal.system.BackPressedDelegate;
import network.minter.bipwallet.sending.ui.SendTabFragment;
import network.minter.bipwallet.settings.ui.SettingsTabFragment;
import network.minter.bipwallet.wallets.ui.WalletsTabFragment;

/**
 * minter-android-wallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@Module
public class HomeModule {
    public final static String EXTRA_TAB = "EXTRA_TAB";
    public final static String EXTRA_MENU_ID = "EXTRA_MENU_ID";
    public final static String TAB_COINS = WalletsTabFragment.class.getName();
    public final static String TAB_SENDING = SendTabFragment.class.getName();
    public final static String TAB_SETTINGS = SettingsTabFragment.class.getName();
    private static HomeComponent component = null;
    private final List<Class<? extends HomeTabFragment>> tabsClassesClient = new ArrayList<Class<? extends HomeTabFragment>>() {
        {
            add(WalletsTabFragment.class);
            add(SendTabFragment.class);
            add(SettingsTabFragment.class);
        }
    };
    private final WeakReference<HomeActivity> mActivity;

    public HomeModule(HomeActivity activity) {
        mActivity = new WeakReference<>(activity);
    }

    public static HomeComponent create(HomeActivity rootView) {
        component = DaggerHomeComponent.builder()
                .walletComponent(Wallet.app())
                .homeModule(new HomeModule(rootView))
                .build();

        return component;
    }

    public static void destroy() {
        component = null;
    }

    @Provides
    @HomeScope
    public BackPressedDelegate provideBackPressDelegate(HomeActivity activity) {
        return activity;
    }

    public static HomeComponent getComponent() {
        return component;
    }

    @Provides
    @HomeScope
    public LifecycleOwner provideLifecycleOwner(HomeActivity activity) {
        return activity;
    }

    @Provides
    @HomeScope
    public HomeActivity provideHomeActivity() {
        return mActivity.get();
    }

    @Provides
    @HomeScope
    public FragmentManager provideFragmentManager(HomeActivity activity) {
        return activity.getFragmentManager();
    }

    @Provides
    @HomeScope
    public androidx.fragment.app.FragmentManager provideSupportFragmentManager(HomeActivity activity) {
        return activity.getSupportFragmentManager();
    }

    @Provides
    @HomeTabsClasses
    @HomeScope
    public List<Class<? extends HomeTabFragment>> provideTabsClasses() {
        return tabsClassesClient;
    }

}
