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

package network.minter.bipwallet.security.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Provider;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricPrompt;
import androidx.fragment.app.FragmentManager;
import butterknife.BindView;
import butterknife.ButterKnife;
import dagger.android.support.AndroidSupportInjection;
import moxy.MvpAppCompatDialogFragment;
import moxy.presenter.InjectPresenter;
import network.minter.bipwallet.R;
import network.minter.bipwallet.auth.ui.AuthActivity;
import network.minter.bipwallet.internal.Wallet;
import network.minter.bipwallet.internal.views.widgets.PinCodeView;
import network.minter.bipwallet.internal.views.widgets.ToolbarProgress;
import network.minter.bipwallet.security.contract.PinValidatingView;
import network.minter.bipwallet.security.views.PinValidatingPresenter;

public class PinValidationDialog extends MvpAppCompatDialogFragment implements PinValidatingView {

    private static PinValidationDialog sInstance;
    @Inject Provider<PinValidatingPresenter> presenterProvider;
    @InjectPresenter PinValidatingPresenter presenter;
    @BindView(R.id.pinpad) PinCodeView pinCode;
    @BindView(R.id.toolbar) ToolbarProgress toolbar;
    private OnDialogDismissListener mOnDialogDismissListener;
    private boolean mShowing = false;

    public static void showIfRequired(FragmentManager fragmentManager) {
        if (!Wallet.app().secretStorage().hasPinCode()) {
            return;
        }

        if (sInstance != null && sInstance.isShowing()) {
            return;
        }

        sInstance = new PinValidationDialog();
        sInstance.setCancelable(false);
        sInstance.mOnDialogDismissListener = () -> releaseDialog(sInstance);
        sInstance.show(fragmentManager, "pin-validation");
    }

    public static void releaseDialog(PinValidationDialog dialog) {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidSupportInjection.inject(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if (mOnDialogDismissListener != null) {
            mOnDialogDismissListener.onDismiss();
        }
    }

    public boolean isShowing() {
        return mShowing;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = new Dialog(requireContext(), R.style.Wallet_DialogActivity);

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        if (dialog.getWindow() != null) {
            WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;

            dialog.getWindow().setAttributes(params);

//            dialog.getWindow().setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        return dialog;
    }

    @Override
    public void onResume() {
        super.onResume();
        mShowing = true;
    }

    @Override
    public void onPause() {
        super.onPause();
        mShowing = false;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        AndroidSupportInjection.inject(this);
        super.onAttach(context);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_pin, container, false);
        ButterKnife.bind(this, view);

        return view;
    }

    @Override
    public void setOnPinValueListener(PinCodeView.OnValueListener listener) {
        pinCode.setOnValueListener(listener);
    }

    @Override
    public void setupTitle(int title) {
        toolbar.setTitle(title);
    }

    @Override
    public void setPinHint(int resId) {
        pinCode.setPinHint(resId);
    }

    @Override
    public void setFingerprintEnabled(boolean enabled) {
        pinCode.setEnableFingerprint(enabled);
    }

    @Override
    public void setOnFingerprintClickListener(PinCodeView.OnFingerprintClickListener listener) {
        pinCode.setOnFingerprintClickListener(listener);
    }

    @Override
    public void startBiometricPrompt(BiometricPrompt.AuthenticationCallback callback) {
        final BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.pin_fp_title_enable))
                .setDescription("")
                .setSubtitle("")
                .setNegativeButtonText(getString(R.string.btn_cancel))
                .build();

        Executor executor = Executors.newSingleThreadExecutor();
        final BiometricPrompt prompt = new BiometricPrompt(this, executor, callback);

        prompt.authenticate(info);
    }

    @Override
    public void setEnableValidation(String pin) {
        pinCode.setValue(pin);
    }

    @Override
    public void setOnPinValidationError(PinCodeView.OnValidationErrorListener listener) {
        pinCode.setOnValidationErrorListener(listener);
    }

    @Override
    public void finishSuccess() {
        dismiss();
    }

    @Override
    public void setPinError(CharSequence error) {
        pinCode.setError(error);
    }

    @Override
    public void setPinError(int errorRes) {
        pinCode.setError(errorRes);
    }

    @Override
    public void setPinEnabled(boolean enabled) {
        pinCode.setEnabled(enabled);
    }

    @Override
    public void resetPin() {
        pinCode.reset();
    }

    @Override
    public void startLogin() {
        Toast.makeText(getActivity(), R.string.pin_invalid_logout, Toast.LENGTH_LONG).show();

        Intent intent = new Intent(getActivity(), AuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(intent);
        dismiss();
    }

    public interface OnDialogDismissListener {
        void onDismiss();
    }
}
