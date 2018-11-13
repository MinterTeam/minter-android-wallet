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

package network.minter.bipwallet.settings.views.migration;

import android.support.annotation.NonNull;
import android.util.SparseArray;
import android.view.View;
import android.widget.EditText;

import com.arellomobile.mvp.InjectViewState;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import network.minter.bipwallet.R;
import network.minter.bipwallet.advanced.models.SecretData;
import network.minter.bipwallet.advanced.repo.SecretStorage;
import network.minter.bipwallet.analytics.AppEvent;
import network.minter.bipwallet.analytics.base.HasAnalyticsEvent;
import network.minter.bipwallet.internal.Wallet;
import network.minter.bipwallet.internal.dialogs.WalletConfirmDialog;
import network.minter.bipwallet.internal.dialogs.WalletProgressDialog;
import network.minter.bipwallet.internal.mvp.MvpBasePresenter;
import network.minter.bipwallet.settings.SettingsTabModule;
import network.minter.core.crypto.EncryptedString;
import network.minter.core.crypto.HashUtil;
import network.minter.profile.models.PasswordChangeRequest;
import network.minter.profile.models.ProfileAddressData;
import network.minter.profile.repo.ProfileAddressRepository;
import network.minter.profile.repo.ProfileRepository;
import timber.log.Timber;

import static network.minter.bipwallet.internal.ReactiveAdapter.rxCallProfile;
import static network.minter.bipwallet.settings.views.migration.MigrationException.STEP_1_GET_REMOTE_ADDRESS_LIST;
import static network.minter.bipwallet.settings.views.migration.MigrationException.STEP_2_RE_ENCRYPT_REMOTE_DATA;
import static network.minter.bipwallet.settings.views.migration.MigrationException.STEP_3_UPDATE_ENCRYPTED_DATA_REMOTE;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@InjectViewState
public class PasswordChangeMigrationPresenter extends MvpBasePresenter<SettingsTabModule.PasswordChangeMigrationView> implements HasAnalyticsEvent {

    @Inject ProfileRepository profileRepo;
    @Inject ProfileAddressRepository addressRepo;
    @Inject SecretStorage secretStorage;

    private String mNewPassword;
    private WeakReference<WalletProgressDialog> mProgressDialog;
    private SparseArray<PublishSubject<Object>> mRetryHandlers = new SparseArray<>(3);

    @Inject
    public PasswordChangeMigrationPresenter() {
        mRetryHandlers.put(STEP_1_GET_REMOTE_ADDRESS_LIST, PublishSubject.create());
        mRetryHandlers.put(STEP_2_RE_ENCRYPT_REMOTE_DATA, PublishSubject.create());
        mRetryHandlers.put(STEP_3_UPDATE_ENCRYPTED_DATA_REMOTE, PublishSubject.create());
    }

    @Override
    public void attachView(SettingsTabModule.PasswordChangeMigrationView view) {
        super.attachView(view);
    }

    @NonNull
    @Override
    public AppEvent getAnalyticsEvent() {
        return AppEvent.PasswordEditScreen;
    }

    @Override
    protected void onFirstViewAttach() {
        super.onFirstViewAttach();
        getViewState().setFormValidateListener(this::onFormValidate);
        getViewState().setTextChangedListener(this::onTextChanged);
        getViewState().setOnClickSubmit(this::onSubmit);
    }

    private Function<Observable<Throwable>, ObservableSource<?>> migrationStepFailed(@MigrationException.MigrationStep int step) {
        return throwableObservable -> throwableObservable.switchMap((Function<Throwable, ObservableSource<?>>) throwable -> {
            onError(throwable, mRetryHandlers.get(step));
            return mRetryHandlers.get(step);
        });
    }

    private void log(int step, String message) {
        Timber.tag("EncryptedMigration").d("[%d] %s", step, message);
    }

