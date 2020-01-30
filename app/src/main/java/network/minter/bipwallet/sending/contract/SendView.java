package network.minter.bipwallet.sending.contract;

import android.text.TextWatcher;
import android.view.View;

import java.util.List;

import androidx.annotation.StringRes;
import moxy.MvpView;
import moxy.viewstate.strategy.OneExecutionStateStrategy;
import moxy.viewstate.strategy.StateStrategyType;
import network.minter.bipwallet.advanced.models.CoinAccount;
import network.minter.bipwallet.internal.dialogs.WalletDialog;
import network.minter.bipwallet.internal.helpers.forms.InputGroup;
import network.minter.bipwallet.internal.mvp.ErrorViewWithRetry;
import network.minter.bipwallet.sending.account.AccountSelectedAdapter;
import network.minter.bipwallet.sending.adapters.RecipientListAdapter;
import network.minter.bipwallet.sending.models.RecipientItem;

/**
 * minter-android-wallet. 2019
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
public interface SendView extends MvpView, ErrorViewWithRetry {
    void setOnClickAccountSelectedListener(View.OnClickListener listener);
    void setOnClickMaximum(View.OnClickListener listener);
    void setOnClickAddPayload(View.OnClickListener listener);
    void setOnTextChangedListener(InputGroup.OnTextChangedListener listener);
    void setFormValidationListener(InputGroup.OnFormValidateListener listener);
    @StateStrategyType(OneExecutionStateStrategy.class)
    void startAccountSelector(List<CoinAccount> accounts, AccountSelectedAdapter.OnClickListener clickListener);
    void setAccountName(CharSequence accountName);
    void setOnSubmit(View.OnClickListener listener);
    void setSubmitEnabled(boolean enabled);
    void clearInputs();
    @StateStrategyType(OneExecutionStateStrategy.class)
    void startDialog(WalletDialog.DialogExecutor executor);
    @StateStrategyType(OneExecutionStateStrategy.class)
    void startExplorer(String txHash);
    @StateStrategyType(OneExecutionStateStrategy.class)
    void startScanQR(int requestCode);
    @StateStrategyType(OneExecutionStateStrategy.class)
    void startScanQRWithPermissions(int requestCode);
    void setRecipient(CharSequence to);
    void setRecipientError(CharSequence error);
    void setAmountError(CharSequence error);
    void setError(CharSequence error);
    void setError(@StringRes int error);
    void setAmount(CharSequence amount);
    void setFee(CharSequence fee);
    void setRecipientsAutocomplete(List<RecipientItem> items, RecipientListAdapter.OnItemClickListener listener);
    void setPayloadChangeListener(TextWatcher listener);
    void setPayload(String payload);
    void setActionTitle(int buttonTitle);
    @StateStrategyType(OneExecutionStateStrategy.class)
    void startExternalTransaction(String rawData);
    void showPayload();
    void hidePayload();
}
