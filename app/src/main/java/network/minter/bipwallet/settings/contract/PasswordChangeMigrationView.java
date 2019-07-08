package network.minter.bipwallet.settings.contract;

import android.view.View;

import moxy.MvpView;
import moxy.viewstate.strategy.OneExecutionStateStrategy;
import moxy.viewstate.strategy.StateStrategyType;
import network.minter.bipwallet.internal.dialogs.WalletDialog;
import network.minter.bipwallet.internal.helpers.forms.InputGroup;

/**
 * minter-android-wallet. 2019
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
public
interface PasswordChangeMigrationView extends MvpView {
    void setTextChangedListener(InputGroup.OnTextChangedListener listener);
    void setFormValidateListener(InputGroup.OnFormValidateListener listener);
    void setOnClickSubmit(View.OnClickListener listener);
    @StateStrategyType(OneExecutionStateStrategy.class)
    void startDialog(WalletDialog.DialogExecutor executor);
    void setEnableSubmit(boolean enable);
    void finish();
}
