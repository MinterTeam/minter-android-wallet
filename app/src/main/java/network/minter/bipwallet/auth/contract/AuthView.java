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
    void setOnAdvancedMode(View.OnClickListener listener);
    void setOnHelp(View.OnClickListener listener);
    @StateStrategyType(OneExecutionStateStrategy.class)
    void startAdvancedMode();
    @StateStrategyType(OneExecutionStateStrategy.class)
    void startHelp();
}
