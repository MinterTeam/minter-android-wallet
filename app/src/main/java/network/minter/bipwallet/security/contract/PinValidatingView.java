package network.minter.bipwallet.security.contract;

import androidx.annotation.StringRes;
import androidx.biometric.BiometricPrompt;
import moxy.MvpView;
import network.minter.bipwallet.internal.views.widgets.PinCodeView;

public interface PinValidatingView extends MvpView {
    void setOnPinValueListener(PinCodeView.OnValueListener listener);
    void setupTitle(@StringRes int title);
    void setPinHint(@StringRes int resId);
    void setFingerprintEnabled(boolean enabled);
    void setOnFingerprintClickListener(PinCodeView.OnFingerprintClickListener listener);
    void startBiometricPrompt(BiometricPrompt.AuthenticationCallback callback);
    void setEnableValidation(String pin);
    void setOnPinValidationError(PinCodeView.OnValidationErrorListener listener);
    void finishSuccess();
    void setPinError(CharSequence error);
    void setPinError(@StringRes int errorRes);
    void setPinEnabled(boolean enabled);
    void resetPin();
    void startLogin();
}
