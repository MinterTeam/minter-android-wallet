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

package network.minter.bipwallet.internal.common;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import io.reactivex.Completable;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;


/**
 * minter-android-wallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class DeferredCall<Context> {

    private LinkedBlockingQueue<Completable> mTasks;
    private Context mContext;

    private DeferredCall() {
    }

    public static <T> DeferredCall<T> createWithSize(int size) {
        final DeferredCall<T> defer = new DeferredCall<>();
        defer.mTasks = new LinkedBlockingQueue<>(size);

        return defer;
    }

    public static <T> DeferredCall<T> create() {
        final DeferredCall<T> defer = new DeferredCall<>();
        defer.mTasks = new LinkedBlockingQueue<>();

        return defer;
    }

    public DeferredCall call(Task<Context> task) {
        if (mContext != null) {
            task.call(mContext);
            return this;
        }

        mTasks.offer(Completable.fromCallable(() -> {
            task.call(mContext);
            return true;
        }));
        return this;
    }

    public void attach(Context context) {
        mContext = context;
        dequeue(mContext)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe();
    }

    public void detach() {
        mContext = null;
    }

    public Completable dequeue(Context context) {
        return inject(context).dequeue(context, AndroidSchedulers.mainThread());
    }

    public void attachSingle(Context context) {
        dequeue(context)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(this::detach);
    }

    public Completable dequeue(Context context, Scheduler on) {
        inject(context);
        final List<Completable> tasks = new ArrayList<>(mTasks.size());
        while (!mTasks.isEmpty()) {
            tasks.add(mTasks.poll());
        }

        return Completable.merge(tasks)
                .subscribeOn(on);
    }

    public void clear() {
        mTasks.clear();
    }

    public int size() {
        return mTasks.size();
    }

    private DeferredCall<Context> inject(Context context) {
        this.mContext = context;
        return this;
    }

    public interface Task<Sender> {
        void call(Sender sender);
    }
}
