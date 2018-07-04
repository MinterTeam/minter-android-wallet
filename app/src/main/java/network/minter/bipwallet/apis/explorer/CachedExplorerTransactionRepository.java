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

package network.minter.bipwallet.apis.explorer;

import android.support.annotation.NonNull;

import com.annimon.stream.Stream;

import java.util.Collections;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import network.minter.bipwallet.advanced.repo.SecretStorage;
import network.minter.bipwallet.internal.data.CachedEntity;
import network.minter.bipwallet.internal.storage.KVStorage;
import network.minter.explorerapi.models.HistoryTransaction;
import network.minter.explorerapi.repo.ExplorerTransactionRepository;
import network.minter.mintercore.crypto.MinterAddress;
import network.minter.mintercore.internal.api.ApiService;

import static network.minter.bipwallet.internal.ReactiveAdapter.rxCallExp;

/**
 * MinterWallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class CachedExplorerTransactionRepository extends ExplorerTransactionRepository implements CachedEntity<List<HistoryTransaction>> {
    private final static String KEY_TRANSACTIONS = "cached_explorer_transaction_repository_transactions";
    private final KVStorage mStorage;
    private final SecretStorage mSecretStorage;

    public CachedExplorerTransactionRepository(KVStorage storage, SecretStorage secretStorage, @NonNull ApiService.Builder apiBuilder) {
        super(apiBuilder);
        mStorage = storage;
        mSecretStorage = secretStorage;
    }

    @Override
    public List<HistoryTransaction> initialData() {
        return Collections.emptyList();
    }

    @Override
    public Observable<List<HistoryTransaction>> getUpdatableData() {
        return rxCallExp(getService().getTransactions(
                Stream.of(mSecretStorage.getAddresses()).map(MinterAddress::toString).toList()
        ))
                .map(res -> res.result)
                .subscribeOn(Schedulers.io());
    }

    @Override
    public void onAfterUpdate(List<HistoryTransaction> result) {
        mStorage.put(KEY_TRANSACTIONS, result);
    }
}
