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
package network.minter.bipwallet.sending.ui.dialogs

import android.animation.FloatEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.animation.DecelerateInterpolator
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import network.minter.bipwallet.R
import network.minter.bipwallet.databinding.DialogTxSendCompleteBinding
import network.minter.bipwallet.internal.dialogs.WalletDialog
import network.minter.bipwallet.internal.dialogs.WalletDialogBuilder
import network.minter.bipwallet.internal.helpers.MathHelper
import network.minter.bipwallet.internal.helpers.ViewExtensions.visible

/**
 * minter-android-wallet. 2018
 *
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
class TxSendSuccessDialog
private constructor(context: Context, private val builder: Builder) : WalletDialog(context) {
    private lateinit var binding: DialogTxSendCompleteBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogTxSendCompleteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.apply {
            txDescription.text = builder.mLabel
            txDescription.visible = !builder.mLabel.isNullOrEmpty()

            dialogSecondValue.visible = builder.mValue != null
            dialogSecondValue.text = builder.mValue

            builder.bindAction(this@TxSendSuccessDialog, actionViewTx, DialogInterface.BUTTON_POSITIVE)
            builder.bindAction(this@TxSendSuccessDialog, actionNeutral, DialogInterface.BUTTON_NEUTRAL)
            builder.bindAction(this@TxSendSuccessDialog, actionClose, DialogInterface.BUTTON_NEGATIVE)
        }

        runOkAnimation()
    }

    private fun runOkAnimation() {
        val anim = ValueAnimator.ofFloat(0.0f, 1f)
        val sourceScale = 4.0f
        val targetScale = 1.0f
        val sourceTY = binding.curveImage.translationY
        val targetTY = 0.0f
        val sourceIconColor: Int = ContextCompat.getColor(context, R.color.green)
        val targetIconColor: Int = 0xFF_FFFFFF.toInt()

        anim.interpolator = DecelerateInterpolator()
        anim.duration = 500
        anim.addUpdateListener {
            val fract = it.animatedValue as Float
            binding.iconOk.setColorFilter(MathHelper.blendColors(sourceIconColor, targetIconColor, fract))
            binding.curveImage.translationY = FloatEvaluator().evaluate(fract, sourceTY, targetTY)
            binding.curveImage.scaleX = FloatEvaluator().evaluate(fract, sourceScale, targetScale)
        }
        anim.startDelay = 400
        anim.start()
    }

    class Builder @JvmOverloads constructor(context: Context, title: CharSequence? = null) :
            WalletDialogBuilder<TxSendSuccessDialog, Builder>(context, title) {
        var mLabel: CharSequence? = null
        var mValue: CharSequence? = null

        fun setLabel(@StringRes resId: Int): Builder {
            return setLabel(resources.getString(resId))
        }

        fun setLabel(label: CharSequence): Builder {
            mLabel = label
            return this
        }

        fun setValue(value: CharSequence?): Builder {
            mValue = value
            return this
        }

        fun setValue(@StringRes resId: Int): Builder {
            mValue = resources.getString(resId)
            return this
        }

        override fun create(): TxSendSuccessDialog {
            return TxSendSuccessDialog(context, this)
        }
    }

}