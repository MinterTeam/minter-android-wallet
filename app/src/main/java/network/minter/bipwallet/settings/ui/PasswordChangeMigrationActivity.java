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

import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;

import com.arellomobile.mvp.presenter.InjectPresenter;
import com.arellomobile.mvp.presenter.ProvidePresenter;

import javax.inject.Inject;
import javax.inject.Provider;

import butterknife.BindView;
import butterknife.ButterKnife;
import network.minter.bipwallet.R;
import network.minter.bipwallet.advanced.repo.SecretStorage;
import network.minter.bipwallet.auth.ui.InputGroup;
import network.minter.bipwallet.internal.BaseMvpInjectActivity;
import network.minter.bipwallet.internal.dialogs.WalletDialog;
import network.minter.bipwallet.internal.helpers.forms.validators.CompareValidator;
import network.minter.bipwallet.internal.helpers.forms.validators.CustomValidator;
import network.minter.bipwallet.internal.helpers.forms.validators.LengthValidator;
import network.minter.bipwallet.settings.SettingsTabModule;
import network.minter.bipwallet.settings.views.PasswordChangeMigrationPresenter;
import network.minter.mintercore.crypto.HashUtil;

/**
 * MinterWallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class PasswordChangeMigrationActivity extends BaseMvpInjectActivity implements SettingsTabModule.PasswordChangeMigrationView {

    @Inject Provider<PasswordChangeMigrationPresenter> presenterProvider;
    @InjectPresenter PasswordChangeMigrationPresenter presenter;
    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.layout_password_old) TextInputLayout layoutPasswordOld;
    @BindView(R.id.layout_password_new) TextInputLayout layoutPasswordNew;
    @BindView(R.id.layout_password_new_repeat) TextInputLayout layoutPasswordNewRepeat;
    @BindView(R.id.action) Button action;
    @Inject SecretStorage secretStorage;
    private WalletDialog mDialog;
    private InputGroup mInputGroup;

    @Override
    public void setTextChangedListener(InputGroup.OnTextChangedListener listener) {
        mInputGroup.addTextChangedListener(listener);
    }

    @Override
    public void setFormValidateListener(InputGroup.OnFormValidateListener listener) {
        mInputGroup.addFormValidateListener(listener);
    }

    @Override
    public void setOnClickSubmit(View.OnClickListener listener) {
        action.setOnClickListener(listener);
    }

    @Override
    public void startDialog(WalletDialog.DialogExecutor executor) {
        mDialog = WalletDialog.switchDialogWithExecutor(this, mDialog, executor);
    }

    @Override
    public void setEnableSubmit(boolean enable) {
        action.setEnabled(enable);
    }

    @ProvidePresenter
    PasswordChangeMigrationPresenter providePresenter() {
        return presenterProvider.get();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_change_migration);
        ButterKnife.bind(this);
        setupToolbar(toolbar);
        mInputGroup = new InputGroup();
        mInputGroup.addInput(layoutPasswordOld, layoutPasswordNew, layoutPasswordNewRepeat);
        mInputGroup.addValidator(layoutPasswordOld, new CustomValidator("Invalid password", (v) -> HashUtil.sha256Hex(v.toString()).equals(secretStorage.getEncryptionKey())));
        mInputGroup.addValidator(layoutPasswordNew, new LengthValidator(getString(R.string.input_signin_password_invalid), 6));
        mInputGroup.addValidator(layoutPasswordNewRepeat, new CompareValidator(getString(R.string.input_signin_password_not_match), layoutPasswordNew));
    }
}
