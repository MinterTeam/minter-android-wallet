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

package network.minter.bipwallet.sending.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.material.textview.MaterialAutoCompleteTextView;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import moxy.presenter.InjectPresenter;
import moxy.presenter.ProvidePresenter;
import network.minter.bipwallet.BuildConfig;
import network.minter.bipwallet.R;
import network.minter.bipwallet.addressbook.models.AddressContact;
import network.minter.bipwallet.addressbook.ui.AddressBookActivity;
import network.minter.bipwallet.home.HomeModule;
import network.minter.bipwallet.home.HomeTabFragment;
import network.minter.bipwallet.internal.Wallet;
import network.minter.bipwallet.internal.dialogs.WalletConfirmDialog;
import network.minter.bipwallet.internal.dialogs.WalletDialog;
import network.minter.bipwallet.internal.helpers.ViewHelper;
import network.minter.bipwallet.internal.helpers.forms.DecimalInputFilter;
import network.minter.bipwallet.internal.helpers.forms.InputGroup;
import network.minter.bipwallet.internal.helpers.forms.validators.BaseValidator;
import network.minter.bipwallet.internal.helpers.forms.validators.ByteLengthValidator;
import network.minter.bipwallet.internal.helpers.forms.validators.EmptyValidator;
import network.minter.bipwallet.internal.helpers.forms.validators.RegexValidator;
import network.minter.bipwallet.internal.system.testing.IdlingManager;
import network.minter.bipwallet.internal.views.utils.SingleCallHandler;
import network.minter.bipwallet.sending.account.AccountSelectedAdapter;
import network.minter.bipwallet.sending.account.WalletAccountSelectorDialog;
import network.minter.bipwallet.sending.adapters.RecipientListAdapter;
import network.minter.bipwallet.sending.contract.SendView;
import network.minter.bipwallet.sending.views.SendTabPresenter;
import network.minter.bipwallet.tx.ui.ExternalTransactionActivity;
import network.minter.bipwallet.wallets.selector.WalletItem;
import network.minter.bipwallet.wallets.selector.WalletSelector;
import network.minter.core.crypto.MinterAddress;
import network.minter.core.crypto.MinterPublicKey;
import network.minter.explorer.models.CoinBalance;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@RuntimePermissions
public class SendTabFragment extends HomeTabFragment implements SendView {
    public final static String IDLE_SEND_CONFIRM_DIALOG = "IDLE_SEND_CONFIRM_DIALOG";
    public final static String IDLE_SEND_COMPLETE_DIALOG = "IDLE_SEND_COMPLETE_DIALOG";
    public final static String IDLE_SEND_WAIT_GAS = "IDLE_SEND_WAIT_GAS";

    public final static int REQUEST_CODE_QR_SCAN_TX = 2001;

