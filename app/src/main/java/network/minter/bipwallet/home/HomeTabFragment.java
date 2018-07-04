package network.minter.bipwallet.home;

import android.support.annotation.NonNull;
import android.view.Menu;
import android.view.View;


import network.minter.bipwallet.R;
import network.minter.bipwallet.internal.BaseFragment;
import network.minter.bipwallet.internal.mvp.ErrorView;
import network.minter.bipwallet.internal.mvp.ErrorViewWithRetry;
import network.minter.bipwallet.internal.views.SnackbarBuilder;

/**
 * Dogsy. 2017
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */

public abstract class HomeTabFragment extends BaseFragment implements ErrorView, ErrorViewWithRetry {


    public void createToolbarMenuOptions(Menu menu) {

    }

    public void onTabSelected() {

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
        new SnackbarBuilder(this)
                .setMessage(errorMessage)
                .setAction(actionName, errorResolver)
                .setDurationIndefinite()
                .show();
    }

    public CharSequence getTitle() {
        return null;
    }
}
