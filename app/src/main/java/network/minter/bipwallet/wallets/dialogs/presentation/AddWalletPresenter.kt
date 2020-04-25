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

import android.text.Editable
import android.view.View
import moxy.InjectViewState
import network.minter.bipwallet.internal.auth.AuthSession
import network.minter.bipwallet.internal.di.annotations.FragmentScope
import network.minter.bipwallet.internal.mvp.MvpBasePresenter
import network.minter.bipwallet.internal.storage.SecretStorage
import network.minter.bipwallet.internal.system.SimpleTextWatcher
import network.minter.bipwallet.wallets.contract.AddWalletView
import network.minter.core.bip39.NativeBip39
import network.minter.profile.models.User
import javax.inject.Inject

@FragmentScope
@InjectViewState
class AddWalletPresenter @Inject constructor() : MvpBasePresenter<AddWalletView>() {
    @Inject lateinit var session: AuthSession
    @Inject lateinit var secretStorage: SecretStorage

    private var mTitle: String? = null
    private var mPhrase: String? = null

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()
        viewState.setOnSubmit(View.OnClickListener { onSubmit() })
        viewState.addTitleInputTextListener(object : SimpleTextWatcher() {
            override fun afterTextChanged(s: Editable) {
                mTitle = s.toString()
                if (mTitle!!.isEmpty()) {
                    mTitle = null
                }
            }
        })
        viewState.addSeedInputTextListener(object : SimpleTextWatcher() {
            override fun afterTextChanged(s: Editable) {
                super.afterTextChanged(s)
                val res = s.toString()
                if (res.isEmpty()) {
                    viewState.setError("Mnemonic can't be empty")
                    viewState.setEnableSubmit(false)
                    return
                }
                mPhrase = res
                viewState.setEnableSubmit(NativeBip39.validateMnemonic(mPhrase, NativeBip39.LANG_DEFAULT))
            }
        })
    }

    private fun onSubmit() {
        secretStorage.add(mPhrase!!, mTitle)
        session.login(
                AuthSession.AUTH_TOKEN_ADVANCED,
                User(AuthSession.AUTH_TOKEN_ADVANCED),
                AuthSession.AuthType.Advanced
        )
        viewState.close()
    }
}