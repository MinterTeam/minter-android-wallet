/*
 * Copyright (C) 2018 by MinterTeam
 * @link https://github.com/MinterTeam
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

import android.app.Activity;
import android.app.Service;
import android.os.Build;
import android.support.multidex.MultiDexApplication;
import android.support.v7.app.AppCompatDelegate;
import android.util.Log;

import java.util.Locale;

import javax.inject.Inject;

import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasActivityInjector;
import dagger.android.HasServiceInjector;
import io.reactivex.functions.Consumer;
import io.reactivex.plugins.RxJavaPlugins;
import network.minter.bipwallet.BuildConfig;
import network.minter.bipwallet.internal.di.DaggerWalletComponent;
import network.minter.bipwallet.internal.di.HelpersModule;
import network.minter.bipwallet.internal.di.RepoModule;
import network.minter.bipwallet.internal.di.WalletComponent;
import network.minter.bipwallet.internal.di.WalletModule;
import network.minter.bipwallet.internal.mvp.ErrorView;
import network.minter.bipwallet.internal.mvp.ProgressView;
import network.minter.mintercore.internal.exceptions.NetworkException;
import timber.log.Timber;

import static network.minter.bipwallet.internal.common.Preconditions.firstNonNull;

/**
 * MinterWallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
//@ParcelClasses({
//        @ParcelClass(MyAddressData.class),
//        @ParcelClass(MinterAddress.class),
//        @ParcelClass(BytesData.class),
//        @ParcelClass(EncryptedString.class),
//})
public class Wallet extends MultiDexApplication implements HasActivityInjector, HasServiceInjector {

    public static final Locale LC_EN = Locale.US;
    public static final Locale LC_RU = new Locale("ru", "RU");
    private static WalletComponent app;

    static {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        }
    }

    @Inject
    DispatchingAndroidInjector<Activity> dispatchingActivityInjector;
    @Inject
    DispatchingAndroidInjector<Service> dispatchingServiceInjector;

    /**
     * Usage:
     * <p>
     * Wallet.app().display().getWidth()
     * Wallet.app().res(); et cetera
     *
     * @return
     * @see WalletComponent
     */
    public static WalletComponent app() {
        return app;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Timber.plant(new Timber.DebugTree());

        Rx.init();

        Locale.setDefault(LC_EN);

        app = DaggerWalletComponent.builder()
                .walletModule(new WalletModule(this, BuildConfig.DEBUG, false))
                .helpersModule(new HelpersModule())
                .repoModule(new RepoModule())
                .build();

        app.inject(this);
    }

    @Override
    public AndroidInjector<Activity> activityInjector() {
        return dispatchingActivityInjector;
    }

    @Override
    public AndroidInjector<Service> serviceInjector() {
        return dispatchingServiceInjector;
    }

    public static class Rx {

        public static void init() {
            RxJavaPlugins.setErrorHandler(errorHandler("Unhandled Rx exception!"));
        }

        /**
         * Просто пишет ошибку в лог
         */
        public static Consumer<Throwable> errorHandler() {
            return throwable -> Timber
                    .e(NetworkException.convertIfNetworking(throwable), "Unexpected error");
        }

        /**
         * Просто пишет ошибку в лог
         */
        public static Consumer<Throwable> errorHandler(String message) {
            return throwable -> Timber.e(NetworkException.convertIfNetworking(throwable), message);
        }

        /**
         * Если контекст является ErrorView то выведет ошибку в виде попапа или в человеческом виде
         * При этом запишет ошибку в лог
         */
        public static Consumer<Throwable> errorHandler(final Object viewContext) {
            return throwable -> {
                Throwable ex = NetworkException.convertIfNetworking(throwable);
                if (viewContext instanceof ProgressView) {
                    ((ProgressView) viewContext).hideProgress();
                }
                if (viewContext instanceof ErrorView) {
                    if (ex instanceof NetworkException) {
                        ((ErrorView) viewContext).onError(((NetworkException) ex).getUserMessage());
                    } else {
                        ((ErrorView) viewContext).onError(ex);
                    }

                }
                Timber.e(ex, "Error occurred %s", ex.getMessage());
            };
        }

        /**
         * Если контекст является ErrorView то выведет ошибку в виде попапа или в человеческом виде
         * При этом запишет ошибку в лог
         */
        public static Consumer<Throwable> errorChain(final Object viewContext,
                                                     final Consumer<Throwable> tAction) {
            return throwable -> {
                Throwable ex = NetworkException.convertIfNetworking(throwable);
                if (viewContext instanceof ProgressView) {
                    ((ProgressView) viewContext).hideProgress();
                }
                if (viewContext instanceof ErrorView) {
                    if (ex instanceof NetworkException) {
                        ((ErrorView) viewContext).onError(((NetworkException) ex).getUserMessage());
                    } else {
                        ((ErrorView) viewContext).onError(ex);
                    }
                }

                Timber.e(ex, "Error occurred");
                if (tAction != null) {
                    tAction.accept(throwable);
                }
            };
        }

        /**
         * Выведет человеческую ошибку и запишет ее в лог
         *
         * @param message Если передать NULL то ошибка не выведется
         */
        public static Consumer<Throwable> errorHandler(final Object viewContext, final String message) {
            return throwable -> {
                Throwable ex = NetworkException.convertIfNetworking(throwable);
                if (viewContext instanceof ProgressView) {
                    ((ProgressView) viewContext).hideProgress();
                }
                if (viewContext instanceof ErrorView && message != null) {
                    ((ErrorView) viewContext).onError(message);
                }
                Timber.e(ex, "Error occurred: %s", firstNonNull(message, "[suppressed message]"));
            };
        }

        /**
         * Выведет человеческую ошибку и запишет ее в лог
         */
        public static Consumer<Throwable> errorChain(final Object viewContext, final String message,
                                                     Consumer<Throwable> tAction) {
            return throwable -> {
                Throwable ex = NetworkException.convertIfNetworking(throwable);
                if (viewContext instanceof ProgressView) {
                    ((ProgressView) viewContext).hideProgress();
                }
                if (viewContext instanceof ErrorView && message != null) {
                    ((ErrorView) viewContext).onError(message);
                }
                Timber.e(ex, "Error occurred: %s", message);
                if (tAction != null) {
                    tAction.accept(throwable);
                }
            };
        }
    }

    public class CrashlyticsTree extends Timber.Tree {
        private static final String CRASHLYTICS_KEY_PRIORITY = "priority";
        private static final String CRASHLYTICS_KEY_TAG = "tag";
        private static final String CRASHLYTICS_KEY_MESSAGE = "message";

        @Override
        protected void log(int priority, String tag, String message, Throwable t) {
            if (priority != Log.ERROR) {
                return;
            }

//            Crashlytics.setInt(CRASHLYTICS_KEY_PRIORITY, priority);
//            Crashlytics.setString(CRASHLYTICS_KEY_TAG, tag);
//            Crashlytics.setString(CRASHLYTICS_KEY_MESSAGE, message);
//
            if (t == null) {
//                Crashlytics.logException(new Exception(message));
            } else {
//                Crashlytics.logException(t);
            }
        }
    }
}
