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

package network.minter.bipwallet.auth.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import javax.inject.Inject;
import javax.inject.Provider;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import butterknife.BindView;
import butterknife.ButterKnife;
import moxy.presenter.InjectPresenter;
import moxy.presenter.ProvidePresenter;
import network.minter.bipwallet.R;
import network.minter.bipwallet.auth.contract.AuthView;
import network.minter.bipwallet.auth.views.AuthPresenter;
import network.minter.bipwallet.internal.BaseInjectFragment;
import network.minter.bipwallet.internal.helpers.IntentHelper;
import network.minter.bipwallet.wallets.dialogs.ui.CreateWalletDialog;
import network.minter.bipwallet.wallets.dialogs.ui.SignInMnemonicDialog;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class AuthFragment extends BaseInjectFragment implements AuthView {
    @Inject Provider<AuthPresenter> authPresenterProvider;
    @InjectPresenter AuthPresenter presenter;
    @BindView(R.id.action_signin) Button actionSignIn;
    @BindView(R.id.action_create_wallet) Button actionCreate;
    @BindView(R.id.action_help) Button actionHelp;
    @BindView(R.id.logo) ImageView logo;
    private BottomSheetDialogFragment mDialog;

    @Override
    public void setOnClickSignIn(View.OnClickListener listener) {
        actionSignIn.setOnClickListener(listener);
    }

    @Override
    public void setOnClickCreateWallet(View.OnClickListener listener) {
        actionCreate.setOnClickListener(listener);
    }

    @Override
    public void setOnHelp(View.OnClickListener listener) {
        actionHelp.setOnClickListener(listener);
    }

    @Override
    public void startSignIn() {
        if (mDialog != null) {
            try {
                mDialog.dismiss();
            } catch (Throwable ignore) {
            }
            mDialog = null;
        }

        mDialog = new SignInMnemonicDialog();
        if (getFragmentManager() != null) {
            mDialog.show(getFragmentManager(), null);
        }
    }

    @Override
    public void startHelp() {
        getActivity().startActivity(IntentHelper.newUrl("https://help.minter.network"));
    }

    @Override
    public void startCreateWallet() {
        if (mDialog != null) {
            try {
                mDialog.dismiss();
            } catch (Throwable ignore) {
            }
            mDialog = null;
        }

        mDialog = new CreateWalletDialog.Builder()
                .build();
        mDialog.show(getFragmentManager(), null);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        postponeEnterTransition();
        View view = inflater.inflate(R.layout.fragment_auth, container, false);
        ButterKnife.bind(this, view);
        ViewCompat.setTransitionName(logo, getString(R.string.transaction_auth_logo));
        startPostponedEnterTransition();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

    }

    @ProvidePresenter
    AuthPresenter providePresenter() {
        return authPresenterProvider.get();
    }
}
