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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import network.minter.bipwallet.R;
import network.minter.bipwallet.home.ui.HomeActivity;
import network.minter.bipwallet.internal.BaseInjectFragment;
import network.minter.bipwallet.internal.BaseMvpInjectActivity;
import network.minter.bipwallet.internal.Wallet;
import network.minter.bipwallet.internal.auth.AuthSession;
import network.minter.bipwallet.internal.storage.SecretStorage;
import network.minter.bipwallet.security.SecurityModule;
import network.minter.bipwallet.security.ui.PinEnterActivity;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class SplashFragment extends BaseInjectFragment {
    private final static int REQUEST_START_PIN_ENTER = 1030;

    @Inject AuthSession session;
    @Inject SecretStorage secretStorage;
    @BindView(R.id.logo) ImageView logo;
    private AuthSwitchActivity mAuthSwitchActivity;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_splash, container, false);
        ButterKnife.bind(this, view);

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_START_PIN_ENTER) {
            if (resultCode == Activity.RESULT_OK) {
                startHome();
            }
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Observable.timer(1, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(res -> {
                    if (mAuthSwitchActivity == null) {
                        return;
                    }

                    if (!session.isLoggedIn(true) || Wallet.app().secretStorage().getAddresses().isEmpty()) {
                        Wallet.app().secretStorage().destroy();
                        Wallet.app().storageCache().deleteAll();
                        startAuth();
                        return;
                    }

                    if (secretStorage.hasPinCode()) {
                        startPinEnter();
                        return;
                    }

                    startHome();
                });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (!(context instanceof AuthSwitchActivity)) {
            throw new IllegalStateException("Activity must implement AuthSwitchActivity");
        }

        mAuthSwitchActivity = ((AuthSwitchActivity) context);

    }

    @Override
    public void onDetach() {
        super.onDetach();
        mAuthSwitchActivity = null;
    }

    private void startPinEnter() {
        new PinEnterActivity.Builder(getActivity(), SecurityModule.PinMode.Validation)
                .startHomeOnSuccess()
                .startClearTop();

        getActivity().finish();
    }

    private void startAuth() {
        ViewCompat.setTransitionName(logo, getString(R.string.transaction_auth_logo));
        mAuthSwitchActivity.showAuth(logo);
    }

    private void startHome() {
        ((BaseMvpInjectActivity) getActivity()).startActivityClearTop(getActivity(), HomeActivity.class);
        getActivity().finish();
    }

    public interface AuthSwitchActivity {
        void showAuth(View sharedView);
    }
}
