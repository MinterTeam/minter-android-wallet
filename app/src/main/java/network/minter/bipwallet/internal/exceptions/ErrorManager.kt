/*
 * Copyright (C) by MinterTeam. 2021
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

package network.minter.bipwallet.internal.exceptions

import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.functions.Function
import io.reactivex.subjects.PublishSubject
import network.minter.bipwallet.R
import network.minter.bipwallet.internal.helpers.ViewExtensions.tr
import network.minter.core.internal.exceptions.NetworkException
import org.reactivestreams.Publisher
import java.util.concurrent.TimeUnit

typealias RetryListener = () -> Unit

data class ErrorGlobalHandler(
        val clazz: Class<*>,
        val onError: (Throwable, RetryListener) -> Unit,
        val onRetried: (() -> Unit)? = null
)

data class ErrorLocalHandler(
        val clazz: Class<*>,
        val onError: (Throwable, RetryListener) -> Unit,
        val onRetried: (() -> Unit)? = null
)

data class ErrorGlobalReceiver(
        val clazz: Class<*>,
        val onError: (Throwable) -> Unit,
        val onRetried: (() -> Unit)? = null
)

data class ErrorLocalReceiver(
        val clazz: Class<*>,
        val onError: (Throwable) -> Unit,
        val onRetried: (() -> Unit)? = null
)

val Throwable.humanMessage: String
    get() {
        val err = NetworkException.convertIfNetworking(this)
        if (err is NetworkException) {
            return err.userMessage
        }
        return err.message ?: tr(R.string.error_unknown)
    }

val Throwable.humanDetailsMessage: String
    get() {
        val err = NetworkException.convertIfNetworking(this)
        if (err is NetworkException) {
            return "${err.userMessage}: ${err.message}"
        }
        return err.message ?: tr(R.string.error_unknown)
    }

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 *
 * This class globally collects errors to use with RxJava operator retryWhen()
 * then show errors in handlers and receivers
 * Handler - master class, that handle retry button click, show and hide global error
 * Receiver - any other class that wants to be notified when error occurred, for example: to hide progress
 *
 * As we have singleton instances of repositories, and this repositories injects into multiple classes at the same screen
 * (few tabs in one activity for example), we can't show retry button and error message at any place where repository used
 * Master class (handler) triggers PublishSubject via simple button in snackbar that notifies update observable from repository that it needs to be retried
 *
 * Example:
 * <pre>
 *     {@code
 *          class MyActivity {
 *              private val userRepo: UserRepository
 *              private val friendsRepo: FriendsRepository
 *              private val errorManager = ErrorManager()
 *              private var retryButton: Button
 *              private var errorMessageView: TextView
 *              private var progress: ProgressView
 *
 *              override fun onCreate() {
 *                  retryButton = findViewById ....
 *                  errorMessageView = ...
 *                  progress = ...
 *
 *                  errorManager.subscribe(ErrorHandler(
 *                      javaClass,
 *                      this::onError,
 *                      this::onRetried
 *                  ));
 *
 *                  userRepo.retryWhen(errorManager.retryWhenHandler)
 *                  friendsRepo.retryWhen(errorManager.retryWhenHandler)
 *
 *                  userRepo.observe().subscribe { user: User ->
 *                     // you don't need to check error, retry handler do that for you
 *                      usernameView.text = user.username
 *                  }
 *                  userRepo.update()
 *
 *                  friendsRepo.observe().subscribe { friendList: List<Friend> ->
 *                      friendListView.setFriends(friendList)
 *                  }
 *                  friendsRepo.update()
 *              }
 *
 *              // show error if User or List<Fried> hasn't been loaded
 *              private fun onError(t: Throwable, retryListener: RetryListener) {
 *                  errorMessageView.text = t.message
 *                  progress.setVisibility(GONE)
 *                  retryButton.setVisibility(VISIBLE)
 *                  retryButton.setOnClickListener {
 *                      retryListener()
 *                  }
 *              }
 *
 *              private fun onRetried() {
 *                  errorMessageView.text = null
 *                  progress.setVisibility(VISIBLE)
 *                  retryButton.setVisibility(GONE)
 *              }
 *          }
 *     }
 *
 *     class FragmentSomething {
 *          private val userRepo: UserRepository
 *          private val progress: ProgressView
 *          private val errorManager = ErrorManager()
 *
 *          override fun onCreateView(...) {
 *              errorManager.subscribe(ErrorReceiver(
 *                  javaClass,
 *                  { t: Throwable ->
 *                       //Something went wrong
 *                      progress.setVisibility(GONE)
 *                  }
 *              ))
 *
 *              userRepo.observe().subscribe { user: User ->
 *                  usernameView.text = user.username
 *              }
 *              userRepo.update()
 *          }
 *     }
 *
 *     </pre>
 *
 */
class ErrorManager {
    private val handlers: MutableMap<Class<*>, ErrorGlobalHandler> = HashMap()
    private val localHandlers: MutableMap<Class<*>, ErrorLocalHandler> = HashMap()
    private val receivers: MutableMap<Class<*>, ErrorGlobalReceiver> = HashMap()
    private val localReceivers: MutableMap<Class<*>, ErrorLocalReceiver> = HashMap()
    private val subject: PublishSubject<Any> = PublishSubject.create()
    private val lock = Any()

    val retryListener: RetryListener = {
        subject.onNext(Any())
    }

    interface ErrorGlobalHandlerListener {
        fun onError(t: Throwable, retryListener: RetryListener)
        fun onRetried() {}
    }

    interface ErrorGlobalReceiverListener {
        fun onError(t: Throwable)
        fun onRetried() {}
    }

