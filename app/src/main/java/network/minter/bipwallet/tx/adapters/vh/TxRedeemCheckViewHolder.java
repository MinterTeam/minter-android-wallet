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

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import network.minter.bipwallet.R;
import network.minter.bipwallet.internal.Wallet;
import network.minter.bipwallet.tx.adapters.TxItem;
import network.minter.core.crypto.MinterAddress;
import network.minter.explorer.models.HistoryTransaction;

import static network.minter.bipwallet.internal.helpers.MathHelper.bdHuman;

/**
 * Created by Alexander Kolpakov (jquickapp@gmail.com) on 21-May-19
 */
public class TxRedeemCheckViewHolder extends ExpandableTxViewHolder {
    public @BindView(R.id.detail_from_value) TextView fromValue;
    public @BindView(R.id.detail_coin_value) TextView coinValue;
    public @BindView(R.id.detail_amount_value) TextView amountValue;

    public TxRedeemCheckViewHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }


    public void bind(TxItem item, List<MinterAddress> myAddresses) {
        super.bind(item);
        avatar.setImageResource(R.drawable.img_avatar_redeem);
        HistoryTransaction tx = item.getTx().get();
        final HistoryTransaction.TxRedeemCheckResult data = tx.getData();
        fromValue.setText(data.getCheck().getSender().toString());

        title.setText(tx.hash.toShortString());

        if (myAddresses.contains(tx.from)) {
            amount.setTextColor(Wallet.app().res().getColor(R.color.textColorGreen));
            amount.setText(String.format("+ %s", bdHuman(data.getCheck().getValue())));
        } else {
            amount.setTextColor(Wallet.app().res().getColor(R.color.textColorPrimary));
            amount.setText(String.format("- %s", bdHuman(data.getCheck().getValue())));
        }

        amountValue.setText(bdHuman(data.getCheck().getValue()));
        subamount.setText(data.getCheck().getCoin());
        coinValue.setText(data.getCheck().getCoin());
        setupCopyListeners(fromValue);
    }
}
