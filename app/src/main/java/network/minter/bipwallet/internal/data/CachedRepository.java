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

package network.minter.bipwallet.internal.data;

import android.support.annotation.CallSuper;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import timber.log.Timber;

/**
 * Wallet. 2017
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class CachedRepository<ResultModel, Entity extends CachedEntity<ResultModel>> {

    public final static int DEFAULT_EXPIRE_TIME = 30; //30 seconds
    protected final static Scheduler THREAD_MAIN = AndroidSchedulers.mainThread();
    protected final static Scheduler THREAD_IO = Schedulers.io();
    /**
     * Observer will call onNext(Action<ResultModel>) all time, even data is not expired and is
     * ready
     */
    protected final static int NOTIFY_ALWAYS_IF_EXISTS = 0;
    /**
     * Observer will call onNext(Action<ResultModel>) only if data is expired or force updated
     */
    protected final static int NOTIFY_ONLY_ON_UPDATE = 1;
    private final Entity mEntity;
    ResultModel mData;
    boolean mDataIsReady = false;
    Date mExpiredAt = null;
    Date mLastUpdateTime = null;
    private BehaviorSubject<MetaResult<ResultModel>> mMetaNotifier = BehaviorSubject.create();
    private BehaviorSubject<ResultModel> mNotifier = BehaviorSubject.create();
    private CompositeDisposable subscriptions = new CompositeDisposable();

    public CachedRepository(Entity entity) {
        mEntity = entity;
    }

    protected ResultModel initialData() {
        return mEntity.initialData();
    }

    public Class<Entity> getEntityClass() {
        return (Class<Entity>) mEntity.getClass();
    }

    public Entity getEntity() {
        return mEntity;
    }

    /**
     * @return Raw observable data
     */
    public Observable<ResultModel> getUpdateObservable() {
        return mEntity.getUpdatableData()
                .doOnNext(d -> {
                    Timber.tag(getClass().getSimpleName()).d(
                            "UpdateInternal: getUpdateObservable().onNext");
                    setData(d);
                })
                .doOnError(this::notifyOnError)
                .subscribeOn(THREAD_IO)
                .observeOn(THREAD_MAIN);
    }

    /**
     * Called after data updated
     */
    @CallSuper
    public void onAfterUpdate(ResultModel result) {
        mEntity.onAfterUpdate(result);
    }

    /**
     * Clear data and set data expired
     */
    public void clear() {
        mData = null;
        mDataIsReady = false;
        expire();
    }

    /**
     * Observer for update event
     *
     * @return obserable with ResultModel
     * @see ResultModel
     * @see Observable
     */
    public Observable<ResultModel> onUpdate() {
        return mNotifier
                .observeOn(THREAD_MAIN)
                .subscribeOn(THREAD_IO);
    }

    /**
     * Check data for expiration
     *
     * @return true if data is expired
     */
    public boolean isExpired() {
        if (mExpiredAt == null) {
            return true;
        }

        final Date now = new Date();
        final Date entityUpdatedAt = mExpiredAt;

        final boolean expired = now.getTime() >= entityUpdatedAt.getTime();
        if (expired) {
            onExpired();
        }

        return expired;
    }

    /**
     * Make data not expired
     */
    public void invalidateTime() {
        mLastUpdateTime = mExpiredAt = new Date();
        int expireTime = getExpireTime();
        expireTime *= 1000;
        mExpiredAt.setTime(mExpiredAt.getTime() + expireTime);
    }

    /**
     * Returns seconds to object life (ttl)
     *
     * @return integer seconds
     */
    public int getExpireTime() {
        return DEFAULT_EXPIRE_TIME;
    }

    /**
     * Make data expired
     */
    public void expire() {
        mLastUpdateTime = mExpiredAt = null;
    }

    @Nullable
    public Date getLastUpdate() {
        return mLastUpdateTime;
    }

    public void unsubscribe() {
        subscriptions.dispose();
        subscriptions.clear();
        mNotifier.onComplete();
        mNotifier = BehaviorSubject.create();
        mMetaNotifier.onComplete();
        mMetaNotifier = BehaviorSubject.create();
    }

    /**
     * @return True if data has set and not expired and not null
     */
    public boolean isDataReady() {
        return mDataIsReady && !isExpired() && getData() != null;
    }

    /**
     * Returns updatable data
     *
     * @return null if not loaded
     */
    public ResultModel getData() {
        return mData;
    }

    /**
     * Setting data
     *
     * @param data Any
     */
    @CallSuper
    public void setData(ResultModel data) {
        setDataInternal(data);
        onAfterUpdate(data);
    }

    /**
     * @param first
     * @return
     */
    public Observable<ResultModel> updateAfter(CachedRepository<ResultModel, Entity> first) {
        return updateAfter(first.mEntity.getUpdatableData());
    }

    /**
     * Creates zip function and after update first value, returns current object value
     *
     * @param first Any observable
     * @param <T>   Any type
     * @return Observable
     */
    public <T> Observable<ResultModel> updateAfter(Observable<T> first) {
        return Observable
                .zip(first, mEntity.getUpdatableData(), (firstT, resultModel) -> resultModel)
                .observeOn(THREAD_MAIN)
                .subscribeOn(THREAD_IO);
    }

    /**
     * @return
     * @see #observe()
     * @see #observeWithMeta()
     */
    @Deprecated
    public Observable<ResultModel> getOrUpdate() {
        if (isDataReady()) {
            return Observable.just(getData())
                    .observeOn(THREAD_MAIN)
                    .subscribeOn(THREAD_MAIN);
        }

        update();
        return onUpdate();
    }

    public void update() {
        updateInternal(false, null, null, null);
    }

    public void update(boolean force) {
        updateInternal(force, null, null, null);
    }

    public void update(boolean force, Consumer<ResultModel> onNext) {
        updateInternal(force, onNext, null, null);
    }

    public void update(boolean force, Consumer<ResultModel> onNext, Consumer<Throwable> onError) {
        updateInternal(force, onNext, onError, null);
    }

    public void update(boolean force, Consumer<ResultModel> onNext, Consumer<Throwable> onError,
                       Action onComplete) {
        updateInternal(force, onNext, onError, onComplete);
    }

    public void update(Consumer<ResultModel> onNext) {
        updateInternal(false, onNext, null, null);
    }

    public void update(Consumer<ResultModel> onNext, Consumer<Throwable> onError) {
        updateInternal(false, onNext, onError, null);
    }

    public void update(Consumer<ResultModel> onNext, Consumer<Throwable> onError,
                       Action onComplete) {
        updateInternal(false, onNext, onError, onComplete);
    }

    public void updateDelayed(int time, TimeUnit unit) {
        updateDelayedInternal(time, unit, false, null, null, null);
    }

    public void updateDelayed(int time, TimeUnit unit, boolean force) {
        updateDelayedInternal(time, unit, force, null, null, null);
    }

    public void updateDelayed(int time, TimeUnit unit, boolean force,
                              Consumer<ResultModel> onNext) {
        updateDelayedInternal(time, unit, force, onNext, null, null);
    }

    public void updateDelayed(int time, TimeUnit unit, boolean force, Consumer<ResultModel> onNext,
                              Consumer<Throwable> onError) {
        updateDelayedInternal(time, unit, force, onNext, onError, null);
    }

    public void updateDelayed(int time, TimeUnit unit, boolean force, Consumer<ResultModel> onNext,
                              Consumer<Throwable> onError, Action onComplete) {
        updateDelayedInternal(time, unit, force, onNext, onError, onComplete);
    }

    public void updateDelayed(int time, TimeUnit unit, Consumer<ResultModel> onNext) {
        updateDelayedInternal(time, unit, false, onNext, null, null);
    }

    public void updateDelayed(int time, TimeUnit unit, Consumer<ResultModel> onNext,
                              Consumer<Throwable> onError) {
        updateDelayedInternal(time, unit, false, onNext, onError, null);
    }

    public void updateDelayed(int time, TimeUnit unit, Consumer<ResultModel> onNext,
                              Consumer<Throwable> onError, Action onComplete) {
        updateDelayedInternal(time, unit, false, onNext, onError, onComplete);
    }

    /**
     * Return subscriber that subscribes to all updates of this repository including some additional
     * information about data
     * Every time, consumer will call #update(), subscriber will notified about updating
     *
     * @return ReplaySubject
     * @see MetaResult
     */
    public BehaviorSubject<MetaResult<ResultModel>> observeWithMeta() {
        return mMetaNotifier;
    }

    /**
     * Return subscriber that subscribes to all updates of this repository
     * Every time, consumer will call #update(), subscriber will notified about updating
     *
     * @return BehaviorSubject
     */
    public BehaviorSubject<ResultModel> observe() {
        return mNotifier;
    }

    final void setIsReadyInternal(boolean isReady) {
        mDataIsReady = isReady;
    }

    final void setDataInternal(ResultModel data) {
        mData = data;
        mDataIsReady = true;
        notifyOnSuccess(true);
    }

    protected void onExpired() {
    }

    @NotifyStrategy
    protected int getNotifyStrategy() {
        return NOTIFY_ALWAYS_IF_EXISTS;
    }

    protected synchronized CachedRepository<ResultModel, Entity> updateDelayedInternal(
            int delay, TimeUnit unit,
            boolean force,
            @Nullable Consumer<ResultModel> onNext,
            @Nullable Consumer<Throwable> onError,
            @Nullable Action onComplete) {

        Disposable sub = Observable.timer(delay, unit)
                .subscribeOn(THREAD_IO)
                .subscribe(
                        est -> updateInternal(force, onNext, onError, onComplete));

        subscriptions.add(sub);

        return this;
    }

    protected synchronized CachedRepository<ResultModel, Entity> updateInternal(
            boolean force,
            @Nullable Consumer<ResultModel> onSuccess,
            @Nullable Consumer<Throwable> onError,
            @Nullable Action onComplete
    ) {
        Observable<ResultModel> observable;

        if (!force && !isExpired() && isDataReady()) {
            if (getNotifyStrategy() == NOTIFY_ONLY_ON_UPDATE) {
                return this;
            }

            observable = Observable.just(getData())
                    .doOnNext(d -> {
                        Timber.tag(getClass().getSimpleName()).d("UpdateInternal: just().onNext (thread: %s)",
                                Thread.currentThread().getName());
                    })
                    .doOnError(this::notifyOnError)
                    .subscribeOn(THREAD_MAIN)
                    .observeOn(THREAD_MAIN);
        } else {
            observable = getUpdateObservable();
            invalidateTime();
        }

        observable
                .subscribe(
                        res -> {
                            Timber.d("OnNext with data: %s", res.toString());
                            callOnNext(onSuccess).accept(res);
                        },
                        callOnError(onError),
                        callOnComplete(onComplete)
                );

        return this;
    }

    protected void notifyOnSuccess(boolean isNew) {
        mMetaNotifier.onNext(new MetaResult<>(getData(), isNew));
        mNotifier.onNext(getData());
    }

    protected void notifyOnError(Throwable t) {
        mMetaNotifier.onError(t);
        mMetaNotifier = BehaviorSubject.create();
        mNotifier.onError(t);
        mNotifier = BehaviorSubject.create();
    }

    private Consumer<ResultModel> callOnNext(Consumer<ResultModel> chain) {
        return r -> {
            if (chain != null) {
                chain.accept(getData());
            }
        };
    }

    private Consumer<Throwable> callOnError(Consumer<Throwable> chain) {
        return t -> {
            if (chain != null) {
                chain.accept(t);
            }
        };
    }

    private Action callOnComplete(Action chain) {
        return () -> {
            if (chain != null) {
                chain.run();
            }
        };
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({NOTIFY_ONLY_ON_UPDATE, NOTIFY_ALWAYS_IF_EXISTS})
    @interface NotifyStrategy {
    }

    public final static class MetaResult<ResultModel> {
        private ResultModel data;
        private boolean isNew = false;

        MetaResult(ResultModel data, boolean isNew) {
            this.data = data;
            this.isNew = isNew;
        }

        public ResultModel value() {
            return data;
        }

        public boolean isNewData() {
            return isNew;
        }

        @Override
        public String toString() {
            return data.toString();
        }

        public Observable<MetaResult<ResultModel>> toObservable() {
            return Observable.just(this);
        }
    }

}
