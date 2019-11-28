package network.minter.bipwallet.advanced.contract;

import android.text.TextWatcher;
import android.view.View;

import moxy.MvpView;
import moxy.viewstate.strategy.OneExecutionStateStrategy;
import moxy.viewstate.strategy.StateStrategyType;
import network.minter.bipwallet.internal.dialogs.WalletInputDialog;
import network.minter.bipwallet.internal.mvp.ErrorView;
import network.minter.bipwallet.internal.mvp.ProgressTextView;

/**
 * minter-android-wallet. 2019
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
public interface MainView extends MvpView, ErrorView, ProgressTextView {
    void setOnGenerate(View.OnClickListener listener);
    void setOnLedger(View.OnClickListener listener);
    void setMnemonicTextChangedListener(TextWatcher textWatcher);
    void setOnActivateMnemonic(View.OnClickListener listener);
    @StateStrategyType(OneExecutionStateStrategy.class)
    void startGenerate();
    @StateStrategyType(OneExecutionStateStrategy.class)
    void startLedger();
    @StateStrategyType(OneExecutionStateStrategy.class)
    void startGenerate(int requestCode);
    void setError(CharSequence errorMessage);
    void setTitle(CharSequence title);
    void askPassword(WalletInputDialog.OnSubmitListener submitListener);
    void finishSuccess();
    @StateStrategyType(OneExecutionStateStrategy.class)
    void startHome();
}
