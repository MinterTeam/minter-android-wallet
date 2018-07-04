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

package network.minter.bipwallet.settings.views.rows;

import android.support.annotation.NonNull;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import butterknife.BindView;
import butterknife.ButterKnife;
import network.minter.bipwallet.R;
import network.minter.bipwallet.internal.common.CallbackProvider;
import network.minter.bipwallet.internal.common.DeferredCall;
import network.minter.bipwallet.internal.views.list.multirow.MultiRowAdapter;
import network.minter.bipwallet.internal.views.list.multirow.MultiRowContract;
import network.minter.bipwallet.internal.views.widgets.BipCircleImageView;
import network.minter.my.models.User;

/**
 * MinterWallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class ChangeAvatarRow implements MultiRowContract.Row<ChangeAvatarRow.ViewHolder> {
    private CallbackProvider<User.Avatar> mAvatar;
    private View.OnClickListener mListener;
    private DeferredCall<ViewHolder> mDefer = DeferredCall.createWithSize(1);

    public ChangeAvatarRow(CallbackProvider<User.Avatar> avatar, View.OnClickListener listener) {
        mAvatar = avatar;
        mListener = listener;
    }

    @Override
    public int getItemView() {
        return R.layout.row_settings_avatar;
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
    public void onBindViewHolder(@NonNull ViewHolder viewHolder) {
        if (mAvatar != null && mAvatar.get() != null) {
            viewHolder.avatar.setImageUrl(mAvatar.get().getUrl());
        }

        viewHolder.action.setOnClickListener(mListener);

        mDefer.attach(viewHolder);
    }

    @Override
    public void onUnbindViewHolder(@NonNull ViewHolder viewHolder) {
        mDefer.detach();
    }

    @NonNull
    @Override
    public Class<ViewHolder> getViewHolderClass() {
        return ViewHolder.class;
    }

    public void setAvatar(CallbackProvider<User.Avatar> avatar) {
        mAvatar = avatar;

        if (mAvatar != null) {
            mDefer.call(ctx -> ctx.avatar.setImageUrl(mAvatar.get().src));
        }
    }

    public void showProgress() {
        mDefer.call(ctx -> {
            ctx.progress.setVisibility(View.VISIBLE);
        });
    }

    public void hideProgress() {
        mDefer.call(ctx -> {
            ctx.progress.setVisibility(View.GONE);
        });
    }

    public static class ViewHolder extends MultiRowAdapter.RowViewHolder {
        @BindView(R.id.userAvatar) BipCircleImageView avatar;
        @BindView(R.id.action) Button action;
        @BindView(R.id.progress) ProgressBar progress;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
