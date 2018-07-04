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

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.CodeScannerView;
import com.budiyev.android.codescanner.DecodeCallback;
import com.google.zxing.Result;

import butterknife.BindView;
import butterknife.ButterKnife;
import network.minter.bipwallet.R;
import network.minter.bipwallet.internal.BaseActivity;
import timber.log.Timber;

import static com.budiyev.android.codescanner.CodeScanner.TWO_DIMENSIONAL_FORMATS;

/**
 * MinterWallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class QRCodeScannerActivity extends BaseActivity implements DecodeCallback {
    public final static String RESULT_TEXT = QRCodeScannerActivity.class.getName() + "_result_text";
    @BindView(R.id.scanner_view) CodeScannerView scannerView;
    private CodeScanner mCodeScanner;

    @Override
    public void onDecoded(@NonNull Result result) {
        runOnUiThread(() -> {
            if (result.getText() != null) {
                Timber.d("Scanned result: %s", result.getText());
                Intent intent = new Intent();
                intent.putExtra(RESULT_TEXT, result.getText());
                setResult(RESULT_OK, intent);
                finish();
            } else {
                Timber.w("Unable to get text result: %s", result.toString());
            }

        });
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        super.onBackPressed();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scanner);
        ButterKnife.bind(this);

        mCodeScanner = new CodeScanner(this, scannerView);
        mCodeScanner.setDecodeCallback(this);
        mCodeScanner.setFormats(TWO_DIMENSIONAL_FORMATS);
        scannerView.setOnClickListener(v -> {
            mCodeScanner.startPreview();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCodeScanner.startPreview();
    }

    @Override
    protected void onPause() {
        mCodeScanner.releaseResources();
        super.onPause();
    }
}
