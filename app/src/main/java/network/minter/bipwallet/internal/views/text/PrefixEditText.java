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

package network.minter.bipwallet.internal.views.text;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.design.widget.TextInputEditText;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;

/**
 * MinterWallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class PrefixEditText extends TextInputEditText {
    private String prefix = this.getText().toString().trim();
    private String fix;

    public PrefixEditText(Context context) {
        super(context);
        this.init();
    }

    public PrefixEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.init();
    }

    public PrefixEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.init();
    }

    private void init() {
        setSelection(prefix.length());
        this.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                PrefixEditText.this.fix = s.toString().replace(PrefixEditText.this.prefix, "");
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String string = s.toString();
                int fl = PrefixEditText.this.prefix.length();
                int sl = string.length();
                String cek;
                if (sl < fl) {
                    cek = PrefixEditText.this.prefix;
                    PrefixEditText.this.setText(cek);
                    PrefixEditText.this.setSelection(cek.length());
                } else {
                    cek = string.substring(0, fl);
                    if (!cek.equals(PrefixEditText.this.prefix)) {
                        String in;
                        if (string.matches(PrefixEditText.this.rubah(PrefixEditText.this.prefix))) {
                            in = PrefixEditText.this.prefix + string.replace(PrefixEditText.this.prefix, "");
                            PrefixEditText.this.setText(in);
                            PrefixEditText.this.setSelection(in.length());
                        } else {
                            in = PrefixEditText.this.prefix + PrefixEditText.this.fix;
                            PrefixEditText.this.setText(in);
                            PrefixEditText.this.setSelection(in.length());
                        }
                    }
                }

            }
        });
    }

    public void setPrefix(String s) {
        this.prefix = s.trim();
        this.setText(s);
    }

    public String getValue() {
        if (getText().length() == 0) {
            return "";
        }

        return getText().toString().substring(prefix.length());
    }

    @SuppressLint("SetTextI18n")
    public void setValue(CharSequence text) {
        if (text == null) {
            return;
        }

        super.setText(prefix + text.toString());
    }

    private String rubah(String s) {
        s = s.replace("+", "\\+").replace("$", "\\$").replace("^", "\\^").replace("*", "\\*").replace("?", "\\?");
        return s;
    }
}
