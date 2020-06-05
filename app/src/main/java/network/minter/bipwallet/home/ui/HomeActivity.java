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

package network.minter.bipwallet.home.ui;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.annimon.stream.Stream;
import com.ittianyu.bottomnavigationviewex.BottomNavigationViewEx;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import javax.inject.Inject;
import javax.inject.Provider;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import butterknife.BindView;
import butterknife.ButterKnife;
import moxy.presenter.InjectPresenter;
import moxy.presenter.ProvidePresenter;
import network.minter.bipwallet.R;
import network.minter.bipwallet.home.HomeModule;
import network.minter.bipwallet.home.HomeTabFragment;
import network.minter.bipwallet.home.HomeTabsClasses;
import network.minter.bipwallet.home.contract.HomeView;
import network.minter.bipwallet.home.views.HomePresenter;
import network.minter.bipwallet.internal.BaseMvpActivity;
import network.minter.bipwallet.internal.Wallet;
import network.minter.bipwallet.internal.system.ActivityBuilder;
import network.minter.bipwallet.internal.system.BackPressedDelegate;
import network.minter.bipwallet.internal.system.BackPressedListener;
import timber.log.Timber;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class HomeActivity extends BaseMvpActivity implements HomeView, BackPressedDelegate {

    @Inject Provider<HomePresenter> presenterProvider;
    @InjectPresenter HomePresenter presenter;
    @Inject @HomeTabsClasses List<Class<? extends HomeTabFragment>> tabsClasses;

    @BindView(R.id.navigation_bottom) BottomNavigationViewEx bottomNavigation;
    @BindView(R.id.home_pager) ViewPager2 homePager;

    private Map<Integer, HomeTabFragment> mActiveTabs = new WeakHashMap<>();
    private List<BackPressedListener> mBackPressedListeners = new ArrayList<>(1);
    private boolean mIsLowRamDevice = false;

    @VisibleForTesting
    public final Map<Integer, HomeTabFragment> getActiveTabs() {
        return mActiveTabs;
    }

    @Override
    public void setCurrentPage(int position) {
        runOnUiThread(() -> {
            try {
                homePager.setCurrentItem(position);
            } catch (IllegalStateException e) {
                Timber.w(e, "Unable to set current item");
            }
        });
    }

    public HomeTabFragment getCurrentTabFragment() {
        return mActiveTabs.get(homePager.getCurrentItem());
    }

    @Override
    public void startUrl(@NonNull String url) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }

    @Override
    public void startRemoteTransaction(String txHash) {

    }

    @Override
    public void addBackPressedListener(BackPressedListener listener) {
        mBackPressedListeners.add(listener);
    }

    @Override
    public void removeBackPressedListener(BackPressedListener listener) {
        mBackPressedListeners.remove(listener);
    }

    @Override
    public void clearBackPressedListeners() {
        mBackPressedListeners.clear();
    }

    @Override
    public void onBackPressed() {
        for (BackPressedListener listener : mBackPressedListeners) {
            if (!listener.onBackPressed()) {
                return;
            }
        }

        super.onBackPressed();
    }

    @Override
    public void showProgress() {

    }

    @Override
    public void hideProgress() {

    }

    public void setCurrentPageById(@IdRes int tab) {
        int pos = presenter.getBottomPositionById(tab);
        setCurrentPage(pos);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (homePager.getOffscreenPageLimit() > 1) {
            homePager.setOffscreenPageLimit(1);
        }
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        for (int i = 0; i < mActiveTabs.size(); i++) {
            if (mActiveTabs.get(i) != null && mActiveTabs.get(i) != null) {
                final HomeTabFragment f = mActiveTabs.get(i);
                if (f != null) {
                    f.onTrimMemory(level);
                }
            }
        }
    }

    @ProvidePresenter
    HomePresenter providePresenter() {
        return presenterProvider.get();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        for (Map.Entry<Integer, HomeTabFragment> entry : mActiveTabs.entrySet()) {
            if (entry.getValue() != null) {
                final HomeTabFragment f = entry.getValue();
                if (f != null) {
                    Timber.d("%s.onActivityResult", f.getClass());
                    f.onActivityResult(requestCode, resultCode, data);
                }

            } else {
                Timber.d("Fragment %d is null", entry.getKey());
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        HomeModule.destroy();
        Wallet.app().ledger().destroy();
        Timber.d("Destroy");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        HomeModule.create(this).inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        ButterKnife.bind(this);

        final ActivityManager am = ((ActivityManager) getSystemService(ACTIVITY_SERVICE));
        if (am != null) {
            mIsLowRamDevice = am.isLowRamDevice();
        }

        presenter.handleExtras(getIntent());

        setupTabAdapter();
        setupBottomNavigation();
    }

    private void setupTabAdapter() {
        final FragmentStateAdapter adapter = new FragmentStateAdapter(this) {
            @Override
            public int getItemCount() {
                return tabsClasses.size();
            }

            @NonNull
            @Override
            public Fragment createFragment(int position) {
                Timber.d("Get item by position: %d", position);
                HomeTabFragment fragment = null;
                try {
                    fragment = tabsClasses.get(position).newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    e.printStackTrace();
                }
                if (fragment == null) {
                    throw new NullPointerException("Wtf?");
                }

                if (getIntent() != null && getIntent().getExtras() != null) {
                    fragment.setArguments(getIntent().getExtras());
                }
                mActiveTabs.put(position, fragment);

                return fragment;
            }

            @Override
            public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
                super.onDetachedFromRecyclerView(recyclerView);

            }
        };

        homePager.setOffscreenPageLimit(mIsLowRamDevice ? 1 : 4);
        homePager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
            }

            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                presenter.onPageSelected(position);
                Stream.of(mActiveTabs.entrySet())
                        .filter(item -> item.getValue() != null)
                        .filter(item -> item.getKey() != position)
                        .forEach(item -> item.getValue().onTabUnselected());

                if (mActiveTabs.get(position) != null && mActiveTabs.get(position) != null) {
                    final HomeTabFragment f = mActiveTabs.get(position);
                    if (f != null) {
                        f.onTabSelected();
                    }
                }

                bottomNavigation.setSelectedItemId(presenter.getBottomIdByPosition(position));
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
            }
        });
        homePager.setAdapter(adapter);
    }

    private void setupBottomNavigation() {
        bottomNavigation.setLabelVisibilityMode(1);
        bottomNavigation.setItemHorizontalTranslationEnabled(false);
        bottomNavigation.enableAnimation(true);
        bottomNavigation.setTextVisibility(true);


        bottomNavigation.setOnNavigationItemSelectedListener(item -> {
            runOnUiThread(() -> homePager.setCurrentItem(presenter.getBottomPositionById(item.getItemId())));
            return true;
        });
    }

    public static final class Builder extends ActivityBuilder {
        public Builder(@NonNull Activity from) {
            super(from);
        }

        public Builder(@NonNull Fragment from) {
            super(from);
        }

        public Builder(@NonNull Service from) {
            super(from);
        }

        @Override
        protected Class<?> getActivityClass() {
            return HomeActivity.class;
        }
    }
}
