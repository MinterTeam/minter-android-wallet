package network.minter.bipwallet.internal.system;

import android.content.Context;
import android.content.Intent;

import com.annimon.stream.Stream;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;

/**
 * Minter. 2020
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
public final class BroadcastReceiverManager implements LifecycleObserver {
    private List<BaseBroadcastReceiver> mReceivers = new ArrayList<>(1);
    private WeakReference<Context> mContext;
    private OnReceiveListener mListener;

    public <T extends Context & LifecycleOwner> BroadcastReceiverManager(T context) {
        mContext = new WeakReference<>(context);
        context.getLifecycle().addObserver(this);
    }

    public BroadcastReceiverManager add(BaseBroadcastReceiver receiver) {
        mReceivers.add(receiver);
        receiver.register(mContext.get());
        return this;
    }

    public void setOnReceiveListener(OnReceiveListener listener) {
        mListener = listener;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void register() {
        Stream.of(mReceivers)
                .withoutNulls()
                .forEach(item -> {
                    final Class<? extends BaseBroadcastReceiver> receiverClass = item.getClass();

                    item.setOnReceiveListener((context, intent) -> {
                        if (mListener != null) {
                            mListener.onReceive(receiverClass, context, intent);
                        }
                    });
                    item.unregister(mContext.get());
                    item.register(mContext.get());
                });
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void unregister() {
        Stream.of(mReceivers)
                .withoutNulls()
                .forEach(item -> item.unregister(mContext.get()));
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    void release() {
        mContext.clear();
    }

    public interface OnReceiveListener {
        void onReceive(Class<? extends BaseBroadcastReceiver> receiverClass, Context context, Intent intent);
    }
}
