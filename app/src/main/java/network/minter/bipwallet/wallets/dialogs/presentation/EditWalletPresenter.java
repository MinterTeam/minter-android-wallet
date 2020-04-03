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

import javax.inject.Inject;

import moxy.InjectViewState;
import network.minter.bipwallet.advanced.models.SecretData;
import network.minter.bipwallet.advanced.repo.SecretStorage;
import network.minter.bipwallet.internal.helpers.IntentHelper;
import network.minter.bipwallet.internal.mvp.MvpBasePresenter;
import network.minter.bipwallet.internal.system.SimpleTextWatcher;
import network.minter.bipwallet.wallets.contract.EditWalletView;
import network.minter.bipwallet.wallets.dialogs.ui.EditWalletDialog;
import network.minter.bipwallet.wallets.selector.WalletItem;

@InjectViewState
public class EditWalletPresenter extends MvpBasePresenter<EditWalletView> {
    @Inject SecretStorage secretStorage;

    private WalletItem mWalletItem;
    private String mTitle;

    @Inject
    public EditWalletPresenter() {

    }

    @Override
    public void handleExtras(Bundle bundle) {
        super.handleExtras(bundle);
        mWalletItem = IntentHelper.getParcelExtraOrError(bundle, EditWalletDialog.EXTRA_WALLET_ITEM, "WalletItem required");

        if (!mWalletItem.getTitle().equals(mWalletItem.getAddressShort())) {
            mTitle = mWalletItem.getTitle();
            getViewState().setTitle(mWalletItem.getTitle());
        }
    }

    @Override
    public void attachView(EditWalletView view) {
        super.attachView(view);

    }

    @Override
    protected void onFirstViewAttach() {
        super.onFirstViewAttach();
        getViewState().setEnableSubmit(true);
        getViewState().setOnSubmitClickListener(this::onSubmit);
        getViewState().setOnDeleteClickListener(this::onDeleteWallet);
        getViewState().addInputTextWatcher(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                super.afterTextChanged(s);
                mTitle = s.toString();
                if (mTitle.isEmpty()) {
                    mTitle = null;
                }
            }
        });
    }

    private void onDeleteWallet(View view) {
        if (mWalletItem != null) {
            secretStorage.delete(mWalletItem.getAddress());
        }
    }

    private void onSubmit(View view) {
        SecretData sd = secretStorage.getSecret(mWalletItem.getAddress());
        sd.setTitle(mTitle);
        secretStorage.update(sd);
        getViewState().close();
    }
}
