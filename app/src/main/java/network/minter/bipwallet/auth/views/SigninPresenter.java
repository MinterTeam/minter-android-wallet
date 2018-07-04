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

package network.minter.bipwallet.auth.views;

import android.view.View;
import android.widget.EditText;

import com.arellomobile.mvp.InjectViewState;

import javax.inject.Inject;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import network.minter.bipwallet.R;
import network.minter.bipwallet.advanced.repo.SecretStorage;
import network.minter.bipwallet.auth.AuthModule;
import network.minter.bipwallet.auth.ui.InputGroup;
import network.minter.bipwallet.internal.Wallet;
import network.minter.bipwallet.internal.auth.AuthSession;
import network.minter.bipwallet.internal.di.annotations.ActivityScope;
import network.minter.bipwallet.internal.mvp.MvpBasePresenter;
import network.minter.my.models.LoginData;
import network.minter.my.models.MyAddressData;
import network.minter.my.repo.MyAddressRepository;
import network.minter.my.repo.MyAuthRepository;

import static network.minter.bipwallet.internal.ReactiveAdapter.convertToMyErrorResult;
import static network.minter.bipwallet.internal.ReactiveAdapter.rxCallMy;

/**
 * MinterWallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@ActivityScope
@InjectViewState
public class SigninPresenter extends MvpBasePresenter<AuthModule.SigninView> {
    @Inject MyAuthRepository authRepo;
    @Inject SecretStorage secretRepo;
    @Inject AuthSession session;
    @Inject MyAddressRepository addressRepo;

    private LoginData mLoginData = new LoginData();
    private boolean mValid = false;

    @Inject
    public SigninPresenter() {
    }

    @Override
    public void attachView(AuthModule.SigninView view) {
        super.attachView(view);
        getViewState().setOnSubmit(this::onSubmit);
        getViewState().setOnTextChangedListener(new InputGroup.OnTextChangedListener() {
            @Override
            public void onTextChanged(EditText editText, boolean valid) {
                if (editText.getId() == R.id.inputUsername) {
                    mLoginData.username = editText.getText().toString();
                    if (!mLoginData.username.isEmpty() && mLoginData.username.charAt(0) == '@') {
                        mLoginData.username = mLoginData.username.substring(1);
                    }
                } else if (editText.getId() == R.id.inputPassword) {
                    mLoginData.rawPassword = editText.getText().toString();
                }
            }
        });
        getViewState().setOnFormValidateListener(valid -> {
            getViewState().setEnableSubmit(valid);
            mValid = valid;
        });
    }

    private void onSubmit(View view) {
        if (!mValid) {
            return;
        }
        getViewState().hideKeyboard();

        getViewState().setEnableSubmit(false);

        getViewState().clearErrors();
        getViewState().showProgress();

        rxCallMy(authRepo.login(mLoginData.preparePassword()))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .onErrorResumeNext(convertToMyErrorResult())
                .retryWhen(getErrorResolver())
                .subscribe(userResult -> {
                    if (userResult.isSuccess()) {
                        session.login(
                                userResult.data.token.accessToken,
                                userResult.data,
                                AuthSession.AuthType.Basic
                        );
                    } else {
                        getViewState().hideProgress();
                        getViewState().setResultError(userResult.error.message);
                        getViewState().setInputErrors(userResult.getError().getData());
                        getViewState().setEnableSubmit(true);
                        return;
                    }

                    rxCallMy(addressRepo.getAddressesWithEncrypted())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribeOn(Schedulers.io())
                            .onErrorResumeNext(convertToMyErrorResult())
                            .subscribe(addressResult -> {
                                getViewState().hideProgress();
                                getViewState().setEnableSubmit(true);
                                secretRepo.setEncryptionKey(mLoginData.rawPassword);
                                if (addressResult.isSuccess()) {
                                    for (MyAddressData addressData : addressResult.data) {
                                        if(addressData.encrypted != null) {
                                            secretRepo.add(addressData.encrypted.decrypt(secretRepo.getEncryptionKey()));
                                        }
                                    }

                                    getViewState().startHome();
                                } else {
                                    getViewState().setResultError(addressResult.getError().message);
                                    getViewState().setInputErrors(addressResult.getError().getData());
                                }


                            }, Wallet.Rx.errorHandler(getViewState()));


                }, Wallet.Rx.errorHandler(getViewState()));
    }
}
