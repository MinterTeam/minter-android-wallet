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
package network.minter.bipwallet.delegation.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Service
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.ChartTouchListener.ChartGesture
import com.github.mikephil.charting.listener.OnChartGestureListener
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.utils.MPPointF
import moxy.presenter.InjectPresenter
import moxy.presenter.ProvidePresenter
import network.minter.bipwallet.R
import network.minter.bipwallet.databinding.ActivityDelegationListBinding
import network.minter.bipwallet.delegation.adapter.DelegatedStake
import network.minter.bipwallet.delegation.adapter.DelegatedValidator
import network.minter.bipwallet.delegation.contract.DelegatedListView
import network.minter.bipwallet.delegation.views.DelegatedListPresenter
import network.minter.bipwallet.internal.BaseMvpInjectActivity
import network.minter.bipwallet.internal.adapter.LoadState
import network.minter.bipwallet.internal.helpers.ContextExtensions.getColorCompat
import network.minter.bipwallet.internal.helpers.DateHelper.toDateMonthOptYear
import network.minter.bipwallet.internal.helpers.MathHelper.bdHuman
import network.minter.bipwallet.internal.helpers.ViewExtensions.nvisible
import network.minter.bipwallet.internal.helpers.ViewExtensions.visible
import network.minter.bipwallet.internal.helpers.ViewExtensions.visibleForTestnet
import network.minter.bipwallet.internal.system.ActivityBuilder
import network.minter.bipwallet.internal.views.list.BorderedItemSeparator
import network.minter.core.crypto.MinterPublicKey
import network.minter.explorer.models.RewardStatistics
import org.joda.time.DateTime
import java.util.*
import javax.inject.Inject
import javax.inject.Provider

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
class DelegatedListActivity : BaseMvpInjectActivity(), DelegatedListView {
    @Inject lateinit var presenterProvider: Provider<DelegatedListPresenter>
    @InjectPresenter lateinit var presenter: DelegatedListPresenter
    private lateinit var binding: ActivityDelegationListBinding
    private var itemSeparator: BorderedItemSeparator? = null
    override fun setAdapter(adapter: RecyclerView.Adapter<*>) {
        binding.list.adapter = adapter
    }

    override fun showChartProgress() {
        binding.chartProgress.visible = true
    }

    override fun hideChartProgress() {
        binding.chartProgress.visible = false
    }

    override fun setOnRefreshListener(listener: OnRefreshListener) {
        binding.containerSwipeRefresh.setOnRefreshListener(listener)
    }

    override fun showRefreshProgress() {
        if (!binding.progress.nvisible) {
            binding.containerSwipeRefresh.isRefreshing = true
        }
    }

    override fun hideRefreshProgress() {
        binding.containerSwipeRefresh.isRefreshing = false
        hideProgress()
    }

    override fun showProgress() {
        if (!binding.containerSwipeRefresh.isRefreshing) {
            binding.progress.nvisible = true
        }
    }

    override fun hideProgress() {
        binding.progress.nvisible = false
    }

    override fun scrollTo(pos: Int) {
        binding.parentScroll.post {
            binding.parentScroll.scrollTo(binding.parentScroll.scrollX, pos)
        }
    }

    override fun showEmpty() {
        binding.emptyText.visible = true
    }

    fun showEmpty(msg: CharSequence?) {
        binding.emptyText.visible = msg != null
        binding.emptyText.text = msg
    }

    override fun syncProgress(loadState: MutableLiveData<LoadState>) {
        loadState.observe(this, Observer { s: LoadState ->
            when (s) {
                LoadState.Loaded,
                LoadState.Failed
                -> {
                    hideRefreshProgress()
                    hideProgress()
                    hideEmpty()
                    if (s == LoadState.Failed) {
                        showEmpty(resources.getString(R.string.error_unable_load_delegations))
                    }
                }
                LoadState.Loading -> showProgress()
                LoadState.Empty -> {
                    showEmpty()
                }
            }
        })
    }

    override fun setChartData(minMaxValue: FloatArray, dataSets: LineData) {
        binding.chart.data = dataSets
        //        chart.getAxisLeft().setAxisMinimum(minMaxValue[0]);
//        chart.getAxisLeft().setAxisMaximum(minMaxValue[1]+(minMaxValue[1]*1.5f));
        binding.chart.zoom(2f, 1f, dataSets.xMax, dataSets.yMax)
        binding.chart.invalidate()
    }

    override fun invalidateChartData(dataSets: LineData) {
        binding.chart.data = dataSets
        binding.chart.data.notifyDataChanged()
        binding.chart.notifyDataSetChanged()
    }

    override fun setBipsPerMinute(amount: String) {
        binding.bipPerMin.text = amount
    }

    override fun startDelegate(delegated: DelegatedValidator) {
        DelegateUnbondActivity.Builder(this, DelegateUnbondActivity.Type.Delegate)
                .setPublicKey(delegated.publicKey)
                .start()
    }

    override fun startDelegate(pubKey: MinterPublicKey) {
        DelegateUnbondActivity.Builder(this, DelegateUnbondActivity.Type.Delegate)
                .setPublicKey(pubKey)
                .start()
    }

    override fun startUnbond(delegated: DelegatedStake) {
        DelegateUnbondActivity.Builder(this, DelegateUnbondActivity.Type.Unbond)
                .setPublicKey(delegated.publicKey!!)
                .setSelectedCoin(delegated.coin!!)
                .start()
    }

    override fun hideChart() {
        binding.collapsingContent.visible = false
    }

    override fun showChart() {
        binding.collapsingContent.visible = true
    }

    override fun hideEmpty() {
        binding.emptyText.visible = false
    }

