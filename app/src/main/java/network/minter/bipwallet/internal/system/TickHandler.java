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

package network.minter.bipwallet.internal.system;

import android.support.annotation.NonNull;

import com.annimon.stream.Stream;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import timber.log.Timber;

/**
 * BipWallet. 2017
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class TickHandler {
    private AtomicLong mStart;
    private AtomicLong mEnd = new AtomicLong(-1);
    private int mInterval = 1;
    private TimeUnit mIntervalUnit = TimeUnit.SECONDS;
    private Disposable mSubscription;
    private ArrayList<TickListener> mListeners = new ArrayList<>();
    private ArrayList<TickEndListener> mEndListeners = new ArrayList<>();
    private ConditionWaiter mStopCondition;
    private boolean mPaused = false;

    public TickHandler() {
    }

    public TickHandler(long startTime, long endTime) {
        set(startTime, endTime);
    }

    public static TickHandler simple(long endTime, TickEndListener tickEndListener) {
        final TickHandler handler = new TickHandler(0, endTime);
        handler.addTickEndListener(tickEndListener);
        return handler;
    }

    public TickHandler setStopCondition(ConditionWaiter conditionWaiter) {
        mStopCondition = conditionWaiter;
        return this;
    }

    public boolean isRunning() {
        return mSubscription != null && !mSubscription.isDisposed();
    }

    public long getCurrent() {
        return mStart.get();
    }

    public void setCurrent(long start) {
        this.mStart = new AtomicLong(start);
    }

    public void clear() {
        mStart = new AtomicLong(0);
        mEnd = new AtomicLong(0);
    }

    public TickHandler setInterval(int interval, TimeUnit unit) {
        mInterval = interval;
        mIntervalUnit = unit;
        return this;
    }

    public void set(long start, long end) {
        mStart = new AtomicLong(start);
        mEnd = new AtomicLong(end);
    }

    public void set(long start) {
        mStart = new AtomicLong(start);
    }

    public void addTickListener(@NonNull TickListener tickListener) {
        mListeners.add(tickListener);
    }

    public TickHandler addTickEndListener(@NonNull TickEndListener tickEndListener) {
        mEndListeners.add(tickEndListener);
        return this;
    }

    public void removeTickListener(@NonNull TickListener listener) {
        if (!mListeners.contains(listener)) {
            return;
        }
        mListeners.remove(listener);
    }

    public void removeTickEndListener(@NonNull TickEndListener listener) {
        if (!mEndListeners.contains(listener)) {
            return;
        }

        mEndListeners.remove(listener);
    }

    public TickHandler restartWith(long start) {
        return restartWith(start, mEnd.get());
    }

    public TickHandler restartWith(long start, long end) {
        stop();
        set(start, end);
        start();
        return this;
    }

    public TickHandler start() {
        if (isRunning()) return this;
        mSubscription = Observable.interval(mInterval, mIntervalUnit, AndroidSchedulers.mainThread())
                .map(tick -> mStart.getAndIncrement())
                .subscribe(tick -> {
                    try {
                        callListenersOnTick();
                    } catch (Throwable tickErr) {
                        interrupt();
                        Timber.e(tickErr, "Error calling tick listener. Terminating loop...");
                        return;
                    }

                    if (tick >= mEnd.get() || (mStopCondition != null && mStopCondition.condition())) {
                        stop();
                        try {
                            callListenersEnd();
                        } catch (Throwable t) {
                            Timber.e(t, "Error calling end listeners");
                        }
                    }
                }, throwable -> {
                    interrupt();
                    Timber.e(throwable, "Error while ticking. Terminating loop...");
                });

        return this;
    }

    public void pause() {
        mPaused = true;
        interrupt();
    }

    public void resume() {
        if (!mPaused) return;
        start();
    }

    public void stop() {
        if (mSubscription != null && !mSubscription.isDisposed()) {
            mSubscription.dispose();
            callListenersStop();
        }
    }

    /**
     * Прерывает работу без оповещения слушателей, вызывается только во время ошибки
     * Возможно стоит rethrow-ить ошибку
     */
    private void interrupt() {
        if (mSubscription != null && !mSubscription.isDisposed()) {
            mSubscription.dispose();
        }
    }

    private void callListenersOnTick() {
        Stream.of(mListeners).forEach(listener -> listener.onTick(getCurrent()));
    }

    private void callListenersStop() {
        Stream.of(mListeners).forEach(listener -> listener.onTickStop(getCurrent()));
    }

    private void callListenersEnd() {
        Stream.of(mListeners).forEach(TickListener::onTickEnd);
        Stream.of(mEndListeners).forEach(TickEndListener::onTickEnd);
    }

    public interface ConditionWaiter {
        boolean condition();
    }

    public interface TickEndListener {
        void onTickEnd();
    }

    public interface TickListener extends TickEndListener {
        void onTick(long time);
        void onTickStop(long time);
    }
}
