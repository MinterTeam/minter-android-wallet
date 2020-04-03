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

package network.minter.bipwallet.internal.helpers.forms;

import android.annotation.SuppressLint;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.annimon.stream.Stream;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import network.minter.bipwallet.internal.helpers.forms.validators.BaseValidator;

import static network.minter.bipwallet.internal.common.Preconditions.firstNonNull;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class InputGroup {
    private List<EditText> mInputs = new ArrayList<>();
    private List<OnTextChangedListener> mTextWatchers = new ArrayList<>();
    private Map<EditText, List<BaseValidator>> mInputValidators = new HashMap<>();
    private List<OnFormValidateListener> mValidFormListeners = new ArrayList<>();
    private Map<String, TextView> mErrorViews = new HashMap<>();
    @SuppressLint("UseSparseArrays")
    private Map<Integer, Boolean> mValidMap = new HashMap<>();
    private List<Integer> mRequiredInputs = new ArrayList<>();
    private OnTextChangedListener mInternalTextListener = new OnTextChangedListener() {
        @Override
        public void onTextChanged(EditText editText, boolean valid) {
            mValidMap.put(editText.getId(), valid);

            int countValid = 0;
            // count required valid inputs
            for (Integer id : mRequiredInputs) {
                if (mValidMap.containsKey(id) && mValidMap.get(id)) {
                    countValid++;
                }
            }

            // valid -> required elements and all elements are valid
            boolean outValid = countValid == mRequiredInputs.size() && Stream.of(mValidMap).filter(
                    Map.Entry::getValue).count() == mValidMap.size();

            for (OnFormValidateListener listener : mValidFormListeners) {
                listener.onValid(outValid);
            }
        }
    };
    private Map<String, EditText> mInputNames = new HashMap<>();
    private Map<EditText, EditText> mValidateRelations = new HashMap<>(2);

    public InputGroup addFormValidateListener(OnFormValidateListener listener) {
        mValidFormListeners.add(listener);
        return this;
    }

    public InputGroup addInput(final TextInputLayout inputLayout) {
        return addInput(inputLayout.getEditText());
    }

    public InputGroup addInput(final TextInputLayout... input) {
        Stream.of(input)
                .forEach(this::addInput);

        return this;
    }

    public InputGroup addInput(final EditText... input) {
        Stream.of(input)
                .forEach(this::addInput);

        return this;
    }

    public InputGroup addInput(final EditText input) {
        mInputs.add(input);
        if (input.getTag() != null && input.getTag() instanceof String) {
            mInputNames.put(((String) input.getTag()), input);
        }

        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!mTextWatchers.isEmpty()) {
                    final boolean[] refValid = new boolean[]{false, false};
                    final boolean[] hasRelated = new boolean[]{false};
                    Stream.of(mTextWatchers).forEach(item -> {
                        if (mValidateRelations.containsKey(input)) {
                            hasRelated[0] = true;
                            boolean secondValid = validate(mValidateRelations.get(input), true);
                            item.onTextChanged(mValidateRelations.get(input), secondValid);
                            refValid[1] = secondValid;
                        }
                        boolean valid = validate(input, true);
                        item.onTextChanged(input, valid);
                        refValid[0] = valid;
                    });
                    mInternalTextListener.onTextChanged(input, refValid[0]);
                    if (hasRelated[0]) {
                        mInternalTextListener.onTextChanged(mValidateRelations.get(input), refValid[1]);
                    }
                } else {
                    final boolean valid = validate(input, true);
                    mInternalTextListener.onTextChanged(input, valid);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        return this;
    }

    public InputGroup setErrorView(EditText input, TextView errorView) {
        if (input.getTag() != null && input.getTag() instanceof String) {
            mErrorViews.put(((String) input.getTag()), errorView);
        }

        return this;
    }

    public InputGroup setErrorView(TextInputLayout inputLayout, TextView errorView) {
        if (inputLayout == null || inputLayout.getEditText() == null) return this;
        return setErrorView(inputLayout.getEditText(), errorView);
    }

    public boolean validate(boolean withError) {
        int countValid = 0;
        for (Map.Entry<EditText, List<BaseValidator>> entry : mInputValidators.entrySet()) {
            if (validate(entry.getKey(), withError)) {
                countValid++;
            }
        }

        return countValid == mInputValidators.size();
    }

    public InputGroup addTextChangedListener(OnTextChangedListener listener) {
        mTextWatchers.add(listener);
        return this;
    }

    public InputGroup addValidator(TextInputLayout inputLayout, BaseValidator validator) {
        return addValidator(inputLayout.getEditText(), validator);
    }

    public InputGroup addValidator(TextInputLayout inputLayout, BaseValidator... validator) {
        Stream.of(validator)
                .forEach(item -> addValidator(inputLayout, item));
        return this;
    }

    public InputGroup addFilter(TextInputLayout inputLayout, InputFilter filter) {
        return addFilter(inputLayout.getEditText(), filter);
    }

    public InputGroup addFilter(EditText editText, InputFilter filter) {
        final InputFilter[] oldFilters = firstNonNull(editText.getFilters(), new InputFilter[0]);
        final InputFilter[] newFilters = new InputFilter[oldFilters.length + 1];
        System.arraycopy(oldFilters, 0, newFilters, 0, oldFilters.length);
        newFilters[oldFilters.length] = filter;
        editText.setFilters(newFilters);

        return this;
    }

    public InputGroup addValidator(EditText editText, BaseValidator validator) {
        if (!mInputValidators.containsKey(editText)) {
            mInputValidators.put(editText, new ArrayList<>());
        }
        mInputValidators.get(editText).add(validator);
        if (validator.isRequired()) {
            mRequiredInputs.add(editText.getId());
        }
        return this;
    }

    public void clearErrors() {
        for (EditText input : mInputNames.values()) {
            if (input.getParent() != null && input.getParent().getParent() instanceof TextInputLayout) {
                final TextInputLayout tl = ((TextInputLayout) input.getParent().getParent());
                tl.post(() -> {
                    tl.setErrorEnabled(false);
                    tl.setError(null);
                });
            } else {
                input.setError(null);
            }
        }
        Stream.of(mErrorViews.values())
                .forEach(item -> {
                    item.setText(null);
                    item.setVisibility(View.GONE);
                });
    }

    public void setOnEditorActionListener(TextView.OnEditorActionListener listener) {
        Stream.of(mInputs)
                .forEach(item -> item.setOnEditorActionListener(listener));
    }

    public void setError(String fieldName, CharSequence message) {
        if (!mInputNames.containsKey(fieldName)) {
            return;
        }

        if (mInputNames.get(fieldName).getParent() != null && mInputNames.get(fieldName).getParent().getParent() instanceof TextInputLayout) {
            final TextInputLayout tl = ((TextInputLayout) mInputNames.get(fieldName).getParent().getParent());

            tl.post(() -> {
                tl.setError(null);
                tl.setErrorEnabled(message != null && message.length() > 0);
                tl.setError(message);
            });
        } else if (mErrorViews.containsKey(fieldName) && mErrorViews.get(fieldName) != null) {
            final TextView errorView = mErrorViews.get(fieldName);
            errorView.setText(message);
            errorView.setVisibility(View.VISIBLE);
        } else {
            final EditText in = mInputNames.get(fieldName);
            if (in != null) {
                in.post(() -> {
                    in.setError(null);
                    in.setError((message == null || message.length() == 0) ? null : message);
                });
            }

        }
    }

    public <T extends CharSequence> void setErrors(Map<String, List<T>> fieldsErrors) {
        if (fieldsErrors == null || fieldsErrors.isEmpty()) {
            return;
        }

        for (Map.Entry<String, List<T>> entry : fieldsErrors.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }

            setError(entry.getKey(), entry.getValue().get(0));
        }
    }

    /**
     * Triggers {@link #validate(boolean)} when one of this inputs changed
     * Example:
     * inputPassword
     * inputPasswordRepeat
     * if inputPassword was changed, validator will triggered for both fields (instead of only for editing field (default behavior))
     * and if inputPasswordRepeat was changed, validator will triggered for both fields
     * @param f1 field 1 Order no matters
     * @param f2 field 2
     * @return
     */
    public InputGroup addValidateRelation(EditText f1, EditText f2) {
        mValidateRelations.put(f1, f2);
        mValidateRelations.put(f2, f1);
        return this;
    }

    /**
     * @param f1 field 1
     * @param f2 field 2
     * @return
     * @see #addValidateRelation(EditText, EditText)
     */
    public InputGroup addValidateRelation(TextInputLayout f1, TextInputLayout f2) {
        return addValidateRelation(f1.getEditText(), f2.getEditText());
    }

    private boolean validate(EditText editText, boolean withError) {
        if (!mInputValidators.containsKey(editText)) {
            return true;
        }

        final CharSequence t = editText.getText();
        long cnt = Stream.of(mInputValidators.get(editText))
                .filter(item -> {
                    boolean valid = item.validate(t);
                    if (withError) {
                        if (editText.getParent() != null && editText.getParent().getParent() instanceof TextInputLayout) {
                            final TextInputLayout lay = ((TextInputLayout) editText.getParent().getParent());
                            lay.post(() -> {
                                if (!valid) {
                                    lay.setErrorEnabled(true);
                                    lay.setError(null);
                                    lay.setError(item.getErrorMessage());
                                } else {
                                    lay.setError(null);
                                    lay.setErrorEnabled(false);
                                }
                            });
                        } else if (editText.getTag() != null && mErrorViews.containsKey(editText.getTag())) {
                            final String fieldName = ((String) editText.getTag());
                            mErrorViews.get(fieldName).post(() -> {
                                if (!valid) {
                                    mErrorViews.get(fieldName).setText(item.getErrorMessage());
                                    mErrorViews.get(fieldName).setVisibility(View.VISIBLE);
                                } else {
                                    mErrorViews.get(fieldName).setText(null);
                                    mErrorViews.get(fieldName).setVisibility(View.GONE);
                                }
                            });
                        } else {
                            editText.post(() -> {
                                if (!valid) {
                                    editText.setError(item.getErrorMessage());
                                } else {
                                    editText.setError(null);
                                }
                            });
                        }
                    }

                    return valid;
                })
                .count();


        // count validated == validators length
        return cnt == mInputValidators.get(editText).size();
    }

    public interface OnFormValidateListener {
        void onValid(boolean valid);
    }

    public interface OnTextChangedListener {
        void onTextChanged(EditText editText, boolean valid);
    }
}
