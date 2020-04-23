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

package network.minter.bipwallet.tx.adapters;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import network.minter.bipwallet.R;
import network.minter.bipwallet.databinding.ItemListTxBinding;
import network.minter.bipwallet.tx.adapters.vh.TxAllViewHolder;
import network.minter.bipwallet.tx.adapters.vh.TxHeaderViewHolder;
import network.minter.bipwallet.tx.adapters.vh.TxProgressViewHolder;
import network.minter.core.crypto.MinterAddress;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class TxItem implements TransactionItem {
    private final TransactionFacade mTx;

    public TxItem(TransactionFacade tx) {
        mTx = tx;
    }

    public static RecyclerView.ViewHolder createViewHolder(final LayoutInflater inflater, final ViewGroup parent, @ListType int viewType) {
        View view;
        RecyclerView.ViewHolder out;
        switch (viewType) {
            case ITEM_HEADER:
                view = inflater.inflate(R.layout.item_list_transaction_header, parent, false);
                out = new TxHeaderViewHolder(view);
                break;
            case ITEM_PROGRESS:
                view = inflater.inflate(R.layout.item_list_transaction_progress, parent, false);
                out = new TxProgressViewHolder(view);
                break;
            case ITEM_TX:
            default:
                ItemListTxBinding b = ItemListTxBinding.inflate(inflater, parent, false);
                out = new TxAllViewHolder(b);
                break;
        }

        return out;
    }

    public static void bindViewHolder(List<MinterAddress> myAddresses, RecyclerView.ViewHolder holder, TransactionItem data) {
        if (holder instanceof TxHeaderViewHolder) {
            HeaderItem item = ((HeaderItem) data);
            ((TxHeaderViewHolder) holder).header.setText(item.getHeader());
        } else if (holder instanceof TxAllViewHolder) {
            final TxItem item = ((TxItem) data);
            ((TxAllViewHolder) holder).bind(item, myAddresses);
        }
    }

    public String getAvatar() {
        return mTx.getAvatar();
    }

    public void setAvatar(TransactionFacade.UserMeta meta) {
        mTx.userMeta = meta;
    }

    public void setAvatar(String url) {
        if (mTx.userMeta != null) {
            mTx.userMeta.avatarUrl = url;
        } else {
            mTx.userMeta = new TransactionFacade.UserMeta();
            mTx.userMeta.avatarUrl = url;
        }
    }

    public String getUsername() {
        return mTx.getName();
    }

    @SuppressLint("WrongConstant")
    @Override
    public int getViewType() {
        return mTx.get().type != null ? mTx.get().type.ordinal() + 1 : 0xFF;
    }

    public TransactionFacade getTx() {
        return mTx;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof TxItem) {
            return mTx.equals(((TxItem) obj).mTx);
        }

        return false;
    }

    @Override
    public boolean isSameOf(TransactionItem item) {
        return ((TxItem) item).getTx().get().hash.equals(mTx.get().hash);
    }
}
