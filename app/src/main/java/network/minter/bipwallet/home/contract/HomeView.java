package network.minter.bipwallet.home.contract;

import moxy.MvpView;
import moxy.viewstate.strategy.OneExecutionStateStrategy;
import moxy.viewstate.strategy.StateStrategyType;
import network.minter.bipwallet.internal.mvp.ErrorView;
import network.minter.bipwallet.internal.mvp.ErrorViewWithRetry;
import network.minter.bipwallet.internal.mvp.ProgressView;

/**
 * minter-android-wallet. 2019
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
public interface HomeView extends MvpView, ErrorView, ErrorViewWithRetry, ProgressView {
    void setCurrentPage(int position);
    @StateStrategyType(OneExecutionStateStrategy.class)
    void startUrl(String url);
    @StateStrategyType(OneExecutionStateStrategy.class)
    void startRemoteTransaction(String txHash);
}
