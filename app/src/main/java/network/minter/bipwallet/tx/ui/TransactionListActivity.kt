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
package network.minter.bipwallet.tx.ui

import android.app.Activity
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import moxy.presenter.InjectPresenter
import moxy.presenter.ProvidePresenter
import network.minter.bipwallet.databinding.ActivityTransactionListBinding
import network.minter.bipwallet.internal.BaseMvpInjectActivity
import network.minter.bipwallet.internal.Wallet
import network.minter.bipwallet.internal.adapter.LoadState
import network.minter.bipwallet.internal.dialogs.BaseBottomSheetDialogFragment
import network.minter.bipwallet.internal.helpers.ViewExtensions.nvisible
import network.minter.bipwallet.internal.helpers.ViewExtensions.visible
import network.minter.bipwallet.internal.helpers.ViewExtensions.visibleForTestnet
import network.minter.bipwallet.internal.system.ActivityBuilder
import network.minter.bipwallet.tx.adapters.TransactionFacade
import network.minter.bipwallet.tx.contract.TransactionListView
import network.minter.bipwallet.tx.views.TransactionListPresenter
import network.minter.explorer.repo.ExplorerTransactionRepository.TxFilter
import javax.inject.Inject
import javax.inject.Provider

/**
 * minter-android-wallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock></edward.vstock>@gmail.com>
 */
class TransactionListActivity : BaseMvpInjectActivity(), TransactionListView {
    @Inject lateinit var presenterProvider: Provider<TransactionListPresenter>
    @InjectPresenter lateinit var presenter: TransactionListPresenter

    private lateinit var b: ActivityTransactionListBinding
    private var bottomDialog: BaseBottomSheetDialogFragment? = null

    @ProvidePresenter
    fun providePresenter(): TransactionListPresenter {
        return presenterProvider.get()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityTransactionListBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.apply {
            setupToolbar(toolbar)
            presenter.handleExtras(intent)
            list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    presenter.onScrolledTo((recyclerView.layoutManager as LinearLayoutManager?)!!.findFirstVisibleItemPosition())
                }
            })

            b.testnetWarning.visibleForTestnet()
        }

    }

    override fun setOnRefreshListener(listener: OnRefreshListener) {
        b.containerSwipeRefresh.setOnRefreshListener(listener)
    }

    override fun showRefreshProgress() {
        if (!b.progress.visible) {
            b.containerSwipeRefresh.isRefreshing = true
        }
    }

    override fun hideRefreshProgress() {
        b.containerSwipeRefresh.isRefreshing = false
    }

    override fun scrollTo(pos: Int) {
        if (pos < 0) {
            return
        }
        b.list.scrollToPosition(pos)
    }

    override fun startExplorer(hash: String?) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Wallet.urlExplorerFront() + "/transactions/" + hash)))
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
                    hideRefreshProgress()
                    hideProgress()
                }
                LoadState.Loading -> showProgress()
                else -> {
                }
            }
        })
    }

    override fun lifecycle(state: MutableLiveData<TxFilter>, cb: (TxFilter) -> Unit) {
        state.observe(this, Observer {
            cb(it)
        })
    }

    override fun onLifecycle(onLifecycle: (LifecycleOwner) -> Unit) {
        onLifecycle(this)
    }

    override fun showProgress() {
        if (!b.containerSwipeRefresh.isRefreshing) {
            b.progress.nvisible = true
        }
    }

    override fun hideProgress() {
        b.progress.nvisible = false
    }

    override fun setAdapter(adapter: RecyclerView.Adapter<*>) {
        b.list.layoutManager = LinearLayoutManager(this)
        b.list.adapter = adapter
    }

    override fun startDetails(tx: TransactionFacade) {
        if (bottomDialog != null) {
            bottomDialog!!.dismiss()
            bottomDialog = null
        }
        bottomDialog = TransactionViewDialog.Builder(tx)
                .build()
        bottomDialog!!.show(supportFragmentManager, "tx_view")
    }

    override fun setFilterObserver(filterState: MutableLiveData<TxFilter>) {
        b.txFilter.setSelectObserver(filterState)
    }

    class Builder : ActivityBuilder {
        constructor(from: Activity) : super(from)
        constructor(from: Fragment) : super(from)
        constructor(from: Service) : super(from)

        override fun getActivityClass(): Class<*> {
            return TransactionListActivity::class.java
        }
    }
}