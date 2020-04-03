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
package network.minter.bipwallet.exchange.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputLayout;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import network.minter.bipwallet.R;
import network.minter.bipwallet.exchange.adapters.CoinsListAdapter;
import network.minter.bipwallet.exchange.contract.BaseCoinTabView;
import network.minter.bipwallet.internal.BaseInjectFragment;
import network.minter.bipwallet.internal.Wallet;
import network.minter.bipwallet.internal.dialogs.WalletDialog;
import network.minter.bipwallet.internal.helpers.forms.DecimalInputFilter;
import network.minter.bipwallet.internal.helpers.forms.InputGroup;
import network.minter.bipwallet.internal.helpers.forms.validators.DecimalValidator;
import network.minter.bipwallet.internal.helpers.forms.validators.RegexValidator;
import network.minter.bipwallet.sending.account.AccountSelectedAdapter;
import network.minter.bipwallet.sending.account.WalletAccountSelectorDialog;
import network.minter.explorer.models.CoinBalance;
import network.minter.explorer.models.CoinItem;
import timber.log.Timber;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public abstract class BaseCoinTabFragment extends BaseInjectFragment implements BaseCoinTabView {
    public static final String IDLE_WAIT_ESTIMATE = "IDLE_WAIT_ESTIMATE";
    public static final String IDLE_WAIT_GAS = "IDLE_WAIT_GAS";

    @BindView(R.id.input_incoming_coin) AutoCompleteTextView inputIncomingCoin;
    @BindView(R.id.input_amount) AppCompatEditText inputAmount;
    @BindView(R.id.layout_amount) TextInputLayout layoutAmount;
    @BindView(R.id.input_outgoing_coin) AppCompatEditText inputOutgoingCoin;
    @BindView(R.id.layout_outgoing_coin) TextInputLayout layoutOutgoingCoin;
    @BindView(R.id.calculation) TextView calculationView;
    @BindView(R.id.layout_calculation) View calculationLayout;
    @BindView(R.id.action) Button action;
    @BindView(R.id.action_maximum) View actionMaximum;
    @BindView(R.id.fee_value) TextView feeValue;
    private Unbinder mUnbinder;
    private InputGroup mInputGroup;
    private WalletDialog mCurrentDialog;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(getLayout(), container, false);
        mUnbinder = ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void setFee(CharSequence commission) {
        feeValue.setText(commission);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mInputGroup = new InputGroup();
        mInputGroup.addInput(inputIncomingCoin, inputAmount);

        mInputGroup.addValidator(inputAmount, new DecimalValidator("Invalid number"));
        mInputGroup.addFilter(inputAmount, new DecimalInputFilter(() -> inputAmount));
        mInputGroup.addValidator(inputIncomingCoin, new RegexValidator("^[a-zA-Z0-9]+$", "Invalid coin name"));

        mInputGroup.addFilter(inputIncomingCoin, (source, start, end, dest, dstart, dend) -> {
            Timber.d("Filter: source=%s, start=%d, end=%d, dest=%s, destStart=%d, destEnd=%d", source, start, end, dest, dstart, dend);
            return source.toString().toUpperCase().replaceAll("[^A-Z0-9]", "");
        });

        mInputGroup.addFilter(inputAmount, new DecimalInputFilter(() -> inputAmount));

        calculationView.setInputType(InputType.TYPE_NULL);
    }

    @Override
    public void setOnClickSelectAccount(View.OnClickListener listener) {
        inputOutgoingCoin.setOnClickListener(listener);
    }

    @Override
    public void setMaximumEnabled(boolean enabled) {
        actionMaximum.setEnabled(enabled);
    }

    @Override
    public void setAmount(CharSequence amount) {
        inputAmount.setText(amount);
    }

    @Override
    public void setCoinsAutocomplete(List<CoinItem> items, CoinsListAdapter.OnItemClickListener listener) {
        if (items.size() > 0) {
            final CoinsListAdapter.OnItemClickListener cl = (item, position) -> {
                listener.onClick(item, position);
                inputIncomingCoin.dismissDropDown();
            };

            final CoinsListAdapter adapter = new CoinsListAdapter(getActivity(), items);
            adapter.setOnItemClickListener(cl);
            inputIncomingCoin.setAdapter(adapter);
        }
    }

    @Override
    public void setIncomingCoin(String symbol) {
        inputIncomingCoin.setText(symbol);
    }

    @Override
    public void onDestroyView() {
        WalletDialog.releaseDialog(mCurrentDialog);
        super.onDestroyView();
        mUnbinder.unbind();
    }

    @Override
    public void startDialog(WalletDialog.DialogExecutor executor) {
        mCurrentDialog = WalletDialog.switchDialogWithExecutor(this, mCurrentDialog, executor);
    }

    @Override
    public void setOnClickMaximum(View.OnClickListener listener) {
        actionMaximum.setOnClickListener(listener);
    }

    @Override
    public void setOnClickSubmit(View.OnClickListener listener) {
        action.setOnClickListener(listener);
    }

    @Override
    public void setTextChangedListener(InputGroup.OnTextChangedListener listener) {
        mInputGroup.addTextChangedListener(listener);
    }

    @Override
    public void startAccountSelector(List<CoinBalance> accounts, AccountSelectedAdapter.OnClickListener clickListener) {
        new WalletAccountSelectorDialog.Builder(getActivity(), R.string.title_select_account)
                .setItems(accounts)
                .setOnClickListener(clickListener)
                .create().show();
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
        action.setEnabled(enabled);
    }

    @Override
    public void setFormValidationListener(InputGroup.OnFormValidateListener listener) {
        mInputGroup.addFormValidateListener(listener);
    }

    @Override
    public void startExplorer(String txHash) {
        //TODO
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Wallet.urlExplorerFront() + "/transactions/" + txHash)));
    }

    @Override
    public void finish() {
        getActivity().finish();
    }

    @Override
    public void setCalculation(String calculation) {
        if (calculationLayout.getVisibility() == View.GONE) {
            calculationLayout.setVisibility(View.VISIBLE);
        }
        calculationView.setMaxLines(100);
        calculationView.setSingleLine(false);

        if (calculation.length() > 64) {
            final Matcher m = Pattern.compile("\\s+").matcher(calculation);
            int cnt = 0;
            while (m.find()) {
                cnt++;
            }

            int idx = (cnt * 3) - 1;
            if (cnt >= 64) {
                String toConcat;
                do {
                    toConcat = calculation.substring(0, ++idx);
                } while (toConcat.charAt(toConcat.length() - 1) != ' ');
                toConcat += "THE MATRIX HAS YOU ";
                toConcat += calculation.substring(idx);
                calculationView.setText(toConcat);
                return;
            }
        }


        calculationView.setText(calculation);
    }

    @Override
    public void setOutAccountName(CharSequence accountName) {
        inputOutgoingCoin.setText(accountName);
    }

    @LayoutRes
    abstract protected int getLayout();


}
