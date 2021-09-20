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
import android.view.View
import androidx.fragment.app.Fragment
import com.edwardstock.inputfield.InputField
import com.edwardstock.inputfield.form.DecimalInputFilter
import com.edwardstock.inputfield.form.InputGroup
import com.edwardstock.inputfield.form.InputWrapper
import moxy.presenter.InjectPresenter
import moxy.presenter.ProvidePresenter
import network.minter.bipwallet.apis.reactive.avatar
import network.minter.bipwallet.databinding.ActivityPoolRemoveLiquidityBinding
import network.minter.bipwallet.internal.BaseMvpInjectActivity
import network.minter.bipwallet.internal.Wallet
import network.minter.bipwallet.internal.helpers.MathHelper.humanize
import network.minter.bipwallet.internal.helpers.MathHelper.plain
import network.minter.bipwallet.internal.helpers.ViewExtensions.textWidth
import network.minter.bipwallet.internal.helpers.forms.validators.BigDecimalNumberValidator
import network.minter.bipwallet.internal.system.ActivityBuilder
import network.minter.bipwallet.pools.contracts.PoolRemoveLiquidityView
import network.minter.bipwallet.pools.models.PoolCombined
import network.minter.bipwallet.pools.views.PoolRemoveLiquidityPresenter
import network.minter.core.crypto.MinterHash
import network.minter.explorer.models.CoinItemBase
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Provider

/**
 * minter-android-wallet. 2021
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
class PoolRemoveLiquidityActivity: BaseMvpInjectActivity(), PoolRemoveLiquidityView {

    companion object {
        const val ARG_POOL = "ARG_POOL"
    }

    @Inject lateinit var presenterProvider: Provider<PoolRemoveLiquidityPresenter>
    @InjectPresenter lateinit var presenter: PoolRemoveLiquidityPresenter

    private lateinit var b: ActivityPoolRemoveLiquidityBinding
    private val inputGroup = InputGroup()

    @ProvidePresenter
    fun providePresenter(): PoolRemoveLiquidityPresenter {
        return presenterProvider.get()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityPoolRemoveLiquidityBinding.inflate(layoutInflater)
        setContentView(b.root)
        setupToolbar(b.toolbar)
        setResult(RESULT_CANCELED)

        presenter.handleExtras(intent)

        b.apply {
            inputGroup.addInput(inputToken)
            inputGroup.addInput(inputLiquidity)

            inputGroup.addFilter(inputToken, DecimalInputFilter(inputToken))
            inputGroup.addFilter(inputLiquidity, DecimalInputFilter(inputLiquidity, 2))

            inputGroup.addValidator(inputLiquidity, BigDecimalNumberValidator(BigDecimal.ZERO, BigDecimal("100")).apply { errorMessage = "Percent must be from 0 to 100" })
        }
    }

    override fun setCoin0(coin0Amount: BigDecimal?, coin: CoinItemBase) {
        b.coin0.text = coin.symbol
        b.coin0Amount.text = coin0Amount.humanize()
        b.coin0Avatar.setImageUrl(coin.avatar)
    }

    override fun setCoin1(coin1Amount: BigDecimal?, coin1: CoinItemBase) {
        b.coin1.text = coin1.symbol
        b.coin1Amount.text = coin1Amount.humanize()
        b.coin1Avatar.setImageUrl(coin1.avatar)
    }

    override fun setLiquidityPercent(percent: BigDecimal) {
        b.inputLiquidity.setText(percent.plain())
    }

    override fun setTokenAmount(amount: BigDecimal) {
        b.inputToken.setText(amount.plain())
    }

    private fun calcSuffixPadding(input: InputField): Int {
        return input.textWidth() + Wallet.app().display().dpToPx(4)
    }

    override fun setTokenAmountValidator(maxValue: BigDecimal) {
        inputGroup.addValidator(b.inputToken, BigDecimalNumberValidator(BigDecimal.ZERO, maxValue, "Incorrect amount: balance ${maxValue.humanize()}"))
    }

    override fun setOnTextChangedListener(listener: (InputWrapper, Boolean) -> Unit) {
        inputGroup.addTextChangedListener(listener)
    }

    override fun setOnSubmit(listener: (View) -> Unit) {
        b.action.setOnClickListener(listener)
    }

    override fun setEnableSubmit(enable: Boolean) {
        b.action.isEnabled = enable
    }

    override fun setFee(feeText: CharSequence) {
        b.feeValue.text = feeText
    }

    override fun setLiquidityPercentError(error: CharSequence?) {
        inputGroup.setError("liquidity", error)
    }

    override fun setTokenAmountError(error: CharSequence?) {
        inputGroup.setError("token", error)
    }

    override fun setOnUseMaxClickListener(listener: (View) -> Unit) {
        b.inputToken.setOnSuffixTextClickListener(listener)
    }

    override fun startExplorer(txHash: MinterHash) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Wallet.urlExplorerFront() + "/transactions/" + txHash.toString())))
    }

    override fun finishSuccess() {
        setResult(Activity.RESULT_OK)
        finish()
    }

    class Builder : ActivityBuilder {
        private val pool: PoolCombined

        constructor(from: Activity, pool: PoolCombined) : super(from) {
            this.pool = pool
        }

        constructor(from: Fragment, pool: PoolCombined) : super(from) {
            this.pool = pool
        }

        constructor(from: Service, pool: PoolCombined) : super(from) {
            this.pool = pool
        }

        override fun onBeforeStart(intent: Intent) {
            super.onBeforeStart(intent)
            intent.putExtra(ARG_POOL, pool)
        }

        override fun getActivityClass(): Class<*> {
            return PoolRemoveLiquidityActivity::class.java
        }
    }
}