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

package network.minter.bipwallet.pools.contracts

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.paging.Pager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import moxy.MvpView
import moxy.viewstate.strategy.AddToEndSingleStrategy
import moxy.viewstate.strategy.OneExecutionStateStrategy
import moxy.viewstate.strategy.StateStrategyType
import network.minter.bipwallet.internal.adapter.LoadState
import network.minter.bipwallet.internal.mvp.ProgressView
import network.minter.bipwallet.pools.adapters.PoolListAdapter
import network.minter.bipwallet.pools.models.PoolCombined
import network.minter.bipwallet.pools.models.PoolsFilter
import network.minter.bipwallet.wallets.contract.WalletSelectorControllerView

/**
 * minter-android-wallet. 2021
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
@StateStrategyType(AddToEndSingleStrategy::class)
interface PoolsTabView: MvpView, WalletSelectorControllerView, ProgressView {

    fun setAdapter(adapter: RecyclerView.Adapter<*>)
    fun setOnRefreshListener(listener: SwipeRefreshLayout.OnRefreshListener)
    fun scrollTo(pos: Int)
    fun setFilterObserver(filterState: MutableLiveData<PoolsFilter>)
    fun observeFilterType(state: MutableLiveData<PoolsFilter>, cb: (PoolsFilter) -> Unit)
    fun observeFilterCoin(filterCoin: MutableLiveData<String?>, function: (String?) -> Unit)
    fun onLifecycle(onLifecycle: (LifecycleOwner) -> Unit)
    fun syncProgress(loadState: MutableLiveData<LoadState>)
    fun submitAdapter(adapter: PoolListAdapter, pager: Pager<Int, PoolCombined>)

    @StateStrategyType(OneExecutionStateStrategy::class)
    fun startAddLiquidity(pool: PoolCombined, requestCode: Int)

    @StateStrategyType(OneExecutionStateStrategy::class)
    fun startRemoveLiquidity(pool: PoolCombined, requestCode: Int)
}