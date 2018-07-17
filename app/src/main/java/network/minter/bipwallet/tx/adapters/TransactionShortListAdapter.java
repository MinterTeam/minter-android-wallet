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

package network.minter.bipwallet.tx.adapters;

import android.support.annotation.NonNull;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.annimon.stream.Stream;

import java.util.ArrayList;
import java.util.List;

import network.minter.bipwallet.internal.views.list.diff.DiffUtilDispatcher;
import network.minter.bipwallet.internal.views.list.diff.DiffUtilDispatcherDelegate;
import network.minter.core.crypto.MinterAddress;
import network.minter.explorer.models.HistoryTransaction;

/**
 * minter-android-wallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class TransactionShortListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements DiffUtilDispatcherDelegate<HistoryTransaction> {

    private final List<MinterAddress> mMyAddresses;
    private List<TxItem> mItems = new ArrayList<>(0);
    private LayoutInflater mInflater;

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
    public <T extends DiffUtil.Callback> void dispatchChanges(Class<T> diffUtilCallbackCls, @NonNull List<HistoryTransaction> items, boolean detectMoves) {
        DiffUtilDispatcher.dispatchChanges(this, diffUtilCallbackCls, items, detectMoves);
    }

    @Override
    public <T extends DiffUtil.Callback> void dispatchChanges(Class<T> diffUtilCallbackCls, @NonNull List<HistoryTransaction> items) {
        DiffUtilDispatcher.dispatchChanges(this, diffUtilCallbackCls, items);
    }

    @Override
    public List<HistoryTransaction> getItems() {
        return Stream.of(mItems)
                .map(TxItem::getTx)
                .toList();
    }

    @Override
    public void setItems(List<HistoryTransaction> items) {
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
