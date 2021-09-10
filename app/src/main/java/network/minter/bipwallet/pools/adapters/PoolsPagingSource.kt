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

package network.minter.bipwallet.pools.adapters

import androidx.lifecycle.MutableLiveData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.rxjava2.RxPagingSource
import io.reactivex.Observable
import io.reactivex.Single
import network.minter.bipwallet.internal.adapter.LoadState
import network.minter.bipwallet.internal.storage.SecretStorage
import network.minter.bipwallet.pools.models.PoolCombined
import network.minter.bipwallet.pools.models.PoolsFilter
import network.minter.bipwallet.pools.repo.RepoCachedFarming
import network.minter.bipwallet.pools.repo.RepoCachedUserPools
import network.minter.core.crypto.MinterAddress
import network.minter.explorer.models.ExpResult
import network.minter.explorer.models.Pool
import network.minter.explorer.repo.ExplorerPoolsRepository
import network.minter.explorer.repo.PageOpts
import timber.log.Timber
import javax.inject.Inject

/**
 * minter-android-wallet. 2021
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
class PoolsPagingSource(private val factory: Factory) : RxPagingSource<Int, PoolCombined>() {

    class Factory @Inject constructor(
            internal val secretStorage: SecretStorage
    ) {
        @Inject lateinit var poolsRepo: ExplorerPoolsRepository
        @Inject lateinit var farmingRepo: RepoCachedFarming
        @Inject lateinit var userPoolsRepo: RepoCachedUserPools

        var loadState: MutableLiveData<LoadState>? = null
        var filterType: PoolsFilter = PoolsFilter.None
        var filterCoin: String? = null

        val myAddress: MinterAddress
            get() {
                return secretStorage.mainWallet
            }

        fun create(forceUpdate: Boolean = false): PagingSource<Int, PoolCombined> {
            if(forceUpdate) {
                Timber.d("Force refresh farming and user pools")
            }
            farmingRepo.update(forceUpdate)
            userPoolsRepo.update(forceUpdate)
            return PoolsPagingSource(this)
        }

        fun observeLoadState(loadState: MutableLiveData<LoadState>) {
            this.loadState = loadState
        }

    }

    override fun getRefreshKey(state: PagingState<Int, PoolCombined>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                    ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override fun loadSingle(params: LoadParams<Int>): Single<LoadResult<Int, PoolCombined>> {
//        factory.loadState?.postValue(LoadState.Loading)

        val pageOpts = PageOpts()
        pageOpts.page = params.key ?: 1
        pageOpts.limit = 50

        val allPoolsObservable: Observable<ExpResult<List<Pool>>>
        val hasPagination = factory.filterType == PoolsFilter.None

        when (factory.filterType) {
            PoolsFilter.Farming -> {
                allPoolsObservable = factory.farmingRepo.fetch()
                        .map { farmings ->
                            val res = ExpResult<List<Pool>>()
                            res.result = farmings.map { it.toPool() }
                            res
                        }
            }
            PoolsFilter.Staked -> {
                allPoolsObservable = factory.userPoolsRepo.fetch()
                        .map { userPools ->
                            val res = ExpResult<List<Pool>>()
                            res.result = userPools.map { it }
                            res
                        }
            }
            else -> {
                allPoolsObservable = factory.poolsRepo.getPools(pageOpts)
            }
        }

        return Observable
                .combineLatest(
                        factory.farmingRepo.fetch(),
                        factory.userPoolsRepo.fetch(),
                        allPoolsObservable,
                        { _, _, c ->
                            Timber.d("POOLS_LIST: data prepared to singleOrError")
                            c
                        }
                )
                .lastElement()
                .toSingle()
                .map {
                    if (!it.isOk) {
                        factory.loadState?.postValue(LoadState.Failed)
                        Timber.e("POOLS_LIST: data error")
                        LoadResult.Error(
                                RuntimeException(it.error.message)
                        )
                    } else {
                        var nextKey: Int? = null
                        var prevKey: Int? = null

                        if (hasPagination) {
                            nextKey = if (it.meta.currentPage == 1) null else it.meta.currentPage - 1
                            prevKey = if (it.meta.currentPage == it.meta.lastPage) null else it.meta.currentPage + 1
                        }

                        Timber.d("POOLS_LIST: data loaded")
                        val data = it.result.map { pool ->
                            val farmingItem = factory.farmingRepo.entity.getByLpToken(pool.token.symbol)
                            val stake = factory.userPoolsRepo.entity.getByLpToken(pool.token.symbol)
                            val combined = PoolCombined(pool, farmingItem, stake, factory.filterType)
                            combined
                        }.filter { pool ->
                            if (factory.filterCoin == null || factory.filterCoin!!.length < 3) {
                                true
                            } else {
                                pool.pool.coin0.symbol.contains(factory.filterCoin!!, true) ||
                                        pool.pool.coin1.symbol.contains(factory.filterCoin!!, true) ||
                                        (factory.filterCoin!!.startsWith("LP-") && factory.filterCoin!!.length > 3 && pool.pool.token.symbol == factory.filterCoin)
                            }
                        }

                        if(data.isEmpty()) {
                            factory.loadState?.postValue(LoadState.Empty)
                        } else {
                            factory.loadState?.postValue(LoadState.Loaded)
                        }

                        LoadResult.Page<Int, PoolCombined>(
                                data = data,
                                prevKey = nextKey,
                                nextKey = prevKey
                        )
                    }
                }
                .onErrorReturn {
                    LoadResult.Error(it)
                }

    }
}