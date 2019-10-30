package network.minter.bipwallet.internal.views.utils;

import android.view.View;

public abstract class OnSingleClickListener implements View.OnClickListener {
    private static final long MIN_CLICK_INTERVAL = 600;

    /**
     * click button
     * @param v The view that was clicked.
     */
    public abstract void onSingleClick(View v);

    @Override
    public final void onClick(View v) {
        SingleCallHandler.call(v, MIN_CLICK_INTERVAL, () -> onSingleClick(v));
    }

}
