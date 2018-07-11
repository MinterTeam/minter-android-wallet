/*******************************************************************************
 * Copyright (C) by MinterTeam. 2018
 * @link https://github.com/MinterTeam
 * @link https://github.com/edwardstock
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
 ******************************************************************************/

package network.minter.bipwallet.tx.adapters.vh;

import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import butterknife.BindView;
import butterknife.ButterKnife;
import network.minter.bipwallet.R;
import network.minter.bipwallet.internal.views.widgets.BipCircleImageView;
import network.minter.bipwallet.tx.adapters.TxItem;

/**
 * minter-android-wallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class ExpandableTxViewHolder extends RecyclerView.ViewHolder {
    public @BindView(R.id.item_avatar) BipCircleImageView avatar;
    public @BindView(R.id.item_title) TextView title;
    public @BindView(R.id.item_amount) TextView amount;
    public @BindView(R.id.item_subamount) TextView subamount;
    public @BindView(R.id.detail_date_value) TextView dateValue;
    public @BindView(R.id.detail_time_value) TextView timeValue;
    public @BindView(R.id.action) Button action;
    public @BindView(R.id.layout_details) ConstraintLayout detailsLayout;

    private boolean mEnableExpanding = true;

    public ExpandableTxViewHolder(View itemView, boolean enableExpanding) {
        super(itemView);
        mEnableExpanding = enableExpanding;
        ButterKnife.bind(this, itemView);
    }

    public ExpandableTxViewHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }

    public final boolean isEnableExpanding() {
        return mEnableExpanding;
    }

    public final void setEnableExpanding(boolean enableExpanding) {
        mEnableExpanding = enableExpanding;
    }

    public void bind(TxItem item) {
        if (autoSetAvatar()) {
            avatar.setImageUrl(item.getAvatar());
        }

        final DateTime dt = new DateTime(item.getTx().timestamp);
        dateValue.setText(dt.toString(DateTimeFormat.forPattern("EEEE, dd MMMM")));
        timeValue.setText(dt.toString(DateTimeFormat.forPattern("HH:mm:ssZ")));
    }

    protected boolean autoSetAvatar() {
        return true;
    }
}
