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

package network.minter.bipwallet.settings.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;

import com.arellomobile.mvp.MvpAppCompatDialogFragment;
import com.arellomobile.mvp.presenter.InjectPresenter;
import com.arellomobile.mvp.presenter.ProvidePresenter;

import javax.inject.Inject;
import javax.inject.Provider;

import butterknife.BindView;
import butterknife.ButterKnife;
import network.minter.bipwallet.R;
import network.minter.bipwallet.auth.ui.InputGroup;
import network.minter.bipwallet.home.HomeModule;
import network.minter.bipwallet.internal.helpers.forms.validators.EmailValidator;
import network.minter.bipwallet.internal.helpers.forms.validators.PhoneValidator;
import network.minter.bipwallet.internal.helpers.forms.validators.RegexValidator;
import network.minter.bipwallet.internal.views.text.PrefixEditText;
import network.minter.bipwallet.settings.SettingsTabModule;
import network.minter.bipwallet.settings.views.SettingsUpdateFieldPresenter;

/**
 * MinterWallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class SettingsUpdateFieldDialog extends MvpAppCompatDialogFragment implements SettingsTabModule.SettingsUpdateFieldView {
    public final static String ARG_LABEL = "ARG_LABEL";
    public final static String ARG_FIELD_NAME = "ARG_FIELD_NAME";
    public final static String ARG_VALUE = "ARG_VALUE";
    public final static String ARG_TYPE = "ARG_TYPE";

    @Inject Provider<SettingsUpdateFieldPresenter> presenterProvider;
    @InjectPresenter SettingsUpdateFieldPresenter presenter;

    @BindView(R.id.field_layout) TextInputLayout inputLayout;
    @BindView(R.id.field_action) Button action;
    private InputGroup mGroup;
    private OnSaveListener mOnSaveListener;

    public static SettingsUpdateFieldDialog newInstance(SettingsFieldType type, CharSequence label, String fieldName,
                                                        String value) {
        Bundle args = new Bundle();
        args.putCharSequence(ARG_LABEL, label);
        args.putString(ARG_FIELD_NAME, fieldName);
        args.putString(ARG_VALUE, value);
        args.putInt(ARG_TYPE, type.ordinal());
        SettingsUpdateFieldDialog dialog = new SettingsUpdateFieldDialog();
        dialog.setArguments(args);

        return dialog;
    }

    public void setOnSaveListener(OnSaveListener listener) {
        mOnSaveListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(0x00FFFFFF));
            getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        SettingsFieldType type = SettingsFieldType.values()[getArguments().getInt(ARG_TYPE)];

        View view;
        if (type == SettingsFieldType.Username) {
            view = inflater.inflate(R.layout.fragment_update_field_username, container, false);
        } else {
            view = inflater.inflate(R.layout.fragment_update_field_simple, container, false);
        }

        view.setOnClickListener(v -> {
            dismiss();
        });

        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        presenter.handleExtras(getArguments());
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        HomeModule.getComponent().inject(this);
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_FRAME, R.style.Wallet_DialogFragment);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getDialog() != null && getDialog().getWindow() != null) {
            WindowManager.LayoutParams params = getDialog().getWindow().getAttributes();
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
            getDialog().getWindow().setAttributes(params);
        }
    }

    @Override
    public void onAttach(Context context) {
        HomeModule.getComponent().inject(this);
        super.onAttach(context);
    }

    @Override
    public void setOnTextChangedListener(TextWatcher watcher) {

    }

    @Override
    public void setLabel(CharSequence label) {
        inputLayout.setHint(label);
        inputLayout.setHintEnabled(label != null);
    }

    @Override
    public void setValue(String value) {
        if (inputLayout.getEditText() instanceof PrefixEditText) {
            ((PrefixEditText) inputLayout.getEditText()).setValue(value);
        } else {
            inputLayout.getEditText().setText(value);
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        inputLayout.setErrorEnabled(false);
        inputLayout.setError(null);
        mGroup = null;
    }

    @Override
    public void setOnSubmit(View.OnClickListener listener) {
        action.setOnClickListener(listener);
    }

    @Override
    public void configureInput(SettingsFieldType type, InputGroup.OnTextChangedListener textChangedListener) {
        mGroup = new InputGroup();
        mGroup.addInput(inputLayout);
        switch (type) {
            case Username:
                mGroup.addValidator(inputLayout, new RegexValidator("^@[a-zA-Z0-9_]{5,32}",
                        getString(R.string.input_signin_username_invalid)));
                break;
            case Email:
                mGroup.addValidator(inputLayout, new EmailValidator("Invalid email"));
                break;
            case Phone:
                mGroup.addValidator(inputLayout, new PhoneValidator("Invalid phone number"));
                break;
        }
        mGroup.addTextChangedListener(textChangedListener);
        mGroup.addFormValidateListener(new InputGroup.OnFormValidateListener() {
            @Override
            public void onValid(boolean valid) {
                inputLayout.setErrorEnabled(!valid);
            }
        });
    }

    @Override
    public void setEnableSubmit(boolean valid) {
        action.setEnabled(valid);
    }

    @Override
    public void callOnSaveListener() {
        if (mOnSaveListener != null) {
            mOnSaveListener.onSave();
        }
    }

    @ProvidePresenter
    SettingsUpdateFieldPresenter providePresenter() {
        return presenterProvider.get();
    }

    public interface OnSaveListener {
        void onSave();
    }
}
