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
package network.minter.bipwallet.delegation.contract

import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.github.mikephil.charting.data.LineData
import moxy.MvpView
import moxy.viewstate.strategy.AddToEndSingleStrategy
import moxy.viewstate.strategy.StateStrategyType
import network.minter.bipwallet.delegation.adapter.DelegatedStake
import network.minter.bipwallet.delegation.adapter.DelegatedValidator
import network.minter.bipwallet.internal.adapter.LoadState
import network.minter.bipwallet.internal.mvp.ProgressView

/**
 * minter-android-wallet. 2019
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
@StateStrategyType(AddToEndSingleStrategy::class)
interface DelegatedListView : MvpView, ProgressView {
    fun setAdapter(adapter: RecyclerView.Adapter<*>)
    fun setOnRefreshListener(listener: OnRefreshListener)
    fun showRefreshProgress()
    fun hideRefreshProgress()
    fun showChartProgress()
    fun hideChartProgress()
    fun scrollTo(pos: Int)
    fun syncProgress(loadState: MutableLiveData<LoadState>)
    fun setChartData(minMaxValue: FloatArray, dataSets: LineData)
    fun invalidateChartData(dataSets: LineData)
    fun setBipsPerMinute(amount: String)
    fun startDelegate(delegated: DelegatedValidator)
    fun startUnbond(delegated: DelegatedStake)
    fun hideChart()
    fun showChart()
    fun showEmpty()
    fun hideEmpty()
}