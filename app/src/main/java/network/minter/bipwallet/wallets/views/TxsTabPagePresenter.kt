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
package network.minter.bipwallet.wallets.views

import android.view.View
import moxy.InjectViewState
import network.minter.bipwallet.R
import network.minter.bipwallet.apis.explorer.RepoTransactions
import network.minter.bipwallet.apis.explorer.RepoValidators
import network.minter.bipwallet.internal.mvp.MvpBasePresenter
import network.minter.bipwallet.internal.storage.SecretStorage
import network.minter.bipwallet.tx.adapters.TransactionDataSource
import network.minter.bipwallet.tx.adapters.TransactionFacade
import network.minter.bipwallet.tx.adapters.TransactionShortListAdapter
import network.minter.bipwallet.wallets.contract.BaseWalletsPageView
import network.minter.bipwallet.wallets.contract.TxsTabPageView
import network.minter.bipwallet.wallets.utils.HistoryTransactionDiffUtil
import network.minter.explorer.models.HistoryTransaction
import javax.inject.Inject

@InjectViewState
class TxsTabPagePresenter @Inject constructor() : MvpBasePresenter<TxsTabPageView>() {
    @Inject lateinit var txRepo: RepoTransactions
    @Inject lateinit var secretRepo: SecretStorage
    @Inject lateinit var validatorsRepo: RepoValidators

    private var adapter: TransactionShortListAdapter? = null

    override fun attachView(view: TxsTabPageView) {
        super.attachView(view)
        txRepo.update()
    }

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()
        adapter = TransactionShortListAdapter(secretRepo.addresses)
        adapter!!.setOnExpandDetailsListener { _, tx ->
            viewState.startDetails(tx)
        }
        viewState.setAdapter(adapter!!)
        viewState.setListTitle("Latest Transactions")
        viewState.setActionTitle(R.string.btn_all_txs)
        viewState.setOnActionClickListener(View.OnClickListener {
            onClickOpenTransactions()
        })

        txRepo
                .retryWhen(errorResolver)
                .observe()
                .joinToUi()
                .switchMap { result: List<HistoryTransaction> -> TransactionDataSource.mapToFacade(result) }
                .switchMap { items: List<TransactionFacade> -> TransactionDataSource.mapValidatorsInfo(validatorsRepo, items) }
                .subscribe(
                        { res: List<TransactionFacade> ->
                            adapter!!.dispatchChanges(HistoryTransactionDiffUtil::class.java, res, true)
                            if (adapter!!.itemCount == 0) {
                                viewState.setViewStatus(BaseWalletsPageView.ViewStatus.Empty)
                            } else {
                                viewState.setViewStatus(BaseWalletsPageView.ViewStatus.Normal)
                            }
                        },
                        {
                            viewState.setViewStatus(BaseWalletsPageView.ViewStatus.Error)
                        }
                )
                .disposeOnDestroy()
    }

    private fun onClickOpenTransactions() {
        viewState.startTransactions()
    }
}