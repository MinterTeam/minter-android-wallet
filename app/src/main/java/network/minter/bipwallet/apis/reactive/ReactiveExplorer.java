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

import org.joda.time.DateTime;
import org.joda.time.Seconds;

import java.io.IOException;
import java.util.Date;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Function;
import network.minter.bipwallet.apis.dummies.ExpErrorMapped;
import network.minter.bipwallet.internal.Wallet;
import network.minter.core.internal.exceptions.NetworkException;
import network.minter.explorer.MinterExplorerApi;
import network.minter.explorer.models.ExpResult;
import retrofit2.Call;
import retrofit2.HttpException;
import retrofit2.Response;
import timber.log.Timber;

/**
 * minter-android-wallet. 2019
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
public final class ReactiveExplorer {

    public static <T> Observable<T> rxExp(Call<T> call) {
        return Observable.create(emitter -> {
            Response<T> res;
            try {
                res = call.execute();
            } catch (Throwable t) {
                if (!emitter.isDisposed()) {
                    emitter.onError(NetworkException.convertIfNetworking(t));
                }

                return;
            }

            Date serverDate = res.headers().getDate("Date");
            if (serverDate != null) {
                DateTime d1 = new DateTime(serverDate);
                DateTime d2 = new DateTime();
                Seconds diff = Seconds.secondsBetween(d2, d1);
                Wallet.setTimeOffset(diff.getSeconds());
            }

            if (res.body() == null) {
                emitter.onNext((T) createExpError(res));
            } else {
                emitter.onNext(res.body());
            }
            emitter.onComplete();

        });
//        return Observable.create(emitter -> call.clone().enqueue(new Callback<T>() {
//            @SuppressWarnings("unchecked")
//            @Override
//            public void onResponse(@NonNull Call<T> call1, @NonNull Response<T> response) {
//                if (response.body() == null) {
//                    emitter.onNext((T) createExpError(response));
//                } else {
//                    emitter.onNext(response.body());
//                }
//
//                emitter.onComplete();
//            }
//
//            @Override
//            public void onFailure(@NonNull Call<T> call1, @NonNull Throwable t) {
//                emitter.onError(t);
//            }
//        }));
    }

    public static <T> Function<? super Throwable, ? extends ObservableSource<? extends ExpResult<T>>> toExpError() {
        return (Function<Throwable, ObservableSource<? extends ExpResult<T>>>) throwable
                -> {

            if (throwable instanceof HttpException) {
                return Observable.just(createExpError(((HttpException) throwable)));
            }

            ExpErrorMapped<T> errResult = new ExpErrorMapped<>();
            if (errResult.mapError(throwable)) {
                return Observable.just(errResult);
            }

            return Observable.just(createExpErrorPlain(NetworkException.convertIfNetworking(throwable)));
        };
    }

    public static <T> ExpResult<T> createExpErrorPlain(Throwable t) {
        final Throwable e = NetworkException.convertIfNetworking(t);
        if (e instanceof NetworkException) {
            return createExpErrorPlain(((NetworkException) e).getUserMessage(), -1, ((NetworkException) e).getStatusCode());
        }

        return createExpErrorPlain(t.getMessage(), -1, -1);

    }

    public static <T> ExpResult<T> createExpErrorPlain(final String errorMessage, int code, int statusCode) {
        ExpResult<T> errorRes = new ExpResult<>();
        errorRes.error = new ExpResult.ErrorResult();
        errorRes.error.message = errorMessage;
        errorRes.error.code = code;
        return errorRes;
    }

    public static <T> ExpResult<T> createExpError(final String json, int code, String message) {
        Gson gson = MinterExplorerApi.getInstance().getGsonBuilder().create();
        ExpResult<T> out;
        try {
            if (json == null || json.isEmpty()) {
                out = createExpEmpty(code, message);
            } else {
                out = gson.fromJson(json, new TypeToken<ExpResult<T>>() {
                }.getType());
            }
        } catch (Exception e) {
            out = createExpEmpty(code, message);
        }

        return out;
    }

    public static <T> ExpResult<T> createExpErrorRes(final Response<ExpResult<T>> response) {
        final String errorBodyString;
        try {
            // нельзя после этой строки пытаться вытащить body из ошибки,
            // потому что retrofit по какой-то причине не хранит у себя это значение
            // а держит в буффере до момента первого доступа
            errorBodyString = response.errorBody().string();
        } catch (IOException e) {
            Timber.e(e, "Unable to resolve http exception response");
            return createExpEmpty(response.code(), response.message());
        }

        return createExpError(errorBodyString, response.code(), response.message());
    }

    public static <T> ExpResult<T> createExpError(final Response<T> response) {
        final String errorBodyString;
        try {
            // нельзя после этой строки пытаться вытащить body из ошибки,
            // потому что retrofit по какой-то причине не хранит у себя это значение
            // а держит в буффере до момента первого доступа
            errorBodyString = response.errorBody().string();
        } catch (IOException e) {
            Timber.e(e, "Unable to resolve http exception response");
            return createExpEmpty(response.code(), response.message());
        }

        return createExpError(errorBodyString, response.code(), response.message());
    }

    public static <T> ExpResult<T> createExpError(final HttpException exception) {
        final String errorBodyString;
        try {
            // нельзя после этой строки пытаться вытащить body из ошибки,
            // потому что retrofit по какой-то причине не хранит у себя это значение
            // а держит в буффере до момента первого доступа
            errorBodyString = ((HttpException) exception).response().errorBody().string();
        } catch (IOException e) {
            Timber.e(e, "Unable to resolve http exception response");
            return createExpEmpty(exception.code(), exception.message());
        }

        return createExpError(errorBodyString, exception.code(), exception.message());
    }

    public static <T> ExpResult<T> createExpEmpty(int code, String message) {
        ExpResult<T> out = new ExpResult<>();
        out.error = new ExpResult.ErrorResult();
        out.error.message = message;
        out.error.code = code;
        return out;
    }
}
