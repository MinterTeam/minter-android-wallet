package network.minter.bipwallet.internal.mvp;

import android.support.v4.widget.SwipeRefreshLayout;

/**
 * Stars. 2017
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public interface RefreshView {
    void setOnRefreshListener(SwipeRefreshLayout.OnRefreshListener listener);
    void showRefreshProgress();
    void hideRefreshProgress();
}
