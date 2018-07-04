/*
 * Copyright (C) 2018 by MinterTeam
 * @link https://github.com/MinterTeam
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

import android.arch.lifecycle.MutableLiveData;
import android.arch.paging.PagedListAdapter;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.transition.AutoTransition;
import android.support.transition.TransitionManager;
import android.support.v7.recyclerview.extensions.AsyncDifferConfig;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import network.minter.bipwallet.R;
import network.minter.bipwallet.internal.Wallet;
import network.minter.bipwallet.internal.views.widgets.BipCircleImageView;
import network.minter.blockchainapi.models.operational.Transaction;
import network.minter.explorerapi.models.HistoryTransaction;
import network.minter.mintercore.crypto.MinterAddress;

import static network.minter.bipwallet.internal.common.Preconditions.firstNonNull;
import static network.minter.bipwallet.tx.adapters.TransactionItem.ITEM_HEADER;
import static network.minter.bipwallet.tx.adapters.TransactionItem.ITEM_PROGRESS;
import static network.minter.bipwallet.tx.adapters.TransactionItem.TX_CONVERT_COIN;
import static network.minter.bipwallet.tx.adapters.TransactionItem.TX_CREATE_COIN;
import static network.minter.bipwallet.tx.adapters.TransactionItem.TX_DECLARE_CANDIDACY;
import static network.minter.bipwallet.tx.adapters.TransactionItem.TX_SEND;
import static network.minter.bipwallet.tx.adapters.TransactionItem.TX_SET_CANDIDATE_OFFLINE;
import static network.minter.bipwallet.tx.adapters.TransactionItem.TX_SET_CANDIDATE_ONLINE;


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

        View view;
        RecyclerView.ViewHolder out;
        switch (viewType) {
            case ITEM_HEADER:
                view = mInflater.inflate(R.layout.item_list_transaction_header, parent, false);
                out = new HeaderViewHolder(view);
                break;
            case ITEM_PROGRESS:
                view = mInflater.inflate(R.layout.item_list_transaction_progress, parent, false);
                out = new ProgressViewHolder(view);
                break;
            case TX_SEND:
                view = mInflater.inflate(R.layout.item_list_tx_send_coin_expandable, parent, false);
                out = new TxSendCoinViewHolder(view);
                break;
            case TX_CONVERT_COIN:
                view = mInflater.inflate(R.layout.item_list_tx_convert_coin_expandable, parent, false);
                out = new TxConvertCoinViewHolder(view);
                break;
            case TX_CREATE_COIN:
                view = mInflater.inflate(R.layout.item_list_tx_create_coin_expandable, parent, false);
                out = new TxCreateCoinViewHolder(view);
                break;
            case TX_DECLARE_CANDIDACY:
                view = mInflater.inflate(R.layout.item_list_tx_declare_candidacy_expandable, parent, false);
                out = new TxDeclareCandidacyViewHolder(view);
                break;
            case TX_SET_CANDIDATE_ONLINE:
            case TX_SET_CANDIDATE_OFFLINE:
                view = mInflater.inflate(R.layout.item_list_tx_set_candidate_on_off_expandable, parent, false);
                out = new TxSetCandidateOnlineOfflineViewHolder(view);
                break;

            default:
                view = mInflater.inflate(R.layout.item_list_tx_unhandled_expandable, parent, false);
                out = new TxUnhandledViewHolder(view);
                break;
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
        if (holder instanceof HeaderViewHolder) {
            HeaderItem item = ((HeaderItem) getItem(position));
            ((HeaderViewHolder) holder).header.setText(item.getHeader());
        } else if (holder instanceof ProgressViewHolder) {
            // do nothing
        } else if (holder instanceof TxSendCoinViewHolder) {
            final TxItem txItem = ((TxItem) getItem(position));
            ((TxSendCoinViewHolder) holder).bind(txItem, mMyAddresses);
        } else if (holder instanceof TxConvertCoinViewHolder) {
            final TxItem txItem = ((TxItem) getItem(position));
            ((TxConvertCoinViewHolder) holder).bind(txItem);
        } else if (holder instanceof TxCreateCoinViewHolder) {
            final TxItem txItem = ((TxItem) getItem(position));
            ((TxCreateCoinViewHolder) holder).bind(txItem);
        } else if (holder instanceof TxDeclareCandidacyViewHolder) {
            final TxItem txItem = ((TxItem) getItem(position));
            ((TxDeclareCandidacyViewHolder) holder).bind(txItem);
        } else if (holder instanceof TxSetCandidateOnlineOfflineViewHolder) {
            final TxItem txItem = ((TxItem) getItem(position));
            ((TxSetCandidateOnlineOfflineViewHolder) holder).bind(txItem);
        } else {
            final TxItem txItem = ((TxItem) getItem(position));
            ((TxUnhandledViewHolder) holder).bind(txItem);
        }

        if (holder instanceof ExpandableTxViewHolder) {
            final ExpandableTxViewHolder h = ((ExpandableTxViewHolder) holder);
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

    public static final class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView header;

        public HeaderViewHolder(View itemView) {
            super(itemView);
            header = ((TextView) ((ViewGroup) itemView).getChildAt(0));
        }
    }

    public static final class ProgressViewHolder extends RecyclerView.ViewHolder {
        public ProgressViewHolder(View itemView) {
            super(itemView);
        }
    }

    public static final class TxSendCoinViewHolder extends ExpandableTxViewHolder {
        @BindView(R.id.detail_from_value) TextView fromValue;
        @BindView(R.id.detail_to_value) TextView toValue;
        @BindView(R.id.detail_coin_value) TextView coinValue;
        @BindView(R.id.detail_amount_value) TextView amountValue;

        public TxSendCoinViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        void bind(TxItem txItem, List<MinterAddress> myAddresses) {
            super.bind(txItem);

            final HistoryTransaction item = txItem.getTx();
            final HistoryTransaction.TxSendCoinResult data = item.getData();

            final String am;

            if (!item.isIncoming(myAddresses)) {
                if (txItem.getUsername() != null) {
                    title.setText(String.format("@%s", txItem.getUsername()));
                } else {
                    if (data.to != null) {
                        title.setText(data.to.toShortString());
                    }

                }

                am = String.format("- %s", data.amount.toPlainString());
                amount.setText(am);
                amount.setTextColor(Wallet.app().res().getColor(R.color.textColorPrimary));
            } else {
                if (txItem.getUsername() != null) {
                    title.setText(txItem.getUsername());
                } else {
                    title.setText(data.to.toShortString());
                }
                am = String.format("+ %s", data.amount.toPlainString());
                amount.setText(am);
                amount.setTextColor(Wallet.app().res().getColor(R.color.textColorGreen));
            }

            fromValue.setText(data.from.toString());
            toValue.setText(data.to.toString());
            subamount.setText(data.getCoin());
            coinValue.setText(data.getCoin());
            amountValue.setText(am);
        }
    }

    static class ExpandableTxViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.item_avatar) BipCircleImageView avatar;
        @BindView(R.id.item_title) TextView title;
        @BindView(R.id.item_amount) TextView amount;
        @BindView(R.id.item_subamount) TextView subamount;
        @BindView(R.id.detail_date_value) TextView dateValue;
        @BindView(R.id.detail_time_value) TextView timeValue;
        @BindView(R.id.action) Button action;
        @BindView(R.id.layout_details) ConstraintLayout detailsLayout;

        public ExpandableTxViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        protected void bind(TxItem item) {
            avatar.setImageUrl(item.getAvatar());
            final DateTime dt = new DateTime(item.getTx().timestamp);
            dateValue.setText(dt.toString(DateTimeFormat.forPattern("EEEE, dd MMMM")));
            timeValue.setText(dt.toString(DateTimeFormat.forPattern("HH:mm:ssZ")));
        }
    }

    public static final class TxConvertCoinViewHolder extends ExpandableTxViewHolder {
        @BindView(R.id.detail_coin_from_value) TextView coinFrom;
        @BindView(R.id.detail_coin_to_value) TextView coinTo;
        @BindView(R.id.detail_convert_amount_value) TextView convertAmount;

        public TxConvertCoinViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        @Override
        protected void bind(TxItem item) {
            super.bind(item);
            final HistoryTransaction.TxConvertCoinResult data = item.getTx().getData();
            title.setText(item.getTx().hash.toShortString());
            amount.setText(String.format("- %s", firstNonNull(data.amount, new BigDecimal(0)).toPlainString()));
            subamount.setText(String.format("%s -> %s", firstNonNull(data.getFromCoin(), "<unknown>"), firstNonNull(data.getToCoin(), "<unknown>")));
            coinFrom.setText(firstNonNull(data.getFromCoin(), "<unknown>"));
            coinTo.setText(firstNonNull(data.getToCoin(), "<unknown>"));
            convertAmount.setText(firstNonNull(data.amount, new BigDecimal(0)).toPlainString());
        }
    }

    public static final class TxCreateCoinViewHolder extends ExpandableTxViewHolder {
        @BindView(R.id.detail_coin_name_value) TextView coinName;
        @BindView(R.id.detail_coin_symbol_value) TextView coinSymbol;
        @BindView(R.id.detail_initial_amount_value) TextView initialAmount;
        @BindView(R.id.detail_initial_reserve_value) TextView initialReserve;
        @BindView(R.id.detail_constant_reserve_ratio_value) TextView crr;

        public TxCreateCoinViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        @Override
        protected void bind(TxItem item) {
            super.bind(item);
            final HistoryTransaction.TxCreateResult data = item.getTx().getData();
            title.setText(item.getTx().hash.toShortString());
            amount.setText(String.format("Create coin: %s", firstNonNull(data.getSymbol(), "<unknown>")));
            coinName.setText(firstNonNull(data.name, "<unknown>"));
            coinSymbol.setText(firstNonNull(data.getSymbol(), "<unknown>"));
            initialAmount.setText(firstNonNull(data.initialAmount, new BigDecimal(0)).toPlainString());
            initialReserve.setText(firstNonNull(data.initialReserve, new BigDecimal(0)).toPlainString());
            crr.setText(firstNonNull(data.constantReserveRatio, new BigDecimal(0)).toPlainString());
        }
    }

    public static final class TxUnhandledViewHolder extends ExpandableTxViewHolder {
        @BindView(R.id.detail_type_value) TextView txType;

        public TxUnhandledViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        @Override
        protected void bind(TxItem item) {
            super.bind(item);
            title.setText(item.getTx().hash.toShortString());
            if (item.getTx().type != null) {
                txType.setText(item.getTx().type.name());
            } else {
                txType.setText("<unknown>");
            }
        }
    }

    public static final class TxSetCandidateOnlineOfflineViewHolder extends ExpandableTxViewHolder {
        @BindView(R.id.detail_pub_value) TextView pubKey;

        public TxSetCandidateOnlineOfflineViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        @Override
        protected void bind(TxItem item) {
            super.bind(item);
            final HistoryTransaction.TxSetCandidateOnlineOfflineResult data = item.getTx().getData();
            title.setText(item.getTx().hash.toShortString());
            amount.setText(item.getTx().type == HistoryTransaction.Type.SetCandidateOnline ? "Set candidate online" : "Set candidate offline");
            if (data.pubKey != null) {
                pubKey.setText(data.pubKey.toString());
            } else {
                pubKey.setText("<unknown>");
            }
        }
    }

    public static final class TxDeclareCandidacyViewHolder extends ExpandableTxViewHolder {
        @BindView(R.id.detail_pub_value) TextView pubKey;
        @BindView(R.id.detail_address_value) TextView address;
        @BindView(R.id.detail_commission_value) TextView commission;
        @BindView(R.id.detail_coin_value) TextView coin;
        @BindView(R.id.detail_stake_value) TextView stake;

        public TxDeclareCandidacyViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        @Override
        protected void bind(TxItem item) {
            super.bind(item);
            title.setText(item.getTx().hash.toShortString());
            amount.setText("Declare Candidacy");
            final HistoryTransaction.TxDeclareCandidacyResult data = item.getTx().getData();
            if (data.pubKey != null) {
                pubKey.setText(data.pubKey.toString());
            } else {
                pubKey.setText("<unknown>");
            }

            if (data.address != null) {
                address.setText(data.address.toString());
            } else {
                address.setText("<unknown>");
            }

            commission.setText(String.format("%s%%", firstNonNull(data.commission, new BigDecimal(0)).toPlainString()));
            coin.setText(data.getCoin());
            stake.setText(firstNonNull(data.stake, new BigDecimal(0)).divide(Transaction.VALUE_MUL_DEC).toPlainString());

        }
    }
}
