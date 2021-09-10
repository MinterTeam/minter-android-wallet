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
package network.minter.bipwallet.pools.ui

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import androidx.annotation.StringRes
import network.minter.bipwallet.apis.reactive.avatar
import network.minter.bipwallet.databinding.DialogTxLiquidityStartBinding
import network.minter.bipwallet.internal.dialogs.WalletDialog
import network.minter.bipwallet.internal.dialogs.WalletDialogBuilder
import network.minter.bipwallet.internal.helpers.MathHelper.humanize
import network.minter.bipwallet.internal.helpers.ViewExtensions.tr
import network.minter.explorer.models.CoinItemBase
import java.math.BigDecimal

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
class TxLiquidityStartDialog
private constructor(context: Context, private val builder: Builder) : WalletDialog(context) {
    private lateinit var binding: DialogTxLiquidityStartBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogTxLiquidityStartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.title.text = builder.title

        binding.apply {
            dialogTxType.text = builder.txTitle
            coin0Avatar.setImageUrl(builder.coin0!!.avatar)
            coin1Avatar.setImageUrl(builder.coin1!!.avatar)
            coin0.text = builder.coin0!!.symbol
            coin1.text = builder.coin1!!.symbol
            coin0Amount.text = builder.amount0.humanize()
            coin1Amount.text = builder.amount1.humanize()
            coin0Subamount.text = builder.subAmount0
            coin1Subamount.text = builder.subAmount1

            builder.bindAction(this@TxLiquidityStartDialog, actionConfirm, DialogInterface.BUTTON_POSITIVE)
            builder.bindAction(this@TxLiquidityStartDialog, actionDecline, DialogInterface.BUTTON_NEGATIVE)
        }

    }

    class Builder : WalletDialogBuilder<TxLiquidityStartDialog, Builder> {
        var txTitle: CharSequence? = null
        var coin0: CoinItemBase? = null
        var coin1: CoinItemBase? = null
        var amount0: BigDecimal = BigDecimal.ZERO
        var amount1: BigDecimal = BigDecimal.ZERO
        var subAmount0: CharSequence? = null
        var subAmount1: CharSequence? = null

        constructor(context: Context, @StringRes title: Int) : super(context, title)
        constructor(context: Context, title: CharSequence?) : super(context, title)

        fun setTxTitle(@StringRes resId: Int): Builder {
            txTitle = tr(resId)
            return this
        }

        fun setTxTitle(txTitle: CharSequence): Builder {
            this.txTitle = txTitle
            return this
        }

        fun setCoins(coin0: CoinItemBase, coin1: CoinItemBase): Builder {
            this.coin0 = coin0
            this.coin1 = coin1
            return this
        }

        fun setAmounts(v0: BigDecimal, v1: BigDecimal): Builder {
            amount0 = v0
            amount1 = v1
            return this
        }
        fun setSubAmount0(v0: CharSequence?): Builder {
            subAmount0 = v0
            return this
        }

        fun setSubAmount1(v1: CharSequence?): Builder {
            subAmount1 = v1
            return this
        }

        override fun create(): TxLiquidityStartDialog {
            return TxLiquidityStartDialog(context, this)
        }
    }

}