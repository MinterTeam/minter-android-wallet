package network.minter.bipwallet.delegation.adapter;

import android.arch.lifecycle.MutableLiveData;
import android.arch.paging.PagedListAdapter;
import android.support.annotation.NonNull;
import android.support.v7.recyclerview.extensions.AsyncDifferConfig;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import network.minter.bipwallet.R;
import network.minter.bipwallet.internal.adapter.LoadState;
import network.minter.bipwallet.internal.helpers.ContextHelper;
import network.minter.bipwallet.tx.adapters.vh.TxProgressViewHolder;

import static network.minter.bipwallet.internal.helpers.MathHelper.bdHuman;

/**
 * Created by Alexander Kolpakov (jquickapp@gmail.com) on 06-Jun-19
 */
public class DelegationListAdapter extends PagedListAdapter<DelegationItem, RecyclerView.ViewHolder> {

    private static int ITEM_PROGRESS = R.layout.item_list_transaction_progress;
    private static int ITEM_DELEGATION = R.layout.item_list_delegation;

    private LayoutInflater mInflater;
    private MutableLiveData<LoadState> mLoadState;

    private final static DiffUtil.ItemCallback<DelegationItem> sDiffCallback =
            new DiffUtil.ItemCallback<DelegationItem>() {
                @Override
                public boolean areItemsTheSame(DelegationItem oldItem, DelegationItem newItem) {
                    return oldItem.pubKey.toString().equals(newItem.pubKey.toString());
                }

                @Override
                public boolean areContentsTheSame(DelegationItem oldItem, DelegationItem newItem) {
                    return oldItem.equals(newItem);
                }
            };

    public DelegationListAdapter() {
        super(sDiffCallback);
    }

    protected DelegationListAdapter(@NonNull AsyncDifferConfig<DelegationItem> config) {
        super(config);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (mInflater == null) {
            mInflater = LayoutInflater.from(parent.getContext());
        }
        View view = mInflater.inflate(viewType, parent, false);
        if (viewType == ITEM_PROGRESS) return new TxProgressViewHolder(view);
        return new DelegationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
        if (getItemViewType(i) == ITEM_DELEGATION)
            ((DelegationViewHolder) viewHolder).bind(getItem(i));
    }

    @Override
    public int getItemViewType(int position) {
        if (hasProgressRow() && position == getItemCount() - 1) {
            return ITEM_PROGRESS;
        }
        return ITEM_DELEGATION;
    }

    public void setLoadState(MutableLiveData<LoadState> loadState) {
        mLoadState = loadState;
    }

    private boolean hasProgressRow() {
        return mLoadState != null && mLoadState.getValue() != LoadState.Loaded;
    }

    public class DelegationViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.item_public_key)
        TextView publicKey;
        @BindView(R.id.item_copy)
        View actionCopy;
        @BindView(R.id.delegation_info)
        LinearLayout coins;

        DelegationViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        public void bind(DelegationItem item) {
            publicKey.setText(item.pubKey.toShortString());
            actionCopy.setOnClickListener(v ->
                    ContextHelper.copyToClipboard(actionCopy.getContext(), item.pubKey.toString()));

            coins.removeAllViews();
            for (DelegationItem.DelegatedCoin coin : item.coins) {
                View view = mInflater.inflate(R.layout.item_list_delegation_coin, coins, false);
                TextView coinText = view.findViewById(R.id.item_coin);
                TextView amountText = view.findViewById(R.id.item_amount);
                coinText.setText(coin.coin);
                amountText.setText(bdHuman(coin.amount));
                coins.addView(view);
            }
        }
    }
}
