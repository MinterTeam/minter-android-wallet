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

package network.minter.bipwallet.wallets.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import dagger.android.support.AndroidSupportInjection;
import network.minter.bipwallet.R;
import network.minter.bipwallet.internal.BaseFragment;
import network.minter.bipwallet.internal.views.list.NonScrollableLinearLayoutManager;
import network.minter.bipwallet.internal.views.widgets.BipCircleImageView;
import network.minter.bipwallet.internal.views.widgets.ColoredProgressBar;
import network.minter.bipwallet.wallets.contract.BaseWalletsPageView;

public abstract class BaseTabPageFragment extends BaseFragment implements BaseWalletsPageView {
    @BindView(R.id.title) TextView title;
    @BindView(R.id.list) RecyclerView list;
    @BindView(R.id.action) Button action;
    @BindView(R.id.empty_title) TextView emptyTitle;
    @BindView(R.id.progress) ColoredProgressBar progress;
    private Unbinder mUnbinder;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidSupportInjection.inject(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        AndroidSupportInjection.inject(this);
        super.onAttach(context);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.row_list_with_button, container, false);
        mUnbinder = ButterKnife.bind(this, view);

        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        list.setLayoutManager(new NonScrollableLinearLayoutManager(getContext()));
        list.setNestedScrollingEnabled(false);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mUnbinder.unbind();
        mUnbinder = null;
    }

    @Override
    public void setViewStatus(ViewStatus status) {
        setViewStatus(status, null);
    }

    @Override
    public void setViewStatus(ViewStatus status, CharSequence error) {
        switch (status) {
            case Progress:
                list.setVisibility(View.GONE);
                action.setVisibility(View.GONE);
                emptyTitle.setVisibility(View.GONE);
                progress.setVisibility(View.VISIBLE);
                break;
            case Empty:
                list.setVisibility(View.GONE);
                action.setVisibility(View.GONE);
                emptyTitle.setVisibility(View.VISIBLE);
                progress.setVisibility(View.GONE);
                break;
            case Error:
                list.setVisibility(View.GONE);
                action.setVisibility(View.GONE);
                emptyTitle.setVisibility(View.VISIBLE);
                emptyTitle.setText(error == null ? "Unexpected error" : error);
                progress.setVisibility(View.GONE);
                break;
            case Normal:
                list.setVisibility(View.VISIBLE);
                action.setVisibility(View.VISIBLE);
                emptyTitle.setVisibility(View.GONE);
                progress.setVisibility(View.GONE);
                break;
        }
    }

    @Override
    public void setListTitle(CharSequence title) {
        this.title.setText(title);
    }

    @Override
    public void setListTitle(int title) {
        this.title.setText(title);
    }

    @Override
    public void setAdapter(RecyclerView.Adapter<?> adapter) {
        list.setAdapter(adapter);
    }

    @Override
    public void setActionTitle(CharSequence title) {
        action.setText(title);
    }

    @Override
    public void setActionTitle(int title) {
        action.setText(title);
    }

    @Override
    public void setOnActionClickListener(View.OnClickListener listener) {
        action.setOnClickListener(listener);
    }

    @Override
    public void showProgress(boolean show) {
        progress.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void setEmptyTitle(CharSequence title) {
        emptyTitle.setText(title);
    }

    @Override
    public void setEmptyTitle(int title) {
        emptyTitle.setText(title);
    }

    @Override
    public void showEmpty(boolean show) {
        emptyTitle.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    public final static class ItemViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.item_avatar)
        public BipCircleImageView avatar;
        @BindView(R.id.item_title)
        public TextView title;
        @BindView(R.id.item_amount)
        public TextView amount;
        @BindView(R.id.item_subamount)
        public TextView subname;

        public ItemViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
