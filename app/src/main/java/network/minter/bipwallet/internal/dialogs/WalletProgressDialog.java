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

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.widget.TextView;

import com.pnikosis.materialishprogress.ProgressWheel;

import butterknife.BindView;
import butterknife.ButterKnife;
import network.minter.bipwallet.R;

/**
 * MinterWallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class WalletProgressDialog extends WalletDialog {
    @BindView(R.id.dialog_text) TextView text;
    @BindView(R.id.progress) ProgressWheel progress;
    private Builder mBuilder;
    private int mMaxProgress = 100;

    protected WalletProgressDialog(@NonNull Context context, Builder builder) {
        super(context);
        mBuilder = builder;
    }

    public void setMax(int max) {
        mMaxProgress = max;
    }

    public void setIndeterminate(boolean indeterminate) {
        if (indeterminate) {
            progress.spin();
        } else {
            setProgress(0);
        }
    }

    public void setProgress(int current) {
        if (current == 0) {
            current = 1;
        }
        progress.setProgress((float) current / (float) mMaxProgress);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wallet_progress_dialog);
        ButterKnife.bind(this);
        progress.spin();
        title.setText(mBuilder.mTitle);
        if (mBuilder.mText == null) {
            text.setText(R.string.please_wait);
        } else {
            text.setText(mBuilder.mText);
        }
    }

    public static final class Builder extends WalletDialogBuilder<WalletProgressDialog, Builder> {
        private CharSequence mText;

        public Builder(Context context, CharSequence title) {
            super(context, title);
        }

        public Builder setText(CharSequence text) {
            mText = text;
            return this;
        }

        public Builder setText(@StringRes int textRes) {
            return setText(mContext.getResources().getString(textRes));
        }

        @Override
        public WalletProgressDialog create() {
            return new WalletProgressDialog(mContext, this);
        }
    }
}
