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
package network.minter.bipwallet.apis.explorer

import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import network.minter.bipwallet.BuildConfig
import network.minter.bipwallet.apis.reactive.ReactiveExplorer
import network.minter.bipwallet.apis.reactive.rxExp
import network.minter.bipwallet.internal.data.CachedEntity
import network.minter.bipwallet.internal.data.CachedRepository
import network.minter.bipwallet.internal.storage.KVStorage
import network.minter.bipwallet.internal.storage.SecretStorage
import network.minter.core.internal.api.ApiService
import network.minter.explorer.models.ExpResult
import network.minter.explorer.models.HistoryTransaction
import network.minter.explorer.repo.ExplorerTransactionRepository

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */

typealias RepoTransactions = CachedRepository<@JvmSuppressWildcards List<HistoryTransaction>, CacheTxRepository>

class CacheTxRepository(
        private val storage: KVStorage,
        private val secretStorage: SecretStorage,
        apiBuilder: ApiService.Builder
) : ExplorerTransactionRepository(apiBuilder), CachedEntity<@JvmSuppressWildcards List<HistoryTransaction>> {

    companion object {
        private const val KEY_TRANSACTIONS = BuildConfig.MINTER_STORAGE_VERS + "cached_explorer_transaction_repository_transactions"
    }

    override fun getData(): List<HistoryTransaction> {
        return storage[KEY_TRANSACTIONS, emptyList()]
    }

    override fun getUpdatableData(): Observable<List<HistoryTransaction>> {
        return instantService
                .getTransactions(
                        listOf(secretStorage.mainWallet).map { it.toString() }.toList(),
                        1, 5
                )
                .rxExp()
                .onErrorResumeNext(ReactiveExplorer.toExpError())
                .map { res: ExpResult<List<HistoryTransaction>> ->
                    if (res.result != null) {
                        return@map res.result
                    }
                    getData()
                }
                .subscribeOn(Schedulers.io())
    }

    override fun onAfterUpdate(result: List<HistoryTransaction>) {
        storage.putAsync(KEY_TRANSACTIONS, result)
    }

    override fun onClear() {
        storage.delete(KEY_TRANSACTIONS)
    }

    override fun isDataReady(): Boolean {
        return storage.contains(KEY_TRANSACTIONS)
    }
}