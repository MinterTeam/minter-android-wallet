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

package network.minter.bipwallet.apis.reactive;

import android.support.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Function;
import network.minter.bipwallet.apis.dummies.ProfileErrorMapped;
import network.minter.profile.MinterProfileApi;
import network.minter.profile.models.ProfileResult;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.HttpException;
import retrofit2.Response;
import timber.log.Timber;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public final class ReactiveMyMinter {

    // MyMinter
    public static <T> Observable<T> rxProfile(Call<T> call) {
        return Observable.create(emitter -> call.clone().enqueue(new Callback<T>() {
            @SuppressWarnings("unchecked")
            @Override
            public void onResponse(@NonNull Call<T> call1, @NonNull Response<T> response) {
                if (response.body() == null) {
                    emitter.onNext((T) createProfileError(response));
                } else {
                    emitter.onNext(response.body());
                }

                emitter.onComplete();
            }

            @Override
            public void onFailure(@NonNull Call<T> call1, @NonNull Throwable t) {
                emitter.onError(t);
            }
        }));
    }

    public static <T> Function<? super Throwable, ? extends ObservableSource<? extends ProfileResult<T>>> toProfileError() {
        return (Function<Throwable, ObservableSource<? extends ProfileResult<T>>>) throwable
                -> {

            final ProfileErrorMapped<T> errResult = new ProfileErrorMapped<>();
            if (errResult.mapError(throwable)) {
                return Observable.just(errResult);
            }

            if (throwable instanceof HttpException) {
                return Observable.just(createProfileError(((HttpException) throwable)));
            }

            return Observable.error(throwable);
        };
    }

    public static <T> ProfileResult<T> createProfileError(final String json, int code, String message) {
        Gson gson = MinterProfileApi.getInstance().getGsonBuilder().create();

        ProfileResult<T> out;
        try {
            if (json == null || json.isEmpty()) {
                out = createProfileEmpty(code, message);
            } else {
                out = gson.fromJson(json, new TypeToken<ProfileResult<T>>() {
                }.getType());
            }

        } catch (Exception e) {
            Timber.e(e, "Unable to parse profile error: %s", json);
            out = createProfileEmpty(code, message);
        }

        return out;
    }

    public static <T> ProfileResult<T> createProfileError(final Response<T> response) {
        final String errorBodyString;
        try {
            // нельзя после этой строки пытаться вытащить body из ошибки,
            // потому что retrofit по какой-то причине не хранит у себя это значение
            // а держит в буффере до момента первого доступа
            errorBodyString = response.errorBody().string();
        } catch (IOException e) {
            Timber.e(e, "Unable to resolve http exception response");
            return createProfileEmpty(response.code(), response.message());
        }

        return createProfileError(errorBodyString, response.code(), response.message());
    }

    public static <T> ProfileResult<T> createProfileError(final HttpException exception) {
        final String errorBodyString;
        try {
            // нельзя после этой строки пытаться вытащить body из ошибки,
            // потому что retrofit по какой-то причине не хранит у себя это значение
            // а держит в буффере до момента первого доступа
            errorBodyString = ((HttpException) exception).response().errorBody().string();
        } catch (IOException e) {
            Timber.e(e, "Unable to resolve http exception response");
            return createProfileEmpty(exception.code(), exception.message());
        }

        return createProfileError(errorBodyString, exception.code(), exception.message());
    }

    public static <T> ProfileResult<T> createProfileEmpty(int code, String message) {
        ProfileResult<T> out = new ProfileResult<>();
        out.error = new ProfileResult.Error();
        out.error.code = String.valueOf(code);
        out.error.message = String.format("%d %s", code, message);
        return out;
    }
}