    private void onSubmit(View view) {
        Wallet.app().sounds().play(R.raw.click_pop_zap);
        getViewState().startDialog(ctx -> {
            WalletProgressDialog dialog = new WalletProgressDialog.Builder(ctx, "Changing password")
                    .setText("Please wait, we are changing password and re-encrypting all your secret data. \n\nDO NOT close app while this process does not finished. It may corrupt encrypted data!")
                    .create();

            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);

            mProgressDialog = new WeakReference<>(dialog);

            rxCallProfile(addressRepo.getAddresses())
                    .subscribeOn(Schedulers.io())
                    .retryWhen(migrationStepFailed(STEP_1_GET_REMOTE_ADDRESS_LIST))
                    // comparing local and remote addresses and get id to update on server
                    .switchMap(res -> Observable.create((ObservableOnSubscribe<PasswordChangeRequest>) emitter -> {
                        final PasswordChangeRequest request = new PasswordChangeRequest();
                        // creating local encryption key to encrypted privates
                        final String encryptionKey = HashUtil.sha256Hex(mNewPassword);
                        // setting raw password, it will be hashed to double sha256 inside PasswordChangeRequest
                        request.setRawPassword(mNewPassword);

                        // sources
                        final List<ProfileAddressData> addresses = new ArrayList<>(res.data);
                        // targets
                        final List<ProfileAddressData> reEncryptedData = new ArrayList<>(res.data.size());
                        for (SecretData dataLocal : secretStorage.getSecrets().values()) {
                            if (dataLocal == null) {
                                emitter.onError(new RuntimeException("Unable to get secret from local storage"));
                                return;
                            }

                            ProfileAddressData dataRemote = null;
                            for (ProfileAddressData item : addresses) {
                                if (dataLocal.getMinterAddress().equals(item.address)) {
                                    dataRemote = item;
                                    break;
                                }
                            }
                            if (dataRemote == null) {
                                emitter.onError(new RuntimeException(String.format("Can't find remote address %s", dataLocal.getMinterAddress())));
                                return;
                            }

                            // re-encrypting data with new encryption key from new password
                            dataRemote.encrypted = new EncryptedString(dataLocal.getSeedPhrase(), encryptionKey);
                            reEncryptedData.add(dataRemote);
                        }

                        request.addEncrypted(reEncryptedData);
                        emitter.onNext(request);
                        emitter.onComplete();
                    }))
                    .retryWhen(migrationStepFailed(STEP_2_RE_ENCRYPT_REMOTE_DATA))
                    // step 3 sending data to server
                    .switchMap(request -> rxCallProfile(profileRepo.changePassword(request)))
                    .retryWhen(migrationStepFailed(STEP_3_UPDATE_ENCRYPTED_DATA_REMOTE))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(res -> {
                        log(3, "Send data");
                        secretStorage.setEncryptionKey(mNewPassword);

                        onMigrationSuccess();
                    });


            return mProgressDialog.get();
        });
    }

    private void onMigrationSuccess() {
        getViewState().startDialog(ctx2 -> new WalletConfirmDialog.Builder(ctx2, "Success!")
                .setText("Password successfully migrated!")
                .setPositiveAction("Ok", (d2, w2) -> {
                    d2.dismiss();
                    getViewState().finish();
                })
                .create());
    }


    private void onError(Throwable throwable, final PublishSubject<Object> errorRetry) {
        Timber.w(throwable);
        showErrorDialog(throwable, errorRetry);
    }

    private void showErrorDialog(Throwable throwable, PublishSubject<Object> errorRetry) {
        String message = String.format("Unable to migrate secret data: %s", throwable.getMessage());
        if (throwable instanceof MigrationException) {
            MigrationException ex = ((MigrationException) throwable);
            switch (ex.getStep()) {
                case STEP_1_GET_REMOTE_ADDRESS_LIST:
                    message = String.format("Unable to resolve MyMinter addresses. %s", throwable.getMessage());
                    break;
                case STEP_2_RE_ENCRYPT_REMOTE_DATA:
                    message = String.format("Unable to encrypt data with new password. %s", throwable.getMessage());
                    break;
                case STEP_3_UPDATE_ENCRYPTED_DATA_REMOTE:
                    message = String.format("Unable to upload newly encrypted data. %s", throwable.getMessage());
                    break;
            }
        }

        final String finalMessage = message;
        getViewState().startDialog(ctx -> new WalletConfirmDialog.Builder(ctx, "Error")
                .setText(finalMessage)
                .setPositiveAction("Try again", (d, w) -> {
                    errorRetry.onNext(new Object());
                    d.dismiss();
                    getViewState().startDialog(ctx2 -> {
                        WalletProgressDialog dialog = new WalletProgressDialog.Builder(ctx, "Changing password")
                                .setText("Please wait, we are changing password and re-encrypting all your secret data. \n\nDO NOT close app while this process does not finished. It may corrupt encrypted data!")
                                .create();
                        dialog.setCancelable(false);
                        mProgressDialog = new WeakReference<>(dialog);
                        return mProgressDialog.get();
                    });
                })
                .setNegativeAction("Cancel", (d, w) -> {
                    d.dismiss();
                })
                .create());
    }

    private void onTextChanged(EditText editText, boolean valid) {
        switch (editText.getId()) {
            case R.id.input_password_new_repeat:
                mNewPassword = editText.getText().toString();
                break;
        }
    }

    private void onFormValidate(boolean valid) {
        getViewState().setEnableSubmit(valid);
    }
}
