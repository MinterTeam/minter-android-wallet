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

package network.minter.bipwallet.coins.repos;

import android.support.annotation.GuardedBy;
import android.support.annotation.NonNull;

import com.annimon.stream.Stream;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.schedulers.Schedulers;
import network.minter.bipwallet.internal.Wallet;
import network.minter.blockchainapi.models.Balance;
import network.minter.blockchainapi.repo.BlockChainAccountRepository;
import network.minter.mintercore.crypto.MinterAddress;
import timber.log.Timber;

import static network.minter.bipwallet.internal.ReactiveAdapter.rxCallBc;

/**
 * MinterWallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class BlockChainBalanceFetcher implements ObservableOnSubscribe<List<Balance.CoinBalance>> {
    @GuardedBy("mLock")
    private final Map<MinterAddress, List<Balance.CoinBalance>> mOut = new HashMap<>();
    private final Object mLock = new Object();
    private final List<MinterAddress> mAddresses;
    private final CountDownLatch mWaiter;
    private final BlockChainAccountRepository mAccountRepo;

    public BlockChainBalanceFetcher(BlockChainAccountRepository accountRepo, @NonNull final List<MinterAddress> addresses) {
        mAddresses = addresses;
        mAccountRepo = accountRepo;
        mWaiter = new CountDownLatch(addresses.size());
    }

    @Override
    public void subscribe(ObservableEmitter<List<Balance.CoinBalance>> emitter) throws Exception {
        if (mAddresses.isEmpty()) {
            Timber.w("No one address");
            emitter.onNext(Collections.emptyList());
            emitter.onComplete();
            return;
        }

        for (MinterAddress address : mAddresses) {
            rxCallBc(mAccountRepo.getBalance(address))
                    .subscribeOn(Schedulers.io())
                    .subscribe(res -> {
                        synchronized (mLock) {
                            if (!mOut.containsKey(address)) {
                                mOut.put(address, new ArrayList<>());
                            }

                            mOut.get(address).addAll(res.result.coins.values());
                        }

                        mWaiter.countDown();
                    }, t -> {
                        Wallet.Rx.errorHandler().accept(t);
                        mWaiter.countDown();
                    });
        }

        mWaiter.await();
        final List<Balance.CoinBalance> out = new ArrayList<>();
        for (List<Balance.CoinBalance> items : mOut.values()) {
            out.addAll(items);
        }
        Map<String, BigDecimal> aggregator = new LinkedHashMap<>();
        for (Balance.CoinBalance balance : out) {
            if (!aggregator.containsKey(balance.coin)) {
                aggregator.put(balance.coin, balance.getBalance());
            } else {
                aggregator.get(balance.coin).add(balance.getBalance());
            }
        }
        List<Balance.CoinBalance> sumOut = new ArrayList<>(aggregator.size());
        for (Map.Entry<String, BigDecimal> entry : aggregator.entrySet()) {
            final Balance.CoinBalance b = new Balance.CoinBalance();
            b.coin = entry.getKey();
            b.setBalance(entry.getValue());
            sumOut.add(b);
        }
        emitter.onNext(Stream.of(sumOut).sortBy(b -> b.coin).toList());
        emitter.onComplete();
    }
}
