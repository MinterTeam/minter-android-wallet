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
package network.minter.bipwallet.exchange.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.Spanned
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
import com.edwardstock.inputfield.form.validators.RegexValidator
import com.otaliastudios.autocomplete.Autocomplete
import com.otaliastudios.autocomplete.AutocompleteCallback
import com.otaliastudios.autocomplete.AutocompletePolicy
import com.otaliastudios.autocomplete.AutocompletePresenter
import network.minter.bipwallet.R
import network.minter.bipwallet.databinding.IncludeExchangeCalculationBinding
import network.minter.bipwallet.exchange.adapters.CoinsAcPresenter
import network.minter.bipwallet.exchange.contract.ExchangeView
import network.minter.bipwallet.internal.BaseInjectFragment
import network.minter.bipwallet.internal.Wallet
import network.minter.bipwallet.internal.dialogs.DialogExecutor
import network.minter.bipwallet.internal.dialogs.WalletDialog
import network.minter.bipwallet.internal.dialogs.WalletDialog.Companion.releaseDialog
import network.minter.bipwallet.internal.dialogs.WalletDialog.Companion.switchDialogWithExecutor
import network.minter.bipwallet.internal.helpers.KeyboardHelper
import network.minter.bipwallet.internal.helpers.ViewExtensions.postApply
import network.minter.bipwallet.internal.helpers.ViewExtensions.visible
import network.minter.bipwallet.internal.system.BroadcastReceiverManager
import network.minter.bipwallet.internal.views.widgets.WalletButton
import network.minter.bipwallet.sending.account.SelectorData
import network.minter.bipwallet.sending.account.WalletAccountSelectorDialog
import network.minter.bipwallet.sending.account.selectorDataFromCoins
import network.minter.bipwallet.services.livebalance.broadcast.RTMBlockReceiver
import network.minter.bipwallet.wallets.utils.LastBlockHandler
import network.minter.explorer.models.CoinBalance
import network.minter.explorer.models.CoinItem
import timber.log.Timber
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
        inputGroup.addValidator(binding.inputAmount, DecimalValidator("Invalid number"))
        inputGroup.addFilter(binding.inputAmount, DecimalInputFilter(binding.inputAmount))
        val coinValidator = RegexValidator("^[a-zA-Z0-9]{1,10}$").apply {
            errorMessage = "Invalid coin name"
        }
        inputGroup.addValidator(binding.inputIncomingCoin, coinValidator)
        inputGroup.addFilter(binding.inputIncomingCoin, InputFilter { source: CharSequence, start: Int, end: Int, dest: Spanned?, dstart: Int, dend: Int ->
            Timber.d("Filter: source=%s, start=%d, end=%d, dest=%s, destStart=%d, destEnd=%d", source, start, end, dest, dstart, dend)
            source.toString().toUpperCase().replace("[^A-Z0-9]".toRegex(), "")
        })
        inputGroup.addFilter(binding.inputAmount, DecimalInputFilter(binding.inputAmount))
        binding.calculationContainer.calculation.inputType = InputType.TYPE_NULL


        val coinsAutocompletePresenter = CoinsAcPresenter(context!!, Wallet.app().coinsCacheRepo())
        val coinsAutocompleteCallback: AutocompleteCallback<CoinItem> = object : AutocompleteCallback<CoinItem> {
            override fun onPopupItemClicked(editable: Editable, item: CoinItem): Boolean {
                onCoinSelected?.invoke(item, 0)
                return true
            }

            override fun onPopupVisibilityChanged(shown: Boolean) {}
        }

        autocomplete = Autocomplete.on<CoinItem>(binding.inputIncomingCoin.input)
                .with(coinsAutocompleteCallback)
                .with(coinsAutocompletePresenter as AutocompletePresenter<CoinItem>)
                .with(coinsAutocompletePresenter as AutocompletePolicy)
                .with(6f)
                .with(ContextCompat.getDrawable(context!!, R.drawable.shape_rounded_white))
                .build()


        LastBlockHandler.handle(binding.lastUpdated)
        val broadcastManager = BroadcastReceiverManager(activity!!)
        broadcastManager.add(RTMBlockReceiver {
            LastBlockHandler.handle(binding.lastUpdated, it)
        })
        broadcastManager.register()
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
        WalletAccountSelectorDialog.Builder<CoinBalance>(activity!!, R.string.dialog_title_choose_coin)
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

    override fun finish() {
        activity!!.finish()
    }

    override fun hideKeyboard() {
        KeyboardHelper.hideKeyboard(this)
    }

    override fun setCoinsAutocomplete(items: List<CoinItem>, listener: (CoinItem, Int) -> Unit) {
        onCoinSelected = listener

//        if (items.isNotEmpty()) {
//            binding.inputIncomingCoin.input.setDropDownBackgroundResource(R.drawable.shape_rounded_white)
//
//            val adapter = CoinsListAdapter(activity!!, items)
//            adapter.setOnItemClickListener { item, position ->
//                listener(item, position)
//                binding.inputIncomingCoin.input.dismissDropDown()
//            }
//            binding.inputIncomingCoin.post {
//                binding.inputIncomingCoin.input.setAdapter(adapter)
//            }
//        }
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
        binding.inputOutgoingCoin.setOnClickListener(listener)
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
                    toConcat += "THE MATRIX HAS YOU "
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

    override fun setOutAccountName(accountName: CharSequence) {
        binding.inputOutgoingCoin.setText(accountName)
    }

    override fun setAmount(amount: CharSequence) {
        binding.inputAmount.setText(amount)
    }


    override fun setIncomingCoin(symbol: String) {
        binding.inputIncomingCoin.setText(symbol)
        binding.inputIncomingCoin.setSelection(symbol.length)
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
}