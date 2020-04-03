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

package network.minter.bipwallet.tx.adapters.vh;

import android.annotation.SuppressLint;
import android.util.Base64;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import java.util.Locale;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import network.minter.bipwallet.R;
import network.minter.bipwallet.internal.helpers.ContextHelper;
import network.minter.bipwallet.internal.views.widgets.BipCircleImageView;
import network.minter.bipwallet.tx.adapters.TxItem;
import timber.log.Timber;

/**
 * minter-android-wallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class ExpandableTxViewHolder extends RecyclerView.ViewHolder {
    public @BindView(R.id.item_avatar) BipCircleImageView avatar;
    public @BindView(R.id.item_title) TextView title;
    public @BindView(R.id.item_amount) TextView amount;
    public @BindView(R.id.item_title_type) TextView type;
    public @BindView(R.id.item_subamount) TextView subamount;
    public @BindView(R.id.detail_date_value) TextView dateValue;
    public @BindView(R.id.detail_time_value) TextView timeValue;
    public @BindView(R.id.action) Button action;
    public @BindView(R.id.layout_details) ConstraintLayout detailsLayout;
    @BindView(R.id.detail_payload_label) public TextView payloadLabel;
    @BindView(R.id.detail_payload_value) public TextView payload;

    private boolean mEnableExpanding = true;
    private boolean mUseAvatars = true;

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
        final DateTime dt = new DateTime(item.getTx().get().timestamp);
        dateValue.setText(dt.toString(DateTimeFormat.forPattern("dd MMMM yyyy").withLocale(Locale.US)));
        timeValue.setText(dt.toString(DateTimeFormat.forPattern("HH:mm:ss z")));
        type.setText(item.getTx().getType().name());

        if (item.getTx().get().getPayload() != null && !item.getTx().get().getPayload().isEmpty()) {
            String decPayload;
            try {
                decPayload = new String(Base64.decode(item.getTx().get().getPayload(), Base64.DEFAULT));
                payload.setText(decPayload);
                payload.setTextIsSelectable(true);
                payload.setVisibility(View.VISIBLE);
                payloadLabel.setVisibility(View.VISIBLE);
            } catch (Throwable t) {
                Timber.w(t, "Unable to decode payload data");
                payload.setVisibility(View.GONE);
                payloadLabel.setVisibility(View.GONE);
            }
        } else {
            payload.setVisibility(View.GONE);
            payloadLabel.setVisibility(View.GONE);
        }
    }

    protected void setupAvatar(TxItem item) {
        if (mUseAvatars) {
            if (autoSetAvatar()) {
                avatar.setImageUrl(item.getAvatar(), R.dimen.tx_item_avatar_size);
            }
        } else {
            avatar.setImageDrawable(null);
        }
    }

    public void setUserAvatars(boolean useAvatars) {
        mUseAvatars = useAvatars;
    }

    @SuppressLint("ClickableViewAccessibility")
    void setupCopyListeners(TextView... views) {
        int selectedColor = ContextCompat.getColor(itemView.getContext(), R.color.textColorSelectedAddress);
        int defaultColor = ContextCompat.getColor(itemView.getContext(), R.color.colorPrimary);
        for (TextView view : views) {
            view.setTextColor(defaultColor);
            view.setOnLongClickListener(v -> {
                view.setTextColor(selectedColor);
                return true;
            });

            view.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                    view.setTextColor(defaultColor);
                    ContextHelper.copyToClipboard(view.getContext(), view.getText().toString());
                }
                return false;
            });
        }
    }

    protected boolean autoSetAvatar() {
        return true;
    }
}
