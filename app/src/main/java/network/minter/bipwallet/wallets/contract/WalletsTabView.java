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

package network.minter.bipwallet.wallets.contract;

import android.view.View;

import java.util.List;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import moxy.MvpView;
import moxy.viewstate.strategy.AddToEndSingleStrategy;
import moxy.viewstate.strategy.OneExecutionStateStrategy;
import moxy.viewstate.strategy.StateStrategyType;
import network.minter.bipwallet.internal.dialogs.BaseBottomSheetDialog;
import network.minter.bipwallet.internal.dialogs.WalletDialog;
import network.minter.bipwallet.wallets.selector.WalletItem;
import network.minter.bipwallet.wallets.selector.WalletListAdapter;

/**
 * minter-android-wallet. 2019
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
@StateStrategyType(AddToEndSingleStrategy.class)
public interface WalletsTabView extends MvpView {
    void showSendAndSetAddress(String address);
    void showRefreshProgress();
    void hideRefreshProgress();

    void setBalance(String intPart, String fractionalPart, CharSequence coinName);
    void setOnRefreshListener(SwipeRefreshLayout.OnRefreshListener listener);
    void setDelegationAmount(String amount);
    void setBalanceClickListener(View.OnClickListener listener);
    void setBalanceTitle(int title);
    void setBalanceRewards(String rewards);
    void setOnClickScanQR(View.OnClickListener listener);
    void setMainWallet(WalletItem mainWallet);
    void setWallets(List<WalletItem> addresses);
    void setOnClickWalletListener(WalletListAdapter.OnClickWalletListener listener);
    void setOnClickAddWalletListener(WalletListAdapter.OnClickAddWalletListener listener);
    void setOnClickEditWalletListener(WalletListAdapter.OnClickEditWalletListener listener);
    void setOnClickDelegated(View.OnClickListener listener);

    @StateStrategyType(OneExecutionStateStrategy.class)
    void startExplorer(String hash);
    @StateStrategyType(OneExecutionStateStrategy.class)
    void startDialog(WalletDialog.DialogExecutor executor);
    @StateStrategyType(OneExecutionStateStrategy.class)
    void startExternalTransaction(String rawData);
    @StateStrategyType(OneExecutionStateStrategy.class)
    void startScanQRWithPermissions(int requestCode);
    @StateStrategyType(OneExecutionStateStrategy.class)
    void startTransactionList();
    @StateStrategyType(OneExecutionStateStrategy.class)
    void startDelegationList();
    @StateStrategyType(OneExecutionStateStrategy.class)
    void startConvertCoins();
    void startTab(int tab);
    @StateStrategyType(OneExecutionStateStrategy.class)
    void startScanQR(int requestCode);
    @StateStrategyType(OneExecutionStateStrategy.class)
    void startWalletEdit(WalletItem walletItem, BaseBottomSheetDialog.OnSubmitListener onSubmitListener);
    @StateStrategyType(OneExecutionStateStrategy.class)
    void startWalletAdd(BaseBottomSheetDialog.OnSubmitListener onSubmit, BaseBottomSheetDialog.OnDismissListener onDismiss);

}
