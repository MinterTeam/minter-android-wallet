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
package network.minter.bipwallet.wallets.contract

import android.content.Context
import android.view.View
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import moxy.MvpView
import moxy.viewstate.strategy.AddToEndSingleStrategy
import moxy.viewstate.strategy.OneExecutionStateStrategy
import moxy.viewstate.strategy.StateStrategyType
import network.minter.bipwallet.internal.dialogs.ActionListener
import network.minter.bipwallet.internal.dialogs.WalletDialog
import network.minter.bipwallet.wallets.selector.WalletItem
import network.minter.bipwallet.wallets.selector.WalletListAdapter.*

/**
 * minter-android-wallet. 2019
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
@StateStrategyType(AddToEndSingleStrategy::class)
interface WalletsTabView : MvpView {
    fun showSendAndSetAddress(address: String)
    fun showRefreshProgress()
    fun hideRefreshProgress()
    fun setBalance(firstPart: CharSequence?, middlePart: CharSequence?, lastPart: CharSequence?)
    fun setOnRefreshListener(listener: OnRefreshListener)
    fun setDelegationAmount(amount: String)
    fun setBalanceClickListener(listener: View.OnClickListener)
    fun setBalanceTitle(title: Int)
    fun setBalanceRewards(rewards: String)
    fun setOnClickScanQR(listener: View.OnClickListener)
    fun setMainWallet(mainWallet: WalletItem)
    fun setWallets(addresses: List<WalletItem>)
    fun setOnClickWalletListener(listener: OnClickWalletListener)
    fun setOnClickAddWalletListener(listener: OnClickAddWalletListener)
    fun setOnClickEditWalletListener(listener: OnClickEditWalletListener)
    fun setOnClickDelegated(listener: View.OnClickListener)

    @StateStrategyType(OneExecutionStateStrategy::class)
    fun startExplorer(hash: String)

    @StateStrategyType(OneExecutionStateStrategy::class)
    fun startDialog(executor: (Context) -> WalletDialog)

    @StateStrategyType(OneExecutionStateStrategy::class)
    fun startExternalTransaction(rawData: String)

    @StateStrategyType(OneExecutionStateStrategy::class)
    fun startScanQRWithPermissions(requestCode: Int)

    @StateStrategyType(OneExecutionStateStrategy::class)
    fun startTransactionList()

    @StateStrategyType(OneExecutionStateStrategy::class)
    fun startDelegationList()

    @StateStrategyType(OneExecutionStateStrategy::class)
    fun startConvertCoins()
    fun startTab(tab: Int)

    @StateStrategyType(OneExecutionStateStrategy::class)
    fun startScanQR(requestCode: Int)

    @StateStrategyType(OneExecutionStateStrategy::class)
    fun startWalletEdit(walletItem: WalletItem, onSubmitListener: ActionListener)

    @StateStrategyType(OneExecutionStateStrategy::class)
    fun startWalletAdd(onSubmit: ActionListener, onDismiss: ActionListener?)
}