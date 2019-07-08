package network.minter.bipwallet.apis.reactive;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;

import androidx.annotation.NonNull;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Function;
import network.minter.bipwallet.apis.dummies.ExpErrorMapped;
import network.minter.core.internal.exceptions.NetworkException;
import network.minter.explorer.MinterExplorerApi;
import network.minter.explorer.models.BCExplorerResult;
import network.minter.explorer.models.ExpResult;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.HttpException;
import retrofit2.Response;
import timber.log.Timber;

/**
 * minter-android-wallet. 2019
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
public final class ReactiveExplorer {

    public static <T> Observable<T> rxExp(Call<T> call) {
        return Observable.create(emitter -> call.clone().enqueue(new Callback<T>() {
            @SuppressWarnings("unchecked")
            @Override
            public void onResponse(@NonNull Call<T> call1, @NonNull Response<T> response) {
                if (response.body() == null) {
                    emitter.onNext((T) createExpError(response));
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
        errorRes.error = new BCExplorerResult.ErrorResult();
        errorRes.error.message = errorMessage;
        errorRes.code = statusCode;
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
        out.code = code;
        out.error = new BCExplorerResult.ErrorResult();
        out.error.message = message;
        out.error.code = -1;
        return out;
    }
}
