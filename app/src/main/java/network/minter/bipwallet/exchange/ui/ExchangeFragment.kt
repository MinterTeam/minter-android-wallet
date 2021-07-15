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
package network.minter.bipwallet.exchange.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.edwardstock.inputfield.InputField
import com.edwardstock.inputfield.InputFieldAutocomplete
import com.edwardstock.inputfield.form.DecimalInputFilter
import com.edwardstock.inputfield.form.InputGroup
import com.edwardstock.inputfield.form.InputWrapper
import com.edwardstock.inputfield.form.validators.DecimalValidator
import com.otaliastudios.autocomplete.Autocomplete
import com.otaliastudios.autocomplete.AutocompleteCallback
import com.otaliastudios.autocomplete.AutocompletePolicy
import com.otaliastudios.autocomplete.AutocompletePresenter
import network.minter.bipwallet.R
import network.minter.bipwallet.databinding.IncludeExchangeCalculationBinding
import network.minter.bipwallet.exchange.adapters.CoinsAcPresenter
import network.minter.bipwallet.exchange.contract.ExchangeView
import network.minter.bipwallet.exchange.models.ExchangeAmount
import network.minter.bipwallet.internal.BaseInjectFragment
import network.minter.bipwallet.internal.Wallet
import network.minter.bipwallet.internal.dialogs.DialogExecutor
import network.minter.bipwallet.internal.dialogs.WalletDialog
import network.minter.bipwallet.internal.dialogs.WalletDialog.Companion.releaseDialog
import network.minter.bipwallet.internal.dialogs.WalletDialog.Companion.switchDialogWithExecutor
import network.minter.bipwallet.internal.helpers.KeyboardHelper
import network.minter.bipwallet.internal.helpers.ViewExtensions.postApply
import network.minter.bipwallet.internal.helpers.ViewExtensions.scrollDownTo
import network.minter.bipwallet.internal.helpers.ViewExtensions.tr
import network.minter.bipwallet.internal.helpers.ViewExtensions.visible
import network.minter.bipwallet.internal.helpers.forms.validators.CoinFilter
import network.minter.bipwallet.internal.helpers.forms.validators.CoinValidatorWithSuffix
import network.minter.bipwallet.internal.mvp.ErrorViewWithRetry
import network.minter.bipwallet.internal.system.BroadcastReceiverManager
import network.minter.bipwallet.internal.views.widgets.WalletButton
import network.minter.bipwallet.sending.account.SelectorData
import network.minter.bipwallet.sending.account.SelectorDialog
import network.minter.bipwallet.sending.account.selectorDataFromCoins
import network.minter.bipwallet.services.livebalance.broadcast.RTMBlockReceiver
import network.minter.bipwallet.wallets.utils.LastBlockHandler
import network.minter.explorer.models.CoinBalance
import network.minter.explorer.models.CoinItem
import java.util.regex.Pattern

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */

data class ExchangeBinding(
        val root: ScrollView,
        val inputOutgoingCoin: InputField,
        val inputIncomingCoin: InputFieldAutocomplete,
        val inputAmount: InputField,
        val calculationContainer: IncludeExchangeCalculationBinding,
        val action: WalletButton,
        val lastUpdated: TextView
)

abstract class ExchangeFragment : BaseInjectFragment(), ExchangeView {

    protected var inputGroup = InputGroup()
    private var walletDialog: WalletDialog? = null
    protected lateinit var binding: ExchangeBinding

    private var autocomplete: Autocomplete<CoinItem>? = null
    private var onCoinSelected: ((CoinItem, Int) -> Unit)? = null


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.inputOutgoingCoin.input.isFocusable = false

        inputGroup.clearErrorBeforeValidate = false
        inputGroup.addInput(binding.inputIncomingCoin, binding.inputAmount)
        inputGroup.addValidator(binding.inputAmount, DecimalValidator(tr(R.string.input_validator_err_invalid_number)))
        inputGroup.addFilter(binding.inputAmount, DecimalInputFilter(binding.inputAmount))
        inputGroup.addValidator(binding.inputIncomingCoin, CoinValidatorWithSuffix(tr(R.string.input_validator_err_invalid_coin_name)))
        inputGroup.addFilter(binding.inputIncomingCoin, CoinFilter())
        inputGroup.addFilter(binding.inputAmount, DecimalInputFilter(binding.inputAmount))
        binding.calculationContainer.calculation.inputType = InputType.TYPE_NULL

        if (this is SellExchangeFragment) {
            binding.inputIncomingCoin.addTextChangedSimpleListener {
                binding.root.scrollDownTo(binding.calculationContainer.root)
            }
        }

        val coinsAutocompletePresenter = CoinsAcPresenter(requireContext(), Wallet.app().coinsCacheRepo())
        val coinsAutocompleteCallback: AutocompleteCallback<CoinItem> = object : AutocompleteCallback<CoinItem> {
            override fun onPopupItemClicked(editable: Editable, item: CoinItem): Boolean {
                runOnUiThread {
                    onCoinSelected?.invoke(item, 0)
                }
                return true
            }

            override fun onPopupVisibilityChanged(shown: Boolean) {}
        }

        autocomplete = Autocomplete.on<CoinItem>(binding.inputIncomingCoin.input)
                .with(coinsAutocompleteCallback)
                .with(coinsAutocompletePresenter as AutocompletePresenter<CoinItem>)
                .with(coinsAutocompletePresenter as AutocompletePolicy)
                .with(6f)
                .with(ContextCompat.getDrawable(requireContext(), R.drawable.shape_rounded_white))
                .build()


