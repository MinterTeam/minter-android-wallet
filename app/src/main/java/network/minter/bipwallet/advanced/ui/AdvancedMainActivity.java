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

package network.minter.bipwallet.advanced.ui;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;

import javax.inject.Inject;
import javax.inject.Provider;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import butterknife.BindView;
import butterknife.ButterKnife;
import moxy.presenter.InjectPresenter;
import moxy.presenter.ProvidePresenter;
import network.minter.bipwallet.BuildConfig;
import network.minter.bipwallet.R;
import network.minter.bipwallet.advanced.contract.MainView;
import network.minter.bipwallet.advanced.views.AdvancedMainPresenter;
import network.minter.bipwallet.home.ui.HomeActivity;
import network.minter.bipwallet.internal.BaseMvpInjectActivity;
import network.minter.bipwallet.internal.dialogs.WalletInputDialog;
import network.minter.bipwallet.internal.dialogs.WalletProgressDialog;
import network.minter.bipwallet.internal.helpers.KeyboardHelper;
import network.minter.bipwallet.internal.system.ActivityBuilder;

/**
 * minter-android-wallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class AdvancedMainActivity extends BaseMvpInjectActivity implements MainView {

    public static final String EXTRA_FOR_RESULT = "EXTRA_FOR_RESULT";
    public static final String EXTRA_TITLE = "EXTRA_TITLE";

    @BindView(R.id.action_generate) Button actionGenerate;
    @BindView(R.id.action_activate) Button actionActivate;
    @BindView(R.id.action_activate_ledger) Button actionLedger;
    @BindView(R.id.input_seed) AppCompatEditText seedInput;
    @BindView(R.id.toolbar) Toolbar toolbar;

    @Inject Provider<AdvancedMainPresenter> presenterProvider;
    @InjectPresenter AdvancedMainPresenter presenter;
    private WalletProgressDialog mProgress;

    @Override
    public void setOnGenerate(View.OnClickListener listener) {
        actionGenerate.setOnClickListener(listener);
    }

    @Override
    public void setOnLedger(View.OnClickListener listener) {
        actionLedger.setOnClickListener(listener);
    }

    @Override
    public void setMnemonicTextChangedListener(TextWatcher textWatcher) {
        seedInput.addTextChangedListener(textWatcher);
    }

    @Override
    public void setOnActivateMnemonic(View.OnClickListener listener) {
        actionActivate.setOnClickListener(listener);
    }

    @Override
    public void startGenerate() {
        startActivity(new Intent(this, AdvancedGenerateActivity.class));
    }

    @Override
    public void startLedger() {
        new AdvancedLedgerActivity.Builder(this).start();
    }

    @Override
    public void startGenerate(int requestCode) {
        new AdvancedGenerateActivity.Builder(this).start(requestCode);
        finish();
    }

    @Override
    public void askPassword(WalletInputDialog.OnSubmitListener submitListener) {
        new WalletInputDialog.Builder(this, "Please, type password")
                .setInputTypePassword()
                .setDescription("We need your password to encrypt all private data")
                .setHint("Password")
                .setSubmitListener(submitListener)
                .create().show();
    }

    @Override
    public void setError(CharSequence errorMessage) {
        seedInput.setError(errorMessage);
    }

    @Override
    public void finishSuccess() {
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public void startHome() {
        KeyboardHelper.hideKeyboard(this);
        startActivityClearTop(this, HomeActivity.class);
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advanced_main);
        ButterKnife.bind(this);
        if (!BuildConfig.ENABLE_LEDGER_UI) {
            actionLedger.setVisibility(View.GONE);
            findViewById(R.id.separator2).setVisibility(View.GONE);
            findViewById(R.id.separator2_text).setVisibility(View.GONE);
        }
        setupToolbar(toolbar);
        presenter.handleExtras(getIntent());
    }

    @ProvidePresenter
    AdvancedMainPresenter providePresenter() {
        return presenterProvider.get();
    }

    // @TODO move out here
    public interface OnSelectSecureVariant {
        void onSelectVariant(int menuId);
    }

    public static final class Builder extends ActivityBuilder {
        private CharSequence mTitle;
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

        public Builder setTitle(CharSequence title) {
            mTitle = title;
            return this;
        }

        public Builder setForResult(boolean forResult) {
            mForResult = forResult;
            return this;
        }

        @Override
        protected void onBeforeStart(Intent intent) {
            super.onBeforeStart(intent);
            intent.putExtra(EXTRA_TITLE, mTitle);
            intent.putExtra(EXTRA_FOR_RESULT, mForResult);
        }

        @Override
        protected Class<?> getActivityClass() {
            return AdvancedMainActivity.class;
        }
    }
}
