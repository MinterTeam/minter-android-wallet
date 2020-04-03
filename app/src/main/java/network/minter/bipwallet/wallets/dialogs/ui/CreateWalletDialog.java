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

package network.minter.bipwallet.wallets.dialogs.ui;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputEditText;

import javax.inject.Inject;
import javax.inject.Provider;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import butterknife.BindView;
import butterknife.ButterKnife;
import dagger.android.support.AndroidSupportInjection;
import moxy.presenter.InjectPresenter;
import moxy.presenter.ProvidePresenter;
import network.minter.bipwallet.R;
import network.minter.bipwallet.auth.contract.CreateWalletView;
import network.minter.bipwallet.home.ui.HomeActivity;
import network.minter.bipwallet.internal.dialogs.BaseBottomSheetDialog;
import network.minter.bipwallet.internal.helpers.KeyboardHelper;
import network.minter.bipwallet.wallets.dialogs.presentation.CreateWalletPresenter;

import static network.minter.bipwallet.internal.helpers.ViewHelper.visible;

public class CreateWalletDialog extends BaseBottomSheetDialog implements CreateWalletView {
    public final static String EXTRA_ENABLE_TITLE_INPUT = "EXTRA_ENABLE_TITLE_INPUT";
    public final static String EXTRA_ENABLE_DESCRIPTION = "EXTRA_ENABLE_DESCRIPTION";
    public final static String EXTRA_ENABLE_START_HOME_ON_SUBMIT = "EXTRA_ENABLE_START_HOME_ON_SUBMIT";
    public final static String EXTRA_TITLE = "EXTRA_TITLE";
    @Inject Provider<CreateWalletPresenter> presenterProvider;
    @InjectPresenter CreateWalletPresenter presenter;
    @BindView(R.id.dialog_title) TextView title;
    @BindView(R.id.dialog_description) TextView description;
    @BindView(R.id.seed) TextView seed;
    @BindView(R.id.layout_seed) View seedLayout;
    @BindView(R.id.layout_seed_alert) View alertCopied;
    @BindView(R.id.action_saved_seed) Switch actionSavedSeed;
    @BindView(R.id.submit) View submit;
    @BindView(R.id.input_title) TextInputEditText inputTitle;
    @BindView(R.id.input_title_hint) TextView inputTitleHint;


    @ProvidePresenter
    public CreateWalletPresenter providePresenter() {
        return presenterProvider.get();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidSupportInjection.inject(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        AndroidSupportInjection.inject(this);
        super.onAttach(context);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_create_wallet, container, false);
        ButterKnife.bind(this, view);
        assert (getArguments() != null);
        presenter.handleExtras(getArguments());

        return view;
    }

    @Override
    public void setTitle(int resId) {
        title.setText(resId);
    }

    @Override
    public void setDescription(int resId) {
        description.setText(resId);
    }

    @Override
    public void setSeed(CharSequence seedPhrase) {
        seed.setText(seedPhrase);
    }

    @Override
    public void setOnSeedClickListener(View.OnClickListener listener) {
        seedLayout.setOnClickListener(listener);
    }

    @Override
    public void setOnSubmit(View.OnClickListener listener) {
        submit.setOnClickListener(v -> {
            listener.onClick(v);
            if (mOnSubmitListener != null) {
                mOnSubmitListener.onSubmit();
            }
        });
    }

    @Override
    public void showCopiedAlert() {
        AnimatorSet set = (AnimatorSet) AnimatorInflater.loadAnimator(getContext(), R.animator.fade_in_out);
        set.setTarget(alertCopied);
        set.start();
    }

    @Override
    public void setSubmitEnabled(boolean enabled) {
        submit.setEnabled(enabled);
    }

    @Override
    public void startHome() {
        KeyboardHelper.hideKeyboard(this);
        if (getActivity() == null) {
            return;
        }

        Intent intent = new Intent(getActivity(), HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_CLEAR_TASK |
                Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);

        getActivity().startActivity(intent);
        getActivity().finish();
    }

    @Override
    public void close() {
        dismiss();
    }

    @Override
    public void setOnSavedClickListener(Switch.OnCheckedChangeListener checkedChangeListener) {
        actionSavedSeed.setOnCheckedChangeListener(checkedChangeListener);
    }

    @Override
    public void addInputTextWatcher(TextWatcher textWatcher) {
        inputTitle.addTextChangedListener(textWatcher);
    }

    @Override
    public void setEnableTitleInput(boolean enable) {
        visible(inputTitle, enable);
        visible(inputTitleHint, enable);
    }

    @Override
    public void setEnableDescription(boolean enable) {
        visible(description, enable);
    }

    @Override
    public void setWalletTitle(String title) {
        inputTitle.setText(title);
    }


    public static class Builder {
        private Bundle mArgs = new Bundle();
        private OnSubmitListener mOnSubmitListener;
        private OnDismissListener mOnDismissListener;

        public Builder setOnSubmitListener(OnSubmitListener listener) {
            mOnSubmitListener = listener;
            return this;
        }

        public Builder setOnDismissListener(OnDismissListener listener) {
            mOnDismissListener = listener;
            return this;
        }

        public Builder setEnableTitleInput(boolean enable) {
            mArgs.putBoolean(EXTRA_ENABLE_TITLE_INPUT, enable);
            return this;
        }

        public Builder setWalletTitle(String title) {
            mArgs.putString(EXTRA_TITLE, title);
            return this;
        }

        public Builder setEnableDescription(boolean enable) {
            mArgs.putBoolean(EXTRA_ENABLE_DESCRIPTION, enable);
            return this;
        }

        public Builder setEnableStartHomeOnSubmit(boolean enable) {
            mArgs.putBoolean(EXTRA_ENABLE_START_HOME_ON_SUBMIT, enable);
            return this;
        }

        public CreateWalletDialog build() {
            CreateWalletDialog dialog = new CreateWalletDialog();
            dialog.setArguments(mArgs);
            dialog.setOnSubmitListener(mOnSubmitListener);
            dialog.setOnDismissListener(mOnDismissListener);
            return dialog;
        }
    }
}
