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
package network.minter.bipwallet.internal.mvp

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.annotation.CallSuper
import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Function
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import moxy.MvpPresenter
import moxy.MvpView
import network.minter.bipwallet.analytics.AnalyticsManager
import network.minter.bipwallet.analytics.base.*
import network.minter.bipwallet.internal.Wallet
import network.minter.core.internal.exceptions.NetworkException
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
abstract class MvpBasePresenter<V : MvpView?> : MvpPresenter<V>() {
    private val subscriptions = CompositeDisposable()
    private var errorRetrySubject: PublishSubject<Any> = PublishSubject.create()
    private var isInitialized = false
    private var retriedAfterError = false
    private var isErrorWithRetryShown: Boolean = false

    internal fun Disposable.disposeOnDestroy(): Disposable {
        subscriptions.add(this)
        return this
    }

    fun unsubscribeOnDestroy(subscription: Disposable?): MvpBasePresenter<V> {
        subscriptions.add(subscription!!)
        return this
    }

    fun retry() {
        doOnErrorResolve()
        errorRetrySubject.onNext(Any())
        retriedAfterError = true
    }

    open fun onSaveInstanceState(outState: Bundle?) {}
    open fun onRestoreInstanceState(savedInstanceState: Bundle?) {}
    fun retryOnClick(): View.OnClickListener {
        return View.OnClickListener { retry() }
    }

    @CallSuper
    override fun attachView(view: V) {
        errorRetrySubject.doOnSubscribe { unsubscribeOnDestroy(it) }
        errorRetrySubject.doOnNext {
            Timber.d("Retry reactive")
        }

        if (!isInitialized) {
            isInitialized = true
            handleAnalytics()
        }
        super.attachView(view)
    }

    @CallSuper
    override fun detachView(view: V) {
        super.detachView(view)
    }

    @CallSuper
    override fun onDestroy() {
        if (!subscriptions.isDisposed) {
            subscriptions.dispose()
        }
        super.onDestroy()
    }

    fun onTrimMemory() {}
    open fun onLowMemory() {}
    open fun handleExtras(intent: Intent?) {}
    open fun handleExtras(bundle: Bundle?) {}
    open fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {}
    protected val analytics: AnalyticsManager
        get() = Wallet.app().analytics()

    protected open fun doOnErrorResolve() {
        isErrorWithRetryShown = false
        if (viewState is ProgressView) {
            (viewState as ProgressView).showProgress()
        }
    }

    protected open fun doOnError(t: Throwable) {
        if (viewState is ProgressView) {
            (viewState as ProgressView).hideProgress()
        }
        if (viewState is ErrorViewWithRetry) {
            var exceptionMessage = t.message
            if (t is NetworkException) {
                exceptionMessage = t.userMessage
            }

            if (!isErrorWithRetryShown) {
                isErrorWithRetryShown = true
                (viewState as ErrorViewWithRetry).onErrorWithRetry(String.format("%s", exceptionMessage), retryOnClick())
            }

        }
    }

    protected open fun <T> safeSubscribe(input: Observable<T>): Observable<T> {
        return input.retryWhen(errorResolver)
    }

    protected open fun <T> safeSubscribeComputeToUi(input: Observable<T>): Observable<T> {
        return input
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
    }

    protected open fun <T> Observable<T>.joinToUi(): Observable<T> {
        return safeSubscribeIoToUi(this)
    }

    protected open fun <T> safeSubscribeIoToUi(input: Observable<T>): Observable<T> {
        return input
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    protected open val errorResolver: Function<Observable<out Throwable>, ObservableSource<*>>
        get() = Function { observable: Observable<out Throwable> ->
            observable.flatMap { err: Throwable ->
                val t = NetworkException.convertIfNetworking(err)
                Timber.w(t, "Error occurred in %s", javaClass.name)
                doOnError(t)
                errorRetrySubject.delay(500, TimeUnit.MILLISECONDS)
            }
        }

    protected open fun errorResolverWithCallback(cb: (Throwable) -> Unit): Function<Observable<out Throwable>, ObservableSource<*>> {
        return Function { observable: Observable<out Throwable> ->
            observable.flatMap { err: Throwable ->
                val t = NetworkException.convertIfNetworking(err)
                Timber.w(t, "Error occurred in %s", javaClass.name)
                doOnError(t)
                cb.invoke(t)
                errorRetrySubject.delay(500, TimeUnit.MILLISECONDS)
            }
        }
    }

    private fun handleAnalytics() {
        val analytics = analytics
        if (this is HasAnalyticsEvent) {
            analytics.send((this as HasAnalyticsEvent).analyticsEvent)
        }
        if (this is HasAnalyticsEventWithId) {
            val eventWithId = (this as HasAnalyticsEventWithId).analyticsEventWithId
            analytics.send(eventWithId.first, eventWithId.second)
        }
        if (this is HasAnalyticsEventWithBundle) {
            val eventWithId = (this as HasAnalyticsEventWithBundle).analyticsEventWithBundle
            analytics.send(eventWithId.first, eventWithId.second)
        }
        if (this is HasAnalyticsEventWithParams) {
            val eventWithId = (this as HasAnalyticsEventWithParams).analyticsEventWithParams
            analytics.send(eventWithId.first, eventWithId.second)
        }
        if (this is HasAnalyticsMultipleEvents) {
            for (event in (this as HasAnalyticsMultipleEvents).analyticsEvents) {
                analytics.send(event)
            }
        }
    }
}