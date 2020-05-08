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

package network.minter.bipwallet.wallets.selector;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import network.minter.bipwallet.R;

import static network.minter.bipwallet.internal.common.Preconditions.firstNonNull;

public class WalletListAdapter extends RecyclerView.Adapter<WalletListAdapter.ViewHolder> {
    private List<WalletItem> mAddresses = Collections.emptyList();
    private LayoutInflater mInflater;
    private OnClickAddWalletListener mOnClickAddWalletListener;
    private OnClickEditWalletListener mOnClickEditWalletListener;
    private OnClickWalletListener mOnClickWalletListener;

    public WalletListAdapter() {

    }

    public WalletListAdapter(List<WalletItem> addresses) {
        mAddresses = addresses;
    }

    public void setWallets(List<WalletItem> addresses) {
        mAddresses = addresses;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (mInflater == null) {
            mInflater = LayoutInflater.from(parent.getContext());
        }

        View view = mInflater.inflate(R.layout.item_list_wallet, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (position == mAddresses.size()) {
            holder.iconAdd.setVisibility(View.VISIBLE);
            holder.weight.setVisibility(View.GONE);
            holder.weight.setText(null);
            holder.name.setText(R.string.btn_add_wallet);
            holder.action.setVisibility(View.GONE);

            holder.action.setOnClickListener(null);
            holder.itemView.setOnClickListener(v -> {
                if (mOnClickAddWalletListener != null) {
                    mOnClickAddWalletListener.onClickAddWallet();
                }
            });
        } else {
            final WalletItem item = mAddresses.get(position);

            holder.iconAdd.setVisibility(View.GONE);
            holder.weight.setVisibility(View.VISIBLE);
            holder.weight.setText(item.getWeight().getEmoji());
            holder.name.setText(firstNonNull(item.getTitle(), item.getAddressShort()));
            holder.action.setVisibility(View.VISIBLE);
            holder.action.setOnClickListener(v -> {
                if (mOnClickEditWalletListener != null) {
                    mOnClickEditWalletListener.onClickEdit(mAddresses.get(holder.getAdapterPosition()));
                }
            });
            holder.itemView.setOnClickListener(v -> {
                if (mOnClickWalletListener != null) {
                    mOnClickWalletListener.onClickWallet(mAddresses.get(holder.getAdapterPosition()));
                }
            });
        }
    }

    public void setOnClickWalletListener(OnClickWalletListener listener) {
        mOnClickWalletListener = listener;
    }

    public void setOnClickAddWalletListener(OnClickAddWalletListener listener) {
        mOnClickAddWalletListener = listener;
    }

    public void setOnClickEditWalletListener(OnClickEditWalletListener listener) {
        mOnClickEditWalletListener = listener;
    }

    @Override
    public int getItemCount() {
        if (mAddresses.size() == 5) {
            return mAddresses.size();
        }
        return mAddresses.size() + 1;
    }

    public interface OnClickAddWalletListener {
        void onClickAddWallet();
    }

    public interface OnClickEditWalletListener {
        void onClickEdit(WalletItem address);
    }

    public interface OnClickWalletListener {
        void onClickWallet(WalletItem address);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.icon) TextView weight;
        @BindView(R.id.title) TextView name;
        @BindView(R.id.action) ImageView action;
        @BindView(R.id.icon_add) ImageView iconAdd;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
