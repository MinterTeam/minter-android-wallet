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

package network.minter.bipwallet.sending.account;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import network.minter.bipwallet.R;
import network.minter.bipwallet.advanced.models.AccountItem;
import network.minter.bipwallet.internal.views.widgets.BipCircleImageView;

/**
 * MinterWallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class AccountSelectedAdapter extends RecyclerView.Adapter<AccountSelectedAdapter.ViewHolder> {

    private List<AccountItem> mItems;
    private LayoutInflater mInflater;
    private OnClickListener mOnClickListener;

    public AccountSelectedAdapter(List<AccountItem> items) {
        mItems = items;
    }

    public void setOnClickListener(OnClickListener listener) {
        mOnClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (mInflater == null) {
            mInflater = LayoutInflater.from(parent.getContext());
        }

        View view = mInflater.inflate(R.layout.item_list_dialog_account_selector, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final AccountItem item = mItems.get(position);
        if (item.avatar != null) {
            holder.avatar.setImageUrl(item.avatar);
        } else {
            holder.avatar.setImageResource(R.drawable.img_avatar_default);
        }

        holder.title.setText(String.format("%s (%s)", item.coin.toUpperCase(), item.balance.toString()));
        holder.subtitle.setText(item.address.toShortString());

        holder.itemView.setOnClickListener(v -> {
            if (mOnClickListener != null) {
                mOnClickListener.onClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public interface OnClickListener {
        void onClick(AccountItem item);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.item_avatar) BipCircleImageView avatar;
        @BindView(R.id.item_title) TextView title;
        @BindView(R.id.item_subtitle) TextView subtitle;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
