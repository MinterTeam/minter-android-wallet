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

package network.minter.bipwallet.settings.views;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.view.View;
import android.widget.Switch;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricPrompt;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import moxy.InjectViewState;
import network.minter.bipwallet.R;
import network.minter.bipwallet.analytics.AppEvent;
import network.minter.bipwallet.home.HomeScope;
import network.minter.bipwallet.internal.Wallet;
import network.minter.bipwallet.internal.auth.AuthSession;
import network.minter.bipwallet.internal.dialogs.ConfirmDialog;
import network.minter.bipwallet.internal.dialogs.WalletProgressDialog;
import network.minter.bipwallet.internal.helpers.FingerprintHelper;
import network.minter.bipwallet.internal.helpers.PrefKeys;
import network.minter.bipwallet.internal.mvp.MvpBasePresenter;
import network.minter.bipwallet.internal.settings.SettingsManager;
import network.minter.bipwallet.internal.storage.SecretStorage;
import network.minter.bipwallet.internal.views.list.multirow.MultiRowAdapter;
import network.minter.bipwallet.security.SecurityModule;
import network.minter.bipwallet.settings.contract.SettingsTabView;
import network.minter.bipwallet.settings.repo.MinterBotRepository;
import network.minter.bipwallet.settings.views.rows.SettingsButtonRow;
import network.minter.bipwallet.settings.views.rows.SettingsSwitchRow;
import network.minter.bipwallet.settings.views.rows.TitleRow;
import network.minter.core.MinterSDK;
import timber.log.Timber;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@HomeScope
@InjectViewState
public class SettingsTabPresenter extends MvpBasePresenter<SettingsTabView> {
    //    private final static int REQUEST_ATTACH_AVATAR = CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE;
    private final static int REQUEST_CREATE_PIN_CODE = 1002;
    private final static int REQUEST_VERIFY_PIN_CODE = 1003;
    @Inject AuthSession session;
    @Inject SecretStorage secretStorage;
    @Inject SharedPreferences prefs;
    @Inject SettingsManager settings;
    @Inject FingerprintHelper fingerHelper;
    private MultiRowAdapter mMainAdapter, mAdditionalAdapter, mSecurityAdapter;
    private MinterBotRepository mBotRepo = new MinterBotRepository();

