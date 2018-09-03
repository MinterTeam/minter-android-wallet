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
import network.minter.bipwallet.apis.explorer.CachedExplorerTransactionRepository;
import network.minter.bipwallet.internal.Wallet;
import network.minter.bipwallet.internal.data.CacheManager;
import network.minter.bipwallet.internal.data.CachedRepository;
import network.minter.bipwallet.internal.dialogs.WalletConfirmDialog;
import network.minter.bipwallet.internal.dialogs.WalletProgressDialog;
import network.minter.bipwallet.internal.exceptions.ProfileResponseException;
import network.minter.bipwallet.internal.mvp.MvpBasePresenter;
import network.minter.bipwallet.sending.SendTabModule;
import network.minter.bipwallet.sending.dialogs.WalletTxSendStartDialog;
import network.minter.bipwallet.sending.dialogs.WalletTxSendSuccessDialog;
import network.minter.bipwallet.sending.dialogs.WalletTxSendWaitingDialog;
import network.minter.bipwallet.sending.models.RecipientItem;
import network.minter.bipwallet.sending.repo.RecipientAutocompleteStorage;
import network.minter.bipwallet.sending.ui.QRCodeScannerActivity;
import network.minter.blockchain.models.BCResult;
import network.minter.blockchain.models.CountableData;
import network.minter.blockchain.models.TransactionCommissionValue;
import network.minter.blockchain.models.TransactionSendResult;
import network.minter.blockchain.models.operational.OperationInvalidDataException;
import network.minter.blockchain.models.operational.OperationType;
import network.minter.blockchain.models.operational.Transaction;
import network.minter.blockchain.models.operational.TransactionSign;
import network.minter.blockchain.repo.BlockChainAccountRepository;
import network.minter.blockchain.repo.BlockChainCoinRepository;
import network.minter.blockchain.repo.BlockChainTransactionRepository;
import network.minter.core.MinterSDK;
import network.minter.core.crypto.MinterAddress;
import network.minter.explorer.models.HistoryTransaction;
import network.minter.profile.models.ProfileResult;
import network.minter.profile.repo.ProfileInfoRepository;
import timber.log.Timber;

import static network.minter.bipwallet.apis.blockchain.BCErrorHelper.normalizeBlockChainInsufficientFundsMessage;
import static network.minter.bipwallet.internal.ReactiveAdapter.convertToBcErrorResult;
import static network.minter.bipwallet.internal.ReactiveAdapter.convertToProfileErrorResult;
import static network.minter.bipwallet.internal.ReactiveAdapter.createBcErrorResultMessage;
import static network.minter.bipwallet.internal.ReactiveAdapter.rxCallBc;
import static network.minter.bipwallet.internal.ReactiveAdapter.rxCallProfile;
import static network.minter.bipwallet.internal.helpers.MathHelper.bdGT;
import static network.minter.bipwallet.internal.helpers.MathHelper.bdGTE;
import static network.minter.bipwallet.internal.helpers.MathHelper.bdHuman;
import static network.minter.bipwallet.internal.helpers.MathHelper.bdLT;
import static network.minter.bipwallet.internal.helpers.MathHelper.bdLTE;
import static network.minter.bipwallet.internal.helpers.MathHelper.bigDecimalFromString;

