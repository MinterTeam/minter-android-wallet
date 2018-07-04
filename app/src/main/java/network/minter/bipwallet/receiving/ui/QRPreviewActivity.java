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

package network.minter.bipwallet.receiving.ui;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.transition.AutoTransition;
import android.widget.ImageView;

import com.squareup.picasso.Callback;

import java.io.File;

import network.minter.bipwallet.R;
import network.minter.bipwallet.internal.BaseActivity;
import network.minter.bipwallet.internal.Wallet;
import network.minter.bipwallet.internal.system.ActivityBuilder;
import timber.log.Timber;

/**
 * MinterWallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class QRPreviewActivity extends BaseActivity {
    private final static String EXTRA_FILE_PATH = "EXTRA_FILE_PATH";

    @Override
    public void onBackPressed() {
//        super.onBackPressed();
//        finishAfterTransition();
        supportFinishAfterTransition();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportPostponeEnterTransition();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setEnterTransition(null);
            getWindow().setExitTransition(null);
            getWindow().setSharedElementEnterTransition(new AutoTransition());
        }

        setContentView(R.layout.activity_qr_preview);
        final ImageView iv = findViewById(R.id.iv);
        Wallet.app().image()
                .load(new File(getIntent().getStringExtra(EXTRA_FILE_PATH)))
                .resize(Wallet.app().display().getWidth(), Wallet.app().display().getHeight())
                .centerInside()
                .into(iv, new Callback() {
                    @Override
                    public void onSuccess() {
                        supportStartPostponedEnterTransition();
                    }

                    @Override
                    public void onError(Exception t) {
                        Timber.w(t, "Unable to load image");
//                        iv.setImageResource(R.drawable.ic_qr);
                        supportStartPostponedEnterTransition();
                    }
                });
    }

    public static class Builder extends ActivityBuilder {
        private String mFilePath;

        public Builder(@NonNull Activity from, String filePath) {
            super(from);
            mFilePath = filePath;
        }

        public Builder(@NonNull Fragment from, String filePath) {
            super(from);
            mFilePath = filePath;
        }

        public Builder(@NonNull Service from, String filePath) {
            super(from);
            mFilePath = filePath;
        }

        @Override
        protected void onBeforeStart(Intent intent) {
            super.onBeforeStart(intent);
            intent.putExtra(EXTRA_FILE_PATH, mFilePath);
        }

        @Override
        protected Class<?> getActivityClass() {
            return QRPreviewActivity.class;
        }
    }
}
