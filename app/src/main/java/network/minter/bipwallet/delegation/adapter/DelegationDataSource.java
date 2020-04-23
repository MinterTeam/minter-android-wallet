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

package network.minter.bipwallet.delegation.adapter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.paging.DataSource;
import androidx.paging.PageKeyedDataSource;
import io.reactivex.disposables.CompositeDisposable;
import network.minter.bipwallet.internal.adapter.LoadState;
import network.minter.bipwallet.internal.helpers.data.CollectionsHelper;
import network.minter.core.MinterSDK;
import network.minter.core.crypto.MinterAddress;
import network.minter.core.crypto.MinterPublicKey;
import network.minter.explorer.models.CoinDelegation;
import network.minter.explorer.models.DelegationList;
import network.minter.explorer.models.ExpResult;
import network.minter.explorer.repo.ExplorerAddressRepository;
import timber.log.Timber;

import static com.google.common.base.MoreObjects.firstNonNull;
import static network.minter.bipwallet.apis.reactive.ReactiveExplorer.rxExp;
import static network.minter.bipwallet.apis.reactive.ReactiveExplorer.toExpError;

/**
 * Created by Alexander Kolpakov (jquickapp@gmail.com) on 06-Jun-19
 */
public class DelegationDataSource extends PageKeyedDataSource<Integer, DelegatedItem> {
    private final DelegationDataSource.Factory factory;
    private CompositeDisposable mDisposables = new CompositeDisposable();
    private Map<MinterPublicKey, BigDecimal> mValidatorTotalStakes = new HashMap<>();

    public DelegationDataSource(DelegationDataSource.Factory factory) {
        this.factory = factory;
    }

    @Override
    public void loadInitial(@NonNull LoadInitialParams<Integer> params,
                            @NonNull LoadInitialCallback<Integer, DelegatedItem> callback) {
        factory.loadState.postValue(LoadState.Loading);
        if (factory.mAddressList == null || factory.mAddressList.isEmpty()) {
            Timber.w("Unable to load transactions list(page: 1): user address list is empty");
            factory.loadState.postValue(LoadState.Loaded);
            callback.onResult(Collections.emptyList(), null, null);
            factory.loadState.postValue(LoadState.Empty);
            return;
        }

        rxExp(factory.mRepo.getDelegations(factory.mAddressList.get(0), 1))
                .onErrorResumeNext(toExpError())
                .map(this::mapToDelegationItem)
                .doOnSubscribe(d -> mDisposables.add(d))
                .subscribe(res -> {
                    factory.loadState.postValue(LoadState.Loaded);
                    callback.onResult(res.result, null,
                            res.getMeta().lastPage == 1 ? null : res.getMeta().currentPage + 1);
                    if (res.result.isEmpty()) {
                        factory.loadState.postValue(LoadState.Empty);
                    }
                });
    }

    @Override
    public void loadBefore(@NonNull LoadParams<Integer> params,
                           @NonNull LoadCallback<Integer, DelegatedItem> callback) {
        factory.loadState.postValue(LoadState.Loading);
        if (factory.mAddressList == null || factory.mAddressList.isEmpty()) {
            Timber.w("Unable to load previous transactions list (page: %s): user address list is empty", params.key);
            factory.loadState.postValue(LoadState.Loaded);
            callback.onResult(Collections.emptyList(), null);
            return;
        }
        rxExp(factory.mRepo.getDelegations(factory.mAddressList.get(0), params.key))
                .onErrorResumeNext(toExpError())
                .map(this::mapToDelegationItem)
                .doOnSubscribe(d -> mDisposables.add(d))
                .subscribe(res -> {
                    factory.loadState.postValue(LoadState.Loaded);
                    callback.onResult(res.result, params.key == 1 ? null : params.key - 1);
                });
    }

    @Override
    public void loadAfter(@NonNull LoadParams<Integer> params,
                          @NonNull LoadCallback<Integer, DelegatedItem> callback) {
        factory.loadState.postValue(LoadState.Loading);
        rxExp(factory.mRepo.getDelegations(factory.mAddressList.get(0), params.key))
                .onErrorResumeNext(toExpError())
                .map(this::mapToDelegationItem)
                .doOnSubscribe(d -> mDisposables.add(d))
                .subscribe(res -> {
                    factory.loadState.postValue(LoadState.Loaded);
                    callback.onResult(res.result, params.key + 1 > res.getMeta().lastPage ? null : params.key + 1);
                });
    }

    @Override
    public void invalidate() {
        super.invalidate();
        mDisposables.dispose();
    }

    private ExpResult<List<DelegatedItem>> mapToDelegationItem(ExpResult<DelegationList> res) {
        ExpResult<List<DelegatedItem>> out = new ExpResult<>();
        out.meta = res.meta;
        out.error = res.error;
        out.latestBlockTime = res.latestBlockTime;
        out.links = res.links;
        out.result = new ArrayList<>();

        List<DelegatedStake> stakes = new ArrayList<>();

        if (res.result != null) {
            for (Map.Entry<MinterPublicKey, List<CoinDelegation>> info : res.result.getDelegations().entrySet()) {
                for (CoinDelegation item : info.getValue()) {
                    DelegatedStake stake = new DelegatedStake(item);
                    stakes.add(stake);
                    if (mValidatorTotalStakes.containsKey(stake.pubKey)) {
                        BigDecimal val = firstNonNull(mValidatorTotalStakes.get(stake.pubKey), BigDecimal.ZERO);
                        val = val.add(stake.amountBIP);
                        mValidatorTotalStakes.put(stake.pubKey, val);
                    } else {
                        mValidatorTotalStakes.put(stake.pubKey, firstNonNull(stake.amountBIP, BigDecimal.ZERO));
                    }
                }
            }
        }

        mValidatorTotalStakes = CollectionsHelper.sortByValue(mValidatorTotalStakes, (o1, o2) -> o2.compareTo(o1));

        for (Map.Entry<MinterPublicKey, BigDecimal> validatorStake : mValidatorTotalStakes.entrySet()) {
            List<DelegatedStake> outStakes = new ArrayList<>();

            for (DelegatedStake s : stakes) {
                if (s.pubKey.equals(validatorStake.getKey())) {
                    outStakes.add(s);
                }
            }

            if (outStakes.isEmpty()) {
                continue;
            }

            Collections.sort(outStakes, (o1, o2) -> o2.amountBIP.compareTo(o1.amountBIP));

            DelegatedValidator validator = new DelegatedValidator(validatorStake.getKey(), outStakes.get(0).validatorMeta);
            out.result.add(validator);
            out.result.addAll(outStakes);
        }

        return out;
    }

    public static class StableCoinSorting implements Comparator<DelegatedStake> {
        private final static String sStable = MinterSDK.DEFAULT_COIN.toLowerCase();

        @Override
        public int compare(DelegatedStake ac, DelegatedStake bc) {
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

    public static class Factory extends DataSource.Factory<Integer, DelegatedItem> {
        private ExplorerAddressRepository mRepo;
        private List<MinterAddress> mAddressList;
        private MutableLiveData<LoadState> loadState;

        public Factory(ExplorerAddressRepository repo, List<MinterAddress> addresses,
                       MutableLiveData<LoadState> loadState) {
            mRepo = repo;
            mAddressList = addresses;
            this.loadState = loadState;
        }

        @Override
        public DataSource<Integer, DelegatedItem> create() {
            return new DelegationDataSource(this);
        }
    }
}
