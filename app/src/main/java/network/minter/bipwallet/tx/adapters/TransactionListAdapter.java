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

package network.minter.bipwallet.tx.adapters;

import android.arch.lifecycle.MutableLiveData;
import android.arch.paging.PagedListAdapter;
import android.support.annotation.NonNull;
import android.support.transition.AutoTransition;
import android.support.transition.TransitionManager;
import android.support.v7.recyclerview.extensions.AsyncDifferConfig;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import network.minter.bipwallet.tx.adapters.vh.ExpandableTxViewHolder;
import network.minter.explorerapi.models.HistoryTransaction;
import network.minter.mintercore.crypto.MinterAddress;

import static network.minter.bipwallet.tx.adapters.TransactionItem.ITEM_PROGRESS;


/**
 * MinterWallet. 2018
 *
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
    private int mExpandedPosition = -1;
    private OnExplorerOpenClickListener mOnExplorerOpenClickListener;
    private MutableLiveData<TransactionDataSource.LoadState> mLoadState;
    private boolean mEnableExpanding = true;

    public TransactionListAdapter(List<MinterAddress> addresses, boolean enableExpanding) {
        super(sDiffCallback);
        mMyAddresses = addresses;
        mEnableExpanding = enableExpanding;
    }

    public TransactionListAdapter(List<MinterAddress> addresses) {
        super(sDiffCallback);
        mMyAddresses = addresses;
    }

    protected TransactionListAdapter(@NonNull AsyncDifferConfig<TransactionItem> config) {
        super(config);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (mInflater == null) {
            mInflater = LayoutInflater.from(parent.getContext());
        }

        final RecyclerView.ViewHolder out = TxItem.createViewHolder(mInflater, parent, viewType);

        if (out instanceof ExpandableTxViewHolder) {
            ((ExpandableTxViewHolder) out).setEnableExpanding(mEnableExpanding);
        }

        return out;
    }

    @Override
    public int getItemViewType(int position) {
        if (hasProgressRow() && position == getItemCount() - 1) {
            return ITEM_PROGRESS;
        }

        return getItem(position).getViewType();
    }

    public void setOnExplorerOpenClickListener(OnExplorerOpenClickListener listener) {
        mOnExplorerOpenClickListener = listener;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        TxItem.bindViewHolder(mMyAddresses, holder, getItem(position));

        if (holder instanceof ExpandableTxViewHolder) {
            final ExpandableTxViewHolder h = ((ExpandableTxViewHolder) holder);

            if (h.isEnableExpanding()) {
                final TxItem item = ((TxItem) getItem(position));
                h.action.setOnClickListener(v -> {
                    if (mOnExplorerOpenClickListener != null) {
                        mOnExplorerOpenClickListener.onClick(v, item.getTx());
                    }
                });

                final boolean isExpanded = position == mExpandedPosition;
                h.detailsLayout.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
                h.detailsLayout.setActivated(isExpanded);
                h.itemView.setOnClickListener(v -> {
                    int prevExp = mExpandedPosition;
                    mExpandedPosition = isExpanded ? -1 : position;
                    TransitionManager.beginDelayedTransition(((ViewGroup) h.itemView), new AutoTransition());
                    notifyItemChanged(holder.getAdapterPosition());
                    notifyItemChanged(prevExp);
                });
            } else {
                h.detailsLayout.setVisibility(View.GONE);
            }
        }
    }

    public void setLoadState(MutableLiveData<TransactionDataSource.LoadState> loadState) {
        mLoadState = loadState;
    }

    private boolean hasProgressRow() {
        return mLoadState != null && mLoadState.getValue() != TransactionDataSource.LoadState.Loaded;
    }

    public interface OnExplorerOpenClickListener {
        void onClick(View view, HistoryTransaction tx);
    }

}
