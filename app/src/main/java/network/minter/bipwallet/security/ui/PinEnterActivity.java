package network.minter.bipwallet.security.ui;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Provider;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricPrompt;
import androidx.fragment.app.Fragment;
import butterknife.BindView;
import butterknife.ButterKnife;
import moxy.presenter.InjectPresenter;
import moxy.presenter.ProvidePresenter;
import network.minter.bipwallet.R;
import network.minter.bipwallet.auth.ui.AuthActivity;
import network.minter.bipwallet.home.ui.HomeActivity;
import network.minter.bipwallet.internal.BaseMvpInjectActivity;
import network.minter.bipwallet.internal.Wallet;
import network.minter.bipwallet.internal.dialogs.WalletDialog;
import network.minter.bipwallet.internal.system.ActivityBuilder;
import network.minter.bipwallet.internal.views.widgets.PinCodeView;
import network.minter.bipwallet.internal.views.widgets.ToolbarProgress;
import network.minter.bipwallet.security.SecurityModule;
import network.minter.bipwallet.security.contract.PinEnterView;
import network.minter.bipwallet.security.views.PinEnterPresenter;

/**
 * minter-android-wallet. 2019
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
public class PinEnterActivity extends BaseMvpInjectActivity implements PinEnterView {

    @Inject Provider<PinEnterPresenter> presenterProvider;
    @InjectPresenter PinEnterPresenter presenter;
    @BindView(R.id.pinpad) PinCodeView pinCode;
    @BindView(R.id.toolbar) ToolbarProgress toolbar;
    private WalletDialog mCurrentDialog = null;

    @Override
    public void setKeypadListener(SecurityModule.KeypadListener listener) {

    }

    @Override
    public void setupTitle(int title) {
        toolbar.setTitle(title);
    }

    @Override
    public void setEnableValidation(String pin) {
        if (pin == null) {
            pinCode.setEnableValidation(false);
        } else {
            pinCode.setEnableValidation(true);
            pinCode.setValue(pin);
        }

    }

    @Override
    public void setPinHint(int resId) {
        pinCode.setPinHint(resId);
    }

    @Override
    public void setOnPinValueListener(PinCodeView.OnValueListener listener) {
        pinCode.setOnValueListener(listener);
    }

    @Override
    public void startConfirmation(int requestCode, String pin) {
        new PinEnterActivity.Builder(this, SecurityModule.PinMode.Confirmation)
                .setPin(pin)
                .start(requestCode);
    }

    @Override
    public void finishSuccess(Intent intent) {
        if (intent != null) {
            startActivityClearTop(this, intent);
            finish();
            return;
        }
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public void setOnPinValidationError(PinCodeView.OnValidationErrorListener listener) {
        pinCode.setOnValidationErrorListener(listener);
    }

    @Override
    public void setPinError(CharSequence error) {
        pinCode.setError(error);
    }

    @Override
    public void setPinError(int errorRes) {
        pinCode.setError(errorRes);
    }

    @Override
    public void setPinEnabled(boolean enabled) {
        pinCode.setEnabled(enabled);
    }

    @Override
    public void resetPin() {
        pinCode.reset();
    }

    @Override
    public void onErrorWithRetry(String errorMessage, View.OnClickListener errorResolver) {

    }

    @Override
    public void onErrorWithRetry(String errorMessage, String actionName, View.OnClickListener errorResolver) {

    }

    @Override
    public void onError(Throwable t) {

    }

    @Override
    public void onError(String err) {

    }

    @Override
    public void startBiometricPrompt(BiometricPrompt.AuthenticationCallback callback) {
        final BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.pin_fp_title_enable))
                .setDescription("")
                .setSubtitle("")
                .setNegativeButtonText(getString(R.string.btn_cancel))
                .build();

        Executor executor = Executors.newSingleThreadExecutor();
        final BiometricPrompt prompt = new BiometricPrompt(this, executor, callback);

        prompt.authenticate(info);
    }

    @Override
    public void finishCancel() {
        finish();
    }

    @Override
    public void startLogin() {

        Toast.makeText(this, R.string.pin_invalid_logout, Toast.LENGTH_LONG).show();


        Wallet.app().cache().clear();
        Intent intent = new Intent(this, AuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(intent);
        finish();
    }

    @Override
    public void setOnFingerprintClickListener(PinCodeView.OnFingerprintClickListener listener) {
        pinCode.setOnFingerprintClickListener(listener);
    }

    @Override
    public void setFingerprintEnabled(boolean enabled) {
        pinCode.setEnableFingerprint(enabled);
    }

    @Override
    public void startDialog(WalletDialog.DialogExecutor executor) {
        mCurrentDialog = WalletDialog.switchDialogWithExecutor(this, mCurrentDialog, executor);
    }

    @ProvidePresenter
    PinEnterPresenter providePresenter() {
        return presenterProvider.get();
    }

    @Override
    protected void onStop() {
        super.onStop();
        WalletDialog.releaseDialog(mCurrentDialog);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin);
        ButterKnife.bind(this);
        setupToolbar(toolbar);

        presenter.handleExtras(getIntent());

        setResult(RESULT_CANCELED);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        presenter.onActivityResult(requestCode, resultCode, data);
    }

    public static final class Builder extends ActivityBuilder {
        private final SecurityModule.PinMode mMode;
        private String mPin = "";
        private boolean mStartHome = false;
        private Intent mSuccessIntent = null;

        public Builder(@NonNull Activity from, SecurityModule.PinMode mode) {
            super(from);
            mMode = mode;
        }

        public Builder(@NonNull Fragment from, SecurityModule.PinMode mode) {
            super(from);
            mMode = mode;
        }

        public Builder(@NonNull Service from, SecurityModule.PinMode mode) {
            super(from);
            mMode = mode;
        }

        public Builder setPin(String pin) {
            mPin = pin;
            return this;
        }

        public Builder setSuccessIntent(Intent intent) {
            mSuccessIntent = intent;
            return this;
        }

        public Builder startHomeOnSuccess() {
            mStartHome = true;
            return this;
        }

        @Override
        protected void onBeforeStart(Intent intent) {
            super.onBeforeStart(intent);
            intent.putExtra(SecurityModule.EXTRA_MODE, mMode.ordinal());
            intent.putExtra(SecurityModule.EXTRA_PIN, mPin);
            if (mStartHome) {
                mSuccessIntent = new Intent(getActivity(), HomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_CLEAR_TASK |
                        Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
            }

            intent.putExtra(SecurityModule.EXTRA_SUCCESS_INTENT, mSuccessIntent);
        }

        @Override
        protected Class<?> getActivityClass() {
            return PinEnterActivity.class;
        }
    }
}
