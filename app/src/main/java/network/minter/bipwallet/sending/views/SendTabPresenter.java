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

package network.minter.bipwallet.sending.views;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.View;
import android.widget.EditText;

import com.annimon.stream.Optional;
import com.annimon.stream.Stream;
import com.arellomobile.mvp.InjectViewState;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import network.minter.bipwallet.R;
import network.minter.bipwallet.advanced.models.AccountItem;
import network.minter.bipwallet.advanced.models.SecretData;
import network.minter.bipwallet.advanced.models.UserAccount;
import network.minter.bipwallet.advanced.repo.AccountStorage;
import network.minter.bipwallet.advanced.repo.SecretStorage;
import network.minter.bipwallet.analytics.AppEvent;
import network.minter.bipwallet.apis.explorer.CachedExplorerTransactionRepository;
import network.minter.bipwallet.internal.Wallet;
import network.minter.bipwallet.internal.data.CacheManager;
import network.minter.bipwallet.internal.data.CachedRepository;
import network.minter.bipwallet.internal.dialogs.WalletConfirmDialog;
import network.minter.bipwallet.internal.dialogs.WalletProgressDialog;
import network.minter.bipwallet.internal.exceptions.ProfileResponseException;
import network.minter.bipwallet.internal.mvp.MvpBasePresenter;
import network.minter.bipwallet.sending.SendTabModule;
import network.minter.bipwallet.sending.models.RecipientItem;
import network.minter.bipwallet.sending.repo.RecipientAutocompleteStorage;
import network.minter.bipwallet.sending.ui.QRCodeScannerActivity;
import network.minter.bipwallet.sending.ui.dialogs.WalletTxSendStartDialog;
import network.minter.bipwallet.sending.ui.dialogs.WalletTxSendSuccessDialog;
import network.minter.bipwallet.sending.ui.dialogs.WalletTxSendWaitingDialog;
import network.minter.blockchain.models.BCResult;
import network.minter.blockchain.models.CountableData;
import network.minter.blockchain.models.TransactionCommissionValue;
import network.minter.blockchain.models.TransactionSendResult;
import network.minter.blockchain.models.operational.OperationInvalidDataException;
import network.minter.blockchain.models.operational.OperationType;
import network.minter.blockchain.models.operational.Transaction;
import network.minter.blockchain.models.operational.TransactionSign;
import network.minter.blockchain.repo.BlockChainTransactionRepository;
import network.minter.core.MinterSDK;
import network.minter.core.crypto.MinterAddress;
import network.minter.explorer.models.BCExplorerResult;
import network.minter.explorer.models.HistoryTransaction;
import network.minter.explorer.repo.ExplorerCoinsRepository;
import network.minter.profile.MinterProfileApi;
import network.minter.profile.models.ProfileResult;
import network.minter.profile.repo.ProfileInfoRepository;
import timber.log.Timber;

import static network.minter.bipwallet.internal.ReactiveAdapter.convertToBcExpErrorResult;
import static network.minter.bipwallet.internal.ReactiveAdapter.convertToProfileErrorResult;
import static network.minter.bipwallet.internal.ReactiveAdapter.createBcExpErrorResultMessage;
import static network.minter.bipwallet.internal.ReactiveAdapter.rxCallBcExp;
import static network.minter.bipwallet.internal.ReactiveAdapter.rxCallProfile;
import static network.minter.bipwallet.internal.helpers.MathHelper.bdGT;
import static network.minter.bipwallet.internal.helpers.MathHelper.bdGTE;
import static network.minter.bipwallet.internal.helpers.MathHelper.bdHuman;
import static network.minter.bipwallet.internal.helpers.MathHelper.bdLT;
import static network.minter.bipwallet.internal.helpers.MathHelper.bdLTE;
import static network.minter.bipwallet.internal.helpers.MathHelper.bdNull;
import static network.minter.bipwallet.internal.helpers.MathHelper.bigDecimalFromString;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@InjectViewState
public class SendTabPresenter extends MvpBasePresenter<SendTabModule.SendView> {
    private static final int REQUEST_CODE_QR_SCAN = 101;
    @Inject SecretStorage secretStorage;
    @Inject CachedRepository<UserAccount, AccountStorage> accountStorage;
    @Inject
    CachedRepository<List<HistoryTransaction>, CachedExplorerTransactionRepository> cachedTxRepo;
    @Inject ExplorerCoinsRepository coinRepo;
    @Inject BlockChainTransactionRepository bcTxRepo;
    @Inject ProfileInfoRepository infoRepo;
    @Inject CacheManager cache;
    @Inject RecipientAutocompleteStorage recipientStorage;
    private AccountItem mFromAccount = null;
    private BigDecimal mAmount = null;
    private CharSequence mToAddress = null;
    private CharSequence mToName = null;
    private String mAvatar = null;
    private AtomicBoolean mEnableUseMax = new AtomicBoolean(false);
    private BehaviorSubject<BigDecimal> mInputChange;
    private String mGasCoin = MinterSDK.DEFAULT_COIN;
    private AccountItem mLastAccount = null;

