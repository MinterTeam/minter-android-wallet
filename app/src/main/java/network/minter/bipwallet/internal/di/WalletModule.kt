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
package network.minter.bipwallet.internal.di

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.os.Build
import com.edwardstock.secp256k1.NativeSecp256k1
import com.fatboyindustrial.gsonjodatime.Converters
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.orhanobut.hawk.ConcealEncryption
import com.orhanobut.hawk.GsonParser
import com.orhanobut.hawk.Hawk
import com.orhanobut.hawk.NoEncryption
import dagger.Module
import dagger.Provides
import io.reactivex.exceptions.CompositeException
import io.reactivex.schedulers.Schedulers
import net.danlew.android.joda.JodaTimeAndroid
import network.minter.bipwallet.BuildConfig
import network.minter.bipwallet.R
import network.minter.bipwallet.internal.Wallet
import network.minter.bipwallet.internal.Wallet.CrashlyticsTree
import network.minter.bipwallet.internal.auth.AuthSession
import network.minter.bipwallet.internal.di.annotations.DbCache
import network.minter.bipwallet.internal.exceptions.ErrorManager
import network.minter.bipwallet.internal.gson.BigDecimalJsonConverter
import network.minter.bipwallet.internal.gson.HistoryTransactionsJsonConverter
import network.minter.bipwallet.internal.helpers.DateHelper
import network.minter.bipwallet.internal.helpers.Plurals
import network.minter.bipwallet.internal.settings.SettingsManager
import network.minter.bipwallet.internal.storage.KVStorage
import network.minter.bipwallet.internal.system.ForegroundDetector
import network.minter.bipwallet.internal.system.UnzipUtil
import network.minter.bipwallet.services.livebalance.RTMService
import network.minter.blockchain.MinterBlockChainSDK
import network.minter.core.MinterSDK
import network.minter.core.bip39.NativeBip39
import network.minter.core.crypto.*
import network.minter.core.internal.api.ApiService
import network.minter.core.internal.api.converters.*
import network.minter.core.internal.exceptions.NativeLoadException
import network.minter.core.internal.log.TimberLogger
import network.minter.explorer.MinterExplorerSDK.Setup
import network.minter.explorer.models.HistoryTransaction
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.lang.reflect.Type
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*
import javax.inject.Named

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock></edward.vstock>@gmail.com>
 */
