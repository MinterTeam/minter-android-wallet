/*
 * Copyright (C) 2018 by MinterTeam
 * @link https://github.com/MinterTeam
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

package network.minter.bipwallet.advanced.views;

import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;

import com.arellomobile.mvp.InjectViewState;

import javax.inject.Inject;

import network.minter.bipwallet.advanced.AdvancedModeModule;
import network.minter.bipwallet.advanced.repo.SecretStorage;
import network.minter.bipwallet.advanced.ui.AdvancedMainActivity;
import network.minter.bipwallet.internal.auth.AuthSession;
import network.minter.bipwallet.internal.mvp.MvpBasePresenter;
import network.minter.mintercore.bip39.NativeBip39;
import network.minter.mintercore.crypto.MinterAddress;
import network.minter.my.models.User;
import network.minter.my.repo.MyAddressRepository;

import static network.minter.bipwallet.internal.ReactiveAdapter.rxCallMy;

/**
 * MinterWallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@InjectViewState
public class AdvancedMainPresenter extends MvpBasePresenter<AdvancedModeModule.MainView> {

    private final static int REQUEST_GENERATE = 200;

    @Inject SecretStorage repo;
    @Inject AuthSession session;
    @Inject MyAddressRepository addressRepo;
    private String mPhrase;
    private boolean mForResult;

    @Inject
    public AdvancedMainPresenter() {

    }

    @Override
    public void attachView(AdvancedModeModule.MainView view) {
        super.attachView(view);

        getViewState().setMnemonicTextChangedListener(onTextChanged());
        getViewState().setOnActivateMnemonic(v -> {
            if (mPhrase == null || mPhrase.isEmpty()) {
                getViewState().setError("Empty phrase");
                return;
            }

            boolean valid = NativeBip39.validateMnemonic(mPhrase, NativeBip39.LANG_DEFAULT);
            if (!valid) {
                getViewState().setError("Phrase is not valid");
                return;
            }

            MinterAddress address = repo.add(mPhrase);

            if (session.isLoggedIn()) {
                if (session.getRole() == AuthSession.AuthType.Basic) {
                    // if basic user, we adding address to local repo and to server
                    // here we ask password to encrypt seed and send to server, and then finishing with success result
                    getViewState().askPassword((field, val) -> onPasswordConfirmed(field, val, address));
                } else {
                    // if adding address in advanced mode, finishing with success result
                    if (mForResult) {
                        getViewState().finishSuccess();
                        return;
                    }

                    // here we already added seed to local repo, added first address, staring home
                    getViewState().startHome();
                }
            } else {
                session.login(
                        AuthSession.AUTH_TOKEN_ADVANCED,
                        new User(AuthSession.AUTH_TOKEN_ADVANCED),
                        AuthSession.AuthType.Advanced
                );
                getViewState().startHome();
            }
        });

        if (mForResult) {
            getViewState().setOnGenerate(v -> getViewState().startGenerate(REQUEST_GENERATE));
        } else {
            getViewState().setOnGenerate(v -> getViewState().startGenerate());
        }
    }

    @Override
    public void handleExtras(Intent intent) {
        super.handleExtras(intent);
        final CharSequence title = intent.getCharSequenceExtra(AdvancedMainActivity.EXTRA_TITLE);
        if (title != null) {
            getViewState().setTitle(title);
        }

        mForResult = intent.getBooleanExtra(AdvancedMainActivity.EXTRA_FOR_RESULT, false);
    }

    private boolean onPasswordConfirmed(String fieldName, String value, final MinterAddress address) {
        getViewState().showProgress(null, "Encrypting...");
        safeSubscribeIoToUi(
                rxCallMy(addressRepo.addAddress(repo.getSecret(address).toAddressData(repo.getAddresses().isEmpty(), true, value)))
        )
                .retryWhen(getErrorResolver())
                .subscribe(res -> {
                    getViewState().hideProgress();
                    if (res.isSuccess()) {
                        if (mForResult) {
                            getViewState().finishSuccess();
                        } else {
                            // is this a real case?
                            getViewState().startHome();
                        }
                    }
                });
        return true;
    }

    private TextWatcher onTextChanged() {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String res = s.toString();
                if (res.isEmpty()) {
                    getViewState().setError(null);
                }

                mPhrase = res;
            }
        };
    }

}
