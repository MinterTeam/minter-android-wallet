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
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.view.View;

import com.annimon.stream.Stream;
import com.arellomobile.mvp.InjectViewState;
import com.theartofdev.edmodo.cropper.CropImage;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
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
import network.minter.bipwallet.internal.helpers.ImageHelper;
import network.minter.bipwallet.internal.helpers.PrefKeys;
import network.minter.bipwallet.internal.helpers.forms.validators.EmailValidator;
import network.minter.bipwallet.internal.helpers.forms.validators.MinterUsernameValidator;
import network.minter.bipwallet.internal.helpers.forms.validators.PhoneValidator;
import network.minter.bipwallet.internal.mvp.MvpBasePresenter;
import network.minter.bipwallet.internal.views.list.multirow.MultiRowAdapter;
import network.minter.bipwallet.settings.SettingsTabModule;
import network.minter.bipwallet.settings.repo.CachedMyProfileRepository;
import network.minter.bipwallet.settings.repo.MinterBotRepository;
import network.minter.bipwallet.settings.ui.SettingsFieldType;
import network.minter.bipwallet.settings.views.rows.ChangeAvatarRow;
import network.minter.bipwallet.settings.views.rows.SettingsButtonRow;
import network.minter.bipwallet.settings.views.rows.SettingsSwitchRow;
import network.minter.profile.models.User;
import network.minter.profile.repo.ProfileAuthRepository;
import network.minter.profile.repo.ProfileRepository;
import timber.log.Timber;

import static network.minter.bipwallet.internal.ReactiveAdapter.convertToProfileErrorResult;
import static network.minter.bipwallet.internal.ReactiveAdapter.rxCallProfile;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@HomeScope
@InjectViewState
public class SettingsTabPresenter extends MvpBasePresenter<SettingsTabModule.SettingsTabView> {
    private final static int REQUEST_ATTACH_AVATAR = CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE;
    public static PublishSubject<User.Avatar> AVATAR_CHANGE_SUBJECT = PublishSubject.create();
    @Inject AuthSession session;
    @Inject SecretStorage secretStorage;
    @Inject ProfileRepository profileRepo;
    @Inject CachedRepository<User.Data, CachedMyProfileRepository> profileCachedRepo;
    @Inject ProfileAuthRepository profileAuthRepo;
    @Inject SharedPreferences prefs;
    private String mSourceUsername = null;

    private MultiRowAdapter mMainAdapter, mAdditionalAdapter;
    private ChangeAvatarRow mChangeAvatarRow;
    private Map<String, SettingsButtonRow> mMainSettingsRows = new LinkedHashMap<>();
    private MinterBotRepository mBotRepo = new MinterBotRepository();

    @Inject
    public SettingsTabPresenter() {
        mMainAdapter = new MultiRowAdapter();
        mAdditionalAdapter = new MultiRowAdapter();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    @Override
    public void attachView(SettingsTabModule.SettingsTabView view) {
        super.attachView(view);
        profileCachedRepo.update(profile -> mMainAdapter.notifyDataSetChanged());
        getViewState().setMainAdapter(mMainAdapter);
        getViewState().setAdditionalAdapter(mAdditionalAdapter);
        //noinspection ConstantConditions
        if (BuildConfig.FLAVOR.equals("netTest") || BuildConfig.FLAVOR.equals("netTestNoCrashlytics")) {
            getViewState().showFreeCoinsButton(true);
            getViewState().setOnFreeCoinsClickListener(v -> {
                Wallet.app().sounds().play(R.raw.bip_beep_digi_octave);
                getViewState().startDialog(exec -> new WalletConfirmDialog.Builder(exec, "Get free coins")
                        .setText("Are you wanna get FREE 100 MNT?")
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
                                rxCallProfile(profileRepo.updateAvatar(ImageHelper.getBase64FromBitmap(avatar, 400))))
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
        }
    }

    public void onLogout() {
        getAnalytics().send(AppEvent.SettingsLogoutButton);
        getViewState().startLogin();
    }

    @Override
    protected void onFirstViewAttach() {
        super.onFirstViewAttach();
        if (session.getRole() == AuthSession.AuthType.Basic) {
            mChangeAvatarRow = new ChangeAvatarRow(() -> session.getUser().getData().getAvatar(), this::onClickChangeAvatar);
            mMainAdapter.addRow(mChangeAvatarRow);

            mMainSettingsRows.put("username", new SettingsButtonRow("Username", () -> "@" + session.getUser().getData().username, (view3, sharedView3, value3) -> onClickChangeUsername(value3)));
            mMainSettingsRows.put("email", new SettingsButtonRow("Email", () -> session.getUser().getData().email, "Add", (view2, sharedView2, value2) -> onClickChangeEmail(value2)));
            mMainSettingsRows.put("password", new SettingsButtonRow("Password", "Change", (view1, sharedView1, value1) -> onClickChangePassword()).setInactive(true));
            Stream.of(mMainSettingsRows.values()).forEach(item -> mMainAdapter.addRow(item));

            mAdditionalAdapter.addRow(new SettingsButtonRow("My Addresses", "Manage", (view, sharedView, value) -> onClickAddresses()).setInactive(true));
            mAdditionalAdapter.addRow(new SettingsSwitchRow("Enable sounds", () -> prefs.getBoolean(PrefKeys.ENABLE_SOUNDS, true), this::onSwitchSounds));
        } else {
            mMainAdapter.addRow(new SettingsButtonRow("My Addresses", "Manage", (view, sharedView, value) -> onClickAddresses()).setInactive(true));
            mMainAdapter.addRow(new SettingsSwitchRow("Enable sounds", () -> prefs.getBoolean(PrefKeys.ENABLE_SOUNDS, true), this::onSwitchSounds));
        }
    }

    private void onSwitchSounds(View view, Boolean isChecked) {
        Wallet.app().sounds().play(R.raw.click_pop_zap);
        prefs.edit().putBoolean(PrefKeys.ENABLE_SOUNDS, isChecked).apply();
        if (isChecked) {
            Wallet.app().sounds().loadAll();
        } else {
            Wallet.app().sounds().releaseAll();
        }
    }

    private void onRequestFreeCoins() {
        getViewState().startDialog(ctx -> {

            unsubscribeOnDestroy(
                    mBotRepo.requestFreeCoins(secretStorage.getAddresses().get(0))
                            .delay(500, TimeUnit.MILLISECONDS)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(this::onRequestFreeCoinsSuccess, t -> onRequestFreeCoinsError(t.getMessage()))
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
                .setText("We sent to you 100 MNT")
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
                    safeSubscribeIoToUi(rxCallProfile(profileAuthRepo.checkUsernameAvailability(value.substring(1))))
                            .onErrorResumeNext(convertToProfileErrorResult())
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
     *
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
                safeSubscribeIoToUi(rxCallProfile(profileRepo.updateField(fieldName, toSave)))
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
