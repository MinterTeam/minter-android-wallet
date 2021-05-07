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
package network.minter.bipwallet.home

import network.minter.bipwallet.internal.BaseFragment
import network.minter.bipwallet.internal.dialogs.ActionListener
import network.minter.bipwallet.internal.dialogs.BaseBottomSheetDialogFragment
import network.minter.bipwallet.internal.dialogs.DialogExecutor
import network.minter.bipwallet.internal.dialogs.WalletDialog
import network.minter.bipwallet.wallets.dialogs.ui.AddWalletDialog
import network.minter.bipwallet.wallets.dialogs.ui.EditWalletDialog
import network.minter.bipwallet.wallets.selector.WalletItem

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
abstract class HomeTabFragment : BaseFragment() {
    protected var bottomSheetDialog: BaseBottomSheetDialogFragment? = null
    protected var walletDialog: WalletDialog? = null

    val title: CharSequence?
        get() = null

    open fun onTabSelected() {}
    open fun onTabUnselected() {}

    override fun onDestroyView() {
        super.onDestroyView()
        WalletDialog.releaseDialog(walletDialog)
        if (bottomSheetDialog != null) {
            try {
                bottomSheetDialog!!.dismissAllowingStateLoss()
                bottomSheetDialog = null
            } catch (t: Throwable) {
            }
        }
    }

    open fun onTrimMemory(level: Int) {}

    fun startDialog(executor: DialogExecutor) {
        walletDialog = WalletDialog.switchDialogWithExecutor(this, walletDialog, executor)
    }

    fun startWalletEdit(walletItem: WalletItem, enableRemove: Boolean, submitListener: (WalletItem) -> Unit, deleteListener: (WalletItem) -> Unit) {
        if (bottomSheetDialog != null) {
            bottomSheetDialog!!.dismiss()
            bottomSheetDialog = null
        }
        bottomSheetDialog = EditWalletDialog.newInstance(walletItem, enableRemove)
        (bottomSheetDialog!! as EditWalletDialog).onSaveListener = submitListener
        (bottomSheetDialog!! as EditWalletDialog).onDeleteListener = deleteListener
        bottomSheetDialog!!.show(parentFragmentManager, "wallet_edit")
    }

    fun startWalletAdd(onSubmit: (WalletItem) -> Unit, onDismiss: ActionListener?) {
        if (bottomSheetDialog != null) {
            bottomSheetDialog!!.dismiss()
            bottomSheetDialog = null
        }
        val addWalletDialog = AddWalletDialog.newInstance()
        addWalletDialog.onAddListener = onSubmit
        addWalletDialog.onDismissListener = onDismiss
        bottomSheetDialog = addWalletDialog
        bottomSheetDialog!!.show(parentFragmentManager, "wallet_add")
    }
}