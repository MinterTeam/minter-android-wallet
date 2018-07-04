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

package network.minter.bipwallet.internal.helpers.forms.validators;


import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.widget.EditText;

import network.minter.mintercore.internal.common.CallbackProvider;

/**
 * Dogsy. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class CompareValidator extends BaseValidator {
    private CallbackProvider<CharSequence> mComparable;

    public CompareValidator(CallbackProvider<CharSequence> comparable, boolean required) {
        super(required);
        mComparable = comparable;
    }

    public CompareValidator(CharSequence errorMessage, boolean required, @NonNull EditText editText) {
        this(errorMessage, required, editText::getText);
    }

    public CompareValidator(CharSequence errorMessage, boolean required, @NonNull TextInputLayout inputLayout) {
        this(errorMessage, required, ()->inputLayout.getEditText().getText());
    }

    public CompareValidator(CharSequence errorMessage, boolean required, CallbackProvider<CharSequence> comparable) {
        super(errorMessage, required);
        mComparable = comparable;
    }

    public CompareValidator(CharSequence errorMessage, @NonNull EditText editText) {
        this(errorMessage, editText::getText);
    }

    public CompareValidator(CharSequence errorMessage, @NonNull TextInputLayout inputLayout) {
        this(errorMessage, ()->inputLayout.getEditText().getText());
    }

    public CompareValidator(CharSequence errorMessage, CallbackProvider<CharSequence> comparable) {
        super(errorMessage);
        mComparable = comparable;
    }

    @Override
    protected boolean getCondition(CharSequence value) {
        return value != null && mComparable.get() != null && value.toString().equals(mComparable.get().toString());
    }
}
