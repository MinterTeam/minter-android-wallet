/*
 * Copyright (C) by MinterTeam. 2018
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
package network.minter.bipwallet.internal.mvp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import androidx.annotation.CallSuper;
import androidx.core.util.Pair;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import moxy.MvpPresenter;
import moxy.MvpView;
import network.minter.bipwallet.analytics.AnalyticsManager;
import network.minter.bipwallet.analytics.AppEvent;
import network.minter.bipwallet.analytics.base.HasAnalyticsEvent;
import network.minter.bipwallet.analytics.base.HasAnalyticsEventWithBundle;
import network.minter.bipwallet.analytics.base.HasAnalyticsEventWithId;
import network.minter.bipwallet.analytics.base.HasAnalyticsEventWithParams;
import network.minter.bipwallet.analytics.base.HasAnalyticsMultipleEvents;
import network.minter.bipwallet.internal.Wallet;
import network.minter.core.internal.exceptions.NetworkException;
import timber.log.Timber;

/**
 * Stars. 2017
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public abstract class MvpBasePresenter<V extends MvpView> extends MvpPresenter<V> {

    private CompositeDisposable mSubscriptions = new CompositeDisposable();
    private PublishSubject<Object> mErrorRetryNotifier;
    private boolean mIsInitialized = false;
    private boolean mRetried = false;
    private boolean mEventBusSubscribed = false;

    public MvpBasePresenter<V> unsubscribeOnDestroy(Disposable subscription) {
        mSubscriptions.add(subscription);
        return this;
    }

    public void retry() {
        doOnErrorResolve();
        if (mErrorRetryNotifier.hasComplete()) {
            mErrorRetryNotifier = PublishSubject.create();
            mErrorRetryNotifier.doOnSubscribe(this::unsubscribeOnDestroy);
        }
        mErrorRetryNotifier.onNext(new Object());
        mRetried = true;
    }

    public void onSaveInstanceState(Bundle outState) {
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
    }

    public View.OnClickListener retryOnClick() {
        return v -> retry();
    }

    @CallSuper
    @Override
    public void attachView(V view) {
        mErrorRetryNotifier = PublishSubject.create();
        mErrorRetryNotifier.doOnSubscribe(this::unsubscribeOnDestroy);

        if (!mIsInitialized) {
            mIsInitialized = true;
            handleAnalytics();
        }

        super.attachView(view);
    }

    @CallSuper
    @Override
    public void detachView(V view) {
        super.detachView(view);
    }

    @CallSuper
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (!mSubscriptions.isDisposed()) {
            mSubscriptions.dispose();
        }
    }

    public void onTrimMemory(int level) {
    }

    public void onLowMemory() {
    }

    public void handleExtras(Intent intent) {
    }

    public void handleExtras(Bundle bundle) {

    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    }

    protected AnalyticsManager getAnalytics() {
        return Wallet.app().analytics();
    }

    protected void doOnErrorResolve() {
        if (getViewState() instanceof ProgressView) {
            ((ProgressView) getViewState()).showProgress();
        }
    }

    protected void doOnError(Throwable t) {
        if (getViewState() instanceof ProgressView) {
            Observable.just(t)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(r -> ((ProgressView) getViewState()).hideProgress());

        }

        if (getViewState() instanceof ErrorViewWithRetry) {
            Observable.just(t)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(r -> {
                        String exceptionMessage = t.getMessage();
                        if (t instanceof NetworkException) {
                            exceptionMessage = ((NetworkException) t).getUserMessage();
                        }
                        ((ErrorViewWithRetry) getViewState()).onErrorWithRetry(String.format("%s", exceptionMessage),
                                retryOnClick());
                    });
        }
    }

    protected <T> Observable<T> safeSubscribe(Observable<T> input) {
        return input.retryWhen(getErrorResolver());
    }

    protected <T> Observable<T> safeSubscribeComputeToUi(Observable<T> input) {
        return input.retryWhen(getErrorResolver())
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread());
    }

    protected <T> Observable<T> safeSubscribeIoToUi(Observable<T> input) {
        return input.retryWhen(getErrorResolver())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    protected Function<Observable<? extends Throwable>, ObservableSource<?>> getErrorResolver() {
        return observable -> observable.flatMap(err -> {
            Throwable t = NetworkException.convertIfNetworking(err);
            Timber.w(t, "Error occurred in %s", getClass().getName());
            doOnError(t);

            if (mErrorRetryNotifier == null) {
                mErrorRetryNotifier = PublishSubject.create();
                mErrorRetryNotifier.onNext(new Object());
            }
            return Single
                    .fromObservable(mErrorRetryNotifier.take(1))
                    .delay(500, TimeUnit.MILLISECONDS)
                    .toObservable()
                    .doOnSubscribe(this::unsubscribeOnDestroy);
        });
    }

    private void handleAnalytics() {
        final AnalyticsManager analytics = getAnalytics();
        if (this instanceof HasAnalyticsEvent) {
            analytics.send(((HasAnalyticsEvent) this).getAnalyticsEvent());
        }
        if (this instanceof HasAnalyticsEventWithId) {
            final Pair<AppEvent, Integer> eventWithId = ((HasAnalyticsEventWithId) this).getAnalyticsEventWithId();
            analytics.send(eventWithId.first, eventWithId.second);
        }
        if (this instanceof HasAnalyticsEventWithBundle) {
            final Pair<AppEvent, Bundle> eventWithId = ((HasAnalyticsEventWithBundle) this).getAnalyticsEventWithBundle();
            analytics.send(eventWithId.first, eventWithId.second);
        }
        if (this instanceof HasAnalyticsEventWithParams) {
            final Pair<AppEvent, Map<String, Object>> eventWithId = ((HasAnalyticsEventWithParams) this).getAnalyticsEventWithParams();
            analytics.send(eventWithId.first, eventWithId.second);
        }
        if (this instanceof HasAnalyticsMultipleEvents) {
            for (AppEvent event : ((HasAnalyticsMultipleEvents) this).getAnalyticsEvents()) {
                analytics.send(event);
            }
        }
    }
}
