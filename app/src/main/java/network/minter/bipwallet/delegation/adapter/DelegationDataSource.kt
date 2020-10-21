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
import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Function
import io.reactivex.schedulers.Schedulers
import network.minter.bipwallet.apis.explorer.RepoValidators
import network.minter.bipwallet.internal.adapter.LoadState
import network.minter.bipwallet.internal.helpers.data.CollectionsHelper.sortByValue
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
class DelegationDataSource(private val factory: Factory) {
    var disposable: Disposable? = null
    private var validatorTotalStakes: MutableMap<MinterPublicKey, BigDecimal> = HashMap()

    /**
     * Find kicked stake in stake list
     */
    private fun ExpResult<MutableList<DelegatedItem>>.hasKicked(): Boolean {
        var hasKicked = false

        for (item in result) {
            if (item is DelegatedStake && item.isKicked) {
                hasKicked = true
                break
            }
        }

        return hasKicked
    }

    fun load(callback: (MutableList<DelegatedItem>) -> Unit) {
        factory.loadState.postValue(LoadState.Loading)

        disposable = factory.repo.getDelegations(factory.mainWallet)
                .retryWhen(factory.retryWhenHandler)
                .map { mapToDelegationItem(it) }
                .switchMap { mapValidatorCommissionAndMinStake(it) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(
                        { res: ExpResult<MutableList<DelegatedItem>> ->
                            factory.loadState.postValue(LoadState.Loaded)

                            // notify receiver we have kicked stake
                            factory.hasInWaitList.postValue(res.hasKicked())

                            callback(res.result)

                            if (res.result?.isEmpty() == true) {
                                factory.loadState.postValue(LoadState.Empty)
                            }
                        },
                        { t ->
                            Timber.w(t, "Unable to load delegations")
                            factory.hasInWaitList.postValue(false)
                            factory.loadState.postValue(LoadState.Failed)
                            callback(ArrayList())
                        }
                )
    }

    fun invalidate() {
        disposable?.dispose()
    }

    private fun mapValidatorCommissionAndMinStake(res: ExpResult<MutableList<DelegatedItem>>): ObservableSource<ExpResult<MutableList<DelegatedItem>>> {
        return factory.validatorsRepo.fetch()
                .map { validators ->
                    for (item in res.result) {
                        if (item is DelegatedValidator) {
                            val validator = validators.findLast { it.pubKey == item.publicKey }
                            if (validator != null) {
                                item.minStake = validator.minStake
                                item.commission = validator.commission
                            }
                        }
                    }

                    res
                }
    }

    /**
     * Convert CoinDelegation to adapter-specific model DelegatedItem (Stake or Validator) to show them in single recyclerview
     */
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

    class Factory(
            val repo: ExplorerAddressRepository,
            val validatorsRepo: RepoValidators,
            val mainWallet: MinterAddress,
            val loadState: MutableLiveData<LoadState>,
            val hasInWaitList: MutableLiveData<Boolean>,
            val retryWhenHandler: Function<Observable<out Throwable>, ObservableSource<*>>
    ) {
        fun create(): DelegationDataSource {
            return DelegationDataSource(this)
        }
    }

}