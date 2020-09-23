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

import moxy.InjectViewState
import network.minter.bipwallet.R
import network.minter.bipwallet.addressbook.db.AddressBookRepository
import network.minter.bipwallet.apis.explorer.RepoTransactions
import network.minter.bipwallet.apis.explorer.RepoValidators
import network.minter.bipwallet.internal.exceptions.ErrorManager
import network.minter.bipwallet.internal.exceptions.humanMessage
import network.minter.bipwallet.internal.mvp.MvpBasePresenter
import network.minter.bipwallet.internal.storage.SecretStorage
import network.minter.bipwallet.internal.views.list.multirow.MultiRowAdapter
import network.minter.bipwallet.tx.adapters.TransactionDataSource
import network.minter.bipwallet.tx.adapters.TransactionFacade
import network.minter.bipwallet.tx.adapters.TransactionShortListAdapter
import network.minter.bipwallet.wallets.contract.BaseWalletsPageView
import network.minter.bipwallet.wallets.contract.TxsTabPageView
import network.minter.bipwallet.wallets.utils.HistoryTransactionDiffUtil
import network.minter.bipwallet.wallets.views.rows.RowWalletsButton
import network.minter.bipwallet.wallets.views.rows.RowWalletsHeader
import network.minter.bipwallet.wallets.views.rows.RowWalletsList
import timber.log.Timber
import javax.inject.Inject

@InjectViewState
class TxsTabPagePresenter @Inject constructor() : MvpBasePresenter<TxsTabPageView>(), ErrorManager.ErrorGlobalReceiverListener {
    @Inject lateinit var txRepo: RepoTransactions
    @Inject lateinit var secretRepo: SecretStorage
    @Inject lateinit var validatorsRepo: RepoValidators
    @Inject lateinit var addressBookRepo: AddressBookRepository
    @Inject lateinit var errorManager: ErrorManager

    private var txsAdapter: TransactionShortListAdapter? = null
    private var globalAdapter = MultiRowAdapter()
    private var rowHeader: RowWalletsHeader? = null
    private var rowList: RowWalletsList? = null
    private var rowButton: RowWalletsButton? = null

    override fun attachView(view: TxsTabPageView) {
        super.attachView(view)
        viewState.setAdapter(globalAdapter)

        errorManager.subscribe(this)
        txRepo.update()
    }

    override fun onError(t: Throwable) {
        viewState.setViewStatus(BaseWalletsPageView.ViewStatus.Error, t.humanMessage)
    }

    override fun detachView(view: TxsTabPageView) {
        super.detachView(view)
        errorManager.unsubscribe(this)
    }

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()
        viewState.setEmptyTitle(R.string.empty_transactions)

        txsAdapter = TransactionShortListAdapter {
            secretRepo.mainWallet
        }
        txsAdapter!!.setOnExpandDetailsListener { _, tx ->
            viewState.startDetails(tx)
        }

        rowHeader = RowWalletsHeader(R.string.title_latest_transactions)
        rowList = RowWalletsList(txsAdapter!!)
        rowButton = RowWalletsButton(R.string.btn_all_txs) {
            onClickOpenTransactions()
        }
        globalAdapter.addRow(rowHeader!!)
        globalAdapter.addRow(rowList!!)
        globalAdapter.addRow(rowButton!!)


        txRepo
                .observe()
                .joinToUi()
                .switchMap { TransactionDataSource.mapToFacade(it) }
                .switchMap { TransactionDataSource.mapValidatorsInfo(validatorsRepo, it) }
                .switchMap { TransactionDataSource.mapAddressBook(addressBookRepo, it) }
                .switchMap { TransactionDataSource.mapAvatar(it) }
                .subscribe(
                        { res: List<TransactionFacade> ->
                            Timber.d("Updated txs")
                            txsAdapter!!.dispatchChanges(HistoryTransactionDiffUtil::class.java, res, true)
                            if (txsAdapter!!.itemCount == 0) {
                                viewState.setViewStatus(BaseWalletsPageView.ViewStatus.Empty)
                            } else {
                                viewState.setViewStatus(BaseWalletsPageView.ViewStatus.Normal)
                            }
                        },
                        {
                            Timber.w(it, "Unable to load transactions")
                            viewState.setViewStatus(BaseWalletsPageView.ViewStatus.Error, it.message)
                        }
                )
                .disposeOnDestroy()
    }

    private fun onClickOpenTransactions() {
        viewState.startTransactions()
    }
}