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
package network.minter.bipwallet.wallets.dialogs.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.edwardstock.inputfield.form.InputGroup
import com.edwardstock.inputfield.form.InputWrapper
import com.edwardstock.inputfield.form.validators.RegexValidator
import dagger.android.support.AndroidSupportInjection
import moxy.presenter.InjectPresenter
import moxy.presenter.ProvidePresenter
import network.minter.bipwallet.R
import network.minter.bipwallet.databinding.DialogAddWalletBinding
import network.minter.bipwallet.internal.dialogs.ActionListener
import network.minter.bipwallet.internal.dialogs.BaseBottomSheetDialogFragment
import network.minter.bipwallet.internal.helpers.forms.validators.IsMnemonicValidator
import network.minter.bipwallet.internal.helpers.forms.validators.TitleInputFilter
import network.minter.bipwallet.internal.helpers.forms.validators.UniqueMnemonicValidator
import network.minter.bipwallet.internal.helpers.forms.validators.UniqueWalletTitleValidator
import network.minter.bipwallet.internal.storage.SecretStorage
import network.minter.bipwallet.internal.views.list.ViewElevationOnScrollNestedScrollView
import network.minter.bipwallet.wallets.contract.AddWalletView
import network.minter.bipwallet.wallets.dialogs.presentation.AddWalletPresenter
import network.minter.bipwallet.wallets.selector.WalletItem
import javax.inject.Inject
import javax.inject.Provider

typealias OnGenerateNewWalletListener = (submitListener: (WalletItem) -> Unit, dismissListener: ActionListener?, title: String?) -> Unit

class AddWalletDialog : BaseBottomSheetDialogFragment(), AddWalletView {
    @Inject lateinit var presenterProvider: Provider<AddWalletPresenter>
    @InjectPresenter lateinit var presenter: AddWalletPresenter
    @Inject lateinit var secretStorage: SecretStorage

    private var mOnGenerateNewWalletListener: OnGenerateNewWalletListener? = null
    private var inputGroup = InputGroup()
    private lateinit var binding: DialogAddWalletBinding
    private var childDialog: BaseBottomSheetDialogFragment? = null
    var onAddListener: ((WalletItem) -> Unit)? = null

    fun setOnGenerateNewWalletListener(listener: OnGenerateNewWalletListener) {
        mOnGenerateNewWalletListener = listener
    }

    override fun setInputError(fieldName: String, error: CharSequence?) {
        inputGroup.setError(fieldName, error)
    }

    @ProvidePresenter
    fun providePresenter(): AddWalletPresenter {
        return presenterProvider.get()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidSupportInjection.inject(this)
        super.onCreate(savedInstanceState)
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun setOnGenerateClickListener(listener: View.OnClickListener) {
        binding.actionGenerate.setOnClickListener(listener)
    }

    override fun startGenerate() {
        if (fragmentManager == null) return

        collapse()
        childDialog = CreateWalletDialog.Builder()
                .setTitle(getString(R.string.dialog_title_generate_new_wallet))
                .setEnableDescription(true)
                .setEnableTitleInput(true)
                .setEnableStartHomeOnSubmit(false)
                .setWalletTitle(binding.inputTitle.text.toString())
                .setOnAddListener(onAddListener)
                .setOnSubmitListener {
                    onSubmitListener?.invoke()
                    dismiss()
                }
                .setOnDismissListener {
                    onDismissListener?.invoke()
                    expand()
                }
                .build()
                .fixNestedDialogBackgrounds()

        childDialog!!.show(fragmentManager!!, "wallet_generate")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DialogAddWalletBinding.inflate(inflater, container, false)

        binding.apply {

            scroll.setOnScrollChangeListener(ViewElevationOnScrollNestedScrollView(dialogTop))

            inputGroup.addInput(inputSeed)
            inputGroup.addValidator(inputSeed, IsMnemonicValidator("Invalid mnemonic"))
            inputGroup.addValidator(inputSeed, UniqueMnemonicValidator())

            inputGroup.addInput(inputTitle)
            inputGroup.addFilter(inputTitle, TitleInputFilter())
            inputGroup.addValidator(inputTitle, RegexValidator("^[^\\s](.{0,18})$").apply {
                errorMessage = "Invalid title format"
                isRequired = false
            })
            inputGroup.addValidator(inputTitle, UniqueWalletTitleValidator())
        }
        return binding.root
    }

    override fun addTextChangedListener(listener: (InputWrapper, Boolean) -> Unit) {
        inputGroup.addTextChangedListener(listener)
    }

    override fun addFormValidListener(listener: (Boolean) -> Unit) {
        inputGroup.addFormValidateListener(listener)
    }

    override fun callOnAdd(wallet: WalletItem) {
        onAddListener?.invoke(wallet)
    }

    override fun setOnSubmitClickListener(listener: View.OnClickListener) {
        binding.actionSubmit.setOnClickListener { v: View? ->
            listener.onClick(v)
            if (onSubmitListener != null) {
                onSubmitListener!!.invoke()
            }
        }
    }

    override fun setSubmitEnabled(enabled: Boolean) {
        binding.actionSubmit.isEnabled = enabled
    }

    override fun close() {
        dismiss()
    }

    override fun setEnableSubmit(enable: Boolean) {
        binding.actionSubmit.isEnabled = enable
    }

    companion object {
        fun newInstance(): AddWalletDialog {
            return AddWalletDialog()
        }
    }
}