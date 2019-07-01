package network.minter.bipwallet.security;

import android.support.annotation.StringRes;

import com.arellomobile.mvp.MvpView;
import com.arellomobile.mvp.viewstate.strategy.OneExecutionStateStrategy;
import com.arellomobile.mvp.viewstate.strategy.StateStrategyType;

import network.minter.bipwallet.internal.mvp.ErrorViewWithRetry;
import network.minter.bipwallet.internal.views.widgets.PinCodeView;

/**
 * minter-android-wallet. 2019
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
public class SecurityModule {
    public final static int MAX_TRIES_UNTIL_LOCK = 5;
    public final static int LOCK_INTERVAL_S = 60 * 1;

    public final static String EXTRA_MODE = "EXTRA_MODE";
    public final static String EXTRA_PIN = "EXTRA_PIN";
    public final static String EXTRA_START_HOME = "EXTRA_START_HOME";

    public enum PinMode {
        Creation,
        Confirmation,
        Validation,
        Deletion;

        public static PinMode fromInt(int ordinal) {
            for (PinMode m : PinMode.values()) {
                if (m.ordinal() == ordinal) {
                    return m;
                }
            }

            return Creation;
        }
    }

    public interface KeypadListener {

    }

    public interface PinPadView extends MvpView, ErrorViewWithRetry {
        void setKeypadListener(KeypadListener listener);
        void setupTitle(@StringRes int title);
        void setEnableValidation(String pin);
        void setPinHint(@StringRes int resId);
        void setOnPinValueListener(PinCodeView.OnValueListener listener);
        @StateStrategyType(OneExecutionStateStrategy.class)
        void startConfirmation(int requestCode, String pin);
        void finishSuccess(boolean startHome);
        void setOnPinValidationError(PinCodeView.OnValidationErrorListener listener);
        void setPinError(CharSequence error);
        void setPinEnabled(boolean enabled);
    }
}
