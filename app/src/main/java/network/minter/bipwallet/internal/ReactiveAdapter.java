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

package network.minter.bipwallet.internal;

import android.support.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.EOFException;
import java.io.IOException;
import java.util.Collections;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Function;
import network.minter.blockchain.MinterBlockChainApi;
import network.minter.blockchain.models.BCResult;
import network.minter.explorer.MinterExplorerApi;
import network.minter.explorer.models.ExpResult;
import network.minter.profile.MinterProfileApi;
import network.minter.profile.models.ProfileResult;
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
    public static <T> Observable<T> rxCallProfile(Call<T> call) {
        return Observable.create(emitter -> call.clone().enqueue(new Callback<T>() {
            @SuppressWarnings("unchecked")
            @Override
            public void onResponse(@NonNull Call<T> call1, @NonNull Response<T> response) {
                if (response.body() == null) {
                    emitter.onNext((T) createProfileErrorResult(response));
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

    public static <T> Function<? super Throwable, ? extends ObservableSource<? extends ProfileResult<T>>> convertToProfileErrorResult() {
        return (Function<Throwable, ObservableSource<? extends ProfileResult<T>>>) throwable
                -> {
            if (!(throwable instanceof HttpException)) {
                return Observable.error(throwable);
            }

            return Observable.just(createProfileErrorResult(((HttpException) throwable)));
        };
    }

    public static <T> ProfileResult<T> createProfileErrorResult(final String json) {
        Gson gson = MinterProfileApi.getInstance().getGsonBuilder().create();

        ProfileResult<T> out;
        try {
            out = gson.fromJson(json, new TypeToken<ProfileResult<T>>() {
            }.getType());
        } catch (Exception e) {
            out = new ProfileResult<>();
            out.error = new ProfileResult.Error();
            out.error.message = "Invalid response";
            out.error.code = "500";
            out.error.data = Collections.emptyMap();
            out.data = null;
        }

        return out;
    }

    public static <T> ProfileResult<T> createProfileErrorResult(final Response<T> response) {
        final String errorBodyString;
        try {
            // нельзя после этой строки пытаться вытащить body из ошибки,
            // потому что retrofit по какой-то причине не хранит у себя это значение
            // а держит в буффере до момента первого доступа
            errorBodyString = response.errorBody().string();
        } catch (IOException e) {
            Timber.e(e, "Unable to resolve http exception response");
            return createProfileEmpty();
        }

        return createProfileErrorResult(errorBodyString);
    }

    public static <T> ProfileResult<T> createProfileErrorResult(final HttpException exception) {
        final String errorBodyString;
        try {
            // нельзя после этой строки пытаться вытащить body из ошибки,
            // потому что retrofit по какой-то причине не хранит у себя это значение
            // а держит в буффере до момента первого доступа
            errorBodyString = ((HttpException) exception).response().errorBody().string();
        } catch (IOException e) {
            Timber.e(e, "Unable to resolve http exception response");
            return createProfileEmpty();
        }

        return createProfileErrorResult(errorBodyString);
    }

    public static <T> ProfileResult<T> createProfileEmpty() {
        return new ProfileResult<>();
    }


    // Blockhain

    public static <T> Observable<T> rxCallBc(Call<T> call) {
        return Observable.create(emitter -> call.clone().enqueue(new Callback<T>() {
            @SuppressWarnings("unchecked")
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

    public static <T> BCResult<T> createBcErrorResultMessage(final String errorMessage, BCResult.ResultCode code, int statusCode) {
        BCResult<T> errorRes = new BCResult<>();
        errorRes.message = errorMessage;
        errorRes.statusCode = statusCode;
        errorRes.code = code;
        return errorRes;
    }

    public static <T> BCResult<T> createBcErrorResult(final String json) {
        Gson gson = MinterBlockChainApi.getInstance().getGsonBuilder().create();

        BCResult<T> out;
        try {
            out = gson.fromJson(json, new TypeToken<BCResult<T>>() {
            }.getType());
        } catch (Exception e) {
            out = new BCResult<>();
            out.code = BCResult.ResultCode.Unknown;
            out.message = "Invalid response";
            out.statusCode = 500;
        }

        return out;
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
            @SuppressWarnings("unchecked")
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
        Gson gson = MinterExplorerApi.getInstance().getGsonBuilder().create();
        ExpResult<T> out;
        try {
            out = gson.fromJson(json, new TypeToken<ExpResult<T>>() {
            }.getType());
        } catch (Exception e) {
            out = new ExpResult<>();
        }

        return out;
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
