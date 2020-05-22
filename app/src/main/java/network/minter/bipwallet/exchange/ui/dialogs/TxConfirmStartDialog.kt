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
package network.minter.bipwallet.exchange.ui.dialogs

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import androidx.annotation.StringRes
import network.minter.bipwallet.databinding.DialogTxSendStartBinding
import network.minter.bipwallet.internal.dialogs.WalletDialog
import network.minter.bipwallet.internal.dialogs.WalletDialogBuilder
import network.minter.bipwallet.internal.helpers.MathHelper.humanize
import java.math.BigDecimal

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
class TxConfirmStartDialog
private constructor(context: Context, private val builder: Builder) : WalletDialog(context) {

    private lateinit var binding: DialogTxSendStartBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogTxSendStartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.title.text = builder.title

        binding.apply {
            dialogFirstLabel.text = builder.firstLabel
            dialogFirstValue.text = String.format("%s %s", builder.firstValue, builder.firstCoin)
            dialogSecondLabel.text = builder.secondLabel

            if (builder.secondValueText != null) {
                dialogSecondValue.text = builder.secondValueText
            } else {
                dialogSecondValue.text = String.format("%s %s", builder.secondValue!!, builder.secondCoin)
            }

            builder.bindAction(this@TxConfirmStartDialog, actionConfirm, DialogInterface.BUTTON_POSITIVE)
            builder.bindAction(this@TxConfirmStartDialog, actionDecline, DialogInterface.BUTTON_NEGATIVE)
        }

    }

    class Builder : WalletDialogBuilder<TxConfirmStartDialog, Builder> {
        var firstLabel: CharSequence? = null
        var firstValue: String? = null
        var firstCoin: String? = null
        var secondLabel: CharSequence? = null
        var secondValue: String? = null
        var secondValueText: CharSequence? = null
        var secondCoin: String? = null

        constructor(context: Context, @StringRes title: Int) : super(context, title)
        constructor(context: Context, title: CharSequence?) : super(context, title)

        fun setFirstLabel(label: CharSequence): Builder {
            firstLabel = label
            return this
        }

        fun setFirstLabel(@StringRes resId: Int): Builder {
            return setFirstLabel(mContext.get()!!.resources.getString(resId))
        }

        fun setSecondLabel(label: CharSequence): Builder {
            secondLabel = label
            return this
        }

        fun setSecondLabel(@StringRes resId: Int): Builder {
            return setSecondLabel(mContext.get()!!.resources.getString(resId))
        }

        fun setFirstCoin(coin: String): Builder {
            firstCoin = coin
            return this
        }

        fun setSecondCoin(coin: String): Builder {
            secondCoin = coin
            return this
        }

        fun setFirstValue(decimal: String): Builder {
            firstValue = decimal
            return this
        }

        fun setFirstValue(amount: BigDecimal): Builder {
            firstValue = amount.humanize()
            return this
        }

        fun setSecondValue(value: BigDecimal): Builder {
            secondValue = value.humanize()
            return this
        }

        fun setSecondValue(decimal: String): Builder {
            secondValue = decimal
            return this
        }

        fun setSecondValueText(text: CharSequence): Builder {
            secondValueText = text
            return this
        }

        override fun create(): TxConfirmStartDialog {
            return TxConfirmStartDialog(context, this)
        }
    }

}