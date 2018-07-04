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

package network.minter.bipwallet.internal;

import android.support.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.EOFException;
import java.io.IOException;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Function;
import network.minter.blockchainapi.MinterBlockChainApi;
import network.minter.blockchainapi.models.BCResult;
import network.minter.explorerapi.models.ExpResult;
import network.minter.my.MyMinterApi;
import network.minter.my.models.MyResult;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.HttpException;
import retrofit2.Response;
import timber.log.Timber;

/**
 * MinterWallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class ReactiveAdapter {

    // MyMinter
    public static <T> Observable<T> rxCallMy(Call<T> call) {
        return Observable.create(emitter -> call.clone().enqueue(new Callback<T>() {
            @Override
            public void onResponse(@NonNull Call<T> call1, @NonNull Response<T> response) {
                if (response.body() == null) {
                    emitter.onNext((T) createMyErrorResult(response));
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

    public static <T> Function<? super Throwable, ? extends ObservableSource<? extends MyResult<T>>> convertToMyErrorResult() {
        return (Function<Throwable, ObservableSource<? extends MyResult<T>>>) throwable
                -> {
            if (!(throwable instanceof HttpException)) {
                return Observable.error(throwable);
            }

            return Observable.just(createMyErrorResult(((HttpException) throwable)));
        };
    }

    public static <T> MyResult<T> createMyErrorResult(final String json) {
        Gson gson = MyMinterApi.getInstance().getGsonBuilder().create();
        return gson.fromJson(json, new TypeToken<MyResult<T>>() {
        }.getType());
    }

    public static <T> MyResult<T> createMyErrorResult(final Response<T> response) {
        final String errorBodyString;
        try {
            // нельзя после этой строки пытаться вытащить body из ошибки,
            // потому что retrofit по какой-то причине не хранит у себя это значение
            // а держит в буффере до момента первого доступа
            errorBodyString = response.errorBody().string();
        } catch (IOException e) {
            Timber.e(e, "Unable to resolve http exception response");
            return createMyEmpty();
        }

        return createMyErrorResult(errorBodyString);
    }

    public static <T> MyResult<T> createMyErrorResult(final HttpException exception) {
        final String errorBodyString;
        try {
            // нельзя после этой строки пытаться вытащить body из ошибки,
            // потому что retrofit по какой-то причине не хранит у себя это значение
            // а держит в буффере до момента первого доступа
            errorBodyString = ((HttpException) exception).response().errorBody().string();
        } catch (IOException e) {
            Timber.e(e, "Unable to resolve http exception response");
            return createMyEmpty();
        }

        return createMyErrorResult(errorBodyString);
    }

    public static <T> MyResult<T> createMyEmpty() {
        return new MyResult<>();
    }


    // Blockhain

    public static <T> Observable<T> rxCallBc(Call<T> call) {
        return Observable.create(emitter -> call.clone().enqueue(new Callback<T>() {
            @Override
            public void onResponse(@NonNull Call<T> call1, @NonNull Response<T> response) {
                if (response.body() == null) {
                    emitter.onNext((T) createBcErrorResult(response));
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
            if (throwable instanceof IOException && throwable.getCause() instanceof EOFException) {
                // blockchain api sometimes instead of 404 gives empty response, just handle it as 404
                final BCResult<T> errResult = new BCResult<>();
                errResult.code = BCResult.ResultCode.EmptyResponse;
                errResult.message = "Not found";
                errResult.result = null;
                errResult.statusCode = 404;
                return Observable.just(errResult);
            }

            if (!(throwable instanceof HttpException)) {
                return Observable.error(throwable);
            }

            return Observable.just(createBcErrorResult(((HttpException) throwable)));
        };
    }

    public static <T> BCResult<T> createBcErrorResult(final String json) {
        Gson gson = MinterBlockChainApi.getInstance().getGsonBuilder().create();
        return gson.fromJson(json, new TypeToken<BCResult<T>>() {
        }.getType());
    }

    public static <T> BCResult<T> createBcErrorResult(final Response<T> response) {
        final String errorBodyString;
        try {
            // нельзя после этой строки пытаться вытащить body из ошибки,
            // потому что retrofit по какой-то причине не хранит у себя это значение
            // а держит в буффере до момента первого доступа
            errorBodyString = response.errorBody().string();
        } catch (IOException e) {
            Timber.e(e, "Unable to resolve http exception response");
            return createBcEmpty();
        }

        final BCResult<T> out = createBcErrorResult(errorBodyString);
        out.statusCode = response.code();
        return out;
    }

    public static <T> BCResult<T> createBcErrorResult(final HttpException exception) {
        final String errorBodyString;
        try {
            // нельзя после этой строки пытаться вытащить body из ошибки,
            // потому что retrofit по какой-то причине не хранит у себя это значение
            // а держит в буффере до момента первого доступа
            errorBodyString = ((HttpException) exception).response().errorBody().string();
        } catch (IOException e) {
            Timber.e(e, "Unable to resolve http exception response");
            return createBcEmpty();
        }

        return createBcErrorResult(errorBodyString);
    }

    public static <T> BCResult<T> createBcEmpty() {
        return new BCResult<>();
    }


    // Explorer

    public static <T> Observable<T> rxCallExp(Call<T> call) {
        return Observable.create(emitter -> call.clone().enqueue(new Callback<T>() {
            @Override
            public void onResponse(@NonNull Call<T> call1, @NonNull Response<T> response) {
                if (response.body() == null) {
                    emitter.onNext((T) createExpErrorResult(response));
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

    public static <T> Function<? super Throwable, ? extends ObservableSource<? extends ExpResult<T>>> convertToExpErrorResult() {
        return (Function<Throwable, ObservableSource<? extends ExpResult<T>>>) throwable
                -> {
            if (!(throwable instanceof HttpException)) {
                return Observable.error(throwable);
            }

            return Observable.just(createExpErrorResult(((HttpException) throwable)));
        };
    }

    public static <T> ExpResult<T> createExpErrorResult(final String json) {
        Gson gson = MinterBlockChainApi.getInstance().getGsonBuilder().create();
        return gson.fromJson(json, new TypeToken<ExpResult<T>>() {
        }.getType());
    }

    public static <T> ExpResult<T> createExpErrorResult(final Response<T> response) {
        final String errorBodyString;
        try {
            // нельзя после этой строки пытаться вытащить body из ошибки,
            // потому что retrofit по какой-то причине не хранит у себя это значение
            // а держит в буффере до момента первого доступа
            errorBodyString = response.errorBody().string();
        } catch (IOException e) {
            Timber.e(e, "Unable to resolve http exception response");
            return createExpEmpty();
        }

        return createExpErrorResult(errorBodyString);
    }

    public static <T> ExpResult<T> createExpErrorResult(final HttpException exception) {
        final String errorBodyString;
        try {
            // нельзя после этой строки пытаться вытащить body из ошибки,
            // потому что retrofit по какой-то причине не хранит у себя это значение
            // а держит в буффере до момента первого доступа
            errorBodyString = ((HttpException) exception).response().errorBody().string();
        } catch (IOException e) {
            Timber.e(e, "Unable to resolve http exception response");
            return createExpEmpty();
        }

        return createExpErrorResult(errorBodyString);
    }

    public static <T> ExpResult<T> createExpEmpty() {
        return new ExpResult<>();
    }
}
