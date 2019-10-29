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

import com.annimon.stream.Stream;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.paging.DataSource;
import androidx.paging.PageKeyedDataSource;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.disposables.CompositeDisposable;
import network.minter.bipwallet.R;
import network.minter.bipwallet.advanced.repo.SecretStorage;
import network.minter.bipwallet.apis.explorer.CachedValidatorsRepository;
import network.minter.bipwallet.internal.Wallet;
import network.minter.bipwallet.internal.adapter.DataSourceMeta;
import network.minter.bipwallet.internal.adapter.LoadState;
import network.minter.bipwallet.internal.data.CachedRepository;
import network.minter.bipwallet.internal.helpers.DateHelper;
import network.minter.core.crypto.MinterAddress;
import network.minter.core.crypto.MinterPublicKey;
import network.minter.explorer.models.ExpResult;
import network.minter.explorer.models.HistoryTransaction;
import network.minter.explorer.models.ValidatorItem;
import network.minter.explorer.models.ValidatorMeta;
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


    private CompositeDisposable mDisposables = new CompositeDisposable();
    private DateTime mLastDate;
    private TransactionDataSource.Factory factory;

    public TransactionDataSource(TransactionDataSource.Factory factory) {
        this.factory = factory;
    }

    public static ObservableSource<ExpResult<List<TransactionFacade>>> mapToFacade(ExpResult<List<HistoryTransaction>> result) {
        List<TransactionFacade> items;
        if (result.result == null) {
            items = Collections.emptyList();
        } else {
            items = Stream.of(result.result).map(TransactionFacade::new).toList();
        }

        ExpResult<List<TransactionFacade>> out = new ExpResult<>();
        out.code = result.code;
        out.links = result.meta;
        out.meta = result.meta;
        out.error = result.error;
        out.result = items;

        return Observable.just(out);
    }

    public static ObservableSource<List<TransactionFacade>> mapToFacade(List<HistoryTransaction> result) {
        if (result == null || result.isEmpty()) {
            return Observable.just(Collections.emptyList());
        }

        return Observable.just(
                Stream.of(result).map(TransactionFacade::new).toList()
        );
    }

    public static ObservableSource<ExpResult<List<TransactionFacade>>> mapAddressesInfo(List<MinterAddress> myAddresses, ProfileInfoRepository infoRepo, ExpResult<List<TransactionFacade>> items) {
        if (items.result == null || items.result.isEmpty()) {
            return Observable.just(items);
        }

        List<MinterAddress> toFetch = new ArrayList<>();
        final Map<MinterAddress, List<TransactionFacade>> toFetchAddresses = new LinkedHashMap<>(items.result.size());
        for (TransactionFacade tx : items.result) {
            final MinterAddress add;
            if (tx.getType() != HistoryTransaction.Type.Send) {
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
                        for (TransactionFacade t : toFetchAddresses.get(info.address)) {
                            t.setUserMeta(info.user.username, info.user.getAvatar().getUrl());
                        }
                    }

                    return items;
                });
    }

    public static ObservableSource<ExpResult<List<TransactionFacade>>> mapValidatorsInfo(CachedRepository<List<ValidatorItem>, CachedValidatorsRepository> validatorRepo, ExpResult<List<TransactionFacade>> result) {
        return Observable.just(result)
                .map(items -> items.result)
                .switchMap(items -> mapValidatorsInfo(validatorRepo, items))
                .switchMap(items -> {
                    result.result = items;
                    return Observable.just(result);
                });
    }

    public static ObservableSource<List<TransactionFacade>> mapValidatorsInfo(CachedRepository<List<ValidatorItem>, CachedValidatorsRepository> validatorRepo, List<TransactionFacade> items) {
        if (Stream.of(items).filter(item -> item.getType() == HistoryTransaction.Type.Delegate).count() == 0) {
            return Observable.just(items);
        }

        return validatorRepo.getOrUpdate()
                .map(validatorItems -> {
                    Map<MinterPublicKey, ValidatorMeta> out = new HashMap<>();
                    Stream.of(validatorItems).forEach(item -> out.put(item.pubKey, item.meta));
                    return out;
                })
                .map(validators -> {
                    for (TransactionFacade tx : items) {
                        if (tx.getType() == HistoryTransaction.Type.Delegate || tx.getType() == HistoryTransaction.Type.Unbond) {
                            HistoryTransaction.TxDelegateUnbondResult d = tx.getData();
                            if (validators.containsKey(d.getPublicKey())) {
                                tx.setValidatorMeta(validators.get(d.getPublicKey()));
                            }
                        }
                    }

                    return items;
                });

    }

    public static ObservableSource<List<TransactionFacade>> mapAddressesInfo(List<MinterAddress> addresses, ProfileInfoRepository infoRepo, List<TransactionFacade> items) {
        if (items == null || items.isEmpty()) {
            return Observable.just(Collections.emptyList());
        }

        List<MinterAddress> toFetch = new ArrayList<>();
        final Map<MinterAddress, List<TransactionFacade>> toFetchAddresses = new LinkedHashMap<>(items.size());
        for (TransactionFacade tx : items) {

            final MinterAddress add;
            if (tx.getType() != HistoryTransaction.Type.Send) {
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
                        for (TransactionFacade t : toFetchAddresses.get(info.address)) {
                            t.userMeta = new TransactionFacade.UserMeta();
                            t.userMeta.username = info.user.username;
                            t.userMeta.avatarUrl = info.user.getAvatar().getUrl();

                        }
                    }

                    return items;
                });
    }

    @Override
    public void loadInitial(@NonNull LoadInitialParams<Integer> params, @NonNull LoadInitialCallback<Integer, TransactionItem> callback) {
        factory.loadState.postValue(LoadState.Loading);
        if (factory.addressList == null || factory.addressList.isEmpty()) {
            Timber.w("Unanble to load transactions list(page: 1): user address list is empty");
            factory.loadState.postValue(LoadState.Loaded);
            callback.onResult(Collections.emptyList(), null, null);
            return;
        }

        resolveInfo(rxExp(factory.repo.getTransactions(factory.addressList, 1)))
                .map(this::groupByDate)
                .doOnSubscribe(d -> mDisposables.add(d))
                .subscribe(res -> {
                    factory.loadState.postValue(LoadState.Loaded);
                    callback.onResult(res.getItems(), null, res.getMeta().lastPage == 1 ? null : res.getMeta().currentPage + 1);
                });
    }

    @Override
    public void loadBefore(@NonNull LoadParams<Integer> params, @NonNull LoadCallback<Integer, TransactionItem> callback) {
        factory.loadState.postValue(LoadState.Loading);
        if (factory.addressList == null || factory.addressList.isEmpty()) {
            Timber.w("Unable to load previous transactions list (page: %s): user address list is empty", params.key);
            factory.loadState.postValue(LoadState.Loaded);
            callback.onResult(Collections.emptyList(), null);
            return;
        }
        resolveInfo(rxExp(factory.repo.getTransactions(factory.addressList, params.key)))
                .map(this::groupByDate)
                .doOnSubscribe(d -> mDisposables.add(d))
                .subscribe(res -> {
                    factory.loadState.postValue(LoadState.Loaded);
                    callback.onResult(res.getItems(), params.key == 1 ? null : params.key - 1);
                });
    }

    @Override
    public void loadAfter(@NonNull LoadParams<Integer> params, @NonNull LoadCallback<Integer, TransactionItem> callback) {
        factory.loadState.postValue(LoadState.Loading);
        resolveInfo(rxExp(factory.repo.getTransactions(factory.addressList, params.key)))
                .map(this::groupByDate)
                .doOnSubscribe(d -> mDisposables.add(d))
                .subscribe(res -> {
                    factory.loadState.postValue(LoadState.Loaded);
                    callback.onResult(res.getItems(), params.key + 1 > res.getMeta().lastPage ? null : params.key + 1);
                });
    }

    private Observable<ExpResult<List<TransactionFacade>>> resolveInfo(Observable<ExpResult<List<HistoryTransaction>>> source) {
        return source
                .onErrorResumeNext(toExpError())
                .switchMap(TransactionDataSource::mapToFacade)
                .switchMap(items -> mapAddressesInfo(factory.addressList, factory.infoRepo, items))
                .switchMap(items -> mapValidatorsInfo(factory.validatorsRepo, items));
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

    private DataSourceMeta<TransactionItem> groupByDate(ExpResult<List<TransactionFacade>> res) {
        List<TransactionItem> out = new ArrayList<>();
        for (TransactionFacade tx : res.result) {
            if (mLastDate == null) {
                mLastDate = new DateTime(tx.get().timestamp);
                out.add(new HeaderItem(lastDay()));
            } else if (!DateHelper.compareFlatDay(mLastDate, dt(tx.get().timestamp))) {
                mLastDate = dt(tx.get().timestamp);
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
        @Inject ExplorerTransactionRepository repo;
        @Inject ProfileInfoRepository infoRepo;
        @Inject CachedRepository<List<ValidatorItem>, CachedValidatorsRepository> validatorsRepo;
        private List<MinterAddress> addressList;
        private MutableLiveData<LoadState> loadState;

        @Inject
        public Factory(SecretStorage secretStorage) {
            addressList = secretStorage.getAddresses();
        }

        @Override
        public DataSource<Integer, TransactionItem> create() {
            return new TransactionDataSource(this);
        }

        public void observeLoadState(MutableLiveData<LoadState> loadState) {
            this.loadState = loadState;
        }
    }
}
