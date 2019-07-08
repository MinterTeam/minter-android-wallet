package network.minter.bipwallet.security.views;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricPrompt;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import moxy.InjectViewState;
import network.minter.bipwallet.R;
import network.minter.bipwallet.advanced.repo.SecretStorage;
import network.minter.bipwallet.internal.Wallet;
import network.minter.bipwallet.internal.helpers.PrefKeys;
import network.minter.bipwallet.internal.helpers.TimeHelper;
import network.minter.bipwallet.internal.mvp.MvpBasePresenter;
import network.minter.bipwallet.internal.storage.KVStorage;
import network.minter.bipwallet.security.SecurityModule;
import network.minter.bipwallet.security.contract.PinEnterView;
import timber.log.Timber;

/**
 * minter-android-wallet. 2019
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
@InjectViewState
public class PinEnterPresenter extends MvpBasePresenter<PinEnterView> {
    private final static int REQUEST_PIN_CONFIRM = 1010;
    private final static String PREF_PIN_INVALID_TYPE_COUNT = "pin_type_invalid_type_count";
    private final static String PREF_PIN_INVALID_TYPE_TIME = "pin_type_invalid_type_time";
    @Inject SecretStorage storage;
    @Inject KVStorage kvStorage;
    @Inject SharedPreferences prefs;
    private SecurityModule.PinMode mMode;
    private Intent mSuccessIntent = null;
    private String mPin = "";
    private String mSourcePin = "";
    private Disposable mWaitUnlock;

    @Inject
    public PinEnterPresenter() {

    }

    private void init() {
        if (storage.hasPinCode()) {
            mSourcePin = storage.getPinCode();
        }

        getViewState().setOnPinValueListener(this::onPinEntered);

        if (isPinLocked()) {
            setPinErrorLocked();
            startLockTimeUpdate();
        }

        switch (mMode) {
            case Creation:
                getViewState().setupTitle(R.string.title_pin_set);
                getViewState().setPinHint(R.string.hint_pin_enter);
                break;
            case Confirmation:
//                getViewState().setupTitle(R.string.title_pin_repeat);
                getViewState().setPinHint(R.string.hint_pin_repeat);
                getViewState().setEnableValidation(mSourcePin);
                break;
            case EnableFingerprint:
            case DisableFingerprint:
            case Validation:
                if (prefs.getBoolean(PrefKeys.ENABLE_FP, false) && Wallet.app().fingerprint().hasEnrolledFingerprints()) {
                    getViewState().setFingerprintEnabled(true);
                    getViewState().setOnFingerprintClickListener(this::onClickFingerprintButton);
                    startFingerprintValidation();
                }
                getViewState().setOnPinValidationError(this::onValidationError);
            case Deletion:
                getViewState().setupTitle(R.string.title_pin_enter);
                getViewState().setPinHint(R.string.hint_pin_enter);
                getViewState().setEnableValidation(mSourcePin);
                break;
        }
    }

    @Override
    public void handleExtras(Intent intent) {
        super.handleExtras(intent);
        mMode = SecurityModule.PinMode.fromInt(intent.getIntExtra(SecurityModule.EXTRA_MODE, SecurityModule.PinMode.Creation.ordinal()));
        mSourcePin = intent.getStringExtra(SecurityModule.EXTRA_PIN);
        mSuccessIntent = intent.getParcelableExtra(SecurityModule.EXTRA_SUCCESS_INTENT);

        init();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_PIN_CONFIRM) {
            if (mMode == SecurityModule.PinMode.Creation && resultCode == Activity.RESULT_OK) {
                getViewState().finishSuccess(mSuccessIntent);
            }
        }
    }

    private void startFingerprintValidation() {
        getViewState().startBiometricPrompt(new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                getViewState().finishSuccess(mSuccessIntent);
            }
        });
    }

    private void onClickFingerprintButton(View view) {
        startFingerprintValidation();
    }

    private void startLockTimeUpdate() {
        mWaitUnlock = Observable.interval(1, TimeUnit.SECONDS)
                .doOnSubscribe(d -> unsubscribeOnDestroy(mWaitUnlock))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(done -> {
                    if (!isPinLocked()) {
                        getViewState().setPinError(null);
                        getViewState().setPinEnabled(true);
                        kvStorage.put(PREF_PIN_INVALID_TYPE_TIME, 0L);
                        kvStorage.put(PREF_PIN_INVALID_TYPE_COUNT, 0);
                        mWaitUnlock.dispose();
                        mWaitUnlock = null;
                    } else {
                        setPinErrorLocked();
                    }
                }, t -> {

                });
    }

    private long getLockSecondsLeft() {
        long timestamp = System.currentTimeMillis() / 1000;
        Long firstInvalidTime = kvStorage.get(PREF_PIN_INVALID_TYPE_TIME, 0L);
        return ((firstInvalidTime + SecurityModule.LOCK_INTERVAL_S) - timestamp);
    }

    private String getLockDurationLeft() {
        return TimeHelper.formatDuration(getLockSecondsLeft());
    }

    private boolean isPinLocked() {
        return getLockSecondsLeft() > 0;
    }

    private void setPinErrorLocked() {
        getViewState().setPinError(Wallet.app().res().getString(R.string.error_pin_locked, getLockDurationLeft(), kvStorage.get(PREF_PIN_INVALID_TYPE_COUNT, 0)));
        getViewState().setPinEnabled(false);
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
//            setPinErrorLocked();
//            startLockTimeUpdate();
            logout();
            return;
        }

        getViewState().setPinError(Wallet.app().res().getString(R.string.error_pin_invalid, SecurityModule.MAX_TRIES_UNTIL_LOCK - invalidCount));
    }

    private void logout() {
        Wallet.app().session().logout();
        Wallet.app().secretStorage().destroy();
        Wallet.app().storage().deleteAll();
        Wallet.app().prefs().edit().clear().apply();
        getViewState().startLogin();
    }

    private void onPinEntered(String value, int len, boolean valid) {
        if (len == 4) {
            mPin = value;
        } else {
            mPin = "";
        }

        if (mMode == SecurityModule.PinMode.Creation) {
            if (len == 4) {
                getViewState().resetPin();
                Timber.d("PIN entered");
                mMode = SecurityModule.PinMode.Confirmation;
                mSourcePin = value;
                init();
            }
        } else if (mMode == SecurityModule.PinMode.Confirmation) {
            if (valid && len == 4) {
                getViewState().setPinEnabled(false);
                storage.setPinCode(mPin);
                prefs.edit().putBoolean(PrefKeys.ENABLE_PIN_CODE, true).commit();
                Timber.d("PIN confirmed");
                getViewState().finishSuccess(mSuccessIntent);
            }

            if (len == 4 && !valid) {
                getViewState().setPinError(R.string.error_pin_invalid_simple);
            }
        } else if (mMode == SecurityModule.PinMode.Validation) {
            if (valid && len == 4) {
                getViewState().setPinEnabled(false);
                Timber.d("PIN validated");
                kvStorage.put(PREF_PIN_INVALID_TYPE_TIME, 0L);
                kvStorage.put(PREF_PIN_INVALID_TYPE_COUNT, 0);
                getViewState().finishSuccess(mSuccessIntent);
            }
        } else if (mMode == SecurityModule.PinMode.Deletion) {
            if (valid && len == 4) {
                storage.removePinCode();
                prefs.edit().remove(PrefKeys.ENABLE_PIN_CODE).commit();
                Timber.d("PIN removed");
                getViewState().finishSuccess(mSuccessIntent);
            }

            if (len == 4 && !valid) {
                getViewState().setPinError(R.string.error_pin_invalid_simple);
            }
        } else if (mMode == SecurityModule.PinMode.EnableFingerprint) {
            if (valid && len == 4) {
                getViewState().setPinEnabled(false);

            }
        } else if (mMode == SecurityModule.PinMode.DisableFingerprint) {
            if (valid) {
                getViewState().setPinEnabled(false);
                prefs.edit().putBoolean(PrefKeys.ENABLE_FP, false).commit();
                getViewState().finishSuccess(mSuccessIntent);
            }
        } else if (mMode == SecurityModule.PinMode.Change) {

        }
    }
}
