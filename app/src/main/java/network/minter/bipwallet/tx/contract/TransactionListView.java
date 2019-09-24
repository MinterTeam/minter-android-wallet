package network.minter.bipwallet.tx.contract;

import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import moxy.MvpView;
import moxy.viewstate.strategy.OneExecutionStateStrategy;
import moxy.viewstate.strategy.StateStrategyType;
import network.minter.bipwallet.internal.adapter.LoadState;
import network.minter.bipwallet.internal.mvp.ProgressView;

/**
 * minter-android-wallet. 2019
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
public
interface TransactionListView extends MvpView, ProgressView {
    void setAdapter(RecyclerView.Adapter<?> adapter);
    void setOnRefreshListener(SwipeRefreshLayout.OnRefreshListener listener);
    void showRefreshProgress();
    void hideRefreshProgress();
    void scrollTo(int pos);
    @StateStrategyType(OneExecutionStateStrategy.class)
    void startExplorer(String hash);
    void syncProgress(MutableLiveData<LoadState> loadState);
}