    @Inject IdlingManager idlingManager;
    @Inject Provider<SendTabPresenter> presenterProvider;
    @InjectPresenter SendTabPresenter presenter;
    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.input_coin) EditText coinInput;
    @BindView(R.id.input_recipient) MaterialAutoCompleteTextView recipientInput;
    @BindView(R.id.input_amount) EditText amountInput;
    @BindView(R.id.input_payload) EditText payloadInput;
    @BindView(R.id.error_text_amount) TextView amountErrorText;
    @BindView(R.id.error_text_recipient) TextView recipientErrorText;
    @BindView(R.id.error_text_payload) TextView payloadErrorText;
    @BindView(R.id.action) Button actionSend;
    @BindView(R.id.layout_input_payload_container) View actionAddPayload;
    @BindView(R.id.action_clear_payload) View actionClearPayload;
    @BindView(R.id.action_maximum) View actionMaximum;
    @BindView(R.id.text_error) TextView errorView;

    @BindView(R.id.fee_value) TextView feeValue;
    @BindView(R.id.wallet_selector) WalletSelector walletSelector;
    @BindView(R.id.action_contacts) View actionContacts;
    private Unbinder mUnbinder;
    private InputGroup mInputGroup;
    private WalletDialog mCurrentDialog = null;
    private RecipientListAdapter mAutocompleteAdapter;

    @Override
    public void onTabSelected() {
        super.onTabSelected();
//        setStatusBarLightness(true);
        ViewHelper.setSystemBarsLightness(this, true);
        ViewHelper.setStatusBarColorAnimate(this, 0xFF_FFFFFF);
    }

    @Override
    public void onAttach(@NotNull Context context) {
        HomeModule.getComponent().inject(this);
        super.onAttach(context);
        idlingManager.add(IDLE_SEND_CONFIRM_DIALOG, IDLE_SEND_COMPLETE_DIALOG, IDLE_SEND_WAIT_GAS);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        HomeModule.getComponent().inject(this);
        super.onCreate(savedInstanceState);
        idlingManager.add(IDLE_SEND_CONFIRM_DIALOG, IDLE_SEND_COMPLETE_DIALOG, IDLE_SEND_WAIT_GAS);
    }

    @Override
    public void onDestroyView() {
        WalletDialog.releaseDialog(mCurrentDialog);
        super.onDestroyView();
        mUnbinder.unbind();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tab_send, container, false);
        mUnbinder = ButterKnife.bind(this, view);
        mInputGroup = new InputGroup();
        mInputGroup.addInput(amountInput);
        mInputGroup.addInput(recipientInput);
        mInputGroup.addInput(payloadInput);

        mInputGroup.setErrorView(amountInput, amountErrorText);
        mInputGroup.setErrorView(recipientInput, recipientErrorText);
        mInputGroup.setErrorView(payloadInput, payloadErrorText);

        mInputGroup.addValidator(amountInput, new RegexValidator("^(\\d*)(\\.)?(\\d{1,18})?$", "Invalid number", true));
        mInputGroup.addValidator(amountInput, new EmptyValidator("Value can't be empty"));
        mInputGroup.addValidator(payloadInput, new ByteLengthValidator("Message too long", false));


        /* ideal case */

