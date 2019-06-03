/*
 * Copyright (C) by MinterTeam. 2019
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
import android.support.annotation.DrawableRes;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

import com.annimon.stream.Optional;
import com.annimon.stream.Stream;
import com.arellomobile.mvp.InjectViewState;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
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
import network.minter.bipwallet.internal.helpers.forms.validators.ByteLengthValidator;
import network.minter.bipwallet.internal.mvp.MvpBasePresenter;
import network.minter.bipwallet.internal.system.testing.IdlingManager;
import network.minter.bipwallet.sending.SendTabModule;
import network.minter.bipwallet.sending.models.RecipientItem;
import network.minter.bipwallet.sending.repo.RecipientAutocompleteStorage;
import network.minter.bipwallet.sending.ui.QRCodeScannerActivity;
import network.minter.bipwallet.sending.ui.SendTabFragment;
import network.minter.bipwallet.sending.ui.dialogs.WalletTxSendStartDialog;
import network.minter.bipwallet.sending.ui.dialogs.WalletTxSendSuccessDialog;
import network.minter.bipwallet.sending.ui.dialogs.WalletTxSendWaitingDialog;
import network.minter.blockchain.models.BCResult;
import network.minter.blockchain.models.NetworkStatus;
import network.minter.blockchain.models.TransactionCommissionValue;
import network.minter.blockchain.models.TransactionSendResult;
import network.minter.blockchain.models.operational.OperationInvalidDataException;
import network.minter.blockchain.models.operational.OperationType;
import network.minter.blockchain.models.operational.Transaction;
import network.minter.blockchain.models.operational.TransactionSign;
import network.minter.blockchain.repo.BlockChainStatusRepository;
import network.minter.blockchain.repo.BlockChainTransactionRepository;
import network.minter.core.MinterSDK;
import network.minter.core.crypto.MinterAddress;
import network.minter.core.crypto.MinterPublicKey;
import network.minter.explorer.models.GateResult;
import network.minter.explorer.models.HistoryTransaction;
import network.minter.explorer.repo.ExplorerCoinsRepository;
import network.minter.explorer.repo.GateEstimateRepository;
import network.minter.explorer.repo.GateGasRepository;
import network.minter.explorer.repo.GateTransactionRepository;
import network.minter.profile.MinterProfileApi;
import network.minter.profile.models.ProfileResult;
import network.minter.profile.repo.ProfileInfoRepository;
import timber.log.Timber;

import static network.minter.bipwallet.apis.reactive.ReactiveGate.createGateErrorPlain;
import static network.minter.bipwallet.apis.reactive.ReactiveGate.rxGate;
import static network.minter.bipwallet.apis.reactive.ReactiveGate.toGateError;
import static network.minter.bipwallet.apis.reactive.ReactiveMyMinter.rxProfile;
import static network.minter.bipwallet.apis.reactive.ReactiveMyMinter.toProfileError;
import static network.minter.bipwallet.internal.helpers.MathHelper.bdGT;
import static network.minter.bipwallet.internal.helpers.MathHelper.bdGTE;
import static network.minter.bipwallet.internal.helpers.MathHelper.bdHuman;
import static network.minter.bipwallet.internal.helpers.MathHelper.bdLT;
import static network.minter.bipwallet.internal.helpers.MathHelper.bdLTE;
import static network.minter.bipwallet.internal.helpers.MathHelper.bdNull;
import static network.minter.bipwallet.internal.helpers.MathHelper.bigDecimalFromString;

/**
 * minter-android-wallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@InjectViewState
public class SendTabPresenter extends MvpBasePresenter<SendTabModule.SendView> {
    private static final int REQUEST_CODE_QR_SCAN = 101;
    private static final BigDecimal PAYLOAD_FEE = BigDecimal.valueOf(0.002);
    @Inject
    SecretStorage secretStorage;
    @Inject
    CachedRepository<UserAccount, AccountStorage> accountStorage;
    @Inject
    CachedRepository<List<HistoryTransaction>, CachedExplorerTransactionRepository> cachedTxRepo;
    @Inject
    ExplorerCoinsRepository coinRepo;
    @Inject
    BlockChainTransactionRepository bcTxRepo;
    @Inject
    BlockChainStatusRepository bcStatusRepo;
    @Inject
    ProfileInfoRepository infoRepo;
    @Inject
    GateGasRepository gasRepo;
    @Inject
    CacheManager cache;
    @Inject
    RecipientAutocompleteStorage recipientStorage;
    @Inject
    IdlingManager idlingManager;
    @Inject
    GateEstimateRepository estimateRepo;
    @Inject
    GateTransactionRepository gateTxRepo;
    private AccountItem mFromAccount = null;
    private BigDecimal mAmount = null;
    private CharSequence mToMxAddress = null;
    private CharSequence mToMpAddress = null;
    private CharSequence mToName = null;
    private String mAvatar = null;
    private @DrawableRes
    int mAvatarRes;
    private AtomicBoolean mEnableUseMax = new AtomicBoolean(false);
    private BehaviorSubject<BigDecimal> mInputChange;
    private BehaviorSubject<String> mAddressChange;
    private String mGasCoin = MinterSDK.DEFAULT_COIN;
    private BigInteger mGasPrice = new BigInteger("1");
    private AccountItem mLastAccount = null;
    private BigDecimal sendFee;
    private byte[] payload;

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
        getViewState().setPayloadChangeListener(payloadChangeListener);
        loadAndSetFee();
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
                if (result != null) {
                    boolean isMxAddress = result.matches(MinterAddress.ADDRESS_PATTERN);
                    boolean isMpAddress = result.matches(MinterPublicKey.PUB_KEY_PATTERN);
                    if (isMxAddress) {
                        mToMxAddress = new MinterAddress(result).toString();
                        getViewState().setRecipient(mToMxAddress);
                        mToName = mToMxAddress.toString();
                    } else if (isMpAddress) {
                        mToMpAddress = new MinterPublicKey(result).toString();
                        getViewState().setRecipient(mToMpAddress);
                        mToName = mToMpAddress.toString();
                    }
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
        mAddressChange = BehaviorSubject.create();

        unsubscribeOnDestroy(mInputChange
                .toFlowable(BackpressureStrategy.LATEST)
                .subscribe(this::onAmountChanged));

        unsubscribeOnDestroy(mAddressChange
                .toFlowable(BackpressureStrategy.LATEST)
                .subscribe(this::onAddressChanged));

        setRecipientAutocomplete();
        getViewState().setSubmitEnabled(false);
        getViewState().setFormValidationListener(valid -> getViewState().setSubmitEnabled(valid && checkZero(mAmount)));
    }

    private void loadAndSetFee() {
        idlingManager.setNeedsWait(SendTabFragment.IDLE_SEND_WAIT_GAS, true);
        rxGate(gasRepo.getMinGas())
                .subscribeOn(Schedulers.io())
                .toFlowable(BackpressureStrategy.LATEST)
                .debounce(200, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(res -> {
                    if (res.isOk()) {
                        mGasPrice = res.result.gas;
                        Timber.d("Min Gas price: %s", mGasPrice.toString());
                        setupFee();
                        idlingManager.setNeedsWait(SendTabFragment.IDLE_SEND_WAIT_GAS, false);
                    }
                }, e -> {
                    idlingManager.setNeedsWait(SendTabFragment.IDLE_SEND_WAIT_GAS, false);
                    Timber.w(e);
                });
    }

    private void setupFee() {
        switch (getTransactionTypeByAddress()) {
            case Delegate:
                sendFee = OperationType.Delegate.getFee().multiply(new BigDecimal(mGasPrice));
                break;
            case SendCoin:
                sendFee = OperationType.SendCoin.getFee().multiply(new BigDecimal(mGasPrice));
                break;
            default:
                sendFee = null;
                break;
        }

        String fee;
        BigDecimal payloadFee = new BigDecimal(0);
        if (payload != null) {
            payloadFee = PAYLOAD_FEE.multiply(BigDecimal.valueOf(payload.length));
        }

        if (sendFee != null) {
            sendFee = sendFee.add(payloadFee);
        } else {
            sendFee = payloadFee;
        }

        fee = String.format("%s %s", bdHuman(sendFee), MinterSDK.DEFAULT_COIN.toUpperCase());

        getViewState().setFee(fee);
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
        loadAndSetFee();
    }

    private void onAddressChanged(String address) {
        if (address.isEmpty()) getViewState().setFee("");
        else setupFee();
    }

    private void onClickScanQR(View view) {
        getAnalytics().send(AppEvent.SendCoinsQRButton);

        getViewState().startScanQRWithPermissions(REQUEST_CODE_QR_SCAN);
    }

    private void onSubmit(View view) {
        if (mToName == null) {
            getViewState().setRecipientError("Recipient required");
            return;
        }
        getAnalytics().send(AppEvent.SendCoinsSendButton);
        switch (getTransactionTypeByAddress()) {
            case Delegate:
                mToMpAddress = mToName;
                mAvatar = null;
                mAvatarRes = R.drawable.img_avatar_delegate;
                mToName = mToMpAddress.toString();
                startSendDialog();
                break;
            case SendCoin:
                mToMxAddress = mToName;
                resolveUserInfo(mToName.toString(), false);
                break;
            default:
                resolveUserInfo(mToName.toString(), true);
        }
    }

    private OperationType getTransactionTypeByAddress() {
        if (mToName == null) {
            // send - default fee
            return OperationType.SendCoin;
        }

        if (mToName.toString().matches(MinterPublicKey.PUB_KEY_PATTERN)) {
            return OperationType.Delegate;
        } else {
            return OperationType.SendCoin;
        }
    }

    private SearchByType getSearchByType(String input) {
        if (MinterAddress.testString(input)) {
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
        idlingManager.setNeedsWait(SendTabFragment.IDLE_SEND_CONFIRM_DIALOG, true);
        getViewState().startDialog(ctx -> {
            rxProfile(infoRepo.findAddressInfoByInput(searchBy))
                    .delay(150, TimeUnit.MILLISECONDS)
                    .onErrorResumeNext(toProfileError())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(result -> {
                        if (result.isSuccess()) {
                            mAvatar = result.data.user.getAvatar().getUrl();
                            mToMxAddress = result.data.address.toString();
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
                            if (mToMxAddress != null) {
                                mAvatar = MinterProfileApi.getUserAvatarUrlByAddress(mToMxAddress.toString());
                            } else {
                                mAvatar = MinterProfileApi.getUserAvatarUrl(1);
                            }

                            if (failOnNotFound) {
                                mToMxAddress = null;
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

    private void resolveValidator(final String searchBy, final boolean failOnNotFound) {
        idlingManager.setNeedsWait(SendTabFragment.IDLE_SEND_CONFIRM_DIALOG, true);
        getViewState().startDialog(ctx -> {
            rxProfile(bcStatusRepo.getValidators())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(result -> {
                        if (result.isOk()) {
                            NetworkStatus.Validator validator = Stream.of(result.result)
                                    .filter(v -> {
                                        if (mToMpAddress.toString().substring(0, 2).equals(MinterSDK.PREFIX_PUBLIC_KEY))
                                            return v.pubKey.toString().equals(mToMpAddress.toString());
                                        else
                                            return v.pubKey.toString().substring(2).equals(mToMpAddress.toString());
                                    })
                                    .findFirst().orElse(null);

                            if (validator != null) {
                                mAvatar = null;
                                mAvatarRes = R.drawable.img_avatar_delegate;
                                mToName = mToMpAddress.toString();
                                startSendDialog();
                                getViewState().setRecipientError(null);
                            } else {
                                onErrorSearchValidator();
                            }
                        } else {
                            onErrorSearchValidator(result);
                        }
                    }, Wallet.Rx.errorHandler(getViewState()));

            return new WalletProgressDialog.Builder(ctx, R.string.tx_address_searching)
                    .setText(String.format("Please, wait, we are searching address for validator \"%s\"", searchBy))
                    .create();
        });
    }

    private void startSendDialog() {
        idlingManager.setNeedsWait(SendTabFragment.IDLE_SEND_CONFIRM_DIALOG, true);
        Timber.d("Confirm dialog: wait for IDLE");
        getViewState().startDialog(ctx -> {
            try {
                idlingManager.setNeedsWait(SendTabFragment.IDLE_SEND_CONFIRM_DIALOG, false);
                Timber.d("Confirm dialog: IDLING");
                getAnalytics().send(AppEvent.SendCoinPopupScreen);
                final WalletTxSendStartDialog.Builder builder = new WalletTxSendStartDialog.Builder(ctx, R.string.tx_send_overall_title)
                        .setAmount(mAmount)
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
                        });
                if (mAvatar == null) {
                    builder.setAvatarResource(mAvatarRes);
                } else {
                    builder.setAvatarUrl(mAvatar);
                }
                final WalletTxSendStartDialog dialog = builder.create();
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
     *
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
        idlingManager.setNeedsWait(SendTabFragment.IDLE_SEND_COMPLETE_DIALOG, true);

        getViewState().startDialog(ctx -> {
            final WalletProgressDialog dialog = new WalletProgressDialog.Builder(ctx, R.string.please_wait)
                    .setText(R.string.tx_send_in_progress)
                    .create();
            dialog.setCancelable(false);

            Optional<AccountItem> mntAccount = findAccountByCoin(MinterSDK.DEFAULT_COIN);
            Optional<AccountItem> sendAccount = findAccountByCoin(mFromAccount.getCoin());

            OperationType type = getTransactionTypeByAddress();
            final boolean enoughBaseCoinForCommission;

            // default coin for pay fee - MNT (base coin)
            final GateResult<TransactionCommissionValue> val = new GateResult<>();
            val.result = new TransactionCommissionValue();
            if (type == OperationType.Delegate) {
                enoughBaseCoinForCommission = bdGTE(mntAccount.get().getBalance(), OperationType.Delegate.getFee());
                val.result.value = OperationType.Delegate.getFee().multiply(Transaction.VALUE_MUL_DEC).toBigInteger();
            } else {
                enoughBaseCoinForCommission = bdGTE(mntAccount.get().getBalance(), OperationType.SendCoin.getFee());
                val.result.value = OperationType.SendCoin.getFee().multiply(Transaction.VALUE_MUL_DEC).toBigInteger();
            }

            Observable<GateResult<TransactionCommissionValue>> exchangeResolver = Observable.just(val);

            // if enough balance on MNT account, set gas coin MNT (BIP)
            if (enoughBaseCoinForCommission) {
                Timber.d("Enough balance in %sw to pay fee", MinterSDK.DEFAULT_COIN);
                Timber.tag("TX Send").d("Resolving base coin commission %s", MinterSDK.DEFAULT_COIN);
                mGasCoin = mntAccount.get().getCoin();
            }
            // if sending account is not MNT (BIP), set gas coin CUSTOM
            else if (!sendAccount.get().getCoin().equals(MinterSDK.DEFAULT_COIN)) {
                Timber.d("Not enough balance in %s to pay fee, using %s", MinterSDK.DEFAULT_COIN, mFromAccount.getCoin());
                mGasCoin = sendAccount.get().getCoin();
                // otherwise getting
                Timber.tag("TX Send").d("Resolving custom coin commission %s", mFromAccount.getCoin());
                // resolving fee currency for custom currency
                // creating tx
                try {
                    final Transaction preTx;
                    Transaction.Builder builder = new Transaction.Builder(new BigInteger("1"))
                            .setGasCoin(mGasCoin)
                            .setGasPrice(mGasPrice);

                    if (payload != null && payload.length > 0) {
                        builder.setPayload(payload);
                    }

                    if (type == OperationType.Delegate) {
                        preTx = builder
                                .delegate()
                                .setCoin(mFromAccount.coin)
                                .setPublicKey(mToMpAddress.toString())
                                .setStake(mAmount)
                                .build();

                    } else {
                        preTx = builder
                                .sendCoin()
                                .setCoin(mFromAccount.coin)
                                .setTo(mToMxAddress)
                                .setValue(mAmount)
                                .build();
                    }

                    final SecretData preData = secretStorage.getSecret(mFromAccount.address);
                    final TransactionSign preSign = preTx.signSingle(preData.getPrivateKey());

                    exchangeResolver = rxGate(estimateRepo.getTransactionCommission(preSign)).onErrorResumeNext(toGateError());
                } catch (OperationInvalidDataException e) {
                    Timber.w(e);
                    final GateResult<TransactionCommissionValue> commissionValue = new GateResult<>();
                    if (type == OperationType.Delegate) {
                        val.result.value = OperationType.Delegate.getFee().multiply(Transaction.VALUE_MUL_DEC).toBigInteger();
                    } else {
                        val.result.value = OperationType.SendCoin.getFee().multiply(Transaction.VALUE_MUL_DEC).toBigInteger();
                    }
                    exchangeResolver = Observable.just(commissionValue);
                }
            }

            // creating preparation result to send transaction

            Disposable d = Observable.combineLatest(
                    exchangeResolver,

                    rxGate(estimateRepo.getTransactionCount(mFromAccount.address)).onErrorResumeNext(toGateError()),
                    (txCommissionValue, countableDataBCResult) -> {

                        // if some request failed, returning error result
                        if (!txCommissionValue.isOk()) {
                            return new SendInitData(GateResult.copyError(txCommissionValue));
                        } else if (!countableDataBCResult.isOk()) {
                            return new SendInitData(GateResult.copyError(countableDataBCResult));
                        }

                        Timber.d("SendInitData: tx commission: %s", txCommissionValue.result.getValue());

                        final SendInitData data = new SendInitData(
                                countableDataBCResult.result.count.add(new BigInteger("1")),
                                txCommissionValue.result.getValue()
                        );
                        Timber.tag("TX Send").d("Resolved: coin %s commission=%s; nonce=%s", mFromAccount.getCoin(), data.commission, data.nonce);

                        // creating preparation data
                        return data;
                    })
                    .switchMap((Function<SendInitData, ObservableSource<GateResult<TransactionSendResult>>>) cntRes -> {
                        // if in previous request we've got error, returning it
                        if (!cntRes.isSuccess()) {
                            return Observable.just(GateResult.copyError(cntRes.errorResult));
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
                            GateResult<TransactionSendResult> errorRes;
                            final BigDecimal balanceMustBe = cntRes.commission.add(mAmount);
                            if (bdLT(mAmount, mFromAccount.getBalance())) {
                                final BigDecimal notEnough = cntRes.commission.subtract(mFromAccount.getBalance().subtract(mAmount));
                                Timber.d("Amount: %s, fromAcc: %s, diff: %s", bdHuman(mAmount), bdHuman(mFromAccount.getBalance()), bdHuman(notEnough));
                                errorRes = createGateErrorPlain(
                                        String.format("Insufficient funds: not enough %s %s, wanted: %s %s", bdHuman(notEnough), mFromAccount.getCoin(), bdHuman(balanceMustBe), mFromAccount.getCoin()),
                                        BCResult.ResultCode.InsufficientFunds.getValue(),
                                        400
                                );
                            } else {
                                Timber.d("Amount: %s, fromAcc: %s, diff: %s", bdHuman(mAmount), bdHuman(mFromAccount.getBalance()), bdHuman(balanceMustBe));
                                errorRes = createGateErrorPlain(
                                        String.format("Insufficient funds: wanted %s %s", bdHuman(balanceMustBe), mFromAccount.getCoin()),
                                        BCResult.ResultCode.InsufficientFunds.getValue(),
                                        400
                                );
                            }

                            return Observable.just(errorRes);
                        }

                        Timber.tag("TX Send").d("Send data: gasCoin=%s, coin=%s, to=%s, from=%s, amount=%s",
                                mFromAccount.getCoin(),
                                mFromAccount.getCoin(),
                                mToName,
                                mFromAccount.getAddress().toString(),
                                amountToSend
                        );
                        // creating tx
                        final Transaction tx;
                        Transaction.Builder builder = new Transaction.Builder(cntRes.nonce)
                                .setGasCoin(mGasCoin)
                                .setGasPrice(mGasPrice);

                        if (payload != null && payload.length > 0) {
                            builder.setPayload(payload);
                        }

                        if (type == OperationType.Delegate) {
                            tx = builder
                                    .delegate()
                                    .setCoin(mFromAccount.coin)
                                    .setPublicKey(mToMpAddress.toString())
                                    .setStake(amountToSend)
                                    .build();
                        } else {
                            tx = builder
                                    .sendCoin()
                                    .setCoin(mFromAccount.coin)
                                    .setTo(mToMxAddress)
                                    .setValue(amountToSend)
                                    .build();
                        }

                        final SecretData data = secretStorage.getSecret(mFromAccount.address);
                        final TransactionSign sign = tx.signSingle(data.getPrivateKey());

                        return safeSubscribeIoToUi(
                                rxGate(gateTxRepo.sendTransaction(sign))
                                        .onErrorResumeNext(toGateError())
                        );

                    })
                    .doFinally(this::onExecuteComplete)
                    .subscribe(this::onSuccessExecuteTransaction, this::onFailedExecuteTransaction);
            unsubscribeOnDestroy(d);

            return dialog;
        });
    }

    private void onExecuteComplete() {
        idlingManager.setNeedsWait(SendTabFragment.IDLE_SEND_CONFIRM_DIALOG, false);
        idlingManager.setNeedsWait(SendTabFragment.IDLE_SEND_COMPLETE_DIALOG, false);
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

    private void onErrorSearchValidator(BCResult<?> errorResult) {
        Timber.e(errorResult.error.message, "Unable to find address");
        getViewState().startDialog(ctx -> new WalletConfirmDialog.Builder(ctx, "Error")
                .setText(String.format("Unable to find validator for address \"%s\": %s", mToName, errorResult.error.message))
                .setPositiveAction("Close")
                .create());
    }

    private void onErrorSearchValidator() {
        getViewState().startDialog(ctx -> new WalletConfirmDialog.Builder(ctx, "Error")
                .setText(String.format("Unable to find validator for address \"%s\"", mToName))
                .setPositiveAction("Close")
                .create());
    }

    private void onErrorExecuteTransaction(GateResult<?> errorResult) {
        Timber.e(errorResult.getMessage(), "Unable to send transaction");
        getViewState().startDialog(ctx -> new WalletConfirmDialog.Builder(ctx, "Unable to send transaction")
                .setText((errorResult.getMessage()))
                .setPositiveAction("Close")
                .create());
    }

    private void onSuccessExecuteTransaction(
            final GateResult<TransactionSendResult> result) {
        if (!result.isOk()) {
            onErrorExecuteTransaction(result);
            return;
        }

        final CharSequence to;
        if (getTransactionTypeByAddress() == OperationType.Delegate) {
            to = mToMpAddress;
        } else {
            to = mToMxAddress;
        }
        recipientStorage.add(new RecipientItem(to, mToName), this::setRecipientAutocomplete);

        accountStorage.update(true);
        cachedTxRepo.update(true);
        getViewState().startDialog(ctx -> {
            getAnalytics().send(AppEvent.SentCoinPopupScreen);

            WalletTxSendSuccessDialog.Builder builder = new WalletTxSendSuccessDialog.Builder(ctx, "Success!")
                    .setRecipientName(mToName)
                    .setPositiveAction("View transaction", (d, v) -> {
                        Wallet.app().sounds().play(R.raw.click_pop_zap);
                        getViewState().startExplorer(result.result.txHash.toString());
                        d.dismiss();
                        getAnalytics().send(AppEvent.SentCoinPopupViewTransactionButton);
                    })
                    .setNegativeAction("Close", (d, w) -> {
                        d.dismiss();
                        getAnalytics().send(AppEvent.SentCoinPopupCloseButton);
                    });

            if (mAvatar == null) {
                builder.setAvatarResource(mAvatarRes);
            } else {
                builder.setAvatarUrl(mAvatar);
            }
            return builder.create();
        });

        getViewState().clearInputs();
        mToName = null;
        mToMpAddress = null;
        mToMxAddress = null;
        sendFee = null;
    }

    private void onInputTextChanged(EditText editText, boolean valid) {
        switch (editText.getId()) {
            case R.id.input_recipient:
                mToName = editText.getText();
                mToMxAddress = null;
                mAddressChange.onNext(mToName.toString());
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
        GateResult<?> errorResult;

        SendInitData(BigInteger nonce, BigDecimal commission) {
            this.nonce = nonce;
            this.commission = commission;
        }

        SendInitData(GateResult<?> err) {
            errorResult = err;
        }

        boolean isSuccess() {
            return errorResult == null || errorResult.isOk();
        }
    }

    private TextWatcher payloadChangeListener = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            byte[] tmpPayload = s.toString().getBytes(StandardCharsets.UTF_8);
            int totalBytes = tmpPayload.length;
            
            if (totalBytes > ByteLengthValidator.MAX_PAYLOAD_LENGTH) {
                tmpPayload = Arrays.copyOfRange(tmpPayload, 0, ByteLengthValidator.MAX_PAYLOAD_LENGTH);
                getViewState().setPayload(new String(tmpPayload, StandardCharsets.UTF_8));
            }

            payload = tmpPayload;
            setupFee();
        }
    };
}