        LastBlockHandler.handle(binding.lastUpdated)
        val broadcastManager = BroadcastReceiverManager(requireActivity())
        broadcastManager.add(RTMBlockReceiver {
            LastBlockHandler.handle(binding.lastUpdated, it)
        })
        broadcastManager.register()
    }

    override fun onStop() {
        super.onStop()
        autocomplete?.dismissPopup()
    }

    override fun onDestroyView() {
        releaseDialog(walletDialog)
        super.onDestroyView()
    }

    override fun startDialog(executor: DialogExecutor) {
        runOnUiThread {
            walletDialog = switchDialogWithExecutor(this, walletDialog, executor)
        }
    }


    override fun setTextChangedListener(listener: (InputWrapper, Boolean) -> Unit) {
        inputGroup.addTextChangedListener(listener)
    }

    override fun startAccountSelector(accounts: List<CoinBalance>, clickListener: (SelectorData<CoinBalance>) -> Unit) {
        SelectorDialog.Builder<CoinBalance>(requireActivity(), R.string.dialog_title_choose_coin)
                .setItems(selectorDataFromCoins(accounts))
                .setOnClickListener(clickListener)
                .create().show()
    }

    override fun setError(field: String, message: CharSequence?) {
        inputGroup.setError(field, message)
    }

    override fun clearErrors() {
        inputGroup.clearErrors()
    }

    override fun setFormValidationListener(listener: (Boolean) -> Unit) {
        inputGroup.addFormValidateListener(listener)
    }

    override fun startExplorer(txHash: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Wallet.urlExplorerFront() + "/transactions/" + txHash)))
    }

    override fun finishCancel() {
        requireActivity().finish()
    }

    override fun finishSuccess(exchangeAmount: ExchangeAmount) {
        val data = Intent()
        data.putExtra(ConvertCoinActivity.RESULT_EXCHANGE_AMOUNT, exchangeAmount)
        requireActivity().setResult(Activity.RESULT_OK, data)
        requireActivity().finish()
    }

    override fun hideKeyboard() {
        KeyboardHelper.hideKeyboard(this)
    }

    override fun setCoinsAutocomplete(listener: (CoinItem, Int) -> Unit) {
        onCoinSelected = listener
    }

    override fun setCalculationTitle(calcTitle: Int) {
        binding.calculationContainer.calculationTitle.setText(calcTitle)
    }

    override fun setOnClickMaximum(listener: View.OnClickListener) {
        binding.inputAmount.setOnSuffixTextClickListener(listener)
    }

    override fun setOnClickSubmit(listener: View.OnClickListener) {
        binding.action.setOnClickListener(listener)
    }

    override fun setOnClickSelectAccount(listener: View.OnClickListener) {
        binding.inputOutgoingCoin.input.setOnClickListener(listener)
        binding.inputOutgoingCoin.setOnSuffixImageClickListener(listener)
    }

    override fun setSubmitEnabled(enabled: Boolean) {
        binding.action.isEnabled = enabled
    }

    override fun setCalculation(calculation: String) {
        val layout = binding.calculationContainer.layoutCalculation
        val view = binding.calculationContainer.calculation

        view.post {
            if (layout.visibility == View.GONE) {
                layout.visibility = View.VISIBLE
            }
            view.maxLines = 100
            view.isSingleLine = false
            if (calculation.length > 64) {
                val m = Pattern.compile("\\s+").matcher(calculation)
                var cnt = 0
                while (m.find()) {
                    cnt++
                }
                var idx = cnt * 3 - 1
                if (cnt >= 64) {
                    var toConcat: String
                    do {
                        toConcat = calculation.substring(0, ++idx)
                    } while (toConcat[toConcat.length - 1] != ' ')
                    toConcat += tr(R.string.easter_egg)
                    toConcat += calculation.substring(idx)
                    view.text = toConcat
                    return@post
                }
            }
            view.text = calculation
        }
    }

    override fun hideCalculation() {
        binding.calculationContainer.layoutCalculation.visible = false
    }

    override fun validateForm() {

    }

    override fun setOutAccountName(accountName: CharSequence) {
        binding.inputOutgoingCoin.setText(accountName)
    }

    override fun setAmount(amount: CharSequence) {
        binding.inputAmount.setText(amount)
    }


    override fun setIncomingCoin(symbol: String) {
        binding.inputIncomingCoin.postApply {
            binding.inputIncomingCoin.setText(symbol)
            autocomplete?.dismissPopup()
            binding.inputIncomingCoin.setSelection(symbol.length)
        }
    }

    override fun setFee(commission: CharSequence) {
        binding.apply {
            calculationContainer.feeValue.postApply {
                it.text = commission
            }
        }

    }

    override fun showCalculationProgress(show: Boolean) {
        binding.apply {
            calculationContainer.calculationProgress.post {
                calculationContainer.calculationProgress.visible = show
            }
        }
    }

    override fun onError(t: Throwable?) {
        (activity as ErrorViewWithRetry).onError(t)
    }

    override fun onError(err: String?) {
        (activity as ErrorViewWithRetry).onError(err)
    }

    override fun onErrorWithRetry(errorMessage: String?, errorResolver: View.OnClickListener?) {
        (activity as ErrorViewWithRetry).onErrorWithRetry(errorMessage, errorResolver)
    }

    override fun onErrorWithRetry(errorMessage: String?, actionName: String?, errorResolver: View.OnClickListener?) {
        (activity as ErrorViewWithRetry).onErrorWithRetry(errorMessage, actionName, errorResolver)
    }
}
