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

package network.minter.bipwallet.sending.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.MotionEvent;
import android.view.WindowManager;

import java.math.BigDecimal;

import network.minter.bipwallet.R;
import network.minter.bipwallet.internal.BaseInjectActivity;
import network.minter.bipwallet.internal.dialogs.DialogSequence;
import network.minter.bipwallet.internal.system.ActivityBuilder;
import network.minter.bipwallet.sending.dialogs.WalletTxStartDialog;
import network.minter.bipwallet.sending.dialogs.WalletTxSuccessDialog;
import network.minter.bipwallet.sending.dialogs.WalletTxWaitingDialog;
import network.minter.mintercore.crypto.MinterAddress;
import timber.log.Timber;

/**
 * MinterWallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class DialogTxSendActivity extends BaseInjectActivity {
    private final static String EXTRA_ADDRESS = "EXTRA_ADDRESS";
    private final static String EXTRA_AMOUNT = "EXTRA_AMOUNT";
    private final static String EXTRA_RECIPIENT = "EXTRA_RECIPIENT";
    private final static String EXTRA_AVATAR = "EXTRA_AVATAR";



    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Make us non-modal, so that others can receive touch events.
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);

        // ...but notify us that it happened.
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH, WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH);
        setContentView(R.layout.fragment_dialog_sequence);


        final MinterAddress address = new MinterAddress(getIntent().getCharSequenceExtra(EXTRA_ADDRESS));
        final BigDecimal amount = (BigDecimal) getIntent().getSerializableExtra(EXTRA_AMOUNT);
        final CharSequence recipient;
        if (getIntent().hasExtra(EXTRA_RECIPIENT)) {
            recipient = getIntent().getCharSequenceExtra(EXTRA_RECIPIENT);
        } else {
            recipient = address.toShortString();
        }
        final String avatar;
        if (getIntent().hasExtra(EXTRA_AVATAR)) {
            avatar = getIntent().getStringExtra(EXTRA_AVATAR);
        } else {
            avatar = "https://my.beta.minter.network/api/v1/avatar/by/user/1";
        }

        DialogSequence dialogSequence = new DialogSequence();

        final WalletTxStartDialog.Builder mainDialog = new WalletTxStartDialog.Builder(this, "You're sending")
                .setAmount(amount)
                .setAvatarUrl(avatar)
                .setRecipientName(recipient)
                .setPositiveAction("BIP!", null)
                .setNegativeAction("Cancel", null);

        final WalletTxWaitingDialog.Builder waitingDialog = new WalletTxWaitingDialog.Builder(this, "Please wait")
                .setCountdownSeconds(10, new WalletTxWaitingDialog.OnCountdownListener() {
                    @Override
                    public void onCountDown(long tick, boolean isEnd) {

                    }
                })
                .setPositiveAction("Express transaction", (d, v) -> {
                    Timber.d("EXPRESS TRANSACTION");
                });

        final WalletTxSuccessDialog.Builder successDialog = new WalletTxSuccessDialog.Builder(this, "Success!")
                .setPositiveAction("View transaction", (d, v) -> {
                    Timber.d("VIEW TRANSACTION");
                });

        dialogSequence.setMain(0, mainDialog, 1);
        dialogSequence.addDialog(1, waitingDialog, 2);
        dialogSequence.addDialog(2, waitingDialog, DialogSequence.NO_ACTION);

        dialogSequence.show();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // If we've received a touch notification that the user has touched
        // outside the app, finish the activity.
        if (MotionEvent.ACTION_OUTSIDE == event.getAction()) {
            finish();
            return true;
        }

        // Delegate everything else to Activity.
        return super.onTouchEvent(event);

    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public static class Builder extends ActivityBuilder {
        private CharSequence mAddress;
        private BigDecimal mAmount;
        private CharSequence mRecipient;
        private String mAvatarUrl;


        public Builder(@NonNull Activity from, CharSequence address, BigDecimal amount) {
            super(from);
            mAddress = address;
            mAmount = amount;
        }

        public Builder(@NonNull Fragment from, CharSequence address, BigDecimal amount) {
            super(from);
            mAddress = address;
            mAmount = amount;
        }

        @Override
        protected void onBeforeStart(Intent intent) {
            super.onBeforeStart(intent);

        }

        @Override
        protected Class<?> getActivityClass() {
            return DialogTxSendActivity.class;
        }
    }
}
