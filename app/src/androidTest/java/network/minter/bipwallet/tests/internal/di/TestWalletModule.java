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

package network.minter.bipwallet.tests.internal.di;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;

import com.fatboyindustrial.gsonjodatime.Converters;
import com.google.gson.GsonBuilder;

import net.danlew.android.joda.JodaTimeAndroid;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import network.minter.bipwallet.BuildConfig;
import network.minter.bipwallet.R;
import network.minter.bipwallet.internal.auth.AuthSession;
import network.minter.bipwallet.internal.di.WalletApp;
import network.minter.bipwallet.internal.di.WalletModule;
import network.minter.bipwallet.internal.helpers.DateHelper;
import network.minter.bipwallet.internal.storage.KVStorage;
import network.minter.bipwallet.internal.system.testing.IdlingManager;
import network.minter.blockchain.MinterBlockChainApi;
import network.minter.core.internal.api.ApiService;
import network.minter.explorer.MinterExplorerApi;
import timber.log.Timber;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
@SuppressWarnings("unchecked")
@Module
public class TestWalletModule {
    private final static Map<String, Object> sMockStorage = new HashMap<>();
    private final Context mContext;

    public TestWalletModule(Context context) {
        mContext = context;

        WalletModule.initCoreSdk(mContext);
        MinterBlockChainApi.initialize(true);
        MinterExplorerApi.initialize(true);

        Timber.uprootAll();
        Timber.plant(new Timber.DebugTree());

        JodaTimeAndroid.init(context);
    }

    @Provides
    @WalletApp
    @Named("uuid")
    public String provideUUID(SharedPreferences preferences) {
        final String appKey = mContext.getString(R.string.app_name) + "_uuid";

        final String uuid = UUID.randomUUID().toString().toUpperCase();
        if (preferences.contains(appKey)) {
            return preferences.getString(appKey, uuid);
        }

        preferences.edit()
                .putString(appKey, uuid)
                .apply();

        return uuid;
    }

    @Provides
    @WalletApp
    public IdlingManager provideIdlingManager() {
        return new IdlingManager(true);
    }

    @Provides
    @WalletApp
    public Context provideContext() {
        return mContext;
    }

    @Provides
    @WalletApp
    public boolean provideDebugMode() {
        return true;
    }

    @Provides
    public Resources provideResources(Context context) {
        return context.getResources();
    }

    @Provides
    public ApiService.Builder provideApiService(AuthSession session, GsonBuilder gsonBuilder) {
        ApiService.Builder builder = new ApiService.Builder("", gsonBuilder);

        builder
                .setEmptyAuthTokenListener(session::logout)
                .setDebug(true)
                .setAuthHeaderName("Authorization")
                .addHeader("User-Agent", "Minter Android " + String.valueOf(BuildConfig.VERSION_CODE))
                .addHeader("X-Client-Version", BuildConfig.VERSION_NAME)
                .addHeader("X-Client-Build", String.valueOf(BuildConfig.VERSION_CODE));

        return builder;
    }

    @Provides
    @WalletApp
    public AuthSession provideAuthSession(KVStorage sessionStorage) {
        return new AuthSession(sessionStorage);
    }

    @Provides
    public GsonBuilder provideGsonBuilder() {
        GsonBuilder gsonBuilder = new GsonBuilder()
                .setDateFormat(DateHelper.DATE_FORMAT_SIMPLE);
        Converters.registerAll(gsonBuilder);

        return gsonBuilder;
    }

    @Provides
    public SharedPreferences providePreferences(Context context) {
        return context
                .getSharedPreferences(context.getString(R.string.user_local_settings_key),
                        Context.MODE_PRIVATE);
    }


    @WalletApp
    @Provides
    public KVStorage provideKeyValueStorage() {
        return new KVStorage() {
            @Override
            public synchronized <T> boolean put(String key, T value) {
                sMockStorage.put(key, value);
                return true;
            }

            @Override
            public synchronized <T> T get(String key) {
                return (T) sMockStorage.get(key);
            }

            @Override
            public synchronized <T> Queue<T> getQueue(String key) {
                return get(key);
            }

            @Override
            public synchronized <T> boolean putQueue(String key, Queue<T> queue) {
                // fix mutability, as we don't serialize in test case
                final Queue<T> copy = new LinkedList<>(queue);
                return put(key, copy);
            }

            @Override
            public <T> T get(String key, T defaultValue) {
                if (sMockStorage.containsKey(key)) {
                    return (T) sMockStorage.get(key);
                }

                return defaultValue;
            }

            @Override
            public synchronized boolean delete(String key) {
                sMockStorage.remove(key);
                return true;
            }

            @Override
            public synchronized boolean deleteAll() {
                sMockStorage.clear();
                return true;
            }

            @Override
            public long count() {
                return sMockStorage.size();
            }

            @Override
            public boolean contains(String key) {
                return sMockStorage.containsKey(key);
            }
        };
    }
}
