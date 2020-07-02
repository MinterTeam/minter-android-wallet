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
package network.minter.bipwallet.delegation.adapter

import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import androidx.paging.PageKeyedDataSource
import io.reactivex.disposables.CompositeDisposable
import network.minter.bipwallet.apis.reactive.ReactiveExplorer
import network.minter.bipwallet.apis.reactive.rxExp
import network.minter.bipwallet.internal.adapter.LoadState
import network.minter.bipwallet.internal.helpers.data.CollectionsHelper.sortByValue
import network.minter.core.MinterSDK
import network.minter.core.crypto.MinterAddress
import network.minter.core.crypto.MinterPublicKey
import network.minter.explorer.models.DelegationList
import network.minter.explorer.models.ExpResult
import network.minter.explorer.repo.ExplorerAddressRepository
import timber.log.Timber
import java.math.BigDecimal
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by Alexander Kolpakov (jquickapp@gmail.com) on 06-Jun-19
 */
class DelegationDataSource(private val factory: Factory) : PageKeyedDataSource<Int, DelegatedItem>() {
    private val disposables = CompositeDisposable()
    private var validatorTotalStakes: MutableMap<MinterPublicKey, BigDecimal> = HashMap()

    override fun loadInitial(params: LoadInitialParams<Int>, callback: LoadInitialCallback<Int, DelegatedItem>) {
        factory.loadState.postValue(LoadState.Loading)

        factory.mRepo.getDelegations(factory.mainWallet, 1).rxExp()
                .onErrorResumeNext(ReactiveExplorer.toExpError())
                .map { mapToDelegationItem(it) }
                .doOnSubscribe { disposables.add(it) }
                .subscribe(
                        { res: ExpResult<MutableList<DelegatedItem>> ->
                            factory.loadState.postValue(LoadState.Loaded)
                            val lastPage = res.meta?.lastPage ?: 0

                            var nextPage: Int? = null
                            if (lastPage > 1) {
                                nextPage = (res.meta?.currentPage ?: 1) + 1
                            }

                            callback.onResult(res.result!!, null, nextPage)
                            if (res.result?.isEmpty() == true) {
                                factory.loadState.postValue(LoadState.Empty)
                            }
                        },
                        { t ->
                            Timber.w(t, "Unable to load delegations")
                            factory.loadState.postValue(LoadState.Failed)
                            callback.onResult(ArrayList(), null, null)
                        }
                )
    }

    override fun loadBefore(params: LoadParams<Int>, callback: LoadCallback<Int, DelegatedItem>) {
        factory.loadState.postValue(LoadState.Loading)
        factory.mRepo.getDelegations(factory.mainWallet, params.key).rxExp()
                .onErrorResumeNext(ReactiveExplorer.toExpError())
                .map { mapToDelegationItem(it) }
                .doOnSubscribe { disposables.add(it) }
                .subscribe(
                        { res: ExpResult<MutableList<DelegatedItem>> ->
                            factory.loadState.postValue(LoadState.Loaded)
                            callback.onResult(res.result!!, if (params.key == 1) null else params.key - 1)
                        },
                        { t ->
                            Timber.w(t, "Unable to load delegations")
                            factory.loadState.postValue(LoadState.Failed)
                            callback.onResult(ArrayList(), null)
                        }
                )
    }

    override fun loadAfter(params: LoadParams<Int>,
                           callback: LoadCallback<Int, DelegatedItem>) {

        factory.loadState.postValue(LoadState.Loading)

        factory.mRepo.getDelegations(factory.mainWallet, params.key).rxExp()
                .onErrorResumeNext(ReactiveExplorer.toExpError())
                .map { mapToDelegationItem(it) }
                .doOnSubscribe { disposables.add(it) }
                .subscribe(
                        { res: ExpResult<MutableList<DelegatedItem>> ->
                            factory.loadState.postValue(LoadState.Loaded)
                            callback.onResult(res.result!!, if (params.key + 1 > res.meta!!.lastPage) null else params.key + 1)
                        },
                        { t ->
                            Timber.w(t, "Unable to load delegations")
                            factory.loadState.postValue(LoadState.Failed)
                            callback.onResult(ArrayList(), null)
                        }
                )
    }

    override fun invalidate() {
        super.invalidate()
        disposables.dispose()
    }

    private fun mapToDelegationItem(res: ExpResult<DelegationList?>): ExpResult<MutableList<DelegatedItem>> {
        val out = ExpResult<MutableList<DelegatedItem>>()
        out.meta = res.meta
        out.error = res.error
        out.latestBlockTime = res.latestBlockTime
        out.links = res.links
        out.result = ArrayList()
        val stakes: MutableList<DelegatedStake> = ArrayList()
        if (res.result != null) {
            for ((_, delegations) in res.result!!.delegations) {
                for (delegation in delegations) {
                    val stake = DelegatedStake(delegation)
                    stakes.add(stake)
                    if (validatorTotalStakes.containsKey(stake.publicKey)) {
                        var stakeValue = validatorTotalStakes[stake.publicKey] ?: BigDecimal.ZERO
                        stakeValue += stake.amountBIP
                        validatorTotalStakes[stake.publicKey!!] = stakeValue
                    } else {
                        validatorTotalStakes[stake.publicKey!!] = stake.amountBIP
                    }
                }
            }
        }
        validatorTotalStakes = sortByValue(validatorTotalStakes, Comparator { o1: BigDecimal, o2: BigDecimal -> o2.compareTo(o1) })

        for ((key) in validatorTotalStakes) {
            val outStakes: MutableList<DelegatedStake> = ArrayList()
            for (s in stakes) {
                if (s.publicKey == key) {
                    outStakes.add(s)
                }
            }
            if (outStakes.isEmpty()) {
                continue
            }

            /*
            Collections.sort(outStakes, (o1, o2) -> o2.amountBIP.compareTo(o1.amountBIP));
             */
            outStakes.sortByDescending { it.amountBIP }

            val validator = DelegatedValidator(key, outStakes[0].validatorMeta!!)
            out.result!!.add(validator)
            out.result!!.addAll(outStakes)
        }
        return out
    }

    class StableCoinSorting : Comparator<DelegatedStake> {
        override fun compare(ac: DelegatedStake, bc: DelegatedStake): Int {
            val a = ac.coin!!.toLowerCase()
            val b = bc.coin!!.toLowerCase()
            if (a == b) // update to make it stable
                return 0
            if (a == sStable) return -1
            return if (b == sStable) 1 else a.compareTo(b)
        }

        companion object {
            private val sStable = MinterSDK.DEFAULT_COIN.toLowerCase()
        }
    }

    class Factory(
            val mRepo: ExplorerAddressRepository,
            val mainWallet: MinterAddress,
            val loadState: MutableLiveData<LoadState>
    ) : DataSource.Factory<Int, DelegatedItem>() {


        override fun create(): DataSource<Int, DelegatedItem> {
            return DelegationDataSource(this)
        }

    }

}