    private enum SearchByType {
        Address, Username, Email
    }

    @Inject
    public SendTabPresenter() {
    }

    @Override
    public void attachView(SendTabModule.SendView view) {
        super.attachView(view);
        getViewState().setOnClickAccountSelectedListener(this::onClickAccountSelector);
        getViewState().setOnTextChangedListener(this::onInputTextChanged);
        getViewState().setOnSubmit(this::onSubmit);
        getViewState().setOnClickScanQR(this::onClickScanQR);
        getViewState().setOnClickMaximum(this::onClickMaximum);
        getViewState().setFee(String.format("%s %s", bdHuman(OperationType.SendCoin.getFee()), MinterSDK.DEFAULT_COIN.toUpperCase()));
        accountStorage.update();
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
    protected void onFirstViewAttach() {
        super.onFirstViewAttach();
        accountStorage.observe()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(res -> {
                    if (!res.isEmpty()) {
                        if (mLastAccount != null) {
                            onAccountSelected(res.findAccountByCoin(mLastAccount.getCoin()).orElse(res.getAccounts().get(0)));
                        } else {
                            onAccountSelected(res.getAccounts().get(0));
                        }
                    }
                }, t -> {
                    getViewState().onError(t);
                });

        mInputChange = BehaviorSubject.create();
        unsubscribeOnDestroy(mInputChange
                .toFlowable(BackpressureStrategy.LATEST)
                .subscribe(this::onAmountChanged));

        setRecipientAutocomplete();
        getViewState().setSubmitEnabled(false);
        getViewState().setFormValidationListener(valid -> getViewState().setSubmitEnabled(valid && checkZero(mAmount)));
    }

    private void setRecipientAutocomplete() {
        if (true) {
            // FIXME some cases working wrong, this task is low priority, so just disable it for now
            return;
        }
        recipientStorage.getItems()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(res -> getViewState().setRecipientsAutocomplete(res, (item, position) -> getViewState().setRecipient(item.getName())));
    }

    private void checkEnoughBalance(BigDecimal amount) {
        boolean enough = bdGT(amount, OperationType.SendCoin.getFee());
        if (!enough) {
            getViewState().setAmountError("Insufficient balance");
        } else {
            getViewState().setAmountError(null);
        }
        getViewState().setSubmitEnabled(enough);
    }

    private boolean checkZero(BigDecimal amount) {
        boolean valid = amount == null || !bdNull(amount);
        if (!valid) {
            getViewState().setAmountError("Amount must be greater than 0");
        } else {
            getViewState().setAmountError(null);
        }

        return valid;
    }

    private void onAmountChanged(BigDecimal amount) {
        checkZero(amount);
        mEnableUseMax.set(false);
    }

    private void onClickScanQR(View view) {
        getAnalytics().send(AppEvent.SendCoinsQRButton);

        getViewState().startScanQRWithPermissions(REQUEST_CODE_QR_SCAN);
    }

    private void onSubmit(View view) {
        getAnalytics().send(AppEvent.SendCoinsSendButton);
        //TODO check this hell
        if (mToName == null) {
            getViewState().setRecipientError("Invalid recipient");
            return;
        }
        if (mToAddress != null && mToName == null) {
            resolveUserInfo(mToAddress.toString(), false);
            return;
        }

        if (mToName.toString().substring(0, 2).equals(MinterSDK.PREFIX_ADDRESS)) {
            mToAddress = mToName;
            resolveUserInfo(mToName.toString(), false);
            return;
        }

        resolveUserInfo(mToName.toString(), true);
    }

    private SearchByType getSearchByType(String input) {
        if (input.substring(0, 2).equals(MinterSDK.PREFIX_ADDRESS) && input.length() == 42) {
            // searching data by address
            return SearchByType.Address;
        } else if (input.substring(0, 1).equals("@")) {
            // searching data by username
            return SearchByType.Username;
        } else {
            // searching by email
            return SearchByType.Email;
        }
    }

    private void resolveUserInfo(final String searchBy, final boolean failOnNotFound) {
        getViewState().startDialog(ctx -> {
            rxCallProfile(infoRepo.findAddressInfoByInput(searchBy))
                    .delay(150, TimeUnit.MILLISECONDS)
                    .onErrorResumeNext(convertToProfileErrorResult())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(result -> {
                        if (result.isSuccess()) {
                            mAvatar = result.data.user.getAvatar().getUrl();
                            mToAddress = result.data.address.toString();
                            final SearchByType nameType = getSearchByType(searchBy);
                            switch (nameType) {
                                case Email:
                                    mToName = result.data.user.email;
                                    break;
                                case Username:
                                    mToName = String.format("@%s", result.data.user.username);
                                    break;
                                case Address:
                                    mToName = result.data.address.toString();
                                    break;
                            }

                            getViewState().setRecipientError(null);
                            startSendDialog();
                        } else {
                            if (mToAddress != null) {
                                mAvatar = MinterProfileApi.getUserAvatarUrlByAddress(mToAddress.toString());
                            } else {
                                mAvatar = MinterProfileApi.getUserAvatarUrl(1);
                            }

                            if (failOnNotFound) {
                                mToAddress = null;
                                onErrorSearchUser(result);
                                Timber.d(new ProfileResponseException(result), "Unable to find address");
                            } else {
                                getViewState().setRecipientError(null);
                                startSendDialog();
                            }
                        }
                    }, Wallet.Rx.errorHandler(getViewState()));

            return new WalletProgressDialog.Builder(ctx, R.string.tx_address_searching)
                    .setText(String.format("Please, wait, we are searching address for user \"%s\"", searchBy))
                    .create();
        });
    }

    private void startSendDialog() {
        getViewState().startDialog(ctx -> {
            try {
                getAnalytics().send(AppEvent.SendCoinPopupScreen);
                final WalletTxSendStartDialog dialog = new WalletTxSendStartDialog.Builder(ctx, R.string.tx_send_overall_title)
                        .setAmount(mAmount)
                        .setAvatarUrl(mAvatar)
                        .setRecipientName(mToName)
                        .setCoin(mFromAccount.coin)
                        .setPositiveAction(R.string.btn_send, (d, w) -> {
                            Wallet.app().sounds().play(R.raw.bip_beep_digi_octave);
                            onStartExecuteTransaction(true);
                            getAnalytics().send(AppEvent.SendCoinPopupSendButton);
                            d.dismiss();
                        })
                        .setNegativeAction(R.string.btn_cancel, (d, w) -> {
                            Wallet.app().sounds().play(R.raw.cancel_pop_hi);
                            getAnalytics().send(AppEvent.SendCoinPopupCancelButton);
                            d.dismiss();
                        })
                        .create();
                dialog.setCancelable(true);
                return dialog;
            } catch (NullPointerException badState) {
                return new WalletConfirmDialog.Builder(ctx, R.string.error)
                        .setText(badState.getMessage())
                        .setPositiveAction(R.string.btn_close)
                        .create();
            }

        });
    }

    /**
     * Unused now, soon
     * @param dialogInterface
     * @param which
     */
    private void onStartWaitingDialog(DialogInterface dialogInterface, int which) {
        getViewState().startDialog(ctx -> {
            final WalletTxSendWaitingDialog dialog = new WalletTxSendWaitingDialog.Builder(ctx, R.string.please_wait)
                    .setCountdownSeconds(10, (tick, isEnd) -> {
                        if (isEnd) {
                            onStartExecuteTransaction(false);
                        }
                    })
                    .setPositiveAction(R.string.btn_express_tx, (d, v) -> {
                        onStartExecuteTransaction(true);
                    })
                    .create();
            dialog.setCancelable(false);
            return dialog;
        });
    }

    private void onClickMaximum(View view) {
        if (mFromAccount == null) {
            getViewState().setError("Account didn't loaded yet...");
            return;
        }
        mEnableUseMax.set(true);
        checkEnoughBalance(mFromAccount.getBalanceBase());
        mAmount = mFromAccount.getBalance();
        getViewState().setAmount(mFromAccount.getBalance().stripTrailingZeros().toPlainString());

        getAnalytics().send(AppEvent.SendCoinsUseMaxButton);
    }

    private Optional<AccountItem> findAccountByCoin(String coin) {
        return Stream.of(accountStorage.getData().getAccounts())
                .filter(item -> item.getCoin().equals(coin.toUpperCase()))
                .findFirst();
    }

    private void onStartExecuteTransaction(boolean express) {
        getViewState().startDialog(ctx -> {
            final WalletProgressDialog dialog = new WalletProgressDialog.Builder(ctx, R.string.please_wait)
                    .setText(R.string.tx_send_in_progress)
                    .create();
            dialog.setCancelable(false);

            Optional<AccountItem> mntAccount = findAccountByCoin(MinterSDK.DEFAULT_COIN);
            Optional<AccountItem> sendAccount = findAccountByCoin(mFromAccount.getCoin());

            // default coin for pay fee - MNT (base coin)
            final BCExplorerResult<TransactionCommissionValue> val = new BCExplorerResult<>();
            val.result = new TransactionCommissionValue();
            val.result.value = OperationType.SendCoin.getFee().multiply(Transaction.VALUE_MUL_DEC).toBigInteger();
            Observable<BCExplorerResult<TransactionCommissionValue>> exchangeResolver = Observable.just(val);

            final boolean enoughBaseCoinForCommission = bdGTE(mntAccount.get().getBalance(), OperationType.SendCoin.getFee());

            // if enough balance on MNT account, set gas coin MNT (BIP)
            if (enoughBaseCoinForCommission) {
                Timber.d("Enough balance in MNT to pay fee");
                Timber.tag("TX Send").d("Resolving base coin commission %s", MinterSDK.DEFAULT_COIN);
                mGasCoin = mntAccount.get().getCoin();
            }
            // if sending account is not MNT (BIP), set gas coin CUSTOM
            else if (!sendAccount.get().getCoin().equals(MinterSDK.DEFAULT_COIN) && !enoughBaseCoinForCommission) {
                Timber.d("Not enough balance in MNT to pay fee, using " + mFromAccount.getCoin());
                mGasCoin = sendAccount.get().getCoin();
                // otherwise getting
                Timber.tag("TX Send").d("Resolving custom coin commission %s", mFromAccount.getCoin());
                // resolving fee currency for custom currency
                // creating tx
                try {
                    final Transaction preTx = new Transaction.Builder(new BigInteger("1"))
                            .setGasCoin(mGasCoin)
                            .sendCoin()
                            .setCoin(mFromAccount.coin)
                            .setTo(mToAddress)
                            .setValue(mAmount)
                            .build();

                    final SecretData preData = secretStorage.getSecret(mFromAccount.address);
                    final TransactionSign preSign = preTx.sign(preData.getPrivateKey());

                    exchangeResolver = rxCallBcExp(cachedTxRepo.getEntity().getTransactionCommission(preSign)).onErrorResumeNext(convertToBcExpErrorResult());
                } catch (OperationInvalidDataException e) {
                    Timber.w(e);
                    final BCExplorerResult<TransactionCommissionValue> commissionValue = new BCExplorerResult<>();
                    val.result.value = OperationType.SendCoin.getFee().multiply(Transaction.VALUE_MUL_DEC).toBigInteger();
                    exchangeResolver = Observable.just(commissionValue);
                }
            }

            // creating preparation result to send transaction
            Disposable d = Observable.combineLatest(
                    exchangeResolver,
                    rxCallBcExp(cachedTxRepo.getEntity().getTransactionCount(mFromAccount.address)).onErrorResumeNext(convertToBcExpErrorResult()),
                    new BiFunction<BCExplorerResult<TransactionCommissionValue>, BCExplorerResult<CountableData>, SendInitData>() {
                        @Override
                        public SendInitData apply(BCExplorerResult<TransactionCommissionValue> txCommissionValue, BCExplorerResult<CountableData> countableDataBCResult) {

                            // if some request failed, returning error result
                            if (!txCommissionValue.isSuccess()) {
                                return new SendInitData(BCExplorerResult.copyError(txCommissionValue));
                            } else if (!countableDataBCResult.isSuccess()) {
                                return new SendInitData(BCExplorerResult.copyError(countableDataBCResult));
                            }

                            Timber.d("SendInitData: tx commission: %s", txCommissionValue.result.getValue());

                            final SendInitData data = new SendInitData(
                                    countableDataBCResult.result.count.add(new BigInteger("1")),
                                    txCommissionValue.result.getValue()
                            );
                            Timber.tag("TX Send").d("Resolved: coin %s commission=%s; nonce=%s", mFromAccount.getCoin(), data.commission, data.nonce);

                            // creating preparation data
                            return data;
                        }
                    })
                    .switchMap((Function<SendInitData, ObservableSource<BCExplorerResult<TransactionSendResult>>>) cntRes -> {
                        // if in previous request we've got error, returning it
                        if (!cntRes.isSuccess()) {
                            return Observable.just(BCExplorerResult.copyError(cntRes.errorResult));
                        }

                        final BigDecimal amountToSend;

                        // don't calc fee if enough balance in base coin and we are sending not a base coin (MNT or BIP)
                        if (enoughBaseCoinForCommission && !mFromAccount.getCoin().equals(MinterSDK.DEFAULT_COIN)) {
                            cntRes.commission = new BigDecimal(0);
                        }

                        // if balance enough to send required sum + fee, do nothing
                        if (bdLTE(mAmount.add(cntRes.commission), mFromAccount.getBalance())) {
                            Timber.tag("TX Send").d("Don't change sending amount - balance enough to send");
                            amountToSend = mAmount;
                        }
                        // if balance not enough to send required sum + fee - subtracting fee from sending sum ("use max" for example)
                        else {
                            amountToSend = mAmount.subtract(cntRes.commission);
                            Timber.tag("TX Send").d("Subtracting sending amount (-%s): balance not enough to send", cntRes.commission);
                        }

                        // if after subtracting fee from sending sum has become less than account balance at all, returning error with message "insufficient funds"
                        if (bdLT(amountToSend, 0)) {
                            final BigDecimal diff = mAmount.subtract(mFromAccount.getBalance());
                            BCExplorerResult<TransactionSendResult> errorRes = createBcExpErrorResultMessage(
                                    String.format("Insufficient funds: need %s %s", diff.toPlainString(), mFromAccount.coin.toUpperCase()),
                                    BCResult.ResultCode.InsufficientFunds,
                                    400
                            );
                            return Observable.just(errorRes);
                        }

                        Timber.tag("TX Send").d("Send data: gasCoin=%s, coin=%s, to=%s, amount=%s",
                                mFromAccount.getCoin(),
                                mFromAccount.getCoin(),
                                mToAddress,
                                amountToSend
                        );
                        // creating tx
                        final Transaction tx = new Transaction.Builder(cntRes.nonce)
                                .setGasCoin(mGasCoin)
                                .sendCoin()
                                .setCoin(mFromAccount.coin)
                                .setTo(mToAddress)
                                .setValue(amountToSend)
                                .build();

                        final SecretData data = secretStorage.getSecret(mFromAccount.address);
                        final TransactionSign sign = tx.signSingle(data.getPrivateKey());

                        return safeSubscribeIoToUi(
                                rxCallBcExp(cachedTxRepo.getEntity().sendTransaction(sign))
                                        .onErrorResumeNext(convertToBcExpErrorResult())
                        );

                    }).subscribe(this::onSuccessExecuteTransaction, this::onFailedExecuteTransaction);
            unsubscribeOnDestroy(d);

            return dialog;
        });
    }

    private void onFailedExecuteTransaction(final Throwable throwable) {
        Timber.w(throwable, "Uncaught tx error");
        getViewState().startDialog(ctx -> new WalletConfirmDialog.Builder(ctx, "Unable to send transaction")
                .setText(throwable.getMessage())
                .setPositiveAction("Close")
                .create());
    }

    private void onErrorSearchUser(ProfileResult<?> errorResult) {
        Timber.e(errorResult.getError().message, "Unable to find address");
        getViewState().startDialog(ctx -> new WalletConfirmDialog.Builder(ctx, "Error")
                .setText(String.format("Unable to find user address for user \"%s\": %s", mToName, errorResult.getError().message))
                .setPositiveAction("Close")
                .create());
    }

    private void onErrorExecuteTransaction(BCExplorerResult<?> errorResult) {
        Timber.e(errorResult.getMessage(), "Unable to send transaction");
        getViewState().startDialog(ctx -> new WalletConfirmDialog.Builder(ctx, "Unable to send transaction")
                .setText((errorResult.getMessage()))
                .setPositiveAction("Close")
                .create());
    }

    private void onSuccessExecuteTransaction(final BCExplorerResult<TransactionSendResult> result) {
        if (!result.isSuccess()) {
            onErrorExecuteTransaction(result);
            return;
        }

        recipientStorage.add(new RecipientItem(mToAddress, mToName), this::setRecipientAutocomplete);

        accountStorage.update(true);
        cachedTxRepo.update(true);
        getViewState().startDialog(ctx -> {
            getAnalytics().send(AppEvent.SentCoinPopupScreen);

            return new WalletTxSendSuccessDialog.Builder(ctx, "Success!")
                    .setRecipientName(mToName)
                    .setAvatarUrl(mAvatar)
                    .setPositiveAction("View transaction", (d, v) -> {
                        Wallet.app().sounds().play(R.raw.click_pop_zap);
                        getViewState().startExplorer(result.result.txHash.toString());
                        d.dismiss();
                        getAnalytics().send(AppEvent.SentCoinPopupViewTransactionButton);
                    })
                    .setNegativeAction("Close", (d, w) -> {
                        d.dismiss();
                        getAnalytics().send(AppEvent.SentCoinPopupCloseButton);
                    })
                    .create();
        });

        getViewState().clearInputs();
    }

    private void onInputTextChanged(EditText editText, boolean valid) {
        switch (editText.getId()) {
            case R.id.input_recipient:
                mToName = editText.getText();
                mToAddress = null;
                break;
            case R.id.input_amount:
                mAmount = bigDecimalFromString(editText.getText());
                mInputChange.onNext(mAmount);
                break;
        }
    }

    private void onClickAccountSelector(View view) {
        getAnalytics().send(AppEvent.SendCoinsChooseCoinButton);
        getViewState().startAccountSelector(accountStorage.getData().getAccounts(), this::onAccountSelected);
    }

    private void onAccountSelected(AccountItem accountItem) {
        mFromAccount = accountItem;
        mLastAccount = accountItem;
        getViewState().setAccountName(String.format("%s (%s)", accountItem.coin.toUpperCase(), bdHuman(accountItem.getBalance())));

    }

    private static final class SendInitData {
        BigInteger nonce;
        BigDecimal commission;
        BCExplorerResult<?> errorResult;

        SendInitData(BigInteger nonce, BigDecimal commission) {
            this.nonce = nonce;
            this.commission = commission;
        }

        SendInitData(BCExplorerResult<?> err) {
            errorResult = err;
        }

        boolean isSuccess() {
            return errorResult == null || errorResult.isSuccess();
        }
    }


}
