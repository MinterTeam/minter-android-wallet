package network.minter.bipwallet.coins.contract;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import moxy.MvpView;
import moxy.viewstate.strategy.OneExecutionStateStrategy;
import moxy.viewstate.strategy.StateStrategyType;

/**
 * minter-android-wallet. 2019
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
public
interface CoinsTabView extends MvpView {
    void setAvatar(String url);
    void setUsername(CharSequence name);
    void setBalance(String intPart, String fractionalPart, CharSequence coinName);
    void setAdapter(RecyclerView.Adapter<?> adapter);
    void setOnAvatarClick(View.OnClickListener listener);
    @StateStrategyType(OneExecutionStateStrategy.class)
    void startTransactionList();
    @StateStrategyType(OneExecutionStateStrategy.class)
    void startDelegationList();
    void hideAvatar();
    @StateStrategyType(OneExecutionStateStrategy.class)
    void startConvertCoins();
    void startTab(int tab);

    void setOnRefreshListener(SwipeRefreshLayout.OnRefreshListener listener);
    void showRefreshProgress();
    void hideRefreshProgress();
    @StateStrategyType(OneExecutionStateStrategy.class)
    void startExplorer(String hash);
    void scrollTop();

    void setDelegationAmount(String amount);
    void setBalanceClickListener(View.OnClickListener listener);
    void setBalanceTitle(int title);
}