    @Inject
    public SettingsTabPresenter() {
        mSecurityAdapter = new MultiRowAdapter();
        mMainAdapter = new MultiRowAdapter();
        mAdditionalAdapter = new MultiRowAdapter();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    @Override
    public void attachView(SettingsTabView view) {
        super.attachView(view);

        getViewState().setSecurityAdapter(mSecurityAdapter);
        getViewState().setMainAdapter(mMainAdapter);
        getViewState().setAdditionalAdapter(mAdditionalAdapter);
        getViewState().setOnOurChannelClickListener(this::onOurChannelClickListener);
        getViewState().setOnSupportChatClickListener(this::onSupportChatClickListener);
    }

    @Override
    public void detachView(SettingsTabView view) {
        super.detachView(view);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CREATE_PIN_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                Timber.d("PIN successfully handled");
            }
        }
    }

    public void onLogout() {
        getAnalytics().send(AppEvent.SettingsLogoutButton);
        Wallet.app().session().logout();
        Wallet.app().secretStorage().destroy();
        Wallet.app().storage().deleteAll();
        Wallet.app().storageCache().deleteAll();
        Wallet.app().prefs().edit().clear().apply();
        getViewState().startLogin();
    }

    @Override
    protected void onFirstViewAttach() {
        super.onFirstViewAttach();

        // security row
        SettingsSwitchRow enablePinRow = new SettingsSwitchRow("Unlock with PIN-code", this::isPinCodeEnabled, this::onEnablePinCode);
        SettingsSwitchRow fingerprintRow = new SettingsSwitchRow("Unlock with fingerprint", this::isFingerprintEnabled, this::onEnableFingerprint);
        fingerprintRow.setEnabled(this::isPinCodeEnabled);

        SettingsButtonRow changePinRow = new SettingsButtonRow("Change PIN-code", "", this::onChangePinClick);
        changePinRow.setEnabled(() -> secretStorage.hasPinCode());

        mSecurityAdapter.addRow(new TitleRow("Security"));
        mSecurityAdapter.addRow(enablePinRow);

        if (fingerHelper.isHardwareDetected()) {
            mSecurityAdapter.addRow(fingerprintRow);
        }
        mSecurityAdapter.addRow(changePinRow);


        mMainAdapter.addRow(new TitleRow("Notifications"));
        mMainAdapter.addRow(new SettingsSwitchRow("Enable sounds", this::isSoundsEnabled, this::onSwitchSounds));
        mMainAdapter.addRow(new SettingsSwitchRow("Enable notifications", this::isNotificationsEnabled, this::onSwitchNotifications));

        mAdditionalAdapter.addRow(new TitleRow("Default Wallet"));
        SettingsButtonRow defWalletRow = new SettingsButtonRow(secretStorage.getMainWallet().toShortString(), "", this::onChangeDefWallet);
        mAdditionalAdapter.addRow(defWalletRow);
    }

    private void onChangeDefWallet(View view, View view1, String s) {

    }

    private void onSupportChatClickListener(View view) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/MinterHelp"));
        getViewState().startIntent(intent);
    }

    private void onOurChannelClickListener(View view) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/MinterTeam"));
        getViewState().startIntent(intent);
    }

    private Boolean isNotificationsEnabled() {
        return settings.getBool(SettingsManager.EnableLiveNotifications);
    }

    private Boolean isSoundsEnabled() {
        return prefs.getBoolean(PrefKeys.ENABLE_SOUNDS, false);
    }

    private Boolean isPinCodeEnabled() {
        return prefs.getBoolean(PrefKeys.ENABLE_PIN_CODE, false);
    }

    private Boolean isFingerprintEnabled() {
        return prefs.getBoolean(PrefKeys.ENABLE_FP, false) && prefs.getBoolean(PrefKeys.ENABLE_PIN_CODE, false);
    }

    private void onChangePinClick(View view, View view1, String s) {
        getViewState().startPinCodeManager(REQUEST_CREATE_PIN_CODE, SecurityModule.PinMode.Change);
    }

    private void onEnableFingerprint(View view, Boolean enabled) {
        if (enabled) {
            if (!fingerHelper.hasEnrolledFingerprints()) {
                ((Switch) view).setChecked(false);
                getViewState().startDialog(ctx -> new ConfirmDialog.Builder(ctx, R.string.fingerprint_dialog_enroll_title)
                        .setText(R.string.fingerprint_dialog_enroll_text)
                        .setPositiveAction(R.string.btn_settings, (dialogInterface, integer) -> {
                            dialogInterface.dismiss();
                            getViewState().startFingerprintEnrollment();
                            return null;
                        })
                        .setNegativeAction(R.string.btn_cancel)
                        .create());

                return;
            }

            getViewState().startBiometricPrompt(new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                    super.onAuthenticationError(errorCode, errString);
                    view.post(() -> {
                        ((Switch) view).setChecked(false);
                    });
                    if (errorCode == BiometricPrompt.ERROR_CANCELED || errorCode == BiometricPrompt.ERROR_USER_CANCELED || errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        return;
                    }

                    Timber.e("Unable to auth FP: [%d] %s", errorCode, errString);
                }

                @Override
                public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                    super.onAuthenticationSucceeded(result);
                    prefs.edit().putBoolean(PrefKeys.ENABLE_FP, true).apply();
                    Timber.e("Success to auth FP");
                }

                @Override
                public void onAuthenticationFailed() {
                    super.onAuthenticationFailed();
                    Timber.e("FP auth failed");
                }
            });

            return;
        }

        prefs.edit().putBoolean(PrefKeys.ENABLE_FP, false).apply();
    }

    private void onEnablePinCode(View view, Boolean enabled) {
        if (!secretStorage.hasPinCode()) {
            getViewState().startPinCodeManager(REQUEST_CREATE_PIN_CODE, SecurityModule.PinMode.Creation);
        } else {
            getViewState().startPinCodeManager(REQUEST_CREATE_PIN_CODE, SecurityModule.PinMode.Deletion);
        }
    }

    private void onSwitchNotifications(View view, Boolean enabled) {
        settings.putBool(SettingsManager.EnableLiveNotifications, enabled);
    }

    private void onSwitchSounds(View view, Boolean isChecked) {
        prefs.edit().putBoolean(PrefKeys.ENABLE_SOUNDS, isChecked).apply();
        Wallet.app().sounds().play(R.raw.click_pop_zap);
    }

    private void onRequestFreeCoins() {
        getViewState().startDialog(ctx -> {
            unsubscribeOnDestroy(
                    mBotRepo.requestFreeCoins(secretStorage.getAddresses().get(0))
                            .delay(500, TimeUnit.MILLISECONDS)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(this::onRequestFreeCoinsSuccess, t -> {
                                Timber.w(t);
                                onRequestFreeCoinsError(t.getMessage());
                            })
            );

            return new WalletProgressDialog.Builder(ctx, "Get free coins")
                    .setText("In progress...")
                    .create();
        });
    }

    private void onRequestFreeCoinsError(String message) {
        getViewState().startDialog(ctx -> new ConfirmDialog.Builder(ctx, "Can't get free coins")
                .setText(String.format("Our apologies, but we can't send to you free coins: %s", message))
                .setPositiveAction("Close")
                .create());
    }

    private void onRequestFreeCoinsSuccess(MinterBotRepository.MinterBotResult result) {
        if (!result.isOk()) {
            onRequestFreeCoinsError(result.getError());
            return;
        }
        getViewState().startDialog(ctx -> new ConfirmDialog.Builder(ctx, "Get free coins")
                .setText(String.format("We sent to you 100 %s", MinterSDK.DEFAULT_COIN))
                .setPositiveAction("OK")
                .create());
    }

}
