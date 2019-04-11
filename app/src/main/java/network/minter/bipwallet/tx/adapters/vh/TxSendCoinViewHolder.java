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

package network.minter.bipwallet.tx.adapters.vh;

import android.view.View;
import android.widget.TextView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import network.minter.bipwallet.R;
import network.minter.bipwallet.internal.Wallet;
import network.minter.bipwallet.tx.adapters.TxItem;
import network.minter.core.crypto.MinterAddress;
import network.minter.explorer.models.HistoryTransaction;
import network.minter.profile.MinterProfileApi;

import static network.minter.bipwallet.internal.helpers.MathHelper.bdHuman;
import static network.minter.bipwallet.internal.helpers.MathHelper.bdNull;

/**
 * minter-android-wallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public final class TxSendCoinViewHolder extends ExpandableTxViewHolder {
    public @BindView(R.id.detail_from_value)
    TextView fromValue;
    public @BindView(R.id.detail_to_value)
    TextView toValue;
    public @BindView(R.id.detail_coin_value)
    TextView coinValue;
    public @BindView(R.id.detail_amount_value)
    TextView amountValue;

    public TxSendCoinViewHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }

    public void bind(TxItem txItem, List<MinterAddress> myAddresses) {
        super.bind(txItem);
        final HistoryTransaction item = txItem.getTx();
        final HistoryTransaction.TxSendCoinResult data = item.getData();

        final boolean isIncoming = item.isIncoming(myAddresses);
        final boolean isSelfSending = item.from.equals(data.to);

        if (!isIncoming) txItem.setAvatar( MinterProfileApi.getUserAvatarUrlByAddress(data.to));
        setupAvatar(txItem);

        if (isSelfSending) {
            if (txItem.getUsername() != null) {
                title.setText(String.format("@%s", txItem.getUsername()));
            } else {
                title.setText(item.getFrom().toShortString());
            }

            amount.setText(bdHuman(data.amount));
            amount.setTextColor(Wallet.app().res().getColor(R.color.textColorPrimary));
        } else {
            if (isIncoming) {
                if (txItem.getUsername() != null) {
                    title.setText(String.format("@%s", txItem.getUsername()));
                } else {
                    title.setText(item.getFrom().toShortString());
                }

                amount.setText(String.format("+ %s", bdHuman(data.amount)));
                amount.setTextColor(Wallet.app().res().getColor(R.color.textColorGreen));
            } else {
                if (txItem.getUsername() != null) {
                    title.setText(String.format("@%s", txItem.getUsername()));
                } else {
                    title.setText(data.to.toShortString());
                }

                amount.setText(String.format("- %s", bdHuman(data.amount)));
                amount.setTextColor(Wallet.app().res().getColor(R.color.textColorPrimary));
            }
        }

        if (bdNull(data.amount)) {
            amount.setText(bdHuman(data.amount));
            amount.setTextColor(Wallet.app().res().getColor(R.color.textColorPrimary));
        }

        fromValue.setText(item.getFrom().toString());
        toValue.setText(data.to.toString());
        subamount.setText(data.getCoin());
        coinValue.setText(data.getCoin());
        amountValue.setText(bdHuman(data.amount));
    }
}
