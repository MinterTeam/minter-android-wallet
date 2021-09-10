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

package network.minter.bipwallet.pools.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.paging.Pager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import moxy.presenter.InjectPresenter
import moxy.presenter.ProvidePresenter
import network.minter.bipwallet.R
import network.minter.bipwallet.databinding.FragmentTabPoolsBinding
import network.minter.bipwallet.home.HomeModule
import network.minter.bipwallet.home.HomeTabFragment
import network.minter.bipwallet.internal.adapter.LoadState
import network.minter.bipwallet.internal.helpers.KeyboardHelper
import network.minter.bipwallet.internal.helpers.ViewExtensions.nvisible
import network.minter.bipwallet.internal.helpers.ViewExtensions.visible
import network.minter.bipwallet.internal.helpers.ViewHelper
import network.minter.bipwallet.internal.helpers.forms.validators.CoinFilter
import network.minter.bipwallet.internal.views.list.BorderedItemSeparator
import network.minter.bipwallet.pools.adapters.PoolListAdapter
import network.minter.bipwallet.pools.contracts.PoolsTabView
import network.minter.bipwallet.pools.models.PoolCombined
import network.minter.bipwallet.pools.models.PoolsFilter
import network.minter.bipwallet.pools.views.PoolsTabPresenter
import network.minter.bipwallet.wallets.selector.WalletItem
import network.minter.bipwallet.wallets.selector.WalletListAdapter
import network.minter.bipwallet.wallets.selector.WalletSelectorBroadcastReceiver
import network.minter.bipwallet.wallets.ui.WalletsTopRecolorHelper
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Provider

/**
 * minter-android-wallet. 2021
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */

class PoolsTabFragment : HomeTabFragment(), PoolsTabView {

    @Inject lateinit var presenterProvider: Provider<PoolsTabPresenter>
    @InjectPresenter lateinit var presenter: PoolsTabPresenter


    private lateinit var b: FragmentTabPoolsBinding

    private var itemSeparator: BorderedItemSeparator? = null

    @ProvidePresenter
    fun providerPresenter(): PoolsTabPresenter {
        return presenterProvider.get()
    }

    override fun onTabSelected() {
        super.onTabSelected()
        if (WalletsTopRecolorHelper.enableRecolorSystemUI()) {
            ViewHelper.setSystemBarsLightness(this, true)
            ViewHelper.setStatusBarColorAnimate(this, 0xFF_FFFFFF.toInt())
        }
    }

    override fun onTabUnselected() {
        super.onTabUnselected()

//        b.inputPoolsFilter.dismissDropDown()
        KeyboardHelper.hideKeyboard(this)
    }

