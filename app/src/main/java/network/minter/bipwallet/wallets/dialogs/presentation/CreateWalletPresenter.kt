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
import network.minter.bipwallet.internal.storage.models.AddressBalanceTotal
import network.minter.bipwallet.wallets.dialogs.ui.CreateWalletDialog
import network.minter.bipwallet.wallets.selector.WalletItem
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

    private val randomDev = SecureRandom()
    private var mnemonicResult: MnemonicResult? = null
    private var enableDescription = true
    private var enableTitleInput = false
    private var startHomeOnSubmit = true
    private var walletTitle: String? = null
    private var formValid = true
    private var savedSeed = false

    override fun handleExtras(bundle: Bundle?) {
        super.handleExtras(bundle)
        enableDescription = bundle!!.getBoolean(CreateWalletDialog.EXTRA_ENABLE_DESCRIPTION, true)
        viewState.setEnableDescription(enableDescription)
        enableTitleInput = bundle.getBoolean(CreateWalletDialog.EXTRA_ENABLE_TITLE_INPUT, false)
        viewState.setEnableTitleInput(enableTitleInput)
        walletTitle = bundle.getString(CreateWalletDialog.EXTRA_WALLET_TITLE, null)


        if (bundle.containsKey(CreateWalletDialog.EXTRA_DIALOG_TITLE)) {
            viewState.setDialogTitle(bundle.getString(CreateWalletDialog.EXTRA_DIALOG_TITLE, null))
        } else {
            viewState.setDialogTitle(R.string.btn_create_wallet)
        }

        viewState.setWalletTitle(walletTitle)
        if (bundle.getBoolean(CreateWalletDialog.EXTRA_ENABLE_CANCEL, false)) {
            viewState.showCancelAction(true)
        }
        startHomeOnSubmit = bundle.getBoolean(CreateWalletDialog.EXTRA_ENABLE_START_HOME_ON_SUBMIT, true)
        if (enableTitleInput) {
            viewState.addInputTextWatcher { input, valid ->
                formValid = valid
                viewState.setSubmitEnabled(valid)
                if (valid) {
                    walletTitle = input.text?.toString()

                    if (walletTitle?.trim()?.isEmpty() == true) {
                        walletTitle = null
                    }

                    viewState.setSubmitEnabled(canSubmit)
                }
            }
        }
    }

    override fun attachView(view: CreateWalletView) {
        super.attachView(view)

        viewState.setDescription(R.string.hint_save_seed)
        viewState.setSeed(mnemonicResult!!.mnemonic)
        viewState.setOnSeedClickListener(View.OnClickListener { onCopySeed(it) })
        viewState.setOnSavedClickListener(CompoundButton.OnCheckedChangeListener { compoundButton: CompoundButton, checked: Boolean ->
            onSavedSeed(compoundButton, checked)
        })
        viewState.setOnSubmit(View.OnClickListener { onSubmit(it) })
    }

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()
        mnemonicResult = NativeBip39.encodeBytes(randomDev.generateSeed(16))
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onSubmit(view: View) {
        session.login(
                AuthSession.AUTH_TOKEN_ADVANCED,
                User(AuthSession.AUTH_TOKEN_ADVANCED),
                AuthSession.AuthType.Advanced
        )
        val address = secretStorage.add(mnemonicResult!!, walletTitle)
        if (startHomeOnSubmit) {
            viewState.startHome()
        } else {
            secretStorage.setMain(address)
            viewState.close()
            viewState.callOnAdd(
                    WalletItem.create(secretStorage, AddressBalanceTotal(address))
            )
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onSavedSeed(compoundButton: CompoundButton, checked: Boolean) {
        savedSeed = checked
        viewState.setSubmitEnabled(canSubmit)
    }

    val canSubmit: Boolean
        get() = savedSeed && formValid

    private fun onCopySeed(view: View) {
        viewState.showCopiedAlert()
        ContextHelper.copyToClipboardNoAlert(view.context, mnemonicResult!!.mnemonic)
    }
}