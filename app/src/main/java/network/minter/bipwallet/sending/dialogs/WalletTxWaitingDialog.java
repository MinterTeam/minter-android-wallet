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

package network.minter.bipwallet.sending.dialogs;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.widget.Button;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import network.minter.bipwallet.R;
import network.minter.bipwallet.internal.dialogs.WalletDialog;
import network.minter.bipwallet.internal.dialogs.WalletDialogBuilder;
import network.minter.bipwallet.internal.helpers.Plurals;
import network.minter.bipwallet.internal.system.TickHandler;

/**
 * MinterWallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class WalletTxWaitingDialog extends WalletDialog implements TickHandler.TickListener {

    private final Builder mBuilder;
    @BindView(R.id.tx_countdown) TextView countdown;
    @BindView(R.id.tx_fee) TextView fee;
    @BindView(R.id.action_express) Button actionExpress;
    private TickHandler mTickHandler;

    public WalletTxWaitingDialog(@NonNull Context context, Builder builder) {
        super(context);
        mBuilder = builder;
    }

    @Override
    public void dismiss() {
        super.dismiss();
        mTickHandler.stop();
    }

    @Override
    public void onTickEnd() {
        if (mBuilder.mCountdownListener != null) {
            mBuilder.mCountdownListener.onCountDown(mBuilder.mCountdownSeconds, true);
        }
    }

    @Override
    public void onTick(long time) {
        countdown.setText(Plurals.countdown(mBuilder.mCountdownSeconds - time));
        if (mBuilder.mCountdownListener != null) {
            mBuilder.mCountdownListener.onCountDown(time, false);
        }
    }

    @Override
    public void onTickStop(long time) {
        countdown.setText(Plurals.countdown(mBuilder.mCountdownSeconds - time));
        if (mBuilder.mCountdownListener != null) {
            mBuilder.mCountdownListener.onCountDown(time, false);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wallet_tx_send_wait_dialog);
        ButterKnife.bind(this);
        title.setText(mBuilder.getTitle());
        countdown.setText(Plurals.countdown(mBuilder.mCountdownSeconds));
        actionExpress.setOnClickListener(v -> {
            mTickHandler.stop();
            if (mBuilder.hasPositiveListener()) {
                mBuilder.getPositiveListener().onClick(WalletTxWaitingDialog.this, BUTTON_POSITIVE);
            }
        });

        mTickHandler = new TickHandler(0, mBuilder.mCountdownSeconds);
        mTickHandler.addTickListener(this);
        mTickHandler.start();
    }

    public interface OnCountdownListener {
        void onCountDown(long tick, boolean isEnd);
    }

    public static class Builder extends WalletDialogBuilder<WalletTxWaitingDialog, Builder> {
        private long mCountdownSeconds = 60L;
        private OnCountdownListener mCountdownListener;
        private CharSequence mFee; //todo ?

        public Builder(Context context, CharSequence title) {
            super(context, title);
        }

        public Builder setPositiveAction(CharSequence title, OnClickListener listener) {
            return super.setAction(BUTTON_POSITIVE, title, listener);
        }

        public Builder setCountdownSeconds(long seconds) {
            return setCountdownSeconds(seconds, null);
        }

        public Builder setCountdownSeconds(long seconds, OnCountdownListener listener) {
            mCountdownSeconds = seconds;
            mCountdownListener = listener;
            return this;
        }

        public WalletTxWaitingDialog create() {
            return new WalletTxWaitingDialog(mContext, this);
        }
    }


}
