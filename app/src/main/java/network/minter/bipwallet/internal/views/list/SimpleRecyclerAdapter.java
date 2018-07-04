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

package network.minter.bipwallet.internal.views.list;

import android.support.annotation.CheckResult;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.annimon.stream.Stream;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import network.minter.bipwallet.internal.helpers.data.CollectionsHelper;
import timber.log.Timber;

import static network.minter.bipwallet.internal.common.Preconditions.checkNotNull;


/**
 * Dogsy. 2017
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */

public class SimpleRecyclerAdapter<Data, VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {

    private Builder<Data, VH> mBuilder;

    private SimpleRecyclerAdapter(Builder<Data, VH> builder) {
        mBuilder = builder;
    }

    public List<Data> getItems() {
        return mBuilder.data;
    }

    public void setItems(List<Data> items) {
        mBuilder.data = items;
    }

    public void setItems(Stream<Data> stream) {
        setItems(stream.toList());
    }

    public void setItems(Collection<Data> collection) {
        setItems(new ArrayList<>(collection));
    }

    public void setItems(Data[] items) {
        setItems(CollectionsHelper.asList(items));
    }

    /**
     * @param diffUtilCallbackCls Class must contain constructor with 2 lists (old and new)
     * @param items
     * @param <T>
     */
    public <T extends DiffUtil.Callback> void dispatchChanges(Class<T> diffUtilCallbackCls, @NonNull List<Data> items, boolean detectMoves) {
        checkNotNull(items, "Data can't be null. Provide empty list if you have no data");

        if (getItemCount() == 0 && !items.isEmpty()) {
            setItems(items);
            notifyItemRangeInserted(0, items.size());
            return;
        }

        final List<Data> old = getItems();
        setItems(items);
        DiffUtil.Callback cb;

        try {
            cb = diffUtilCallbackCls.getDeclaredConstructor(List.class, List.class).newInstance(old, items);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(cb, detectMoves);
        diffResult.dispatchUpdatesTo(this);
    }

    /**
     * @param diffUtilCallbackCls Object must contains construct with 2 lists (old and new)
     * @param items
     * @param <T>
     */
    public <T extends DiffUtil.Callback> void dispatchChanges(Class<T> diffUtilCallbackCls, @NonNull List<Data> items) {
        dispatchChanges(diffUtilCallbackCls, items, false);
    }

    public void addItem(Data item) {
        mBuilder.data.add(item);
    }

    public void removeItem(Data item) {
        mBuilder.data.remove(item);
    }

    public void clear() {
        mBuilder.data.clear();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return mBuilder.creator.createHolder(LayoutInflater.from(parent.getContext()), parent);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        final Data item = mBuilder.data.get(position);
        if (mBuilder.binder != null) {
            mBuilder.binder.onBind(holder, item, position);
        }

        if (mBuilder.clickListener != null) {
            holder.itemView.setOnClickListener(v -> {
                mBuilder.clickListener.onClick(v, item, position);
            });

        }
    }

    @Override
    public int getItemCount() {
        return mBuilder.data.size();
    }


    public interface Binder<Data, VH extends RecyclerView.ViewHolder> {
        void onBind(VH vh, Data item, int position);
    }

    public interface Creator<VH extends RecyclerView.ViewHolder> {
        VH createHolder(LayoutInflater inflater, ViewGroup parent);
    }

    public interface ItemClickListener<Data> {
        void onClick(View view, Data item, int position);
    }

    public final static class Builder<Data, VH extends RecyclerView.ViewHolder> {
        private Binder<Data, VH> binder = null;
        private Creator<VH> creator;
        private List<Data> data = new ArrayList<>();

        private ItemClickListener<Data> clickListener;

        public Builder() {
        }

        public Builder(List<Data> data) {
            this.data = data;
        }

        public Builder(Stream<Data> stream) {
            this.data = stream.toList();
        }

        public Builder(Data[] data) {
            this.data = CollectionsHelper.asList(data);
        }

        @CheckResult(suggest = "build()")
        public Builder<Data, VH> setClickListener(ItemClickListener<Data> listener) {
            clickListener = listener;
            return this;
        }

        @CheckResult(suggest = "build()")
        public Builder<Data, VH> setCreator(final @LayoutRes int layoutItemId,
                                            final Class<? extends VH> holder) {
            creator = (inflater, parent) -> {
                final View view = inflater.inflate(layoutItemId, parent, false);
                VH item = null;
                try {
                    item = holder.getDeclaredConstructor(View.class).newInstance(view);
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    Timber.e(e);
                }
                return item;
            };
            return this;
        }

        @CheckResult(suggest = "build()")
        public Builder<Data, VH> setBinder(Binder<Data, VH> binder) {
            this.binder = binder;
            return this;
        }

        @CheckResult(suggest = "build()")
        public Builder<Data, VH> setCreator(Creator<VH> creator) {
            this.creator = creator;
            return this;
        }

        public SimpleRecyclerAdapter<Data, VH> build() {
            checkNotNull(creator, "View holder creator required");
            return new SimpleRecyclerAdapter<>(this);
        }

    }
}