//        mInputGroup.addValidator(recipientInput,
//                new RegexValidator(
//                        // address or username with @ at begin or email
//                        String.format("%s|%s|%s", MinterAddress.ADDRESS_PATTERN + "|" + MinterPublicKey.PUB_KEY_PATTERN, MinterUsernameValidator.PATTERN, Patterns.EMAIL_ADDRESS),
//                        "Incorrect recipient format"
//                ));
        mInputGroup.addValidator(recipientInput, new RecipientValidator("Invalid recipient format", true));

        mInputGroup.addFilter(amountInput, new DecimalInputFilter(() -> amountInput));

        recipientInput.clearFocus();
        amountInput.clearFocus();

        setHasOptionsMenu(true);
        getActivity().getMenuInflater().inflate(R.menu.menu_send_toolbar, toolbar.getMenu());
        toolbar.setOnMenuItemClickListener(this::onOptionsItemSelected);

        mAutocompleteAdapter = new RecipientListAdapter(getContext());
        recipientInput.setAdapter(mAutocompleteAdapter);

        return view;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_scan_tx) {
            SingleCallHandler.call(item, () -> startScanQRWithPermissions(REQUEST_CODE_QR_SCAN_TX));
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void setOnClickAccountSelectedListener(View.OnClickListener listener) {
        coinInput.setOnClickListener(listener);
    }

    @Override
    public void setOnClickMaximum(View.OnClickListener listener) {
        actionMaximum.setOnClickListener(listener);
    }

    @Override
    public void setOnClickAddPayload(View.OnClickListener listener) {
        actionAddPayload.setOnClickListener(listener);
    }

    @Override
    public void setOnClickClearPayload(View.OnClickListener listener) {
        actionClearPayload.setOnClickListener(listener);
    }

    @Override
    public void setPayloadChangeListener(TextWatcher listener) {
        payloadInput.addTextChangedListener(listener);
    }

    @Override
    public void setPayload(String payload) {
        payloadInput.setText(payload);
        payloadInput.setSelection(payload.length());
    }

    //@TODO
    @Override
    public void setActionTitle(int buttonTitle) {
        actionSend.setText(buttonTitle);
    }

    @Override
    public void startExternalTransaction(String rawData) {
        new ExternalTransactionActivity.Builder(getActivity(), rawData)
                .start();
    }

    @Override
    public void showPayload() {
        actionAddPayload.setVisibility(View.GONE);
    }

    @Override
    public void hidePayload() {
        actionAddPayload.setVisibility(View.VISIBLE);
    }

    @Override
    public void setWallets(List<WalletItem> walletItems) {
        walletSelector.setWallets(walletItems);
    }

    @Override
    public void setMainWallet(WalletItem walletItem) {
        walletSelector.setMainWallet(walletItem);
    }

    @Override
    public void setPayloadError(CharSequence errorMessage) {
        mInputGroup.setError("payload", errorMessage);
    }

    @Override
    public void setOnTextChangedListener(InputGroup.OnTextChangedListener listener) {
        mInputGroup.addTextChangedListener(listener);
    }

    @Override
    public void setOnContactsClickListener(View.OnClickListener listener) {
        actionContacts.setOnClickListener(listener);
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
        actionSend.post(() -> {
            actionSend.setEnabled(enabled);
        });
    }

    @Override
    public void clearInputs() {
        recipientInput.setText(null);
        amountInput.setText(null);
        payloadInput.setText(null);
        recipientInput.clearFocus();
        amountInput.clearFocus();
        mInputGroup.clearErrors();
    }

    @Override
    public void startDialog(WalletDialog.DialogExecutor executor) {
        mCurrentDialog = WalletDialog.switchDialogWithExecutor(this, mCurrentDialog, executor);
    }

    @Override
    public void startExplorer(String txHash) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Wallet.urlExplorerFront() + "/transactions/" + txHash)));
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
    public void startAddressBook(int requestCode) {
        new AddressBookActivity.Builder(this)
                .start(requestCode);
    }

    @Override
    public void setRecipient(AddressContact to) {
        recipientInput.post(() -> {
            recipientInput.setText(to.name);
        });
    }

    @Override
    public void setRecipientError(CharSequence error) {
        recipientInput.post(() -> {
            mInputGroup.setError("recipient", error);
        });
    }

    @Override
    public void setAmountError(CharSequence error) {
        mInputGroup.setError("amount", error);
    }

    @Override
    public void setError(CharSequence error) {
        ViewHelper.visible(errorView, error != null && error.length() > 0);
        errorView.setText(error);
    }

    @Override
    public void setError(int error) {
        ViewHelper.visible(errorView, error != 0);
        errorView.setText(error);
    }

    @Override
    public void setAmount(CharSequence amount) {
        amountInput.setText(amount);
    }

    @Override
    public void setFee(CharSequence fee) {
        feeValue.setText(fee);
    }

    @Override
    public void setRecipientAutocompleteItemClickListener(RecipientListAdapter.OnItemClickListener listener) {
        final RecipientListAdapter.OnItemClickListener cl = (item, position) -> {
            listener.onClick(item, position);
            recipientInput.dismissDropDown();
        };
        mAutocompleteAdapter.setOnItemClickListener(cl);
    }

    @Override
    public void setRecipientAutocompleteItems(List<AddressContact> items) {
        recipientInput.post(() -> {
            mAutocompleteAdapter.setItems(items);
            recipientInput.showDropDown();
        });
    }

    @Override
    public void hideAutocomplete() {
        recipientInput.post(() -> {
            recipientInput.dismissDropDown();
        });
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
    public void startAccountSelector(List<CoinBalance> accounts, AccountSelectedAdapter.OnClickListener clickListener) {
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

    private final static class RecipientValidator extends BaseValidator {
        public RecipientValidator(CharSequence errorMessage, boolean required) {
            super(errorMessage, required);
        }

        @Override
        protected boolean getCondition(CharSequence value) {
            if (value == null || value.length() == 0) {
                return false;
            }

            String v = value.toString();
            if (v.length() >= 2) {
                final String pref = v.substring(0, 2);
                if (pref.toLowerCase().equals("mx")) {
                    return v.matches(MinterAddress.ADDRESS_PATTERN);
                } else if (pref.toLowerCase().equals("mp")) {
                    return v.matches(MinterPublicKey.PUB_KEY_PATTERN);
                }
            }

            return true;
        }
    }
}
