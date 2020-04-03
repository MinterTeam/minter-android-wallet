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

package network.minter.bipwallet.tx.contract;

import android.text.TextWatcher;
import android.view.View;

import moxy.MvpView;
import moxy.viewstate.strategy.AddToEndSingleStrategy;
import moxy.viewstate.strategy.OneExecutionStateStrategy;
import moxy.viewstate.strategy.StateStrategyType;
import network.minter.bipwallet.internal.dialogs.WalletDialog;
import network.minter.bipwallet.internal.mvp.ProgressView;

/**
 * minter-android-wallet. 2019
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
@StateStrategyType(AddToEndSingleStrategy.class)
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
    @StateStrategyType(OneExecutionStateStrategy.class)
    void startDialog(boolean cancelable, WalletDialog.DialogExecutor executor);
    void setPayloadTextChangedListener(TextWatcher textWatcher);
    void setOnConfirmListener(View.OnClickListener listener);
    void setOnCancelListener(View.OnClickListener listener);

    void finishSuccess();
    void finishCancel();

    @StateStrategyType(OneExecutionStateStrategy.class)
    void startExplorer(String hash);

    void disableAll();
}
