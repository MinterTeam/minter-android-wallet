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
package network.minter.bipwallet.tx.views

import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.rxjava2.flowable
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import moxy.InjectViewState
import moxy.presenterScope
import network.minter.bipwallet.analytics.AppEvent
import network.minter.bipwallet.analytics.base.HasAnalyticsEvent
import network.minter.bipwallet.internal.adapter.LoadState
import network.minter.bipwallet.internal.mvp.MvpBasePresenter
import network.minter.bipwallet.internal.storage.SecretStorage
import network.minter.bipwallet.tx.adapters.TransactionDataSource
import network.minter.bipwallet.tx.adapters.TransactionFacade
import network.minter.bipwallet.tx.adapters.TransactionItem
import network.minter.bipwallet.tx.adapters.TransactionListAdapter
import network.minter.bipwallet.tx.contract.TransactionListView
import network.minter.explorer.repo.ExplorerTransactionRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
@InjectViewState
class TransactionListPresenter @Inject constructor() : MvpBasePresenter<TransactionListView>(), HasAnalyticsEvent {
    @Inject lateinit var transactionRepo: ExplorerTransactionRepository
    @Inject lateinit var secretRepo: SecretStorage
    @Inject lateinit var sourceFactory: TransactionDataSource.Factory

    private var adapter: TransactionListAdapter? = null
    private var listDisposable: Disposable? = null
    private var listBuilder: Pager<Int, TransactionItem>? = null
    private var lastPosition = 0
    private var loadState: MutableLiveData<LoadState>? = null
    private val filterState: MutableLiveData<ExplorerTransactionRepository.TxFilter> = MutableLiveData()

    var source: PagingSource<Int, TransactionItem>? = null

    override fun attachView(view: TransactionListView) {
        super.attachView(view)
        viewState.setAdapter(adapter!!)
        viewState.setOnRefreshListener {
            onRefresh()
        }
        viewState.scrollTo(lastPosition)
        viewState.setFilterObserver(filterState)

    }

    fun onScrolledTo(position: Int) {
        lastPosition = position
    }

    override fun getAnalyticsEvent(): AppEvent {
        return AppEvent.TransactionsScreen
    }

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()

        viewState.lifecycle(filterState) {
            Timber.d("TX_LIST: Changed filter to: %s", it.name)
            sourceFactory.txFilter = it
//            Timber.d("TX_LIST: Changed filter invalidate")
//            mAdapter?.refresh()
            onRefresh()
        }

        adapter = TransactionListAdapter { secretRepo.mainWallet }
        adapter?.setOnExpandDetailsListener { v, tx -> onExpandTx(v, tx) }
        loadState = MutableLiveData<LoadState>().also {
            viewState.syncProgress(it)
            adapter?.setLoadState(it)
            sourceFactory.observeLoadState(it)
        }

        listBuilder = Pager(
                config = PagingConfig(
                        pageSize = 50,
                        enablePlaceholders = false
                ),
                initialKey = 1,
                pagingSourceFactory = {
                    Timber.d("TX_LIST: create source instance")
                    sourceFactory.create().also {
                        source = it
                    }
                }
        )
        listBuilder?.flow?.onEach { pd ->
            viewState.onLifecycle { lifecycleOwner ->
                Timber.d("TX_LIST: Submit adapter")
                adapter?.submitData(lifecycleOwner.lifecycle, pd)
            }
        }?.launchIn(presenterScope)
//        listDisposable = listBuilder?.flowable
//                .subscribe { pd ->
//
//
//                }
//        unsubscribeOnDestroy(listDisposable)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onExpandTx(view: View, tx: TransactionFacade) {
        analytics.send(AppEvent.TransactionDetailsButton)
        viewState.startDetails(tx)
    }

    private fun onRefresh() {
//        mListDisposable!!.dispose()
        viewState.scrollTo(0)

//        source?.invalidate()
        adapter?.refresh()
//        refresh()
    }

    @ExperimentalCoroutinesApi
    private fun refresh() {
        listDisposable = listBuilder!!.flowable
                .subscribe { pd ->
                    viewState.onLifecycle {
                        Timber.d("TX_LIST: Submit adapter")
                        adapter!!.submitData(it.lifecycle, pd)
                    }

                }

//                .doOnSubscribe { subscription: Disposable? -> unsubscribeOnDestroy(subscription) }
//                .subscribe { res: PagedList<TransactionItem> ->
//                    viewState.hideRefreshProgress()
//                    mAdapter!!.submitList(res)
//                }
    }

    private fun onExplorerClick(historyTransaction: TransactionFacade) {
        viewState.startExplorer(historyTransaction.hash.toString())
        analytics.send(AppEvent.TransactionExplorerButton)
    }
}