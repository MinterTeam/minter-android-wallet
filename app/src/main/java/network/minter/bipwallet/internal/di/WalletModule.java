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

package network.minter.bipwallet.internal.di;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;
import com.fatboyindustrial.gsonjodatime.Converters;
import com.google.gson.GsonBuilder;
import com.orhanobut.hawk.Hawk;

import net.danlew.android.joda.JodaTimeAndroid;

import java.util.UUID;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import io.fabric.sdk.android.Fabric;
import network.minter.bipwallet.BuildConfig;
import network.minter.bipwallet.R;
import network.minter.bipwallet.internal.auth.AuthSession;
import network.minter.bipwallet.internal.helpers.DateHelper;
import network.minter.bipwallet.internal.storage.KVStorage;
import network.minter.blockchain.MinterBlockChainApi;
import network.minter.core.MinterSDK;
import network.minter.core.internal.api.ApiService;
import network.minter.explorer.MinterExplorerApi;
import timber.log.Timber;

/**
 * minter-android-wallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@Module
public class WalletModule {
    private Context mContext;
    private boolean mDebug;
    private boolean mEnableExternalLog;

    public WalletModule(Context context, boolean debug, boolean enableExternalLog) {
        mContext = context;
        mDebug = debug;
        mEnableExternalLog = enableExternalLog;
        initCrashlytics();
        Hawk.init(mContext)
                .setLogInterceptor(message -> Timber.tag("Hawk").d(message))
                .build();

        Timber.uprootAll();

        MinterSDK.initialize();
        MinterBlockChainApi.initialize(debug);
        MinterExplorerApi.initialize(debug);
        MinterExplorerApi.getInstance().setNetworkId(MinterExplorerApi.NET_ID_TESTNET_WITH_MULTISIG);

        JodaTimeAndroid.init(context);
    }

    private void initCrashlytics() {
        CrashlyticsCore core = new CrashlyticsCore.Builder().disabled(!mEnableExternalLog).build();
        Fabric.with(mContext, new Crashlytics.Builder().core(core).build());
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
    public KVStorage provideKeyValueStorage() {
        return new KVStorage();
    }

    @Provides
    @WalletApp
    public Context provideContext() {
        return mContext;
    }

    @Provides
    @WalletApp
    public boolean provideDebugMode() {
        return mDebug;
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
                .setDebug(mDebug)
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
}

