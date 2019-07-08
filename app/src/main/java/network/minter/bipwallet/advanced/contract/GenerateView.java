package network.minter.bipwallet.advanced.contract;

import android.view.View;
import android.widget.Switch;

import moxy.MvpView;
import moxy.viewstate.strategy.OneExecutionStateStrategy;
import moxy.viewstate.strategy.StateStrategyType;
import network.minter.bipwallet.advanced.ui.AdvancedMainActivity;
import network.minter.bipwallet.internal.dialogs.WalletInputDialog;
import network.minter.bipwallet.internal.mvp.ErrorView;
import network.minter.bipwallet.internal.mvp.ProgressTextView;

/**
 * minter-android-wallet. 2019
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
public interface GenerateView extends MvpView, ProgressTextView, ErrorView {
    void setOnCopy(View.OnClickListener listener);
    void setOnSwitchedConfirm(Switch.OnCheckedChangeListener listener);
    void setOnActionClick(View.OnClickListener listener);
    void setOnSecuredByClickListener(View.OnClickListener listener);
    void setMnemonic(CharSequence phrase);
    void setEnableLaunch(boolean enable);
    void setEnableCopy(boolean enable);
    @StateStrategyType(OneExecutionStateStrategy.class)
    void startHome();
    void setEnableSecureVariants(boolean enable, AdvancedMainActivity.OnSelectSecureVariant onSelect);
    void askPassword(WalletInputDialog.OnSubmitListener submitListener);
    void finishSuccess();
    void setActionTitle(CharSequence title);
}
