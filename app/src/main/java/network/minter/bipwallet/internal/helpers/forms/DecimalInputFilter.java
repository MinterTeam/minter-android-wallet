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

package network.minter.bipwallet.internal.helpers.forms;

import android.text.Spanned;
import android.text.method.DigitsKeyListener;
import android.widget.EditText;

import network.minter.bipwallet.internal.common.CallbackProvider;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class DecimalInputFilter extends DigitsKeyListener {
    private final CallbackProvider<EditText> mView;
    private int mDecimals = 18;

    public DecimalInputFilter(CallbackProvider<EditText> txtView) {
        this(txtView, 18);
    }

    public DecimalInputFilter(CallbackProvider<EditText> txtView, int decimals) {
        super(false, true);
        mView = txtView;
        mDecimals = decimals;
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        String tmp = mView.get().getText().toString();

        if (source.length() > 1) {
            return source.toString().replaceAll("[^0-9\\.]", "");
        }

        if (source.equals(",")) {
            return "";
        }

        if (source.equals(".") && tmp.isEmpty()) {
            return "0.";
        }

        if (tmp.equals("0") && !source.equals(".")) {
            return "";
        }

        if (source.equals(".") && tmp.contains(".")) {
            return "";
        }

        final int ptIndex = tmp.indexOf(".");
        if (ptIndex == -1) {
            if (tmp.equals("0") && source.equals(".")) {
                return source;
            }

            if (tmp.length() > 0 && ((tmp.charAt(0) == '0' && dstart > 0) || (tmp.charAt(0) != '0' && source.equals("0") && dstart == 0))) {
                return "";
            }
            return source;
        }


        if (ptIndex >= dstart) {
            if (tmp.charAt(0) == '.') {
                return source;
            }
            if ((tmp.charAt(0) == '0' && dstart > 0) || (tmp.charAt(0) != '0' && source.equals("0") && dstart == 0)) {
                return "";
            }
        } else if (ptIndex < dstart) {
            String decimals = tmp.substring(ptIndex + 1);
            if (decimals.length() >= mDecimals) {
                return "";
            }
        }

        return super.filter(source, start, end, dest, dstart, dend);
    }
}
