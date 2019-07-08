package network.minter.bipwallet.settings.contract;

import android.view.View;

import androidx.biometric.BiometricPrompt;
import androidx.recyclerview.widget.RecyclerView;
import moxy.MvpView;
import moxy.viewstate.strategy.OneExecutionStateStrategy;
import moxy.viewstate.strategy.StateStrategyType;
import network.minter.bipwallet.internal.dialogs.WalletDialog;
import network.minter.bipwallet.security.SecurityModule;

/**
 * minter-android-wallet. 2019
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
public
interface SettingsTabView extends MvpView {
    void setOnFreeCoinsClickListener(View.OnClickListener listener);
    void showFreeCoinsButton(boolean show);
    @StateStrategyType(OneExecutionStateStrategy.class)
    void startLogin();
    void setMainAdapter(RecyclerView.Adapter<?> mainAdapter);
    void setAdditionalAdapter(RecyclerView.Adapter<?> additionalAdapter);
    void setSecurityAdapter(RecyclerView.Adapter<?> securityAdapter);
    @StateStrategyType(OneExecutionStateStrategy.class)
    void startAddressList();
    @StateStrategyType(OneExecutionStateStrategy.class)
    void startAvatarChooser();
    @StateStrategyType(OneExecutionStateStrategy.class)
    void startPasswordChange();
    void showMessage(CharSequence message);
    @StateStrategyType(OneExecutionStateStrategy.class)
    void startDialog(WalletDialog.DialogExecutor executor);
    @StateStrategyType(OneExecutionStateStrategy.class)
    void startPinCodeManager(int requestCode, SecurityModule.PinMode mode);
    void startBiometricPrompt(BiometricPrompt.AuthenticationCallback callback);
    void startFingerprintEnrollment();
}
