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

package network.minter.bipwallet.settings.views.rows;

import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import network.minter.bipwallet.R;
import network.minter.bipwallet.internal.common.CallbackProvider;
import network.minter.bipwallet.internal.common.DeferredCall;
import network.minter.bipwallet.internal.views.list.multirow.MultiRowAdapter;
import network.minter.bipwallet.internal.views.list.multirow.MultiRowContract;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class SettingsSwitchRow implements MultiRowContract.Row<SettingsSwitchRow.ViewHolder> {

    private CharSequence mKey;
    private CallbackProvider<Boolean> mValue;
    private OnClickListener mListener;
    private DeferredCall<ViewHolder> mDefer = DeferredCall.createWithSize(1);

    public SettingsSwitchRow(CharSequence key, boolean value, OnClickListener listener) {
        mKey = key;
        mValue = () -> value;
        mListener = listener;
    }

    public SettingsSwitchRow(CharSequence key, CallbackProvider<Boolean> value, OnClickListener listener) {
        mKey = key;
        mValue = value;
        mListener = listener;
    }

    public SettingsSwitchRow setValue(CallbackProvider<Boolean> value, OnClickListener listener) {
        mValue = value;
        mListener = listener;
        mDefer.call(this::fill);

        return this;
    }

    public SettingsSwitchRow setValue(CallbackProvider<Boolean> value) {
        mValue = value;
        mDefer.call(this::fill);
        return this;
    }

    @Override
    public int getItemView() {
        return R.layout.row_item_switch_settings;
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
        fill(viewHolder);
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

    private void fill(ViewHolder vh) {
        if (mKey != null && mKey.length() > 0) {
            ViewCompat.setTransitionName(vh.key, "settings_field");
        }

        vh.key.setText(mKey);
        vh.value.setChecked(mValue.get());
        vh.value.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mListener != null) {
                    mListener.onClick(buttonView, isChecked);
                }
            }
        });
    }

    public interface OnClickListener {
        void onClick(View view, Boolean value);
    }

    public static class ViewHolder extends MultiRowAdapter.RowViewHolder {
        @BindView(R.id.item_key) TextView key;
        @BindView(R.id.item_value) Switch value;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
