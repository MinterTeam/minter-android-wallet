/*
 * Copyright (C) by MinterTeam. 2022
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

package network.minter.bipwallet.services.livebalance.broadcast;

import android.content.Context;
import android.content.Intent;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.util.List;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import network.minter.bipwallet.BuildConfig;
import network.minter.bipwallet.internal.Wallet;
import network.minter.bipwallet.internal.system.BaseBroadcastReceiver;
import network.minter.bipwallet.services.livebalance.models.RTMBalance;
import timber.log.Timber;

public class RTMBalanceUpdateReceiver extends BaseBroadcastReceiver {
    public static final String BROADCAST_ACTION = BuildConfig.APPLICATION_ID + ".RTM_BALANCE_UPDATE_RECEIVER";
    public static final String EXTRA_MESSAGE = "EXTRA_MESSAGE";
    private final Listener mListener;

    public RTMBalanceUpdateReceiver(Listener listener) {
        mListener = listener;
    }

    public static void send(Context context, String message) {
        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra(EXTRA_MESSAGE, message);

        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (mListener == null) {
            return;
        }

        String json = intent.getStringExtra(EXTRA_MESSAGE);
        if (json == null || json.isEmpty()) {
            Timber.w("Balance update message is empty or null");
            return;
        }

        GsonBuilder gsonBuilder = Wallet.app().gsonBuilder();
        List<RTMBalance> message;
        try {
            message = gsonBuilder.create().fromJson(json, new TypeToken<List<RTMBalance>>() {
            }.getType());
        } catch (Throwable t) {
            Timber.e(t, "Unable to decode live balance message: %s", json);
            return;
        }

        mListener.onUpdated(message);
    }

    @Override
    public void onRegister() {
        super.onRegister();
    }

    @Override
    public void onUnregister() {
        super.onUnregister();
    }

    @Override
    public String getActionName() {
        return BROADCAST_ACTION;
    }

    public interface Listener {
        void onUpdated(List<RTMBalance> balances);
    }
}
