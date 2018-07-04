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

package network.minter.bipwallet.exchange.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;

import com.arellomobile.mvp.presenter.InjectPresenter;
import com.arellomobile.mvp.presenter.ProvidePresenter;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;

import butterknife.BindView;
import butterknife.ButterKnife;
import network.minter.bipwallet.R;
import network.minter.bipwallet.advanced.models.AccountItem;
import network.minter.bipwallet.auth.ui.InputGroup;
import network.minter.bipwallet.exchange.ExchangeModule;
import network.minter.bipwallet.exchange.views.ConvertCoinPresenter;
import network.minter.bipwallet.internal.BaseMvpInjectActivity;
import network.minter.bipwallet.internal.dialogs.WalletDialog;
import network.minter.bipwallet.internal.helpers.forms.validators.DecimalValidator;
import network.minter.bipwallet.internal.helpers.forms.validators.RegexValidator;
import network.minter.bipwallet.sending.account.AccountSelectedAdapter;
import network.minter.bipwallet.sending.account.WalletAccountSelectorDialog;
import network.minter.explorerapi.MinterExplorerApi;
import timber.log.Timber;

/**
 * MinterWallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class ConvertCoinActivity extends BaseMvpInjectActivity implements ExchangeModule.ConvertCoinView {

    @Inject Provider<ConvertCoinPresenter> presenterProvider;
    @InjectPresenter ConvertCoinPresenter presenter;
    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.input_incoming_coin) TextInputEditText inCoin;
    @BindView(R.id.input_incoming_amount) TextInputEditText inAmount;
    @BindView(R.id.input_outgoing_coin) TextInputEditText outCoin;
    @BindView(R.id.input_outgoing_amount) TextInputEditText outAmount;
    @BindView(R.id.action_maximum) Button actionMaximum;
    @BindView(R.id.action_exchange) Button actionExchange;

    private InputGroup mInputGroup;

    @Override
    public void setOnClickMaximum(View.OnClickListener listener) {
        actionMaximum.setOnClickListener(listener);
    }

    @Override
    public void setOnClickSubmit(View.OnClickListener listener) {
        actionExchange.setOnClickListener(listener);
    }

    @Override
    public void setOnClickSelectAccount(View.OnClickListener listener) {
        outCoin.setOnClickListener(listener);
    }

    @Override
    public void setMaximumTitle(CharSequence title) {
        actionMaximum.setText(title);
    }

    @Override
    public void setTextChangedListener(InputGroup.OnTextChangedListener listener) {
        mInputGroup.addTextChangedListener(listener);
    }

    @Override
    public void startAccountSelector(List<AccountItem> accounts, AccountSelectedAdapter.OnClickListener clickListener) {
        new WalletAccountSelectorDialog.Builder(this, "Select account")
                .setItems(accounts)
                .setOnClickListener(clickListener)
                .create().show();
    }

    @Override
    public void setOutAccountName(CharSequence accountName) {
        outCoin.setText(accountName);
    }

    @Override
    public void setError(String field, CharSequence message) {
        mInputGroup.setError(field, message);
    }

    @Override
    public void clearErrors() {
        mInputGroup.clearErrors();
    }

    @Override
    public void setSubmitEnabled(boolean enabled) {
        actionExchange.setEnabled(enabled);
    }

    @Override
    public void setMaximumEnabled(boolean enabled) {
        actionMaximum.setEnabled(enabled);
    }

    @Override
    public void setFormValidationListener(InputGroup.OnFormValidateListener listener) {
        mInputGroup.addFormValidateListener(listener);
    }

    private WalletDialog mCurrentDialog;

    @Override
    public void setAmountSpending(String amount) {
        outAmount.setText(amount);
    }

    @Override
    public void setAmountGetting(String amount) {
        inAmount.setText(amount);
    }

    @Override
    public void startDialog(WalletDialog.DialogExecutor executor) {
        mCurrentDialog = WalletDialog.switchDialogWithExecutor(this, mCurrentDialog, executor);
    }

    @Override
    public void startExplorer(String txHash) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(MinterExplorerApi.FRONT_URL + "/transactions/" + txHash)));
    }

    @ProvidePresenter
    ConvertCoinPresenter providePresenter() {
        return presenterProvider.get();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_convert_coin);
        ButterKnife.bind(this);
        setupToolbar(toolbar);

        mInputGroup = new InputGroup();
        mInputGroup.addInput(inCoin, inAmount);
        mInputGroup.addInput(outAmount);

        mInputGroup.addValidator(inAmount, new DecimalValidator("Invalid number"));
        mInputGroup.addValidator(outAmount, new DecimalValidator("Invalid number"));
        mInputGroup.addValidator(inCoin, new RegexValidator("^[a-zA-Z]+$", "Invalid coin name"));

        mInputGroup.addFilter(inCoin, (source, start, end, dest, dstart, dend) -> {
            Timber.d("Filter: source=%s, start=%d, end=%d, dest=%s, destStart=%d, destEnd=%d", source, start, end, dest, dstart, dend);
            return source.toString().toUpperCase().replaceAll("[^A-Z]", "");
        });


    }
}
