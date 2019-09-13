package network.minter.bipwallet.exchange.contract;

import android.view.View;

import java.util.List;

import moxy.MvpView;
import moxy.viewstate.strategy.OneExecutionStateStrategy;
import moxy.viewstate.strategy.StateStrategyType;
import network.minter.bipwallet.advanced.models.CoinAccount;
import network.minter.bipwallet.exchange.adapters.CoinsListAdapter;
import network.minter.bipwallet.internal.dialogs.WalletDialog;
import network.minter.bipwallet.internal.helpers.forms.InputGroup;
import network.minter.bipwallet.sending.account.AccountSelectedAdapter;
import network.minter.explorer.models.CoinItem;

/**
 * minter-android-wallet. 2019
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
public interface BaseCoinTabView extends MvpView {
    @StateStrategyType(OneExecutionStateStrategy.class)
    void startDialog(WalletDialog.DialogExecutor executor);
    void setOnClickMaximum(View.OnClickListener listener);
    void setOnClickSubmit(View.OnClickListener listener);
    void setTextChangedListener(InputGroup.OnTextChangedListener listener);
    @StateStrategyType(OneExecutionStateStrategy.class)
    void startAccountSelector(List<CoinAccount> accounts, AccountSelectedAdapter.OnClickListener clickListener);
    void setOnClickSelectAccount(View.OnClickListener listener);
    void setError(String field, CharSequence message);
    void clearErrors();
    void setSubmitEnabled(boolean enabled);
    void setFormValidationListener(InputGroup.OnFormValidateListener listener);
    @StateStrategyType(OneExecutionStateStrategy.class)
    void startExplorer(String s);
    void finish();
    void setCalculation(String calculation);
    void setOutAccountName(CharSequence accountName);
    void setMaximumEnabled(boolean enabled);
    void setAmount(CharSequence amount);
    void setCoinsAutocomplete(List<CoinItem> items, CoinsListAdapter.OnItemClickListener listener);
    void setIncomingCoin(String symbol);
    void setFee(CharSequence commission);
}
