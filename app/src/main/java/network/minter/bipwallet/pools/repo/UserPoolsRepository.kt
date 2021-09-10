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
import network.minter.bipwallet.internal.storage.SecretStorage
import network.minter.explorer.models.PoolProvider
import network.minter.explorer.repo.ExplorerPoolsRepository

/**
 * minter-android-wallet. 2021
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */

typealias RepoCachedUserPools = CachedRepository<@JvmSuppressWildcards List<PoolProvider>, UserPoolsRepository>

class UserPoolsRepository(
        private val secretStorage: SecretStorage,
        private val storage: KVStorage,
        private val poolRepo: ExplorerPoolsRepository
) : CachedEntity<@JvmSuppressWildcards List<PoolProvider>> {

    companion object {
        const val KEY_USER_POOLS = BuildConfig.MINTER_CACHE_VERS + "user_pools_"
        const val KEY_USER_POOLS_MAP = BuildConfig.MINTER_CACHE_VERS + "user_pools_map_"
    }

    private val cacheKey: String
        get() {
            return KEY_USER_POOLS + "${secretStorage.mainWallet}"
        }

    private val cacheKeyMap: String
        get() {
            return KEY_USER_POOLS_MAP + "${secretStorage.mainWallet}"
        }

    fun getByLpToken(token: String): PoolProvider? {
        val map: Map<String, PoolProvider> = storage[cacheKeyMap, emptyMap()]
        return if (map.containsKey(token)) {
            map[token]
        } else {
            null
        }
    }

    override fun getData(): List<PoolProvider> {
        return storage[cacheKey, emptyList()]
    }

    override fun getUpdatableData(): Observable<List<PoolProvider>> {
        return poolRepo.getProvidersByAddress(secretStorage.mainWallet.toString())
                .map {
                    if (!it.isOk) {
                        return@map emptyList()
                    }
                    it.result
                }
    }

    override fun onAfterUpdate(result: List<PoolProvider>) {
        storage.put(cacheKey, result)
        val map = HashMap<String, PoolProvider>()
        result.forEach {
            map[it.token.symbol] = it
        }
        storage.put(cacheKeyMap, map)
    }

    override fun onClear() {
        storage.delete(cacheKey)
        storage.delete(cacheKeyMap)
    }

    override fun isDataReady(): Boolean {
        return storage.contains(cacheKey) && storage.contains(cacheKeyMap)
    }
}