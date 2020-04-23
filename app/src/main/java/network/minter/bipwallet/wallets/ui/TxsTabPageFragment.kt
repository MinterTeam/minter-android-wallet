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
package network.minter.bipwallet.wallets.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import moxy.presenter.InjectPresenter
import moxy.presenter.ProvidePresenter
import network.minter.bipwallet.internal.dialogs.BaseBottomSheetDialogFragment
import network.minter.bipwallet.tx.adapters.TransactionFacade
import network.minter.bipwallet.tx.ui.TransactionListActivity
import network.minter.bipwallet.tx.ui.TransactionViewDialog
import network.minter.bipwallet.wallets.contract.TxsTabPageView
import network.minter.bipwallet.wallets.views.TxsTabPagePresenter
import javax.inject.Inject
import javax.inject.Provider

class TxsTabPageFragment : BaseTabPageFragment(), TxsTabPageView {
    @JvmField @Inject
    var presenterProvider: Provider<TxsTabPagePresenter>? = null

    @JvmField @InjectPresenter
    var presenter: TxsTabPagePresenter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun startTransactions() {
        TransactionListActivity.Builder(this).start()
    }

    private var bottomDialog: BaseBottomSheetDialogFragment? = null

    override fun startDetails(tx: TransactionFacade) {
        bottomDialog?.dismiss()
        bottomDialog = null
        bottomDialog = TransactionViewDialog.Builder(tx).build()
        bottomDialog!!.show(fragmentManager!!, "tx_view")
    }

    @ProvidePresenter
    fun providePresenter(): TxsTabPagePresenter {
        return presenterProvider!!.get()
    }
}