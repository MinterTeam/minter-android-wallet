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
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.paging.PagedListAdapter;
import androidx.recyclerview.widget.AsyncDifferConfig;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import network.minter.bipwallet.internal.adapter.LoadState;
import network.minter.bipwallet.tx.adapters.vh.TxAllViewHolder;
import network.minter.core.crypto.MinterAddress;

import static network.minter.bipwallet.tx.adapters.TransactionItem.ITEM_PROGRESS;


/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class TransactionListAdapter extends PagedListAdapter<TransactionItem, RecyclerView.ViewHolder> {
    private final static DiffUtil.ItemCallback<TransactionItem> sDiffCallback = new DiffUtil.ItemCallback<TransactionItem>() {
        @Override
        public boolean areItemsTheSame(TransactionItem oldItem, TransactionItem newItem) {
            return oldItem.isSameOf(newItem);
        }

        @Override
        public boolean areContentsTheSame(TransactionItem oldItem, TransactionItem newItem) {
            return oldItem.equals(newItem);
        }
    };
    List<MinterAddress> mMyAddresses = new ArrayList<>();
    private LayoutInflater mInflater;
    private MutableLiveData<LoadState> mLoadState;
    private OnExpandDetailsListener mOnExpandDetailsListener;

    public TransactionListAdapter(List<MinterAddress> addresses) {
        super(sDiffCallback);
        mMyAddresses = addresses;
    }

    protected TransactionListAdapter(@NonNull AsyncDifferConfig<TransactionItem> config) {
        super(config);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, @TransactionItem.ListType int viewType) {
        if (mInflater == null) {
            mInflater = LayoutInflater.from(parent.getContext());
        }

        return TxItem.createViewHolder(mInflater, parent, viewType);
    }

    @Override
    public int getItemViewType(int position) {
        if (hasProgressRow() && position == getItemCount() - 1) {
            return ITEM_PROGRESS;
        }

        return getItem(position).getViewType();
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        TxItem.bindViewHolder(mMyAddresses, holder, getItem(position));

        if (holder instanceof TxAllViewHolder) {
            holder.itemView.setOnClickListener(v -> {
                if (mOnExpandDetailsListener != null) {
                    mOnExpandDetailsListener.onExpand(v, ((TxItem) getItem(holder.getAdapterPosition())).getTx());
                }
            });
        }
    }

    public void setLoadState(MutableLiveData<LoadState> loadState) {
        mLoadState = loadState;
    }

    public void setOnExpandDetailsListener(OnExpandDetailsListener listener) {
        mOnExpandDetailsListener = listener;
    }

    private boolean hasProgressRow() {
        return mLoadState != null && mLoadState.getValue() != LoadState.Loaded;
    }

    public interface OnExpandDetailsListener {
        void onExpand(View view, TransactionFacade tx);
    }

    public interface OnExplorerOpenClickListener {
        void onClick(View view, TransactionFacade tx);
    }

}
