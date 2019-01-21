/*
 * Copyright (C) by MinterTeam. 2019
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

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.view.ViewCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import com.arellomobile.mvp.presenter.InjectPresenter;
import com.arellomobile.mvp.presenter.ProvidePresenter;

import javax.inject.Inject;
import javax.inject.Provider;

import butterknife.BindView;
import butterknife.ButterKnife;
import network.minter.bipwallet.R;
import network.minter.bipwallet.advanced.ui.AdvancedMainActivity;
import network.minter.bipwallet.auth.AuthModule;
import network.minter.bipwallet.auth.views.AuthPresenter;
import network.minter.bipwallet.internal.BaseInjectFragment;
import network.minter.bipwallet.internal.helpers.IntentHelper;
import network.minter.bipwallet.internal.system.testing.CallbackIdlingResource;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class AuthFragment extends BaseInjectFragment implements AuthModule.AuthView {
    @Inject Provider<AuthPresenter> authPresenterProvider;
    @InjectPresenter AuthPresenter presenter;
    @BindView(R.id.action_create_wallet) Button actionCreateWallet;
    @BindView(R.id.action_advanced_mode) Button actionAdvancedMode;
    @BindView(R.id.action_signin) Button actionSignin;
    @BindView(R.id.action_help) Button actionHelp;
    @BindView(R.id.logo) ImageView logo;
    private CallbackIdlingResource mAuthWait;

    @VisibleForTesting
    public void registerIdling(CallbackIdlingResource authWaitIdlingRes) {
        mAuthWait = authWaitIdlingRes;
    }

    @Override
    public void setOnCreateWallet(View.OnClickListener listener) {
        actionCreateWallet.setOnClickListener(listener);
    }

    @Override
    public void setOnAdvancedMode(View.OnClickListener listener) {
        actionAdvancedMode.setOnClickListener(listener);
    }

    @Override
    public void setOnSignin(View.OnClickListener listener) {
        actionSignin.setOnClickListener(listener);
    }

    @Override
    public void setOnHelp(View.OnClickListener listener) {
        actionHelp.setOnClickListener(listener);
    }

    @Override
    public void startAdvancedMode() {
        getActivity().startActivity(new Intent(getActivity(), AdvancedMainActivity.class));
    }

    @Override
    public void startRegister() {
        getActivity().startActivity(new Intent(getActivity(), RegisterActivity.class));
    }

    @Override
    public void startSignIn() {
        getActivity().startActivity(new Intent(getActivity(), SigninActivity.class));
    }

    @Override
    public void startHelp() {
        getActivity().startActivity(
                IntentHelper.newUrl("https://help.minter.network")
        );
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

        if (mAuthWait != null) {
            mAuthWait.setIdleState(true);
        }

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
