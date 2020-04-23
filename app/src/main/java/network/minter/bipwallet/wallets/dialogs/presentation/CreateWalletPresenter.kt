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
package network.minter.bipwallet.wallets.dialogs.presentation

import android.os.Bundle
import android.text.Editable
import android.view.View
import android.widget.CompoundButton
import moxy.InjectViewState
import network.minter.bipwallet.R
import network.minter.bipwallet.auth.contract.CreateWalletView
import network.minter.bipwallet.internal.auth.AuthSession
import network.minter.bipwallet.internal.di.annotations.FragmentScope
import network.minter.bipwallet.internal.helpers.ContextHelper
import network.minter.bipwallet.internal.mvp.MvpBasePresenter
import network.minter.bipwallet.internal.storage.SecretStorage
import network.minter.bipwallet.internal.system.SimpleTextWatcher
import network.minter.bipwallet.wallets.dialogs.ui.CreateWalletDialog
import network.minter.core.bip39.MnemonicResult
import network.minter.core.bip39.NativeBip39
import network.minter.profile.models.User
import java.security.SecureRandom
import javax.inject.Inject

@FragmentScope
@InjectViewState
class CreateWalletPresenter @Inject constructor() : MvpBasePresenter<CreateWalletView>() {
    @Inject lateinit var session: AuthSession
    @Inject lateinit var secretStorage: SecretStorage

    private val mRandom = SecureRandom()
    private var mMnemonicResult: MnemonicResult? = null
    private var mEnableDescription = true
    private var mEnableTitleInput = false
    private var mStartHomeOnSubmit = true
    private var mTitle: String? = null

    override fun handleExtras(bundle: Bundle?) {
        super.handleExtras(bundle)
        mEnableDescription = bundle!!.getBoolean(CreateWalletDialog.EXTRA_ENABLE_DESCRIPTION, true)
        viewState.setEnableDescription(mEnableDescription)
        mEnableTitleInput = bundle.getBoolean(CreateWalletDialog.EXTRA_ENABLE_TITLE_INPUT, false)
        viewState.setEnableTitleInput(mEnableTitleInput)
        mTitle = bundle.getString(CreateWalletDialog.EXTRA_TITLE, null)
        viewState.setWalletTitle(mTitle)
        if (bundle.getBoolean(CreateWalletDialog.EXTRA_ENABLE_CANCEL, false)) {
            viewState.showCancelAction(true)
        }
        mStartHomeOnSubmit = bundle.getBoolean(CreateWalletDialog.EXTRA_ENABLE_START_HOME_ON_SUBMIT, true)
        if (mEnableTitleInput) {
            viewState.addInputTextWatcher(object : SimpleTextWatcher() {
                override fun afterTextChanged(s: Editable) {
                    super.afterTextChanged(s)

                    if (s.toString().isNotEmpty()) {
                        mTitle = s.toString()
                    }
                }
            })
        }
    }

    override fun attachView(view: CreateWalletView) {
        super.attachView(view)
        viewState.setTitle(R.string.btn_create_wallet)
        viewState.setDescription(R.string.hint_save_seed)
        viewState.setSeed(mMnemonicResult!!.mnemonic)
        viewState.setOnSeedClickListener(View.OnClickListener { onCopySeed(it) })
        viewState.setOnSavedClickListener(CompoundButton.OnCheckedChangeListener { compoundButton: CompoundButton, checked: Boolean ->
            onSavedSeed(compoundButton, checked)
        })
        viewState.setOnSubmit(View.OnClickListener { onSubmit(it) })
    }

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()
        mMnemonicResult = NativeBip39.encodeBytes(mRandom.generateSeed(16))
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onSubmit(view: View) {
        session.login(
                AuthSession.AUTH_TOKEN_ADVANCED,
                User(AuthSession.AUTH_TOKEN_ADVANCED),
                AuthSession.AuthType.Advanced
        )
        secretStorage.add(mMnemonicResult!!, mTitle)
        if (mStartHomeOnSubmit) {
            viewState.startHome()
        } else {
            viewState.close()
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onSavedSeed(compoundButton: CompoundButton, checked: Boolean) {
        viewState.setSubmitEnabled(checked)
    }

    private fun onCopySeed(view: View) {
        viewState.showCopiedAlert()
        ContextHelper.copyToClipboardNoAlert(view.context, mMnemonicResult!!.mnemonic)
    }
}