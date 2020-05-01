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
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import dagger.android.support.AndroidSupportInjection
import moxy.presenter.InjectPresenter
import moxy.presenter.ProvidePresenter
import network.minter.bipwallet.R
import network.minter.bipwallet.databinding.DialogEditWalletBinding
import network.minter.bipwallet.internal.dialogs.BaseBottomSheetDialogFragment
import network.minter.bipwallet.internal.dialogs.DialogExecutor
import network.minter.bipwallet.internal.helpers.ViewExtensions.visible
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

    override fun startDialog(executor: DialogExecutor) {
        collapse()
        super.startDialog {
            val d = executor(it)
            d.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            d.window?.setWindowAnimations(0)
            d
        }
    }

    override fun expand() {
        super.expand(false)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DialogEditWalletBinding.inflate(inflater, container, false)

        binding.dialogTop.dialogTitle.setText(R.string.title_edit_title)
        binding.dialogTop.dialogDescription.visible = false

        if (arguments == null) {
            throw IllegalStateException("Don't show this dialog without newInstance method")
        }
        presenter.handleExtras(arguments)

        return binding.root
    }

    override fun setEnableRemove(enableRemove: Boolean) {
        binding.actionDelete.visible = enableRemove
    }

    override fun setEnableSubmit(enable: Boolean) {
        binding.submit.isEnabled = enable
    }

    override fun setOnSubmitClickListener(listener: View.OnClickListener) {
        binding.submit.setOnClickListener {
            listener.onClick(it)
        }
    }

    override fun addInputTextWatcher(textWatcher: TextWatcher) {
        binding.inputTitle.addTextChangedListener(textWatcher)
    }

    override fun setError(error: CharSequence?) {
        binding.errorText.text = error
    }

    override fun close() {
        dismiss()
    }

    override fun setTitle(title: CharSequence?) {
        binding.inputTitle.setText(title)
    }

    override fun setDeleteActionVisible(visible: Boolean) {
        binding.actionDelete.visible = visible
    }

    override fun callOnDelete(walletItem: WalletItem) {
        onDeleteListener?.invoke(walletItem)
    }

    override fun callOnSave(walletItem: WalletItem) {
        onSaveListener?.invoke(walletItem)
    }

    override fun setOnDeleteClickListener(listener: View.OnClickListener, walletItem: WalletItem) {
        binding.actionDelete.setOnClickListener { v: View? ->
            listener.onClick(v)
        }
    }
}