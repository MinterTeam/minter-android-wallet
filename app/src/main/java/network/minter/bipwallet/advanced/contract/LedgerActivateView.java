package network.minter.bipwallet.advanced.contract;

import android.view.View;
import android.widget.Switch;

import moxy.MvpView;
import moxy.viewstate.strategy.OneExecutionStateStrategy;
import moxy.viewstate.strategy.StateStrategyType;
import network.minter.bipwallet.internal.mvp.ErrorView;

/**
 * minter-android-wallet. 2019
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
public interface LedgerActivateView extends MvpView, ErrorView {
    void setOnSwitchedConfirm(Switch.OnCheckedChangeListener listener);
    void setOnActionClick(View.OnClickListener listener);
    void setAddress(CharSequence phrase);
    void setEnableLaunch(boolean enable);
    void setSecuredByValue(CharSequence name);
    @StateStrategyType(OneExecutionStateStrategy.class)
    void startHome();
    void finishSuccess();
    void showAddress(boolean show);
    void showProgress(boolean show);
    void setProgressText(CharSequence text);
    void setEnableSwitch(boolean enable);
}
