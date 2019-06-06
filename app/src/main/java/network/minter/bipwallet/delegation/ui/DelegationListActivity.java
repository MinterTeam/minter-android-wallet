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

package network.minter.bipwallet.delegation.ui;

import android.app.Activity;
import android.app.Service;
import android.arch.lifecycle.MutableLiveData;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ProgressBar;
import com.arellomobile.mvp.presenter.InjectPresenter;
import com.arellomobile.mvp.presenter.ProvidePresenter;

import javax.inject.Inject;
import javax.inject.Provider;
import butterknife.BindView;
import butterknife.ButterKnife;
import network.minter.bipwallet.R;
import network.minter.bipwallet.coins.CoinsTabModule;
import network.minter.bipwallet.delegation.adapter.DelegationDataSource;
import network.minter.bipwallet.delegation.views.DelegationListPresenter;
import network.minter.bipwallet.internal.BaseMvpInjectActivity;
import network.minter.bipwallet.internal.adapter.LoadState;
import network.minter.bipwallet.internal.helpers.ContextHelper;
import network.minter.bipwallet.internal.system.ActivityBuilder;
import network.minter.bipwallet.tx.adapters.TransactionDataSource;
import network.minter.bipwallet.tx.ui.TransactionListActivity;
import network.minter.bipwallet.tx.views.TransactionListPresenter;

/**
 * Created by Alexander Kolpakov (jquickapp@gmail.com) on 05-Jun-19
 */
public class DelegationListActivity extends BaseMvpInjectActivity implements CoinsTabModule.DelegationListView {

    @Inject
    Provider<DelegationListPresenter> presenterProvider;
    @InjectPresenter
    DelegationListPresenter presenter;

    @ProvidePresenter
    DelegationListPresenter providePresenter() {
        return presenterProvider.get();
    }

    @BindView(R.id.list) RecyclerView list;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.progress)
    ProgressBar progress;
    @BindView(R.id.container_swipe_refresh) SwipeRefreshLayout swipeRefreshLayout;
    @Nullable
    @BindView(R.id.testnet_warning)
    View testNetWarning;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delegation_list);
        ButterKnife.bind(this);
        setupToolbar(toolbar);
        presenter.handleExtras(getIntent());
        list.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                presenter.onScrolledTo(((LinearLayoutManager) recyclerView.getLayoutManager()).findFirstVisibleItemPosition());
            }
        });
        ContextHelper.showTestnetBanner(this, testNetWarning);
    }

    @Override
    public void setAdapter(RecyclerView.Adapter<?> adapter) {
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
    }

    @Override
    public void setOnRefreshListener(SwipeRefreshLayout.OnRefreshListener listener) {
        swipeRefreshLayout.setOnRefreshListener(listener);
    }

    @Override
    public void showRefreshProgress() {
        swipeRefreshLayout.setRefreshing(true);
    }

    @Override
    public void hideRefreshProgress() {
        swipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void showProgress() {
        progress.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideProgress() {
        progress.setVisibility(View.GONE);
    }

    @Override
    public void scrollTo(int pos) {

    }

    @Override
    public void syncProgress(MutableLiveData<LoadState> loadState) {
        loadState.observe(this, s -> {
            if (s == null) {
                showProgress();
                return;
            }
            switch (s) {
                case Loaded:
                case Failed:
                    hideRefreshProgress();
                    hideProgress();
                    break;
                case Loading:
                    showProgress();
                    break;
            }
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
            return DelegationListActivity.class;
        }
    }
}
