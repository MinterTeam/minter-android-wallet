package network.minter.bipwallet.internal;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import dagger.android.support.AndroidSupportInjection;
import network.minter.bipwallet.R;
import network.minter.bipwallet.internal.mvp.ErrorView;
import network.minter.bipwallet.internal.mvp.ErrorViewWithRetry;

/**
 * Dogsy. 2017
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */

public abstract class BaseInjectFragment extends BaseFragment implements ErrorView, ErrorViewWithRetry {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        if (enableAutoInjection()) {
            AndroidSupportInjection.inject(this);
        }

        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(Context context) {
        if (enableAutoInjection()) {
            AndroidSupportInjection.inject(this);
        }
        super.onAttach(context);
    }

    @Override
    public void onError(Throwable t) {
        if (getActivity() instanceof ErrorView) {
            ((ErrorView) getActivity()).onError(t);
        }
    }

    @Override
    public void onError(String err) {
        if (getActivity() instanceof ErrorView) {
            ((ErrorView) getActivity()).onError(err);
        }
    }

    @Override
    public void onErrorWithRetry(String errorMessage, View.OnClickListener errorResolver) {
        onErrorWithRetry(errorMessage, getResources().getString(R.string.btn_retry), errorResolver);
    }

    @Override
    public void onErrorWithRetry(String errorMessage, String actionName,
                                 View.OnClickListener errorResolver) {
//        new SnackbarBuilder(this)
//                .setMessage(errorMessage)
//                .setAction(actionName, errorResolver)
//                .setDurationIndefinite()
//                .show();
    }

    protected boolean enableAutoInjection() {
        return true;
    }
}
