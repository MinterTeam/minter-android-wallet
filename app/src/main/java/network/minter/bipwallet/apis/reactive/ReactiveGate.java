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

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Function;
import network.minter.bipwallet.apis.dummies.GateErrorMapped;
import network.minter.core.internal.exceptions.NetworkException;
import network.minter.explorer.MinterExplorerSDK;
import network.minter.explorer.models.GateResult;
import retrofit2.Call;
import retrofit2.HttpException;
import retrofit2.Response;
import timber.log.Timber;

/**
 * minter-android-wallet. 2019
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
public final class ReactiveGate {

    @SuppressWarnings("unchecked")
    public static <T> Observable<T> rxGate(Call<T> call) {
        return Observable.create(emitter -> {
            Response<T> res;
            try {
                res = call.execute();
            } catch (Throwable t) {
                emitter.onError(NetworkException.convertIfNetworking(t));
                return;
            }

            if (res.body() == null) {
                emitter.onNext((T) createGateError(res));
            } else {
                emitter.onNext(res.body());
            }
            emitter.onComplete();

        });
    }

    public static <T> Function<? super Throwable, ? extends ObservableSource<? extends GateResult<T>>> toGateError() {
        return (Function<Throwable, ObservableSource<? extends GateResult<T>>>) throwable
                -> {

            if (throwable instanceof HttpException) {
                return Observable.just(createGateError(((HttpException) throwable)));
            }

            GateErrorMapped<T> errResult = new GateErrorMapped<>();
            if (errResult.mapError(throwable)) {
                return Observable.just(errResult);
            }

            return Observable.error(throwable);
        };
    }

    public static <T> GateResult<T> createGateError(final String json, int code, String message) {
        Gson gson = MinterExplorerSDK.getInstance().getGsonBuilder().create();
        GateResult<T> out;
        try {
            if (json == null || json.isEmpty()) {
                out = createGateEmpty(code, message);
            } else {
                out = gson.fromJson(json, new TypeToken<GateResult<T>>() {
                }.getType());
            }
        } catch (Exception e) {
            out = createGateEmpty(code, message);
        }

        return out;
    }

    public static <T> GateResult<T> createGateError(final Response<T> response) {
        final String errorBodyString;
        try {
            // нельзя после этой строки пытаться вытащить body из ошибки,
            // потому что retrofit по какой-то причине не хранит у себя это значение
            // а держит в буффере до момента первого доступа
            errorBodyString = response.errorBody().string();
        } catch (IOException e) {
            Timber.e(e, "Unable to resolve http exception response");
            return createGateEmpty(response.code(), response.message());
        }

        return createGateError(errorBodyString, response.code(), response.message());
    }

    public static <T> GateResult<T> createGateError(final HttpException exception) {
        final String errorBodyString;
        try {
            // нельзя после этой строки пытаться вытащить body из ошибки,
            // потому что retrofit по какой-то причине не хранит у себя это значение
            // а держит в буффере до момента первого доступа
            errorBodyString = ((HttpException) exception).response().errorBody().string();
        } catch (IOException e) {
            Timber.e(e, "Unable to resolve http exception response");
            return createGateEmpty(exception.code(), exception.message());
        }

        return createGateError(errorBodyString, exception.code(), exception.message());
    }

    public static <T> GateResult<T> createDummy(T result) {
        GateResult<T> out = new GateResult<>();
        out.result = result;
        return out;
    }

    public static <T> GateResult<T> createGateEmpty(int code, String message) {
        GateResult<T> out = new GateResult<>();
        out.error = new GateResult.ErrorResult();
        out.error.message = message;
        out.error.code = code;
        out.statusCode = code;
        return out;
    }

    public static <T> GateResult<T> createGateErrorPlain(Throwable t) {
        final Throwable e = NetworkException.convertIfNetworking(t);
        if (e instanceof NetworkException) {
            return createGateErrorPlain(((NetworkException) e).getUserMessage(), -1, ((NetworkException) e).getStatusCode());
        }

        return createGateErrorPlain(t.getMessage(), -1, -1);

    }

    public static <T> GateResult<T> createGateErrorPlain(final String errorMessage, int code, int statusCode) {
        GateResult<T> errorRes = new GateResult<>();
        errorRes.error = new GateResult.ErrorResult();
        errorRes.error.message = errorMessage;
        errorRes.error.code = code;
        errorRes.statusCode = statusCode;
        return errorRes;
    }
}
