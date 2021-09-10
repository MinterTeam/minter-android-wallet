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

import android.app.Activity
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.fragment.app.Fragment
import com.edwardstock.inputfield.InputField
import com.edwardstock.inputfield.form.DecimalInputFilter
import com.edwardstock.inputfield.form.InputGroup
import com.edwardstock.inputfield.form.InputWrapper
import moxy.ktx.moxyPresenter
import network.minter.bipwallet.databinding.ActivityPoolAddLiquidityBinding
import network.minter.bipwallet.internal.BaseMvpInjectActivity
import network.minter.bipwallet.internal.Wallet
import network.minter.bipwallet.internal.helpers.MathHelper.plain
import network.minter.bipwallet.internal.helpers.ViewExtensions.postApply
import network.minter.bipwallet.internal.helpers.ViewExtensions.textWidth
import network.minter.bipwallet.internal.system.ActivityBuilder
import network.minter.bipwallet.pools.contracts.PoolAddLiquidityView
import network.minter.bipwallet.pools.models.PoolCombined
import network.minter.bipwallet.pools.views.PoolAddLiquidityPresenter
import network.minter.core.crypto.MinterHash
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Provider

/**
 * minter-android-wallet. 2021
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
class PoolAddLiquidityActivity: BaseMvpInjectActivity(), PoolAddLiquidityView {

    companion object {
        const val ARG_POOL = "ARG_POOL"
    }

    @Inject lateinit var presenterProvider: Provider<PoolAddLiquidityPresenter>
    private val presenter by moxyPresenter { presenterProvider.get() }

    private lateinit var b: ActivityPoolAddLiquidityBinding
    private val inputGroup = InputGroup()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityPoolAddLiquidityBinding.inflate(layoutInflater)
        setContentView(b.root)

        setResult(RESULT_CANCELED)

        presenter.handleExtras(intent)

        b.apply {
            setupToolbar(b.toolbar)
            inputGroup.addInput(inputCoin0)
            inputGroup.addInput(inputCoin1)
            inputGroup.addInput(inputSlippage)
            inputGroup.addInput(inputMaxSpend)
            inputGroup.addFilter(inputCoin0, DecimalInputFilter(inputCoin0))
            inputGroup.addFilter(inputCoin1, DecimalInputFilter(inputCoin1))
            inputGroup.addFilter(inputSlippage, DecimalInputFilter(inputSlippage, 2))
            inputGroup.addFilter(inputMaxSpend, DecimalInputFilter(inputMaxSpend))

            inputCoin0.input.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    calcCoin0Padding(coin0Suffix.text.toString())
                }
                override fun afterTextChanged(s: Editable?) {}
            })
            inputCoin1.input.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    calcCoin1Padding(coin1Suffix.text.toString())
                }
                override fun afterTextChanged(s: Editable?) {}
            })
            inputSlippage.input.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    b.slippageSuffix.setPadding(calcSuffixPadding(inputSlippage), 0, 0, 0)

                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
    }

    override fun setSlippageError(text: CharSequence?) {
        inputGroup.setError("slippage", text)
    }

    override fun setMaxAmountError(text: CharSequence?) {
        inputGroup.setError("max_amount", text)
    }

    override fun setEnableSubmit(enable: Boolean) {
        b.action.isEnabled = enable
    }

    private fun calcSuffixPadding(input: InputField): Int {
        return input.textWidth() + Wallet.app().display().dpToPx(4)
    }

    override fun calcCoin0Padding(coin0: String) {
        b.coin0Suffix.setPadding(calcSuffixPadding(b.inputCoin0), 0, 0, 0)
        b.coin0Suffix.text = coin0
    }

    override fun setOnClickUseMax0(listener: (View) -> Unit) {
        b.inputCoin0.setOnSuffixTextClickListener(listener)
    }

    override fun setOnClickUseMax1(listener: (View) -> Unit) {
        b.inputCoin1.setOnSuffixTextClickListener(listener)
    }

    override fun setCoin0SuffixEnable(enable: Boolean) {
        b.inputCoin0.setSuffixType(if(enable) InputField.SuffixType.Text else InputField.SuffixType.None)
    }

    override fun setCoin1SuffixEnable(enable: Boolean) {
        b.inputCoin1.setSuffixType(if(enable) InputField.SuffixType.Text else InputField.SuffixType.None)
    }

    override fun calcCoin1Padding(coin0: String) {
        b.coin1Suffix.setPadding(calcSuffixPadding(b.inputCoin1), 0, 0, 0)
        b.coin1Suffix.text = coin0
    }

    override fun setOnSwapCoins(listener: (View) -> Unit) {
        b.actionSwapCoins.setOnClickListener(listener)
    }

    override fun setOnTextChangedListener(listener: (InputWrapper, Boolean) -> Unit) {
        inputGroup.addTextChangedListener(listener)
    }

    override fun setCoin0(coin0Amount: BigDecimal?, coin0: String) {
        coin0Amount?.let {
            if(it == BigDecimal.ZERO) {
                b.inputCoin0.setText("0")
                b.inputCoin0.input.setSelection(0, b.inputCoin0.text?.length?:0)
            } else {
                b.inputCoin0.setText(it.plain())
            }

        }

        calcCoin0Padding(coin0)
    }

    override fun setOnSubmit(listener: (View) -> Unit) {
        b.action.setOnClickListener(listener)
    }

    override fun setCoin0Error(error: CharSequence?) {
        inputGroup.setError("coin0", error)
    }

    override fun setCoin1Error(error: CharSequence?) {
        inputGroup.setError("coin1", error)
    }

    override fun setFee(feeText: CharSequence) {
        b.feeValue.postApply {
            it.text = feeText
        }
    }

    override fun setCoin1(coin1Amount: BigDecimal?, coin1: String) {
        coin1Amount?.let {
            if(it == BigDecimal.ZERO) {
                b.inputCoin1.setText("0")
                b.inputCoin1.input.setSelection(0, b.inputCoin1.text?.length?:0)
            } else {
                b.inputCoin1.setText(it.plain())
            }
        }
        calcCoin1Padding(coin1)
    }

    override fun setSlippage(amount: String) {
        b.inputSlippage.setText(amount)
    }

    override fun setMaxAmount(amount: String) {
        b.inputMaxSpend.setText(amount)
    }

    override fun setSlippageLabel(text: CharSequence) {
        b.inputSlippage.label = text
    }

    override fun setMaxAmountLabel(text: CharSequence) {
        b.inputMaxSpend.label = text
    }

    override fun finishSuccess() {
        setResult(Activity.RESULT_OK)
        finish()
    }

    override fun finishCancel() {
        finish()
    }

    override fun startExplorer(txHash: MinterHash) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Wallet.urlExplorerFront() + "/transactions/" + txHash.toString())))
    }

    class Builder : ActivityBuilder {
        private val pool: PoolCombined

        constructor(from: Activity, pool: PoolCombined) : super(from) {
            this.pool = pool
        }

        constructor(from: Fragment, pool: PoolCombined) : super(from) {
            this.pool = pool
        }

        constructor(from: Service,pool: PoolCombined) : super(from) {
            this.pool = pool
        }

        override fun onBeforeStart(intent: Intent) {
            super.onBeforeStart(intent)
            intent.putExtra(ARG_POOL, pool)
        }

        override fun getActivityClass(): Class<*> {
            return PoolAddLiquidityActivity::class.java
        }
    }


}