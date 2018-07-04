package network.minter.bipwallet.internal;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Dimension;
import android.support.annotation.StyleRes;
import android.support.v4.app.FragmentManager;
import android.view.WindowManager;

import com.arellomobile.mvp.MvpAppCompatDialogFragment;

import dagger.android.support.AndroidSupportInjection;
import fr.tvbarthel.lib.blurdialogfragment.BlurDialogEngine;
import network.minter.bipwallet.R;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

/**
 * Stars. 2017
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */

public abstract class MvpBlurCompatDialogFragment extends MvpAppCompatDialogFragment {

    private BlurDialogEngine mBlur;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (autoInjection()) {
            AndroidSupportInjection.inject(this);
        }

        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_FRAME, getDialogStyle());

        mBlur = new BlurDialogEngine(getActivity());
        mBlur.debug(false);
        mBlur.setBlurRadius(8);
        mBlur.setDownScaleFactor(4F);
        mBlur.setBlurActionBar(true);
        mBlur.setUseRenderScript(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getDialog() != null && getDialog().getWindow() != null) {
            WindowManager.LayoutParams params = getDialog().getWindow().getAttributes();
            params.width = getDialogWidth();
            params.height = getDialogHeight();
            getDialog().getWindow().setAttributes(params);
        }
        mBlur.onResume(getRetainInstance());
    }

    @Override
    public void onAttach(Context context) {
        if (autoInjection()) {
            AndroidSupportInjection.inject(this);
        }

        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mBlur.onDetach();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        mBlur.onDismiss();
    }

    public void show(FragmentManager fragmentManager) {
        show(fragmentManager, getClass().getName());
    }

    protected boolean autoInjection() {
        return true;
    }

    protected BlurDialogEngine getBlur() {
        return mBlur;
    }

    @StyleRes
    protected int getDialogStyle() {
        return R.style.Wallet_DialogFragment;
    }

    @Dimension
    protected int getDialogWidth() {
        return MATCH_PARENT;
    }

    @Dimension
    protected int getDialogHeight() {
        return MATCH_PARENT;
    }
}
