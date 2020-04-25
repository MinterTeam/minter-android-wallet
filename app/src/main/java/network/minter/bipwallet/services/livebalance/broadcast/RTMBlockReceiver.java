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

package network.minter.bipwallet.services.livebalance.broadcast;

import android.content.Context;
import android.content.Intent;

import com.google.gson.Gson;

import org.parceler.Parcels;

import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import network.minter.bipwallet.BuildConfig;
import network.minter.bipwallet.internal.Wallet;
import network.minter.bipwallet.internal.system.BaseBroadcastReceiver;
import network.minter.bipwallet.services.livebalance.models.RTMBlock;

import static network.minter.bipwallet.internal.settings.SettingsManagerKt.LastBlockTime;

public class RTMBlockReceiver extends BaseBroadcastReceiver {
    public static final String BROADCAST_ACTION = BuildConfig.APPLICATION_ID + ".RTM_BLOCK_COMMIT_RECEIVER";
    public static final String EXTRA_BLOCK_DATA = "EXTRA_BLOCK_DATA";
    private Listener mListener;
    private Disposable mIntervalDisposable;

    public RTMBlockReceiver(Listener listener) {
        mListener = listener;
    }

    /**
     * @param context Context
     * @param id if null, will cancel all current tasks
     */
    public static void send(Context context, @NonNull String blockJson) {
        Intent intent = new Intent(BROADCAST_ACTION);
        Gson gson = Wallet.app().gsonBuilder().create();
        RTMBlock block = gson.fromJson(blockJson, RTMBlock.class);
        intent.putExtra(EXTRA_BLOCK_DATA, Parcels.wrap(block));
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    @Override
    public void onRegister() {
        super.onRegister();
        mIntervalDisposable = Observable.interval(5, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(res -> {
                    if (mListener != null) {
                        mListener.exec();
                    }
                });
    }

    @Override
    public void onUnregister() {
        super.onUnregister();
        mIntervalDisposable.dispose();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        RTMBlock block = Parcels.unwrap(intent.getParcelableExtra(EXTRA_BLOCK_DATA));
        if (block == null) return;

        Wallet.app().settings().set(LastBlockTime, block.timestamp.getMillis());
    }

    @Override
    public String getActionName() {
        return BROADCAST_ACTION;
    }

    public interface Listener {
        void exec();
    }
}
