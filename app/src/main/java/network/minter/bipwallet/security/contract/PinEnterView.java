package network.minter.bipwallet.security.contract;

import android.content.Intent;

import androidx.annotation.StringRes;
import androidx.biometric.BiometricPrompt;
import moxy.MvpView;
import moxy.viewstate.strategy.OneExecutionStateStrategy;
import moxy.viewstate.strategy.StateStrategyType;
import network.minter.bipwallet.internal.dialogs.WalletDialog;
import network.minter.bipwallet.internal.mvp.ErrorViewWithRetry;
import network.minter.bipwallet.internal.views.widgets.PinCodeView;
import network.minter.bipwallet.security.SecurityModule;

/**
 * minter-android-wallet. 2019
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
public interface PinEnterView extends MvpView, ErrorViewWithRetry, WalletDialog.DialogContractView {
    void setKeypadListener(SecurityModule.KeypadListener listener);
    void setupTitle(@StringRes int title);
    void setEnableValidation(String pin);
    void setPinHint(@StringRes int resId);
    void setOnPinValueListener(PinCodeView.OnValueListener listener);
    @StateStrategyType(OneExecutionStateStrategy.class)
    void startConfirmation(int requestCode, String pin);
    void finishSuccess(Intent intent);
    void setOnPinValidationError(PinCodeView.OnValidationErrorListener listener);
    void setPinError(CharSequence error);
    void setPinError(@StringRes int errorRes);
    void setPinEnabled(boolean enabled);
    void resetPin();
    void startBiometricPrompt(BiometricPrompt.AuthenticationCallback callback);
    void finishCancel();
    void startLogin();
    void setOnFingerprintClickListener(PinCodeView.OnFingerprintClickListener listener);
    void setFingerprintEnabled(boolean enabled);
}
