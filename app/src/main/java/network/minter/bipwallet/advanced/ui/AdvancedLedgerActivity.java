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
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;

import javax.inject.Inject;
import javax.inject.Provider;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import butterknife.BindView;
import butterknife.ButterKnife;
import moxy.presenter.InjectPresenter;
import moxy.presenter.ProvidePresenter;
import network.minter.bipwallet.R;
import network.minter.bipwallet.advanced.contract.LedgerActivateView;
import network.minter.bipwallet.advanced.views.AdvancedLedgerPresenter;
import network.minter.bipwallet.home.ui.HomeActivity;
import network.minter.bipwallet.internal.BaseMvpInjectActivity;
import network.minter.bipwallet.internal.system.ActivityBuilder;
import network.minter.bipwallet.internal.views.widgets.ColoredProgressBar;

/**
 * minter-android-wallet. 2019
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class AdvancedLedgerActivity extends BaseMvpInjectActivity implements LedgerActivateView {
    public static final String EXTRA_FOR_RESULT = "EXTRA_FOR_RESULT";

    @Inject Provider<AdvancedLedgerPresenter> presenterProvider;
    @InjectPresenter AdvancedLedgerPresenter presenter;

    @BindView(R.id.switch_compared_addresses) Switch comparedSwitch;
    @BindView(R.id.action) Button action;
    @BindView(R.id.address) TextView addressText;
    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.secured_name) TextView securedByValue;
    @BindView(R.id.progress_layout) View progressLayout;
    @BindView(R.id.progress) ColoredProgressBar progress;
    @BindView(R.id.progress_text) TextView progressText;

    @Override
    public void setOnSwitchedConfirm(Switch.OnCheckedChangeListener listener) {
        comparedSwitch.setOnCheckedChangeListener(listener);
    }

    public void showProgress(boolean show) {
        progress.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void setProgressText(CharSequence text) {
        progressText.setText(text);
    }

    @Override
    public void setOnActionClick(View.OnClickListener listener) {
        action.setOnClickListener(listener);
    }

    @Override
    public void setAddress(CharSequence address) {
        addressText.setText(address);
    }

    @Override
    public void setEnableLaunch(boolean enable) {
        action.setEnabled(enable);
    }

    @Override
    public void setSecuredByValue(CharSequence name) {
        securedByValue.setText(name);
    }

    @Override
    public void startHome() {
        startActivityClearTop(this, HomeActivity.class);
        finish();
    }

    @Override
    public void finishSuccess() {
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public void showAddress(boolean show) {
        addressText.setVisibility(show ? View.VISIBLE : View.GONE);
        progressLayout.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    @Override
    public void setEnableSwitch(boolean enable) {
        comparedSwitch.setEnabled(enable);
    }

    @ProvidePresenter
    AdvancedLedgerPresenter providePresenter() {
        return presenterProvider.get();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advanced_ledger);
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
            return AdvancedLedgerActivity.class;
        }
    }
}
