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

package network.minter.bipwallet.sending.dialogs;

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
import network.minter.mintercore.MinterSDK;

import static network.minter.bipwallet.internal.common.Preconditions.checkNotNull;

/**
 * MinterWallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public final class WalletTxStartDialog extends WalletDialog {

    @BindView(R.id.dialog_amount) TextView amount;
    @BindView(R.id.recipient_avatar) BipCircleImageView avatar;
    @BindView(R.id.tx_recipient_name) TextView recipientName;
    @BindView(R.id.action_confirm) Button actionConfirm;
    @BindView(R.id.action_decline) Button actionDecline;

    private Builder mBuilder;

    public WalletTxStartDialog(@NonNull Context context, Builder builder) {
        super(context);
        mBuilder = builder;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wallet_tx_send_start_dialog);
        ButterKnife.bind(this);
        title.setText(mBuilder.getTitle());
        amount.setText(String.format("%s %s", mBuilder.mAmount.toString(), mBuilder.mCoin));
        if (mBuilder.mAvatarUrl != null) {
            avatar.setImageUrl(mBuilder.mAvatarUrl);
        }

        recipientName.setText(mBuilder.mRecipientName);
        actionConfirm.setText(mBuilder.getPositiveTitle());
        actionConfirm.setOnClickListener(v -> {
            if (mBuilder.hasPositiveListener()) {
                mBuilder.getPositiveListener().onClick(WalletTxStartDialog.this, BUTTON_POSITIVE);
            }
        });

        actionDecline.setText(mBuilder.getNegativeTitle());
        actionDecline.setOnClickListener(v -> {
            if (mBuilder.hasNegativeListener()) {
                mBuilder.getNegativeListener().onClick(WalletTxStartDialog.this, BUTTON_NEGATIVE);
            } else {
                dismiss();
            }
        });
    }

    public static final class Builder extends WalletDialogBuilder<WalletTxStartDialog, Builder> {
        private BigDecimal mAmount;
        private String mAvatarUrl;
        private CharSequence mRecipientName;
        private String mCoin = MinterSDK.DEFAULT_COIN;


        public Builder(Context context, CharSequence title) {
            super(context, title);
        }

        public Builder setAmount(String decimalString) {
            return setAmount(new BigDecimal(decimalString).setScale(18, BigDecimal.ROUND_UNNECESSARY));
        }

        public Builder setAmount(BigDecimal amount) {
            mAmount = amount;
            return this;
        }

        public Builder setAvatarUrl(String avatarUrl) {
            mAvatarUrl = avatarUrl;
            return this;
        }

        public Builder setRecipientName(CharSequence recipientName) {
            mRecipientName = recipientName;
            return this;
        }

        public WalletTxStartDialog.Builder setNegativeAction(CharSequence title, Dialog.OnClickListener listener) {
            return super.setAction(BUTTON_NEGATIVE, title, listener);
        }

        public WalletTxStartDialog.Builder setPositiveAction(CharSequence title, OnClickListener listener) {
            return super.setAction(BUTTON_POSITIVE, title, listener);
        }

        public WalletTxStartDialog create() {
            checkNotNull(mRecipientName, "Recipient name required");
            checkNotNull(mAmount, "Amount required");
            return new WalletTxStartDialog(mContext, this);
        }

        public Builder setCoin(String coin) {
            if (coin == null) {
                return this;
            }

            mCoin = coin.toUpperCase();
            return this;
        }
    }


}
