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
package network.minter.bipwallet.internal.data

import androidx.annotation.CallSuper
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Action
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import network.minter.bipwallet.BuildConfig
import network.minter.bipwallet.internal.storage.KVStorage
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
open class CachedRepository<ResultModel, Entity : CachedEntity<ResultModel>>(
        val storage: KVStorage,
        val entity: Entity
) {
    companion object {
        const val DEFAULT_EXPIRE_TIME = 30 //30 seconds
        protected val THREAD_MAIN: Scheduler = AndroidSchedulers.mainThread()
        protected val THREAD_IO = Schedulers.io()

        /**
         * Observer will call onNext(Action<ResultModel>) only if data is expired or force updated
         */
        protected const val NOTIFY_ONLY_ON_UPDATE = 1
        protected const val NOTIFY_ALWAYS_IF_EXISTS = 0

        private const val KEY_EXPIRED_ITEM = BuildConfig.MINTER_STORAGE_VERS + "_expire_time_item_"
    }

    //    protected var mData: ResultModel
//    protected var mDataIsReady: Boolean
    protected var _expiredAt: Date? = null
    protected var lastUpdate: Date? = null
    private var mMetaNotifier: BehaviorSubject<MetaResult<ResultModel>> = BehaviorSubject.create()
    private var mNotifier: BehaviorSubject<ResultModel> = BehaviorSubject.create()
    private val subscriptions = CompositeDisposable()

    private val expiredEntityStorageKey: String
        get() {
            return KEY_EXPIRED_ITEM + entity.getDataKey()
        }

    /**
     * Observer will call onNext(Action<ResultModel>) all time, even data is not expired and is ready
     */
    protected val notifyStrategy = NOTIFY_ALWAYS_IF_EXISTS

    /**
     * Returns seconds to object life (ttl)
     * @return integer seconds
     */
    var expireTime = DEFAULT_EXPIRE_TIME
        private set

    val entityClass: Class<Entity>
        get() = entity.javaClass

    init {
        _expiredAt = storage[expiredEntityStorageKey, null]
        expire()
    }

    /**
     * @return Raw observable data
     */
    val updateObservable: Observable<ResultModel>
        get() = entity.getUpdatableData()
                .doOnError { t: Throwable -> notifyOnError(t) }
                .subscribeOn(THREAD_IO)
//                .observeOn(THREAD_MAIN)

    /**
     * Called after data updated
     */
    @CallSuper
    fun onAfterUpdate(result: ResultModel) {
        entity.onAfterUpdate(result)
    }

    /**
     * Clear data and set data expired
     */
    fun clear() {
        entity.onClear()
//        mData = entity.getData()
//        mDataIsReady = false
        expire()
        mNotifier.onComplete()
        mNotifier = BehaviorSubject.create()
    }

    /**
     * Observer for update event
     * @return obserable with ResultModel
     * @see ResultModel
     *
     * @see Observable
     */
    fun onUpdate(): Observable<ResultModel> {
        return mNotifier
                .observeOn(THREAD_MAIN)
                .subscribeOn(THREAD_IO)
    }

    /**
     * Check data for expiration
     * @return true if data is expired
     */
    val isExpired: Boolean
        get() {
            if (_expiredAt == null) {
                Timber.d("Entity %s is expired=true", entity.javaClass.name)
                return true
            }
            val now = Date()
            val entityUpdatedAt: Date = _expiredAt!!
            val expired = now.time >= entityUpdatedAt.time
            if (expired) {
                onExpired()
            }
            Timber.d("Entity %s is expired=%b", entity.javaClass.name, expired)
            return expired
        }

    /**
     * Make data not expired
     */
    fun invalidateTime() {
        _expiredAt = Date()
        lastUpdate = _expiredAt
        var expireTime = expireTime
        expireTime *= 1000
        _expiredAt!!.time = _expiredAt!!.time + expireTime

        storage.put(expiredEntityStorageKey, _expiredAt)
    }

    fun setTimeToLive(ttlSeconds: Int): CachedRepository<ResultModel, Entity> {
        expireTime = ttlSeconds
        return this
    }

    /**
     * Make data expired
     */
    fun expire() {
        _expiredAt = null
        lastUpdate = _expiredAt
        storage.delete(expiredEntityStorageKey)
    }

    fun unsubscribe() {
        subscriptions.dispose()
        subscriptions.clear()
        mNotifier.onComplete()
        mNotifier = BehaviorSubject.create()
        mMetaNotifier.onComplete()
        mMetaNotifier = BehaviorSubject.create()
    }

    /**
     * @return True if data has set and not expired and not null
     */
    val isDataReady: Boolean
        get() = entity.isDataReady() && !isExpired

    /**
     * Returns updatable data
     * @return null if not loaded
     */
    val data: ResultModel
        get() = entity.getData()

    /**
     * Setting data
     * @param data Any
     */
    @CallSuper
    fun notifyData(data: ResultModel) {
        onAfterUpdate(data)
        notifyOnSuccess(true)
    }

    /**
     * @param first
     * @return
     */
    fun updateAfter(first: CachedRepository<ResultModel, Entity>): Observable<ResultModel> {
        return updateAfter(first.entity.getUpdatableData())
    }

    /**
     * Creates zip function and after update first value, returns current object value
     * @param first Any observable
     * @param <T> Any type
     * @return Observable
     */
    fun <T> updateAfter(first: Observable<T>): Observable<ResultModel> {
        return Observable
                .zip(first, entity.getUpdatableData(), BiFunction { _: T, resultModel: ResultModel -> resultModel })
                .observeOn(THREAD_MAIN)
                .subscribeOn(THREAD_IO)
    }

    /**
     * Returns local data, or if doesn't loaded, returns observable to remote source
     * @see .observe
     * @see .observeWithMeta
     */
    fun fetch(): Observable<ResultModel> {
        if (isDataReady) {
            return Observable.just(entity.getData())
                    .observeOn(THREAD_MAIN)
                    .subscribeOn(THREAD_MAIN)
        }
        update()
        return onUpdate()
    }

    @JvmOverloads
    fun update(
            force: Boolean,
            onNext: Consumer<ResultModel>? = null,
            onError: Consumer<Throwable>? = null,
            onComplete: Action? = null) {
        updateInternal(force, onNext, onError, onComplete)
    }

    @JvmOverloads
    fun update(
            onNext: Consumer<ResultModel>? = null,
            onError: Consumer<Throwable>? = null,
            onComplete: Action? = null) {
        updateInternal(false, onNext, onError, onComplete)
    }

    @JvmOverloads
    fun updateDelayed(
            time: Int,
            unit: TimeUnit = TimeUnit.SECONDS,
            force: Boolean = false,
            onNext: Consumer<ResultModel>? = null,
            onError: Consumer<Throwable>? = null,
            onComplete: Action? = null) {
        updateDelayedInternal(time, unit, force, onNext, onError, onComplete)
    }

    /**
     * Return subscriber that subscribes to all updates of this repository including some additional
     * information about data
     * Every time, consumer will call #update(), subscriber will notified about updating
     * @return ReplaySubject
     * @see MetaResult
     */
    fun observeWithMeta(): BehaviorSubject<MetaResult<ResultModel>> {
        return mMetaNotifier
    }

    /**
     * Return subscriber that subscribes to all updates of this repository
     * Every time, consumer will call #update(), subscriber will notified about updating
     * @return BehaviorSubject
     */
    fun observe(): BehaviorSubject<ResultModel> {
        return mNotifier
    }


    protected fun onExpired() {}

    @Synchronized
    protected fun updateDelayedInternal(
            delay: Int,
            unit: TimeUnit = TimeUnit.SECONDS,
            force: Boolean = false,
            onNext: Consumer<ResultModel>? = null,
            onError: Consumer<Throwable>? = null,
            onComplete: Action? = null
    ): CachedRepository<ResultModel, Entity> {

        val sub = Observable.timer(delay.toLong(), unit)
                .subscribeOn(THREAD_IO)
                .subscribe { updateInternal(force, onNext, onError, onComplete) }
        subscriptions.add(sub)
        return this
    }

    @Synchronized
    protected fun updateInternal(
            force: Boolean = false,
            onSuccess: Consumer<ResultModel>? = null,
            onError: Consumer<Throwable>? = null,
            onComplete: Action? = null
    ) {
        val observable: Observable<ResultModel>
        if (!force && !isExpired && isDataReady) {
            if (notifyStrategy == NOTIFY_ONLY_ON_UPDATE) {
                return
            }
            observable = Observable.just(entity.getData())
        } else {
            observable = updateObservable
            invalidateTime()
        }
        observable
                .subscribe(
                        callOnNext(onSuccess),
                        callOnError(onError),
                        callOnComplete(onComplete)
                )
    }

    protected fun notifyOnSuccess(isNew: Boolean) {
        mMetaNotifier.onNext(MetaResult(entity.getData(), isNew))
        mNotifier.onNext(entity.getData())
    }

    protected fun notifyOnError(t: Throwable) {
        mMetaNotifier.onError(t)
        mMetaNotifier = BehaviorSubject.create()
        mNotifier.onError(t)
        mNotifier = BehaviorSubject.create()
    }

    private fun callOnNext(chain: Consumer<ResultModel>?): Consumer<ResultModel> {
        return Consumer {
            notifyData(it)
            chain?.accept(it)
        }
    }

    private fun callOnError(chain: Consumer<Throwable>?): Consumer<Throwable> {
        return Consumer {
            notifyOnError(it)
            chain?.accept(it)
        }
    }

    private fun callOnComplete(chain: Action?): Action {
        return Action {
            chain?.run()
        }
    }

    class MetaResult<ResultModel> internal constructor(private val data: ResultModel, isNew: Boolean) {
        var isNewData = false
        fun value(): ResultModel {
            return data
        }

        override fun toString(): String {
            return data.toString()
        }

        fun toObservable(): Observable<MetaResult<ResultModel>> {
            return Observable.just(this)
        }

        init {
            isNewData = isNew
        }
    }
}