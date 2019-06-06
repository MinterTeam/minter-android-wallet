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

package network.minter.bipwallet.tx.adapters;

import android.arch.lifecycle.MutableLiveData;
import android.arch.paging.DataSource;
import android.arch.paging.PageKeyedDataSource;
import android.support.annotation.NonNull;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.disposables.CompositeDisposable;
import network.minter.bipwallet.R;
import network.minter.bipwallet.delegation.adapter.DelegationItem;
import network.minter.bipwallet.internal.Wallet;
import network.minter.bipwallet.internal.adapter.DataSourceMeta;
import network.minter.bipwallet.internal.adapter.LoadState;
import network.minter.bipwallet.internal.helpers.DateHelper;
import network.minter.core.crypto.MinterAddress;
import network.minter.explorer.models.ExpResult;
import network.minter.explorer.models.HistoryTransaction;
import network.minter.explorer.repo.ExplorerTransactionRepository;
import network.minter.profile.models.AddressInfoResult;
import network.minter.profile.repo.ProfileInfoRepository;
import timber.log.Timber;

import static network.minter.bipwallet.apis.reactive.ReactiveExplorer.rxExp;
import static network.minter.bipwallet.apis.reactive.ReactiveExplorer.toExpError;
import static network.minter.bipwallet.apis.reactive.ReactiveMyMinter.rxProfile;
import static network.minter.bipwallet.apis.reactive.ReactiveMyMinter.toProfileError;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class TransactionDataSource extends PageKeyedDataSource<Integer, TransactionItem> {

    private ExplorerTransactionRepository mRepo;
    private ProfileInfoRepository mInfoRepo;
    private List<MinterAddress> mAddressList;
    private CompositeDisposable mDisposables = new CompositeDisposable();
    private DateTime mLastDate;
    private MutableLiveData<LoadState> mLoadState;

    public TransactionDataSource(ExplorerTransactionRepository repo, ProfileInfoRepository infoRepo, List<MinterAddress> addresses, MutableLiveData<LoadState> loadState) {
        mRepo = repo;
        mInfoRepo = infoRepo;
        mAddressList = addresses;
        mLoadState = loadState;
    }

    public static ObservableSource<ExpResult<List<HistoryTransaction>>> mapAddressesInfo(List<MinterAddress> myAddresses, ProfileInfoRepository infoRepo, ExpResult<List<HistoryTransaction>> items) {
        if (items.result == null || items.result.isEmpty()) {
            return Observable.just(items);
        }

        List<MinterAddress> toFetch = new ArrayList<>();
        final Map<MinterAddress, List<HistoryTransaction>> toFetchAddresses = new LinkedHashMap<>(items.result.size());
        for (HistoryTransaction tx : items.result) {
            final MinterAddress add;
            if (tx.type != HistoryTransaction.Type.Send) {
                continue;
            }

            if (tx.isIncoming(myAddresses)) {
                add = tx.getFrom();
            } else {
                add = tx.<HistoryTransaction.TxSendCoinResult>getData().getTo();
            }

            if (add != null && !toFetch.contains(add)) {
                toFetch.add(add);
            }

            if (!toFetchAddresses.containsKey(add)) {
                toFetchAddresses.put(add, new ArrayList<>());
            }
            toFetchAddresses.get(add).add(tx);
        }

        return rxProfile(infoRepo.getAddressesWithUserInfo(toFetch))
                .onErrorResumeNext(toProfileError())
                .map(listInfoResult -> {
                    if (listInfoResult.data.isEmpty()) {
                        return items;
                    }

                    for (AddressInfoResult info : listInfoResult.data) {
                        for (HistoryTransaction t : toFetchAddresses.get(info.address)) {
                            t.setUsername(info.user.username).setAvatar(info.user.getAvatar().getUrl());
                        }
                    }

                    return items;
                });
    }

    public static ObservableSource<List<HistoryTransaction>> mapAddressesInfo(List<MinterAddress> addresses, ProfileInfoRepository infoRepo, List<HistoryTransaction> items) {
        if (items == null || items.isEmpty()) {
            return Observable.just(Collections.emptyList());
        }

        List<MinterAddress> toFetch = new ArrayList<>();
        final Map<MinterAddress, List<HistoryTransaction>> toFetchAddresses = new LinkedHashMap<>(items.size());
        for (HistoryTransaction tx : items) {
            final MinterAddress add;
            if (tx.type != HistoryTransaction.Type.Send) {
                continue;
            }

            if (tx.isIncoming(addresses)) {
                add = tx.getFrom();
            } else {
                add = tx.<HistoryTransaction.TxSendCoinResult>getData().to;
            }

            if (add != null && !toFetch.contains(add)) {
                toFetch.add(add);
            }

            if (!toFetchAddresses.containsKey(add)) {
                toFetchAddresses.put(add, new ArrayList<>());
            }
            toFetchAddresses.get(add).add(tx);
        }

        return rxProfile(infoRepo.getAddressesWithUserInfo(toFetch))
                .onErrorResumeNext(toProfileError())
                .map(listInfoResult -> {
                    if (listInfoResult.data == null) {
                        listInfoResult.data = Collections.emptyList();
                    }
                    if (listInfoResult.data.isEmpty()) {
                        return items;
                    }

                    for (AddressInfoResult info : listInfoResult.data) {
                        for (HistoryTransaction t : toFetchAddresses.get(info.address)) {
                            t.setUsername(info.user.username).setAvatar(info.user.getAvatar().getUrl());
                        }
                    }

                    return items;
                });
    }

    @Override
    public void loadInitial(@NonNull LoadInitialParams<Integer> params, @NonNull LoadInitialCallback<Integer, TransactionItem> callback) {
        mLoadState.postValue(LoadState.Loading);
        if (mAddressList == null || mAddressList.isEmpty()) {
            Timber.w("Unanble to load transactions list(page: 1): user address list is empty");
            mLoadState.postValue(LoadState.Loaded);
            callback.onResult(Collections.emptyList(), null, null);
            return;
        }

        rxExp(mRepo.getTransactions(mAddressList, 1))
                .onErrorResumeNext(toExpError())
                .switchMap(items -> mapAddressesInfo(mAddressList, mInfoRepo, items))
                .map(this::groupByDate)
                .doOnSubscribe(d -> mDisposables.add(d))
                .subscribe(res -> {
                    mLoadState.postValue(LoadState.Loaded);
                    callback.onResult(res.getItems(), null, res.getMeta().lastPage == 1 ? null : res.getMeta().currentPage + 1);
                });
    }

    @Override
    public void loadBefore(@NonNull LoadParams<Integer> params, @NonNull LoadCallback<Integer, TransactionItem> callback) {
        mLoadState.postValue(LoadState.Loading);
        if (mAddressList == null || mAddressList.isEmpty()) {
            Timber.w("Unable to load previous transactions list (page: %s): user address list is empty", params.key);
            mLoadState.postValue(LoadState.Loaded);
            callback.onResult(Collections.emptyList(), null);
            return;
        }
        rxExp(mRepo.getTransactions(mAddressList, params.key))
                .onErrorResumeNext(toExpError())
                .switchMap(items -> mapAddressesInfo(mAddressList, mInfoRepo, items))
                .map(this::groupByDate)
                .doOnSubscribe(d -> mDisposables.add(d))
                .subscribe(res -> {
                    mLoadState.postValue(LoadState.Loaded);
                    callback.onResult(res.getItems(), params.key == 1 ? null : params.key - 1);
                });
    }

    @Override
    public void loadAfter(@NonNull LoadParams<Integer> params, @NonNull LoadCallback<Integer, TransactionItem> callback) {
        mLoadState.postValue(LoadState.Loading);
        rxExp(mRepo.getTransactions(mAddressList, params.key))
                .onErrorResumeNext(toExpError())
                .switchMap(items -> mapAddressesInfo(mAddressList, mInfoRepo, items))
                .map(this::groupByDate)
                .doOnSubscribe(d -> mDisposables.add(d))
                .subscribe(res -> {
                    mLoadState.postValue(LoadState.Loaded);
                    callback.onResult(res.getItems(), params.key + 1 > res.getMeta().lastPage ? null : params.key + 1);
                });
    }

    @Override
    public void invalidate() {
        super.invalidate();
        mDisposables.dispose();
    }

    private String lastDay() {
        if (DateHelper.compareFlatDay(mLastDate, new DateTime())) {
            return Wallet.app().res().getString(R.string.today);
        }

        return mLastDate.toString(DateTimeFormat.forPattern("EEEE, dd MMMM").withLocale(Locale.US));
    }

    private DateTime dt(Date d) {
        return new DateTime(d);
    }

    private DataSourceMeta<TransactionItem> groupByDate(ExpResult<List<HistoryTransaction>> res) {
        List<TransactionItem> out = new ArrayList<>();
        for (HistoryTransaction tx : res.result) {
            if (mLastDate == null) {
                mLastDate = new DateTime(tx.timestamp);
                out.add(new HeaderItem(lastDay()));
            } else if (!DateHelper.compareFlatDay(mLastDate, dt(tx.timestamp))) {
                mLastDate = dt(tx.timestamp);
                out.add(new HeaderItem(lastDay()));
            }

            out.add(new TxItem(tx));
        }

        final DataSourceMeta<TransactionItem> meta = new DataSourceMeta<>();
        meta.setItems(out);
        meta.setMeta(res.getMeta());

        return meta;
    }

    public static class Factory extends DataSource.Factory<Integer, TransactionItem> {
        private ExplorerTransactionRepository mRepo;
        private List<MinterAddress> mAddressList;
        private ProfileInfoRepository mInfoRepo;
        private MutableLiveData<LoadState> mLoadState;

        public Factory(ExplorerTransactionRepository repo, ProfileInfoRepository infoRepo, List<MinterAddress> addresses, MutableLiveData<LoadState> loadState) {
            mRepo = repo;
            mInfoRepo = infoRepo;
            mAddressList = addresses;
            mLoadState = loadState;
        }

        @Override
        public DataSource<Integer, TransactionItem> create() {
            return new TransactionDataSource(mRepo, mInfoRepo, mAddressList, mLoadState);
        }
    }
}
