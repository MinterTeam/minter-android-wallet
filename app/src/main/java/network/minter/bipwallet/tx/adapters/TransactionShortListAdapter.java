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

import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.annimon.stream.Stream;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import network.minter.bipwallet.internal.views.list.diff.DiffUtilDispatcher;
import network.minter.bipwallet.internal.views.list.diff.DiffUtilDispatcherDelegate;
import network.minter.bipwallet.tx.adapters.vh.TxAllViewHolder;
import network.minter.core.crypto.MinterAddress;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class TransactionShortListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements DiffUtilDispatcherDelegate<TransactionFacade> {

    private final List<MinterAddress> mMyAddresses;
    private List<TxItem> mItems = new ArrayList<>(0);
    private LayoutInflater mInflater;
    private TransactionListAdapter.OnExpandDetailsListener mOnExpandDetailsListener;

    public void setOnExpandDetailsListener(TransactionListAdapter.OnExpandDetailsListener listener) {
        mOnExpandDetailsListener = listener;
    }

    public TransactionShortListAdapter(List<MinterAddress> myAddresses) {
        mMyAddresses = myAddresses;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (mInflater == null) {
            mInflater = LayoutInflater.from(parent.getContext());
        }

        return TxItem.createViewHolder(mInflater, parent, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        TxItem.bindViewHolder(mMyAddresses, holder, mItems.get(position));

        if (holder instanceof TxAllViewHolder) {
            holder.itemView.setOnClickListener(v -> {
                if (mOnExpandDetailsListener != null) {
                    mOnExpandDetailsListener.onExpand(v, getItem(holder.getAdapterPosition()).getTx());
                }
            });
        }
    }

    public TxItem getItem(int position) {
        return mItems.get(position);
    }

    @Override
    public int getItemViewType(int position) {
        return mItems.get(position).getViewType();
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @Override
    public <T extends DiffUtil.Callback> void dispatchChanges(Class<T> diffUtilCallbackCls, @NonNull List<TransactionFacade> items, boolean detectMoves) {
        DiffUtilDispatcher.dispatchChanges(this, diffUtilCallbackCls, items, detectMoves);
    }

    @Override
    public <T extends DiffUtil.Callback> void dispatchChanges(Class<T> diffUtilCallbackCls, @NonNull List<TransactionFacade> items) {
        DiffUtilDispatcher.dispatchChanges(this, diffUtilCallbackCls, items);
    }

    @Override
    public List<TransactionFacade> getItems() {
        return Stream.of(mItems)
                .map(TxItem::getTx)
                .toList();
    }

    @Override
    public void setItems(List<TransactionFacade> items) {
        mItems = Stream.of(items)
                .map(TxItem::new)
                .toList();
    }

    @Override
    public void clear() {
        mItems.clear();
        notifyDataSetChanged();
    }
}
