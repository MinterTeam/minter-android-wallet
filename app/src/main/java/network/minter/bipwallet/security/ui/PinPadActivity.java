package network.minter.bipwallet.security.ui;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.View;

import com.arellomobile.mvp.presenter.InjectPresenter;
import com.arellomobile.mvp.presenter.ProvidePresenter;

import javax.inject.Inject;
import javax.inject.Provider;

import butterknife.BindView;
import butterknife.ButterKnife;
import network.minter.bipwallet.R;
import network.minter.bipwallet.home.ui.HomeActivity;
import network.minter.bipwallet.internal.BaseMvpInjectActivity;
import network.minter.bipwallet.internal.system.ActivityBuilder;
import network.minter.bipwallet.internal.views.widgets.PinCodeView;
import network.minter.bipwallet.internal.views.widgets.ToolbarProgress;
import network.minter.bipwallet.security.SecurityModule;
import network.minter.bipwallet.security.views.PinCodePresenter;

/**
 * minter-android-wallet. 2019
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
public class PinPadActivity extends BaseMvpInjectActivity implements SecurityModule.PinPadView {

    @Inject Provider<PinCodePresenter> presenterProvider;
    @InjectPresenter PinCodePresenter presenter;
    @BindView(R.id.pinpad) PinCodeView pinCode;
    @BindView(R.id.toolbar) ToolbarProgress toolbar;

    @Override
    public void setKeypadListener(SecurityModule.KeypadListener listener) {

    }

    @Override
    public void setupTitle(int title) {
        toolbar.setTitle(title);
    }

    @Override
    public void setEnableValidation(String pin) {
        pinCode.setValue(pin);
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
        new PinPadActivity.Builder(this, SecurityModule.PinMode.Confirmation)
                .setPin(pin)
                .start(requestCode);
    }

    @Override
    public void finishSuccess(boolean startHome) {
        if (startHome) {
            startActivityClearTop(this, HomeActivity.class);
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
    public void setPinEnabled(boolean enabled) {
        pinCode.setEnabled(enabled);
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

    @ProvidePresenter
    PinCodePresenter providePresenter() {
        return presenterProvider.get();
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
        presenter.onActivityResult(requestCode, resultCode, data);
    }

    public static final class Builder extends ActivityBuilder {
        private final SecurityModule.PinMode mMode;
        private String mPin = "";
        private boolean mStartHome = false;

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

        public Builder startHomeOnSuccess() {
            mStartHome = true;
            return this;
        }

        @Override
        protected void onBeforeStart(Intent intent) {
            super.onBeforeStart(intent);
            intent.putExtra(SecurityModule.EXTRA_MODE, mMode.ordinal());
            intent.putExtra(SecurityModule.EXTRA_PIN, mPin);
            intent.putExtra(SecurityModule.EXTRA_START_HOME, mStartHome);
        }

        @Override
        protected Class<?> getActivityClass() {
            return PinPadActivity.class;
        }
    }
}
