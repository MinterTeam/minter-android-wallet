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

package network.minter.bipwallet.auth.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputLayout;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;

import butterknife.BindView;
import butterknife.ButterKnife;
import moxy.presenter.InjectPresenter;
import moxy.presenter.ProvidePresenter;
import network.minter.bipwallet.R;
import network.minter.bipwallet.auth.contract.RegisterView;
import network.minter.bipwallet.auth.views.RegisterPresenter;
import network.minter.bipwallet.home.ui.HomeActivity;
import network.minter.bipwallet.internal.BaseMvpInjectActivity;
import network.minter.bipwallet.internal.helpers.KeyboardHelper;
import network.minter.bipwallet.internal.helpers.forms.InputGroup;
import network.minter.bipwallet.internal.helpers.forms.validators.CompareValidator;
import network.minter.bipwallet.internal.helpers.forms.validators.LengthValidator;
import network.minter.bipwallet.internal.helpers.forms.validators.MinterUsernameValidator;
import network.minter.bipwallet.internal.views.widgets.ToolbarProgress;

import static androidx.core.content.res.ResourcesCompat.getFont;

/**
 * minter-android-wallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class RegisterActivity extends BaseMvpInjectActivity implements RegisterView {
    @Inject Provider<RegisterPresenter> presenterProvider;
    @InjectPresenter RegisterPresenter presenter;
    @BindView(R.id.layout_input_username) TextInputLayout usernameLayout;
    @BindView(R.id.layout_input_password) TextInputLayout passwordLayout;
    @BindView(R.id.layout_input_password_repeat) TextInputLayout passwordRepeatLayout;
    @BindView(R.id.layout_input_phone) TextInputLayout phoneLayout;
    @BindView(R.id.action) Button action;
    @BindView(R.id.toolbar) ToolbarProgress toolbar;
    @BindView(R.id.error_text) TextView errorText;
    private InputGroup mInputGroup;

    @Override
    public void setOnTextChangedListener(InputGroup.OnTextChangedListener listener) {
        mInputGroup.addTextChangedListener(listener);
    }

    @Override
    public void setOnSubmit(View.OnClickListener listener) {
        action.setOnClickListener(listener);

        usernameLayout.getEditText().setNextFocusDownId(R.id.layout_input_password);
        passwordLayout.getEditText().setNextFocusDownId(R.id.layout_input_password_repeat);

        passwordRepeatLayout.getEditText().setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                listener.onClick(textView);
                return true;
            } else if (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
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

        passwordLayout.setTypeface(getFont(this, R.font._ubuntu_regular));
        passwordRepeatLayout.setTypeface(getFont(this, R.font._ubuntu_regular));

        mInputGroup = new InputGroup();
        mInputGroup.addInput(usernameLayout);
        mInputGroup.addInput(passwordLayout);
        mInputGroup.addInput(passwordRepeatLayout);
        mInputGroup.addInput(phoneLayout);

        mInputGroup.addValidator(usernameLayout, new MinterUsernameValidator(getString(R.string.input_username_invalid)));
        mInputGroup.addValidator(passwordLayout, new LengthValidator(getString(R.string.input_password_invalid), 6));
        mInputGroup.addValidator(passwordRepeatLayout,
                                 new CompareValidator(getString(R.string.input_signin_password_not_match),
                                                      passwordLayout));
        mInputGroup.addValidateRelation(passwordLayout, passwordRepeatLayout);


        mInputGroup.addFilter(usernameLayout, (source, start, end, dest, dstart, dend) -> source.toString().toLowerCase());

    }
}
