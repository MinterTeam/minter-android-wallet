/*
 * Copyright (C) by MinterTeam. 2018
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

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.widget.Button;
import android.widget.TextView;

import java.math.BigDecimal;

import butterknife.BindView;
import butterknife.ButterKnife;
import network.minter.bipwallet.R;
import network.minter.bipwallet.internal.dialogs.WalletDialog;
import network.minter.bipwallet.internal.dialogs.WalletDialogBuilder;
import network.minter.bipwallet.internal.views.widgets.BipCircleImageView;
import network.minter.profile.MinterProfileApi;

import static network.minter.bipwallet.internal.common.Preconditions.checkNotNull;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class WalletTxConvertStartDialog extends WalletDialog {
    @BindView(R.id.dialog_amount) TextView amount;
    @BindView(R.id.tx_from_coin_avatar) BipCircleImageView fromCoinAvatar;
    @BindView(R.id.tx_to_coin_avatar) BipCircleImageView toCoinAvatar;
    @BindView(R.id.tx_from_coin_name) TextView fromCoinName;
    @BindView(R.id.tx_to_coin_name) TextView toCoinName;
    @BindView(R.id.action_confirm) Button actionConfirm;
    @BindView(R.id.action_decline) Button actionDecline;
    @BindView(R.id.dialog_amount_label) TextView amountLabel;

    private Builder mBuilder;

    protected WalletTxConvertStartDialog(@NonNull Context context, Builder builder) {
        super(context);
        mBuilder = builder;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wallet_tx_convert_start_dialog);
        ButterKnife.bind(this);
        title.setText(mBuilder.getTitle());

        if (mBuilder.mAmountPostfix != null && mBuilder.mAmountPostfix.length() > 0) {
            amount.setText(String.format("%s %s", mBuilder.mAmount, mBuilder.mAmountPostfix));
        } else {
            amount.setText(mBuilder.mAmount);
        }
        fromCoinAvatar.setImageUrlFallback(mBuilder.mFromCoinAvatar, R.drawable.img_avatar_default);
        toCoinAvatar.setImageUrlFallback(mBuilder.mToCoinAvatar, R.drawable.img_avatar_default);
        fromCoinName.setText(mBuilder.mFromCoin);
        toCoinName.setText(mBuilder.mToCoin);
        if (mBuilder.mLabel != null && mBuilder.mLabel.length() > 0) {
            amountLabel.setText(mBuilder.mLabel);
        }

        actionConfirm.setText(mBuilder.getPositiveTitle());
        actionConfirm.setOnClickListener(v -> {
            if (mBuilder.hasPositiveListener()) {
                mBuilder.getPositiveListener().onClick(this, BUTTON_POSITIVE);
            }
        });

        actionDecline.setText(mBuilder.getNegativeTitle());
        actionDecline.setOnClickListener(v -> {
            if (mBuilder.hasNegativeListener()) {
                mBuilder.getNegativeListener().onClick(this, BUTTON_NEGATIVE);
            } else {
                dismiss();
            }
        });
    }

    public static final class Builder extends WalletDialogBuilder<WalletTxConvertStartDialog, Builder> {
        private String mFromCoin, mToCoin;
        private String mFromCoinAvatar, mToCoinAvatar;
        private String mAmount;
        private CharSequence mLabel;
        private CharSequence mAmountPostfix;

        public Builder(Context context) {
            super(context);
        }

        public Builder(Context context, CharSequence title) {
            super(context, title);
        }

        public Builder setNegativeAction(CharSequence title, Dialog.OnClickListener listener) {
            return super.setAction(BUTTON_NEGATIVE, title, listener);
        }

        public Builder setNegativeAction(CharSequence title) {
            return super.setAction(BUTTON_NEGATIVE, title, null);
        }

        public Builder setPositiveAction(CharSequence title) {
            return super.setAction(BUTTON_POSITIVE, title, null);
        }

        public Builder setPositiveAction(CharSequence title, OnClickListener listener) {
            return super.setAction(BUTTON_POSITIVE, title, listener);
        }

        public Builder setAmountPostfix(CharSequence postfix) {
            mAmountPostfix = postfix;
            return this;
        }

        public Builder setFromCoin(String fromCoin) {
            mFromCoin = fromCoin;
            setFromCoinAvatar(MinterProfileApi.getCoinAvatarUrl(fromCoin));
            return this;
        }

        public Builder setFromCoin(String fromCoin, String fromCoinAvatar) {
            mFromCoin = fromCoin;
            mFromCoinAvatar = fromCoinAvatar;
            return this;
        }

        public Builder setToCoin(String toCoin) {
            mToCoin = toCoin;
            setToCoinAvatar(MinterProfileApi.getCoinAvatarUrl(toCoin));
            return this;
        }

        public Builder setToCoin(String toCoin, String toCoinAvatar) {
            mToCoin = toCoin;
            mToCoinAvatar = toCoinAvatar;
            return this;
        }

        public Builder setFromCoinAvatar(String fromCoinAvatar) {
            mFromCoinAvatar = fromCoinAvatar;
            return this;
        }

        public Builder setToCoinAvatar(String toCoinAvatar) {
            mToCoinAvatar = toCoinAvatar;
            return this;
        }

        public Builder setAmount(String amount) {
            mAmount = amount;
            return this;
        }

        public Builder setAmount(BigDecimal amount) {
            return setAmount(amount.toPlainString());
        }

        @Override
        public WalletTxConvertStartDialog create() {
            checkNotNull(mFromCoin, "Source coin required");
            checkNotNull(mToCoin, "Target coin required");
            checkNotNull(mAmount, "Amount required");

            mToCoin = mToCoin.toUpperCase();
            mFromCoin = mFromCoin.toUpperCase();
            return new WalletTxConvertStartDialog(getContext(), this);
        }

        public Builder setLabel(CharSequence label) {
            mLabel = label;
            return this;
        }
    }
}
