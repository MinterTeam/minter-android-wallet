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

package network.minter.bipwallet.delegation.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.zerobranch.layout.SwipeLayout;

import org.jetbrains.annotations.NotNull;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.paging.PagedListAdapter;
import androidx.recyclerview.widget.AsyncDifferConfig;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import network.minter.bipwallet.R;
import network.minter.bipwallet.internal.adapter.LoadState;
import network.minter.bipwallet.internal.helpers.ViewHelper;
import network.minter.bipwallet.internal.views.widgets.BipCircleImageView;
import network.minter.bipwallet.tx.adapters.vh.TxProgressViewHolder;
import network.minter.core.MinterSDK;

import static network.minter.bipwallet.internal.common.Preconditions.firstNonNull;
import static network.minter.bipwallet.internal.helpers.MathHelper.bdHuman;

/**
 * Created by Alexander Kolpakov (jquickapp@gmail.com) on 06-Jun-19
 */
public class DelegationListAdapter extends PagedListAdapter<DelegatedItem, RecyclerView.ViewHolder> {

    private final static DiffUtil.ItemCallback<DelegatedItem> sDiffCallback =
            new DiffUtil.ItemCallback<DelegatedItem>() {
                @Override
                public boolean areItemsTheSame(DelegatedItem oldItem, @NotNull DelegatedItem newItem) {
                    return oldItem.isSameOf(newItem);
                }

                @Override
                public boolean areContentsTheSame(DelegatedItem oldItem, @NotNull DelegatedItem newItem) {
                    return oldItem.equals(newItem);
                }
            };
    private static int ITEM_PROGRESS = R.layout.item_list_transaction_progress;
    private LayoutInflater mInflater;
    private MutableLiveData<LoadState> mLoadState;
    private int mLastOpened = -1;
    private OnDelegatedClickListener mOnDelegatedClickListener;
    private OnUnbondItemClickListener mOnUnbondItemClickListener;

    public DelegationListAdapter() {
        super(sDiffCallback);
    }

    protected DelegationListAdapter(@NonNull AsyncDifferConfig<DelegatedItem> config) {
        super(config);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (mInflater == null) {
            mInflater = LayoutInflater.from(parent.getContext());
        }
        View view = mInflater.inflate(viewType, parent, false);
        if (viewType == ITEM_PROGRESS) {
            return new TxProgressViewHolder(view);
        } else if (viewType == DelegatedItem.ITEM_STAKE) {
            return new StakeViewHolder(view);
        }
        return new ValidatorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
        if (getItemViewType(i) == DelegatedItem.ITEM_VALIDATOR) {
            ValidatorViewHolder vh = ((ValidatorViewHolder) viewHolder);
            DelegatedValidator item = ((DelegatedValidator) getItem(i));

            ViewHelper.visible(vh.fakeHeader, i > 0);
            vh.title.setText(firstNonNull(item.name, vh.title.getContext().getString(R.string.label_public_key)));
            vh.publicKey.setText(item.pubKey.toShortString());
            if (item.description != null) {
                ViewCompat.setTooltipText(vh.publicKey, item.description);
            }

            if (item.getImageUrl() != null) {
                vh.icon.setImageUrlFallback(item.getImageUrl(), R.drawable.img_avatar_default);
            } else {
                vh.icon.setImageResource(R.drawable.img_avatar_delegate);
            }

            vh.actionDelegate.setOnClickListener(v -> {
                if (mOnDelegatedClickListener != null) {
                    mOnDelegatedClickListener.onDelegateClick(((DelegatedValidator) getItem(viewHolder.getAdapterPosition())));
                }
            });
        } else if (getItemViewType(i) == DelegatedItem.ITEM_STAKE) {
            StakeViewHolder vh = ((StakeViewHolder) viewHolder);
            DelegatedStake item = ((DelegatedStake) getItem(i));

            ((SwipeLayout) vh.itemView).setOnActionsListener(new SwipeLayout.SwipeActionsListener() {
                @Override
                public void onOpen(int direction, boolean isContinuous) {
                    closeOpened();
                    mLastOpened = vh.getAdapterPosition();
                }

                @Override
                public void onClose() {
                }
            });
            if (vh.getAdapterPosition() == mLastOpened) {
                vh.itemView.post(() -> {
                    mLastOpened = -1;
                    ((SwipeLayout) vh.itemView).close(true);
                });
            }

            vh.avatar.setImageUrl(item);
            vh.coin.setText(item.coin);
            vh.amount.setText(bdHuman(item.amount));
            if (item.coin.equals(MinterSDK.DEFAULT_COIN)) {
                vh.subamount.setVisibility(View.GONE);
                vh.subamount.setText(null);
            } else {
                vh.subamount.setText(String.format("%s %s", bdHuman(item.amountBIP), MinterSDK.DEFAULT_COIN));
                vh.subamount.setVisibility(View.VISIBLE);
            }
            vh.actionUnbond.setOnClickListener(v -> {
                if (mOnUnbondItemClickListener != null) {
                    mOnUnbondItemClickListener.onUnbondClick(((DelegatedStake) getItem(viewHolder.getAdapterPosition())));
                }
            });
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (hasProgressRow() && position == getItemCount() - 1) {
            return ITEM_PROGRESS;
        }
        return getItem(position).getViewType();
    }

    public void setLoadState(MutableLiveData<LoadState> loadState) {
        mLoadState = loadState;
    }

    public void closeOpened() {
        if (mLastOpened >= 0) {
            notifyItemChanged(mLastOpened);
        }

    }

    public void setOnDelegatedClickListener(OnDelegatedClickListener listener) {
        mOnDelegatedClickListener = listener;
    }

    public void setOnUnbondItemClickListener(OnUnbondItemClickListener listener) {
        mOnUnbondItemClickListener = listener;
    }

    private boolean hasProgressRow() {
        return mLoadState != null && mLoadState.getValue() != LoadState.Loaded;
    }

    public interface OnDelegatedClickListener {
        void onDelegateClick(DelegatedValidator validator);
    }

    public interface OnUnbondItemClickListener {
        void onUnbondClick(DelegatedStake stake);
    }

    public static class StakeViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.item_avatar) BipCircleImageView avatar;
        @BindView(R.id.item_coin) TextView coin;
        @BindView(R.id.item_amount) TextView amount;
        @BindView(R.id.item_subamount) TextView subamount;
        @BindView(R.id.action_unbond) View actionUnbond;
        @BindView(R.id.main) View main;

        public StakeViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    public static class ValidatorViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.fake_header) View fakeHeader;
        @BindView(R.id.item_title) TextView title;
        @BindView(R.id.item_public_key) TextView publicKey;
        @BindView(R.id.action_delegate) View actionDelegate;
        @BindView(R.id.item_avatar) BipCircleImageView icon;

        ValidatorViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
