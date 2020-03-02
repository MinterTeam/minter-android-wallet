/*
 * Copyright (C) by MinterTeam. 2019
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

package network.minter.bipwallet.wallets.repos;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import network.minter.bipwallet.advanced.models.CoinAccount;
import network.minter.bipwallet.advanced.models.UserAccount;
import network.minter.bipwallet.internal.helpers.data.CollectionsHelper;
import network.minter.core.crypto.MinterAddress;
import network.minter.explorer.models.AddressData;
import network.minter.explorer.repo.ExplorerAddressRepository;
import timber.log.Timber;

import static network.minter.bipwallet.apis.reactive.ReactiveExplorerGate.rxExpGate;
import static network.minter.bipwallet.apis.reactive.ReactiveExplorerGate.toExpGateError;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class ExplorerBalanceFetcher implements ObservableOnSubscribe<List<UserAccount>> {
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

    public static Observable<List<UserAccount>> create(@NonNull ExplorerAddressRepository addressRepository, @NonNull final List<MinterAddress> addresses) {
        return Observable.create(new ExplorerBalanceFetcher(addressRepository, addresses));
    }

    public static Observable<BigDecimal> createSingleCoinBalance(ExplorerAddressRepository addressRepository, MinterAddress address, String coin) {
        return rxExpGate(addressRepository.getAddressData(address, true))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .onErrorResumeNext(toExpGateError())
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
    public void subscribe(ObservableEmitter<List<UserAccount>> emitter) throws Exception {
        Timber.d("Fetching amount in thread: %s", Thread.currentThread().getName());
        if (mAddresses.isEmpty()) {
            Timber.w("No one address");
            emitter.onNext(Collections.emptyList());
            emitter.onComplete();
            return;
        }

        for (MinterAddress address : mAddresses) {
            rxExpGate(mAddressRepository.getAddressData(address, true))
                    .onErrorResumeNext(toExpGateError())
                    .subscribeOn(Schedulers.io())
                    .subscribe(res -> {
                        synchronized (mLock) {
                            if (res.result == null) {
                                res.result = new AddressData();
                            }
                            res.result.fillDefaultsOnEmpty();
                            mRawBalances.put(address, res.result);
                        }

                        mWaiter.countDown();
                    }, t -> {
                        mWaiter.countDown();
                        emitter.onError(t);
                    });
        }

        mWaiter.await();

        List<UserAccount> out = new ArrayList<>();

        for (Map.Entry<MinterAddress, AddressData> entry : mRawBalances.entrySet()) {
            if (entry.getKey() == null) continue;

            List<CoinAccount> accounts = new ArrayList<>(entry.getValue().coins.size());

            for (AddressData.CoinBalance balance : entry.getValue().coins.values()) {
                CoinAccount item = new CoinAccount(
                        null,
                        balance.getCoin(),
                        entry.getKey(),
                        balance.getAmount()
                );
                accounts.add(item);
            }
            Collections.sort(accounts, new CollectionsHelper.StableCoinSorting());

            UserAccount ua = new UserAccount(
                    accounts,
                    entry.getValue().getAvailableBalanceBIP(),
                    entry.getValue().getTotalBalance(),
                    entry.getValue().getTotalBalanceUSD());

            out.add(ua);
        }

        emitter.onNext(out);
        emitter.onComplete();
    }
}
