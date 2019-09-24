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

package network.minter.bipwallet.tx.ui;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;

import javax.inject.Inject;
import javax.inject.Provider;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import butterknife.BindView;
import butterknife.ButterKnife;
import moxy.presenter.InjectPresenter;
import moxy.presenter.ProvidePresenter;
import network.minter.bipwallet.R;
import network.minter.bipwallet.internal.BaseMvpInjectActivity;
import network.minter.bipwallet.internal.adapter.LoadState;
import network.minter.bipwallet.internal.helpers.ContextHelper;
import network.minter.bipwallet.internal.system.ActivityBuilder;
import network.minter.bipwallet.tx.contract.TransactionListView;
import network.minter.bipwallet.tx.views.TransactionListPresenter;
import network.minter.explorer.MinterExplorerApi;

/**
 * minter-android-wallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class TransactionListActivity extends BaseMvpInjectActivity implements TransactionListView {

    @Inject Provider<TransactionListPresenter> presenterProvider;
    @InjectPresenter TransactionListPresenter presenter;

    @BindView(R.id.list) RecyclerView list;
    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.progress) ProgressBar progress;
    @BindView(R.id.container_swipe_refresh) SwipeRefreshLayout swipeRefreshLayout;
    @Nullable
    @BindView(R.id.testnet_warning) View testNetWarning;

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
    public void scrollTo(int pos) {
        if (pos < 0) {
            return;
        }

        list.scrollToPosition(pos);
    }

    @Override
    public void startExplorer(String hash) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(MinterExplorerApi.FRONT_URL + "/transactions/" + hash)));
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

    @Override
    public void showProgress() {
        progress.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideProgress() {
        progress.setVisibility(View.GONE);
    }

    @ProvidePresenter
    TransactionListPresenter providePresenter() {
        return presenterProvider.get();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_list);
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
            return TransactionListActivity.class;
        }
    }
}
