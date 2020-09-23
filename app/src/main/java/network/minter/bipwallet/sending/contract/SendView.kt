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
package network.minter.bipwallet.sending.contract

import android.text.TextWatcher
import android.view.View
import androidx.annotation.StringRes
import com.edwardstock.inputfield.form.InputWrapper
import moxy.MvpView
import moxy.viewstate.strategy.AddToEndSingleStrategy
import moxy.viewstate.strategy.OneExecutionStateStrategy
import moxy.viewstate.strategy.StateStrategyType
import network.minter.bipwallet.addressbook.models.AddressContact
import network.minter.bipwallet.internal.dialogs.DialogExecutor
import network.minter.bipwallet.sending.account.SelectorData
import network.minter.bipwallet.wallets.contract.WalletSelectorControllerView
import network.minter.core.crypto.MinterPublicKey
import network.minter.explorer.models.CoinBalance

/**
 * minter-android-wallet. 2019
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
@StateStrategyType(AddToEndSingleStrategy::class)
interface SendView : WalletSelectorControllerView, MvpView {
    fun setOnClickAccountSelectedListener(listener: View.OnClickListener)
    fun setOnClickMaximum(listener: View.OnClickListener)
    fun setOnClickAddPayload(listener: View.OnClickListener)
    fun setOnClickClearPayload(listener: View.OnClickListener)
    fun setOnTextChangedListener(listener: (InputWrapper, Boolean) -> Unit)
    fun setOnContactsClickListener(listener: View.OnClickListener)
    fun setFormValidationListener(listener: (Boolean) -> Unit)

    @StateStrategyType(OneExecutionStateStrategy::class)
    fun startAccountSelector(accounts: List<SelectorData<CoinBalance>>, clickListener: (SelectorData<CoinBalance>) -> Unit)
    fun setAccountName(accountName: CharSequence)
    fun setOnSubmit(listener: View.OnClickListener)
    fun setSubmitEnabled(enabled: Boolean)
    fun clearInputs()
    fun clearAmount()

    @StateStrategyType(OneExecutionStateStrategy::class)
    fun startDialog(executor: DialogExecutor)

    @StateStrategyType(OneExecutionStateStrategy::class)
    fun startExplorer(txHash: String)

    @StateStrategyType(OneExecutionStateStrategy::class)
    fun startScanQR(requestCode: Int)

    @StateStrategyType(OneExecutionStateStrategy::class)
    fun startScanQRWithPermissions(requestCode: Int)

    @StateStrategyType(OneExecutionStateStrategy::class)
    fun startAddContact(address: String, onAdded: (AddressContact) -> Unit)

    @StateStrategyType(OneExecutionStateStrategy::class)
    fun startAddressBook(requestCode: Int)
    fun setRecipient(to: AddressContact)
    fun setRecipientError(error: CharSequence?)
    fun setAmountError(error: CharSequence?)
    fun setPayloadError(error: CharSequence?)
    fun setCommonError(error: CharSequence?)
    fun setError(@StringRes error: Int)
    fun setAmount(amount: CharSequence?)
    fun setFee(fee: CharSequence?)
    fun setRecipientAutocompleteItemClickListener(listener: (AddressContact, Int) -> Unit)
    fun setRecipientAutocompleteItems(items: List<AddressContact>)
    fun hideAutocomplete()
    fun setPayloadChangeListener(listener: TextWatcher)
    fun setPayload(payload: String?)
    fun setActionTitle(buttonTitle: Int)

    @StateStrategyType(OneExecutionStateStrategy::class)
    fun startExternalTransaction(rawData: String?)
    fun showPayload()
    fun hidePayload()
    fun setMaxAmountValidator(coinSupplier: () -> CoinBalance?)
    fun showBalanceProgress(show: Boolean)

    @StateStrategyType(OneExecutionStateStrategy::class)
    fun startDelegate(publicKey: MinterPublicKey)

    fun validate(onValidated: (Boolean) -> Unit)


}