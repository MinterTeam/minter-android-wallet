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

package network.minter.bipwallet.tx.views;

import android.view.View;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.paging.PagedList;
import androidx.paging.RxPagedListBuilder;
import io.reactivex.disposables.Disposable;
import moxy.InjectViewState;
import network.minter.bipwallet.advanced.repo.SecretStorage;
import network.minter.bipwallet.analytics.AppEvent;
import network.minter.bipwallet.analytics.base.HasAnalyticsEvent;
import network.minter.bipwallet.internal.adapter.LoadState;
import network.minter.bipwallet.internal.mvp.MvpBasePresenter;
import network.minter.bipwallet.tx.adapters.TransactionDataSource;
import network.minter.bipwallet.tx.adapters.TransactionFacade;
import network.minter.bipwallet.tx.adapters.TransactionItem;
import network.minter.bipwallet.tx.adapters.TransactionListAdapter;
import network.minter.bipwallet.tx.contract.TransactionListView;
import network.minter.explorer.repo.ExplorerTransactionRepository;
import network.minter.profile.repo.ProfileInfoRepository;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@InjectViewState
public class TransactionListPresenter extends MvpBasePresenter<TransactionListView> implements HasAnalyticsEvent {

    @Inject ExplorerTransactionRepository transactionRepo;
    @Inject SecretStorage secretRepo;
    @Inject ProfileInfoRepository infoRepo;
    @Inject TransactionDataSource.Factory sourceFactory;
    private TransactionListAdapter mAdapter;

    private Disposable mListDisposable;
    private RxPagedListBuilder<Integer, TransactionItem> listBuilder;
    private int mLastPosition = 0;
    private MutableLiveData<LoadState> mLoadState;

    @Inject
    public TransactionListPresenter() {
    }

    @Override
    public void attachView(TransactionListView view) {
        super.attachView(view);
        getViewState().setAdapter(mAdapter);
        getViewState().setOnRefreshListener(this::onRefresh);
        getViewState().scrollTo(mLastPosition);
    }

    public void onScrolledTo(int position) {
        mLastPosition = position;
    }

    @NonNull
    @Override
    public AppEvent getAnalyticsEvent() {
        return AppEvent.TransactionsScreen;
    }

    @Override
    protected void onFirstViewAttach() {
        super.onFirstViewAttach();
        mAdapter = new TransactionListAdapter(secretRepo.getAddresses());

        mAdapter.setOnExplorerOpenClickListener(this::onExplorerClick);
        mAdapter.setOnExpandDetailsListener(this::onExpandTx);
        mLoadState = new MutableLiveData<>();
        getViewState().syncProgress(mLoadState);
        mAdapter.setLoadState(mLoadState);
        sourceFactory.observeLoadState(mLoadState);
        PagedList.Config cfg = new PagedList.Config.Builder()
                .setPageSize(50)
                .setEnablePlaceholders(false)
                .build();


        listBuilder = new RxPagedListBuilder<>(sourceFactory, cfg);
        refresh();

        unsubscribeOnDestroy(mListDisposable);
    }

    private void onExpandTx(View view, TransactionFacade tx) {
        getAnalytics().send(AppEvent.TransactionDetailsButton);
    }

    private void onRefresh() {
        mListDisposable.dispose();
        getViewState().scrollTo(0);
        refresh();
    }

    private void refresh() {
        mListDisposable = listBuilder.buildObservable()
                .doOnSubscribe(this::unsubscribeOnDestroy)
                .subscribe(res -> {
                    getViewState().hideRefreshProgress();
                    mAdapter.submitList(res);
                });
    }

    private void onExplorerClick(View view, TransactionFacade historyTransaction) {
        getViewState().startExplorer(historyTransaction.getHash().toString());
        getAnalytics().send(AppEvent.TransactionExplorerButton);
    }
}
