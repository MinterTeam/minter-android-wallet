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

package network.minter.bipwallet.sending.account;

import android.content.Context;
import android.os.Bundle;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import network.minter.bipwallet.R;
import network.minter.bipwallet.internal.dialogs.WalletDialog;
import network.minter.bipwallet.internal.dialogs.WalletDialogBuilder;
import network.minter.core.crypto.MinterAddress;
import network.minter.explorer.models.CoinBalance;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class WalletAccountSelectorDialog extends WalletDialog {
    @BindView(R.id.list) RecyclerView list;
    private Builder mBuilder;

    protected WalletAccountSelectorDialog(@NonNull Context context, Builder builder) {
        super(context);
        mBuilder = builder;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wallet_account_selector_dialog);
        ButterKnife.bind(this);
        title.setText(mBuilder.getTitle());

        AccountSelectedAdapter adapter = new AccountSelectedAdapter(mBuilder.mItems);
        adapter.setOnClickListener(item -> {
            if (mBuilder.mOnClickListener != null) {
                mBuilder.mOnClickListener.onClick(item);
            }
            dismiss();
        });

        list.setLayoutManager(new LinearLayoutManager(getContext()));
        list.setAdapter(adapter);
    }

    public static final class Builder extends WalletDialogBuilder<WalletAccountSelectorDialog, WalletAccountSelectorDialog.Builder> {
        private List<CoinBalance> mItems = new ArrayList<>();
        private AccountSelectedAdapter.OnClickListener mOnClickListener;

        public Builder(Context context) {
            super(context);
        }

        public Builder(Context context, CharSequence title) {
            super(context, title);
        }

        public Builder(Context context, @StringRes int titleRes) {
            super(context, titleRes);
        }

        @Override
        public WalletAccountSelectorDialog create() {
            return new WalletAccountSelectorDialog(getContext(), this);
        }

        public Builder setOnClickListener(AccountSelectedAdapter.OnClickListener listener) {
            mOnClickListener = listener;
            return this;
        }

        public Builder addItem(String coin, MinterAddress address, BigDecimal balance) {
            mItems.add(new CoinBalance(coin, balance, address));
            return this;
        }

        public Builder addItem(CoinBalance item) {
            mItems.add(item);
            return this;
        }

        public Builder addItems(List<CoinBalance> items) {
            mItems.addAll(items);
            return this;
        }

        public Builder setItems(List<CoinBalance> items) {
            mItems = new ArrayList<>(items);
            return this;
        }


    }
}