    interface ErrorLocalHandlerListener {
        fun onError(t: Throwable, retryListener: RetryListener)
        fun onRetried() {}
        fun handleErrorFor(): Class<*>
    }

    interface ErrorLocalReceiverListener {
        fun onError(t: Throwable)
        fun onRetried() {}
        fun handleErrorFor(): Class<*>
    }

    fun subscribe(handler: ErrorGlobalHandlerListener) {
        synchronized(lock) {
            handlers[handler.javaClass] = ErrorGlobalHandler(handler.javaClass, handler::onError, handler::onRetried)
        }
    }

    fun subscribe(receiver: ErrorGlobalReceiverListener) {
        synchronized(lock) {
            receivers[receiver.javaClass] = ErrorGlobalReceiver(receiver.javaClass, receiver::onError, receiver::onRetried)
        }
    }

    fun subscribe(handler: ErrorLocalHandlerListener) {
        synchronized(lock) {
            localHandlers[handler.handleErrorFor()] = ErrorLocalHandler(handler.handleErrorFor(), handler::onError, handler::onRetried)
        }
    }

    fun subscribe(receiver: ErrorLocalReceiverListener) {
        synchronized(lock) {
            localReceivers[receiver.handleErrorFor()] = ErrorLocalReceiver(receiver.handleErrorFor(), receiver::onError, receiver::onRetried)
        }
    }

    fun subscribe(handler: ErrorGlobalHandler) {
        synchronized(lock) {
            handlers[handler.clazz] = handler
        }
    }

    fun subscribe(localHandler: ErrorLocalHandler) {
        synchronized(lock) {
            localHandlers[localHandler.clazz] = localHandler
        }
    }

    fun subscribe(localReceiver: ErrorLocalReceiver) {
        synchronized(lock) {
            localReceivers[localReceiver.clazz] = localReceiver
        }
    }

    fun subscribe(receiver: ErrorGlobalReceiver) {
        synchronized(lock) {
            receivers[receiver.clazz] = receiver
        }
    }

    fun unsubscribe(handler: ErrorGlobalHandlerListener) {
        synchronized(lock) {
            if (handlers.containsKey(handler.javaClass)) {
                handlers.remove(handler.javaClass)
            }
        }
    }

    fun unsubscribe(handler: ErrorLocalHandlerListener) {
        synchronized(lock) {
            if (localHandlers.containsKey(handler.handleErrorFor())) {
                localHandlers.remove(handler.handleErrorFor())
            }
        }
    }

    fun unsubscribe(receiver: ErrorLocalReceiverListener) {
        synchronized(lock) {
            if (localReceivers.containsKey(receiver.handleErrorFor())) {
                localReceivers.remove(receiver.handleErrorFor())
            }
        }
    }

    fun unsubscribe(receiver: ErrorGlobalReceiverListener) {
        synchronized(lock) {
            if (localReceivers.containsKey(receiver.javaClass)) {
                localReceivers.remove(receiver.javaClass)
            }
        }
    }

    fun unsubscribe(clazz: Class<*>) {
        synchronized(lock) {
            if (handlers.containsKey(clazz)) {
                handlers.remove(clazz)
            }
            if (receivers.containsKey(clazz)) {
                receivers.remove(clazz)
            }
            if (localHandlers.containsKey(clazz)) {
                localHandlers.remove(clazz)
            }
            if (localReceivers.containsKey(clazz)) {
                localReceivers.remove(clazz)
            }
        }
    }

    fun createLocalRetryWhenHandler(clazz: Class<*>): Function<Observable<out Throwable>, ObservableSource<*>> {
        return Function { observable: Observable<out Throwable> ->
            observable.flatMap { err: Throwable ->
                synchronized(lock) {
                    if (localHandlers.containsKey(clazz)) {
                        localHandlers[clazz]!!.onError(err, retryListener)
                    }
                    if (localReceivers.containsKey(clazz)) {
                        localReceivers[clazz]!!.onError(err)
                    }
                }
                subject.doOnNext {
                    synchronized(lock) {
                        if (localHandlers.containsKey(clazz)) {
                            localHandlers[clazz]!!.onRetried?.invoke()
                        }
                        if (localReceivers.containsKey(clazz)) {
                            localReceivers[clazz]!!.onRetried?.invoke()
                        }
                    }
                }

                subject.delay(500, TimeUnit.MILLISECONDS)
            }
        }
    }

    val retryWhenHandlerCompletable: Function<Flowable<out Throwable>, Publisher<*>>
        get() {
            return Function { observable: Flowable<out Throwable> ->
                observable.flatMap { err: Throwable ->

                    synchronized(lock) {
                        handlers.values.forEach { it.onError(err, retryListener) }
                        receivers.values.forEach { it.onError(err) }
                    }
                    subject.doOnNext {
                        synchronized(lock) {
                            handlers.values.forEach { it.onRetried?.invoke() }
                            receivers.values.forEach { it.onRetried?.invoke() }
                        }
                    }

                    subject.toFlowable(BackpressureStrategy.LATEST).delay(500, TimeUnit.MILLISECONDS)
                }
            }
        }

    val retryWhenHandler: Function<Observable<out Throwable>, ObservableSource<*>>
        get() {
            return Function { observable: Observable<out Throwable> ->
                observable.flatMap { err: Throwable ->

                    synchronized(lock) {
                        handlers.values.forEach { it.onError(err, retryListener) }
                        receivers.values.forEach { it.onError(err) }
                    }
                    subject.doOnNext {
                        synchronized(lock) {
                            handlers.values.forEach { it.onRetried?.invoke() }
                            receivers.values.forEach { it.onRetried?.invoke() }
                        }
                    }

                    subject.delay(500, TimeUnit.MILLISECONDS)
                }
            }
        }
}