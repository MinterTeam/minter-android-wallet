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

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
import network.minter.bipwallet.internal.dialogs.BaseBottomSheetDialog;
import network.minter.bipwallet.wallets.contract.AddWalletView;
import network.minter.bipwallet.wallets.dialogs.presentation.AddWalletPresenter;

import static network.minter.bipwallet.internal.helpers.ViewHelper.visible;

public class AddWalletDialog extends BaseBottomSheetDialog implements AddWalletView {

    @Inject Provider<AddWalletPresenter> presenterProvider;
    @InjectPresenter AddWalletPresenter presenter;

    @BindView(R.id.dialog_title) TextView title;
    @BindView(R.id.input_seed) TextInputEditText inputSeed;
    @BindView(R.id.input_title) TextInputEditText inputTitle;
    @BindView(R.id.error_text) TextView errorText;
    @BindView(R.id.action_generate) Button actionGenerate;
    @BindView(R.id.action_submit) View actionSubmit;
    private OnGenerateNewWalletListener mOnGenerateNewWalletListener;

    public static AddWalletDialog newInstance() {
        return new AddWalletDialog();
    }

    public void setOnGenerateNewWalletListener(OnGenerateNewWalletListener listener) {
        mOnGenerateNewWalletListener = listener;
    }

    @ProvidePresenter
    public AddWalletPresenter providePresenter() {
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
        View view = inflater.inflate(R.layout.dialog_add_wallet, container, false);
        ButterKnife.bind(this, view);

        actionGenerate.setOnClickListener(v -> {
            close();
            if (mOnGenerateNewWalletListener != null) {
                final Editable s = inputTitle.getText();
                final String title = (s != null && s.length() > 0) ? s.toString() : null;
                mOnGenerateNewWalletListener.onGenerateNewWallet(mOnSubmitListener, mOnDismissListener, title);
            }
        });

        return view;
    }

    @Override
    public void setOnSubmit(View.OnClickListener listener) {
        actionSubmit.setOnClickListener(v -> {
            listener.onClick(v);
            if (mOnSubmitListener != null) {
                mOnSubmitListener.onSubmit();
            }
        });
    }

    @Override
    public void setSubmitEnabled(boolean enabled) {
        actionSubmit.setEnabled(enabled);
    }

    @Override
    public void addSeedInputTextListener(TextWatcher textWatcher) {
        inputSeed.addTextChangedListener(textWatcher);
    }

    @Override
    public void addTitleInputTextListener(TextWatcher textWatcher) {
        inputTitle.addTextChangedListener(textWatcher);
    }

    @Override
    public void setError(CharSequence error) {
        visible(errorText, error != null && error.length() > 0);
        errorText.setText(error);
    }

    @Override
    public void close() {
        dismiss();
    }

    @Override
    public void setEnableSubmit(boolean enable) {
        actionSubmit.setEnabled(enable);
    }

    public interface OnGenerateNewWalletListener {
        void onGenerateNewWallet(@Nullable OnSubmitListener submitListener, @Nullable OnDismissListener dismissListener, @Nullable String title);
    }
}
