/*
 * Copyright (C) by MinterTeam. 2020
 * @link <a href="https://github.com/MinterTeam">Org Github</a>
 * @link <a href="https://github.com/edwardstock">Maintainer Github</a>
 *
 * The MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package network.minter.bipwallet.sending.contract;

import android.text.TextWatcher;
import android.view.View;

import java.util.List;

import androidx.annotation.StringRes;
import moxy.MvpView;
import moxy.viewstate.strategy.AddToEndSingleStrategy;
import moxy.viewstate.strategy.OneExecutionStateStrategy;
import moxy.viewstate.strategy.StateStrategyType;
import network.minter.bipwallet.addressbook.models.AddressContact;
import network.minter.bipwallet.internal.dialogs.WalletDialog;
import network.minter.bipwallet.internal.helpers.forms.InputGroup;
import network.minter.bipwallet.internal.mvp.ErrorViewWithRetry;
import network.minter.bipwallet.sending.account.AccountSelectedAdapter;
import network.minter.bipwallet.sending.adapters.RecipientListAdapter;
import network.minter.bipwallet.wallets.selector.WalletItem;
import network.minter.explorer.models.CoinBalance;

/**
 * minter-android-wallet. 2019
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
@StateStrategyType(AddToEndSingleStrategy.class)
public interface SendView extends MvpView, ErrorViewWithRetry {
    void setOnClickAccountSelectedListener(View.OnClickListener listener);
    void setOnClickMaximum(View.OnClickListener listener);
    void setOnClickAddPayload(View.OnClickListener listener);
    void setOnClickClearPayload(View.OnClickListener listener);
    void setOnTextChangedListener(InputGroup.OnTextChangedListener listener);
    void setOnContactsClickListener(View.OnClickListener listener);
    void setFormValidationListener(InputGroup.OnFormValidateListener listener);
    @StateStrategyType(OneExecutionStateStrategy.class)
    void startAccountSelector(List<CoinBalance> accounts, AccountSelectedAdapter.OnClickListener clickListener);
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
    @StateStrategyType(OneExecutionStateStrategy.class)
    void startAddressBook(int requestCode);
    void setRecipient(AddressContact to);
    void setRecipientError(CharSequence error);
    void setAmountError(CharSequence error);
    void setPayloadError(CharSequence error);
    void setError(CharSequence error);
    void setError(@StringRes int error);
    void setAmount(CharSequence amount);
    void setFee(CharSequence fee);

    void setRecipientAutocompleteItemClickListener(RecipientListAdapter.OnItemClickListener listener);
    void setRecipientAutocompleteItems(List<AddressContact> items);
    void hideAutocomplete();
    void setPayloadChangeListener(TextWatcher listener);
    void setPayload(String payload);
    void setActionTitle(int buttonTitle);
    @StateStrategyType(OneExecutionStateStrategy.class)
    void startExternalTransaction(String rawData);
    void showPayload();
    void hidePayload();
    void setWallets(List<WalletItem> walletItems);
    void setMainWallet(WalletItem walletItem);

}
