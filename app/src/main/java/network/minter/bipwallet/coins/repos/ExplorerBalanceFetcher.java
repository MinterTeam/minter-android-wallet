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

package network.minter.bipwallet.coins.repos;

import android.support.annotation.GuardedBy;
import android.support.annotation.NonNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import network.minter.bipwallet.advanced.models.AccountItem;
import network.minter.bipwallet.internal.Wallet;
import network.minter.core.crypto.MinterAddress;
import network.minter.explorer.models.AddressData;
import network.minter.explorer.repo.ExplorerAddressRepository;
import timber.log.Timber;

import static network.minter.bipwallet.internal.ReactiveAdapter.convertToExpErrorResult;
import static network.minter.bipwallet.internal.ReactiveAdapter.rxCallExp;

/**
 * minter-android-wallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class ExplorerBalanceFetcher implements ObservableOnSubscribe<List<AccountItem>> {
    @GuardedBy("mLock")
    private final Map<MinterAddress, AddressData> mRawBalances = new HashMap<>();
    private final Object mLock = new Object();
    private final List<MinterAddress> mAddresses;
    private final CountDownLatch mWaiter;
    private final ExplorerAddressRepository mAddressRepository;

    public ExplorerBalanceFetcher(ExplorerAddressRepository addressRepository, @NonNull final List<MinterAddress> addresses) {
        mAddresses = addresses;
        mAddressRepository = addressRepository;
        mWaiter = new CountDownLatch(addresses.size());
    }

    public static Observable<List<AccountItem>> create(@NonNull ExplorerAddressRepository addressRepository, @NonNull final List<MinterAddress> addresses) {
        return Observable.create(new ExplorerBalanceFetcher(addressRepository, addresses));
    }

    public static Observable<BigDecimal> createSingleTotalBalance(ExplorerAddressRepository addressRepository, MinterAddress address) {
        return rxCallExp(addressRepository.getAddressData(address))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .onErrorResumeNext(convertToExpErrorResult())
                .map(item -> item.result.getTotalBalance());
    }

    public static Observable<BigDecimal> createSingleCoinBalance(ExplorerAddressRepository addressRepository, MinterAddress address, String coin) {
        return rxCallExp(addressRepository.getAddressData(address))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .onErrorResumeNext(convertToExpErrorResult())
                .map(item -> {
                    BigDecimal out = new BigDecimal(0);
                    for (Map.Entry<String, AddressData.CoinBalance> entry : item.result.coins.entrySet()) {
                        if (entry.getValue().getCoin().toUpperCase().equals(coin.toUpperCase())) {
                            out = entry.getValue().getAmount();
                            break;
                        }
                    }

                    return out;
                });
    }

    @Override
    public void subscribe(ObservableEmitter<List<AccountItem>> emitter) throws Exception {
        Timber.d("Fetching amount in thread: %s", Thread.currentThread().getName());
        if (mAddresses.isEmpty()) {
            Timber.w("No one address");
            emitter.onNext(Collections.emptyList());
            emitter.onComplete();
            return;
        }

        for (MinterAddress address : mAddresses) {
            rxCallExp(mAddressRepository.getAddressData(address))
                    .onErrorResumeNext(convertToExpErrorResult())
                    .subscribeOn(Schedulers.io())
                    .subscribe(res -> {
                        synchronized (mLock) {
                            res.result.fillDefaultsOnEmpty();
                            mRawBalances.put(address, res.result);
                        }

                        mWaiter.countDown();
                    }, t -> {
                        Wallet.Rx.errorHandler().accept(t);
                        mWaiter.countDown();
                        emitter.onError(t);
                    });
        }

        mWaiter.await();

        List<AccountItem> out = new ArrayList<>();
        for (Map.Entry<MinterAddress, AddressData> entry : mRawBalances.entrySet()) {
            if (entry.getKey() == null) continue;

            for (AddressData.CoinBalance balance : entry.getValue().coins.values()) {
                out.add(new AccountItem(
                        null,
                        balance.getCoin(),
                        entry.getKey(),
                        balance.getAmount(),
                        balance.getUsdAmount(),
                        balance.baseCoinAmount
                ));
            }
        }

        emitter.onNext(out);
        emitter.onComplete();
    }
}
