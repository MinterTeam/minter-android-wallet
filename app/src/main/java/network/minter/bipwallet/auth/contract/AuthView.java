package network.minter.bipwallet.auth.contract;

import android.view.View;

import moxy.MvpView;
import moxy.viewstate.strategy.OneExecutionStateStrategy;
import moxy.viewstate.strategy.StateStrategyType;

/**
 * minter-android-wallet. 2019
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */

public interface AuthView extends MvpView {
    void setOnClickSignIn(View.OnClickListener listener);
    void setOnClickCreateWallet(View.OnClickListener listener);
    void setOnHelp(View.OnClickListener listener);

    @StateStrategyType(OneExecutionStateStrategy.class)
    void startSignIn();
    @StateStrategyType(OneExecutionStateStrategy.class)
    void startCreateWallet();
    @StateStrategyType(OneExecutionStateStrategy.class)
    void startHelp();
}
