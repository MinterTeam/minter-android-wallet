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
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public final class TxDelegateUnboundViewHolder extends ExpandableTxViewHolder {
    public @BindView(R.id.detail_pub_value) TextView pubKey;
    public @BindView(R.id.detail_coin_value) TextView coin;
    public @BindView(R.id.detail_stake_value) TextView stake;
    public @BindView(R.id.item_title_type) TextView itemTitleType;

    public TxDelegateUnboundViewHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }

    @Override
    public void bind(TxItem item) {
        super.bind(item);

        final HistoryTransaction.TxDelegateUnbondResult data = item.getTx().getData();

        if (item.getTx().getType() == HistoryTransaction.Type.Delegate) {
            avatar.setImageResource(R.drawable.img_avatar_delegate);
            amount.setTextColor(Wallet.app().res().getColor(R.color.textColorPrimary));
            if (bdNull(data.getValue())) {
                amount.setText(String.format("%s", bdHuman(data.getValue())));
            } else {
                amount.setText(String.format("- %s", bdHuman(data.getValue())));
            }
        } else {
            avatar.setImageResource(R.drawable.img_avatar_unbond);
            if (bdNull(data.getValue())) {
                amount.setText(String.format("%s", bdHuman(data.getValue())));
            } else {
                amount.setTextColor(Wallet.app().res().getColor(R.color.textColorGreen));
                amount.setText(String.format("+ %s", bdHuman(data.getValue())));
            }
        }

        subamount.setText(data.coin);
        coin.setText(data.getCoin());
        stake.setText(bdHuman(firstNonNull(data.getValue(), new BigDecimal(0))));

        itemTitleType.setText(item.getTx().getType().name());
        if (data.getPublicKey() != null) {
            pubKey.setText(data.getPublicKey().toString());
            title.setText(data.getPublicKey().toShortString());
        } else {
            pubKey.setText("<unknown>");
            title.setText(item.getTx().hash.toShortString());
        }
        setupCopyListeners(pubKey);
    }

    @Override
    protected boolean autoSetAvatar() {
        return false;
    }
}
