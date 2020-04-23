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

package network.minter.bipwallet.addressbook.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import network.minter.bipwallet.R;
import network.minter.bipwallet.addressbook.models.AddressBookItem;
import network.minter.bipwallet.addressbook.models.AddressBookItemHeader;
import network.minter.bipwallet.addressbook.models.AddressContact;
import network.minter.bipwallet.internal.views.list.diff.DiffUtilDispatcher;
import network.minter.bipwallet.internal.views.list.diff.DiffUtilDispatcherDelegate;
import network.minter.bipwallet.internal.views.widgets.BipCircleImageView;

public class AddressBookAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements DiffUtilDispatcherDelegate<AddressBookItem> {
    private List<AddressBookItem> mItems = new ArrayList<>();
    private LayoutInflater mInflater;
    private OnEditContactListener mOnEditContactListener;
    private OnDeleteContactListener mOnDeleteContactListener;
    private OnItemClickListener mOnItemClickListener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (mInflater == null) {
            mInflater = LayoutInflater.from(parent.getContext());
        }

        final View view;
        if (viewType == AddressBookItem.TYPE_HEADER) {
            view = mInflater.inflate(R.layout.item_list_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            view = mInflater.inflate(R.layout.item_list_address_book, parent, false);
            return new ItemViewHolder(view);
        }
    }

    public void setOnEditContactListener(OnEditContactListener listener) {
        mOnEditContactListener = listener;
    }

    public void setOnDeleteContactListener(OnDeleteContactListener listener) {
        mOnDeleteContactListener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return mItems.get(position).getViewType();
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            HeaderViewHolder vh = ((HeaderViewHolder) holder);
            final AddressBookItemHeader item = ((AddressBookItemHeader) mItems.get(position));
            vh.title.setText(item.header);
        } else {
            ItemViewHolder vh = ((ItemViewHolder) holder);
            AddressContact item = ((AddressContact) mItems.get(position));

            vh.mainView.setOnClickListener(v -> {
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onClick(((AddressContact) mItems.get(holder.getAdapterPosition())));
                }
            });

            item.applyAddressIcon(vh.avatar);
            vh.title.setText(item.name);
            vh.subtitle.setText(item.getShortAddress());
            vh.actionEdit.setOnClickListener(v -> {
                if (mOnEditContactListener != null) {
                    mOnEditContactListener.onEditContact(((AddressContact) mItems.get(holder.getAdapterPosition())));
                }
            });
            vh.actionDelete.setOnClickListener(v -> {
                if (mOnDeleteContactListener != null) {
                    mOnDeleteContactListener.onDeleteContact(((AddressContact) mItems.get(holder.getAdapterPosition())));
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @Override
    public <T extends DiffUtil.Callback> void dispatchChanges(Class<T> diffUtilCallbackCls, @NonNull List<AddressBookItem> items, boolean detectMoves) {
        DiffUtilDispatcher.dispatchChanges(this, diffUtilCallbackCls, items, detectMoves);
    }

    @Override
    public <T extends DiffUtil.Callback> void dispatchChanges(Class<T> diffUtilCallbackCls, @NonNull List<AddressBookItem> items) {
        DiffUtilDispatcher.dispatchChanges(this, diffUtilCallbackCls, items);
    }

    @Override
    public List<AddressBookItem> getItems() {
        return mItems;
    }

    @Override
    public void setItems(List<AddressBookItem> items) {
        mItems = items;
    }

    @Override
    public void clear() {
        mItems.clear();
        notifyDataSetChanged();
    }

    public interface OnDeleteContactListener {
        void onDeleteContact(AddressContact contact);
    }

    public interface OnEditContactListener {
        void onEditContact(AddressContact contact);
    }

    public interface OnItemClickListener {
        void onClick(AddressContact contact);
    }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.title) TextView title;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.avatar) BipCircleImageView avatar;
        @BindView(R.id.title) TextView title;
        @BindView(R.id.subtitle) TextView subtitle;
        @BindView(R.id.swipe_actions) View swipeActions;
        @BindView(R.id.action_delete) View actionDelete;
        @BindView(R.id.action_edit) View actionEdit;
        @BindView(R.id.main) View mainView;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }


}
