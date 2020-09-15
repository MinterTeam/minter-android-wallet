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

package network.minter.bipwallet.tests.internal.di;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;

import com.fatboyindustrial.gsonjodatime.Converters;
import com.google.gson.GsonBuilder;
import com.orhanobut.hawk.ConcealEncryption;
import com.orhanobut.hawk.Hawk;
import com.orhanobut.hawk.NoEncryption;

import net.danlew.android.joda.JodaTimeAndroid;

import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.BigInteger;
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
import network.minter.bipwallet.internal.Wallet;
import network.minter.bipwallet.internal.auth.AuthSession;
import network.minter.bipwallet.internal.di.WalletApp;
import network.minter.bipwallet.internal.di.WalletModule;
import network.minter.bipwallet.internal.di.annotations.DbCache;
import network.minter.bipwallet.internal.gson.BigDecimalJsonConverter;
import network.minter.bipwallet.internal.gson.HistoryTransactionsJsonConverter;
import network.minter.bipwallet.internal.helpers.DateHelper;
import network.minter.bipwallet.internal.settings.SettingsManager;
import network.minter.bipwallet.internal.storage.KVStorage;
import network.minter.bipwallet.internal.system.ForegroundDetector;
import network.minter.bipwallet.services.livebalance.RTMService;
import network.minter.blockchain.MinterBlockChainSDK;
import network.minter.core.crypto.BytesData;
import network.minter.core.crypto.MinterAddress;
import network.minter.core.crypto.MinterCheck;
import network.minter.core.crypto.MinterHash;
import network.minter.core.crypto.MinterPublicKey;
import network.minter.core.internal.api.ApiService;
import network.minter.core.internal.api.converters.BigIntegerJsonConverter;
import network.minter.core.internal.api.converters.BytesDataJsonConverter;
import network.minter.core.internal.api.converters.MinterAddressJsonConverter;
import network.minter.core.internal.api.converters.MinterCheckJsonConverter;
import network.minter.core.internal.api.converters.MinterHashJsonConverter;
import network.minter.core.internal.api.converters.MinterPublicKeyJsonConverter;
import network.minter.core.internal.log.TimberLogger;
import network.minter.explorer.MinterExplorerSDK;
import network.minter.explorer.models.HistoryTransaction;
import network.minter.ledger.connector.rxjava2.RxMinterLedger;
import timber.log.Timber;

import static network.minter.bipwallet.internal.Wallet.ENABLE_CRASHLYTICS;
import static network.minter.bipwallet.internal.di.WalletModule.DB_CACHE;
import static network.minter.bipwallet.internal.di.WalletModule.initCoreSdk;

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

        MinterBlockChainSDK.initialize(true);

        if (BuildConfig.LIVE_BALANCE_URL != null) {
            RTMService.LIVE_BALANCE_URL = BuildConfig.LIVE_BALANCE_URL;
        }

        {
            MinterExplorerSDK.Setup explorerSdk = new MinterExplorerSDK.Setup().setEnableDebug(true);

            if (BuildConfig.EXPLORER_API_URL != null) {
                explorerSdk.setExplorerApiUrl(BuildConfig.EXPLORER_API_URL);
            }
            if (BuildConfig.GATE_API_URL != null) {
                explorerSdk.setGateApiUrl(BuildConfig.GATE_API_URL);
            }
            explorerSdk.setLogger(new TimberLogger());
            explorerSdk.init();
        }

        WalletModule.WalletGsonHawkParer parser = new WalletModule.WalletGsonHawkParer(getGson());

        Hawk.init(mContext)
                .setParser(parser)
                .setEncryption(new ConcealEncryption(mContext))
                .build();

        Hawk.init(DB_CACHE, mContext)
                .setEncryption(new NoEncryption())
                .setParser(parser)
                .build();

        initCoreSdk(context);


        Timber.uprootAll();

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }

        if (ENABLE_CRASHLYTICS) {
            Timber.plant(new Wallet.CrashlyticsTree());
        }

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
    public Context provideContext() {
        return mContext;
    }

//    @Provides
//    @WalletApp
//    public IdlingManager provideIdlingManager() {
//        return new IdlingManager(true);
//    }

    @Provides
    @WalletApp
    public SettingsManager provideSettingsManager(KVStorage storage) {
        return new SettingsManager(storage);
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
                .addHeader("User-Agent", "Minter Android " + BuildConfig.VERSION_CODE)
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
    @WalletApp
    public RxMinterLedger provideLedger() {
        return null;
    }

    @Provides
    @WalletApp
    public ForegroundDetector provideFgDetectorMock() {
        return new ForegroundDetector();
    }

    @Provides
    public SharedPreferences providePreferences(Context context) {
        return context
                .getSharedPreferences(context.getString(R.string.user_local_settings_key),
                        Context.MODE_PRIVATE);
    }

    @Provides
    @WalletApp
    public KVStorage provideSecretKVStorage() {
        return createKVStorage();
    }

    @Provides
    @WalletApp
    @DbCache
    public KVStorage provideCacheKVStorage() {
        return createKVStorage();
    }

    private GsonBuilder getGson() {
        GsonBuilder out = new GsonBuilder();
        out.registerTypeAdapter(BigInteger.class, new BigIntegerJsonConverter());
        out.registerTypeAdapter(BigDecimal.class, new BigDecimalJsonConverter());
        out.registerTypeAdapter(MinterAddress.class, new MinterAddressJsonConverter());
        out.registerTypeAdapter(MinterPublicKey.class, new MinterPublicKeyJsonConverter());
        out.registerTypeAdapter(MinterHash.class, new MinterHashJsonConverter());
        out.registerTypeAdapter(MinterCheck.class, new MinterCheckJsonConverter());
        out.registerTypeAdapter(BytesData.class, new BytesDataJsonConverter());
        out.registerTypeAdapter(HistoryTransaction.class, new HistoryTransactionsJsonConverter());

        return out;
    }

    private KVStorage createKVStorage() {
        return new KVStorage() {
            @Override
            public synchronized <T> boolean put(@NotNull String key, T value) {
                sMockStorage.put(key, value);
                return true;
            }

            @Override
            public synchronized <T> T get(@NotNull String key) {
                return (T) sMockStorage.get(key);
            }

            @Override
            public synchronized <T> Queue<T> getQueue(@NotNull String key) {
                return get(key);
            }

            @Override
            public synchronized <T> boolean putQueue(@NotNull String key, Queue<T> queue) {
                // fix mutability, as we don't serialize in test case
                final Queue<T> copy = new LinkedList<>(queue);
                return put(key, copy);
            }

            @Override
            public <T> T get(@NotNull String key, T defaultValue) {
                if (sMockStorage.containsKey(key)) {
                    return (T) sMockStorage.get(key);
                }

                return defaultValue;
            }

            @Override
            public synchronized boolean delete(@NotNull String key) {
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
            public boolean contains(@NotNull String key) {
                return sMockStorage.containsKey(key);
            }
        };
    }
}
