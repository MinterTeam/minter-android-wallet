/*
 * Copyright (C) by MinterTeam. 2019
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

package network.minter.bipwallet.services.livebalance;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import java.lang.reflect.Field;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import dagger.android.AndroidInjection;
import io.github.centrifugal.centrifuge.Client;
import io.github.centrifugal.centrifuge.ConnectEvent;
import io.github.centrifugal.centrifuge.DisconnectEvent;
import io.github.centrifugal.centrifuge.ErrorEvent;
import io.github.centrifugal.centrifuge.EventListener;
import io.github.centrifugal.centrifuge.MessageEvent;
import io.github.centrifugal.centrifuge.Options;
import io.github.centrifugal.centrifuge.PublishEvent;
import io.github.centrifugal.centrifuge.Subscription;
import io.github.centrifugal.centrifuge.SubscriptionEventListener;
import io.reactivex.disposables.CompositeDisposable;
import network.minter.bipwallet.BuildConfig;
import network.minter.bipwallet.advanced.repo.SecretStorage;
import network.minter.bipwallet.internal.auth.AuthSession;
import network.minter.bipwallet.internal.data.CacheManager;
import network.minter.core.crypto.MinterAddress;
import timber.log.Timber;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class LiveBalanceService extends Service {

    public static String LIVE_BALANCE_URL = BuildConfig.LIVE_BALANCE_URL;

    private final IBinder mBinder = new LocalBinder();
    @Inject CacheManager cache;
    @Inject SecretStorage secretStorage;
    @Inject AuthSession session;
    private CompositeDisposable mCompositeDisposable = new CompositeDisposable();
    private OnMessageListener mOnMessageListener;
    private Client mClient = null;
    private MinterAddress mAddress;

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        try {
            disconnect();
        } catch (Exception e) {
            Timber.w(e);
        }
    }

    @Override
    public void onCreate() {
        AndroidInjection.inject(this);
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            disconnect();
        } catch (Exception e) {
            Timber.w(e);
        }
        mCompositeDisposable.clear();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        try {
            disconnect();
        } catch (Exception e) {
            Timber.w(e);
        }
        return super.onUnbind(intent);
    }

    public void setOnMessageListener(OnMessageListener listener) {
        mOnMessageListener = listener;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        try {
            connect();
        } catch (Exception e) {
            Timber.w(e);
        }
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    public Client getClient() {
        return mClient;
    }

    private void connect() {
        try {
            EventListener listener = new EventListener() {
                @Override
                public void onConnect(Client client, ConnectEvent event) {
                    super.onConnect(client, event);
                    Timber.d("Connected");
                }

                @Override
                public void onDisconnect(Client client, DisconnectEvent event) {
                    super.onDisconnect(client, event);
                    Timber.i("Disconnected");
                }


                @SuppressLint("TimberExceptionLogging")
                @Override
                public void onError(Client client, ErrorEvent event) {
                    super.onError(client, event);
                    try {
                        Field msgField = ErrorEvent.class.getDeclaredField("message");
                        Field exceptionField = ErrorEvent.class.getDeclaredField("exception");
                        msgField.setAccessible(true);
                        exceptionField.setAccessible(true);

                        String msg = ((String) msgField.get("message"));
                        Throwable exception = (Throwable) exceptionField.get("exception");

                        Timber.w(exception, msg);
                    } catch (Throwable t) {
                        Timber.d(t);
                        Timber.d("LiveBalance connection error (unknown)");
                    }
                    // @TODO WTF?


                }

                @Override
                public void onMessage(Client client, MessageEvent event) {
                    super.onMessage(client, event);
                    Timber.d("OnMessage: event=%s", new String(event.getData()));
                }
            };
            Options opts = new Options();
            mClient = new Client(BuildConfig.LIVE_BALANCE_URL + "?format=protobuf", opts, listener);
            mClient.connect();

            mAddress = secretStorage.getAddresses().get(0);
            Subscription sub = mClient.newSubscription(mAddress.toString(), new SubscriptionEventListener() {
                @Override
                public void onPublish(Subscription subscription, PublishEvent publishEvent) {
                    super.onPublish(subscription, publishEvent);
                    Timber.d("OnPublish: sub=%s, ev=%s", subscription.getChannel(), new String(publishEvent.getData()));
                    if (mOnMessageListener != null) {
                        mOnMessageListener.onMessage(new String(publishEvent.getData()), subscription.getChannel(), mAddress);
                    }
                }
            });
            sub.subscribe();
        } catch (Throwable t) {
            Timber.w(t, "Unable to connect with RTM");
        }
    }

    private void disconnect() {
        if (mClient == null) return;
        try {
            mClient.disconnect();
            mClient = null;
        } catch (Exception e) {
        }
    }

    public interface OnMessageListener {
        void onMessage(String message, String channel, MinterAddress address);
    }

    public final class LocalBinder extends Binder {
        public LiveBalanceService getService() {
            return LiveBalanceService.this;
        }
    }
}
