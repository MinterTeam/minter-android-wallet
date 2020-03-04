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

package network.minter.bipwallet.tx.adapters;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import network.minter.bipwallet.R;
import network.minter.bipwallet.tx.adapters.vh.TxConvertCoinViewHolder;
import network.minter.bipwallet.tx.adapters.vh.TxCreateCoinViewHolder;
import network.minter.bipwallet.tx.adapters.vh.TxCreateMultisigAddressViewHolder;
import network.minter.bipwallet.tx.adapters.vh.TxDeclareCandidacyViewHolder;
import network.minter.bipwallet.tx.adapters.vh.TxDelegateUnboundViewHolder;
import network.minter.bipwallet.tx.adapters.vh.TxEditCandidateViewHolder;
import network.minter.bipwallet.tx.adapters.vh.TxHeaderViewHolder;
import network.minter.bipwallet.tx.adapters.vh.TxMultiSendCoinViewHolder;
import network.minter.bipwallet.tx.adapters.vh.TxProgressViewHolder;
import network.minter.bipwallet.tx.adapters.vh.TxRedeemCheckViewHolder;
import network.minter.bipwallet.tx.adapters.vh.TxSendCoinViewHolder;
import network.minter.bipwallet.tx.adapters.vh.TxSetCandidateOnlineOfflineViewHolder;
import network.minter.bipwallet.tx.adapters.vh.TxUnhandledViewHolder;
import network.minter.core.crypto.MinterAddress;

/**
 * minter-android-wallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class TxItem implements TransactionItem {
    private final TransactionFacade mTx;

    public TxItem(TransactionFacade tx) {
        mTx = tx;
    }

    public static RecyclerView.ViewHolder createViewHolder(final LayoutInflater inflater, final ViewGroup parent,
                                                           @ListType int viewType) {
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
            case TX_SEND:
                view = inflater.inflate(R.layout.item_list_tx_send_coin_expandable, parent, false);
                out = new TxSendCoinViewHolder(view);
                break;
            case TX_SELL_COIN:
            case TX_BUY_COIN:
            case TX_SELL_ALL_COINS:
                view = inflater.inflate(R.layout.item_list_tx_convert_coin_expandable, parent, false);
                out = new TxConvertCoinViewHolder(view);
                break;
            case TX_CREATE_COIN:
                view = inflater.inflate(R.layout.item_list_tx_create_coin_expandable, parent, false);
                out = new TxCreateCoinViewHolder(view);
                break;
            case TX_DECLARE_CANDIDACY:
                view = inflater.inflate(R.layout.item_list_tx_declare_candidacy_expandable, parent, false);
                out = new TxDeclareCandidacyViewHolder(view);
                break;
            case TX_SET_CANDIDATE_ONLINE:
            case TX_SET_CANDIDATE_OFFLINE:
                view = inflater.inflate(R.layout.item_list_tx_set_candidate_on_off_expandable, parent, false);
                out = new TxSetCandidateOnlineOfflineViewHolder(view);
                break;
            case TX_DELEGATE:
            case TX_UNBOUND:
                view = inflater.inflate(R.layout.item_list_tx_delegate_unbound_expandable, parent, false);
                out = new TxDelegateUnboundViewHolder(view);
                break;
            case TX_MULTISEND:
                view = inflater.inflate(R.layout.item_list_tx_multisend_expandable, parent, false);
                out = new TxMultiSendCoinViewHolder(view);
                break;
            case TX_EDIT_CANDIDATE:
                view = inflater.inflate(R.layout.item_list_tx_set_candidate_on_off_expandable, parent, false);
                out = new TxEditCandidateViewHolder(view);
                break;
            case TX_REDEEM_CHECK:
                view = inflater.inflate(R.layout.item_list_tx_redeem_check_expandable, parent, false);
                out = new TxRedeemCheckViewHolder(view);
                break;
            case TX_CREATE_MULTISIG_ADDRESS:
                view = inflater.inflate(R.layout.item_list_tx_create_multisig_expandable, parent, false);
                out = new TxCreateMultisigAddressViewHolder(view);
                break;
            default:
                view = inflater.inflate(R.layout.item_list_tx_unhandled_expandable, parent, false);
                out = new TxUnhandledViewHolder(view);
                break;
        }

        return out;
    }

    public static void bindViewHolder(List<MinterAddress> myAddresses, RecyclerView.ViewHolder holder, TransactionItem data) {
        if (holder instanceof TxHeaderViewHolder) {
            HeaderItem item = ((HeaderItem) data);
            ((TxHeaderViewHolder) holder).header.setText(item.getHeader());
        } else if (holder instanceof TxProgressViewHolder) {
            // do nothing
        } else if (holder instanceof TxSendCoinViewHolder) {
            final TxItem txItem = ((TxItem) data);
            ((TxSendCoinViewHolder) holder).bind(txItem, myAddresses);
        } else if (holder instanceof TxConvertCoinViewHolder) {
            final TxItem txItem = ((TxItem) data);
            ((TxConvertCoinViewHolder) holder).bind(txItem);
        } else if (holder instanceof TxCreateCoinViewHolder) {
            final TxItem txItem = ((TxItem) data);
            ((TxCreateCoinViewHolder) holder).bind(txItem);
        } else if (holder instanceof TxDeclareCandidacyViewHolder) {
            final TxItem txItem = ((TxItem) data);
            ((TxDeclareCandidacyViewHolder) holder).bind(txItem);
        } else if (holder instanceof TxSetCandidateOnlineOfflineViewHolder) {
            final TxItem txItem = ((TxItem) data);
            ((TxSetCandidateOnlineOfflineViewHolder) holder).bind(txItem);
        } else if (holder instanceof TxDelegateUnboundViewHolder) {
            final TxItem txItem = ((TxItem) data);
            ((TxDelegateUnboundViewHolder) holder).bind(txItem);
        } else if (holder instanceof TxMultiSendCoinViewHolder) {
            final TxItem txItem = ((TxItem) data);
            ((TxMultiSendCoinViewHolder) holder).bind(txItem, myAddresses);
        } else if (holder instanceof TxEditCandidateViewHolder) {
            final TxItem txItem = ((TxItem) data);
            ((TxEditCandidateViewHolder) holder).bind(txItem);
        } else if (holder instanceof TxRedeemCheckViewHolder) {
            final TxItem txItem = ((TxItem) data);
            ((TxRedeemCheckViewHolder) holder).bind(txItem, myAddresses);
        } else if(holder instanceof TxCreateMultisigAddressViewHolder) {
            final TxItem txItem = ((TxItem) data);
            ((TxCreateMultisigAddressViewHolder) holder).bind(txItem);
        } else {
            final TxItem txItem = ((TxItem) data);
            ((TxUnhandledViewHolder) holder).bind(txItem);
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
