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

package network.minter.bipwallet.sending.views;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.View;
import android.widget.EditText;

import com.arellomobile.mvp.InjectViewState;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import network.minter.bipwallet.R;
import network.minter.bipwallet.advanced.models.AccountItem;
import network.minter.bipwallet.advanced.models.SecretData;
import network.minter.bipwallet.advanced.models.UserAccount;
import network.minter.bipwallet.advanced.repo.AccountStorage;
import network.minter.bipwallet.advanced.repo.SecretStorage;
import network.minter.bipwallet.apis.explorer.CachedExplorerTransactionRepository;
import network.minter.bipwallet.internal.Wallet;
import network.minter.bipwallet.internal.data.CacheManager;
import network.minter.bipwallet.internal.data.CachedRepository;
import network.minter.bipwallet.internal.dialogs.WalletConfirmDialog;
import network.minter.bipwallet.internal.dialogs.WalletProgressDialog;
import network.minter.bipwallet.internal.exceptions.MyResponseException;
import network.minter.bipwallet.internal.mvp.MvpBasePresenter;
import network.minter.bipwallet.sending.SendTabModule;
import network.minter.bipwallet.sending.dialogs.WalletTxStartDialog;
import network.minter.bipwallet.sending.dialogs.WalletTxSuccessDialog;
import network.minter.bipwallet.sending.dialogs.WalletTxWaitingDialog;
import network.minter.bipwallet.sending.ui.QRCodeScannerActivity;
import network.minter.blockchainapi.models.BCResult;
import network.minter.blockchainapi.models.operational.Transaction;
import network.minter.blockchainapi.models.operational.TransactionSign;
import network.minter.blockchainapi.models.operational.TxSendCoin;
import network.minter.blockchainapi.repo.BlockChainAccountRepository;
import network.minter.explorerapi.models.HistoryTransaction;
import network.minter.mintercore.MinterSDK;
import network.minter.mintercore.crypto.BytesData;
import network.minter.mintercore.crypto.MinterAddress;
import network.minter.my.models.MyResult;
import network.minter.my.repo.MyInfoRepository;
import timber.log.Timber;

import static network.minter.bipwallet.internal.ReactiveAdapter.convertToBcErrorResult;
import static network.minter.bipwallet.internal.ReactiveAdapter.convertToMyErrorResult;
import static network.minter.bipwallet.internal.ReactiveAdapter.rxCallBc;
import static network.minter.bipwallet.internal.ReactiveAdapter.rxCallMy;
import static network.minter.mintercore.MinterSDK.PREFIX_TX;

