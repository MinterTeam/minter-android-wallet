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

package network.minter.bipwallet.apis.explorer

import io.reactivex.Observable
import network.minter.bipwallet.BuildConfig
import network.minter.bipwallet.internal.data.CachedEntity
import network.minter.bipwallet.internal.data.CachedRepository
import network.minter.bipwallet.internal.storage.KVStorage
import network.minter.explorer.repo.ExplorerStatusRepository
import java.math.BigDecimal

typealias RepoCachedBipUsdRate = CachedRepository<@JvmSuppressWildcards BigDecimal, BipToUsdRepository>

/**
 * minter-android-wallet. 2021
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
class BipToUsdRepository(
        private val storage: KVStorage,
        private val statusRepo: ExplorerStatusRepository
) : CachedEntity<BigDecimal> {

    companion object {
        private const val KEY_CACHE_BIP_USD_RATE = BuildConfig.MINTER_CACHE_VERS + "_bip_usd_rate_decimal"
    }

    override fun getData(): BigDecimal {
        return storage[KEY_CACHE_BIP_USD_RATE, BigDecimal.ONE]
    }

    override fun getUpdatableData(): Observable<BigDecimal> {
        return statusRepo.status
                .map {
                    if (!it.isOk) {
                        return@map BigDecimal.ONE
                    }

                    it.result.bipPriceUsd
                }
    }

    override fun onAfterUpdate(result: BigDecimal) {
        storage.put(KEY_CACHE_BIP_USD_RATE, result)
    }

    override fun onClear() {
        storage.delete(KEY_CACHE_BIP_USD_RATE)
    }

    override fun isDataReady(): Boolean {
        return storage.contains(KEY_CACHE_BIP_USD_RATE)
    }
}