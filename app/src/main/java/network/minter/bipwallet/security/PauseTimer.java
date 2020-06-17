/*
 * Copyright (C) by MinterTeam. 2020
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

    private final AtomicBoolean mLoggedIn = new AtomicBoolean(false);
    private final AtomicBoolean mRun = new AtomicBoolean(false);
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
                    logout();
                    Timber.d("Logging out...");
                    if (listener != null) {
                        listener.onExit();
                    }
                }, t -> Timber.w(t, "Timer error"));
        INSTANCE.mRun.set(true);
    }

    public static void logout() {
        if (INSTANCE == null) return;
        INSTANCE.mLoggedIn.set(false);
        INSTANCE.mRun.set(false);
        INSTANCE.mTimer = null;
        Timber.d("Logging out...");
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
