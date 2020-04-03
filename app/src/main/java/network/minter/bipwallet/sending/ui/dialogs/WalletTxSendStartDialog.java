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

package network.minter.bipwallet.sending.ui.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import java.math.BigDecimal;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import butterknife.BindView;
import butterknife.ButterKnife;
import network.minter.bipwallet.R;
import network.minter.bipwallet.internal.dialogs.WalletDialog;
import network.minter.bipwallet.internal.dialogs.WalletDialogBuilder;
import network.minter.bipwallet.internal.views.widgets.BipCircleImageView;
import network.minter.core.MinterSDK;

import static network.minter.bipwallet.internal.common.Preconditions.checkNotNull;
import static network.minter.bipwallet.internal.helpers.MathHelper.bdHuman;

/**
 * minter-android-wallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public final class WalletTxSendStartDialog extends WalletDialog {

    @BindView(R.id.dialog_amount) TextView amount;
    @BindView(R.id.tx_recipient_avatar) BipCircleImageView avatar;
    @BindView(R.id.tx_recipient_name) TextView recipientName;
    @BindView(R.id.action_confirm) Button actionConfirm;
    @BindView(R.id.action_decline) Button actionDecline;

    private Builder mBuilder;

    public WalletTxSendStartDialog(@NonNull Context context, Builder builder) {
        super(context);
        mBuilder = builder;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wallet_tx_send_start_dialog);
        ButterKnife.bind(this);
        title.setText(mBuilder.getTitle());
        amount.setText(String.format("%s %s", bdHuman(mBuilder.mAmount), mBuilder.mCoin));
        if (mBuilder.mAvatarUrl != null) {
            avatar.setImageUrl(mBuilder.mAvatarUrl);
        } else if(mBuilder.mAvatarRes != -1){
            avatar.setImageResource(mBuilder.mAvatarRes);
        }

        recipientName.setText(mBuilder.mRecipientName);
        actionConfirm.setText(mBuilder.getPositiveTitle());
        actionConfirm.setOnClickListener(v -> {
            if (mBuilder.hasPositiveListener()) {
                mBuilder.getPositiveListener().onClick(WalletTxSendStartDialog.this, BUTTON_POSITIVE);
            }
        });

        actionDecline.setText(mBuilder.getNegativeTitle());
        actionDecline.setOnClickListener(v -> {
            if (mBuilder.hasNegativeListener()) {
                mBuilder.getNegativeListener().onClick(WalletTxSendStartDialog.this, BUTTON_NEGATIVE);
            } else {
                dismiss();
            }
        });
    }

    public static final class Builder extends WalletDialogBuilder<WalletTxSendStartDialog, Builder> {
        private BigDecimal mAmount;
        private String mAvatarUrl;
        @DrawableRes private int mAvatarRes = -1;
        private CharSequence mRecipientName;
        private String mCoin = MinterSDK.DEFAULT_COIN;

        public Builder(Context context, @StringRes int title) {
            super(context, title);
        }

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

        public Builder setAvatarUrlFallback(String avatarUrl, @DrawableRes int avatarRes) {
            if (avatarUrl == null) {
                mAvatarRes = avatarRes;
            } else {
                mAvatarUrl = avatarUrl;
            }
            return this;
        }

        public Builder setAvatarResource(@DrawableRes int avatarResId) {
            mAvatarRes = avatarResId;
            return this;
        }

        public Builder setRecipientName(CharSequence recipientName) {
            mRecipientName = recipientName;
            return this;
        }

        public WalletTxSendStartDialog.Builder setNegativeAction(CharSequence title, Dialog.OnClickListener listener) {
            return super.setAction(BUTTON_NEGATIVE, title, listener);
        }

        public WalletTxSendStartDialog.Builder setNegativeAction(@StringRes int titleRes, Dialog.OnClickListener listener) {
            return super.setAction(BUTTON_NEGATIVE, titleRes, listener);
        }

        public WalletTxSendStartDialog.Builder setPositiveAction(@StringRes int titleRes, OnClickListener listener) {
            return super.setAction(BUTTON_POSITIVE, titleRes, listener);
        }

        public WalletTxSendStartDialog.Builder setPositiveAction(CharSequence title, OnClickListener listener) {
            return super.setAction(BUTTON_POSITIVE, title, listener);
        }

        public WalletTxSendStartDialog create() {
            checkNotNull(mRecipientName, "Recipient name required");
            checkNotNull(mAmount, "Amount can't be empty");
            return new WalletTxSendStartDialog(getContext(), this);
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
