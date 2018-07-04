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

package network.minter.bipwallet.auth.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.TextView;

import com.arellomobile.mvp.presenter.InjectPresenter;
import com.arellomobile.mvp.presenter.ProvidePresenter;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;

import butterknife.BindView;
import butterknife.ButterKnife;
import network.minter.bipwallet.R;
import network.minter.bipwallet.auth.AuthModule;
import network.minter.bipwallet.auth.views.RegisterPresenter;
import network.minter.bipwallet.home.ui.HomeActivity;
import network.minter.bipwallet.internal.BaseMvpInjectActivity;
import network.minter.bipwallet.internal.helpers.KeyboardHelper;
import network.minter.bipwallet.internal.helpers.forms.validators.CompareValidator;
import network.minter.bipwallet.internal.helpers.forms.validators.EmailValidator;
import network.minter.bipwallet.internal.helpers.forms.validators.LengthValidator;
import network.minter.bipwallet.internal.helpers.forms.validators.PhoneValidator;
import network.minter.bipwallet.internal.helpers.forms.validators.RegexValidator;
import network.minter.bipwallet.internal.views.widgets.ToolbarProgress;

/**
 * MinterWallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class RegisterActivity extends BaseMvpInjectActivity implements AuthModule.RegisterView {
    @Inject Provider<RegisterPresenter> presenterProvider;
    @InjectPresenter RegisterPresenter presenter;
    @BindView(R.id.inputLayoutUsername) TextInputLayout usernameLayout;
    @BindView(R.id.inputLayoutPassword) TextInputLayout passwordLayout;
    @BindView(R.id.inputLayoutPasswordRepeat) TextInputLayout passwordRepeatLayout;
    @BindView(R.id.inputLayoutEmail) TextInputLayout emailLayout;
    @BindView(R.id.inputLayoutPhone) TextInputLayout phoneLayout;
    @BindView(R.id.action) Button action;
    @BindView(R.id.toolbar) ToolbarProgress toolbar;
    @BindView(R.id.errorText) TextView errorText;
    private InputGroup mInputGroup;

    @Override
    public void setOnTextChangedListener(InputGroup.OnTextChangedListener listener) {
        mInputGroup.addTextChangedListener(listener);
    }

    @Override
    public void setOnSubmit(View.OnClickListener listener) {
        action.setOnClickListener(listener);
        mInputGroup.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND || event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                listener.onClick(textView);
                return true;
            }

            return false;
        });
    }

    @Override
    public void setOnFormValidateListener(InputGroup.OnFormValidateListener listener) {
        mInputGroup.addFormValidateListener(listener);
        mInputGroup.validate(false);
    }

    @Override
    public void setEnableSubmit(boolean enable) {
        action.setEnabled(enable);
    }

    @Override
    public void startHome() {
        startActivityClearTop(this, HomeActivity.class);
        finish();
    }

    @Override
    public void validate(boolean withError) {
        mInputGroup.validate(withError);
    }

    @Override
    public void setInputError(String fieldName, String message) {
        mInputGroup.setError(fieldName, message);
    }

    @Override
    public void setResultError(CharSequence error) {
        errorText.setText(error);
        errorText.setVisibility(error != null && error.length() > 0 ? View.VISIBLE : View.GONE);
    }

    @Override
    public void clearErrors() {
        mInputGroup.clearErrors();
    }

    @Override
    public void setInputErrors(Map<String, List<String>> data) {
        mInputGroup.setErrors(data);
    }

    @Override
    public void startConfirmation(Uri endpoint) {
        startActivity(new Intent(Intent.ACTION_VIEW, endpoint));
//        startActivityClearTop(this, AuthActivity.class);
        finish();
    }

    @Override
    public void hideKeyboard() {
        KeyboardHelper.hideKeyboard(this);
    }

    @Override
    public void showProgress() {
        toolbar.showProgress();
    }

    @Override
    public void hideProgress() {
        toolbar.hideProgress();
    }

    @ProvidePresenter
    RegisterPresenter providePresenter() {
        return presenterProvider.get();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        ButterKnife.bind(this);
        setupToolbar(toolbar);

        mInputGroup = new InputGroup();
        mInputGroup.addInput(usernameLayout);
        mInputGroup.addInput(passwordLayout);
        mInputGroup.addInput(passwordRepeatLayout);
        mInputGroup.addInput(emailLayout);
        mInputGroup.addInput(phoneLayout);

        mInputGroup.addValidator(usernameLayout, new RegexValidator("^@[a-zA-Z0-9_]{5,32}",
                                                                    getString(R.string.input_signin_username_invalid)));
        mInputGroup.addValidator(passwordLayout,
                                 new LengthValidator(getString(R.string.input_signin_password_invalid), 6));
        mInputGroup.addValidator(passwordRepeatLayout,
                                 new CompareValidator(getString(R.string.input_signin_password_not_match),
                                                      passwordLayout));
        mInputGroup.addValidator(emailLayout, new EmailValidator("Invalid email", false));
        mInputGroup.addValidator(phoneLayout, new PhoneValidator("Invalid phone number", false));

    }
}
