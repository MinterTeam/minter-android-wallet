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

package network.minter.bipwallet.sending.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import network.minter.bipwallet.R;
import network.minter.bipwallet.sending.models.RecipientItem;
import timber.log.Timber;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class RecipientListAdapter extends ArrayAdapter<RecipientItem> implements Filterable {
    private List<RecipientItem> mItems;
    private List<RecipientItem> mItemsAll;
    private List<RecipientItem> mSuggestions;
    private int mViewResourceId;
    private LayoutInflater mInflater;
    private OnItemClickListener mOnItemClickListener;

    public RecipientListAdapter(@NonNull Context context, @NonNull List<RecipientItem> items) {
        super(context, R.layout.search_item, items);
        setItems(items);
        mViewResourceId = R.layout.search_item;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }

    @NonNull
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        View v = convertView;
        ViewHolder holder;
        if (mInflater == null) {
            mInflater = LayoutInflater.from(parent.getContext());
        }

        if (v == null) {
            v = mInflater.inflate(mViewResourceId, null);
            holder = new ViewHolder(v);
            v.setTag(holder);
        } else {
            holder = ((ViewHolder) v.getTag());
        }

        onBindViewHolder(holder, position);
        return v;
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            public String convertResultToString(Object resultValue) {
                return ((RecipientItem) (resultValue)).getName();
            }

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                if (constraint != null) {
                    mSuggestions.clear();
                    for (RecipientItem item : mItemsAll) {
                        if (item.getName() != null && item.getName().toLowerCase().startsWith(constraint.toString().toLowerCase())) {
                            mSuggestions.add(item);
                        } else if (item.getAddress() != null && item.getAddress().toLowerCase().startsWith(constraint.toString().toLowerCase())) {
                            mSuggestions.add(item);
                        }
                    }
                    FilterResults filterResults = new FilterResults();
                    filterResults.values = mSuggestions;
                    filterResults.count = mSuggestions.size();
                    return filterResults;
                } else {
                    return new FilterResults();
                }
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                //noinspection unchecked
                List<RecipientItem> filteredList = (List<RecipientItem>) results.values;
                //noinspection ConstantConditions
                if (results != null && results.count > 0) {
                    Timber.d("Add filter item (items: %d)", results.count);
                    clear();
                    for (RecipientItem c : filteredList) {
                        add(c);
                    }
                    notifyDataSetChanged();
                }
            }
        };
    }

    public void setItems(List<RecipientItem> items) {
        mItems = items;
        //noinspection unchecked
        mItemsAll = (ArrayList<RecipientItem>) ((ArrayList<RecipientItem>) mItems).clone();
        mSuggestions = new ArrayList<>();
    }

    private void onBindViewHolder(ViewHolder vh, int position) {
        if (mOnItemClickListener != null) {
            vh.itemView.setOnClickListener(vv -> mOnItemClickListener.onClick(mItems.get(position), position));
        }

        RecipientItem item = mItems.get(position);

        if (item.getName() == null) {
            vh.title.setText(item.getAddress());
            vh.subtitle.setVisibility(View.GONE);
        } else {
            vh.title.setText(item.getName());
            vh.subtitle.setVisibility(View.VISIBLE);
            vh.subtitle.setText(item.getAddress());
        }
    }

    public interface OnItemClickListener {
        void onClick(RecipientItem item, int position);
    }

    static class ViewHolder {
        View itemView;
        @BindView(R.id.search_item_title) TextView title;
        @BindView(R.id.search_item_subtitle) TextView subtitle;

        public ViewHolder(View itemView) {
            this.itemView = itemView;
            ButterKnife.bind(this, itemView);
        }
    }
}