@Module
class WalletModule(
        private val mContext: Context,
        private val mDebug: Boolean,
        private val mEnableExternalLog: Boolean) {

    init {
        initCrashlytics()
        MinterBlockChainSDK.initialize(mDebug)

        @Suppress("UNNECESSARY_SAFE_CALL")
        BuildConfig.LIVE_BALANCE_URL?.let {
            RTMService.LIVE_BALANCE_URL = BuildConfig.LIVE_BALANCE_URL
        }

        val explorerSdk = Setup().setEnableDebug(mDebug)

        /* for test purposes only
        if(BuildConfig.FLAVOR.startsWith("netMain")) {
            explorerSdk.setNetId("chilinet");
        }
        */
        if (BuildConfig.EXPLORER_API_URL != null) {
            explorerSdk.setExplorerApiUrl(BuildConfig.EXPLORER_API_URL)
        }
        if (BuildConfig.GATE_API_URL != null) {
            explorerSdk.setGateApiUrl(BuildConfig.GATE_API_URL)
        }
        explorerSdk.setLogger(TimberLogger())
        explorerSdk.init()

        val parser = WalletGsonHawkParer(gson)
        Hawk.init(mContext)
                .setParser(parser)
                .setEncryption(ConcealEncryption(mContext))
                .build()
        Hawk.init(DB_CACHE, mContext)
                .setEncryption(NoEncryption())
                .setParser(parser)
                .build()
        initCoreSdk(mContext)
        Timber.uprootAll()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        if (Wallet.ENABLE_CRASHLYTICS) {
            Timber.plant(CrashlyticsTree())
        }
        JodaTimeAndroid.init(mContext)
        Plurals.init(mContext.resources)
    }

    @Provides
    @WalletApp
    @Named("uuid")
    fun provideUUID(preferences: SharedPreferences): String {
        val appKey = mContext.getString(R.string.app_name) + "_uuid"
        val uuid = UUID.randomUUID().toString().uppercase(Locale.getDefault())
        if (preferences.contains(appKey)) {
            return preferences.getString(appKey, uuid)!!
        }
        preferences.edit()
                .putString(appKey, uuid)
                .apply()
        return uuid
    }

    @Provides
    @WalletApp
    fun provideSecretKVStorage(): KVStorage {
        return KVStorage()
    }

    @Provides
    @WalletApp
    @DbCache
    fun provideCacheKVStorage(): KVStorage {
        return KVStorage(DB_CACHE)
    }

    @Provides
    @WalletApp
    fun provideContext(): Context {
        return mContext
    }

    @Provides
    fun provideResources(context: Context): Resources {
        return context.resources
    }

    @Provides
    fun provideApiService(session: AuthSession, gsonBuilder: GsonBuilder?): ApiService.Builder {
        val builder = ApiService.Builder("", gsonBuilder)
        builder
                .setEmptyAuthTokenListener { session.logout() }
                .setDebug(mDebug)
                .setDebugRequestLevel(HttpLoggingInterceptor.Level.BODY)
                .setAuthHeaderName("Authorization")
                .addHeader("User-Agent", "Minter Android " + BuildConfig.VERSION_CODE)
                .addHeader("X-Client-Version", BuildConfig.VERSION_NAME)
                .addHeader("X-Client-Build", BuildConfig.VERSION_CODE.toString())
        return builder
    }

    @Provides
    @Named("stories")
    fun provideStoriesApiService(): ApiService.Builder {
        val api = ApiService.Builder(BuildConfig.STORIES_API_URL)
        api.setDebug(mDebug)
        api.addHeader("User-Agent", "Minter Android " + BuildConfig.VERSION_CODE)
        api.addHeader("X-Client-Version", BuildConfig.VERSION_NAME)
        api.addHeader("X-Client-Build", BuildConfig.VERSION_CODE.toString())
        api.addHeader("Content-Type", "application/json")
        api.addHeader("Accept-Language", Locale.getDefault().language + "-" + Locale.getDefault().country)
        var dateFormat = "yyyy-MM-dd HH:mm:ssX"
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            dateFormat = "yyyy-MM-dd HH:mm:ssZ"
        }
        api.setDateFormat(dateFormat)
        api.setRetrofitClientConfig { builder -> builder.addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io())) }
        return api
    }

    @Provides
    @Named("chainik")
    fun provideChainikApiService(): ApiService.Builder {
        val api = ApiService.Builder(BuildConfig.CHAINIK_API_URL)
        api.setDebug(mDebug)
        api.addHeader("User-Agent", "Minter Android " + BuildConfig.VERSION_CODE)
        api.addHeader("X-Client-Version", BuildConfig.VERSION_NAME)
        api.addHeader("X-Client-Build", BuildConfig.VERSION_CODE.toString())
        api.addHeader("Content-Type", "application/json")
        api.addHeader("Accept-Language", Locale.getDefault().language + "-" + Locale.getDefault().country)
        var dateFormat = "yyyy-MM-dd HH:mm:ssX"
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            dateFormat = "yyyy-MM-dd HH:mm:ssZ"
        }
        api.setDateFormat(dateFormat)
        api.setRetrofitClientConfig { builder: Retrofit.Builder -> builder.addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io())) }
        return api
    }

    @Provides
    @WalletApp
    fun provideAuthSession(sessionStorage: KVStorage?): AuthSession {
        return AuthSession(sessionStorage!!)
    }

    @Provides
    fun provideGsonBuilder(): GsonBuilder {
        val gsonBuilder = GsonBuilder()
                .setDateFormat(DateHelper.DATE_FORMAT_SIMPLE)
        Converters.registerAll(gsonBuilder)
        return gsonBuilder
    }

    @Provides
    fun providePreferences(context: Context): SharedPreferences {
        return context
                .getSharedPreferences(context.getString(R.string.user_local_settings_key),
                        Context.MODE_PRIVATE)
    }

    @Provides
    @WalletApp
    fun provideForegroundDetector(): ForegroundDetector {
        return ForegroundDetector()
    }

    @Provides
    @WalletApp
    fun provideSettingsManager(@DbCache storage: KVStorage?): SettingsManager {
        return SettingsManager(storage!!)
    }

    @Provides
    @WalletApp
    fun provideErrorManager(): ErrorManager {
        return ErrorManager()
    }

    private val gson: GsonBuilder
        get() {
            val out = GsonBuilder()
            out.registerTypeAdapter(BigInteger::class.java, BigIntegerJsonConverter())
            out.registerTypeAdapter(BigDecimal::class.java, BigDecimalJsonConverter())
            out.registerTypeAdapter(MinterAddress::class.java, MinterAddressJsonConverter())
            out.registerTypeAdapter(MinterPublicKey::class.java, MinterPublicKeyJsonConverter())
            out.registerTypeAdapter(MinterHash::class.java, MinterHashJsonConverter())
            out.registerTypeAdapter(MinterCheck::class.java, MinterCheckJsonConverter())
            out.registerTypeAdapter(BytesData::class.java, BytesDataJsonConverter())
            out.registerTypeAdapter(HistoryTransaction::class.java, HistoryTransactionsJsonConverter())
            return out
        }

    private fun initCrashlytics() {
        if (mEnableExternalLog) {
            try {
                FirebaseApp.initializeApp(mContext)
            } catch (ignore: IllegalStateException) {
                // it must create instance by itself but on some devices it doesn't work
            }
            try {
                FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(mEnableExternalLog)
            } catch (err1: Throwable) {
                try {
                    Thread.sleep(200)
                } catch (e: InterruptedException) {
                }
                try {
                    FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(mEnableExternalLog)
                } catch (wtfGoogle: Throwable) {
                    Timber.e(CompositeException(err1, wtfGoogle), "Crashed Crashlytics")
                }
            }
        }
    }

    class WalletGsonHawkParer : GsonParser {
        constructor(gsonBuilder: GsonBuilder?) : super(gsonBuilder)
        constructor(gson: Gson?) : super(gson)

        @Throws(JsonSyntaxException::class)
        override fun <T> fromJson(content: String, type: Type): T {
            return try {
                super.fromJson(content, type)
            } catch (t: Throwable) {
                Timber.e(t, "Unable to decode json object:\n----\n%s\n----\n", content)
                throw t
            }
        }
    }

    companion object {
        const val DB_CACHE = "minter_cache"

        @JvmStatic
        @SuppressLint("UnsafeDynamicallyLoadedCode")
        fun initCoreSdk(context: Context) {
            val db = Hawk.db()
            try {
                // Try loading our native lib, see if it works...
                MinterSDK.initialize()
            } catch (t0: NativeLoadException) {
                val appInfo = context.applicationInfo
                Timber.d("Source apk file: %s", appInfo.sourceDir)
                val libFiles: MutableList<String> = ArrayList(NativeBip39.LIB_FILES.size + NativeSecp256k1.LIB_FILES.size)

                libFiles.addAll(NativeBip39.LIB_FILES.toList())
                libFiles.addAll(NativeSecp256k1.LIB_FILES.toList())
                val iter = libFiles.iterator()
                while (iter.hasNext()) {
                    val libFileName = iter.next()
                    val libFilePath = db.get<String>(libFileName)
                    if (db.contains(libFileName) && File(libFilePath).exists()) {
                        iter.remove()
                    }
                    System.load(libFilePath)
                }
                if (libFiles.isEmpty()) {
                    Timber.d("Loaded cached lib files")
                    MinterSDK.setEnabledNativeLibs(true)
                    return
                }
                var destPath = context.filesDir.toString()
                Timber.w(t0, "Unable to load native libs. Trying to extract from APK to %s", destPath)
                try {
                    for (libFileName in libFiles) {
                        val libFilePath = destPath + File.separator + libFileName
                        File(libFilePath).delete()
                        Timber.d("Extract lib (%s) %s to %s...", Build.CPU_ABI, libFileName, destPath)
                        UnzipUtil.extractFile(appInfo.sourceDir, "lib/" + Build.CPU_ABI + "/" + libFileName, destPath)
                        db.put(libFileName, libFilePath)
                        System.load(libFilePath)
                    }
                    MinterSDK.setEnabledNativeLibs(true)
                } catch (t2: IOException) {
                    // extractFile to app files dir did not work. Not enough space? Try elsewhere...
                    if (context.externalCacheDir == null) {
                        throw RuntimeException("External memory is unavailable")
                    }
                    destPath = context.externalCacheDir.toString()
                    Timber.w(t2, "Unable to extract native libs into app directory. Trying to extract from APK to external cache %s", destPath)
                    // Note: location on external memory is not secure, everyone can read/write it...
                    // However we extract from a "secure" place (our apk) and instantly load it,
                    // on each start of the app, this should make it safer.
                    try {
                        for (libFileName in libFiles) {
                            val libFilePath = destPath + File.separator + libFileName
                            // this copy could be old, or altered by an attack
                            File(libFilePath).delete()
                            Timber.d("Extract lib (%s) %s to %s...", Build.CPU_ABI, libFileName, destPath)
                            UnzipUtil.extractFile(appInfo.sourceDir, "lib/" + Build.CPU_ABI + "/" + libFileName, destPath)
                            // we don't store path if file was extracted to external memory
                            System.load(libFilePath)
                        }
                        MinterSDK.setEnabledNativeLibs(true)
                    } catch (t3: IOException) {
                        Timber.e(t3, "Can't load native libs after trying unzip native libs")
                        MinterSDK.setEnabledNativeLibs(false)
                        throw RuntimeException(t3)
                    }
                }
            }
        }
    }
}