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

package network.minter.bipwallet.security.views;

import android.content.SharedPreferences;
import android.view.View;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricPrompt;
import moxy.InjectViewState;
import network.minter.bipwallet.R;
import network.minter.bipwallet.internal.Wallet;
import network.minter.bipwallet.internal.helpers.PrefKeys;
import network.minter.bipwallet.internal.mvp.MvpBasePresenter;
import network.minter.bipwallet.internal.storage.KVStorage;
import network.minter.bipwallet.internal.storage.SecretStorage;
import network.minter.bipwallet.security.SecurityModule;
import network.minter.bipwallet.security.contract.PinValidatingView;

@InjectViewState
public class PinValidatingPresenter extends MvpBasePresenter<PinValidatingView> {
    private final static String PREF_PIN_INVALID_TYPE_COUNT = "pin_type_invalid_type_count";
    private final static String PREF_PIN_INVALID_TYPE_TIME = "pin_type_invalid_type_time";
    @Inject SecretStorage storage;
    @Inject KVStorage kvStorage;
    @Inject SharedPreferences prefs;

    private String mSourcePin = "";
    private String mPin = "";

    @Inject
    public PinValidatingPresenter() {

    }

    private void init() {
        if (storage.hasPinCode()) {
            mSourcePin = storage.getPinCode();
        }

        getViewState().setOnPinValueListener(this::onPinEntered);
    }

    @Override
    protected void onFirstViewAttach() {
        super.onFirstViewAttach();
        init();
    }

    private void startFingerprintValidation() {
        getViewState().startBiometricPrompt(new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                getViewState().finishSuccess();
            }
        });
    }

    private void onClickFingerprintButton(View view) {
        startFingerprintValidation();
    }

    private void logout() {
        Wallet.app().session().logout();
        Wallet.app().secretStorage().destroy();
        Wallet.app().storage().deleteAll();
        Wallet.app().storageCache().deleteAll();
        Wallet.app().prefs().edit().clear().apply();
        getViewState().startLogin();
    }

    private void onValidationError(String value) {
        long timestamp = System.currentTimeMillis() / 1000;
        Long firstInvalidTime = kvStorage.get(PREF_PIN_INVALID_TYPE_TIME, 0L);
        Integer invalidCount = kvStorage.get(PREF_PIN_INVALID_TYPE_COUNT, 0);

        invalidCount++;

        // first invalid
        if (firstInvalidTime == 0) {
            firstInvalidTime = timestamp;
        }

        if (firstInvalidTime + SecurityModule.LOCK_INTERVAL_S < timestamp) {
            firstInvalidTime = timestamp;
            invalidCount = 0;
        }

        kvStorage.put(PREF_PIN_INVALID_TYPE_TIME, firstInvalidTime);
        kvStorage.put(PREF_PIN_INVALID_TYPE_COUNT, invalidCount);

        if (invalidCount >= (SecurityModule.MAX_TRIES_UNTIL_LOCK)) {
            logout();
            return;
        }

        getViewState().setPinError(Wallet.app().res().getString(R.string.error_pin_invalid, SecurityModule.MAX_TRIES_UNTIL_LOCK - invalidCount));
    }

    private void onPinEntered(String value, int len, boolean valid) {
        if (len == 4) {
            mPin = value;
        } else {
            mPin = "";
        }

        getViewState().setupTitle(R.string.title_pin_enter);
        getViewState().setPinHint(R.string.hint_pin_enter);
        if (prefs.getBoolean(PrefKeys.ENABLE_FP, false) && Wallet.app().fingerprint().hasEnrolledFingerprints()) {
            getViewState().setFingerprintEnabled(true);
            getViewState().setOnFingerprintClickListener(this::onClickFingerprintButton);
            startFingerprintValidation();
        }
        getViewState().setEnableValidation(mSourcePin);
        getViewState().setOnPinValidationError(this::onValidationError);
    }


}
