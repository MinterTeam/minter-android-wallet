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

package network.minter.bipwallet.pools.views

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.rxjava2.flowable
import io.reactivex.BackpressureStrategy
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import moxy.InjectViewState
import moxy.presenterScope
import network.minter.bipwallet.apis.explorer.RepoCachedBipUsdRate
import network.minter.bipwallet.apis.gate.TxInitDataRepository
import network.minter.bipwallet.home.HomeScope
import network.minter.bipwallet.internal.adapter.LoadState
import network.minter.bipwallet.internal.exceptions.ErrorManager
import network.minter.bipwallet.internal.helpers.data.Optional
import network.minter.bipwallet.internal.helpers.data.asOptional
import network.minter.bipwallet.internal.mvp.MvpBasePresenter
import network.minter.bipwallet.pools.adapters.PoolListAdapter
import network.minter.bipwallet.pools.adapters.PoolsPagingSource
import network.minter.bipwallet.pools.contracts.PoolsTabView
import network.minter.bipwallet.pools.models.PoolCombined
import network.minter.bipwallet.pools.models.PoolsFilter
import network.minter.bipwallet.wallets.views.WalletSelectorController
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * minter-android-wallet. 2021
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
@HomeScope
@InjectViewState
class PoolsTabPresenter @Inject constructor() : MvpBasePresenter<PoolsTabView>(), ErrorManager.ErrorGlobalReceiverListener {

    companion object {
        const val START_ADD_REMOVE_LIQUIDITY_REQUEST = 3000
    }

    @Inject lateinit var bipUsdRate: RepoCachedBipUsdRate
    @Inject lateinit var initDataRepo: TxInitDataRepository
    @Inject lateinit var errorManager: ErrorManager
    @Inject lateinit var sourceFactory: PoolsPagingSource.Factory
    @Inject lateinit var walletSelectorController: WalletSelectorController

    private var listDisposable: Disposable? = null
    private var pager: Pager<Int, PoolCombined>? = null
    private var source: PagingSource<Int, PoolCombined>? = null
    private var lastPosition: Int = 0
    private var loadState: MutableLiveData<LoadState>? = null
    private val filterType: MutableLiveData<PoolsFilter> = MutableLiveData()
    private val filterCoin: MutableLiveData<String?> = MutableLiveData()

    private val inputChangeSubject: BehaviorSubject<Optional<String>> by lazy { BehaviorSubject.create<Optional<String>>() }

    private var adapter: PoolListAdapter? = null
    private var needsScrollTop = false
    private var needsRefresh = false


    override fun onError(t: Throwable) {
        Timber.e(t)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == START_ADD_REMOVE_LIQUIDITY_REQUEST && resultCode == Activity.RESULT_OK) {
            viewState.showProgress()
            needsRefresh = true
            adapter?.refresh()
        }
    }

    @ExperimentalCoroutinesApi
    override fun onFirstViewAttach() {
        super.onFirstViewAttach()
        walletSelectorController.onFirstViewAttach(viewState)
        bipUsdRate.update()


        inputChangeSubject
                .toFlowable(BackpressureStrategy.LATEST)
                .debounce(200, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { filter: Optional<String> ->
                            Timber.d("Changed coin filter to: %s", filter.value)
                            sourceFactory.filterCoin = filter.value
                            if(filter.value == null || filter.value.isEmpty() || filter.value.length >= 3) {
                                onRefresh()
                            }

                        },
                        { t ->
                            Timber.e(t, "Unable to handle amount change")
                        }
                )
                .disposeOnDestroy()

        viewState.observeFilterType(filterType) {
            Timber.d("Changed filter to: %s", it.name)
            sourceFactory.filterType = it
            onRefresh()
        }
        viewState.observeFilterCoin(filterCoin) {
            inputChangeSubject.onNext(it.asOptional())
        }

        adapter = PoolListAdapter()
        adapter!!.setOnAddLiquidityListener {
            viewState.startAddLiquidity(it, START_ADD_REMOVE_LIQUIDITY_REQUEST)
        }
        adapter!!.setOnRemoveLiquidityListener {
            viewState.startRemoveLiquidity(it, START_ADD_REMOVE_LIQUIDITY_REQUEST)
        }


        loadState = MutableLiveData()
        viewState.syncProgress(loadState!!)
        sourceFactory.observeLoadState(loadState!!)

        pager = Pager(
                config = PagingConfig(
                        pageSize = 50,
                        enablePlaceholders = false
                ),
                initialKey = 1,
                pagingSourceFactory = {
                    Timber.d("POOLS_LIST: factory create")
                    source = sourceFactory.create(needsRefresh)
                    needsRefresh = false
                    source!!
                }
        )



        presenterScope.launch {
            adapter?.loadStateFlow?.distinctUntilChangedBy { it.refresh }
                    ?.filter { it.refresh is androidx.paging.LoadState.NotLoading || it.refresh is androidx.paging.LoadState.Loading }
                    ?.collect {
                        if(it.refresh is androidx.paging.LoadState.Loading) {
                            viewState.showProgress()
                        } else {
                            viewState.hideProgress()
                            if (needsScrollTop) {
                                Timber.d("Adapter load state: ScrollTOP")
                                viewState.scrollTo(0)
                                needsScrollTop = false
                            }
                        }

                    }
        }

        listDisposable = pager!!.flowable
                .subscribe { pd ->
                    viewState.onLifecycle {
                        Timber.d("POOLS_LIST: submit data")
                        adapter!!.submitData(it.lifecycle, pd)
                        if (needsScrollTop) {
                            viewState.scrollTo(0)
                            needsScrollTop = false
                        }
                    }
                }
        unsubscribeOnDestroy(listDisposable)
    }


    override fun attachView(view: PoolsTabView) {
        walletSelectorController.attachView(view)
        walletSelectorController.onWalletSelected = {
//            viewState.showBalanceProgress(true)
//            viewState.clearAmount()
            onRefresh()
        }
        super.attachView(view)

        viewState.setAdapter(adapter!!)
        viewState.setOnRefreshListener {
            needsRefresh = true
            onRefresh()
        }
//        viewState.scrollTo(lastPosition)
        viewState.setFilterObserver(filterType)

    }


    private fun onRefresh() {
        adapter?.refresh()
        needsScrollTop = true
        viewState.scrollTo(0)
    }

    override fun detachView(view: PoolsTabView) {
        super.detachView(view)
        walletSelectorController.detachView()
    }
}