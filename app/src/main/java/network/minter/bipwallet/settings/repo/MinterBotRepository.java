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

package network.minter.bipwallet.settings.repo;

import com.google.gson.JsonDeserializer;

import org.parceler.Parcel;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.annotation.Nonnull;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Function;
import network.minter.bipwallet.BuildConfig;
import network.minter.bipwallet.internal.Wallet;
import network.minter.bipwallet.settings.api.MinterBotEndpoint;
import network.minter.core.crypto.HashUtil;
import network.minter.core.crypto.MinterAddress;
import network.minter.core.internal.api.ApiService;
import network.minter.core.internal.data.DataRepository;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.HttpException;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import timber.log.Timber;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
public class MinterBotRepository extends DataRepository<MinterBotEndpoint> implements DataRepository.Configurator {
    public MinterBotRepository() {
        super(new ApiService.Builder("https://minter-bot-wallet.dl-dev.ru/api/")
                .setDebugRequestLevel(HttpLoggingInterceptor.Level.BODY)
                .setDebug(true)
                .setRetrofitClientConfig(b -> {
                    b.addCallAdapterFactory(RxJava2CallAdapterFactory.create());
                })
                .addHeader("Content-Type", "application/json"));
    }

    public Observable<MinterBotResult> requestFreeCoins(MinterAddress address) {

        return getInstantService().requestFreeCoins(new HashMap<String, String>() {{
            put("address", address.toString());
            put("signature", makeSignature(address));
        }})
                .onErrorResumeNext(new Function<Throwable, ObservableSource<MinterBotResult>>() {
                    @Override
                    public ObservableSource<MinterBotResult> apply(Throwable throwable) {
                        if (!(throwable instanceof HttpException)) {
                            return Observable.error(throwable);
                        }

                        MinterBotResult errResult;
                        try {
                            // нельзя после этой строки пытаться вытащить body из ошибки,
                            // потому что retrofit по какой-то причине не хранит у себя это значение
                            // а держит в буффере до момента первого доступа
                            final String errorBodyString = ((HttpException) throwable).response().errorBody().string();
                            errResult = Wallet.app().gsonBuilder().create().fromJson(errorBodyString, MinterBotResult.class);
                        } catch (Throwable e) {
                            Timber.e(e, "Unable to resolve http exception response");
                            errResult = new MinterBotResult();
                            errResult.data = throwable.getMessage();
                        }

                        return Observable.just(errResult);
                    }
                });
    }

    private String makeSignature(MinterAddress address) {
        TimeZone timeZone = TimeZone.getTimeZone("UTC");
        Calendar calendar = Calendar.getInstance(timeZone);
        return HashUtil.sha256Hex(String.format("%s%s%02d", address, BuildConfig.MINTER_BOT_SECRET, calendar.get(Calendar.HOUR_OF_DAY)));
    }

    @Override
    public void configure(ApiService.Builder api) {
        api.registerTypeAdapter(MinterBotResult.class, (JsonDeserializer<MinterBotResult>) (jsonElement, type, jsonDeserializationContext) -> {
            if (jsonElement.isJsonArray() && jsonElement.getAsJsonArray().size() == 0) {
                return new MinterBotResult();
            }

            return Wallet.app().gsonBuilder().create().fromJson(jsonElement, MinterBotResult.class);
        });
    }

    @Nonnull
    @Override
    protected Class<MinterBotEndpoint> getServiceClass() {
        return MinterBotEndpoint.class;
    }

    @Parcel
    public static final class MinterBotResult {
        public String data;
        public Map<String, List<String>> errors;

        public boolean isOk() {
            return data == null || errors == null || errors.isEmpty();
        }

        public String getError() {
            if (errors == null || errors.isEmpty()) {
                return data;
            }
            StringBuilder b = new StringBuilder();

            for (Map.Entry<String, List<String>> entry : errors.entrySet()) {
                for (String s : entry.getValue()) {
                    b.append(" - ").append(s).append("\n");
                }
            }

            return b.toString();
        }
    }

}
