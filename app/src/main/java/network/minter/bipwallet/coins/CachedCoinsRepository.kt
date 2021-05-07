/*
 * Copyright (C) by MinterTeam. 2021
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

package network.minter.bipwallet.coins

import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import network.minter.bipwallet.BuildConfig
import network.minter.bipwallet.R
import network.minter.bipwallet.db.WalletDatabase
import network.minter.bipwallet.internal.Wallet
import network.minter.bipwallet.internal.data.CachedEntity
import network.minter.bipwallet.internal.data.CachedRepository
import network.minter.bipwallet.internal.helpers.ViewExtensions.tr
import network.minter.bipwallet.internal.storage.KVStorage
import network.minter.core.MinterSDK
import network.minter.core.internal.api.ApiService
import network.minter.explorer.models.CoinItem
import network.minter.explorer.repo.ExplorerCoinsRepository
import okhttp3.internal.toImmutableList
import timber.log.Timber
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*
import kotlin.Comparator
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */

typealias RepoCoins = CachedRepository<@JvmSuppressWildcards List<CoinItem>, CachedCoinsRepository>

class CachedCoinsRepository(
        private val storage: KVStorage,
        private val db: WalletDatabase,
        api: ApiService.Builder
) : ExplorerCoinsRepository(api), CachedEntity<@kotlin.jvm.JvmSuppressWildcards List<CoinItem>> {

    companion object {
        private const val KEY_COINS = BuildConfig.MINTER_CACHE_VERS + "coins_all"
    }

    override fun getData(): List<CoinItem> {
        return storage[KEY_COINS, emptyList()]
    }

    override fun getUpdatableData(): Observable<List<CoinItem>> {
        return all
                .map { res ->
                    if (res.isOk && res.result != null) {
                        res.result!!.filter {
                            it.symbol.uppercase(Wallet.LC_EN) == MinterSDK.DEFAULT_COIN
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

    fun getAllDb(): Single<List<CoinItem>> {
        return db.coins().findAll().map { list ->
            list.map { it.asCoin() }
        }
    }

    fun getAllByIds(ids: ArrayList<BigInteger>): Single<List<CoinItem>> {
        return db.coins().findAllByIds(ids).map { list -> list.map { it.asCoin() }.toList() }
    }

    fun findByName(name: String): Maybe<CoinItem> {
        return db.coins().findByName(name)
                .map { it.asCoin() }
                .switchIfEmpty(Maybe.error(RuntimeException(tr(R.string.coin_err_not_found))))
    }

    fun findById(coinId: BigInteger): Maybe<CoinItem> {
        return db.coins().findById(coinId)
                .map { it.asCoin() }
                .switchIfEmpty(Maybe.error(RuntimeException(tr(R.string.coin_err_not_found))))
    }

    override fun onAfterUpdate(result: List<CoinItem>) {
    }

    override fun onAfterUpdateDeferred(result: List<CoinItem>): Completable? {
        return Completable.create { emitter ->
            storage.put(KEY_COINS, result)
            db.coins().findAll()
                    .observeOn(Schedulers.io())
                    .subscribeOn(Schedulers.io())
                    .subscribe(
                            { dbCoins ->
                                val mapped: HashMap<BigInteger, DbCoin> = HashMap()
                                for (dbCoin in dbCoins) {
                                    mapped[dbCoin.coinId!!] = dbCoin
                                }

                                val toInsert = ArrayList<DbCoin>()
                                val toUpdate = ArrayList<DbCoin>()
                                for (coinItem in result) {
                                    if (!mapped.containsKey(coinItem.id)) {
                                        toInsert.add(DbCoin(coinItem))
                                    } else {
                                        val dbCoin = mapped[coinItem.id]
                                        if (dbCoin!!.symbol != coinItem.symbol) {
                                            dbCoin.symbol = coinItem.symbol
                                            toUpdate.add(dbCoin)
                                        }
                                    }
                                }

                                Completable.concatArray(insertDbCoins(toInsert), updateDbCoins(toUpdate))
                                        .observeOn(Schedulers.io())
                                        .subscribeOn(Schedulers.io())
                                        .subscribe({
                                            emitter.onComplete()
                                        }, { t ->
                                            Timber.e(t, "Unable to insert or update coins to DB")
                                            emitter.onError(t)
                                        })
                            },
                            { t ->
                                Timber.w(t, "Unable to get all coins")
                                emitter.onError(t)
                            }
                    )
        }
    }

    private fun insertDbCoins(coins: List<DbCoin>): Completable {
        return Completable.create { emitter ->
            if (coins.isEmpty()) {
                Timber.d("Nothing to insert")
                emitter.onComplete()
                return@create
            }
            try {
                db.coins().insert(coins)
                Timber.d("Inserted %d new coins", coins.size)
                emitter.onComplete()
            } catch (t: Throwable) {
                emitter.onError(t)
            }
        }
    }

    private fun updateDbCoins(coins: List<DbCoin>): Completable {
        return Completable.create { emitter ->
            if (coins.isEmpty()) {
                Timber.d("Nothing to update")
                emitter.onComplete()
                return@create
            }
            try {
                db.coins().updateMultiple(coins)
                Timber.d("Updated %d coins", coins.size)
                emitter.onComplete()
            } catch (t: Throwable) {
                emitter.onError(t)
            }
        }
    }

    override fun onClear() {
        storage.deleteAsync(KEY_COINS)
    }

    override fun isDataReady(): Boolean {
        return storage.contains(KEY_COINS)
    }




}