/**
 * MinterWallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@InjectViewState
public class SendTabPresenter extends MvpBasePresenter<SendTabModule.SendView> {
    private static final int REQUEST_CODE_QR_SCAN = 101;
    @Inject SecretStorage secretStorage;
    @Inject CachedRepository<UserAccount, AccountStorage> accountStorage;
    @Inject CachedRepository<List<HistoryTransaction>, CachedExplorerTransactionRepository> txRepo;
    @Inject BlockChainAccountRepository accountRepo;
    @Inject MyInfoRepository infoRepo;
    @Inject CacheManager cache;
    private AccountItem mFromAccount = null;
    private CharSequence mAmount = null;
    private CharSequence mToAddress = null;
    private CharSequence mToName = null;
    private String mAvatar = null;

    @Inject
    public SendTabPresenter() {
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        if (requestCode == REQUEST_CODE_QR_SCAN) {
            if (data != null && data.hasExtra(QRCodeScannerActivity.RESULT_TEXT)) {
                //Getting the passed result
                String result = data.getStringExtra(QRCodeScannerActivity.RESULT_TEXT);
                Timber.d("QR Code scan result: %s", result);
                try {
                    mToAddress = new MinterAddress(result).toString();
                    getViewState().setRecipient(mToAddress);
                } catch (Throwable ignore) {
                }
            }
        }
    }

    @Override
    public void attachView(SendTabModule.SendView view) {
        super.attachView(view);
        getViewState().setOnClickAccountSelectedListener(this::onClickAccountSelector);
        getViewState().setOnTextChangedListener(this::onInputTextChanged);
        getViewState().setOnSubmit(this::onSubmit);
        getViewState().setOnClickScanQR(this::onScanQR);
        accountStorage.update();
    }

    @Override
    protected void onFirstViewAttach() {
        super.onFirstViewAttach();
        safeSubscribeIoToUi(accountStorage.observe())
                .subscribe(res -> {
                    if (!res.isEmpty()) {
                        mFromAccount = res.getAccounts().get(0);
                        getViewState().setAccountName(String.format("%s (%s)", mFromAccount.coin.toUpperCase(), mFromAccount.balance.toString()));
                    }
                });

        getViewState().setSubmitEnabled(false);
        getViewState().setFormValidationListener(valid -> getViewState().setSubmitEnabled(valid));
    }

    private void onScanQR(View view) {
        getViewState().startScanQRWithPermissions(REQUEST_CODE_QR_SCAN);
    }

    private void onSubmit(View view) {
        if (mToName == null) {
            getViewState().setRecipientError("Invalid recipient");
            return;
        }
        if (mToAddress != null) {
            startSendDialog();
            return;
        }

        if (mToName.toString().substring(0, 2).equals(MinterSDK.PREFIX_ADDRESS)) {
            mToAddress = mToName;
            startSendDialog();
            return;
        }

        getViewState().startDialog(ctx -> {
            rxCallMy(infoRepo.findAddressByInput(mToName.toString()))
                    .delay(150, TimeUnit.MILLISECONDS)
                    .onErrorResumeNext(convertToMyErrorResult())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(result -> {
                        if (result.isSuccess()) {
                            mAvatar = result.data.user.getAvatar().getUrl();
                            mToAddress = result.data.address.toString();
                            getViewState().setRecipientError(null);
                            startSendDialog();
                        } else {
                            mToAddress = null;
                            onErrorSearchUser(result);
                            Timber.d(new MyResponseException(result), "Unable to find address");
                        }
                    }, Wallet.Rx.errorHandler());

            return new WalletProgressDialog.Builder(ctx, "Searching address")
                    .setText(String.format("Please, wait, we are searching address for user \"%s\"", mToName))
                    .create();
        });
    }

    private void startSendDialog() {
        getViewState().startDialog(ctx -> {
            final WalletTxStartDialog dialog = new WalletTxStartDialog.Builder(ctx, "You're sending")
                    .setAmount(new BigDecimal(mAmount.toString()))
                    .setAvatarUrl(mAvatar)
                    .setRecipientName(mToName)
                    .setCoin(mFromAccount.coin)
                    .setPositiveAction("BIP!", this::onStartWaitingDialog)
                    .setNegativeAction("Cancel", null)
                    .create();
            dialog.setCancelable(true);
            return dialog;
        });
    }

    private void onStartWaitingDialog(DialogInterface dialogInterface, int which) {
        getViewState().startDialog(ctx -> {
            final WalletTxWaitingDialog dialog = new WalletTxWaitingDialog.Builder(ctx, "Please wait")
                    .setCountdownSeconds(10, (tick, isEnd) -> {
                        if (isEnd) {
                            onStartExecuteTransaction(false);
                        }
                    })
                    .setPositiveAction("Express transaction", (d, v) -> {
                        onStartExecuteTransaction(true);
                    })
                    .create();
            dialog.setCancelable(false);
            return dialog;
        });
    }

    private void onStartExecuteTransaction(boolean express) {
        getViewState().startDialog(ctx -> {
            final WalletProgressDialog dialog = new WalletProgressDialog.Builder(ctx, "Please wait")
                    .setText("Sending transaction...")
                    .create();
            dialog.setCancelable(false);

            safeSubscribeIoToUi(rxCallBc(accountRepo.getTransactionCount(mFromAccount.address)))
                    .onErrorResumeNext(convertToBcErrorResult())
                    .switchMap(cntRes -> {
                        if (!cntRes.isSuccess()) {
                            return Observable.just(BCResult.<BytesData>copyError(cntRes));
                        }

                        final Transaction<TxSendCoin> tx = Transaction.newSendTransaction(cntRes.result.add(new BigInteger("1")))
                                .setCoin(mFromAccount.coin)
                                .setTo(mToAddress)
                                .setValue(mAmount)
                                .build();

                        final SecretData data = secretStorage.getSecret(mFromAccount.address);
                        final TransactionSign sign = tx.sign(data.getPrivateKey());

                        return safeSubscribeIoToUi(rxCallBc(accountRepo.sendTransaction(sign)))
                                .onErrorResumeNext(convertToBcErrorResult());

                    }).subscribe(this::onSuccessExecuteTransaction, Wallet.Rx.errorHandler(getViewState()));

            return dialog;
        });
    }

    private void onErrorSearchUser(MyResult<?> errorResult) {
        Timber.e(errorResult.getError().message, "Unable to find address");
        getViewState().startDialog(ctx -> new WalletConfirmDialog.Builder(ctx, errorResult.getError().message)
                .setText(String.format("Unable to find user address for user \"%s\"", mToName))
                .setPositiveAction("Close")
                .create());
    }

    private void onErrorExecuteTransaction(BCResult<?> errorResult) {
        Timber.e(errorResult.message, "Unable to send transaction");
        getViewState().startDialog(ctx -> new WalletConfirmDialog.Builder(ctx, "Unable to send transaction")
                .setText(errorResult.message)
                .setPositiveAction("Close")
                .create());
    }

    private void onSuccessExecuteTransaction(final BCResult<BytesData> result) {
        if (!result.isSuccess()) {
            onErrorExecuteTransaction(result);
            return;
        }

        accountStorage.update(true);
        txRepo.update(true);
        getViewState().startDialog(ctx -> new WalletTxSuccessDialog.Builder(ctx, "Success!")
                .setRecipientName(mToName)
                .setAvatar(mAvatar)
                .setPositiveAction("View transaction", (d, v) -> {
                    getViewState().startExplorer(result.result.toHexString(PREFIX_TX));
                    d.dismiss();
                })
                .setNegativeAction("Close", null)
                .create());

        getViewState().clearInputs();
    }

    private void onInputTextChanged(EditText editText, boolean valid) {
        switch (editText.getId()) {
            case R.id.recipient_input:
                mToName = editText.getText();
                mToAddress = null;
                break;
            case R.id.amount_input:
                mAmount = editText.getText();
                break;
        }
    }

    private void onClickAccountSelector(View view) {
        getViewState().startAccountSelector(accountStorage.getData().getAccounts(), this::onSelectAccount);
    }

    private void onSelectAccount(AccountItem accountItem) {
        mFromAccount = accountItem;
        getViewState().setAccountName(String.format("%s (%s)", accountItem.coin.toUpperCase(), accountItem.balance.toString()));
    }


}
