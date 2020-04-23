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
package network.minter.bipwallet.internal.dialogs

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.fragment.app.Fragment
import moxy.MvpView
import moxy.viewstate.strategy.AddToEndSingleStrategy
import moxy.viewstate.strategy.StateStrategyType
import network.minter.bipwallet.R

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
typealias DialogExecutor = (ctx: Context) -> WalletDialog

abstract class WalletDialog protected constructor(context: Context) :
        Dialog(context, R.style.Wallet_Dialog) {

    private var dismissListeners: MutableList<DialogInterface.OnDismissListener> = ArrayList(1)

    fun runOnUiThread(task: Runnable) {
        Handler(Looper.getMainLooper()).post(task)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        if (window != null) {
            val params = window!!.attributes
            params.width = ViewGroup.LayoutParams.MATCH_PARENT
            params.height = ViewGroup.LayoutParams.MATCH_PARENT
            window!!.attributes = params
            window!!.setGravity(Gravity.CENTER_VERTICAL or Gravity.CENTER_HORIZONTAL)
            window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }

        super.setOnDismissListener {
            dismissListeners.forEach { it.onDismiss(this) }
        }
    }

    override fun onStart() {
        super.onStart()
    }

    @StateStrategyType(AddToEndSingleStrategy::class)
    interface DialogContractView : MvpView {
        fun startDialog(executor: (ctx: Context) -> WalletDialog)
    }

    interface WithPositiveAction<T> {
        fun setPositiveAction(title: CharSequence?, listener: DialogInterface.OnClickListener?): T
    }

    fun addDismissListener(listener: (DialogInterface) -> Unit) {
        addDismissListener(DialogInterface.OnDismissListener { dialog -> listener(dialog) })
    }

    fun addDismissListener(listener: DialogInterface.OnDismissListener) {
        dismissListeners.add(listener)
    }

    override fun setOnDismissListener(listener: DialogInterface.OnDismissListener?) {
        throw IllegalAccessException("Use WalletDialog.addDismissListener instead")
    }

    companion object {
        @JvmStatic
        fun dismissInstance(inputDialog: WalletDialog?) {
            if (inputDialog == null) return
            inputDialog.dismiss()
        }

        @JvmStatic
        fun <T : WalletDialog> switchDialogWithExecutor(fragment: Fragment, dialog: T?, executor: DialogExecutor): T {
            return switchDialogWithExecutor(fragment.activity!!, dialog, executor)
        }

        @JvmStatic
        fun <T : WalletDialog?> releaseDialog(dialog: T?) {
            if (dialog != null && dialog.isShowing) {
                dialog.dismiss()
            }
        }

        @Suppress("UNCHECKED_CAST")
        @JvmStatic
        fun <T : WalletDialog> switchDialogWithExecutor(activity: Activity, dialog: T?, executor: DialogExecutor): T {
            releaseDialog(dialog)
            val newDialog = executor(activity) as T
            newDialog.show()
            return newDialog
        }
    }
}