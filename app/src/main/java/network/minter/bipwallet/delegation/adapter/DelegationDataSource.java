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

package network.minter.bipwallet.delegation.adapter;

import android.arch.lifecycle.MutableLiveData;
import android.arch.paging.DataSource;
import android.arch.paging.PageKeyedDataSource;
import android.support.annotation.NonNull;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import io.reactivex.disposables.CompositeDisposable;
import network.minter.bipwallet.internal.adapter.DataSourceMeta;
import network.minter.bipwallet.internal.adapter.LoadState;
import network.minter.bipwallet.internal.helpers.DateHelper;
import network.minter.bipwallet.tx.adapters.HeaderItem;
import network.minter.bipwallet.tx.adapters.TransactionDataSource;
import network.minter.bipwallet.tx.adapters.TransactionItem;
import network.minter.bipwallet.tx.adapters.TxItem;
import network.minter.core.crypto.MinterAddress;
import network.minter.explorer.models.DelegationInfo;
import network.minter.explorer.models.ExpResult;
import network.minter.explorer.models.HistoryTransaction;
import network.minter.explorer.repo.ExplorerAddressRepository;
import timber.log.Timber;

import static network.minter.bipwallet.apis.reactive.ReactiveExplorer.rxExp;
import static network.minter.bipwallet.apis.reactive.ReactiveExplorer.toExpError;

/**
 * Created by Alexander Kolpakov (jquickapp@gmail.com) on 06-Jun-19
 */
public class DelegationDataSource extends PageKeyedDataSource<Integer, DelegationItem> {

    private ExplorerAddressRepository mRepo;
    private List<MinterAddress> mAddressList;
    private CompositeDisposable mDisposables = new CompositeDisposable();
    private MutableLiveData<LoadState> mLoadState;
    private List<DelegationItem> data = new ArrayList<>();

    public DelegationDataSource(ExplorerAddressRepository repo, List<MinterAddress> addresses,
                                MutableLiveData<LoadState> loadState) {
        mRepo = repo;
        mAddressList = addresses;
        mLoadState = loadState;
    }

    @Override
    public void loadInitial(@NonNull LoadInitialParams<Integer> params,
                            @NonNull LoadInitialCallback<Integer, DelegationItem> callback) {
        mLoadState.postValue(LoadState.Loading);
        if (mAddressList == null || mAddressList.isEmpty()) {
            Timber.w("Unanble to load transactions list(page: 1): user address list is empty");
            mLoadState.postValue(LoadState.Loaded);
            callback.onResult(Collections.emptyList(), null, null);
            return;
        }

        rxExp(mRepo.getDelegations(mAddressList.get(0), 1))
                .onErrorResumeNext(toExpError())
                .map(this::mapToDelegationItem)
                .doOnSubscribe(d -> mDisposables.add(d))
                .subscribe(res -> {
                    mLoadState.postValue(LoadState.Loaded);
                    callback.onResult(res.getItems(), null,
                            res.getMeta().lastPage == 1 ? null : res.getMeta().currentPage + 1);
                });
    }

    @Override
    public void loadBefore(@NonNull LoadParams<Integer> params,
                           @NonNull LoadCallback<Integer, DelegationItem> callback) {
        mLoadState.postValue(LoadState.Loading);
        if (mAddressList == null || mAddressList.isEmpty()) {
            Timber.w("Unable to load previous transactions list (page: %s): user address list is empty", params.key);
            mLoadState.postValue(LoadState.Loaded);
            callback.onResult(Collections.emptyList(), null);
            return;
        }
        rxExp(mRepo.getDelegations(mAddressList.get(0), params.key))
                .onErrorResumeNext(toExpError())
                .map(this::mapToDelegationItem)
                .doOnSubscribe(d -> mDisposables.add(d))
                .subscribe(res -> {
                    mLoadState.postValue(LoadState.Loaded);
                    callback.onResult(res.getItems(), params.key == 1 ? null : params.key - 1);
                });
    }

    @Override
    public void loadAfter(@NonNull LoadParams<Integer> params,
                          @NonNull LoadCallback<Integer, DelegationItem> callback) {
        mLoadState.postValue(LoadState.Loading);
        rxExp(mRepo.getDelegations(mAddressList.get(0), params.key))
                .onErrorResumeNext(toExpError())
                .map(this::mapToDelegationItem)
                .doOnSubscribe(d -> mDisposables.add(d))
                .subscribe(res -> {
                    mLoadState.postValue(LoadState.Loaded);
                    callback.onResult(res.getItems(), params.key + 1 > res.getMeta().lastPage ? null : params.key + 1);
                });
    }

    private DataSourceMeta<DelegationItem> mapToDelegationItem(ExpResult<List<DelegationInfo>> res) {
        for (DelegationInfo info : res.result) {
            boolean isPresent = false;
            for (DelegationItem item : data) {
                if(item.pubKey.toString().equals(info.pubKey.toString())){
                    isPresent = true;
                    item.coins.add(new DelegationItem.DelegatedCoin(info.coin, info.value));
                    break;
                }
            }
            if (isPresent) continue;
            DelegationItem item = new DelegationItem();
            item.pubKey = info.pubKey;
            item.coins.add(new DelegationItem.DelegatedCoin(info.coin, info.value));
            data.add(item);
        }

        final DataSourceMeta<DelegationItem> meta = new DataSourceMeta<>();
        meta.setItems(data);
        meta.setMeta(res.getMeta());

        return meta;
    }

    @Override
    public void invalidate() {
        super.invalidate();
        mDisposables.dispose();
    }

    public static class Factory extends DataSource.Factory<Integer, DelegationItem> {
        private ExplorerAddressRepository mRepo;
        private List<MinterAddress> mAddressList;
        private MutableLiveData<LoadState> mLoadState;

        public Factory(ExplorerAddressRepository repo, List<MinterAddress> addresses,
                       MutableLiveData<LoadState> loadState) {
            mRepo = repo;
            mAddressList = addresses;
            mLoadState = loadState;
        }

        @Override
        public DataSource<Integer, DelegationItem> create() {
            return new DelegationDataSource(mRepo, mAddressList, mLoadState);
        }
    }
}
