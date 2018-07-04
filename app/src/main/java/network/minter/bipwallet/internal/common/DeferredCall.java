package network.minter.bipwallet.internal.common;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import io.reactivex.Completable;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;


/**
 * Dogsy. 2017
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
