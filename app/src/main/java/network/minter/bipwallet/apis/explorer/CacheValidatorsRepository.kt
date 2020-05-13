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
import network.minter.bipwallet.BuildConfig
import network.minter.bipwallet.apis.reactive.rxExp
import network.minter.bipwallet.internal.data.CachedEntity
import network.minter.bipwallet.internal.data.CachedRepository
import network.minter.bipwallet.internal.storage.KVStorage
import network.minter.core.internal.api.ApiService
import network.minter.explorer.models.ExpResult
import network.minter.explorer.models.ValidatorItem
import network.minter.explorer.repo.ExplorerValidatorsRepository

typealias RepoValidators = CachedRepository<@JvmSuppressWildcards List<ValidatorItem>, CacheValidatorsRepository>

class CacheValidatorsRepository(
        private val storage: KVStorage,
        apiBuilder: ApiService.Builder
) : ExplorerValidatorsRepository(apiBuilder), CachedEntity<@JvmSuppressWildcards List<ValidatorItem>> {

    companion object {
        private const val KEY_VALIDATORS = BuildConfig.MINTER_STORAGE_VERS + "cached_explorer_validators_repository_list"
    }

    override fun getData(): List<ValidatorItem> {
        return storage[KEY_VALIDATORS, ArrayList(0)]
    }

    override fun getUpdatableData(): Observable<List<ValidatorItem>> {
        return instantService.validators.rxExp()
                .map { result: ExpResult<List<ValidatorItem>> -> result.result }
    }

    override fun onAfterUpdate(result: List<ValidatorItem>) {
        storage.putAsync(KEY_VALIDATORS, result.filter { it.status == ValidatorItem.STATUS_ONLINE }.toMutableList())
    }

    override fun onClear() {
        storage.delete(KEY_VALIDATORS)
    }

    override fun isDataReady(): Boolean {
        return storage.contains(KEY_VALIDATORS)
    }
}