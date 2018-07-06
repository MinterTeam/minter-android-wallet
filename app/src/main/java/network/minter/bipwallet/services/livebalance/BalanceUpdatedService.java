/*******************************************************************************
 * Copyright (C) by MinterTeam. 2018
 * @link https://github.com/MinterTeam
 * @link https://github.com/edwardstock
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
 ******************************************************************************/

package network.minter.bipwallet.services.livebalance;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.centrifugal.centrifuge.android.Centrifugo;
import com.centrifugal.centrifuge.android.config.ReconnectConfig;
import com.centrifugal.centrifuge.android.credentials.Token;
import com.centrifugal.centrifuge.android.credentials.User;
import com.centrifugal.centrifuge.android.listener.ConnectionListener;
import com.centrifugal.centrifuge.android.listener.DataMessageListener;
import com.centrifugal.centrifuge.android.listener.JoinLeaveListener;
import com.centrifugal.centrifuge.android.listener.SubscriptionListener;
import com.centrifugal.centrifuge.android.message.presence.JoinMessage;
import com.centrifugal.centrifuge.android.message.presence.LeftMessage;
import com.centrifugal.centrifuge.android.subscription.SubscriptionRequest;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import network.minter.bipwallet.advanced.repo.SecretStorage;
import network.minter.bipwallet.internal.auth.AuthSession;
import network.minter.bipwallet.internal.data.CacheManager;
import network.minter.explorerapi.repo.ExplorerAddressRepository;
import timber.log.Timber;

import static network.minter.bipwallet.internal.ReactiveAdapter.rxCallExp;

/**
 * minter-android-wallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class BalanceUpdatedService extends Service {

    private final IBinder mBinder = new LocalBinder();
    @Inject CacheManager cache;
    @Inject SecretStorage secretStorage;
    @Inject ExplorerAddressRepository addressRepo;
    @Inject AuthSession session;

    private Centrifugo mClient = null;
    private DataMessageListener mListener;

    @Override
    public void onCreate() {
        super.onCreate();
        AndroidInjection.inject(this);
        rxCallExp(addressRepo.getBalanceChannel(secretStorage.getAddresses(), String.valueOf(session.getUser().getData().id)))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(res -> {
                    final String uid = String.valueOf(session.getUser().getData().id);
                    Timber.d("Connecting to: channel=%s, token=%s, timestamp=%d", res.result.channel, res.result.token, res.result.timestamp);
                    mClient = new Centrifugo.Builder("wss://92.53.87.98:8000/connection/websocket")
                            .setReconnectConfig(new ReconnectConfig(10, 3, TimeUnit.SECONDS))
                            .setUser(new User(uid, res.result.token))
                            .setToken(new Token(res.result.token, String.valueOf(res.result.timestamp)))
                            .build();

                    mClient.setConnectionListener(new ConnectionListener() {
                        @Override
                        public void onWebSocketOpen() {
                            Timber.d("Opened connection");
                        }

                        @Override
                        public void onConnected() {
                            Timber.d("Connected");
                        }

                        @Override
                        public void onDisconnected(int code, String reason, boolean remote) {
                            Timber.d("Disconnected[%d]: %s (by remote:%b)", code, reason, remote);
                        }
                    });
                    mClient.setSubscriptionListener(new SubscriptionListener() {
                        @Override
                        public void onSubscribed(String channelName) {
                            Timber.d("Subscribed %s", channelName);
                        }

                        @Override
                        public void onUnsubscribed(String channelName) {
                            Timber.d("Unsubscribed %s", channelName);
                        }

                        @Override
                        public void onSubscriptionError(String channelName, String error) {
                            Timber.w("Subscription error %s, %s", channelName, error);
                        }
                    });
                    mClient.setJoinLeaveListener(new JoinLeaveListener() {
                        @Override
                        public void onJoin(JoinMessage joinMessage) {
                            Timber.d("Joined %s (%s) %s", joinMessage.getUser(), joinMessage.getBody().toString(), joinMessage.getError());
                        }

                        @Override
                        public void onLeave(LeftMessage leftMessage) {

                        }
                    });
                    mClient.setDataMessageListener(mListener);
                    mClient.connect();
                    mClient.subscribe(new SubscriptionRequest(res.result.channel, res.result.token));

                });

    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        disconnect();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disconnect();
    }

    public void setOnMessageListener(DataMessageListener listener) {
        mListener = listener;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public Centrifugo getClient() {
        return mClient;
    }

    private void disconnect() {
        if (mClient == null) return;
        mClient.disconnect();
    }

    public final class LocalBinder extends Binder {
        public BalanceUpdatedService getService() {
            return BalanceUpdatedService.this;
        }
    }
}