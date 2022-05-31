/*
 * Copyright (C) by MinterTeam. 2022
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

package network.minter.bipwallet.internal.views.list;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import androidx.annotation.CheckResult;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import network.minter.bipwallet.internal.helpers.data.CollectionsHelper;
import network.minter.bipwallet.internal.views.list.diff.DiffUtilDispatcher;
import network.minter.bipwallet.internal.views.list.diff.DiffUtilDispatcherDelegate;
import timber.log.Timber;

import static network.minter.bipwallet.internal.common.Preconditions.checkNotNull;


/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */

public class SimpleRecyclerAdapter<Data, VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> implements DiffUtilDispatcherDelegate<Data> {

    private final Builder<Data, VH> mBuilder;

    private SimpleRecyclerAdapter(Builder<Data, VH> builder) {
        mBuilder = builder;
    }

    public List<Data> getItems() {
        return mBuilder.data;
    }

    public void setItems(Data[] items) {
        setItems(CollectionsHelper.asList(items));
    }

    @Override
    public <T extends DiffUtil.Callback> void dispatchChanges(Class<T> diffUtilCallbackCls, @NonNull List<Data> items, boolean detectMoves) {
        DiffUtilDispatcher.dispatchChanges(this, diffUtilCallbackCls, items, detectMoves);
    }

    @Override
    public <T extends DiffUtil.Callback> void dispatchChanges(Class<T> diffUtilCallbackCls, @NonNull List<Data> items) {
        DiffUtilDispatcher.dispatchChanges(this, diffUtilCallbackCls, items);
    }

    public void setItems(List<Data> items) {
        mBuilder.data = items;
    }

    public void setItems(Collection<Data> collection) {
        setItems(new ArrayList<>(collection));
    }

    public void addItem(Data item) {
        mBuilder.data.add(item);
    }

    public void removeItem(Data item) {
        mBuilder.data.remove(item);
    }

    public void clearNotify() {
        int sz = mBuilder.data.size();
        mBuilder.data.clear();
        if (sz > 0) {
            notifyItemRangeRemoved(0, sz);
        }
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
