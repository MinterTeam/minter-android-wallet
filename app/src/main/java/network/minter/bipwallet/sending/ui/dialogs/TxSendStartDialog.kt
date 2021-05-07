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
package network.minter.bipwallet.sending.ui.dialogs

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import androidx.annotation.StringRes
import network.minter.bipwallet.databinding.DialogTxSendStartBinding
import network.minter.bipwallet.internal.common.Preconditions
import network.minter.bipwallet.internal.dialogs.WalletDialog
import network.minter.bipwallet.internal.dialogs.WalletDialogBuilder
import network.minter.bipwallet.internal.helpers.MathHelper.humanize
import network.minter.core.MinterSDK
import java.math.BigDecimal
import java.math.BigDecimal.ZERO
import java.util.*

/**
 * minter-android-wallet. 2018
 *
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
class TxSendStartDialog(context: Context, private val builder: Builder) : WalletDialog(context) {
    private lateinit var binding: DialogTxSendStartBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogTxSendStartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.apply {
            title.text = builder.title
            dialogFirstValue.text = String.format("%s %s", (builder.amount ?: ZERO).humanize(), builder.coin)
            dialogSecondValue.text = builder.recipient

            builder.bindAction(this@TxSendStartDialog, actionConfirm, DialogInterface.BUTTON_POSITIVE)
            builder.bindAction(this@TxSendStartDialog, actionDecline, DialogInterface.BUTTON_NEGATIVE)
        }
    }

    class Builder : WalletDialogBuilder<TxSendStartDialog, Builder> {
        var amount: BigDecimal? = null
        var recipient: CharSequence? = null
        var coin = MinterSDK.DEFAULT_COIN

        constructor(context: Context, @StringRes title: Int) : super(context, title)
        constructor(context: Context, title: CharSequence?) : super(context, title)

        fun setAmount(decimalString: String?): Builder {
            return setAmount(BigDecimal(decimalString).setScale(18, BigDecimal.ROUND_UNNECESSARY))
        }

        fun setAmount(amount: BigDecimal?): Builder {
            this.amount = amount
            return this
        }

        fun setRecipientName(recipientName: CharSequence?): Builder {
            recipient = recipientName
            return this
        }

        override fun create(): TxSendStartDialog {
            Preconditions.checkNotNull(recipient, "Recipient name required")
            Preconditions.checkNotNull(amount, "Amount can't be empty")
            return TxSendStartDialog(context, this)
        }

        fun setCoin(coin: String?): Builder {
            if (coin == null) {
                return this
            }
            this.coin = coin.uppercase(Locale.getDefault())
            return this
        }
    }

}