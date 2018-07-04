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

import android.content.Context;
import android.content.Intent;

import com.arellomobile.mvp.InjectViewState;

import java.security.SecureRandom;

import javax.inject.Inject;

import network.minter.bipwallet.R;
import network.minter.bipwallet.advanced.AdvancedModeModule;
import network.minter.bipwallet.advanced.repo.SecretStorage;
import network.minter.bipwallet.advanced.ui.AdvancedGenerateActivity;
import network.minter.bipwallet.internal.auth.AuthSession;
import network.minter.bipwallet.internal.mvp.MvpBasePresenter;
import network.minter.mintercore.bip39.MnemonicResult;
import network.minter.mintercore.bip39.NativeBip39;
import network.minter.mintercore.crypto.MinterAddress;
import network.minter.my.models.User;
import network.minter.my.repo.MyAddressRepository;

import static network.minter.bipwallet.internal.ReactiveAdapter.rxCallMy;
import static network.minter.bipwallet.internal.helpers.ContextHelper.copyToClipboard;

/**
 * MinterWallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@InjectViewState
public class AdvancedGeneratePresenter extends MvpBasePresenter<AdvancedModeModule.GenerateView> {

    @Inject Context context;
    @Inject SecretStorage repo;
    @Inject AuthSession session;
    @Inject MyAddressRepository addressRepo;

    private SecureRandom mRandom = new SecureRandom();
    private MnemonicResult mMnemonicResult;
    private boolean mForResult = false;
    private boolean mSaveOnServer = false;

    @Inject
    public AdvancedGeneratePresenter() {
    }

    @Override
    public void handleExtras(Intent intent) {
        super.handleExtras(intent);
        mForResult = intent.getBooleanExtra(AdvancedGenerateActivity.EXTRA_FOR_RESULT, false);
    }

    @Override
    public void attachView(AdvancedModeModule.GenerateView view) {
        super.attachView(view);

        // if basic user, give secure variant selector
        getViewState().setEnableSecureVariants(session.getRole() == AuthSession.AuthType.Basic, this::onSelectSecureVariant);
        // default value for basic user
        mSaveOnServer = session.getRole() == AuthSession.AuthType.Basic;

        mMnemonicResult = NativeBip39.encodeBytes(mRandom.generateSeed(16));
        getViewState().setMnemonic(mMnemonicResult.getMnemonic());

        getViewState().setEnableCopy(true);
        getViewState().setOnCopy(v -> copyToClipboard(context, mMnemonicResult.getMnemonic()));

        getViewState().setOnSwitchedConfirm((buttonView, isChecked) -> getViewState().setEnableLaunch(isChecked));

        getViewState().setOnActionClick(v -> {

            MinterAddress address = repo.add(mMnemonicResult);

            if (mSaveOnServer) {
                getViewState().askPassword((field, val) -> saveServerAddress(field, val, address));
                return;
            }

            session.login(
                    AuthSession.AUTH_TOKEN_ADVANCED,
                    new User(AuthSession.AUTH_TOKEN_ADVANCED),
                    AuthSession.AuthType.Advanced
            );

            getViewState().startHome();
        });
    }

    @Override
    protected void onFirstViewAttach() {
        super.onFirstViewAttach();
        getViewState().setEnableLaunch(false);
    }

    private boolean saveServerAddress(String fieldName, String value, MinterAddress address) {
        getViewState().showProgress(null, "Encrypting...");
        safeSubscribeIoToUi(rxCallMy(addressRepo.addAddress(repo.getSecret(address).toAddressData(repo.getAddresses().isEmpty(), true, value))))
                .subscribe(res -> {
                    getViewState().hideProgress();
                    if (!res.isSuccess()) {
                        getViewState().onError(res.getError().message);
                        return;
                    }

                    if (mForResult) {
                        getViewState().finishSuccess();
                    } else {
                        getViewState().startHome();
                    }
                });
        return true;
    }

    private void onSelectSecureVariant(int menuId) {
        mSaveOnServer = menuId == R.id.securedBy_bip;
    }
}
