package network.minter.bipwallet.apis.reactive;

import android.support.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Function;
import network.minter.bipwallet.apis.dummies.ExpGateErrorMapped;
import network.minter.blockchain.MinterBlockChainApi;
import network.minter.blockchain.models.BCResult;
import network.minter.core.internal.exceptions.NetworkException;
import network.minter.explorer.models.BCExplorerResult;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.HttpException;
import retrofit2.Response;
import timber.log.Timber;

/**
 * minter-android-wallet. 2019
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
public final class ReactiveExplorerGate {

    public static <T> Observable<T> rxExpGate(Call<T> call) {
        return Observable.create(emitter -> call.clone().enqueue(new Callback<T>() {
            @SuppressWarnings("unchecked")
            @Override
            public void onResponse(@NonNull Call<T> call1, @NonNull Response<T> response) {
                if (response.body() == null) {
                    emitter.onNext((T) createExpGateError(response));
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

    public static <T> Function<? super Throwable, ? extends ObservableSource<? extends BCExplorerResult<T>>> toExpGateError() {
        return (Function<Throwable, ObservableSource<? extends BCExplorerResult<T>>>) throwable
                -> {
            final ExpGateErrorMapped<T> errResult = new ExpGateErrorMapped<>();
            if (errResult.mapError(throwable)) {
                return Observable.just(errResult);
            }

            if (throwable instanceof HttpException) {
                return Observable.just(createExpGateError(((HttpException) throwable)));
            }

            return Observable.just(createExpGateErrorPlain(NetworkException.convertIfNetworking(throwable)));
        };
    }

    public static <T> BCExplorerResult<T> createExpGateErrorPlain(Throwable t) {
        final Throwable e = NetworkException.convertIfNetworking(t);
        if (e instanceof NetworkException) {
            return createExpGateErrorPlain(((NetworkException) e).getUserMessage(), -1, ((NetworkException) e).getStatusCode());
        }

        return createExpGateErrorPlain(t.getMessage(), -1, -1);

    }

    public static <T> BCExplorerResult<T> createExpGateErrorPlain(final String errorMessage, int code, int statusCode) {
        BCExplorerResult<T> errorRes = new BCExplorerResult<>();
        errorRes.error = new BCExplorerResult.ErrorResult();
        errorRes.error.message = errorMessage;
        errorRes.statusCode = statusCode;
        errorRes.error.code = code;
        return errorRes;
    }

    public static <T> BCExplorerResult<T> createExpGateError(final String json, int code, String message) {
        Gson gson = MinterBlockChainApi.getInstance().getGsonBuilder().create();

        BCExplorerResult<T> out;
        try {
            if (json == null || json.isEmpty()) {
                out = createExpGateEmpty(code, message);
            } else {
                out = gson.fromJson(json, new TypeToken<BCExplorerResult<T>>() {
                }.getType());
            }
        } catch (Exception e) {
            Timber.w(e, "Unable to parse explorer (blockchain) error: %s", json);
            out = createExpGateEmpty(code, message);
        }

        return out;
    }

    public static <T> BCExplorerResult<T> createExpGateError(final Response<T> response) {
        final String errorBodyString;
        try {
            // нельзя после этой строки пытаться вытащить body из ошибки,
            // потому что retrofit по какой-то причине не хранит у себя это значение
            // а держит в буффере до момента первого доступа
            errorBodyString = response.errorBody().string();
        } catch (IOException e) {
            Timber.e(e, "Unable to resolve http exception response");
            return createExpGateEmpty(response.code(), response.message());
        }

        final BCExplorerResult<T> out = createExpGateError(errorBodyString, response.code(), response.message());
        out.statusCode = response.code();
        return out;
    }

    public static <T> BCExplorerResult<T> createExpGateError(final HttpException exception) {
        final String errorBodyString;
        try {
            // нельзя после этой строки пытаться вытащить body из ошибки,
            // потому что retrofit по какой-то причине не хранит у себя это значение
            // а держит в буффере до момента первого доступа
            errorBodyString = ((HttpException) exception).response().errorBody().string();
        } catch (IOException e) {
            Timber.e(e, "Unable to resolve http exception response");
            return createExpGateEmpty(exception.code(), exception.message());
        }

        return createExpGateError(errorBodyString, exception.code(), exception.message());
    }

    public static <T> BCExplorerResult<T> createExpGateEmpty(int code, String message) {
        BCExplorerResult<T> out = new BCExplorerResult<>();
        out.error = new BCExplorerResult.ErrorResult();
        out.error.code = BCResult.ResultCode.UnknownError.getValue();
        out.error.message = String.format("%d (%s) %s", code, out.getErrorCode().name(), message);
        return out;
    }

    public static <T> BCExplorerResult<T> createExpGateEmpty() {
        BCExplorerResult<T> out = new BCExplorerResult<>();
        out.error = new BCExplorerResult.ErrorResult();
        return out;
    }
}
