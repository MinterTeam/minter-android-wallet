package network.minter.bipwallet.internal.mvp;

import android.view.View;

/**
 * Stars. 2017
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public interface ErrorViewWithRetry extends ErrorView {
    void onErrorWithRetry(final String errorMessage, final View.OnClickListener errorResolver);
    void onErrorWithRetry(final String errorMessage, final String actionName,
                          final View.OnClickListener errorResolver);
}
