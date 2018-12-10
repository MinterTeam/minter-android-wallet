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

package network.minter.bipwallet.services.livebalance;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

import javax.inject.Inject;

import centrifuge.Centrifuge;
import centrifuge.Client;
import centrifuge.ConnectEvent;
import centrifuge.ConnectHandler;
import centrifuge.ErrorEvent;
import centrifuge.ErrorHandler;
import centrifuge.MessageEvent;
import centrifuge.MessageHandler;
import centrifuge.PublishEvent;
import centrifuge.PublishHandler;
import centrifuge.Subscription;
import dagger.android.AndroidInjection;
import io.reactivex.disposables.CompositeDisposable;
import network.minter.bipwallet.advanced.repo.SecretStorage;
import network.minter.bipwallet.internal.auth.AuthSession;
import network.minter.bipwallet.internal.data.CacheManager;
import network.minter.explorer.repo.ExplorerSettingsRepository;
import timber.log.Timber;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class BalanceUpdateService extends Service {

    private final IBinder mBinder = new LocalBinder();
    @Inject CacheManager cache;
    @Inject SecretStorage secretStorage;
    @Inject ExplorerSettingsRepository settingsRepo;
    @Inject AuthSession session;
    private CompositeDisposable mCompositeDisposable = new CompositeDisposable();
    private OnMessageListener mOnMessageListener;
    private final PublishHandler mListener = new PublishHandler() {
        @Override
        public void onPublish(Subscription subscription, PublishEvent publishEvent) {
            Timber.d("OnPublish: sub=%s, ev=%s", subscription.channel(), publishEvent.toString());
            if (mOnMessageListener != null) {
                mOnMessageListener.onMessage(new String(publishEvent.getData()), subscription.channel());
            }
        }
    };
    private Client mClient = null;

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
            mClient = Centrifuge.new_("wss://rtm.explorer.minter.network/connection/websocket", Centrifuge.defaultConfig());
            mClient.onConnect(new ConnectHandler() {
                @Override
                public void onConnect(Client client, ConnectEvent connectEvent) {
                    Timber.d("Connected");
                }
            });

            mClient.onError(new ErrorHandler() {
                @Override
                public void onError(Client client, ErrorEvent errorEvent) {
                    Timber.d("OnError[%d]: %s", errorEvent.incRefnum(), errorEvent.getMessage());
                }
            });

            mClient.onMessage(new MessageHandler() {
                @Override
                public void onMessage(Client p0, MessageEvent p1) {
                    Timber.d("OnMessage: event=%s", p1.toString());
                }
            });
            mClient.connect();

            Subscription sub = mClient.newSubscription(secretStorage.getAddresses().get(0).toString());
            sub.onPublish(mListener);
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
        void onMessage(String message, String channel);
    }

    public final class LocalBinder extends Binder {
        public BalanceUpdateService getService() {
            return BalanceUpdateService.this;
        }
    }
}
