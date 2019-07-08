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

package network.minter.bipwallet.sending.account;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import network.minter.bipwallet.R;
import network.minter.bipwallet.advanced.models.AccountItem;
import network.minter.bipwallet.internal.views.widgets.BipCircleImageView;

import static network.minter.bipwallet.internal.helpers.MathHelper.bdHuman;

/**
 * minter-android-wallet. 2018
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
        holder.avatar.setImageUrlFallback(item.getAvatar(), R.drawable.img_avatar_default);
        holder.title.setText(String.format("%s (%s)", item.coin.toUpperCase(), bdHuman(item.balance)));
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
