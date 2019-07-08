package network.minter.bipwallet.security;

import dagger.Module;

/**
 * minter-android-wallet. 2019
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
@Module
public class SecurityModule {
    public final static int MAX_TRIES_UNTIL_LOCK = 5;
    public final static int LOCK_INTERVAL_S = 60 * 1;

    public final static String EXTRA_MODE = "EXTRA_MODE";
    public final static String EXTRA_PIN = "EXTRA_PIN";
    public final static String EXTRA_SUCCESS_INTENT = "EXTRA_SUCCESS_INTENT";

    public enum PinMode {
        Creation,
        Confirmation,
        Validation,
        Deletion,
        Change,
        EnableFingerprint,
        DisableFingerprint;

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

}