/**
 * MinterWallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@InjectViewState
public class SendTabPresenter extends MvpBasePresenter<SendTabModule.SendView> {
    private static final int REQUEST_CODE_QR_SCAN = 101;
    @Inject SecretStorage secretStorage;
    @Inject CachedRepository<UserAccount, AccountStorage> accountStorage;
    @Inject CachedRepository<List<HistoryTransaction>, CachedExplorerTransactionRepository> txRepo;
    @Inject BlockChainAccountRepository accountRepo;
    @Inject BlockChainCoinRepository coinRepo;
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

    private enum SearchByType {
        Address, Username, Email
    }

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
        getViewState().setOnClickScanQR(this::onClickScanQR);
        getViewState().setOnClickMaximum(this::onClickMaximum);
        getViewState().setFee(String.format("%s %s", bdHuman(OperationType.SendCoin.getFee()), MinterSDK.DEFAULT_COIN.toUpperCase()));
        accountStorage.update();
    }

    @Override
    protected void onFirstViewAttach() {
        super.onFirstViewAttach();
        safeSubscribeIoToUi(accountStorage.observe())
                .subscribe(res -> {
                    if (!res.isEmpty()) {
                        onAccountSelected(res.getAccounts().get(0));
                    }
                });

        setRecipientAutocomplete();
        getViewState().setSubmitEnabled(false);
        getViewState().setFormValidationListener(valid -> getViewState().setSubmitEnabled(valid));

        mInputChange = BehaviorSubject.create();
        unsubscribeOnDestroy(mInputChange
                .toFlowable(BackpressureStrategy.LATEST)
                .debounce(200, TimeUnit.MILLISECONDS)
                .subscribe(this::onAmountChanged));
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

    private void onAmountChanged(BigDecimal amount) {
        mEnableUseMax.set(false);
    }

    private void onClickScanQR(View view) {
        getViewState().startScanQRWithPermissions(REQUEST_CODE_QR_SCAN);
    }

    private void onSubmit(View view) {
        if (mToName == null) {
            getViewState().setRecipientError("Invalid recipient");
            return;
        }
        if (mToAddress != null) {
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
//                            mToName = String.format("@%s", result.data.user.username);
                            getViewState().setRecipientError(null);
                            startSendDialog();
                        } else {
                            if (failOnNotFound) {
                                mToAddress = null;
                                onErrorSearchUser(result);
                                Timber.d(new ProfileResponseException(result), "Unable to find address");
                            } else {
                                getViewState().setRecipientError(null);
                                startSendDialog();
                            }
                        }
                    }, Wallet.Rx.errorHandler());

            return new WalletProgressDialog.Builder(ctx, "Searching address")
                    .setText(String.format("Please, wait, we are searching address for user \"%s\"", searchBy))
                    .create();
        });
    }

    private void startSendDialog() {
        getViewState().startDialog(ctx -> {
            final WalletTxSendStartDialog dialog = new WalletTxSendStartDialog.Builder(ctx, "You're sending")
                    .setAmount(mAmount)
                    .setAvatarUrl(mAvatar)
                    .setRecipientName(mToName)
                    .setCoin(mFromAccount.coin)
                    .setPositiveAction("Send", (d, w) -> onStartExecuteTransaction(true))
                    .setNegativeAction("Cancel", null)
                    .create();
            dialog.setCancelable(true);
            return dialog;
        });
    }

    /**
     * Unused now, soon
     * @param dialogInterface
     * @param which
     */
    private void onStartWaitingDialog(DialogInterface dialogInterface, int which) {
        getViewState().startDialog(ctx -> {
            final WalletTxSendWaitingDialog dialog = new WalletTxSendWaitingDialog.Builder(ctx, "Please wait")
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

    private void onClickMaximum(View view) {
        mEnableUseMax.set(true);
        checkEnoughBalance(mFromAccount.getBalanceBase());
        mAmount = mFromAccount.getBalance();
        getViewState().setAmount(mFromAccount.getBalance().stripTrailingZeros().toPlainString());
    }

    private Optional<AccountItem> findAccountByCoin(String coin) {
        return Stream.of(accountStorage.getData().getAccounts())
                .filter(item -> item.getCoin().equals(coin.toUpperCase()))
                .findFirst();
    }

    private void onStartExecuteTransaction(boolean express) {
        getViewState().startDialog(ctx -> {
            final WalletProgressDialog dialog = new WalletProgressDialog.Builder(ctx, "Please wait")
                    .setText("Sending transaction...")
                    .create();
            dialog.setCancelable(false);

            Optional<AccountItem> mntAccount = findAccountByCoin(MinterSDK.DEFAULT_COIN);
            Optional<AccountItem> sendAccount = findAccountByCoin(mFromAccount.getCoin());

            // if enough balance on MNT account, set gas coin MNT (BIP)
            if (bdGTE(mntAccount.get().getBalance(), OperationType.SendCoin.getFee())) {
                Timber.d("Enough balance in MNT to pay fee");
                mGasCoin = mntAccount.get().getCoin();
            }
            // if sending account is not MNT (BIP), set sending account coin
            else if (!sendAccount.get().getCoin().equals(MinterSDK.DEFAULT_COIN)) {
                Timber.d("Not enough balance in MNT to pay fee, using " + mFromAccount.getCoin());
                mGasCoin = sendAccount.get().getCoin();
            }

            // resolving default fee (in pips)
            Observable<BCResult<TransactionCommissionValue>> exchangeResolver;
            if (mFromAccount.coin.toUpperCase().equals(MinterSDK.DEFAULT_COIN)) {
                Timber.tag("TX Send").d("Resolving default coin currency %s", MinterSDK.DEFAULT_COIN);
                final BCResult<TransactionCommissionValue> val = new BCResult<>();
                val.result = new TransactionCommissionValue();
                val.result.value = OperationType.SendCoin.getFee().multiply(Transaction.VALUE_MUL_DEC).toBigInteger();
                exchangeResolver = Observable.just(val);
            } else {
                Timber.tag("TX Send").d("Resolving custom coin currency %s", mFromAccount.getCoin());
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

                    exchangeResolver = rxCallBc(bcTxRepo.getTransactionCommission(preSign));
                } catch (OperationInvalidDataException e) {
                    Timber.w(e);
                    final BCResult<TransactionCommissionValue> val = new BCResult<>();
                    val.result.value = OperationType.SendCoin.getFee().multiply(Transaction.VALUE_MUL_DEC).toBigInteger();
                    exchangeResolver = Observable.just(val);
                }
            }

            // creating preparation result to send transaction
            Observable.combineLatest(
                    exchangeResolver.onErrorResumeNext(convertToBcErrorResult()),
                    rxCallBc(accountRepo.getTransactionCount(mFromAccount.address)).onErrorResumeNext(convertToBcErrorResult()),
                    new BiFunction<BCResult<TransactionCommissionValue>, BCResult<CountableData>, SendInitData>() {
                        @Override
                        public SendInitData apply(BCResult<TransactionCommissionValue> txCommissionValue, BCResult<CountableData> countableDataBCResult) {

                            // if some request failed, returning error result
                            if (!txCommissionValue.isSuccess()) {
                                return new SendInitData(BCResult.copyError(txCommissionValue));
                            } else if (!countableDataBCResult.isSuccess()) {
                                return new SendInitData(BCResult.copyError(countableDataBCResult));
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
                    .switchMap((Function<SendInitData, ObservableSource<BCResult<TransactionSendResult>>>) cntRes -> {
                        // if in previous request we've got error, returning it
                        if (!cntRes.isSuccess()) {
                            return Observable.just(BCResult.copyError(cntRes.errorResult));
                        }

                        final BigDecimal amountToSend;
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
                            BCResult<TransactionSendResult> errorRes = createBcErrorResultMessage(
                                    String.format("Insufficient funds: need %s %s", diff.toPlainString(), mFromAccount.coin.toUpperCase()),
                                    BCResult.ResultCode.InsufficientFundsB,
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
                        final TransactionSign sign = tx.sign(data.getPrivateKey());

                        return safeSubscribeIoToUi(rxCallBc(accountRepo.sendTransaction(sign)))
                                .onErrorResumeNext(convertToBcErrorResult());

                    }).subscribe(this::onSuccessExecuteTransaction, this::onFailedExecuteTransaction);

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
        getViewState().startDialog(ctx -> new WalletConfirmDialog.Builder(ctx, errorResult.getError().message)
                .setText(String.format("Unable to find user address for user \"%s\"", mToName))
                .setPositiveAction("Close")
                .create());
    }

    private void onErrorExecuteTransaction(BCResult<?> errorResult) {
        Timber.e(errorResult.message, "Unable to send transaction");
        getViewState().startDialog(ctx -> new WalletConfirmDialog.Builder(ctx, "Unable to send transaction")
                .setText(normalizeBlockChainInsufficientFundsMessage(errorResult.message))
                .setPositiveAction("Close")
                .create());
    }

    private void onSuccessExecuteTransaction(final BCResult<TransactionSendResult> result) {
        if (!result.isSuccess()) {
            onErrorExecuteTransaction(result);
            return;
        }

        recipientStorage.add(new RecipientItem(mToAddress, mToName), this::setRecipientAutocomplete);

        accountStorage.update(true);
        txRepo.update(true);
        getViewState().startDialog(ctx -> new WalletTxSendSuccessDialog.Builder(ctx, "Success!")
                .setRecipientName(mToName)
                .setAvatar(mAvatar)
                .setPositiveAction("View transaction", (d, v) -> {
                    getViewState().startExplorer(result.result.txHash.toString());
                    d.dismiss();
                })
                .setNegativeAction("Close", null)
                .create());

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
        getViewState().startAccountSelector(accountStorage.getData().getAccounts(), this::onAccountSelected);
    }

    private void onAccountSelected(AccountItem accountItem) {
        mFromAccount = accountItem;
        getViewState().setAccountName(String.format("%s (%s)", accountItem.coin.toUpperCase(), bdHuman(accountItem.getBalance())));
    }

    private static final class SendInitData {
        BigInteger nonce;
        BigDecimal commission;
        BCResult<?> errorResult;

        SendInitData(BigInteger nonce, BigDecimal commission) {
            this.nonce = nonce;
            this.commission = commission;
        }

        SendInitData(BCResult<?> err) {
            errorResult = err;
        }

        boolean isSuccess() {
            return errorResult == null || errorResult.isSuccess();
        }
    }


}
