package network.minter.bipwallet.tx.contract;

import android.text.TextWatcher;
import android.view.View;

import moxy.MvpView;
import moxy.viewstate.strategy.OneExecutionStateStrategy;
import moxy.viewstate.strategy.StateStrategyType;
import network.minter.bipwallet.internal.dialogs.WalletDialog;
import network.minter.bipwallet.internal.mvp.ProgressView;

/**
 * minter-android-wallet. 2019
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
public interface ExternalTransactionView extends MvpView, ProgressView {
    void setFirstLabel(CharSequence label);
    void setFirstValue(CharSequence value);

    void setSecondLabel(CharSequence label);
    void setSecondValue(CharSequence value);
    void setPayload(CharSequence payloadString);
    void setCommission(CharSequence fee);
    void setFirstVisible(int visibility);
    void setSecondVisible(int visibility);
    @StateStrategyType(OneExecutionStateStrategy.class)
    void startDialog(WalletDialog.DialogExecutor executor);
    void setPayloadTextChangedListener(TextWatcher textWatcher);
    void setOnConfirmListener(View.OnClickListener listener);
    void setOnCancelListener(View.OnClickListener listener);

    void finishSuccess();
    void finishCancel();

    @StateStrategyType(OneExecutionStateStrategy.class)
    void startExplorer(String hash);

}
