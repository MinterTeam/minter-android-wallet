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
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import network.minter.bipwallet.BuildConfig
import network.minter.bipwallet.apis.reactive.rxExp
import network.minter.bipwallet.internal.data.CachedEntity
import network.minter.bipwallet.internal.data.CachedRepository
import network.minter.bipwallet.internal.storage.KVStorage
import network.minter.core.MinterSDK
import network.minter.core.internal.api.ApiService
import network.minter.explorer.models.CoinItem
import network.minter.explorer.repo.ExplorerCoinsRepository
import okhttp3.internal.toImmutableList
import java.math.BigDecimal

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */

typealias RepoCoins = CachedRepository<@JvmSuppressWildcards List<CoinItem>, CachedCoinsRepository>

class CachedCoinsRepository(
        private val storage: KVStorage,
        api: ApiService.Builder
) : ExplorerCoinsRepository(api), CachedEntity<@kotlin.jvm.JvmSuppressWildcards List<CoinItem>> {

    companion object {
        private const val KEY_COINS = BuildConfig.MINTER_STORAGE_VERS + "coins_all"
    }

    override fun getData(): List<CoinItem> {
        return storage[KEY_COINS, emptyList()]
    }

    override fun getUpdatableData(): Observable<List<CoinItem>> {
        return all.rxExp()
                .map { res ->
                    if (res.isOk && res.result != null) {
                        res.result!!.filter {
                            it.symbol.toUpperCase() == MinterSDK.DEFAULT_COIN
                        }.forEach {
                            it.reserveBalance = BigDecimal("10e9")
                        }

                        val resMutable = res.result!!.toMutableList()

                        resMutable.sortWith(Comparator { a, b ->
                            b.reserveBalance.compareTo(a.reserveBalance)
                        })

                        resMutable.toImmutableList()
                    } else {
                        emptyList()
                    }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
    }

    override fun onAfterUpdate(result: List<CoinItem>) {
        storage.put(KEY_COINS, result)
    }

    override fun onClear() {
        storage.deleteAsync(KEY_COINS)
    }

    override fun isDataReady(): Boolean {
        return storage.contains(KEY_COINS)
    }
}