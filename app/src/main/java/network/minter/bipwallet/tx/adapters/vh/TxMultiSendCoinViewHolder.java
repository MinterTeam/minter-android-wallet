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

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import butterknife.BindView;
import butterknife.ButterKnife;
import network.minter.bipwallet.R;
import network.minter.bipwallet.internal.Wallet;
import network.minter.bipwallet.tx.adapters.TxItem;
import network.minter.core.crypto.MinterAddress;
import network.minter.explorer.models.HistoryTransaction;

import static network.minter.bipwallet.internal.helpers.MathHelper.bdHuman;

/**
 * minter-android-wallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public final class TxMultiSendCoinViewHolder extends ExpandableTxViewHolder {
    public @BindView(R.id.detail_from_value)
    TextView fromValue;
    public @BindView(R.id.detail_amount_value)
    TextView amountValue;

    public TxMultiSendCoinViewHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }

    public void bind(TxItem txItem, List<MinterAddress> myAddresses) {
        super.bind(txItem);

        final HistoryTransaction item = txItem.getTx();
        final HistoryTransaction.TxMultisendResult data = item.getData();

        final boolean isIncoming = !myAddresses.contains(item.from);

        if (txItem.getUsername() != null) {
            title.setText(String.format("@%s", txItem.getUsername()));
        } else {
            title.setText(item.getFrom().toShortString());
        }

        HashMap<String, BigDecimal> coinsAmount = new HashMap<>();

        if (isIncoming) {
            for (HistoryTransaction.TxSendCoinResult i : data.items) {
                if (myAddresses.contains(i.to)) {
                    if (coinsAmount.containsKey(i.coin)) {
                        BigDecimal amount = coinsAmount.get(i.coin).add(i.amount);
                        coinsAmount.put(i.coin, amount);
                    } else {
                        coinsAmount.put(i.coin, i.amount);
                    }
                }
            }

            amount.setTextColor(Wallet.app().res().getColor(R.color.textColorGreen));

            if (coinsAmount.size() > 1) {
                amount.setText(R.string.dots);
                subamount.setText(subamount.getContext().getString(R.string.multiple_coins));
            } else {
                try {
                    Map.Entry<String, BigDecimal> entry = coinsAmount.entrySet().iterator().next();
                    amount.setText(String.format("+ %s", bdHuman(entry.getValue())));
                    subamount.setText(entry.getKey());
                } catch (NoSuchElementException e) {
                    amount.setText(R.string.dots);
                    subamount.setText(null);
                }
            }
        } else {
            for (HistoryTransaction.TxSendCoinResult i : data.items) {
                if (coinsAmount.containsKey(i.coin)) {
                    BigDecimal amount = coinsAmount.get(i.coin).add(i.amount);
                    coinsAmount.put(i.coin, amount);
                } else {
                    coinsAmount.put(i.coin, i.amount);
                }
            }

            amount.setTextColor(Wallet.app().res().getColor(R.color.textColorPrimary));

            if (coinsAmount.size() > 1) {
                amount.setText(R.string.dots);
                subamount.setText(subamount.getContext().getString(R.string.multiple_coins));
            } else {
                Map.Entry<String, BigDecimal> entry = coinsAmount.entrySet().iterator().next();
                amount.setText(String.format("- %s", bdHuman(entry.getValue())));
                subamount.setText(entry.getKey());
            }
        }

        fromValue.setText(item.getFrom().toString());
        avatar.setImageResource(R.drawable.img_avatar_multisend);
        setupCopyListeners(fromValue);
    }
}
