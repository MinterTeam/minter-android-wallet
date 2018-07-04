/*
 * Copyright (C) 2018 by MinterTeam
 * @link https://github.com/MinterTeam
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

package network.minter.bipwallet.sending;

import android.view.View;

import com.arellomobile.mvp.MvpView;

import java.util.List;

import dagger.Module;
import network.minter.bipwallet.advanced.models.AccountItem;
import network.minter.bipwallet.auth.ui.InputGroup;
import network.minter.bipwallet.internal.dialogs.WalletDialog;
import network.minter.bipwallet.internal.mvp.ErrorViewWithRetry;
import network.minter.bipwallet.sending.account.AccountSelectedAdapter;

/**
 * MinterWallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@Module
public class SendTabModule {

    public interface SendView extends MvpView, ErrorViewWithRetry {
        void setOnClickAccountSelectedListener(View.OnClickListener listener);
        void setOnTextChangedListener(InputGroup.OnTextChangedListener listener);
        void setFormValidationListener(InputGroup.OnFormValidateListener listener);
        void startAccountSelector(List<AccountItem> accounts, AccountSelectedAdapter.OnClickListener clickListener);
        void setAccountName(CharSequence accountName);
        void setOnSubmit(View.OnClickListener listener);
        void setSubmitEnabled(boolean enabled);
        void clearInputs();
        void startDialog(WalletDialog.DialogExecutor executor);
        void startExplorer(String txHash);
        void setOnClickScanQR(View.OnClickListener listener);
        void startScanQR(int requestCode);
        void startScanQRWithPermissions(int requestCode);
        void setRecipient(CharSequence to);
        void setRecipientError(CharSequence error);
    }

    public static class TxData {

    }
}
