/*
 * Copyright (C) by MinterTeam. 2018
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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import com.annimon.stream.Stream;
import com.theartofdev.edmodo.cropper.CropImage;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricPrompt;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import moxy.InjectViewState;
import network.minter.bipwallet.BuildConfig;
import network.minter.bipwallet.R;
import network.minter.bipwallet.advanced.repo.SecretStorage;
import network.minter.bipwallet.analytics.AppEvent;
import network.minter.bipwallet.home.HomeScope;
import network.minter.bipwallet.internal.Wallet;
import network.minter.bipwallet.internal.auth.AuthSession;
import network.minter.bipwallet.internal.data.CachedRepository;
import network.minter.bipwallet.internal.dialogs.WalletConfirmDialog;
import network.minter.bipwallet.internal.dialogs.WalletInputDialog;
import network.minter.bipwallet.internal.dialogs.WalletProgressDialog;
import network.minter.bipwallet.internal.exceptions.ProfileResponseException;
import network.minter.bipwallet.internal.helpers.ContextHelper;
import network.minter.bipwallet.internal.helpers.FingerprintHelper;
import network.minter.bipwallet.internal.helpers.ImageHelper;
import network.minter.bipwallet.internal.helpers.PrefKeys;
import network.minter.bipwallet.internal.helpers.forms.validators.EmailValidator;
import network.minter.bipwallet.internal.helpers.forms.validators.MinterUsernameValidator;
import network.minter.bipwallet.internal.helpers.forms.validators.PhoneValidator;
import network.minter.bipwallet.internal.mvp.MvpBasePresenter;
import network.minter.bipwallet.internal.settings.SettingsManager;
import network.minter.bipwallet.internal.views.list.multirow.MultiRowAdapter;
import network.minter.bipwallet.security.SecurityModule;
import network.minter.bipwallet.settings.contract.SettingsTabView;
import network.minter.bipwallet.settings.repo.CachedMyProfileRepository;
import network.minter.bipwallet.settings.repo.MinterBotRepository;
import network.minter.bipwallet.settings.ui.SettingsFieldType;
import network.minter.bipwallet.settings.views.rows.ChangeAvatarRow;
import network.minter.bipwallet.settings.views.rows.SettingsButtonRow;
import network.minter.bipwallet.settings.views.rows.SettingsSwitchRow;
import network.minter.core.MinterSDK;
import network.minter.profile.models.User;
import network.minter.profile.repo.ProfileAuthRepository;
import network.minter.profile.repo.ProfileRepository;
import timber.log.Timber;

import static network.minter.bipwallet.apis.reactive.ReactiveMyMinter.rxProfile;
import static network.minter.bipwallet.apis.reactive.ReactiveMyMinter.toProfileError;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@HomeScope
@InjectViewState
public class SettingsTabPresenter extends MvpBasePresenter<SettingsTabView> {
    private final static int REQUEST_ATTACH_AVATAR = CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE;
    private final static int REQUEST_CREATE_PIN_CODE = 1002;
    private final static int REQUEST_VERIFY_PIN_CODE = 1003;
    public static PublishSubject<User.Avatar> AVATAR_CHANGE_SUBJECT = PublishSubject.create();
    @Inject AuthSession session;
    @Inject SecretStorage secretStorage;
    @Inject ProfileRepository profileRepo;
    @Inject CachedRepository<User.Data, CachedMyProfileRepository> profileCachedRepo;
    @Inject ProfileAuthRepository profileAuthRepo;
    @Inject SharedPreferences prefs;
    @Inject SettingsManager settings;
    @Inject FingerprintHelper fingerHelper;
    private String mSourceUsername = null;
    private MultiRowAdapter mMainAdapter, mAdditionalAdapter, mSecurityAdapter;
    private ChangeAvatarRow mChangeAvatarRow;
    private Map<String, SettingsButtonRow> mMainSettingsRows = new LinkedHashMap<>();
    private MinterBotRepository mBotRepo = new MinterBotRepository();

    @Inject
    public SettingsTabPresenter() {
        mMainAdapter = new MultiRowAdapter();
        mAdditionalAdapter = new MultiRowAdapter();
        mSecurityAdapter = new MultiRowAdapter();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    @Override
    public void attachView(SettingsTabView view) {
        super.attachView(view);
//        getViewState().setFingerprintHandler(fingerHelper);
        profileCachedRepo.update(profile -> mMainAdapter.notifyDataSetChanged());
        getViewState().setMainAdapter(mMainAdapter);
        getViewState().setAdditionalAdapter(mAdditionalAdapter);
        getViewState().setSecurityAdapter(mSecurityAdapter);
        //noinspection ConstantConditions
        if (BuildConfig.FLAVOR.equals("netTest") || BuildConfig.FLAVOR.equals("netTestNoCrashlytics")) {
            getViewState().showFreeCoinsButton(true);
            getViewState().setOnFreeCoinsClickListener(v -> {
                Wallet.app().sounds().play(R.raw.bip_beep_digi_octave);
                getViewState().startDialog(exec -> new WalletConfirmDialog.Builder(exec, "Get free coins")
                        .setText(String.format("Are you wanna get FREE 100 %s?", MinterSDK.DEFAULT_COIN))
                        .setPositiveAction("Sure!", (d, w) -> {
                            d.dismiss();
                            onRequestFreeCoins();
                        })
                        .setNegativeAction("Nope")
                        .create());
            });
        }

    }

    @Override
    public void detachView(SettingsTabView view) {
        super.detachView(view);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ATTACH_AVATAR) {
            boolean isOk = resultCode == Activity.RESULT_OK;
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (isOk) {
                mChangeAvatarRow.showProgress();
                Bitmap avatar;
                avatar = result.getBitmap();
                if (avatar == null) {
                    try {
                        avatar = MediaStore.Images.Media.getBitmap(Wallet.app().context().getContentResolver(), result.getUri());
                    } catch (IOException e) {
                        Timber.w(e);
                    }
                }

                if (avatar == null) {
                    mChangeAvatarRow.hideProgress();
                    return;
                }

                unsubscribeOnDestroy(
                        safeSubscribeIoToUi(
                                rxProfile(profileRepo.updateAvatar(ImageHelper.getBase64FromBitmap(avatar, 400))))
                                .subscribe(res -> {
                                    mChangeAvatarRow.hideProgress();
                                    if (res.isSuccess()) {
                                        final User user = session.getUser();
                                        user.getData().avatar = res.data;
                                        session.setUser(user);
                                        mChangeAvatarRow.setAvatar(() -> session.getUser().getData().getAvatar());
                                        mMainAdapter.notifyItemChanged(0);
                                        AVATAR_CHANGE_SUBJECT.onNext(session.getUser().getData().getAvatar());
                                    }
                                }, t -> mChangeAvatarRow.hideProgress())
                );
            }
        } else if (requestCode == REQUEST_CREATE_PIN_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                Timber.d("PIN successfully handled");
            }
        } else if (requestCode == REQUEST_VERIFY_PIN_CODE) {

        }
    }

    public void onLogout() {
        getAnalytics().send(AppEvent.SettingsLogoutButton);
        Wallet.app().session().logout();
        Wallet.app().secretStorage().destroy();
        Wallet.app().storage().deleteAll();
        Wallet.app().prefs().edit().clear().apply();
        getViewState().startLogin();
    }

    @Override
    protected void onFirstViewAttach() {
        super.onFirstViewAttach();

        // security row
        {
            SettingsSwitchRow enablePinRow = new SettingsSwitchRow("Unlock with PIN-code", () -> prefs.getBoolean(PrefKeys.ENABLE_PIN_CODE, false), this::onEnablePinCode);

            SettingsSwitchRow fingerprintRow = new SettingsSwitchRow("Unlock with fingerprint", () -> {
                return prefs.getBoolean(PrefKeys.ENABLE_FP, false) && prefs.getBoolean(PrefKeys.ENABLE_PIN_CODE, false);
            }, this::onEnableFingerprint);
            fingerprintRow.setEnabled(() -> prefs.getBoolean(PrefKeys.ENABLE_PIN_CODE, false));

            SettingsButtonRow changePinRow = new SettingsButtonRow("Change PIN-code", "", this::onChangePinClick);
            changePinRow.setEnabled(() -> secretStorage.hasPinCode());

            SettingsButtonRow mnemonicViewRow = new SettingsButtonRow("Show mnemonic", "", this::onClickShowMnemonic);

            mSecurityAdapter.addRow(enablePinRow);

            if (fingerHelper.isHardwareDetected()) {
                mSecurityAdapter.addRow(fingerprintRow);
            }

            mSecurityAdapter.addRow(changePinRow);
            mSecurityAdapter.addRow(mnemonicViewRow);
        }


        if (session.getRole() == AuthSession.AuthType.Basic) {
            mChangeAvatarRow = new ChangeAvatarRow(() -> session.getUser().getData().getAvatar(), this::onClickChangeAvatar);
            mMainAdapter.addRow(mChangeAvatarRow);

            mMainSettingsRows.put("username", new SettingsButtonRow("Username", () -> "@" + session.getUser().getData().username, (view3, sharedView3, value3) -> onClickChangeUsername(value3)));
            mMainSettingsRows.put("email", new SettingsButtonRow("Email", () -> session.getUser().getData().email, "Add", (view2, sharedView2, value2) -> onClickChangeEmail(value2)));
            mMainSettingsRows.put("password", new SettingsButtonRow("Password", "Change", (view1, sharedView1, value1) -> onClickChangePassword()).setInactive(true));
            Stream.of(mMainSettingsRows.values()).forEach(item -> mMainAdapter.addRow(item));

//            mAdditionalAdapter.addRow(new SettingsButtonRow("My Addresses", "Manage", (view, sharedView, value) -> onClickAddresses()).setInactive(true));
            mAdditionalAdapter.addRow(new SettingsSwitchRow("Enable sounds", () -> prefs.getBoolean(PrefKeys.ENABLE_SOUNDS, true), this::onSwitchSounds));
            mAdditionalAdapter.addRow(new SettingsSwitchRow("Enable notifications", () -> settings.getBool(SettingsManager.EnableLiveNotifications), this::onSwitchNotifications));
        } else {
//            mMainAdapter.addRow(new SettingsButtonRow("My Addresses", "Manage", (view, sharedView, value) -> onClickAddresses()).setInactive(true));
            mMainAdapter.addRow(new SettingsSwitchRow("Enable sounds", () -> prefs.getBoolean(PrefKeys.ENABLE_SOUNDS, true), this::onSwitchSounds));
            mMainAdapter.addRow(new SettingsSwitchRow("Enable notifications", () -> settings.getBool(SettingsManager.EnableLiveNotifications), this::onSwitchNotifications));
        }
    }

    private void onClickShowMnemonic(View view, View view1, String s) {
        if (!secretStorage.hasPinCode()) {
            getViewState().showMessage("Please, first set PIN-code to see your mnemonic");
            return;
        }
        getViewState().startDialog(ctx -> new WalletConfirmDialog.Builder(ctx, R.string.dialog_show_mnemonic_title)
                .setDescription(R.string.dialog_show_mnemonic_description)
                .setText(secretStorage.getSecretsStream().findFirst().get().getValue().getSeedPhrase())
                .setTextTypeface(Typeface.DEFAULT_BOLD)
                .setDescriptionTypeface(Typeface.DEFAULT_BOLD)
                .setTextAlignment(View.TEXT_ALIGNMENT_CENTER)
                .setOnTextClickListener(v -> ContextHelper.copyToClipboard(v.getContext(), ((TextView) v).getText()))
                .setPositiveAction("Close")
                .create());

    }

    private void onChangePinClick(View view, View view1, String s) {
        getViewState().startPinCodeManager(REQUEST_CREATE_PIN_CODE, SecurityModule.PinMode.Creation);
    }

    private void onEnableFingerprint(View view, Boolean enabled) {
        if (enabled) {
            if (!fingerHelper.hasEnrolledFingerprints()) {
                getViewState().startDialog(ctx -> new WalletConfirmDialog.Builder(ctx, R.string.fingerprint_dialog_enroll_title)
                        .setText(R.string.fingerprint_dialog_enroll_text)
                        .setPositiveAction(R.string.btn_settings, this::openFingerprintEnroll)
                        .setNegativeAction(R.string.btn_cancel, (d, w) -> d.dismiss())
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

    private void openFingerprintEnroll(DialogInterface dialogInterface, int i) {
        getViewState().startFingerprintEnrollment();
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
        if (isChecked) {
//            Wallet.app().sounds().loadAll();
        } else {
//            Wallet.app().sounds().releaseAll();
        }
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
        getViewState().startDialog(ctx -> new WalletConfirmDialog.Builder(ctx, "Can't get free coins")
                .setText(String.format("Our apologies, but we can't send to you free coins: %s", message))
                .setPositiveAction("Close")
                .create());
    }

    private void onRequestFreeCoinsSuccess(MinterBotRepository.MinterBotResult result) {
        if (!result.isOk()) {
            onRequestFreeCoinsError(result.getError());
            return;
        }
        getViewState().startDialog(ctx -> new WalletConfirmDialog.Builder(ctx, "Get free coins")
                .setText(String.format("We sent to you 100 %s", MinterSDK.DEFAULT_COIN))
                .setPositiveAction("OK")
                .create());
    }

    private void onClickAddresses() {
        getViewState().startAddressList();
    }

    private void onClickChangePassword() {
        getViewState().startPasswordChange();
    }

    private void onClickChangeEmail(String value) {
        startEditProfileField(SettingsFieldType.Email, "Email", "email", value);
    }

    private void onClickChangePhone(String value) {
        startEditProfileField(SettingsFieldType.Phone, "Mobile number", "phone", value);
    }

    private void onClickChangeUsername(String value) {
        mSourceUsername = value;
        startEditProfileField(SettingsFieldType.Username, "Username", "username", value);
    }

    private void onSubmitField(final WalletInputDialog dialog, final String fieldName, final String value) {
        Wallet.app().sounds().play(R.raw.click_pop_zap);
        dialog.showProgress();
        if (fieldName.equals("username") && !value.equals(mSourceUsername)) {
            // check username is available
            unsubscribeOnDestroy(
                    safeSubscribeIoToUi(rxProfile(profileAuthRepo.checkUsernameAvailability(value.substring(1))))
                            .onErrorResumeNext(toProfileError())
                            .subscribe(res -> {
                                if (!res.isSuccess() || !res.data.isAvailable) {
                                    dialog.setError("Username is unavailable");
                                    dialog.hideProgress();
                                } else {
                                    submitProfile(dialog, fieldName, value);
                                }
                            }, Wallet.Rx.errorHandler(getViewState()))
            );
        } else {
            submitProfile(dialog, fieldName, value);
        }
    }

    /**
     * @param dialog
     * @param fieldName
     * @param value
     */
    private void submitProfile(WalletInputDialog dialog, final String fieldName, final String value) {
        dialog.showProgress();
        final String toSave;
        if (fieldName.equals("username") && value.substring(0, 1).equals("@")) {
            toSave = value.substring(1);
        } else {
            toSave = value;
        }

        unsubscribeOnDestroy(
                safeSubscribeIoToUi(rxProfile(profileRepo.updateField(fieldName, toSave)))
                        .subscribe(res -> {
                            dialog.hideProgress();
                            if (!res.isSuccess()) {
                                dialog.setError(res.getError().message);
                                Timber.w(new ProfileResponseException(res.getError()));
                                getViewState().showMessage(res.getError().message);
                            } else {
                                if (mMainSettingsRows != null && mMainSettingsRows.containsKey(fieldName)) {
                                    mMainSettingsRows.get(fieldName).setValue(() -> value);
                                    final User user = session.getUser();
                                    switch (fieldName) {
                                        case "username":
                                            user.data.username = toSave;
                                            break;
                                        case "email":
                                            user.data.email = toSave;
                                            break;
                                    }
                                    session.setUser(user);
                                    profileCachedRepo.update(true);
                                }
                                dialog.dismiss();
                                getViewState().showMessage("Profile updated");
                            }
                        }, Wallet.Rx.errorHandler(getViewState()))
        );
    }

    private void startEditProfileField(final SettingsFieldType type, final String label, final String fieldName, final String value) {
        getViewState().startDialog(ctx -> {
            final WalletInputDialog.Builder dialog = new WalletInputDialog.Builder(ctx, label);
            switch (type) {
                case Phone:
                    dialog.setInputTypePhone();
                    dialog.addValidator(new PhoneValidator());
                    break;
                case Email:
                    dialog.setInputTypeEmail();
                    dialog.addValidator(new EmailValidator());
                    getAnalytics().send(AppEvent.EmailEditScreen);
                    break;
                case Username:
                    dialog.setInputTypeUsername();
                    dialog.addValidator(new MinterUsernameValidator(ctx.getString(R.string.input_username_invalid)));
                    getAnalytics().send(AppEvent.UsernameEditScreen);
                    break;
            }

            dialog.setHint(label);
            dialog.setValue(value);
            dialog.setActionTitle(ctx.getString(R.string.btn_save));
            dialog.setFieldName(fieldName);
            dialog.setSubmitListener(SettingsTabPresenter.this::onSubmitField);

            return dialog.create();
        });
    }

    private void onClickChangeAvatar(View view) {
        getAnalytics().send(AppEvent.SettingsChangeUserpicButton);
        getViewState().startAvatarChooser();
    }


}
