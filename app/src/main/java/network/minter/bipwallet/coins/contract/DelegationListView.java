package network.minter.bipwallet.coins.contract;

import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import moxy.MvpView;
import network.minter.bipwallet.internal.adapter.LoadState;
import network.minter.bipwallet.internal.mvp.ProgressView;

/**
 * minter-android-wallet. 2019
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
public
interface DelegationListView extends MvpView, ProgressView {
    void setAdapter(RecyclerView.Adapter<?> adapter);
    void setOnRefreshListener(SwipeRefreshLayout.OnRefreshListener listener);
    void showRefreshProgress();
    void hideRefreshProgress();
    void scrollTo(int pos);
    void syncProgress(MutableLiveData<LoadState> loadState);
}
