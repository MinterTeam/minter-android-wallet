/*
 * Copyright (C) 2018 by MinterTeam
 * @link https://github.com/MinterTeam
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

import com.fatboyindustrial.gsonjodatime.Converters;
import com.google.gson.GsonBuilder;
import com.orhanobut.hawk.Hawk;

import net.danlew.android.joda.JodaTimeAndroid;

import java.util.UUID;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import network.minter.bipwallet.BuildConfig;
import network.minter.bipwallet.R;
import network.minter.bipwallet.internal.auth.AuthSession;
import network.minter.bipwallet.internal.auth.SessionStorage;
import network.minter.bipwallet.internal.helpers.DateHelper;
import network.minter.bipwallet.internal.storage.KVStorage;
import network.minter.blockchainapi.MinterBlockChainApi;
import network.minter.explorerapi.MinterExplorerApi;
import network.minter.mintercore.MinterSDK;
import network.minter.mintercore.internal.api.ApiService;
import network.minter.my.MyMinterApi;
import timber.log.Timber;

/**
 * MinterWallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@Module
public class WalletModule {
    private Context mContext;
    private boolean mDebug;
    private boolean mEnableExternalLog = false;

    public WalletModule(Context context, boolean debug, boolean enableExternalLog) {
        mContext = context;
        mDebug = debug;
        mEnableExternalLog = enableExternalLog;
        Hawk.init(mContext)
                .setLogInterceptor(message -> Timber.tag("Hawk").d(message))
                .build();

        MinterSDK.initialize();
        MinterBlockChainApi.initialize(debug);
        MinterExplorerApi.initialize(debug);
        MyMinterApi.initialize(debug);
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
//        ApiService.IntentBuilder builder = new ApiService.IntentBuilder(BuildConfig.BASE_API_URL, gsonBuilder);
        ApiService.Builder builder = new ApiService.Builder("", gsonBuilder);

        builder
                .setEmptyAuthTokenListener(session::logout)
//                .setTokenGetter(() -> String.format("Bearer %s", session.getAuthToken()))
                .setDebug(mDebug)
                .setAuthHeaderName("Authorization")
                .addHeader("User-Agent", "Minter Android " + String.valueOf(BuildConfig.VERSION_CODE))
                .addHeader("X-Client-Version", BuildConfig.VERSION_NAME)
                .addHeader("X-Client-Build", String.valueOf(BuildConfig.VERSION_CODE));

        return builder;
    }

    @Provides
    @WalletApp
    public AuthSession provideAuthSession(SessionStorage sessionStorage) {
        return new AuthSession(sessionStorage);
    }

    @Provides
    public GsonBuilder provideGsonBuilder() {
        GsonBuilder gsonBuilder = new GsonBuilder()
                .setDateFormat(DateHelper.DATE_FORMAT_SIMPLE);

//        gsonBuilder.registerTypeAdapter(Dog.class, new DogDeserializer());
//        gsonBuilder.registerTypeAdapter(SitterShort.class, new SitterShortDeserializer<>());
//        gsonBuilder.registerTypeAdapter(Sitter.class, new SitterDeserializer());

        Converters.registerAll(gsonBuilder);

        return gsonBuilder;
    }


    @Provides
    @WalletApp
    public SessionStorage provideSessionStorage(Context context, GsonBuilder gsonBuilder) {
        return new SessionStorage(context, gsonBuilder);
    }

//    @Provides
//    @Named("sessionVerifier")
//    public Observable<Boolean> provideSessionVerifier(final AuthSession session) {
//        if (!session.isLoggedIn()) {
//            session.restore();
//        }
//
//        if (!session.isLoggedIn()) {
//            Timber.d("Session is not valid. Auth required.");
//            return Observable.just(false);
//        }
//
//        return Observable.create(emitter -> {
//            final TokenVerifier verifier = new TokenVerifier()
//                    .setDebug(mDebug)
//                    .setMethod(TokenVerifier.Method.POST)
//                    .setBaseUrl(BuildConfig.BASE_API_URL)
//                    .setToken(session.getAuthToken())
//                    .setPath("/v1/auth/verify");
//
//            verifier.verify()
//                    .observeOn(AndroidSchedulers.mainThread())
//                    .subscribeOn(Schedulers.io())
//                    .subscribe(result -> {
//                        Timber.i("Restoring session. Everything is ok");
//                        if (verifier.isValid()) {
//                            //@TODO
////                                String token = FirebaseInstanceId.getInstance().getToken();
////                                if (token != null) {
//                            // userRepository.setDeviceToken(token);
////                                }
//
//                            emitter.onNext(true);
//                            emitter.onComplete();
//                        } else {
//                            session.logout();
//                            emitter.onNext(false);
//                            emitter.onComplete();
//                        }
//                    }, t -> {
//                        if (t instanceof NetworkException) {
//                            if (((NetworkException) t).getStatusCode() == 401 || ((NetworkException) t).getStatusCode() == 403) {
//                                Timber.i("Token is not valid. Destroying session");
//                                session.logout();
//                                emitter.onNext(false);
//                                emitter.onComplete();
//                                return;
//                            }
//                        }
//
//                        emitter.onError(t);
//                    });
//        });
//    }

//    @Provides
//    @Named("userId")
//    @BipApp
//    public long provideUserId(AuthSession session) {
//        return session.getUser() != null ? session.getUser().getId() : 0;
//    }

    @Provides
    public SharedPreferences providePreferences(Context context) {
        return context
                .getSharedPreferences(context.getString(R.string.user_local_settings_key),
                                      Context.MODE_PRIVATE);
    }
}

