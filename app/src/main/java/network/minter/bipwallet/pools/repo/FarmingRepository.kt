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

package network.minter.bipwallet.pools.repo

import io.reactivex.Observable
import network.minter.bipwallet.BuildConfig
import network.minter.bipwallet.internal.data.CachedEntity
import network.minter.bipwallet.internal.data.CachedRepository
import network.minter.bipwallet.internal.storage.KVStorage
import network.minter.bipwallet.pools.models.FarmingItem
import network.minter.core.crypto.MinterAddress
import network.minter.core.internal.api.ApiService
import network.minter.core.internal.api.converters.MinterAddressJsonConverter
import network.minter.core.internal.data.DataRepository

/**
 * minter-android-wallet. 2021
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */

typealias RepoCachedFarming = CachedRepository<@JvmSuppressWildcards List<FarmingItem>, FarmingRepository>

class FarmingRepository(
        chainikApiService: ApiService.Builder,
        private val storage: KVStorage,
): DataRepository<FarmingChainikEndpoint>(chainikApiService), CachedEntity<@JvmSuppressWildcards List<FarmingItem>>, DataRepository.Configurator {

    companion object {
        private const val KEY_FARMINGS = BuildConfig.MINTER_CACHE_VERS + "farmings_default"
        private const val KEY_FARMINGS_MAP = BuildConfig.MINTER_CACHE_VERS + "farmings_lp_map"
    }

    fun getByLpToken(tokenName: String): FarmingItem? {
        val map: Map<String, FarmingItem> = storage[KEY_FARMINGS_MAP, emptyMap()]

        return if(map.containsKey(tokenName)) {
            map[tokenName]
        } else {
            null
        }
    }

    override fun getServiceClass(): Class<FarmingChainikEndpoint> {
        return FarmingChainikEndpoint::class.java
    }

    override fun getData(): List<FarmingItem> {
        return storage[KEY_FARMINGS, emptyList()]
    }

    override fun getUpdatableData(): Observable<List<FarmingItem>> {
        if(!BuildConfig.FLAVOR.startsWith("netMain")) {
            return Observable.just(emptyList())
        }
        return instantService.getPrograms()
                .map {
                    if(!it.isOk()) {
                        emptyList()
                    } else {
                        it.data
                    }
                }
                .onErrorResumeNext(Observable.just(emptyList()))
    }

    override fun onAfterUpdate(result: List<FarmingItem>) {
        storage.put(KEY_FARMINGS, result)
        val map = HashMap<String, FarmingItem>()
        result.forEach {
            map["LP-${it.poolId}"] = it
        }
        storage.put(KEY_FARMINGS_MAP, map)
    }

    override fun onClear() {
        storage.delete(KEY_FARMINGS)
        storage.delete(KEY_FARMINGS_MAP)
    }

    override fun isDataReady(): Boolean {
        return storage.contains(KEY_FARMINGS)
    }

    override fun configure(api: ApiService.Builder) {
        api.registerTypeAdapter(MinterAddress::class.java, MinterAddressJsonConverter())
    }
}