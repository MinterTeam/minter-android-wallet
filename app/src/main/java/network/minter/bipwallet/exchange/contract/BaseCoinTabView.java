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

package network.minter.bipwallet.exchange.contract;

import android.view.View;

import java.util.List;

import moxy.MvpView;
import moxy.viewstate.strategy.AddToEndSingleStrategy;
import moxy.viewstate.strategy.OneExecutionStateStrategy;
import moxy.viewstate.strategy.StateStrategyType;
import network.minter.bipwallet.exchange.adapters.CoinsListAdapter;
import network.minter.bipwallet.internal.dialogs.WalletDialog;
import network.minter.bipwallet.internal.helpers.forms.InputGroup;
import network.minter.bipwallet.sending.account.AccountSelectedAdapter;
import network.minter.explorer.models.CoinBalance;
import network.minter.explorer.models.CoinItem;

/**
 * minter-android-wallet. 2019
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
@StateStrategyType(AddToEndSingleStrategy.class)
public interface BaseCoinTabView extends MvpView {
    @StateStrategyType(OneExecutionStateStrategy.class)
    void startDialog(WalletDialog.DialogExecutor executor);
    void setOnClickMaximum(View.OnClickListener listener);
    void setOnClickSubmit(View.OnClickListener listener);
    void setTextChangedListener(InputGroup.OnTextChangedListener listener);
    @StateStrategyType(OneExecutionStateStrategy.class)
    void startAccountSelector(List<CoinBalance> accounts, AccountSelectedAdapter.OnClickListener clickListener);
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
