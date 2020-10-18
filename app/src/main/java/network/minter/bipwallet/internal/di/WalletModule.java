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

package network.minter.bipwallet.internal.di;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.hardware.usb.UsbManager;
import android.os.Build;

import com.annimon.stream.Stream;
import com.edwardstock.secp256k1.NativeSecp256k1;
import com.fatboyindustrial.gsonjodatime.Converters;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.orhanobut.hawk.ConcealEncryption;
import com.orhanobut.hawk.GsonParser;
import com.orhanobut.hawk.Hawk;
import com.orhanobut.hawk.HawkDB;
import com.orhanobut.hawk.NoEncryption;

import net.danlew.android.joda.JodaTimeAndroid;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import network.minter.bipwallet.BuildConfig;
import network.minter.bipwallet.R;
import network.minter.bipwallet.internal.Wallet;
import network.minter.bipwallet.internal.auth.AuthSession;
import network.minter.bipwallet.internal.di.annotations.DbCache;
import network.minter.bipwallet.internal.exceptions.ErrorManager;
import network.minter.bipwallet.internal.gson.BigDecimalJsonConverter;
import network.minter.bipwallet.internal.gson.HistoryTransactionsJsonConverter;
import network.minter.bipwallet.internal.helpers.DateHelper;
import network.minter.bipwallet.internal.settings.SettingsManager;
import network.minter.bipwallet.internal.storage.KVStorage;
import network.minter.bipwallet.internal.system.ForegroundDetector;
import network.minter.bipwallet.internal.system.UnzipUtil;
import network.minter.bipwallet.services.livebalance.RTMService;
import network.minter.blockchain.MinterBlockChainSDK;
import network.minter.core.MinterSDK;
import network.minter.core.bip39.NativeBip39;
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
import network.minter.core.internal.exceptions.NativeLoadException;
import network.minter.core.internal.log.TimberLogger;
import network.minter.explorer.MinterExplorerSDK;
import network.minter.explorer.models.HistoryTransaction;
import network.minter.ledger.connector.rxjava2.RxMinterLedger;
import timber.log.Timber;

import static android.content.Context.USB_SERVICE;
import static network.minter.bipwallet.internal.Wallet.ENABLE_CRASHLYTICS;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@Module
public class WalletModule {
    public final static String DB_CACHE = "minter_cache";
    private final Context mContext;
    private final boolean mDebug;
    private final boolean mEnableExternalLog;

