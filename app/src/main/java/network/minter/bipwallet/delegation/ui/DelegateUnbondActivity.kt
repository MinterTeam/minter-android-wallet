/*
 * Copyright (C) by MinterTeam. 2022
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

package network.minter.bipwallet.delegation.ui

import android.app.Activity
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.edwardstock.inputfield.InputField
import com.edwardstock.inputfield.form.DecimalInputFilter
import com.edwardstock.inputfield.form.InputGroup
import com.edwardstock.inputfield.form.InputWrapper
import com.edwardstock.inputfield.form.validators.CustomValidator
import com.edwardstock.inputfield.form.validators.RegexValidator
import com.otaliastudios.autocomplete.Autocomplete
import com.otaliastudios.autocomplete.AutocompleteCallback
import com.otaliastudios.autocomplete.AutocompletePolicy
import com.otaliastudios.autocomplete.AutocompletePresenter
import moxy.presenter.InjectPresenter
import moxy.presenter.ProvidePresenter
import network.minter.bipwallet.R
import network.minter.bipwallet.databinding.ActivityDelegateUnbondBinding
import network.minter.bipwallet.delegation.adapter.autocomplete.ValidatorsAcPresenter
import network.minter.bipwallet.delegation.contract.DelegateUnbondView
import network.minter.bipwallet.delegation.contract.GetValidator
import network.minter.bipwallet.delegation.contract.GetValidatorOptions
import network.minter.bipwallet.delegation.views.DelegateUnbondPresenter
import network.minter.bipwallet.internal.BaseMvpInjectActivity
import network.minter.bipwallet.internal.Wallet
import network.minter.bipwallet.internal.helpers.ErrorViewHelper
import network.minter.bipwallet.internal.helpers.MathHelper.parseBigDecimal
import network.minter.bipwallet.internal.helpers.ViewExtensions.postApply
import network.minter.bipwallet.internal.helpers.forms.validators.NewLineInputFilter
import network.minter.bipwallet.internal.system.ActivityBuilder
import network.minter.bipwallet.internal.system.BroadcastReceiverManager
import network.minter.bipwallet.sending.account.SelectorData
import network.minter.bipwallet.sending.account.SelectorDialog
import network.minter.bipwallet.services.livebalance.broadcast.RTMBlockReceiver
import network.minter.bipwallet.wallets.utils.LastBlockHandler
import network.minter.blockchain.models.operational.OperationType
import network.minter.core.crypto.MinterHash
import network.minter.core.crypto.MinterPublicKey
import network.minter.explorer.models.BaseCoinValue
import network.minter.explorer.models.CoinItemBase
import network.minter.explorer.models.ValidatorItem
import org.parceler.Parcels
import javax.inject.Inject
import javax.inject.Provider

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
class DelegateUnbondActivity : BaseMvpInjectActivity(), DelegateUnbondView {

    enum class Type(
            val opType: OperationType,
            val titleRes: Int,
            val firstLabelRes: Int,
            val secondLabelRes: Int,
            val progressTitleRes: Int,
            val resultLabelRes: Int
    ) {
        Delegate(
                OperationType.Delegate,
                R.string.dialog_title_delegate_begin,
                R.string.dialog_label_delegate_you_are_delegating,
                R.string.dialog_label_delegate_to,
                R.string.dialog_label_delegate_progress,
                R.string.dialog_label_delegate_result
        ),
        Unbond(
                OperationType.Unbound,
                R.string.dialog_title_unbond_begin,
                R.string.dialog_label_unbond_you_are_unbonding,
                R.string.dialog_label_unbond_from,
                R.string.dialog_label_unbond_progress,
                R.string.dialog_label_unbond_result
        );
    }

    companion object {
        const val ARG_PUB_KEY = "ARG_PUB_KEY"
        const val ARG_COIN = "ARG_COIN"
        const val ARG_TYPE = "ARG_TYPE"
    }

    @Inject lateinit var presenterProvider: Provider<DelegateUnbondPresenter>
    @InjectPresenter lateinit var presenter: DelegateUnbondPresenter
    private lateinit var binding: ActivityDelegateUnbondBinding

    private val inputGroup = InputGroup()
    private var type: Type = Type.Delegate
    private var autocomplete: Autocomplete<ValidatorItem>? = null

    private val getValidatorLauncher = registerForActivityResult(GetValidator()) {
        it?.let {
            presenter.onValidatorSelected(it)
        }
    }

    @ProvidePresenter
    fun providePresenter(): DelegateUnbondPresenter {
        return presenterProvider.get()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDelegateUnbondBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setResult(Activity.RESULT_CANCELED)

        setupToolbar(binding.toolbar)

        inputGroup.setup {
            add(binding.inputAmount, RegexValidator("^(\\d*)(\\.)?(\\d{1,18})?$").apply {
                errorMessage = "Invalid number"
            })
        }
        inputGroup.addFilter(binding.inputAmount, DecimalInputFilter(binding.inputAmount))

        inputGroup.addInput(binding.inputValidator)
        inputGroup.addFilter(binding.inputValidator, NewLineInputFilter())
        inputGroup.clearErrorBeforeValidate = false

        presenter.handleExtras(intent)
        type = intent.extras!!.getSerializable(ARG_TYPE) as Type

        binding.inputValidator.input.isFocusable = type != Type.Unbond
        binding.inputCoin.input.isFocusable = false
        binding.inputAmount.input.setOnClickListener {
            binding.inputAmount.scrollToError()
        }

        setupAutocomplete()
        setupSyncStatus()
    }

    override fun onError(t: Throwable?) {
        ErrorViewHelper(binding.errorView).onError(t)
    }

    override fun onError(err: String?) {
        ErrorViewHelper(binding.errorView).onError(err)
    }

    override fun onErrorWithRetry(errorMessage: String?, errorResolver: View.OnClickListener?) {
        ErrorViewHelper(binding.errorView).onErrorWithRetry(errorMessage, errorResolver)
    }

    override fun onErrorWithRetry(errorMessage: String?, actionName: String?, errorResolver: View.OnClickListener?) {
        ErrorViewHelper(binding.errorView).onErrorWithRetry(errorMessage, actionName, errorResolver)
    }

    private fun setupSyncStatus() {
        LastBlockHandler.handle(binding.lastUpdated)
        val broadcastManager = BroadcastReceiverManager(this)
        broadcastManager.add(RTMBlockReceiver {
            LastBlockHandler.handle(binding.lastUpdated, it)
        })
        broadcastManager.register()
    }

    private var onAutocompleteItemSelect: ((ValidatorItem) -> Unit)? = null
    private var autocompletePresenter: ValidatorsAcPresenter? = null

    private fun setupAutocomplete() {
        autocompletePresenter = ValidatorsAcPresenter(this) { showPopup, query ->
            if (!showPopup) {
                autocomplete?.dismissPopup()
            } else {
                autocomplete?.showPopup(query)
            }
        }
        val callback: AutocompleteCallback<ValidatorItem> = object : AutocompleteCallback<ValidatorItem> {
            override fun onPopupItemClicked(editable: Editable, item: ValidatorItem): Boolean {
                onAutocompleteItemSelect?.invoke(item)
                return true
            }

            override fun onPopupVisibilityChanged(shown: Boolean) {
            }
        }

        autocomplete = Autocomplete.on<ValidatorItem>(binding.inputValidator.input)
                .with(callback)
                .with(autocompletePresenter as AutocompletePresenter<ValidatorItem>)
                .with(autocompletePresenter as AutocompletePolicy)
                .with(6f)
                .with(ContextCompat.getDrawable(this, R.drawable.shape_rounded_white))
                .build()
    }

    override fun hideValidatorOverlay() {
        binding.inputValidator.inputOverlayVisible = false
        binding.inputValidator.input.isFocusable = true
        binding.inputValidator.input.isFocusableInTouchMode = true
    }

    override fun setTextChangedListener(listener: (input: InputWrapper, valid: Boolean) -> Unit) {
        inputGroup.addTextChangedListener(listener)
    }

    override fun setEnableSubmit(enable: Boolean) {
        binding.action.postApply { it.isEnabled = enable }
    }

    override fun setAccountError(error: CharSequence?) {
        binding.inputCoin.error = error
        binding.inputCoin.errorEnabled = !error.isNullOrEmpty()
    }

    override fun setValidator(validator: ValidatorItem, onInflated: (View) -> Unit) {
        binding.apply {
            inputValidator.setSuffixImageSrc(R.drawable.ic_validator_list)
            inputValidator.text = null
            inputValidator.inputOverlayVisible = true
            binding.inputValidator.input.isFocusable = false
            onInflated(inputValidator.inputOverlay!!)
        }
    }

    override fun setValidator(validator: MinterPublicKey, onInflated: (View) -> Unit) {
        binding.apply {
            inputValidator.setSuffixImageSrc(R.drawable.ic_validator_list)
            inputValidator.inputOverlayVisible = true
            binding.inputValidator.input.isFocusable = !binding.inputValidator.inputOverlayVisible
            inputValidator.input.isFocusable = false
            onInflated(inputValidator.inputOverlay!!)
        }
    }

    override fun setValidatorRaw(validator: MinterPublicKey) {
        binding.inputValidator.inputOverlayVisible = false
        binding.inputValidator.setText(
                validator.toString()
        )
    }

    override fun clearValidatorInput() {
        binding.inputValidator.setText(null)
    }

    override fun setValidatorClearSuffix(listener: (View) -> Unit) {
        binding.inputValidator.setSuffixImageSrc(R.drawable.ic_cancel_circle_grey)
        binding.inputValidator.setOnSuffixImageClickListener(listener)
    }

    override fun setValidatorSelectSuffix(listener: (View) -> Unit) {
        binding.inputValidator.setSuffixImageSrc(R.drawable.ic_validator_list)
        binding.inputValidator.setOnSuffixImageClickListener(listener)
    }

    override fun setOnValidatorSelectListener(onClick: (View) -> Unit) {
        binding.inputValidator.isEnabled = true
        binding.inputValidator.setOnSuffixImageClickListener(onClick)
    }

    override fun setValidatorSelectDisabled() {
        binding.inputValidator.isEnabled = false
        binding.inputValidator.setSuffixType(InputField.SuffixType.None)
    }

    override fun setOnValidatorOverlayClickListener(listener: (View) -> Unit) {
        binding.inputValidator.input.setOnClickListener {
            if (binding.inputValidator.inputOverlayVisible) {
                binding.inputValidator.inputOverlayVisible = false
            }
            listener(it)
        }
    }

    override fun setOnValidatorSelectListener(listener: View.OnClickListener) {
        // both cases - open validators dialog by clicking on dropdown icon
        binding.inputValidator.setOnSuffixImageClickListener(listener)

        // and only if unbonding,
        if (type == Type.Unbond) {
            binding.inputValidator.input.setOnClickListener(listener)
        }
    }

    override fun setOnAccountSelectListener(listener: View.OnClickListener) {
        binding.inputCoin.input.setOnClickListener(listener)
        binding.inputCoin.setOnSuffixImageClickListener(listener)
    }

    override fun setEnableValidator(enable: Boolean) {
        binding.inputValidator.isEnabled = enable
        binding.inputValidator.input.isFocusable = enable
        binding.inputValidator.input.isFocusableInTouchMode = enable
    }

    override fun startValidatorSelector(requestCode: Int, filter: ValidatorSelectorActivity.Filter) {
        getValidatorLauncher.launch(GetValidatorOptions(filter))
    }

    override fun startAccountSelector(items: List<SelectorData<BaseCoinValue>>, listener: (SelectorData<BaseCoinValue>) -> Unit) {
        startDialog {
            SelectorDialog.Builder<BaseCoinValue>(this, R.string.dialog_title_choose_coin)
                    .setItems(items)
                    .setOnClickListener(listener)
                    .create()
        }
    }

    override fun onStop() {
        super.onStop()
        autocomplete?.dismissPopup()
    }

    override fun setAccountTitle(accountName: CharSequence?) {
        binding.inputCoin.postApply { it.setText(accountName) }
    }

    override fun onError(err: CharSequence?) {
        binding.textError.postApply {
            T.isVisible = v = err != null
            it.text = err
        }
    }

    override fun setFee(fee: CharSequence) {
        binding.feeValue.postApply { it.text = fee }
    }

    override fun setOnClickUseMax(listener: View.OnClickListener) {
        binding.inputAmount.setOnSuffixTextClickListener(listener)
    }

    override fun setAmount(amount: String) {
        binding.inputAmount.setText(amount)
    }

    override fun setSubtitle(resId: Int) {
        binding.subtitle.setText(resId)
    }

    override fun setTitle(resId: Int) {
        binding.toolbarTitle.setText(resId)
    }

    override fun setOnSubmitListener(listener: View.OnClickListener) {
        binding.action.setOnClickListener(listener)
    }

    override fun addValidatorTextChangeListener(textWatcher: TextWatcher) {
        binding.inputValidator.addTextChangedListener(textWatcher)
    }

    override fun setValidatorError(message: CharSequence?) {
        binding.inputValidator.error = message
        binding.inputValidator.errorEnabled = !message.isNullOrEmpty()
    }

    override fun setCoinLabel(labelRes: Int) {
        binding.inputCoin.label = getString(labelRes)
    }

    override fun setValidatorsAutocomplete(items: List<ValidatorItem>, listener: (ValidatorItem) -> Unit) {
        onAutocompleteItemSelect = listener
        autocompletePresenter?.setItems(items)
    }

    override fun setMaxAmountValidator(@StringRes errorMessage: Int, coinSupplier: () -> BaseCoinValue?) {
        inputGroup.addValidator(binding.inputAmount, CustomValidator {
            if (it.isNullOrEmpty() || coinSupplier() == null) {
                return@CustomValidator true
            }

            val num = it.parseBigDecimal()
            num <= coinSupplier()!!.amount
        }.apply { this.errorMessage = getString(errorMessage) })
    }

    override fun startExplorer(txHash: MinterHash) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Wallet.urlExplorerFront() + "/transactions/" + txHash.toString())))
    }

    override fun finishSuccess() {
        setResult(Activity.RESULT_OK)
        finish()
    }

    class Builder : ActivityBuilder {
        private var publicKey: MinterPublicKey? = null
        private var coin: CoinItemBase? = null
        private var type: Type = Type.Delegate

        constructor(from: Activity, type: Type) : super(from) {
            this.type = type
        }

        constructor(from: Fragment, type: Type) : super(from) {
            this.type = type
        }

        constructor(from: Service, type: Type) : super(from) {
            this.type = type
        }

        override fun getActivityClass(): Class<*> {
            return DelegateUnbondActivity::class.java
        }

        fun setPublicKey(pubKey: MinterPublicKey): Builder {
            publicKey = pubKey
            return this
        }

        fun setSelectedCoin(coin: CoinItemBase): Builder {
            this.coin = coin
            return this
        }

        override fun onBeforeStart(intent: Intent) {
            super.onBeforeStart(intent)
            intent.putExtra(ARG_TYPE, type)
            if (publicKey != null) {
                intent.putExtra(ARG_PUB_KEY, publicKey)
            }
            if (coin != null) {
                intent.putExtra(ARG_COIN, Parcels.wrap(coin))
            }

        }
    }

    override fun finishCancel() {
        finish()
    }

}
