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
package network.minter.bipwallet.delegation.views

import androidx.lifecycle.MutableLiveData
import androidx.paging.PagedList
import androidx.paging.RxPagedListBuilder
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.annimon.stream.Stream
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import moxy.InjectViewState
import network.minter.bipwallet.R
import network.minter.bipwallet.apis.explorer.RepoMonthlyRewards
import network.minter.bipwallet.apis.reactive.rxExp
import network.minter.bipwallet.delegation.adapter.DelegatedItem
import network.minter.bipwallet.delegation.adapter.DelegationDataSource
import network.minter.bipwallet.delegation.adapter.DelegationListAdapter
import network.minter.bipwallet.delegation.contract.DelegationListView
import network.minter.bipwallet.internal.Wallet
import network.minter.bipwallet.internal.adapter.LoadState
import network.minter.bipwallet.internal.helpers.DateHelper
import network.minter.bipwallet.internal.helpers.DateHelper.day
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
import kotlin.collections.ArrayList

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
@InjectViewState
class DelegationListPresenter @Inject constructor() : MvpBasePresenter<DelegationListView>() {
    @Inject lateinit var addressRepo: ExplorerAddressRepository
    @Inject lateinit var rewardsMonthlyRepo: RepoMonthlyRewards
    @Inject lateinit var secretRepo: SecretStorage
    @Inject lateinit var display: DisplayHelper

    private var adapter: DelegationListAdapter? = null
    private var sourceFactory: DelegationDataSource.Factory? = null
    private var listDisposable: Disposable? = null
    private var listBuilder: RxPagedListBuilder<Int, DelegatedItem>? = null
    private var lastScrollPosition = 0
    private var loadState: MutableLiveData<LoadState>? = null
    private var rewardsSet: LineDataSet? = null
    private var pastLastDateChart: DateTime? = null
    private var futureLastDateChart: DateTime? = null
    private var noMoreRewards = false
    private var cachedRewards: MutableList<RewardStatistics> = ArrayList()
    private var firstRewardsLoad = true
    private var rewardsPerMinuteSettled = false

    override fun attachView(view: DelegationListView) {
        super.attachView(view)
        viewState.setAdapter(adapter!!)
        viewState.setOnRefreshListener(SwipeRefreshLayout.OnRefreshListener {
            onRefresh()
        })
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

        if (rewardsMonthlyRepo.isDataReady) {
            cachedRewards = rewardsMonthlyRepo.data
        } else {
            rewardsMonthlyRepo.update()
        }

        loadState = MutableLiveData()
        viewState.syncProgress(loadState!!)
        adapter!!.setLoadState(loadState)
        sourceFactory = DelegationDataSource.Factory(addressRepo, secretRepo.addresses, loadState!!)
        val cfg = PagedList.Config.Builder()
                .setPageSize(50)
                .setEnablePlaceholders(false)
                .build()
        listBuilder = RxPagedListBuilder(sourceFactory!!, cfg)
        refresh()
        unsubscribeOnDestroy(listDisposable)
        loadRewards()
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
            val tmp = ExpResult<MutableList<RewardStatistics>>()
            tmp.result = cachedRewards

            onRewardsLoaded(tmp)
            return
        }

        addressRepo
                .getRewardStatistics(secretRepo.mainWallet, DateHelper.toSimpleISODate(pastLastDateChart), DateHelper.toSimpleISODate(futureLastDateChart))
                .rxExp()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe({ rewardsRes: ExpResult<MutableList<RewardStatistics>> -> onRewardsLoaded(rewardsRes) }) { t: Throwable? -> Timber.w(t) }
                .disposeOnDestroy()

    }

    private fun rewardsToEntry(stats: List<RewardStatistics>): List<Entry> {
        return Stream.of(stats)
                .map { item: RewardStatistics -> Entry(item.time.time.toFloat(), item.amount.setScale(4, RoundingMode.HALF_DOWN).toFloat(), item) }
                .toList()
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

    private fun calcRewardsPerMinutes(rewardsRes: ExpResult<MutableList<RewardStatistics>>) {
        if (rewardsPerMinuteSettled) {
            return
        }

        var hours = 24.0f
        // using past day, because today we don't have enough data
        val last: RewardStatistics = if (rewardsRes.result.size >= 2) {
            rewardsRes.result[rewardsRes.result.size - 2]
        } else {
            hours = DateTime().hourOfDay.toFloat()
            hours += ((DateTime().minuteOfHour * 100.0f / 60.0f) / 100.0f)
            rewardsRes.result[rewardsRes.result.size - 1]
        }
        val secondsDivider = BigDecimal("60") * BigDecimal(hours.toString())

        val perMinutes = last.amount.divide(secondsDivider, RoundingMode.HALF_DOWN)
        viewState!!.setBipsPerMinute(perMinutes.humanize())
        rewardsPerMinuteSettled = true

    }

    private fun onRewardsLoaded(rewardsRes: ExpResult<MutableList<RewardStatistics>>) {
        if (!rewardsRes.isOk || rewardsRes.result.isEmpty()) {
            viewState.setBipsPerMinute("0")
            viewState.hideChartProgress()
            viewState.hideChart()
            return
        }
        viewState.showChart()

        calcRewardsPerMinutes(rewardsRes)

        if (rewardsSet == null) {
            rewardsSet = LineDataSet(rewardsToEntry(rewardsRes.result), null)
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
            viewState!!.setChartData(getMinMaxRewardAmount(rewardsRes.result), dataSets)
        } else {
            if (rewardsRes.result.isEmpty()) {
                noMoreRewards = true
                viewState!!.hideChartProgress()
                return
            }
            val oldVals = rewardsSet!!.values
            val newVals = rewardsToEntry(rewardsRes.result)
            oldVals.addAll(newVals)
            Collections.sort(oldVals) { o1, o2 -> java.lang.Float.compare(o1.x, o2.x) }
            rewardsSet!!.values = oldVals
            val dataSets = LineData(rewardsSet)
            viewState.invalidateChartData(dataSets)
        }
        viewState.hideChartProgress()
    }

    private fun onRefresh() {
        listDisposable!!.dispose()
        viewState.scrollTo(0)
        refresh()
    }

    private fun refresh() {
        listDisposable = listBuilder!!.buildObservable()
                .subscribe { res: PagedList<DelegatedItem> ->
                    viewState!!.hideRefreshProgress()
                    adapter!!.submitList(res)
                }
                .disposeOnDestroy()
    }
}