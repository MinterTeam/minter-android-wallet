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

package network.minter.bipwallet.sending.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;

import com.arellomobile.mvp.presenter.InjectPresenter;
import com.arellomobile.mvp.presenter.ProvidePresenter;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import network.minter.bipwallet.BuildConfig;
import network.minter.bipwallet.R;
import network.minter.bipwallet.advanced.models.AccountItem;
import network.minter.bipwallet.auth.ui.InputGroup;
import network.minter.bipwallet.home.HomeModule;
import network.minter.bipwallet.home.HomeTabFragment;
import network.minter.bipwallet.internal.dialogs.WalletConfirmDialog;
import network.minter.bipwallet.internal.dialogs.WalletDialog;
import network.minter.bipwallet.internal.helpers.forms.validators.RegexValidator;
import network.minter.bipwallet.sending.SendTabModule;
import network.minter.bipwallet.sending.account.AccountSelectedAdapter;
import network.minter.bipwallet.sending.account.WalletAccountSelectorDialog;
import network.minter.bipwallet.sending.views.SendTabPresenter;
import network.minter.explorerapi.MinterExplorerApi;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;

/**
 * MinterWallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@RuntimePermissions
public class SendTabFragment extends HomeTabFragment implements SendTabModule.SendView {
    @Inject Provider<SendTabPresenter> presenterProvider;
    @InjectPresenter SendTabPresenter presenter;
    @BindView(R.id.coin_input) TextInputEditText coinInput;
    @BindView(R.id.recipient_layout) TextInputLayout recipientLayout;
    @BindView(R.id.recipient_input) TextInputEditText recipientInput;
    @BindView(R.id.amount_layout) TextInputLayout amountLayout;
    @BindView(R.id.amount_input) TextInputEditText amountInput;
    @BindView(R.id.free_value) Switch freeValue;
    @BindView(R.id.action) Button actionSend;
    @BindView(R.id.action_read_qr) View actionScanQR;
    private Unbinder mUnbinder;
    private InputGroup mInputGroup;
    private WalletDialog mCurrentDialog = null;

    @Override
    public void onAttach(Context context) {
        HomeModule.getComponent().inject(this);
        super.onAttach(context);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        HomeModule.getComponent().inject(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mUnbinder.unbind();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tab_send, container, false);
        mUnbinder = ButterKnife.bind(this, view);
        mInputGroup = new InputGroup();
        mInputGroup.addInput(recipientInput);
        mInputGroup.addInput(amountInput);
        mInputGroup.addValidator(amountInput, new RegexValidator("^(\\d{0,})(\\.|\\,)?(\\d{1,18})$", "Invalid number"));
        /* ideal case */
        mInputGroup.addValidator(recipientInput,
                new RegexValidator(
                        // address or username with @ at begin or email
                        String.format("(((0|M|m)x)?([a-fA-F0-9]{40}))|(@[a-zA-Z0-9_]{5,32})|%s", Patterns.EMAIL_ADDRESS),
                        "Incorrect recipient format"
                ));

        /*
        mInputGroup.addValidator(recipientInput,
                new RegexValidator(
                        // address only for now
                        StringHelper.ADDRESS_PATTERN, "Incorrect recipient format"
                ));
                */

        recipientLayout.clearFocus();
        amountLayout.clearFocus();

        return view;
    }

    @Override
    public void setOnClickAccountSelectedListener(View.OnClickListener listener) {
        coinInput.setOnClickListener(listener);
    }

    @Override
    public void setOnTextChangedListener(InputGroup.OnTextChangedListener listener) {
        mInputGroup.addTextChangedListener(listener);
    }

    @Override
    public void setAccountName(CharSequence accountName) {
        coinInput.setText(accountName);
    }

    @Override
    public void setOnSubmit(View.OnClickListener listener) {
        actionSend.setOnClickListener(listener);
    }

    @Override
    public void setSubmitEnabled(boolean enabled) {
        actionSend.setEnabled(enabled);
    }

    @Override
    public void clearInputs() {
        recipientInput.setText(null);
        amountInput.setText(null);
        recipientLayout.clearFocus();
        amountLayout.clearFocus();
        mInputGroup.clearErrors();
    }

    @Override
    public void startDialog(WalletDialog.DialogExecutor executor) {
        mCurrentDialog = WalletDialog.switchDialogWithExecutor(this, mCurrentDialog, executor);
    }

    @Override
    public void startExplorer(String txHash) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(MinterExplorerApi.FRONT_URL + "/transactions/" + txHash)));
    }

    @Override
    public void setOnClickScanQR(View.OnClickListener listener) {
        actionScanQR.setOnClickListener(listener);
    }

    @NeedsPermission(Manifest.permission.CAMERA)
    @Override
    public void startScanQR(int requestCode) {
        Intent i = new Intent(getActivity(), QRCodeScannerActivity.class);
        getActivity().startActivityForResult(i, requestCode);
    }

    @Override
    public void startScanQRWithPermissions(int requestCode) {
        SendTabFragmentPermissionsDispatcher.startScanQRWithPermissionCheck(this, requestCode);
    }

    @Override
    public void setRecipient(CharSequence to) {
        recipientInput.setText(to);
    }

    @Override
    public void setRecipientError(CharSequence error) {
        mInputGroup.setError("recipient", error);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        presenter.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void setFormValidationListener(InputGroup.OnFormValidateListener listener) {
        mInputGroup.addFormValidateListener(listener);
    }

    @Override
    public void startAccountSelector(List<AccountItem> accounts, AccountSelectedAdapter.OnClickListener clickListener) {
        new WalletAccountSelectorDialog.Builder(getActivity(), "Select account")
                .setItems(accounts)
                .setOnClickListener(clickListener)
                .create().show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        SendTabFragmentPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @OnShowRationale(Manifest.permission.CAMERA)
    void showRationaleForCamera(final PermissionRequest request) {
        new WalletConfirmDialog.Builder(getActivity(), "Camera request")
                .setText("We need access to your camera to take a shot with Minter Address QR Code")
                .setPositiveAction("Sure", (d, w) -> {
                    request.proceed();
                    d.dismiss();
                })
                .setNegativeAction("No, I've change my mind", (d, w) -> {
                    request.cancel();
                    d.dismiss();
                }).create()
                .show();
    }

    @OnPermissionDenied(Manifest.permission.CAMERA)
    void showOpenPermissionsForCamera() {
        new WalletConfirmDialog.Builder(getActivity(), "Camera request")
                .setText("We need access to your camera to take a shot with Minter Address QR Code")
                .setPositiveAction("Open settings", (d, w) -> {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null);
                    intent.setData(uri);
                    startActivity(intent);
                    d.dismiss();
                })
                .setNegativeAction("Cancel", (d, w) -> {
                    d.dismiss();
                })
                .create()
                .show();
    }

    @ProvidePresenter
    SendTabPresenter providePresenter() {
        return presenterProvider.get();
    }
}
