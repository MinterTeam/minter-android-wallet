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

import android.content.Context
import android.content.DialogInterface
import android.content.res.Resources
import android.widget.TextView
import androidx.annotation.StringRes
import network.minter.bipwallet.internal.helpers.ViewExtensions.visible
import java.lang.ref.WeakReference

/**
 * minter-android-wallet. 2018
 *
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
typealias DialogActionListener = (dialog: DialogInterface, which: Int) -> Unit

@Suppress("UNCHECKED_CAST")
abstract class WalletDialogBuilder<D : WalletDialog?, B : WalletDialogBuilder<D, B>>(context: Context) {
    protected val mContext: WeakReference<Context> = WeakReference(context)

    @JvmField
    var title: CharSequence? = null

    protected val actionData = HashMap<Int, Pair<CharSequence?, DialogActionListener?>>()

    fun <T : TextView> bindAction(dialog: DialogInterface, view: T, whichBtn: Int) {
        if (!actionData.containsKey(whichBtn) || actionData[whichBtn] == null) {
            view.visible = false
            return
        }
        val data = actionData[whichBtn]!!
        if (data.first == null || data.first!!.isEmpty()) {
            view.visible = false
            return
        }

        view.text = data.first
        view.setOnClickListener {
            if (data.second != null) {
                data.second!!.invoke(dialog, whichBtn)
            } else {
                dialog.dismiss()
            }
        }
    }

    protected fun hasActionTitle(whichBtn: Int): Boolean {
        return actionData.containsKey(whichBtn) && actionData[whichBtn]!!.first != null
    }

    protected val context: Context
        get() = mContext.get()!!

    protected val resources: Resources
        get() = context.resources

    constructor(context: Context, @StringRes title: Int) : this(context) {
        setTitle(title)
    }

    constructor(context: Context, title: CharSequence?) : this(context) {
        setTitle(title)
    }

    fun setTitle(@StringRes title: Int): WalletDialogBuilder<*, *> {
        return setTitle(mContext.get()!!.resources.getString(title))
    }

    fun setTitle(title: CharSequence?): WalletDialogBuilder<*, *> {
        this.title = title
        return this
    }

    abstract fun create(): D

    @JvmOverloads
    fun setPositiveAction(title: CharSequence?, listener: DialogActionListener? = null): B {
        return setAction(DialogInterface.BUTTON_POSITIVE, title, listener)
    }

    @JvmOverloads
    fun setPositiveAction(@StringRes resId: Int, listener: DialogActionListener? = null): B {
        return setAction(DialogInterface.BUTTON_POSITIVE, resId, listener)
    }

    @JvmOverloads
    fun setNegativeAction(title: CharSequence?, listener: DialogActionListener? = null): B {
        return setAction(DialogInterface.BUTTON_NEGATIVE, title, listener)
    }

    @JvmOverloads
    fun setNegativeAction(@StringRes resId: Int, listener: DialogActionListener? = null): B {
        return setAction(DialogInterface.BUTTON_NEGATIVE, resId, listener)
    }

    @JvmOverloads
    fun setNeutralAction(title: CharSequence?, listener: DialogActionListener? = null): B {
        return setAction(DialogInterface.BUTTON_NEUTRAL, title, listener)
    }

    @JvmOverloads
    fun setNeutralAction(@StringRes resId: Int, listener: DialogActionListener? = null): B {
        return setAction(DialogInterface.BUTTON_NEUTRAL, resId, listener)
    }

    protected fun setAction(whichBtn: Int, @StringRes titleRes: Int, listener: DialogActionListener? = null): B {
        return setAction(whichBtn, context.resources.getString(titleRes), listener)
    }

    protected fun setAction(whichBtn: Int, title: CharSequence?, listener: DialogActionListener? = null): B {
        actionData[whichBtn] = Pair(title, listener)
        return this as B
    }

}