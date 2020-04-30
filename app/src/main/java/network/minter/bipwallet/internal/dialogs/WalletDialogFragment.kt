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

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import moxy.MvpAppCompatDialogFragment
import network.minter.bipwallet.R

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */

typealias DialogFragmentExecutor = (ctx: Context) -> WalletDialogFragment

abstract class WalletDialogFragment : MvpAppCompatDialogFragment() {
    companion object {
        @JvmStatic
        fun dismissInstance(inputDialog: WalletDialogFragment?) {
            if (inputDialog == null) return
            inputDialog.dismiss()
        }

        @JvmStatic
        fun <T : WalletDialogFragment> switchDialogWithExecutor(fragment: Fragment, dialog: T?, executor: DialogFragmentExecutor): T {
            return switchDialogWithExecutor(fragment.activity!! as AppCompatActivity, dialog, executor)
        }

        @JvmStatic
        fun <T : WalletDialogFragment?> releaseDialog(dialog: T?) {
            if (dialog != null && dialog.isAdded) {
                dialog.dismiss()
            }
        }

        @Suppress("UNCHECKED_CAST")
        @JvmStatic
        fun <T : WalletDialogFragment> switchDialogWithExecutor(activity: AppCompatActivity, dialog: T?, executor: DialogFragmentExecutor): T {
            releaseDialog(dialog)
            val newDialog = executor(activity) as T

            newDialog.show(activity.supportFragmentManager, newDialog.javaClass.simpleName)
            return newDialog
        }
    }

    val window: Window?
        get() = activity?.window

    private var dismissListeners: MutableList<DialogInterface.OnDismissListener> = ArrayList(1)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val d = AppCompatDialog(context, R.style.Wallet_Dialog)
        d.requestWindowFeature(Window.FEATURE_NO_TITLE)
        if (d.window != null) {
            val params = d.window!!.attributes
            params.width = ViewGroup.LayoutParams.MATCH_PARENT
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT
            d.window!!.attributes = params
            d.window!!.setGravity(Gravity.CENTER_VERTICAL or Gravity.CENTER_HORIZONTAL)
            d.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            d.window!!.navigationBarColor = ContextCompat.getColor(context!!, R.color.white)
        }

        return d
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        dismissListeners.forEach { it.onDismiss(dialog) }
    }

    fun addOnDismissListener(listener: DialogInterface.OnDismissListener) {
        dismissListeners.add(listener)
    }


}