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

package network.minter.bipwallet.internal.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import network.minter.bipwallet.R;
import network.minter.bipwallet.internal.ReactiveAdapter;
import network.minter.bipwallet.internal.helpers.ExceptionHelper;
import network.minter.mintercore.internal.exceptions.NetworkException;
import network.minter.my.models.MyResult;
import retrofit2.HttpException;

import static network.minter.bipwallet.internal.common.Preconditions.checkNotNull;

/**
 * MinterWallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public final class WalletConfirmDialog extends WalletDialog {

    private final Builder mBuilder;
    @BindView(R.id.dialog_text) TextView text;
    @BindView(R.id.action_confirm) Button actionConfirm;
    @BindView(R.id.action_decline) Button actionDecline;

    public WalletConfirmDialog(@NonNull Context context, Builder builder) {
        super(context);
        mBuilder = builder;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wallet_confirm_dialog);
        ButterKnife.bind(this);
        title.setText(mBuilder.mTitle);
        text.setText(mBuilder.mText);
        actionConfirm.setText(mBuilder.getPositiveTitle());
        actionConfirm.setOnClickListener(v -> {
            if (mBuilder.hasPositiveListener()) {
                mBuilder.getPositiveListener().onClick(WalletConfirmDialog.this, BUTTON_POSITIVE);
            } else {
                dismiss();
            }
        });

        if (mBuilder.mNegativeTitle != null) {
            actionDecline.setText(mBuilder.getNegativeTitle());
            actionDecline.setOnClickListener(v -> {
                if (mBuilder.hasNegativeListener()) {
                    mBuilder.getNegativeListener().onClick(WalletConfirmDialog.this, BUTTON_NEGATIVE);
                } else {
                    dismiss();
                }
            });
        } else {
            actionDecline.setVisibility(View.GONE);
        }
    }

    public static final class Builder extends WalletDialogBuilder<WalletConfirmDialog, WalletConfirmDialog.Builder> {
        private CharSequence mText;

        public Builder(Context context, CharSequence title) {
            super(context, title);
        }

        @Override
        public WalletConfirmDialog create() {
            checkNotNull(mPositiveTitle, "At least, positive action title should be set");
            return new WalletConfirmDialog(mContext, this);
        }

        public Builder setText(CharSequence text) {
            mText = text;
            return this;
        }

        public Builder setText(String text, Object... args) {
            mText = String.format(text, args);
            return this;
        }

        public Builder setText(Throwable t) {
            if (t instanceof HttpException) {
                if (((HttpException) t).code() >= 500 && ((HttpException) t).code() < 1000) {
                    setTitle(mTitle + " (server error " + String.valueOf(((HttpException) t).code()) + ")");
                } else if (((HttpException) t).code() < 500 && ((HttpException) t).code() > 0) {
                    setTitle(mTitle + " (client error " + String.valueOf(((HttpException) t).code()) + ")");
                } else {
                    setTitle(mTitle + " (network error " + String.valueOf(((HttpException) t).code()) + ")");
                }
                try {
                    String out = ((HttpException) t).response().errorBody().string() + "\n";
                    MyResult errorResult = ReactiveAdapter.createMyErrorResult(((HttpException) t));
                    out += errorResult.getError().message + "\n" + errorResult.getError().message + "\n" + ExceptionHelper.getStackTrace(t);
                    mText = out;
                } catch (IOException e) {
                    e.printStackTrace();
                    mText = ((HttpException) t).message() + "\n" + ExceptionHelper.getStackTrace(t);
                }

            } else if (t instanceof NetworkException) {
                final int statusCode = ((NetworkException) t).getStatusCode();
                if (statusCode >= 500 && statusCode < 1000) {
                    setTitle(mTitle + " (server error " + String.valueOf(statusCode) + ")");
                } else if (statusCode < 500 && statusCode > 0) {
                    setTitle(mTitle + " (client error " + String.valueOf(statusCode) + ")");
                } else {
                    setTitle(mTitle + " (network error " + String.valueOf(statusCode) + ")");
                }
                mText = t.getMessage();

            } else {
                mText = t.getMessage() + "\n" + ExceptionHelper.getStackTrace(t);
            }

            return this;
        }

        public Builder setPositiveAction(CharSequence title) {
            return setPositiveAction(title, null);
        }

        public Builder setNegativeAction(CharSequence title) {
            return setNegativeAction(title, null);
        }

        public Builder setNegativeAction(CharSequence title, Dialog.OnClickListener listener) {
            return super.setAction(BUTTON_NEGATIVE, title, listener);
        }

        public Builder setPositiveAction(CharSequence title, OnClickListener listener) {
            return super.setAction(BUTTON_POSITIVE, title, listener);
        }
    }


}
