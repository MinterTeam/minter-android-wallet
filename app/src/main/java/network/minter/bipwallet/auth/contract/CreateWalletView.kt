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
package network.minter.bipwallet.auth.contract

import android.view.View
import android.widget.CompoundButton
import androidx.annotation.StringRes
import com.edwardstock.inputfield.form.InputWrapper
import moxy.MvpView
import moxy.viewstate.strategy.AddToEndSingleStrategy
import moxy.viewstate.strategy.OneExecutionStateStrategy
import moxy.viewstate.strategy.StateStrategyType
import network.minter.bipwallet.wallets.selector.WalletItem

@StateStrategyType(AddToEndSingleStrategy::class)
interface CreateWalletView : MvpView {

    fun setDescription(@StringRes resId: Int)
    fun setSeed(seedPhrase: CharSequence)
    fun setOnSeedClickListener(listener: View.OnClickListener)
    fun setOnSavedClickListener(checkedChangeListener: CompoundButton.OnCheckedChangeListener)
    fun setOnSubmit(listener: View.OnClickListener)
    fun showCopiedAlert()
    fun setSubmitEnabled(enabled: Boolean)
    fun addInputTextWatcher(listener: (InputWrapper, Boolean) -> Unit)
    fun setEnableTitleInput(enable: Boolean)
    fun setEnableDescription(enable: Boolean)
    fun setWalletTitle(title: String?)
    fun showCancelAction(show: Boolean)
    fun setDialogTitle(title: String?)
    fun setDialogTitle(@StringRes resId: Int)

    @StateStrategyType(OneExecutionStateStrategy::class)
    fun startHome()

    @StateStrategyType(OneExecutionStateStrategy::class)
    fun close()
    fun callOnAdd(walletItem: WalletItem)
}