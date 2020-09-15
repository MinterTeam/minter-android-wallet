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

package network.minter.bipwallet.wallets.dialogs.presentation;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import javax.inject.Inject;

import moxy.InjectViewState;
import network.minter.bipwallet.auth.contract.SignInMnemonicView;
import network.minter.bipwallet.internal.auth.AuthSession;
import network.minter.bipwallet.internal.mvp.MvpBasePresenter;
import network.minter.bipwallet.internal.storage.SecretStorage;
import network.minter.bipwallet.internal.system.SimpleTextWatcher;
import network.minter.core.bip39.NativeBip39;
import network.minter.core.crypto.MinterAddress;

@InjectViewState
public class SingInMnemonicPresenter extends MvpBasePresenter<SignInMnemonicView> {

    @Inject SecretStorage secretStorage;
    @Inject AuthSession session;
    private String mPhrase;

    @Inject
    public SingInMnemonicPresenter() {

    }

    @Override
    public void attachView(SignInMnemonicView view) {
        super.attachView(view);

    }

    @Override
    protected void onFirstViewAttach() {
        super.onFirstViewAttach();
        getViewState().addMnemonicInputTextWatcher(onInputMnemonic());
        getViewState().setOnSubmitClickListener(this::onSubmit);
    }

    private void onSubmit(View view) {
        MinterAddress address = secretStorage.add(mPhrase);
        session.login(
                AuthSession.AUTH_TOKEN_ADVANCED,
                AuthSession.AuthType.Advanced
        );
        getViewState().startHome();
    }

    private TextWatcher onInputMnemonic() {
        return new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                String res = s.toString();
                if (res.isEmpty()) {
                    getViewState().setError("Mnemonic can't be empty");
                    getViewState().setEnableSubmit(false);
                    return;
                }

                mPhrase = res;
                getViewState().setError(null);
                getViewState().setEnableSubmit(NativeBip39.validateMnemonic(mPhrase, NativeBip39.LANG_DEFAULT));
            }
        };
    }
}
