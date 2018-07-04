/*
 * Copyright (C) 2018 by MinterTeam
 * @link https://github.com/MinterTeam
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
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.disposables.CompositeDisposable;
import network.minter.bipwallet.R;
import network.minter.bipwallet.internal.Wallet;
import network.minter.bipwallet.internal.helpers.DateHelper;
import network.minter.explorerapi.models.ExpResult;
import network.minter.explorerapi.models.HistoryTransaction;
import network.minter.explorerapi.repo.ExplorerTransactionRepository;
import network.minter.mintercore.crypto.MinterAddress;
import network.minter.my.models.AddressInfoResult;
import network.minter.my.repo.MyInfoRepository;

import static network.minter.bipwallet.internal.ReactiveAdapter.convertToExpErrorResult;
import static network.minter.bipwallet.internal.ReactiveAdapter.convertToMyErrorResult;
import static network.minter.bipwallet.internal.ReactiveAdapter.rxCallExp;
import static network.minter.bipwallet.internal.ReactiveAdapter.rxCallMy;

/**
 * MinterWallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class TransactionDataSource extends PageKeyedDataSource<Integer, TransactionItem> {

    private ExplorerTransactionRepository mRepo;
    private MyInfoRepository mInfoRepo;
    private List<MinterAddress> mAddressList;
    private CompositeDisposable mDisposables = new CompositeDisposable();
    private DateTime mLastDate;
    private MutableLiveData<LoadState> mLoadState;

    public enum LoadState {
        Loading,
        Loaded,
        Failed
    }

    public TransactionDataSource(ExplorerTransactionRepository repo, MyInfoRepository infoRepo, List<MinterAddress> addresses, MutableLiveData<LoadState> loadState) {
        mRepo = repo;
        mInfoRepo = infoRepo;
        mAddressList = addresses;
        mLoadState = loadState;
    }

    public static ObservableSource<ExpResult<List<HistoryTransaction>>> mapAddressesInfo(List<MinterAddress> addresses, MyInfoRepository infoRepo, ExpResult<List<HistoryTransaction>> items) {
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

            if (tx.isIncoming(addresses)) {
                add = tx.<HistoryTransaction.TxSendCoinResult>getData().from;
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

        return rxCallMy(infoRepo.getAddressesWithUserInfo(toFetch))
                .onErrorResumeNext(convertToMyErrorResult())
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

    public static ObservableSource<List<HistoryTransaction>> mapAddressesInfo(List<MinterAddress> addresses, MyInfoRepository infoRepo, List<HistoryTransaction> items) {
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
                add = tx.<HistoryTransaction.TxSendCoinResult>getData().from;
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

        return rxCallMy(infoRepo.getAddressesWithUserInfo(toFetch))
                .onErrorResumeNext(convertToMyErrorResult())
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

    @Override
    public void loadInitial(@NonNull LoadInitialParams<Integer> params, @NonNull LoadInitialCallback<Integer, TransactionItem> callback) {
        mLoadState.postValue(LoadState.Loading);
        rxCallExp(mRepo.getTransactions(mAddressList, 1))
                .onErrorResumeNext(convertToExpErrorResult())
                .switchMap(items -> mapAddressesInfo(mAddressList, mInfoRepo, items))
                .map(this::groupByDate)
                .doOnSubscribe(d -> mDisposables.add(d))
                .subscribe(res -> {
                    mLoadState.postValue(LoadState.Loaded);
                    callback.onResult(res.items, null, res.getMeta().lastPage == 1 ? null : res.getMeta().currentPage + 1);
                });
    }

    @Override
    public void loadBefore(@NonNull LoadParams<Integer> params, @NonNull LoadCallback<Integer, TransactionItem> callback) {
        mLoadState.postValue(LoadState.Loading);
        rxCallExp(mRepo.getTransactions(mAddressList, params.key))
                .onErrorResumeNext(convertToExpErrorResult())
                .switchMap(items -> mapAddressesInfo(mAddressList, mInfoRepo, items))
                .map(this::groupByDate)
                .doOnSubscribe(d -> mDisposables.add(d))
                .subscribe(res -> {
                    mLoadState.postValue(LoadState.Loaded);
                    callback.onResult(res.items, params.key == 1 ? null : params.key - 1);
                });
    }

    @Override
    public void loadAfter(@NonNull LoadParams<Integer> params, @NonNull LoadCallback<Integer, TransactionItem> callback) {
        mLoadState.postValue(LoadState.Loading);
        rxCallExp(mRepo.getTransactions(mAddressList, params.key))
                .onErrorResumeNext(convertToExpErrorResult())
                .switchMap(items -> mapAddressesInfo(mAddressList, mInfoRepo, items))
                .map(this::groupByDate)
                .doOnSubscribe(d -> mDisposables.add(d))
                .subscribe(res -> {
                    mLoadState.postValue(LoadState.Loaded);
                    callback.onResult(res.items, params.key + 1 > res.getMeta().lastPage ? null : params.key + 1);
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

        return mLastDate.toString(DateTimeFormat.forPattern("EEEE, dd MMMM"));
    }

    private DateTime dt(Date d) {
        return new DateTime(d);
    }

    private MetaTx groupByDate(ExpResult<List<HistoryTransaction>> res) {
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

        final MetaTx metaTx = new MetaTx();
        metaTx.items = out;
        metaTx.meta = res.getMeta();

        return metaTx;
    }

    private final static class MetaTx {
        private List<TransactionItem> items;
        private ExpResult.Meta meta;

        ExpResult.Meta getMeta() {
            return meta;
        }
    }

    public static class Factory extends DataSource.Factory<Integer, TransactionItem> {
        private ExplorerTransactionRepository mRepo;
        private List<MinterAddress> mAddressList;
        private MyInfoRepository mInfoRepo;
        private MutableLiveData<LoadState> mLoadState;

        public Factory(ExplorerTransactionRepository repo, MyInfoRepository infoRepo, List<MinterAddress> addresses, MutableLiveData<LoadState> loadState) {
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
