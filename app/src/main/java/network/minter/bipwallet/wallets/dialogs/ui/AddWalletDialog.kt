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
import dagger.android.support.AndroidSupportInjection
import moxy.presenter.InjectPresenter
import moxy.presenter.ProvidePresenter
import network.minter.bipwallet.databinding.DialogAddWalletBinding
import network.minter.bipwallet.internal.dialogs.ActionListener
import network.minter.bipwallet.internal.dialogs.BaseBottomSheetDialogFragment
import network.minter.bipwallet.wallets.contract.AddWalletView
import network.minter.bipwallet.wallets.dialogs.presentation.AddWalletPresenter
import javax.inject.Inject
import javax.inject.Provider

typealias OnGenerateNewWalletListener = (submitListener: ActionListener?, dismissListener: ActionListener?, title: String?) -> Unit

class AddWalletDialog : BaseBottomSheetDialogFragment(), AddWalletView {
    @Inject lateinit var presenterProvider: Provider<AddWalletPresenter>
    @InjectPresenter lateinit var presenter: AddWalletPresenter

    private var mOnGenerateNewWalletListener: OnGenerateNewWalletListener? = null
    private lateinit var binding: DialogAddWalletBinding

    fun setOnGenerateNewWalletListener(listener: OnGenerateNewWalletListener) {
        mOnGenerateNewWalletListener = listener
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DialogAddWalletBinding.inflate(inflater, container, false)

        binding.apply {
            actionGenerate.setOnClickListener {
                close()
                if (mOnGenerateNewWalletListener != null) {
                    val s = inputTitle.text
                    val title = if (s != null && s.isNotEmpty()) s.toString() else null
                    mOnGenerateNewWalletListener!!.invoke(onSubmitListener, onDismissListener, title)
                }
            }
        }
        return binding.root
    }

    override fun setOnSubmit(listener: View.OnClickListener) {
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

    override fun addSeedInputTextListener(textWatcher: TextWatcher) {
        binding.inputSeed.addTextChangedListener(textWatcher)
    }

    override fun addTitleInputTextListener(textWatcher: TextWatcher) {
        binding.inputTitle.addTextChangedListener(textWatcher)
    }

    override fun setError(error: CharSequence?) {
        binding.inputSeed.error = error
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