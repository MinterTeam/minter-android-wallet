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
package network.minter.bipwallet.exchange.contract

import android.view.View
import androidx.annotation.StringRes
import com.edwardstock.inputfield.form.InputWrapper
import moxy.MvpView
import moxy.viewstate.strategy.AddToEndSingleStrategy
import moxy.viewstate.strategy.OneExecutionStateStrategy
import moxy.viewstate.strategy.StateStrategyType
import network.minter.bipwallet.exchange.ExchangeAmount
import network.minter.bipwallet.internal.dialogs.DialogExecutor
import network.minter.bipwallet.sending.account.SelectorData
import network.minter.explorer.models.CoinBalance
import network.minter.explorer.models.CoinItem

/**
 * minter-android-wallet. 2019
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
@StateStrategyType(AddToEndSingleStrategy::class)
interface ExchangeView : MvpView {
    @StateStrategyType(OneExecutionStateStrategy::class)
    fun startDialog(executor: DialogExecutor)
    fun setOnClickMaximum(listener: View.OnClickListener)
    fun setOnClickSubmit(listener: View.OnClickListener)
    fun setTextChangedListener(listener: (InputWrapper, Boolean) -> Unit)

    @StateStrategyType(OneExecutionStateStrategy::class)
    fun startAccountSelector(accounts: List<CoinBalance>, clickListener: (SelectorData<CoinBalance>) -> Unit)
    fun setOnClickSelectAccount(listener: View.OnClickListener)
    fun setError(field: String, message: CharSequence?)
    fun clearErrors()
    fun setSubmitEnabled(enabled: Boolean)
    fun setFormValidationListener(listener: (Boolean) -> Unit)

    @StateStrategyType(OneExecutionStateStrategy::class)
    fun startExplorer(txHash: String)
    fun finishCancel()
    fun finishSuccess(exchangeAmount: ExchangeAmount)
    fun setCalculation(calculation: String)
    fun setOutAccountName(accountName: CharSequence)
    fun setAmount(amount: CharSequence)
    fun setCoinsAutocomplete(listener: (CoinItem, Int) -> Unit)
    fun setIncomingCoin(symbol: String)
    fun setFee(commission: CharSequence)
    fun showCalculationProgress(show: Boolean)
    fun setCalculationTitle(@StringRes calcTitle: Int)
    fun hideKeyboard()
    fun hideCalculation()
    fun validateForm()
}