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
import android.os.Bundle
import androidx.annotation.StringRes
import network.minter.bipwallet.R
import network.minter.bipwallet.databinding.DialogProgressBinding
import network.minter.bipwallet.internal.common.DeferredCall

/**
 * minter-android-wallet. 2018
 *
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
class WalletProgressDialog protected constructor(context: Context, private val mBuilder: Builder) : WalletDialog(context) {

    private lateinit var b: DialogProgressBinding
    private val mDefer = DeferredCall.createWithSize<WalletProgressDialog>(1)

    fun setText(@StringRes resId: Int) {
        mDefer.call { ctx: WalletProgressDialog -> runOnUiThread { ctx.b.dialogText.setText(resId) } }
    }

    fun setText(text: CharSequence, vararg args: Any?) {
        setText(String.format(text.toString(), *args))
    }

    fun setText(text: CharSequence?) {
        mDefer.call { ctx: WalletProgressDialog -> runOnUiThread { ctx.b.dialogText.text = text } }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = DialogProgressBinding.inflate(layoutInflater)
        setContentView(b.root)
        mDefer.attach(this)
        b.title.text = mBuilder.title
        if (mBuilder.mText == null) {
            b.dialogText.setText(R.string.please_wait)
        } else {
            b.dialogText.text = mBuilder.mText
        }
    }

    override fun onStop() {
        super.onStop()
        mDefer.detach()
    }

    class Builder : WalletDialogBuilder<WalletProgressDialog, Builder> {
        var mText: CharSequence? = null

        constructor(context: Context, @StringRes title: Int) : super(context, title)
        constructor(context: Context, title: CharSequence?) : super(context, title)

        fun setText(text: CharSequence?): Builder {
            mText = text
            return this
        }

        fun setText(@StringRes textRes: Int): Builder {
            return setText(context.resources.getString(textRes))
        }

        override fun create(): WalletProgressDialog {
            return WalletProgressDialog(context, this)
        }
    }
}