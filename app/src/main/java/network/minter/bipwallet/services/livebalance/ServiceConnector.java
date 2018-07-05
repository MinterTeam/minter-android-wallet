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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import io.reactivex.Observable;
import io.reactivex.subjects.ReplaySubject;
import timber.log.Timber;

/**
 * minter-android-wallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class ServiceConnector implements ServiceConnection {
    private static BalanceUpdatedService service = null;
    private static ServiceConnector instance;
    private static boolean bound = false;
    private static ReplaySubject<BalanceUpdatedService> serviceConnected = ReplaySubject.create(1);

    private ServiceConnector() {
    }

    public static BalanceUpdatedService get() {
        return service;
    }

    public static boolean isBound() {
        return bound;
    }

    public static void bind(Context context) {
        if (instance == null) {
            instance = new ServiceConnector();
        }

        if (!bound) {
            Intent intent = new Intent(context, BalanceUpdatedService.class);
            context.startService(intent);
            context.bindService(intent, instance, Context.BIND_AUTO_CREATE);
        }
    }


    public static void release(Context context) {
        if (bound && instance != null && service != null) {
            try {
                context.unbindService(instance);
            } catch (Throwable e) {
                Timber.i(e);
            }

            context.stopService(new Intent(context, BalanceUpdatedService.class));
            service = null;
        }
        bound = false;
    }

    public static Observable<BalanceUpdatedService> onConnected() {
        if (service != null) {
            return Observable.just(service);
        }

        return serviceConnected;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        if (!bound) {
            service = ((BalanceUpdatedService.LocalBinder) binder).getService();
            bound = true;
            serviceConnected.onNext(service);
            Timber.i("Service connected");
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        bound = false;
        service = null;
        Timber.d("Service disconnected");
    }
}

