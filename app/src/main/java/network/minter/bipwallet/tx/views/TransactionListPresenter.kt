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
package network.minter.bipwallet.tx.views

import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.paging.PagedList
import androidx.paging.RxPagedListBuilder
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import io.reactivex.disposables.Disposable
import moxy.InjectViewState
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
import javax.inject.Inject

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
@InjectViewState
class TransactionListPresenter @Inject constructor() : MvpBasePresenter<TransactionListView>(), HasAnalyticsEvent {
    lateinit var transactionRepo: ExplorerTransactionRepository
        @Inject set
    lateinit var secretRepo: SecretStorage
        @Inject set
    lateinit var sourceFactory: TransactionDataSource.Factory
        @Inject set

    private var mAdapter: TransactionListAdapter? = null
    private var mListDisposable: Disposable? = null
    private var listBuilder: RxPagedListBuilder<Long, TransactionItem>? = null
    private var mLastPosition = 0
    private var mLoadState: MutableLiveData<LoadState>? = null
    override fun attachView(view: TransactionListView) {
        super.attachView(view)
        viewState.setAdapter(mAdapter!!)
        viewState.setOnRefreshListener(SwipeRefreshLayout.OnRefreshListener { onRefresh() })
        viewState.scrollTo(mLastPosition)
    }

    fun onScrolledTo(position: Int) {
        mLastPosition = position
    }

    override fun getAnalyticsEvent(): AppEvent {
        return AppEvent.TransactionsScreen
    }

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()
        mAdapter = TransactionListAdapter(secretRepo.addresses)
        mAdapter!!.setOnExpandDetailsListener { view: View, tx: TransactionFacade -> onExpandTx(view, tx) }
        mLoadState = MutableLiveData()
        viewState.syncProgress(mLoadState!!)
        mAdapter!!.setLoadState(mLoadState)
        sourceFactory.observeLoadState(mLoadState!!)
        val cfg = PagedList.Config.Builder()
                .setPageSize(50)
                .setEnablePlaceholders(false)
                .build()
        listBuilder = RxPagedListBuilder(sourceFactory, cfg)
        refresh()
        unsubscribeOnDestroy(mListDisposable)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onExpandTx(view: View, tx: TransactionFacade) {
        analytics.send(AppEvent.TransactionDetailsButton)
        viewState.startDetails(tx)
    }

    private fun onRefresh() {
        mListDisposable!!.dispose()
        viewState.scrollTo(0)
        refresh()
    }

    private fun refresh() {
        mListDisposable = listBuilder!!.buildObservable()
                .doOnSubscribe { subscription: Disposable? -> unsubscribeOnDestroy(subscription) }
                .subscribe { res: PagedList<TransactionItem> ->
                    viewState.hideRefreshProgress()
                    mAdapter!!.submitList(res)
                }
    }

    private fun onExplorerClick(historyTransaction: TransactionFacade) {
        viewState.startExplorer(historyTransaction.hash.toString())
        analytics.send(AppEvent.TransactionExplorerButton)
    }
}