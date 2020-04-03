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

package network.minter.bipwallet.tx.adapters.vh;

import android.view.View;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import network.minter.bipwallet.R;
import network.minter.bipwallet.internal.Wallet;
import network.minter.bipwallet.tx.adapters.TxItem;
import network.minter.explorer.models.HistoryTransaction;

import static network.minter.bipwallet.internal.common.Preconditions.firstNonNull;
import static network.minter.bipwallet.internal.helpers.MathHelper.bdHuman;
import static network.minter.bipwallet.internal.helpers.MathHelper.bdNull;

/**
 * minter-android-wallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public final class TxConvertCoinViewHolder extends ExpandableTxViewHolder {
    public @BindView(R.id.detail_coin_from_value) TextView coinFrom;
    public @BindView(R.id.detail_coin_to_value) TextView coinTo;
    public @BindView(R.id.detail_received_amount_value) TextView receivedAmount;
    public @BindView(R.id.detail_spent_amount_value) TextView spentAmount;

    public TxConvertCoinViewHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }

    @Override
    public void bind(TxItem item) {
        super.bind(item);
        final HistoryTransaction.TxConvertCoinResult data = item.getTx().getData();
        title.setText(String.format("%s –> %s", data.getCoinToSell(), data.getCoinToBuy()));
        avatar.setImageResource(R.drawable.img_avatar_exchange);
        if (bdNull(data.valueToBuy)) {
            amount.setText(bdHuman(data.valueToBuy));
            amount.setTextColor(Wallet.app().res().getColor(R.color.textColorPrimary));
        } else {
            amount.setText(String.format("+ %s", bdHuman(data.valueToBuy)));
            amount.setTextColor(Wallet.app().res().getColor(R.color.textColorGreen));
        }

        subamount.setText(data.getCoinToBuy());
        coinFrom.setText(firstNonNull(data.getCoinToSell(), "<unknown>"));
        coinTo.setText(firstNonNull(data.getCoinToBuy(), "<unknown>"));
        receivedAmount.setText(bdHuman(data.valueToBuy));
        spentAmount.setText(bdHuman(data.valueToSell));
    }

    @Override
    protected boolean autoSetAvatar() {
        return false;
    }
}
