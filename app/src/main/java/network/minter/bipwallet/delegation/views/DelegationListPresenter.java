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

package network.minter.bipwallet.delegation.views;

import android.arch.lifecycle.MutableLiveData;
import android.arch.paging.PagedList;
import android.arch.paging.RxPagedListBuilder;

import com.arellomobile.mvp.InjectViewState;

import javax.inject.Inject;

import io.reactivex.disposables.Disposable;
import network.minter.bipwallet.advanced.repo.SecretStorage;
import network.minter.bipwallet.coins.CoinsTabModule;
import network.minter.bipwallet.delegation.adapter.DelegationDataSource;
import network.minter.bipwallet.delegation.adapter.DelegationItem;
import network.minter.bipwallet.delegation.adapter.DelegationListAdapter;
import network.minter.bipwallet.internal.adapter.LoadState;
import network.minter.bipwallet.internal.mvp.MvpBasePresenter;
import network.minter.explorer.models.DelegationInfo;
import network.minter.explorer.repo.ExplorerAddressRepository;

/**
 * Created by Alexander Kolpakov (jquickapp@gmail.com) on 05-Jun-19
 */
@InjectViewState
public class DelegationListPresenter extends MvpBasePresenter<CoinsTabModule.DelegationListView> {

    @Inject
    ExplorerAddressRepository addressRepo;
    @Inject
    SecretStorage secretRepo;

    private DelegationListAdapter mAdapter;
    private DelegationDataSource.Factory mSourceFactory;
    private Disposable mListDisposable;
    private RxPagedListBuilder<Integer, DelegationItem> listBuilder;
    private int mLastPosition = 0;
    private MutableLiveData<LoadState> mLoadState;

    @Inject
    public DelegationListPresenter() {
    }


    @Override
    public void attachView(CoinsTabModule.DelegationListView view) {
        super.attachView(view);
        getViewState().setAdapter(mAdapter);
        getViewState().setOnRefreshListener(this::onRefresh);
        getViewState().scrollTo(mLastPosition);
    }

    public void onScrolledTo(int position) {
        mLastPosition = position;
    }

    @Override
    protected void onFirstViewAttach() {
        super.onFirstViewAttach();
        mAdapter = new DelegationListAdapter();

        mLoadState = new MutableLiveData<>();
        getViewState().syncProgress(mLoadState);
        mAdapter.setLoadState(mLoadState);
        mSourceFactory = new DelegationDataSource.Factory(addressRepo, secretRepo.getAddresses(), mLoadState);
        PagedList.Config cfg = new PagedList.Config.Builder()
                .setPageSize(50)
                .setEnablePlaceholders(false)
                .build();


        listBuilder = new RxPagedListBuilder<>(mSourceFactory, cfg);
        refresh();

        unsubscribeOnDestroy(mListDisposable);
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
}