    @ProvidePresenter
    fun providePresenter(): DelegatedListPresenter {
        return presenterProvider.get()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDelegationListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.testnetWarning.visibleForTestnet()

        setupToolbar(binding.toolbar)
        presenter.handleExtras(intent)
        binding.parentScroll.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, _ ->
            presenter.onScrolledTo(scrollY)
        })
        binding.list.layoutManager = LinearLayoutManager(this)
        itemSeparator = BorderedItemSeparator(this, R.drawable.shape_bottom_separator, false, true)
        binding.list.addItemDecoration(itemSeparator!!)

        setupChart()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_delegation_list, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_delegate) {
            DelegateUnbondActivity.Builder(this, DelegateUnbondActivity.Type.Delegate)
                    .start()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupChart() {
        binding.chart.setBackgroundColor(Color.WHITE)
        // disable description text
        binding.chart.description.isEnabled = false
        val markerView = DateAmountMarkerView(this)
        markerView.chartView = binding.chart
        binding.chart.marker = markerView


        // enable touch gestures
        binding.chart.setTouchEnabled(true)
        binding.chart.onChartGestureListener = object : OnChartGestureListener {
            override fun onChartGestureStart(me: MotionEvent, lastPerformedGesture: ChartGesture) {}
            override fun onChartGestureEnd(me: MotionEvent, lastPerformedGesture: ChartGesture) {
                if (lastPerformedGesture == ChartGesture.DRAG) {
                    presenter.onChartDraggedTo(DateTime(binding.chart.lowestVisibleX.toLong()))
                    //                    Timber.d("OnChartGestureEnd: %s, %s", me.toString(), lastPerformedGesture.name());
//                    Timber.d("XChartMin %f", chart.getXChartMin());
//                    Timber.d("YChartMin %f", chart.getYChartMin());
//                    Timber.d("Lowest VisibleX %f (%s)",
//                            chart.getLowestVisibleX(),
//                            DateHelper.toSimpleISODate()
//                            ));
                }
            }

            override fun onChartLongPressed(me: MotionEvent) {}
            override fun onChartDoubleTapped(me: MotionEvent) {}
            override fun onChartSingleTapped(me: MotionEvent) {}
            override fun onChartFling(me1: MotionEvent, me2: MotionEvent, velocityX: Float, velocityY: Float) {}
            override fun onChartScale(me: MotionEvent, scaleX: Float, scaleY: Float) {}
            override fun onChartTranslate(me: MotionEvent, dX: Float, dY: Float) {}
        }
//        val pointXYOffset = Wallet.app().display().dpToPx(6) / 2f


        // set listeners
        binding.chart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry, h: Highlight) {
//                chartSelectedPoint.setX(h.getXPx()-pointXYOffset);
//                chartSelectedPoint.setY(h.getYPx()+pointXYOffset);
//                chartSelectedPoint.setVisibility(View.VISIBLE);
            }

            override fun onNothingSelected() {
//                chartSelectedPoint.setVisibility(View.INVISIBLE);
            }
        })
        binding.chart.isAutoScaleMinMaxEnabled = true
        binding.chart.legend.isEnabled = false
        binding.chart.setDrawGridBackground(false)
        // enable scaling and dragging
        binding.chart.isDragEnabled = true
        binding.chart.isScaleXEnabled = true
        binding.chart.isScaleYEnabled = false
        // force pinch zoom along both axis
        binding.chart.setPinchZoom(true)
        binding.chart.isVerticalScrollBarEnabled = false
        var xAxis: XAxis
        run {
            // // X-Axis Style // //
            xAxis = binding.chart.xAxis
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.textSize = 11f
            xAxis.typeface = ResourcesCompat.getFont(this, R.font._inter_semi_bold)
            xAxis.textColor = getColorCompat(R.color.textColorGrey)
            xAxis.valueFormatter = object : ValueFormatter() {
                override fun getAxisLabel(value: Float, axis: AxisBase): String {
                    return toDateMonthOptYear(Date(value.toLong()))
                }
            }
            // vertical grid lines
            xAxis.axisLineColor = getColorCompat(R.color.greyLight)
            xAxis.axisLineWidth = 1f
            xAxis.textSize = 11f
        }
        var yAxis: YAxis
        run {
            yAxis = binding.chart.axisLeft
            yAxis.setDrawZeroLine(false)
            yAxis.isEnabled = false
            yAxis.setDrawAxisLine(false)
            yAxis.setDrawGridLines(false)
            yAxis.setDrawLabels(false)
            yAxis.setDrawZeroLine(false)
            yAxis = binding.chart.axisRight
            yAxis.setDrawZeroLine(false)
            yAxis.isEnabled = false
            yAxis.setDrawAxisLine(false)
            yAxis.setDrawGridLines(false)
            yAxis.setDrawLabels(false)
            yAxis.setDrawZeroLine(false)
        }
    }

    private class DateAmountMarkerView(context: Context?) : MarkerView(context, R.layout.view_chart_marker) {
        private val amount: TextView = findViewById(R.id.amount)
        private val date: TextView = findViewById(R.id.date)

        override fun getOffset(): MPPointF {
            return MPPointF(0.0f, (-(height + 10)).toFloat())
        }

        override fun refreshContent(e: Entry, highlight: Highlight) {
            val data = e.data as RewardStatistics
            amount.text = bdHuman(data.amount)
            date.text = toDateMonthOptYear(data.time!!)
            super.refreshContent(e, highlight)
        }
    }

    class Builder : ActivityBuilder {
        constructor(from: Activity) : super(from)
        constructor(from: Fragment) : super(from)
        constructor(from: Service) : super(from)

        override fun getActivityClass(): Class<*> {
            return DelegatedListActivity::class.java
        }
    }
}