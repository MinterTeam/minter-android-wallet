/*
 * Copyright (C) by MinterTeam. 2018
 * @link https://github.com/MinterTeam
 * @link https://github.com/edwardstock
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
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.view.View;

import com.annimon.stream.Stream;
import com.arellomobile.mvp.InjectViewState;
import com.theartofdev.edmodo.cropper.CropImage;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.inject.Inject;

import network.minter.bipwallet.R;
import network.minter.bipwallet.advanced.repo.SecretStorage;
import network.minter.bipwallet.home.HomeScope;
import network.minter.bipwallet.internal.Wallet;
import network.minter.bipwallet.internal.auth.AuthSession;
import network.minter.bipwallet.internal.dialogs.WalletInputDialog;
import network.minter.bipwallet.internal.exceptions.ProfileResponseException;
import network.minter.bipwallet.internal.helpers.ImageHelper;
import network.minter.bipwallet.internal.helpers.forms.validators.EmailValidator;
import network.minter.bipwallet.internal.helpers.forms.validators.MinterUsernameValidator;
import network.minter.bipwallet.internal.helpers.forms.validators.PhoneValidator;
import network.minter.bipwallet.internal.mvp.MvpBasePresenter;
import network.minter.bipwallet.internal.views.list.multirow.MultiRowAdapter;
import network.minter.bipwallet.settings.SettingsTabModule;
import network.minter.bipwallet.settings.ui.SettingsFieldType;
import network.minter.bipwallet.settings.views.rows.ChangeAvatarRow;
import network.minter.bipwallet.settings.views.rows.SettingsButtonRow;
import network.minter.profile.models.User;
import network.minter.profile.repo.ProfileAuthRepository;
import network.minter.profile.repo.ProfileRepository;
import timber.log.Timber;

import static network.minter.bipwallet.internal.ReactiveAdapter.convertToProfileErrorResult;
import static network.minter.bipwallet.internal.ReactiveAdapter.rxCallMy;

/**
 * MinterWallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@HomeScope
@InjectViewState
public class SettingsTabPresenter extends MvpBasePresenter<SettingsTabModule.SettingsTabView> {
    private final static int REQUEST_ATTACH_AVATAR = CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE;
    @Inject AuthSession session;
    @Inject SecretStorage secretStorage;
    @Inject ProfileRepository profileRepo;
    @Inject ProfileAuthRepository profileAuthRepo;
    @Inject Context context;
    private String mSourceUsername = null;

    private MultiRowAdapter mMainAdapter, mAdditionalAdapter;
    private ChangeAvatarRow mChangeAvatarRow;
    private Map<String, SettingsButtonRow> mMainSettingsRows = new LinkedHashMap<>();
    private Map<String, SettingsButtonRow> mAdditionalSettingsRows = new LinkedHashMap<>();

    @Inject
    public SettingsTabPresenter() {
        mMainAdapter = new MultiRowAdapter();
        mAdditionalAdapter = new MultiRowAdapter();
    }

    public void onUpdateProfile() {
        safeSubscribeIoToUi(rxCallMy(profileRepo.getProfile()))
                .subscribe(res -> {
                    if (res.isSuccess()) {
                        User u = session.getUser();
                        u.data = res.data;
                        session.setUser(u);
                        mMainAdapter.notifyItemRangeChanged(0, mMainAdapter.getItemCount());
                        mAdditionalAdapter.notifyItemRangeChanged(0, 1);
                    }
                });
    }

    @Override
    public void attachView(SettingsTabModule.SettingsTabView view) {
        super.attachView(view);

        getViewState().setMainAdapter(mMainAdapter);
        getViewState().setAdditionalAdapter(mAdditionalAdapter);
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
                        avatar = MediaStore.Images.Media.getBitmap(context.getContentResolver(), result.getUri());
                    } catch (IOException e) {
                        Timber.w(e);
                    }
                }

                if (avatar == null) {
                    mChangeAvatarRow.hideProgress();
                    return;
                }

                safeSubscribeIoToUi(rxCallMy(profileRepo.updateAvatar(ImageHelper.getBase64FromBitmap(avatar, 400))))
                        .subscribe(res -> {
                            mChangeAvatarRow.hideProgress();
                            if (res.isSuccess()) {
                                final User user = session.getUser();
                                user.getData().avatar = res.data;
                                session.setUser(user);
                                mChangeAvatarRow.setAvatar(() -> session.getUser().getData().getAvatar());
                                mMainAdapter.notifyItemChanged(0);
                            }
                        }, t -> mChangeAvatarRow.hideProgress());
            }
        }
    }

    public void onLogout() {
        getViewState().startLogin();
    }

    @Override
    protected void onFirstViewAttach() {
        super.onFirstViewAttach();
        if (session.getRole() == AuthSession.AuthType.Basic) {
            mChangeAvatarRow = new ChangeAvatarRow(() -> session.getUser().getData().getAvatar(), this::onClickChangeAvatar);
            mMainAdapter.addRow(mChangeAvatarRow);

            mMainSettingsRows.put("username", new SettingsButtonRow("Username", () -> session.getUser().getData().username, this::onClickChangeUsername));
            mMainSettingsRows.put("email", new SettingsButtonRow("Email", () -> session.getUser().getData().email, "Add", this::onClickChangeEmail));
            mMainSettingsRows.put("password", new SettingsButtonRow("Password", "Change", this::onClickChangePassword).setInactive(true));
            Stream.of(mMainSettingsRows.values()).forEach(item -> mMainAdapter.addRow(item));

            mAdditionalAdapter.addRow(new SettingsButtonRow("My Addresses", "Manage", this::onClickAddresses).setInactive(true));
        } else {
            mMainAdapter.addRow(new SettingsButtonRow("My Addresses", "Manage", this::onClickAddresses).setInactive(true));
        }
    }

    private void onClickAddresses(View view, View sharedView, String value) {
        getViewState().startAddressList();
    }

    private void onClickChangePassword(View view, View sharedView, String value) {
        getViewState().startPasswordChange();
    }

    private void onClickChangeEmail(View view, View sharedView, String value) {
        startEditProfileField(SettingsFieldType.Email, "Email", "email", value);
    }

    private void onClickChangePhone(View view, View sharedView, String value) {
        startEditProfileField(SettingsFieldType.Phone, "Mobile number", "phone", value);
    }

    private void onClickChangeUsername(View view, View sharedView, String value) {
        mSourceUsername = value;
        startEditProfileField(SettingsFieldType.Username, "Username", "username", value);
    }

    private void onSubmitField(final WalletInputDialog dialog, final String fieldName, final String value) {
        if (fieldName.equals("username") && !value.equals(mSourceUsername)) {
            // check username is available
            safeSubscribeIoToUi(rxCallMy(profileAuthRepo.checkUsernameAvailability(value.substring(1))))
                    .onErrorResumeNext(convertToProfileErrorResult())
                    .subscribe(res -> {
                        if (!res.isSuccess() || !res.data.isAvailable) {
                            dialog.setError("Username is unavailable");
                        } else {
                            submitProfile(dialog, fieldName, value);
                        }
                    }, Wallet.Rx.errorHandler(getViewState()));
        } else {
            submitProfile(dialog, fieldName, value);
        }
    }

    /**
     * @param dialog
     * @param fieldName
     * @param value
     * @TODO refactoring
     */
    private void submitProfile(WalletInputDialog dialog, final String fieldName, final String value) {
        final String toSave;
        if (fieldName.equals("username") && value.substring(0, 1).equals("@")) {
            toSave = value.substring(1);
        } else {
            toSave = value;
        }
        safeSubscribeIoToUi(rxCallMy(profileRepo.updateField(fieldName, toSave)))
                .subscribe(res -> {
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
                        }
                        dialog.dismiss();
                        getViewState().showMessage("Profile updated");
                    }
                }, Wallet.Rx.errorHandler(getViewState()));
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
                    break;
                case Username:
                    dialog.setInputTypeUsername();
                    dialog.addValidator(new MinterUsernameValidator(ctx.getString(R.string.input_username_invalid)));
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
        getViewState().startAvatarChooser();
    }


}
