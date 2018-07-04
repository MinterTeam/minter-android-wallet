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
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.text.InputType;
import android.view.View;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.TextView;

import com.annimon.stream.Stream;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import network.minter.bipwallet.R;
import network.minter.bipwallet.auth.ui.InputGroup;
import network.minter.bipwallet.internal.common.CallbackProvider;
import network.minter.bipwallet.internal.helpers.forms.validators.BaseValidator;

/**
 * MinterWallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class WalletInputDialog extends WalletDialog {
    @BindView(R.id.input_layout_stub) ViewStub mInputStub;
    @BindView(R.id.input_description) TextView description;
    @BindView(R.id.action) Button action;
    private Builder mBuilder;
    private InputGroup mInputGroup;
    private TextInputLayout mInputLayout;
    private boolean mValid = true;
    private String mOutValue;

    protected WalletInputDialog(@NonNull Context context, Builder builder) {
        super(context);
        mBuilder = builder;
        mInputGroup = new InputGroup();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wallet_input_dialog);
        ButterKnife.bind(this);
        title.setText(mBuilder.mTitle);

        if (mBuilder.mInputTypeFlags == InputType.TYPE_NULL) {
            mInputStub.setLayoutResource(R.layout.inc_dialog_input_prefix);
        } else {
            mInputStub.setLayoutResource(R.layout.inc_dialog_input_simple);
        }
        mInputLayout = (TextInputLayout) mInputStub.inflate();
        final TextInputEditText input = (TextInputEditText) mInputLayout.getEditText();
        assert input != null;

        input.setText(mBuilder.mValue.get());
        mInputLayout.setHintEnabled(mBuilder.mHint != null);
        if (mBuilder.mHint != null) {
            mInputLayout.setHint(mBuilder.mHint);
        }
        input.setInputType(mBuilder.mInputTypeFlags);

        description.setText(mBuilder.mDescription);
        description.setVisibility(mBuilder.mDescription != null ? View.VISIBLE : View.GONE);
        if (mBuilder.mActionTitle != null) {
            action.setText(mBuilder.mActionTitle);
        }

        mInputGroup.addInput(mInputLayout);
        if (mBuilder.mValidators != null) {
            Stream.of(mBuilder.mValidators)
                    .forEach(item -> mInputGroup.addValidator(mInputLayout, item));
        }


        mInputGroup.addTextChangedListener((editText, valid) -> {
            if (valid) {
                mOutValue = editText.getText().toString();
            } else {
                mOutValue = null;
            }
        });
        mInputGroup.addFormValidateListener(valid -> mValid = valid);
        action.setTag(mBuilder.mFieldName);

        action.setOnClickListener(v -> {
            if (!mValid) {
                return;
            }

            if (mBuilder.mOnSubmitListener != null) {
                final String fieldName = v.getTag() == null ? null : ((String) v.getTag());
                if (mBuilder.mOnSubmitListener.onSubmit(fieldName, mOutValue)) {
                    dismiss();
                }

                return;
            }

            dismiss();
        });
    }

    public interface OnSubmitListener {
        /**
         * @param fieldName input field name (tag)
         * @param value
         * @return True if dismiss, false - dialog will not be dismissed
         */
        boolean onSubmit(@Nullable String fieldName, String value);
    }

    public static final class Builder extends WalletDialogBuilder<WalletInputDialog, WalletInputDialog.Builder> {
        private OnSubmitListener mOnSubmitListener;
        private CharSequence mDescription;
        private CallbackProvider<String> mValue = () -> null;
        private CharSequence mHint;
        private int mInputTypeFlags = InputType.TYPE_CLASS_TEXT;
        private CharSequence mActionTitle;
        private List<BaseValidator> mValidators;
        private String mFieldName = null;

        public Builder(Context context, CharSequence title) {
            super(context, title);
        }

        public Builder setSubmitListener(OnSubmitListener listener) {
            mOnSubmitListener = listener;
            return this;
        }

        public Builder setDescription(CharSequence text) {
            mDescription = text;
            return this;
        }

        public Builder setDescription(@StringRes int resId) {
            return setDescription(mContext.getResources().getString(resId));
        }

        public Builder setActionTitle(CharSequence title) {
            mActionTitle = title;
            return this;
        }

        public Builder setFieldName(String fieldName) {
            if (fieldName == null || fieldName.isEmpty()) {
                return this;
            }

            mFieldName = fieldName;
            return this;
        }

        public Builder setValue(String value) {
            mValue = () -> value;
            return this;
        }

        public Builder setValue(CallbackProvider<String> value) {
            mValue = value;
            return this;
        }

        public Builder addValidator(BaseValidator... validator) {
            if (mValidators == null) {
                mValidators = new ArrayList<>();
            }
            mValidators.addAll(Stream.of(validator).toList());
            return this;
        }

        public Builder setInputType(int flags) {
            mInputTypeFlags = flags;
            return this;
        }

        public Builder setInputTypeText() {
            return setInputType(InputType.TYPE_CLASS_TEXT);
        }

        public Builder setInputTypePassword() {
            return setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }

        public Builder setInputTypePhone() {
            return setInputType(InputType.TYPE_CLASS_PHONE);
        }

        public Builder setInputTypeEmail() {
            return setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        }

        public Builder setInputTypeUsername() {
            return setInputType(InputType.TYPE_NULL);
        }

        public Builder setHint(CharSequence hint) {
            mHint = hint;
            return this;
        }

        @Override
        public WalletInputDialog create() {
            return new WalletInputDialog(mContext, this);
        }
    }
}