    override fun onAttach(context: Context) {
        HomeModule.component!!.inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        HomeModule.component!!.inject(this)
        super.onCreate(savedInstanceState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        presenter.onActivityResult(requestCode, resultCode, data)
    }

    override fun submitAdapter(adapter: PoolListAdapter, pager: Pager<Int, PoolCombined>) {
//        pager.flowable.subscribe {
//            adapter.submitData(lifecycle, it)
//        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        b = FragmentTabPoolsBinding.inflate(inflater, container, false)

//        itemSeparator = BorderedItemSeparator(requireContext(), R.drawable.shape_bottom_separator, false, true)
//        itemSeparator!!.setDividerPadding(0, Wallet.app().display().dpToPx(16f), 0, Wallet.app().display().dpToPx(16f))

        b.apply {
            walletSelector.registerLifecycle(requireActivity())
            inputPoolsFilter.input.filters = arrayOf(CoinFilter())

//            list.addItemDecoration(itemSeparator!!)
        }

        return b.root
    }

    override fun startAddLiquidity(pool: PoolCombined, requestCode: Int) {
        PoolAddLiquidityActivity.Builder(this, pool).start(requestCode)
    }

    override fun startRemoveLiquidity(pool: PoolCombined, requestCode: Int) {
        PoolRemoveLiquidityActivity.Builder(this, pool).start(requestCode)
    }

    override fun setAdapter(adapter: RecyclerView.Adapter<*>) {
        b.list.layoutManager = LinearLayoutManager(requireContext())
        b.list.adapter = adapter
    }

    override fun setOnRefreshListener(listener: SwipeRefreshLayout.OnRefreshListener) {
        b.containerSwipeRefresh.setOnRefreshListener(listener)
    }

    override fun scrollTo(pos: Int) {
        if (pos < 0) {
            return
        }
        b.list.scrollToPosition(pos)
    }

    override fun setFilterObserver(filterState: MutableLiveData<PoolsFilter>) {
        b.poolsFilter.setSelectObserver(filterState)
    }

    override fun observeFilterType(state: MutableLiveData<PoolsFilter>, cb: (PoolsFilter) -> Unit) {
        state.observe(this, cb)
    }

    override fun observeFilterCoin(filterCoin: MutableLiveData<String?>, function: (String?) -> Unit) {
        b.inputPoolsFilter.addTextChangedSimpleListener {
            if(it == null) {
                filterCoin.postValue(null)
                b.inputPoolsFilter.setSuffixImageSrc(R.drawable.ic_search_grey)
                b.inputPoolsFilter.setOnSuffixImageClickListener {  }
            } else {
                b.inputPoolsFilter.setSuffixImageSrc(R.drawable.ic_close_grey)
                b.inputPoolsFilter.setOnSuffixImageClickListener {
                    b.inputPoolsFilter.text = null
                    b.inputPoolsFilter.setSuffixImageSrc(R.drawable.ic_search_grey)
                    b.inputPoolsFilter.setOnSuffixImageClickListener {  }
                }
                filterCoin.postValue(it.toString())
            }
        }
        filterCoin.observe(viewLifecycleOwner, function)
    }

    override fun onLifecycle(onLifecycle: (LifecycleOwner) -> Unit) {
        onLifecycle(this)
    }

    override fun syncProgress(loadState: MutableLiveData<LoadState>) {
        loadState.observe(this, Observer { s: LoadState? ->
            if (s == null) {
                showProgress()
                return@Observer
            }
            when (s) {
                LoadState.Loaded,
                LoadState.Failed
                -> {
                    b.emptyText.visible = false
                    Timber.d("Hide progress")
                    hideRefreshProgress()
                    hideProgress()
                }
                LoadState.Loading -> {
                    b.emptyText.visible = false
                    showProgress()
                }
                LoadState.Empty -> {
                    hideProgress()
                    when(b.poolsFilter.selected) {
                        PoolsFilter.None -> {
                            b.emptyText.setText(R.string.empty_pools_all)
                        }
                        PoolsFilter.Staked -> {
                            b.emptyText.setText(R.string.empty_pools_staked)
                        }
                        PoolsFilter.Farming -> {
                            b.emptyText.setText(R.string.empty_pools_farming)
                        }
                    }
                    b.emptyText.visible = true
                }
            }
        })
    }

    override fun setWallets(walletItems: List<WalletItem>) {
        activity?.let {
            runOnUiThread {
                activity?.let {
                    WalletSelectorBroadcastReceiver.setWallets(it, walletItems)
                }
            }
        }
    }

    override fun setMainWallet(walletItem: WalletItem) {
        activity?.let {
            runOnUiThread {
                activity?.let {
                    WalletSelectorBroadcastReceiver.setMainWallet(it, walletItem)
                }
            }
        }
    }

    fun showRefreshProgress() {
        if (!b.progress.visible) {
            b.containerSwipeRefresh.isRefreshing = true
        }
    }

    fun hideRefreshProgress() {
        b.containerSwipeRefresh.isRefreshing = false
    }

    override fun setOnClickWalletListener(listener: WalletListAdapter.OnClickWalletListener) {
        b.walletSelector.setOnClickWalletListener(listener)
    }

    override fun setOnClickAddWalletListener(listener: WalletListAdapter.OnClickAddWalletListener) {
        b.walletSelector.setOnClickAddWalletListener(listener)
    }

    override fun setOnClickEditWalletListener(listener: WalletListAdapter.OnClickEditWalletListener) {
        b.walletSelector.setOnClickEditWalletListener(listener)
    }

    override fun showProgress() {
        if (!b.containerSwipeRefresh.isRefreshing) {
            b.progress.nvisible = true
        }
    }

    override fun hideProgress() {
        b.progress.nvisible = false
        b.containerSwipeRefresh.isRefreshing = false
    }
}