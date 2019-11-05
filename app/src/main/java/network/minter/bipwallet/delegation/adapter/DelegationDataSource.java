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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.paging.DataSource;
import androidx.paging.PageKeyedDataSource;
import io.reactivex.disposables.CompositeDisposable;
import network.minter.bipwallet.internal.adapter.DataSourceMeta;
import network.minter.bipwallet.internal.adapter.LoadState;
import network.minter.core.MinterSDK;
import network.minter.core.crypto.MinterAddress;
import network.minter.explorer.models.DelegationInfo;
import network.minter.explorer.models.ExpResult;
import network.minter.explorer.repo.ExplorerAddressRepository;
import timber.log.Timber;

import static com.google.common.base.MoreObjects.firstNonNull;
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
                    item.coins.add(new DelegationItem.DelegatedCoin(info.coin, info.value, info.bipValue));
                    item.delegatedBips = item.delegatedBips.add(info.bipValue);
                    break;
                }
            }
            if (isPresent) continue;
            DelegationItem item = new DelegationItem();
            item.pubKey = info.pubKey;
            item.coins.add(new DelegationItem.DelegatedCoin(info.coin, info.value, info.bipValue));
            item.delegatedBips = info.bipValue;
            if (info.meta != null) {
                item.name = firstNonNull(info.meta.name, item.pubKey.toShortString());
                item.icon = info.meta.iconUrl;
                item.description = info.meta.description;
            }
            data.add(item);
        }

        final DataSourceMeta<DelegationItem> meta = new DataSourceMeta<>();
        for (DelegationItem item : data) {
            Collections.sort(item.coins, (o1, o2) -> o2.amountBIP.compareTo(o1.amountBIP));
        }
        Collections.sort(data, (o1, o2) -> o2.delegatedBips.compareTo(o1.delegatedBips));
        meta.setItems(data);
        meta.setMeta(res.getMeta());

        return meta;
    }

    public static class StableCoinSorting implements Comparator<DelegationItem.DelegatedCoin> {
        private final static String sStable = MinterSDK.DEFAULT_COIN.toLowerCase();

        @Override
        public int compare(DelegationItem.DelegatedCoin ac, DelegationItem.DelegatedCoin bc) {
            final String a = ac.coin.toLowerCase();
            final String b = bc.coin.toLowerCase();

            if (a.equals(b)) // update to make it stable
                return 0;
            if (a.equals(sStable))
                return -1;
            if (b.equals(sStable))
                return 1;

            return a.compareTo(b);
        }
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
