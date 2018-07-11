/*******************************************************************************
 * Copyright (C) by MinterTeam. 2018
 * @link https://github.com/MinterTeam
 * @link https://github.com/edwardstock
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
 ******************************************************************************/

package network.minter.bipwallet.tx.adapters.vh;

import android.view.View;
import android.widget.TextView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import network.minter.bipwallet.R;
import network.minter.bipwallet.internal.Wallet;
import network.minter.bipwallet.tx.adapters.TxItem;
import network.minter.explorerapi.models.HistoryTransaction;
import network.minter.mintercore.crypto.MinterAddress;

/**
 * minter-android-wallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public final class TxSendCoinViewHolder extends ExpandableTxViewHolder {
    public @BindView(R.id.detail_from_value) TextView fromValue;
    public @BindView(R.id.detail_to_value) TextView toValue;
    public @BindView(R.id.detail_coin_value) TextView coinValue;
    public @BindView(R.id.detail_amount_value) TextView amountValue;

    public TxSendCoinViewHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }

    public void bind(TxItem txItem, List<MinterAddress> myAddresses) {
        super.bind(txItem);

        final HistoryTransaction item = txItem.getTx();
        final HistoryTransaction.TxSendCoinResult data = item.getData();

        final String am;

        final boolean isIncoming = item.isIncoming(myAddresses);

        if (isIncoming) {
            if (txItem.getUsername() != null) {
                title.setText(String.format("@%s", txItem.getUsername()));
            } else {
                title.setText(data.from.toShortString());
            }
            am = String.format("+ %s", data.amount.toPlainString());
            amount.setText(am);
            amount.setTextColor(Wallet.app().res().getColor(R.color.textColorGreen));
        } else {
            if (txItem.getUsername() != null) {
                title.setText(String.format("@%s", txItem.getUsername()));
            } else {
                title.setText(data.to.toShortString());
            }

            am = String.format("- %s", data.amount.toPlainString());
            amount.setText(am);
            amount.setTextColor(Wallet.app().res().getColor(R.color.textColorPrimary));
        }

        fromValue.setText(data.from.toString());
        toValue.setText(data.to.toString());
        subamount.setText(data.getCoin());
        coinValue.setText(data.getCoin());
        amountValue.setText(am);
    }
}
