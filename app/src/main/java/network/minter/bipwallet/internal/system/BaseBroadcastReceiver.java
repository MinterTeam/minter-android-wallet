package network.minter.bipwallet.internal.system;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.annotation.CallSuper;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

/**
 * Minter. 2020
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
public abstract class BaseBroadcastReceiver extends BroadcastReceiver {

    private IntentFilter mFilter;
    private OnReceiveListener mOnReceiveListener;

    public void setOnReceiveListener(OnReceiveListener receiveListener) {
        mOnReceiveListener = receiveListener;
    }

    public abstract String getActionName();

    public void register(Context context) {
        if (mFilter == null) {
            mFilter = new IntentFilter(getActionName());
        }

        LocalBroadcastManager.getInstance(context).registerReceiver(this, mFilter);
        onRegister();
    }

    public void onRegister() {
    }

    public void onUnregister() {
    }

    @CallSuper
    @Override
    public void onReceive(Context context, Intent intent) {
        if (mOnReceiveListener != null) {
            mOnReceiveListener.onReceive(context, intent);
        }
    }

    public void unregister(Context context) {
        try {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(this);
        } catch (Throwable ignore) {
        } finally {
            onUnregister();
        }
    }

    public interface OnReceiveListener {
        void onReceive(Context context, Intent intent);
    }
}
