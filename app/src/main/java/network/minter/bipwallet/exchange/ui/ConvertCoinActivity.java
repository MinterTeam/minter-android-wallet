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

package network.minter.bipwallet.exchange.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;

import com.arellomobile.mvp.presenter.InjectPresenter;
import com.arellomobile.mvp.presenter.ProvidePresenter;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;

import butterknife.BindView;
import butterknife.ButterKnife;
import network.minter.bipwallet.BuildConfig;
import network.minter.bipwallet.R;
import network.minter.bipwallet.exchange.ExchangeModule;
import network.minter.bipwallet.exchange.views.ConvertCoinPresenter;
import network.minter.bipwallet.internal.BaseMvpInjectActivity;

/**
 * minter-android-wallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class ConvertCoinActivity extends BaseMvpInjectActivity implements ExchangeModule.ConvertCoinView {

    private final static List<Class<? extends BaseCoinTabFragment>> sTabs = new ArrayList<Class<? extends BaseCoinTabFragment>>() {{
        add(SpendCoinTabFragment.class);
        add(GetCoinTabFragment.class);
    }};

    @Inject
    Provider<ConvertCoinPresenter> presenterProvider;
    @InjectPresenter
    ConvertCoinPresenter presenter;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.tabs)
    TabLayout tabs;
    @BindView(R.id.pager)
    ViewPager tabsPager;
    @Nullable
    @BindView(R.id.testnet_warning)
    View testNetWarning;

    @VisibleForTesting
    public final BaseCoinTabFragment getTab(int position) {
        return (BaseCoinTabFragment) (((FragmentStatePagerAdapter) tabsPager.getAdapter()).getItem(position));
    }

    @Override
    public void setupTabs() {
        tabsPager.setAdapter(createTabsAdapter());
        tabsPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabs));
        tabsPager.setOffscreenPageLimit(2);
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                tabsPager.setCurrentItem(tab.getPosition());
                presenter.onTabSelected(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    @Override
    public void setCurrentPage(int page) {
        tabsPager.setCurrentItem(page);
    }

    @ProvidePresenter
    ConvertCoinPresenter providePresenter() {
        return presenterProvider.get();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_convert_coin);
        ButterKnife.bind(this);
        setupToolbar(toolbar);

        tabs.setTabGravity(TabLayout.GRAVITY_FILL);

        if (BuildConfig.FLAVOR.contains("netTest") && testNetWarning != null) {
            testNetWarning.setVisibility(View.VISIBLE);
            testNetWarning.setOnClickListener(v -> {
                Intent goToMarket = new Intent(Intent.ACTION_VIEW).setData(Uri.parse("market://details?id=network.minter.bipwallet.mainnet"));
                startActivity(goToMarket);
            });
        }
    }

    private FragmentStatePagerAdapter createTabsAdapter() {
        return new FragmentStatePagerAdapter(getSupportFragmentManager()) {
            @Override
            public int getCount() {
                return 2;
            }

            @Override
            public Fragment getItem(int position) {
                try {
                    return sTabs.get(position).newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    e.printStackTrace();
                }

                return null;
            }

            @NonNull
            @Override
            public Object instantiateItem(@NonNull ViewGroup container, int position) {
                return super.instantiateItem(container, position);
            }

            @Override
            public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
                super.destroyItem(container, position, object);
            }
        };
    }
}
