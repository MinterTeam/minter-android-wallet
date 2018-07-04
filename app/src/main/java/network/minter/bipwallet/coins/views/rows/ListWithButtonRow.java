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

package network.minter.bipwallet.coins.views.rows;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.subjects.BehaviorSubject;
import network.minter.bipwallet.R;
import network.minter.bipwallet.internal.views.list.BorderedItemSeparator;
import network.minter.bipwallet.internal.views.list.NonScrollableLinearLayoutManager;
import network.minter.bipwallet.internal.views.list.multirow.MultiRowAdapter;
import network.minter.bipwallet.internal.views.list.multirow.MultiRowContract;

import static network.minter.bipwallet.coins.views.rows.ListWithButtonRow.Status.Progress;

/**
 * MinterWallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class ListWithButtonRow implements MultiRowContract.Row<ListWithButtonRow.ViewHolder> {
    private Builder mBuilder;
    private BehaviorSubject<Status> mCommand = BehaviorSubject.createDefault(Progress);
    private String mError;

    public enum Status {
        Normal,
        Progress,
        Empty,
        Error
    }

    private ListWithButtonRow(Builder builder) {
        mBuilder = builder;
        mCommand.onNext(mBuilder.mStatus);
    }

    @Override
    public int getItemView() {
        return R.layout.row_list_with_button;
    }

    @Override
    public int getRowPosition() {
        return 0;
    }

    @Override
    public boolean isVisible() {
        return true;
    }

    @Override
    public void onBindViewHolder(final @NonNull ViewHolder viewHolder) {
        mCommand.observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(command -> {
                    switchView(command, viewHolder);
                });

        if (mBuilder.mActionTitle != null) {
            viewHolder.action.setText(mBuilder.mActionTitle);
            viewHolder.action.setOnClickListener(mBuilder.mActionListener);
        }

        switchView(mCommand.getValue(), viewHolder);

        viewHolder.title.setText(mBuilder.mTitle);
        if (mBuilder.mEmptyListTitle != null) {
            viewHolder.emptyTitle.setText(mBuilder.mEmptyListTitle);
        } else {
            viewHolder.emptyTitle.setText("No elements was found");
        }

        viewHolder.action.setVisibility(View.VISIBLE);
        viewHolder.emptyTitle.setVisibility(View.GONE);
        viewHolder.list.setLayoutManager(new NonScrollableLinearLayoutManager(viewHolder.itemView.getContext()));
        viewHolder.list.addItemDecoration(new BorderedItemSeparator(viewHolder.itemView.getContext(), R.drawable.shape_bottom_separator, false, true));
        viewHolder.list.setAdapter(mBuilder.mAdapter);
        viewHolder.list.setNestedScrollingEnabled(false);
    }

    public void setStatus(Status command) {
        mCommand.onNext(command);
    }

    @Override
    public void onUnbindViewHolder(@NonNull ViewHolder viewHolder) {

    }

    @NonNull
    @Override
    public Class<ViewHolder> getViewHolderClass() {
        return ViewHolder.class;
    }

    public void setError(String errorMessage) {
        mError = errorMessage;
        mCommand.onNext(Status.Error);
    }

    private void switchView(Status command, ViewHolder viewHolder) {
        if (command == null) {
            command = Status.Normal;
        }
        switch (command) {
            case Progress:
                viewHolder.list.setVisibility(View.GONE);
                viewHolder.action.setVisibility(View.GONE);
                viewHolder.emptyTitle.setVisibility(View.GONE);
                viewHolder.progress.setVisibility(View.VISIBLE);
                break;
            case Empty:
                viewHolder.list.setVisibility(View.GONE);
                viewHolder.action.setVisibility(View.GONE);
                viewHolder.emptyTitle.setVisibility(View.VISIBLE);
                viewHolder.progress.setVisibility(View.GONE);
                break;
            case Error:
                viewHolder.list.setVisibility(View.GONE);
                viewHolder.action.setVisibility(View.GONE);
                viewHolder.emptyTitle.setVisibility(View.VISIBLE);
                viewHolder.emptyTitle.setText(mError == null ? "Unexpected error" : mError);
                viewHolder.progress.setVisibility(View.GONE);
            case Normal:
                viewHolder.list.setVisibility(View.VISIBLE);
                viewHolder.action.setVisibility(View.VISIBLE);
                viewHolder.emptyTitle.setVisibility(View.GONE);
                viewHolder.progress.setVisibility(View.GONE);
                break;
        }
    }

    public static class ViewHolder extends MultiRowAdapter.RowViewHolder {
        @BindView(R.id.title) TextView title;
        @BindView(R.id.action) Button action;
        @BindView(R.id.list) RecyclerView list;
        @BindView(R.id.emptyTitle) TextView emptyTitle;
        @BindView(R.id.progress) ProgressBar progress;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    public static final class Builder {
        private RecyclerView.Adapter<?> mAdapter;
        private CharSequence mTitle;
        private CharSequence mActionTitle;
        private View.OnClickListener mActionListener;
        private CharSequence mEmptyListTitle;
        private Status mStatus = Progress;

        public Builder(CharSequence title) {
            mTitle = title;
        }

        public Builder setEmptyTitle(CharSequence title) {
            mEmptyListTitle = title;
            return this;
        }

        public Builder setTitle(CharSequence title) {
            mTitle = title;
            return this;
        }

        public Builder setAdapter(RecyclerView.Adapter<?> adapter) {
            mAdapter = adapter;
            return this;
        }

        public Builder setAction(CharSequence name, View.OnClickListener listener) {
            mActionTitle = name;
            mActionListener = listener;
            return this;
        }

        public ListWithButtonRow build() {
            return new ListWithButtonRow(this);
        }
    }
}
