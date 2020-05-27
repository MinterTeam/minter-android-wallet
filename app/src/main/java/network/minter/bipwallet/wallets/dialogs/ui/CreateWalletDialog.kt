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

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import com.edwardstock.inputfield.form.InputGroup
import com.edwardstock.inputfield.form.InputWrapper
import com.edwardstock.inputfield.form.validators.RegexValidator
import dagger.android.support.AndroidSupportInjection
import moxy.presenter.InjectPresenter
import moxy.presenter.ProvidePresenter
import network.minter.bipwallet.R
import network.minter.bipwallet.auth.contract.CreateWalletView
import network.minter.bipwallet.databinding.DialogCreateWalletBinding
import network.minter.bipwallet.home.ui.HomeActivity
import network.minter.bipwallet.internal.dialogs.ActionListener
import network.minter.bipwallet.internal.dialogs.BaseBottomSheetDialogFragment
import network.minter.bipwallet.internal.helpers.KeyboardHelper
import network.minter.bipwallet.internal.helpers.ViewExtensions.visible
import network.minter.bipwallet.internal.helpers.forms.validators.TitleInputFilter
import network.minter.bipwallet.internal.helpers.forms.validators.UniqueWalletTitleValidator
import network.minter.bipwallet.internal.views.list.ViewElevationOnScrollNestedScrollView
import network.minter.bipwallet.wallets.dialogs.presentation.CreateWalletPresenter
import network.minter.bipwallet.wallets.selector.WalletItem
import javax.inject.Inject
import javax.inject.Provider

class CreateWalletDialog : BaseBottomSheetDialogFragment(), CreateWalletView {

    companion object {
        const val EXTRA_ENABLE_TITLE_INPUT = "EXTRA_ENABLE_TITLE_INPUT"
        const val EXTRA_ENABLE_DESCRIPTION = "EXTRA_ENABLE_DESCRIPTION"
        const val EXTRA_ENABLE_START_HOME_ON_SUBMIT = "EXTRA_ENABLE_START_HOME_ON_SUBMIT"
        const val EXTRA_ENABLE_CANCEL = "EXTRA_ENABLE_CANCEL"
        const val EXTRA_WALLET_TITLE = "EXTRA_WALLET_TITLE"
        const val EXTRA_DIALOG_TITLE = "EXTRA_TITLE"
    }

    @Inject lateinit var presenterProvider: Provider<CreateWalletPresenter>
    @InjectPresenter lateinit var presenter: CreateWalletPresenter

    private lateinit var binding: DialogCreateWalletBinding
    private val inputGroup = InputGroup()

    var onAddListener: ((WalletItem) -> Unit)? = null

    @ProvidePresenter
    fun providePresenter(): CreateWalletPresenter {
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DialogCreateWalletBinding.inflate(inflater, container, false)

        presenter.handleExtras(arguments)

        binding.apply {
            scroll.setOnScrollChangeListener(ViewElevationOnScrollNestedScrollView(dialogTop))

            inputGroup.addInput(inputTitle)
            inputGroup.addFilter(inputTitle, TitleInputFilter())
            inputGroup.addValidator(binding.inputTitle, RegexValidator("^[^\\s].*$").apply {
                errorMessage = "Title can't starts from whitespace"
                isRequired = false
            })
            inputGroup.addValidator(inputTitle, UniqueWalletTitleValidator())
        }

        return binding.root
    }

    override fun setDialogTitle(resId: Int) {
        binding.dialogTitle.setText(resId)
    }

    override fun setDialogTitle(title: String?) {
        binding.dialogTitle.text = title
    }

    override fun setDescription(resId: Int) {
        binding.dialogDescription.setText(resId)
    }

    override fun setSeed(seedPhrase: CharSequence) {
        binding.seed.text = seedPhrase
    }

    override fun setOnSeedClickListener(listener: View.OnClickListener) {
        binding.layoutSeed.setOnClickListener(listener)
    }

    override fun setOnSubmit(listener: View.OnClickListener) {
        binding.submit.setOnClickListener { v: View? ->
            listener.onClick(v)
            if (onSubmitListener != null) {
                onSubmitListener!!.invoke()
            }
        }
    }

    override fun showCopiedAlert() {
        val set = AnimatorInflater.loadAnimator(context, R.animator.fade_in_out) as AnimatorSet
        set.setTarget(binding.layoutSeedAlert)
        set.start()
    }

    override fun setSubmitEnabled(enabled: Boolean) {
        binding.submit.isEnabled = enabled
    }

    override fun startHome() {
        KeyboardHelper.hideKeyboard(this)
        if (activity == null) {
            return
        }
        val intent = Intent(activity, HomeActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_CLEAR_TASK or
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
        activity!!.startActivity(intent)
        activity!!.finish()
    }

    override fun close() {
        dismiss()
    }

    override fun setOnSavedClickListener(checkedChangeListener: CompoundButton.OnCheckedChangeListener) {
        binding.actionSavedSeed.setOnCheckedChangeListener(checkedChangeListener)
    }

    override fun addInputTextWatcher(listener: (InputWrapper, Boolean) -> Unit) {
        inputGroup.addTextChangedListener(listener)
    }

    override fun setEnableTitleInput(enable: Boolean) {
        binding.inputTitle.visible = enable
    }

    override fun setEnableDescription(enable: Boolean) {
        binding.dialogDescription.visible = enable
    }

    override fun setWalletTitle(title: String?) {
        binding.inputTitle.setText(title)
    }

    override fun callOnAdd(walletItem: WalletItem) {
        onAddListener?.invoke(walletItem)
    }

    override fun showCancelAction(show: Boolean) {
        binding.actionCancel.visibility = View.VISIBLE
        binding.actionCancel.setOnClickListener { dismiss() }
    }

    class Builder {
        private val args = Bundle()
        private var onSubmitListener: ActionListener? = null
        private var onDismissListener: ActionListener? = null
        private var onAddListener: ((WalletItem) -> Unit)? = null
        fun setOnSubmitListener(listener: ActionListener?): Builder {
            onSubmitListener = listener
            return this
        }

        fun setOnDismissListener(listener: ActionListener?): Builder {
            onDismissListener = listener
            return this
        }

        fun setOnAddListener(onAdd: ((WalletItem) -> Unit)?): Builder {
            onAddListener = onAdd
            return this
        }

        fun setEnableTitleInput(enable: Boolean): Builder {
            args.putBoolean(EXTRA_ENABLE_TITLE_INPUT, enable)
            return this
        }

        fun setWalletTitle(title: String?): Builder {
            args.putString(EXTRA_WALLET_TITLE, title)
            return this
        }

        fun setTitle(title: String): Builder {
            args.putString(EXTRA_DIALOG_TITLE, title)
            return this
        }

        fun setEnableDescription(enable: Boolean): Builder {
            args.putBoolean(EXTRA_ENABLE_DESCRIPTION, enable)
            return this
        }

        fun setEnableStartHomeOnSubmit(enable: Boolean): Builder {
            args.putBoolean(EXTRA_ENABLE_START_HOME_ON_SUBMIT, enable)
            return this
        }

        fun build(): CreateWalletDialog {
            val dialog = CreateWalletDialog()
            dialog.arguments = args
            dialog.onAddListener = onAddListener
            dialog.onSubmitListener = onSubmitListener
            dialog.onDismissListener = onDismissListener
            return dialog
        }
    }


}