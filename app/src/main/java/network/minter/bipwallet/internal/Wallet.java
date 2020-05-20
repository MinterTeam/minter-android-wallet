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

package network.minter.bipwallet.internal;

import android.util.Log;

import com.crashlytics.android.Crashlytics;

import org.jetbrains.annotations.NotNull;
import org.joda.time.Seconds;

import java.util.Iterator;
import java.util.Locale;

import javax.inject.Inject;

import androidx.multidex.MultiDexApplication;
import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasAndroidInjector;
import network.minter.bipwallet.BuildConfig;
import network.minter.bipwallet.internal.data.CircularFifoBuffer;
import network.minter.bipwallet.internal.di.DaggerWalletComponent;
import network.minter.bipwallet.internal.di.HelpersModule;
import network.minter.bipwallet.internal.di.RepoModule;
import network.minter.bipwallet.internal.di.WalletComponent;
import network.minter.bipwallet.internal.di.WalletModule;
import network.minter.bipwallet.internal.system.ForegroundDetector;
import network.minter.explorer.MinterExplorerApi;
import timber.log.Timber;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class Wallet extends MultiDexApplication implements HasAndroidInjector {

    public static final Locale LC_EN = Locale.US;
    @SuppressWarnings("ConstantConditions")
    public final static boolean ENABLE_CRASHLYTICS = BuildConfig.FLAVOR.equalsIgnoreCase("netTest") || BuildConfig.FLAVOR.equalsIgnoreCase("netMain");
    protected static WalletComponent app;
    protected static boolean sEnableInject = true;

    static {
        Locale.setDefault(LC_EN);
    }

    @Inject
    DispatchingAndroidInjector<Object> dispatchingAndroidInjector;

    @Inject ForegroundDetector foregroundDetector;

    @SuppressWarnings("ConstantConditions")
    public static String urlExplorerFront() {
        if (BuildConfig.EXPLORER_FRONT_URL != null) {
            return BuildConfig.EXPLORER_FRONT_URL;
        }

        return MinterExplorerApi.FRONT_URL;
    }

    private static CircularFifoBuffer<Integer> sTimeOffsets = new CircularFifoBuffer<>(5);

    public static int timeOffset() {
        if (sTimeOffsets.size() == 0) {
            return 0;
        }

        Iterator<Integer> it = sTimeOffsets.iterator();
        float avgValue = (float) it.next();
        while (it.hasNext()) {
            float v = (float) it.next();
            avgValue = (avgValue + v) / 2f;
        }

        return Seconds.seconds(Math.round(avgValue)).getSeconds();
    }

    public static void setTimeOffset(int diff) {
        sTimeOffsets.push(diff);
    }

    /**
     * Usage:
     * <p>
     * Wallet.app().display().getWidth()
     * Wallet.app().res(); et cetera
     * @return WalletComponent
     * @see WalletComponent
     */
    public static WalletComponent app() {
        return app;
    }

    private static boolean isChinese() {
        return Locale.getDefault() == Locale.CHINA ||
                Locale.getDefault() == Locale.CHINESE ||
                Locale.getDefault() == Locale.SIMPLIFIED_CHINESE ||
                Locale.getDefault() == Locale.TRADITIONAL_CHINESE;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }

        if (ENABLE_CRASHLYTICS) {
            Timber.plant(new CrashlyticsTree());
        }

        Locale.setDefault(LC_EN);


        if (sEnableInject) {
            app = DaggerWalletComponent.builder()
                    .walletModule(new WalletModule(this, BuildConfig.DEBUG, ENABLE_CRASHLYTICS))
                    .helpersModule(new HelpersModule())
                    .repoModule(new RepoModule())
                    .build();

            app.inject(this);
        }

        registerActivityLifecycleCallbacks(foregroundDetector);
        registerComponentCallbacks(foregroundDetector);
    }

    @Override
    public AndroidInjector<Object> androidInjector() {
        return dispatchingAndroidInjector;
    }

    public static final class CrashlyticsTree extends Timber.Tree {
        private static final String CRASHLYTICS_KEY_PRIORITY = "priority";
        private static final String CRASHLYTICS_KEY_TAG = "tag";
        private static final String CRASHLYTICS_KEY_MESSAGE = "message";

        @Override
        protected void log(int priority, String tag, @NotNull String message, Throwable t) {
            if (priority == Log.ERROR || priority == Log.WARN) {
                Crashlytics.setInt(CRASHLYTICS_KEY_PRIORITY, priority);
                Crashlytics.setString(CRASHLYTICS_KEY_TAG, tag);
                Crashlytics.setString(CRASHLYTICS_KEY_MESSAGE, message);

                if (t == null) {
                    Crashlytics.logException(new Exception(message));
                } else {
                    Crashlytics.logException(t);
                }
            }
        }
    }
}
