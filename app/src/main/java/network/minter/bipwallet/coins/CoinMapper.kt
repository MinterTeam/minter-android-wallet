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

package network.minter.bipwallet.coins

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import network.minter.bipwallet.internal.helpers.data.CollectionsHelper.toMap
import network.minter.bipwallet.internal.helpers.data.CollectionsHelper.unique
import network.minter.blockchain.models.operational.*
import network.minter.core.MinterSDK
import network.minter.explorer.models.CoinItem
import network.minter.explorer.repo.GateCoinRepository
import timber.log.Timber
import java.math.BigInteger

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 * @todo re-work this helper
 */
class CoinMapper(
        val coinsRepo: RepoCoins,
        val coinsGateRepo: GateCoinRepository
) {

    /**
     * Lock object for synchronizations
     */
    private val mLock: Any = Any()

    /**
     * Memory cache for all coins {id -> coin}
     */
    private var idCache: MutableMap<BigInteger, CoinItem> = HashMap()

    /**
     * Reversed cache {symbol -> coin}
     */
    private var nameCache: MutableMap<String, CoinItem> = HashMap()

    private var resolvedAll = false

    init {
        coinsRepo.observe()
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .switchMapCompletable { resolveAllCoins() }
                .subscribe {
                    Timber.d("Coin mapper cache has been auto-updated")
                }

        coinsRepo.update()
    }

    fun resolveAllCoins(): Completable {
        return coinsRepo.entity.getAllDb()
                .flatMapCompletable {
                    val out = HashMap<BigInteger, CoinItem>()
                    for (item in it) {
                        out[item.id] = item
                    }
                    synchronized(mLock) {
                        idCache = out
                        for (entry in idCache) {
                            nameCache[entry.value.symbol] = entry.value
                        }
                        resolvedAll = true
                    }

                    Completable.complete()
                }
    }

    fun resolveCoins(ids: List<BigInteger>): Completable {
        return Completable.create { emitter ->
            val ulist = ids.unique()

            coinsRepo.entity.getAllByIds(ulist)
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .subscribe(
                            { coins ->
                                Timber.d("Mapped %d coins", coins.size)
                                val tmp = coins.toMap {
                                    Pair<BigInteger, CoinItem>(it.id, it)
                                }

                                synchronized(mLock) {
                                    for (entry in tmp) {
                                        if (!idCache.containsKey(entry.key)) {
                                            idCache[entry.key] = entry.value
                                        }
                                    }
                                }

                                emitter.onComplete()
                            },
                            {
                                emitter.onError(it)
                            }
                    )
        }
    }


    fun idToSymbol(id: BigInteger): String? {
        if (id == MinterSDK.DEFAULT_COIN_ID) {
            return MinterSDK.DEFAULT_COIN
        }

        if (!idCache.containsKey(id)) {
            return null
        }

        return idCache[id]?.symbol
    }

    fun idToSymbolDef(id: BigInteger): String {
        return idToSymbol(id) ?: "coin ID: $id"
    }

    fun idToSymbolDef(id: BigInteger, format: String): String {
        return idToSymbol(id) ?: String.format(format, id.toString())
    }

    operator fun get(id: BigInteger): String? {
        return idToSymbol(id)
    }

    operator fun get(name: String): BigInteger? {
        return symbolToId(name)
    }

    fun symbolToId(symbol: String): BigInteger? {
        val name = symbol.toUpperCase()
        if (name == MinterSDK.DEFAULT_COIN) {
            return MinterSDK.DEFAULT_COIN_ID
        }

        // if has local cache , get from it
        if (nameCache.containsKey(name)) {
            return nameCache[name]!!.id
        }

        // try to find in global list
        val out = try {
            idCache.values.first { it.symbol.toUpperCase() == name }
        } catch (e: Throwable) {
            null
        }

        // if something was found, store to cache
        if (out != null && !nameCache.containsKey(name)) {
            nameCache[name] = out
        }

        return out?.id

    }

    fun getById(id: BigInteger): CoinItem? {
        if (!idCache.containsKey(id)) {
            return null
        }
        return idCache[id]
    }

    fun getByName(name: String): CoinItem? {
        if (!nameCache.containsKey(name)) {
            return null
        }
        return nameCache[name]
    }

    fun existMultiple(extTx: ExternalTransaction): Single<Boolean> {
        val coinsToSearch: MutableList<BigInteger> = ArrayList()
        when (extTx.type) {
            OperationType.SendCoin -> {
                coinsToSearch.add(extTx.getData<TxSendCoin>().coinId)
            }
            OperationType.SellCoin -> {
                val data = extTx.getData<TxCoinSell>()
                coinsToSearch.add(data.coinIdToBuy)
                coinsToSearch.add(data.coinIdToSell)
            }
            OperationType.SellAllCoins -> {
                val data = extTx.getData<TxCoinSellAll>()
                coinsToSearch.add(data.coinIdToBuy)
                coinsToSearch.add(data.coinIdToSell)
            }
            OperationType.BuyCoin -> {
                val data = extTx.getData<TxCoinBuy>()
                coinsToSearch.add(data.coinIdToBuy)
                coinsToSearch.add(data.coinIdToSell)
            }
            OperationType.DeclareCandidacy -> {
                val data = extTx.getData<TxDeclareCandidacy>()
                coinsToSearch.add(data.coinId)
            }
            OperationType.Delegate -> {
                val data = extTx.getData<TxDelegate>()
                coinsToSearch.add(data.coinId)
            }
            OperationType.Unbound -> {
                val data = extTx.getData<TxUnbound>()
                coinsToSearch.add(data.coinId)
            }
            OperationType.RedeemCheck -> {
                val data = extTx.getData<TxRedeemCheck>()
                coinsToSearch.add(data.decodedCheck.coinId)
            }
            OperationType.Multisend -> {
                val data = extTx.getData<TxMultisend>()
                data.items.map { it.coinId }
                for (item in data.items) {
                    if (!coinsToSearch.contains(item.coinId)) {
                        coinsToSearch.add(item.coinId)
                    }
                }
            }
            else -> {
                //chill
            }
        }

        if (coinsToSearch.isEmpty()) {
            return Single.just(true)
        }

        var resolve = false
        for (id in coinsToSearch) {
            if (!idCache.containsKey(id)) {
                resolve = true
                break
            }
        }

        if (!resolve) {
            return Single.just(true)
        }

        Timber.d("Unable to find some coins. Trying to refresh cache from local db")
        return resolveAllCoins()
                .toSingle {
                    var success = true
                    // search again after reloading
                    for (id in coinsToSearch) {
                        if (!idCache.containsKey(id)) {
                            success = false
                            break
                        }
                    }

                    success
                }

    }

    fun exist(name: String): Single<Boolean> {
        if (resolvedAll && get(name) != null) {
            return Single.just(true)
        }

        return resolveAllCoins()
                .toSingle { get(name) != null }
                .toObservable()
                .switchMap {
                    if (!it) {
                        return@switchMap coinsGateRepo.coinExists(name)
                    }

                    Observable.just(it)
                }
                .single(false)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
    }

    fun exist(id: BigInteger): Single<Boolean> {
        if (resolvedAll && get(id) != null) {
            return Single.just(true)
        }

        return resolveAllCoins()
                .toSingle { get(id) != null }
                .toObservable()
                .switchMap {
                    if (!it) {
                        return@switchMap coinsGateRepo.coinExists(id)
                    }

                    Observable.just(it)
                }
                .single(false)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
    }
}