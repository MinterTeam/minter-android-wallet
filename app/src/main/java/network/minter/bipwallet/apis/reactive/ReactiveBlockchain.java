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

package network.minter.bipwallet.apis.reactive;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;

import androidx.annotation.NonNull;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Function;
import network.minter.bipwallet.apis.dummies.BCErrorMapped;
import network.minter.blockchain.MinterBlockChainApi;
import network.minter.blockchain.models.BCResult;
import network.minter.core.internal.exceptions.NetworkException;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.HttpException;
import retrofit2.Response;
import timber.log.Timber;

/**
 * minter-android-wallet. 2019
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
public final class ReactiveBlockchain {

    public static <T> Observable<T> rxBc(Call<T> call) {
        return Observable.create(emitter -> call.clone().enqueue(new Callback<T>() {
            @SuppressWarnings("unchecked")
            @Override
            public void onResponse(@NonNull Call<T> call1, @NonNull Response<T> response) {
                if (response.body() == null) {
                    emitter.onNext((T) createBcError(response));
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

    public static <T> Function<? super Throwable, ? extends ObservableSource<? extends BCResult<T>>> convertToBcErrorResult() {
        return (Function<Throwable, ObservableSource<? extends BCResult<T>>>) throwable
                -> {

            final BCErrorMapped<T> errResult = new BCErrorMapped<>();
            if (errResult.mapError(throwable)) {
                return Observable.just(errResult);
            }

            if (throwable instanceof HttpException) {
                return Observable.just(createBcError(((HttpException) throwable)));
            }

            return Observable.just(createBcErrorPlain(NetworkException.convertIfNetworking(throwable)));
        };
    }

    public static <T> BCResult<T> createBcErrorPlain(Throwable t) {
        Throwable e = NetworkException.convertIfNetworking(t);
        if (e instanceof NetworkException) {
            return createBcErrorPlain(((NetworkException) e).getUserMessage(), BCResult.ResultCode.UnknownError, ((NetworkException) e).getStatusCode());
        }

        return createBcErrorPlain(e.getMessage(), BCResult.ResultCode.UnknownError, -1);
    }

    public static <T> BCResult<T> createBcErrorPlain(final String errorMessage, BCResult.ResultCode code, int statusCode) {
        BCResult<T> errorRes = new BCResult<>();
        errorRes.error = new BCResult.ErrorResult();
        errorRes.error.data = errorMessage;
        errorRes.error.code = code.getValue();
        errorRes.statusCode = statusCode;
        return errorRes;
    }

    public static <T> BCResult<T> createBcError(final String json, int code, String message) {
        Gson gson = MinterBlockChainApi.getInstance().getGsonBuilder().create();

        BCResult<T> out;
        try {
            if (json == null || json.isEmpty()) {
                out = createBcEmpty(code, message);
            } else {
                out = gson.fromJson(json, new TypeToken<BCResult<T>>() {
                }.getType());
            }

        } catch (Exception e) {
            Timber.e(e, "Unable to parse blockchain error: %s", json);
            out = createBcEmpty(code, message);
        }

        return out;
    }

    public static <T> BCResult<T> createBcError(final Response<T> response) {
        final String errorBodyString;
        try {
            // нельзя после этой строки пытаться вытащить body из ошибки,
            // потому что retrofit по какой-то причине не хранит у себя это значение
            // а держит в буффере до момента первого доступа
            errorBodyString = response.errorBody().string();
        } catch (IOException e) {
            Timber.e(e, "Unable to resolve http exception response");
            return createBcEmpty(response.code(), response.message());
        }

        final BCResult<T> out = createBcError(errorBodyString, response.code(), response.message());
        out.statusCode = response.code();
        return out;
    }

    public static <T> BCResult<T> createBcError(final HttpException exception) {
        final String errorBodyString;
        try {
            // нельзя после этой строки пытаться вытащить body из ошибки,
            // потому что retrofit по какой-то причине не хранит у себя это значение
            // а держит в буффере до момента первого доступа
            errorBodyString = ((HttpException) exception).response().errorBody().string();
        } catch (IOException e) {
            Timber.e(e, "Unable to resolve http exception response");
            return createBcEmpty(exception.code(), exception.message());
        }

        return createBcError(errorBodyString, exception.code(), exception.message());
    }

    public static <T> BCResult<T> createBcEmpty(int code, String message) {
        BCResult<T> out = new BCResult<>();
        out.statusCode = code;
        out.error = new BCResult.ErrorResult();
        out.error.message = String.format("%d %s", code, message);
        return out;
    }
}
