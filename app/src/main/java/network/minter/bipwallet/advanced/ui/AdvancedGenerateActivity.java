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

package network.minter.bipwallet.advanced.ui;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;

import com.arellomobile.mvp.presenter.InjectPresenter;
import com.arellomobile.mvp.presenter.ProvidePresenter;

import javax.inject.Inject;
import javax.inject.Provider;

import butterknife.BindView;
import butterknife.ButterKnife;
import network.minter.bipwallet.R;
import network.minter.bipwallet.advanced.AdvancedModeModule;
import network.minter.bipwallet.advanced.views.AdvancedGeneratePresenter;
import network.minter.bipwallet.home.ui.HomeActivity;
import network.minter.bipwallet.internal.BaseMvpInjectActivity;
import network.minter.bipwallet.internal.dialogs.WalletInputDialog;
import network.minter.bipwallet.internal.helpers.forms.validators.RegexValidator;
import network.minter.bipwallet.internal.system.ActivityBuilder;

/**
 * MinterWallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class AdvancedGenerateActivity extends BaseMvpInjectActivity implements AdvancedModeModule.GenerateView {
    public static final String EXTRA_FOR_RESULT = "EXTRA_FOR_RESULT";

    @Inject Provider<AdvancedGeneratePresenter> presenterProvider;
    @InjectPresenter AdvancedGeneratePresenter presenter;

    @BindView(R.id.actionCopy) View actionCopy;
    @BindView(R.id.saveSwitch) Switch saveSwitch;
    @BindView(R.id.action) Button action;
    @BindView(R.id.mnemonic) TextView mnemonicPhrase;
    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.securedBy) View securedBySelector;
    @BindView(R.id.secName) TextView securedByValue;

    @Override
    public void setOnCopy(View.OnClickListener listener) {
        actionCopy.setOnClickListener(listener);
    }

    @Override
    public void setOnSwitchedConfirm(Switch.OnCheckedChangeListener listener) {
        saveSwitch.setOnCheckedChangeListener(listener);
    }

    @Override
    public void setOnActionClick(View.OnClickListener listener) {
        action.setOnClickListener(listener);
    }

    @Override
    public void setOnSecuredByClickListener(View.OnClickListener listener) {
    }

    @Override
    public void setMnemonic(CharSequence phrase) {
        mnemonicPhrase.setText(phrase);
    }

    @Override
    public void setEnableLaunch(boolean enable) {
        action.setEnabled(enable);
    }

    @Override
    public void setEnableCopy(boolean enable) {
        actionCopy.setEnabled(enable);
    }

    @Override
    public void startHome() {
        startActivityClearTop(this, HomeActivity.class);
        finish();
    }

    /**
     * @param enable
     * @TODO less hardcode
     */
    @Override
    public void setEnableSecureVariants(boolean enable, AdvancedMainActivity.OnSelectSecureVariant onSelect) {
        securedBySelector.setVisibility(enable ? View.VISIBLE : View.GONE);

        if (enable) {
            securedBySelector.setOnClickListener(v -> {
                final PopupMenu menu = new PopupMenu(this, v);
                menu.inflate(R.menu.menu_secured_select);
                menu.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == R.id.securedBy_you) {
                        securedByValue.setText("You");
                    } else if (item.getItemId() == R.id.securedBy_bip) {
                        securedByValue.setText("Bip Wallet");
                    }
                    onSelect.onSelectVariant(item.getItemId());
                    return true;
                });

                menu.show();

            });
        }
    }

    @Override
    public void askPassword(WalletInputDialog.OnSubmitListener submitListener) {
        new WalletInputDialog.Builder(this, "Please, type password")
                .setInputTypePassword()
                .setDescription("We need your password to encrypt all private data")
                .setHint("Password")
                .addValidator(new RegexValidator(".{6,}", getString(R.string.input_signin_password_invalid)))
                .setSubmitListener(submitListener)
                .create().show();
    }

    @Override
    public void finishSuccess() {
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public void setActionTitle(CharSequence title) {
        action.setText(title);
    }

    @ProvidePresenter
    AdvancedGeneratePresenter providePresenter() {
        return presenterProvider.get();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advanced_generate);
        ButterKnife.bind(this);
        setupToolbar(toolbar);
        presenter.handleExtras(getIntent());
    }

    public static final class Builder extends ActivityBuilder {
        private boolean mForResult = false;

        public Builder(@NonNull Activity from) {
            super(from);
        }

        public Builder(@NonNull Fragment from) {
            super(from);
        }

        public Builder(@NonNull Service from) {
            super(from);
        }

        public Builder setForResult(boolean forResult) {
            mForResult = forResult;
            return this;
        }

        @Override
        protected void onBeforeStart(Intent intent) {
            super.onBeforeStart(intent);
            intent.putExtra(EXTRA_FOR_RESULT, mForResult);
        }

        @Override
        protected Class<?> getActivityClass() {
            return AdvancedGenerateActivity.class;
        }
    }
}
