package network.minter.bipwallet.internal;

import android.content.Context;
import android.os.Build;

import com.arellomobile.mvp.MvpAppCompatFragment;

import network.minter.bipwallet.internal.helpers.KeyboardHelper;


public abstract class BaseFragment extends MvpAppCompatFragment {

    @Override
    public Context getContext() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            return super.getContext();
        }

        return getActivity();
    }

    @Override
    public void onPause() {
        super.onPause();
        KeyboardHelper.hideKeyboard(getActivity());
    }

    public void runOnUiThread(Runnable r) {
        if (getActivity() == null) {
            return;
        }

        getActivity().runOnUiThread(r);
    }
}
