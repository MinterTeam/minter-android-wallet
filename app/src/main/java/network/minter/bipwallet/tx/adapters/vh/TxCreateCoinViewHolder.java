/*
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
 */

package network.minter.bipwallet.tx.adapters.vh;

import android.view.View;
import android.widget.TextView;

import java.math.BigDecimal;

import butterknife.BindView;
import butterknife.ButterKnife;
import network.minter.bipwallet.R;
import network.minter.bipwallet.tx.adapters.TxItem;
import network.minter.blockchain.models.operational.Transaction;
import network.minter.core.MinterSDK;
import network.minter.explorer.models.HistoryTransaction;
import network.minter.profile.MinterProfileApi;

import static network.minter.bipwallet.internal.common.Preconditions.firstNonNull;

/**
 * minter-android-wallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public final class TxCreateCoinViewHolder extends ExpandableTxViewHolder {
    public @BindView(R.id.detail_coin_name_value) TextView coinName;
    public @BindView(R.id.detail_coin_symbol_value) TextView coinSymbol;
    public @BindView(R.id.detail_initial_amount_value) TextView initialAmount;
    public @BindView(R.id.detail_initial_reserve_value) TextView initialReserve;
    public @BindView(R.id.detail_constant_reserve_ratio_value) TextView crr;

    public TxCreateCoinViewHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }

    @Override
    public void bind(TxItem item) {
        super.bind(item);
        final HistoryTransaction.TxCreateResult data = item.getTx().getData();
        final String coinSymbol = firstNonNull(data.getSymbol(), "<unknown>");
        avatar.setImageUrlFallback(MinterProfileApi.getCoinAvatarUrl(coinSymbol), MinterProfileApi.getCoinAvatarUrl("MNT"));
        title.setText(coinSymbol);
        amount.setText(String.format("- %s", firstNonNull(item.getTx().fee, new BigDecimal(0)).toPlainString()));
        subamount.setText(MinterSDK.DEFAULT_COIN);
        coinName.setText(firstNonNull(data.name, "<unknown>"));
        this.coinSymbol.setText(firstNonNull(data.getSymbol(), "<unknown>"));
        initialAmount.setText(firstNonNull(data.initialAmount, new BigDecimal(0)).divide(Transaction.VALUE_MUL_DEC).toPlainString());
        initialReserve.setText(firstNonNull(data.initialReserve, new BigDecimal(0)).divide(Transaction.VALUE_MUL_DEC).toPlainString());
        crr.setText(firstNonNull(data.constantReserveRatio, new BigDecimal(0)).toPlainString());
    }

    @Override
    protected boolean autoSetAvatar() {
        return false;
    }
}
