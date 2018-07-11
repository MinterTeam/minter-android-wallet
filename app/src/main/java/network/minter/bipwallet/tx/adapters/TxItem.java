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

import android.annotation.SuppressLint;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import network.minter.bipwallet.R;
import network.minter.bipwallet.tx.adapters.vh.TxConvertCoinViewHolder;
import network.minter.bipwallet.tx.adapters.vh.TxCreateCoinViewHolder;
import network.minter.bipwallet.tx.adapters.vh.TxDeclareCandidacyViewHolder;
import network.minter.bipwallet.tx.adapters.vh.TxHeaderViewHolder;
import network.minter.bipwallet.tx.adapters.vh.TxProgressViewHolder;
import network.minter.bipwallet.tx.adapters.vh.TxSendCoinViewHolder;
import network.minter.bipwallet.tx.adapters.vh.TxSetCandidateOnlineOfflineViewHolder;
import network.minter.bipwallet.tx.adapters.vh.TxUnhandledViewHolder;
import network.minter.explorerapi.models.HistoryTransaction;
import network.minter.mintercore.crypto.MinterAddress;

/**
 * MinterWallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class TxItem implements TransactionItem {
    private final HistoryTransaction mTx;
    private String mAvatar;
    private String mUsername;

    public TxItem(HistoryTransaction tx) {
        mTx = tx;
        mAvatar = tx.getAvatar();
        mUsername = tx.username;
    }

    public String getAvatar() {
        if (mAvatar == null) {
            return "https://my.beta.minter.network/api/v1/avatar/by/user/1";
        }

        return mAvatar;
    }

    public TxItem setAvatar(String avatar) {
        mAvatar = avatar;
        return this;
    }

    public String getUsername() {
        return mUsername;
    }

    public TxItem setUsername(String username) {
        mUsername = username;
        return this;
    }

    @SuppressLint("WrongConstant")
    @Override
    public int getViewType() {
        return mTx.type != null ? mTx.type.ordinal() + 1 : 0xFF;
    }

    public HistoryTransaction getTx() {
        return mTx;
    }

    @Override
    public boolean isSameOf(TransactionItem item) {
        return item.getViewType() == TX_SEND && ((TxItem) item).getTx().hash.equals(mTx.hash);
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
            case TX_SEND:
                view = inflater.inflate(R.layout.item_list_tx_send_coin_expandable, parent, false);
                out = new TxSendCoinViewHolder(view);
                break;
            case TX_CONVERT_COIN:
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
        } else {
            final TxItem txItem = ((TxItem) data);
            ((TxUnhandledViewHolder) holder).bind(txItem);
        }
    }
}