    public WalletModule(Context context, boolean debug, boolean enableExternalLog) {
        mContext = context;
        mDebug = debug;
        mEnableExternalLog = enableExternalLog;
        initCrashlytics();

        MinterBlockChainSDK.initialize(debug);

        if (BuildConfig.LIVE_BALANCE_URL != null) {
            RTMService.LIVE_BALANCE_URL = BuildConfig.LIVE_BALANCE_URL;
        }

        {
            MinterExplorerSDK.Setup explorerSdk = new MinterExplorerSDK.Setup().setEnableDebug(debug);

            /* for test purposes only
            if(BuildConfig.FLAVOR.startsWith("netMain")) {
                explorerSdk.setNetId("chilinet");
            }
            */

            if (BuildConfig.EXPLORER_API_URL != null) {
                explorerSdk.setExplorerApiUrl(BuildConfig.EXPLORER_API_URL);
            }
            if (BuildConfig.GATE_API_URL != null) {
                explorerSdk.setGateApiUrl(BuildConfig.GATE_API_URL);
            }
            explorerSdk.setLogger(new TimberLogger());
            explorerSdk.init();
        }

        WalletGsonHawkParer parser = new WalletGsonHawkParer(getGson());

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

    @SuppressLint("UnsafeDynamicallyLoadedCode")
    public static void initCoreSdk(Context context) {
        HawkDB db = Hawk.db();
        try {
            // Try loading our native lib, see if it works...
            MinterSDK.initialize();
        } catch (NativeLoadException t0) {
            final ApplicationInfo appInfo = context.getApplicationInfo();
            Timber.d("Source apk file: %s", appInfo.sourceDir);

            final List<String> libFiles = new ArrayList<>(NativeBip39.LIB_FILES.length + NativeSecp256k1.LIB_FILES.length);
            libFiles.addAll(Stream.of(NativeBip39.LIB_FILES).toList());
            libFiles.addAll(Stream.of(NativeSecp256k1.LIB_FILES).toList());

            Iterator<String> iter = libFiles.iterator();
            while (iter.hasNext()) {
                String libFileName = iter.next();
                String libFilePath = db.get(libFileName);
                if (db.contains(libFileName) && new File(libFilePath).exists()) {
                    iter.remove();
                }
                System.load(libFilePath);
            }

            if (libFiles.isEmpty()) {
                Timber.d("Loaded cached lib files");
                MinterSDK.setEnabledNativeLibs(true);
                return;
            }

            String destPath = context.getFilesDir().toString();
            Timber.w(t0, "Unable to load native libs. Trying to extract from APK to %s", destPath);
            try {
                for (String libFileName : libFiles) {
                    String libFilePath = destPath + File.separator + libFileName;
                    new File(libFilePath).delete();
                    Timber.d("Extract lib (%s) %s to %s...", Build.CPU_ABI, libFileName, destPath);

                    UnzipUtil.extractFile(appInfo.sourceDir, "lib/" + Build.CPU_ABI + "/" + libFileName, destPath);
                    db.put(libFileName, libFilePath);
                    System.load(libFilePath);
                }

                MinterSDK.setEnabledNativeLibs(true);
            } catch (IOException t2) {
                // extractFile to app files dir did not work. Not enough space? Try elsewhere...

                if (context.getExternalCacheDir() == null) {
                    throw new RuntimeException("External memory is unavailable");
                }
                destPath = context.getExternalCacheDir().toString();
                Timber.w(t2, "Unable to extract native libs into app directory. Trying to extract from APK to external cache %s", destPath);
                // Note: location on external memory is not secure, everyone can read/write it...
                // However we extract from a "secure" place (our apk) and instantly load it,
                // on each start of the app, this should make it safer.

                try {
                    for (String libFileName : libFiles) {
                        String libFilePath = destPath + File.separator + libFileName;
                        // this copy could be old, or altered by an attack
                        new File(libFilePath).delete();
                        Timber.d("Extract lib (%s) %s to %s...", Build.CPU_ABI, libFileName, destPath);

                        UnzipUtil.extractFile(appInfo.sourceDir, "lib/" + Build.CPU_ABI + "/" + libFileName, destPath);
                        // we don't store path if file was extracted to external memory
                        System.load(libFilePath);
                    }

                    MinterSDK.setEnabledNativeLibs(true);
                } catch (IOException t3) {
                    Timber.e(t3, "Can't load native libs after trying unzip native libs");
                    MinterSDK.setEnabledNativeLibs(false);
                    throw new RuntimeException(t3);
                }
            }
        }
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
    public KVStorage provideSecretKVStorage() {
        return new KVStorage();
    }

    @Provides
    @WalletApp
    @DbCache
    public KVStorage provideCacheKVStorage() {
        return new KVStorage(DB_CACHE);
    }

    @Provides
    @WalletApp
    public Context provideContext() {
        return mContext;
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
    public SharedPreferences providePreferences(Context context) {
        return context
                .getSharedPreferences(context.getString(R.string.user_local_settings_key),
                        Context.MODE_PRIVATE);
    }

    @Provides
    @WalletApp
    public ForegroundDetector provideForegroundDetector() {
        return new ForegroundDetector();
    }

    @Provides
    @WalletApp
    public SettingsManager provideSettingsManager(@DbCache KVStorage storage) {
        return new SettingsManager(storage);
    }

    @Provides
    public RxMinterLedger provideLedger(Context context) {
        return new RxMinterLedger(context, (UsbManager) context.getSystemService(USB_SERVICE));
    }

    @Provides
    @WalletApp
    public ErrorManager provideErrorManager() {
        return new ErrorManager();
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

    private void initCrashlytics() {
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(mEnableExternalLog);
    }

    public final static class WalletGsonHawkParer extends GsonParser {

        public WalletGsonHawkParer(GsonBuilder gsonBuilder) {
            super(gsonBuilder);
        }

        public WalletGsonHawkParer(Gson gson) {
            super(gson);
        }

        @Override
        public <T> T fromJson(String content, Type type) throws JsonSyntaxException {
            try {
                return super.fromJson(content, type);
            } catch (Throwable t) {
                Timber.e(t, "Unable to decode json object:\n----\n%s\n----\n", content);
                throw t;
            }
        }
    }
}

