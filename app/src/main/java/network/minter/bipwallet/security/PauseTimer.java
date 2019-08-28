package network.minter.bipwallet.security;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import network.minter.bipwallet.internal.Wallet;
import timber.log.Timber;

public class PauseTimer {
    private final static PauseTimer INSTANCE = new PauseTimer();

    private AtomicBoolean mLoggedIn = new AtomicBoolean(true);
    private AtomicBoolean mRun = new AtomicBoolean(false);
    private Disposable mTimer;

    public static boolean isLoggedIn() {
        return INSTANCE.mLoggedIn.get();
    }

    public static void onPause(OnExitListener listener) {
        if (!Wallet.app().secretStorage().hasPinCode()) return;

        if (INSTANCE.mRun.get()) {
            return;
        }

        Timber.d("Start timer");
        INSTANCE.mTimer = Observable.timer(30, TimeUnit.SECONDS)
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .subscribe(res -> {
                    INSTANCE.mLoggedIn.set(false);
                    INSTANCE.mRun.set(false);
                    INSTANCE.mTimer = null;
                    Timber.d("Logging out...");
                    if (listener != null) {
                        listener.onExit();
                    }
                });
        INSTANCE.mRun.set(true);
    }

    public static void onResume() {
        if (!Wallet.app().secretStorage().hasPinCode()) return;

        if (!INSTANCE.mRun.get()) {
            return;
        }

        Timber.d("Stop timer..");

        destroy();
    }

    public static void destroy() {
        if (!Wallet.app().secretStorage().hasPinCode()) return;

        if (INSTANCE.mTimer != null) {
            INSTANCE.mTimer.dispose();
            INSTANCE.mTimer = null;
        }
        INSTANCE.mRun.set(false);
        INSTANCE.mLoggedIn.set(true);
    }

    public interface OnExitListener {
        void onExit();
    }
}
