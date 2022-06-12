/*
 * Copyright (C) by MinterTeam. 2022
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
package network.minter.bipwallet.delegation.views

import androidx.lifecycle.MutableLiveData
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import moxy.InjectViewState
import network.minter.bipwallet.R
import network.minter.bipwallet.apis.explorer.RepoMonthlyRewards
import network.minter.bipwallet.apis.explorer.RepoValidators
import network.minter.bipwallet.delegation.adapter.DelegatedItemDiffUtil
import network.minter.bipwallet.delegation.adapter.DelegationDataSource
import network.minter.bipwallet.delegation.adapter.DelegationListAdapter
import network.minter.bipwallet.delegation.contract.DelegatedListView
import network.minter.bipwallet.internal.Wallet
import network.minter.bipwallet.internal.adapter.LoadState
import network.minter.bipwallet.internal.exceptions.ErrorManager
import network.minter.bipwallet.internal.exceptions.RetryListener
import network.minter.bipwallet.internal.helpers.DateHelper
import network.minter.bipwallet.internal.helpers.DateHelper.day
import network.minter.bipwallet.internal.helpers.DateHelper.toSimpleISODate
import network.minter.bipwallet.internal.helpers.DisplayHelper
import network.minter.bipwallet.internal.helpers.MathHelper.bdGT
import network.minter.bipwallet.internal.helpers.MathHelper.bdLT
import network.minter.bipwallet.internal.helpers.MathHelper.humanize
import network.minter.bipwallet.internal.mvp.MvpBasePresenter
import network.minter.bipwallet.internal.storage.SecretStorage
import network.minter.blockchain.models.operational.Transaction
import network.minter.explorer.models.ExpResult
import network.minter.explorer.models.RewardStatistics
import network.minter.explorer.repo.ExplorerAddressRepository
import org.joda.time.DateTime
import org.joda.time.Days
import timber.log.Timber
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*
import javax.inject.Inject

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
@InjectViewState
class DelegatedListPresenter @Inject constructor() : MvpBasePresenter<DelegatedListView>(), ErrorManager.ErrorGlobalHandlerListener {
    @Inject lateinit var addressRepo: ExplorerAddressRepository
    @Inject lateinit var validatorsRepo: RepoValidators
    @Inject lateinit var rewardsMonthlyRepo: RepoMonthlyRewards
    @Inject lateinit var secretRepo: SecretStorage
    @Inject lateinit var display: DisplayHelper
    @Inject lateinit var errorManager: ErrorManager

    private var adapter: DelegationListAdapter? = null
    private var dataSource: DelegationDataSource? = null
    private var lastScrollPosition = 0
    private var loadState: MutableLiveData<LoadState>? = null
    private var hasInWaitList: MutableLiveData<Boolean>? = null
    private var rewardsSet: LineDataSet? = null
    private var pastLastDateChart: DateTime? = null
    private var futureLastDateChart: DateTime? = null
    private var noMoreRewards = false
    private var cachedRewards: MutableList<RewardStatistics> = ArrayList()
    private var firstRewardsLoad = true
    private var rewardsPerMinuteSettled = false

    override fun onError(t: Throwable, retryListener: RetryListener) {
        loadState?.postValue(LoadState.Failed)
        handlerError(t, retryListener)
    }

    override fun attachView(view: DelegatedListView) {
        super.attachView(view)
        viewState.setAdapter(adapter!!)
        viewState.setOnRefreshListener { onRefresh() }
        viewState.scrollTo(lastScrollPosition)
    }

    fun onScrolledTo(scrollY: Int) {
        lastScrollPosition = scrollY
    }

    fun onChartDraggedTo(dateTime: DateTime?) {
        val days = Days.daysBetween(dateTime, pastLastDateChart)
        if (days.days >= -1) {
            loadRewards()
        }
    }

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()
        adapter = DelegationListAdapter()
        adapter!!.setOnDelegatedClickListener {
            viewState.startDelegate(it)
        }
        adapter!!.setOnUnbondItemClickListener {
            viewState.startUnbond(it)
        }

//        if (rewardsMonthlyRepo.isDataReady) {
//            cachedRewards = rewardsMonthlyRepo.data
//        } else {
//            rewardsMonthlyRepo.update()
//        }

        loadState = MutableLiveData()
        hasInWaitList = MutableLiveData()
        viewState.syncProgress(loadState!!)
        viewState.syncHasInWaitList(hasInWaitList!!)
        adapter!!.setLoadState(loadState)
        dataSource = DelegationDataSource.Factory(
                addressRepo,
                validatorsRepo,
                secretRepo.mainWallet,
                loadState!!,
                hasInWaitList!!,
                errorManager.retryWhenHandler
        ).create()
        refresh()
        unsubscribeOnDestroy(dataSource?.disposable)
//        loadRewards()
    }

    private fun loadRewards() {
        if (noMoreRewards) {
            return
        }

        futureLastDateChart = if (pastLastDateChart == null) {
            DateHelper.flatDay(DateTime())
        } else {
            pastLastDateChart!! - 1.day()
        }
        pastLastDateChart = futureLastDateChart!!.minusDays(30)

        viewState!!.showChartProgress()
        if (firstRewardsLoad && cachedRewards.isNotEmpty()) {
            Timber.d("Getting rewards from cache")
            firstRewardsLoad = false
            val tmp = ExpResult<List<RewardStatistics>>()
            tmp.result = cachedRewards

            onRewardsLoaded(tmp)
            return
        }

        addressRepo
                .getRewardStatistics(secretRepo.mainWallet, toSimpleISODate(pastLastDateChart!!), toSimpleISODate(futureLastDateChart!!))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(
                        { rewardsRes: ExpResult<List<RewardStatistics>> ->
                            onRewardsLoaded(rewardsRes)
                        },
                        { t: Throwable? -> Timber.w(t, "Unable to get rewards statistics") }
                )
                .disposeOnDestroy()

    }

    private fun rewardsToEntry(stats: List<RewardStatistics>): List<Entry> {
        return stats.map {
            Entry(it.time!!.time.toFloat(), it.amount.setScale(4, RoundingMode.HALF_DOWN).toFloat(), it)
        }.toList()
    }

    private fun getMinMaxRewardAmount(res: List<RewardStatistics>): FloatArray {
        var maxVal = BigDecimal.ONE
        var minVal = Transaction.VALUE_MUL_DEC
        for (r in res) {
            if (bdGT(r.amount, maxVal)) {
                maxVal = r.amount
            }
            if (bdLT(r.amount, minVal)) {
                minVal = r.amount
            }
        }
        return floatArrayOf(minVal.toFloat(), maxVal.toFloat())
    }

    private fun calcRewardsPerMinutes(rewardsRes: ExpResult<List<RewardStatistics>>) {
        if (rewardsPerMinuteSettled) {
            return
        }

        var hours = 24.0f
        // using past day, because today we don't have enough data
        val last: RewardStatistics = if (rewardsRes.result!!.size >= 2) {
            rewardsRes.result!![rewardsRes.result!!.size - 2]
        } else {
            hours = DateTime().hourOfDay.toFloat()
            hours += ((DateTime().minuteOfHour * 100.0f / 60.0f) / 100.0f)
            rewardsRes.result!![rewardsRes.result!!.size - 1]
        }
        val secondsDivider = BigDecimal("60") * BigDecimal(hours.toString())

        val perMinutes = last.amount.divide(secondsDivider, RoundingMode.HALF_DOWN)
        viewState!!.setBipsPerMinute(perMinutes.humanize())
        rewardsPerMinuteSettled = true

    }

    @Suppress("DEPRECATION")
    private fun onRewardsLoaded(rewardsRes: ExpResult<List<RewardStatistics>>) {
        if (!rewardsRes.isOk || rewardsRes.result.isEmpty()) {
            viewState.setBipsPerMinute("0")
            viewState.hideChartProgress()
            viewState.hideChart()
            return
        }
        viewState.showChart()

        calcRewardsPerMinutes(rewardsRes)

        if (rewardsSet == null) {
            rewardsSet = LineDataSet(rewardsToEntry(rewardsRes.result!!), null)
            rewardsSet!!.lineWidth = 2f
            rewardsSet!!.color = Wallet.app().res().getColor(R.color.colorPrimaryOrange)
            rewardsSet!!.setDrawVerticalHighlightIndicator(true)
            rewardsSet!!.setDrawHorizontalHighlightIndicator(false)
            rewardsSet!!.highlightLineWidth = 2f
            rewardsSet!!.highLightColor = Wallet.app().res().getColor(R.color.colorPrimaryLighter)
            rewardsSet!!.setDrawValues(false)
            rewardsSet!!.setDrawIcons(false)
            rewardsSet!!.setDrawCircles(false)
            rewardsSet!!.setDrawFilled(true)
            rewardsSet!!.fillDrawable = Wallet.app().res().getDrawable(R.drawable.fade_orange)
            val dataSets = LineData(rewardsSet)
            viewState!!.setChartData(getMinMaxRewardAmount(rewardsRes.result!!), dataSets)
        } else {
            if (rewardsRes.result!!.isEmpty()) {
                noMoreRewards = true
                viewState!!.hideChartProgress()
                return
            }
            val oldVals = rewardsSet!!.values.toMutableList()
            val newVals = rewardsToEntry(rewardsRes.result!!)
            oldVals.addAll(newVals)
            oldVals.sortWith { o1, o2 -> o1.x.compareTo(o2.x) }
            rewardsSet!!.values = oldVals
            val dataSets = LineData(rewardsSet)
            viewState.invalidateChartData(dataSets)
        }
        viewState.hideChartProgress()
    }

    private fun onRefresh() {
        dataSource?.invalidate()
        viewState.scrollTo(0)
        refresh()
    }

    private fun refresh() {
        errorManager.retryListener()
        dataSource!!.load {
            viewState!!.hideRefreshProgress()
            adapter?.dispatchChanges(DelegatedItemDiffUtil::class.java, it, true)
        }
    }
}