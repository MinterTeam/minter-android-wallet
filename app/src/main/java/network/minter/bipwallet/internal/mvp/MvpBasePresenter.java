package network.minter.bipwallet.internal.mvp;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.view.View;

import com.arellomobile.mvp.MvpPresenter;
import com.arellomobile.mvp.MvpView;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import network.minter.mintercore.internal.exceptions.NetworkException;
import timber.log.Timber;

/**
 * Stars. 2017
 *
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

    public void handleExtras(Intent intent) {
    }

    public void handleExtras(Bundle bundle) {

    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
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
}
