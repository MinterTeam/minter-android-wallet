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
import network.minter.bipwallet.databinding.DialogEditWalletBinding
import network.minter.bipwallet.internal.dialogs.BaseBottomSheetDialogFragment
import network.minter.bipwallet.internal.helpers.ViewExtensions.tr
import network.minter.bipwallet.internal.helpers.ViewExtensions.visible
import network.minter.bipwallet.internal.helpers.forms.validators.TitleInputFilter
import network.minter.bipwallet.internal.helpers.forms.validators.UniqueWalletTitleValidator
import network.minter.bipwallet.internal.views.list.ViewElevationOnScrollNestedScrollView
import network.minter.bipwallet.wallets.contract.EditWalletView
import network.minter.bipwallet.wallets.dialogs.presentation.EditWalletPresenter
import network.minter.bipwallet.wallets.selector.WalletItem
import org.parceler.Parcels
import javax.inject.Inject
import javax.inject.Provider

class EditWalletDialog : BaseBottomSheetDialogFragment(), EditWalletView {

    companion object {
        const val EXTRA_WALLET_ITEM = "EXTRA_WALLET_ITEM"
        const val EXTRA_ENABLE_REMOVE = "EXTRA_ENABLE_REMOVE"

        fun newInstance(walletItem: WalletItem, enableRemove: Boolean): EditWalletDialog {
            val dialog = EditWalletDialog()
            val extras = Bundle()
            extras.putParcelable(EXTRA_WALLET_ITEM, Parcels.wrap(walletItem))
            extras.putBoolean(EXTRA_ENABLE_REMOVE, enableRemove)
            dialog.arguments = extras
            return dialog
        }
    }

    @Inject lateinit var presenterProvider: Provider<EditWalletPresenter>
    @InjectPresenter lateinit var presenter: EditWalletPresenter

    private val inputGroup = InputGroup()
    private lateinit var binding: DialogEditWalletBinding

    var onDeleteListener: ((WalletItem) -> Unit)? = null
    var onSaveListener: ((WalletItem) -> Unit)? = null

    @ProvidePresenter
    fun providePresenter(): EditWalletPresenter {
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

    override fun expand() {
        super.expand(false, null)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DialogEditWalletBinding.inflate(inflater, container, false)

        binding.scroll.setOnScrollChangeListener(ViewElevationOnScrollNestedScrollView(binding.dialogTop))

        if (arguments == null) {
            throw IllegalStateException("Don't show this dialog without newInstance method")
        }
        presenter.handleExtras(arguments)

        inputGroup.addInput(binding.inputTitle)
        inputGroup.addFilter(binding.inputTitle, TitleInputFilter())
        inputGroup.addValidator(binding.inputTitle, RegexValidator("^[^\\s].*$").apply {
            errorMessage = tr(R.string.input_validator_title_cant_starts_from_ws)
            isRequired = false
        })

        return binding.root
    }

    override fun setUniqueTitleExclude(title: String?) {
        inputGroup.addValidator(binding.inputTitle, UniqueWalletTitleValidator(title))
    }

    override fun setEnableRemove(enable: Boolean) {
        binding.actionDelete.visible = enable
    }

    override fun setEnableSubmit(enable: Boolean) {
        binding.submit.isEnabled = enable
    }

    override fun setOnSubmitClickListener(listener: View.OnClickListener) {
        binding.submit.setOnClickListener {
            listener.onClick(it)
        }
    }

    override fun addInputTextWatcher(listener: (InputWrapper, Boolean) -> Unit) {
        inputGroup.addTextChangedListener(listener)
    }

    override fun close() {
        dismiss()
    }

    override fun setTitle(title: CharSequence?) {
        binding.inputTitle.setText(title)
    }

    override fun callOnDelete(walletItem: WalletItem) {
        onDeleteListener?.invoke(walletItem)
    }

    override fun callOnSave(walletItem: WalletItem) {
        onSaveListener?.invoke(walletItem)
    }

    override fun setOnRemoveClickListener(listener: View.OnClickListener) {
        binding.actionDelete.setOnClickListener { v: View? ->
            listener.onClick(v)
        }
    }
}