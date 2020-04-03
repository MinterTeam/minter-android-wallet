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

import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.CompoundButton;

import java.security.SecureRandom;

import javax.inject.Inject;

import moxy.InjectViewState;
import network.minter.bipwallet.R;
import network.minter.bipwallet.advanced.repo.SecretStorage;
import network.minter.bipwallet.auth.contract.CreateWalletView;
import network.minter.bipwallet.internal.auth.AuthSession;
import network.minter.bipwallet.internal.di.annotations.FragmentScope;
import network.minter.bipwallet.internal.mvp.MvpBasePresenter;
import network.minter.bipwallet.internal.system.SimpleTextWatcher;
import network.minter.bipwallet.wallets.dialogs.ui.CreateWalletDialog;
import network.minter.core.bip39.MnemonicResult;
import network.minter.core.bip39.NativeBip39;
import network.minter.profile.models.User;

import static network.minter.bipwallet.internal.helpers.ContextHelper.copyToClipboardNoAlert;

@FragmentScope
@InjectViewState
public class CreateWalletPresenter extends MvpBasePresenter<CreateWalletView> {

    @Inject AuthSession session;
    @Inject SecretStorage secretStorage;

    private SecureRandom mRandom = new SecureRandom();
    private MnemonicResult mMnemonicResult;
    private boolean mEnableDescription = true;
    private boolean mEnableTitleInput = false;
    private boolean mStartHomeOnSubmit = true;
    private String mTitle = null;

    @Inject
    public CreateWalletPresenter() {

    }

    @Override
    public void handleExtras(Bundle bundle) {
        super.handleExtras(bundle);
        mEnableDescription = bundle.getBoolean(CreateWalletDialog.EXTRA_ENABLE_DESCRIPTION, true);
        getViewState().setEnableDescription(mEnableDescription);

        mEnableTitleInput = bundle.getBoolean(CreateWalletDialog.EXTRA_ENABLE_TITLE_INPUT, false);
        getViewState().setEnableTitleInput(mEnableTitleInput);

        mTitle = bundle.getString(CreateWalletDialog.EXTRA_TITLE, null);
        getViewState().setWalletTitle(mTitle);

        mStartHomeOnSubmit = bundle.getBoolean(CreateWalletDialog.EXTRA_ENABLE_START_HOME_ON_SUBMIT, true);

        if (mEnableTitleInput) {
            getViewState().addInputTextWatcher(new SimpleTextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    super.afterTextChanged(s);
                    if (!s.toString().isEmpty()) {
                        mTitle = s.toString();
                    }
                }
            });
        }
    }

    @Override
    public void attachView(CreateWalletView view) {
        super.attachView(view);
        getViewState().setTitle(R.string.btn_create_wallet);
        getViewState().setDescription(R.string.hint_save_seed);
        getViewState().setSeed(mMnemonicResult.getMnemonic());
        getViewState().setOnSeedClickListener(this::onCopySeed);
        getViewState().setOnSavedClickListener(this::onSavedSeed);
        getViewState().setOnSubmit(this::onSubmit);

    }

    @Override
    protected void onFirstViewAttach() {
        super.onFirstViewAttach();
        mMnemonicResult = NativeBip39.encodeBytes(mRandom.generateSeed(16));
    }

    private void onSubmit(View view) {
        session.login(
                AuthSession.AUTH_TOKEN_ADVANCED,
                new User(AuthSession.AUTH_TOKEN_ADVANCED),
                AuthSession.AuthType.Advanced
        );
        secretStorage.add(mMnemonicResult, mTitle);
        if (mStartHomeOnSubmit) {
            getViewState().startHome();
        } else {
            getViewState().close();
        }

    }

    private void onSavedSeed(CompoundButton compoundButton, boolean checked) {
        getViewState().setSubmitEnabled(checked);
    }

    private void onCopySeed(View view) {
        getViewState().showCopiedAlert();
        copyToClipboardNoAlert(view.getContext(), mMnemonicResult.getMnemonic());
    }
}
