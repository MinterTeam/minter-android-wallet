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

package network.minter.bipwallet.sending.views;

import android.app.Activity;
import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

import com.annimon.stream.Optional;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import androidx.annotation.DrawableRes;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import moxy.InjectViewState;
import network.minter.bipwallet.R;
import network.minter.bipwallet.addressbook.db.AddressBookRepository;
import network.minter.bipwallet.addressbook.models.AddressContact;
import network.minter.bipwallet.addressbook.ui.AddressBookActivity;
import network.minter.bipwallet.advanced.models.AddressBalanceTotal;
import network.minter.bipwallet.advanced.models.AddressListBalancesTotal;
import network.minter.bipwallet.advanced.models.SecretData;
import network.minter.bipwallet.advanced.repo.AccountStorage;
import network.minter.bipwallet.advanced.repo.SecretStorage;
import network.minter.bipwallet.analytics.AppEvent;
import network.minter.bipwallet.apis.explorer.CacheTxRepository;
import network.minter.bipwallet.internal.Wallet;
import network.minter.bipwallet.internal.auth.AuthSession;
import network.minter.bipwallet.internal.data.CachedRepository;
import network.minter.bipwallet.internal.dialogs.WalletConfirmDialog;
import network.minter.bipwallet.internal.dialogs.WalletProgressDialog;
import network.minter.bipwallet.internal.helpers.KeyboardHelper;
import network.minter.bipwallet.internal.helpers.forms.validators.ByteLengthValidator;
import network.minter.bipwallet.internal.helpers.forms.validators.IsNotMnemonicValidator;
import network.minter.bipwallet.internal.mvp.MvpBasePresenter;
import network.minter.bipwallet.internal.system.SimpleTextWatcher;
import network.minter.bipwallet.internal.system.testing.IdlingManager;
import network.minter.bipwallet.sending.contract.SendView;
import network.minter.bipwallet.sending.ui.QRCodeScannerActivity;
import network.minter.bipwallet.sending.ui.SendTabFragment;
import network.minter.bipwallet.sending.ui.dialogs.WalletTxSendStartDialog;
import network.minter.bipwallet.sending.ui.dialogs.WalletTxSendSuccessDialog;
import network.minter.bipwallet.tx.contract.TxInitData;
import network.minter.bipwallet.wallets.selector.WalletItem;
import network.minter.blockchain.models.BCResult;
import network.minter.blockchain.models.TransactionCommissionValue;
import network.minter.blockchain.models.TransactionSendResult;
import network.minter.blockchain.models.operational.OperationInvalidDataException;
import network.minter.blockchain.models.operational.OperationType;
import network.minter.blockchain.models.operational.Transaction;
import network.minter.blockchain.models.operational.TransactionSign;
import network.minter.blockchain.repo.BlockChainTransactionRepository;
import network.minter.core.MinterSDK;
import network.minter.core.crypto.MinterAddress;
import network.minter.core.crypto.MinterPublicKey;
import network.minter.core.crypto.PrivateKey;
import network.minter.explorer.models.CoinBalance;
import network.minter.explorer.models.GateResult;
import network.minter.explorer.models.HistoryTransaction;
import network.minter.explorer.models.TxCount;
import network.minter.explorer.repo.ExplorerCoinsRepository;
import network.minter.explorer.repo.ExplorerValidatorsRepository;
import network.minter.explorer.repo.GateEstimateRepository;
import network.minter.explorer.repo.GateGasRepository;
import network.minter.explorer.repo.GateTransactionRepository;
import network.minter.ledger.connector.rxjava2.RxMinterLedger;
import network.minter.profile.repo.ProfileInfoRepository;
import timber.log.Timber;

import static com.google.common.base.MoreObjects.firstNonNull;
import static java.lang.String.format;
import static network.minter.bipwallet.apis.reactive.ReactiveGate.createGateErrorPlain;
import static network.minter.bipwallet.apis.reactive.ReactiveGate.rxGate;
import static network.minter.bipwallet.apis.reactive.ReactiveGate.toGateError;
import static network.minter.bipwallet.internal.helpers.MathHelper.bdGTE;
import static network.minter.bipwallet.internal.helpers.MathHelper.bdHuman;
import static network.minter.bipwallet.internal.helpers.MathHelper.bdLT;
import static network.minter.bipwallet.internal.helpers.MathHelper.bigDecimalFromString;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@InjectViewState
public class SendTabPresenter extends MvpBasePresenter<SendView> {
    private static final int REQUEST_CODE_QR_SCAN_ADDRESS = 101;
    private static final int REQUEST_CODE_ADDRESS_BOOK_SELECT = 102;
    private static final BigDecimal PAYLOAD_FEE = BigDecimal.valueOf(0.002);
    @Inject SecretStorage secretStorage;
    @Inject AuthSession session;
    @Inject CachedRepository<List<HistoryTransaction>, CacheTxRepository> cachedTxRepo;
    @Inject CachedRepository<AddressListBalancesTotal, AccountStorage> accountStorage;
    @Inject ExplorerCoinsRepository coinRepo;
    @Inject BlockChainTransactionRepository bcTxRepo;
    @Inject ExplorerValidatorsRepository validatorsRepo;
    @Inject ProfileInfoRepository infoRepo;
    @Inject GateGasRepository gasRepo;
    @Inject IdlingManager idlingManager;
    @Inject GateEstimateRepository estimateRepo;
    @Inject GateTransactionRepository gateTxRepo;
    @Inject AddressBookRepository addressBookRepo;
    private CoinBalance mFromAccount = null;
    private BigDecimal mAmount = null;
    private AddressContact mRecipient;
    private String mAvatar = null;
    private @DrawableRes int mAvatarRes;
    private AtomicBoolean mUseMax = new AtomicBoolean(false);
    private AtomicBoolean mClickedUseMax = new AtomicBoolean(false);
    private BehaviorSubject<String> mInputChange;
    private BehaviorSubject<String> mAddressChange;
    private String mGasCoin = MinterSDK.DEFAULT_COIN;
    private BigInteger mGasPrice = new BigInteger("1");
    private CoinBalance mLastAccount = null;
    private BigDecimal mSendFee;
    private byte[] mPayload;
    private final TextWatcher mPayloadChangeListener = new SimpleTextWatcher() {
        @Override
        public void afterTextChanged(Editable s) {
            byte[] tmpPayload = s.toString().getBytes(StandardCharsets.UTF_8);
            int totalBytes = tmpPayload.length;

            if (totalBytes > ByteLengthValidator.MAX_PAYLOAD_LENGTH) {
                byte[] tmp = new byte[1024];
                System.arraycopy(tmpPayload, 0, tmp, 0, 1024);
                getViewState().setPayload(new String(tmp, StandardCharsets.UTF_8));
                tmpPayload = tmp;
            }

            mPayload = tmpPayload;


            IsNotMnemonicValidator validator = new IsNotMnemonicValidator("ATTENTION: You are about to send seed phrase in the message attached to this transaction.\n" +
                    "\n" +
                    "If you do this, anyone will be able to see it and access your funds!", false);

            if (!validator.validate(new String(mPayload))) {
                getViewState().setPayloadError(validator.getErrorMessage());
            } else {
                getViewState().setPayloadError(null);
            }

            setupFee();
        }
    };
    private boolean mFormValid = false;

    @Inject
    public SendTabPresenter() {
    }

    @Override
    public void attachView(SendView view) {
        super.attachView(view);
        getViewState().setOnClickAccountSelectedListener(this::onClickAccountSelector);
        getViewState().setOnTextChangedListener(this::onInputTextChanged);
        getViewState().setOnSubmit(this::onSubmit);
        getViewState().setOnClickMaximum(this::onClickMaximum);
        getViewState().setOnClickAddPayload(this::onClickAddPayload);
        getViewState().setOnClickClearPayload(this::onClickClearPayload);
        getViewState().setPayloadChangeListener(mPayloadChangeListener);
        getViewState().setOnContactsClickListener(this::onClickContacts);
        getViewState().setRecipientAutocompleteItemClickListener(this::onAutocompleteSelected);
        loadAndSetFee();
        accountStorage.update();
    }

    @Override
    public void detachView(SendView view) {
        super.detachView(view);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        if (requestCode == REQUEST_CODE_QR_SCAN_ADDRESS) {
            if (data != null && data.hasExtra(QRCodeScannerActivity.RESULT_TEXT)) {
                //Getting the passed result
                String result = data.getStringExtra(QRCodeScannerActivity.RESULT_TEXT);
                if (result != null) {
                    boolean isMxAddress = result.matches(MinterAddress.ADDRESS_PATTERN);
                    boolean isMpAddress = result.matches(MinterPublicKey.PUB_KEY_PATTERN);
                    mRecipient.address = result;
                    if (isMxAddress) {
                        mRecipient.type = AddressContact.AddressType.Address;
                        getViewState().setRecipient(mRecipient);
                    } else if (isMpAddress) {
                        mRecipient.type = AddressContact.AddressType.ValidatorPubKey;
                        getViewState().setRecipient(mRecipient);
                    }
                }
            }
        } else if (requestCode == SendTabFragment.REQUEST_CODE_QR_SCAN_TX) {
            String result = data.getStringExtra(QRCodeScannerActivity.RESULT_TEXT);
            if (result != null) {

                boolean isMxAddress = result.matches(MinterAddress.ADDRESS_PATTERN);
                boolean isMpAddress = result.matches(MinterPublicKey.PUB_KEY_PATTERN);
                if (isMxAddress || isMpAddress) {
                    onActivityResult(REQUEST_CODE_QR_SCAN_ADDRESS, resultCode, data);
                    return;
                }

                try {
                    getViewState().startExternalTransaction(result);
                } catch (Throwable t) {
                    Timber.w(t, "Unable to parse remote transaction: %s", result);
                    getViewState().startDialog(ctx -> new WalletConfirmDialog.Builder(ctx, "Unable to scan QR")
                            .setText("Invalid transaction data: %s", t.getMessage())
                            .setPositiveAction(R.string.btn_close)
                            .create());
                }
            }
        } else if (requestCode == REQUEST_CODE_ADDRESS_BOOK_SELECT) {
            AddressContact contact = AddressBookActivity.getResult(data);
            if (contact == null) {
                return;
            }
            mRecipient = contact;
            getViewState().setRecipient(mRecipient);
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
                        getViewState().setWallets(WalletItem.create(secretStorage, res));
                        getViewState().setMainWallet(WalletItem.create(secretStorage, res.getBalance(secretStorage.getMainWallet())));

                        AddressBalanceTotal acc = accountStorage.getEntity().getMainWallet();
                        if (mLastAccount != null) {
                            onAccountSelected(acc.findCoinByName(mLastAccount.getCoin()).orElse(acc.getCoinsList().get(0)));
                        } else {
                            onAccountSelected(acc.getCoinsList().get(0));
                        }
                    }
                }, t -> {
                    getViewState().onError(t);
                });

        mInputChange = BehaviorSubject.create();
        mAddressChange = BehaviorSubject.create();

        unsubscribeOnDestroy(mInputChange
                .toFlowable(BackpressureStrategy.LATEST)
                .debounce(200, TimeUnit.MILLISECONDS)
                .subscribe(this::onAmountChanged));

        unsubscribeOnDestroy(mAddressChange
                .toFlowable(BackpressureStrategy.LATEST)
                .debounce(200, TimeUnit.MILLISECONDS)
                .subscribe(this::onAddressChanged));

        setRecipientAutocomplete();
        checkEnableSubmit();
        getViewState().setFormValidationListener(valid -> {
            mFormValid = valid;
            Timber.d("Form is valid: %b", valid);
            checkEnableSubmit();
        });
    }

    private void onClickClearPayload(View view) {
        getViewState().hidePayload();
    }

    private void onClickContacts(View view) {
        getViewState().startAddressBook(REQUEST_CODE_ADDRESS_BOOK_SELECT);
    }

    private void onClickAddPayload(View view) {
        getViewState().showPayload();
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
                mSendFee = OperationType.Delegate.getFee().multiply(new BigDecimal(mGasPrice));
                break;
            case SendCoin:
                mSendFee = OperationType.SendCoin.getFee().multiply(new BigDecimal(mGasPrice));
                break;
            default:
                mSendFee = null;
                break;
        }

        String fee;
        BigDecimal payloadFee = getPayloadFee();

        if (mSendFee != null) {
            mSendFee = mSendFee.add(payloadFee);
        } else {
            mSendFee = payloadFee;
        }

        fee = format("%s %s", bdHuman(mSendFee), MinterSDK.DEFAULT_COIN.toUpperCase());

        getViewState().setFee(fee);
    }

    private BigDecimal getPayloadFee() {
        return BigDecimal.valueOf(firstNonNull(mPayload, new byte[0]).length).multiply(PAYLOAD_FEE);
    }

    private void setRecipientAutocomplete() {
        if (true) {
            // FIXME some cases working wrong, this task is low priority, so just disable it for now
            return;
        }
//        recipientStorage.getItems()
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(res -> getViewState().setRecipientsAutocomplete(res, (item, position) -> getViewState().setRecipient(item.getName())));
    }

    private boolean checkEnoughBalance(BigDecimal amount) {
        if (!mFromAccount.getCoin().toLowerCase().equals(MinterSDK.DEFAULT_COIN.toLowerCase())) {
            return true;
        }
        boolean enough = bdGTE(amount, OperationType.SendCoin.getFee());
        if (!enough) {
            getViewState().setAmountError("Insufficient balance");
        } else {
            getViewState().setAmountError(null);
        }
        return enough;
    }

    /**
     * Checks input amount is not empty and not negative number
     * @param amount
     * @return
     */
    private boolean checkAmountIsValid(String amount) {
        if (amount == null || amount.isEmpty()) {
            getViewState().setAmountError("Amount can't be empty");
            return false;
        }

        boolean valid = bdGTE(bigDecimalFromString(amount), BigDecimal.ZERO);
        getViewState().setAmountError(!valid ? "Amount must be greater or equals to 0" : null);

        return valid;
    }

    private void onAmountChanged(String amount) {
        if (amount == null || amount.isEmpty()) {
            mAmount = BigDecimal.ZERO;
        } else {
            mAmount = bigDecimalFromString(amount);
        }

        if (!mClickedUseMax.get()) {
            mUseMax.set(false);
        }
        mClickedUseMax.set(false);

        checkAmountIsValid(amount);
//        checkEnableSubmit();
        loadAndSetFee();
        checkEnableSubmit();
    }

    private void onAddressChanged(String address) {
        if (address.isEmpty()) getViewState().setFee("");
        else setupFee();
    }

    private void onSubmit(View view) {
        if (mRecipient == null) {
            getViewState().setRecipientError("Recipient required");
            return;
        }
        getAnalytics().send(AppEvent.SendCoinsSendButton);
        switch (getTransactionTypeByAddress()) {
            case Delegate:
                mAvatar = null;
                mAvatarRes = R.drawable.img_avatar_delegate;
                startSendDialog();
                break;
            case SendCoin:
                mAvatar = mRecipient.getAvatar();
                startSendDialog();
                break;
        }
    }

    private OperationType getTransactionTypeByAddress() {
        if (mRecipient == null) {
            return OperationType.SendCoin;
        }

        return mRecipient.type == AddressContact.AddressType.ValidatorPubKey ? OperationType.Delegate : OperationType.SendCoin;
    }

    private void startSendDialog() {
        idlingManager.setNeedsWait(SendTabFragment.IDLE_SEND_CONFIRM_DIALOG, true);
        Timber.d("Confirm dialog: wait for IDLE");
        getViewState().startDialog(ctx -> {
            try {
                idlingManager.setNeedsWait(SendTabFragment.IDLE_SEND_CONFIRM_DIALOG, false);
                Timber.d("Confirm dialog: IDLING");
                getAnalytics().send(AppEvent.SendCoinPopupScreen);
                final WalletTxSendStartDialog dialog = new WalletTxSendStartDialog.Builder(ctx, R.string.tx_send_overall_title)
                        .setAmount(mAmount)
                        .setRecipientName(mRecipient.name)
                        .setCoin(mFromAccount.coin)
                        .setAvatarUrlFallback(mAvatar, mAvatarRes)
                        .setPositiveAction(R.string.btn_send, (d, w) -> {
                            Wallet.app().sounds().play(R.raw.bip_beep_digi_octave);
                            onStartExecuteTransaction();
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

    private void checkEnableSubmit() {
        if (mFromAccount == null) {
            Timber.d("Account did not loaded yet!");
            getViewState().setSubmitEnabled(false);
            return;
        }

        if (mAmount == null) {
            Timber.w("Amount did not set and it is NULL");
            getViewState().setSubmitEnabled(false);
            return;
        } else {
            Timber.d("Amount did set and it's NOT a NULL");
        }

        boolean a = bdGTE(mAmount, BigDecimal.ZERO);
        boolean b = mFormValid;
        boolean c = checkEnoughBalance(mFromAccount.getAmount());
        boolean formFullyValid = a && b && c;

        getViewState().setSubmitEnabled(formFullyValid);
    }

    private void onClickMaximum(View view) {
        if (mFromAccount == null) {
            getViewState().setError("Account didn't loaded yet...");
            return;
        }
        mUseMax.set(true);
        mClickedUseMax.set(true);
        mAmount = mFromAccount.getAmount();
//        checkEnableSubmit();
        getViewState().setAmount(mFromAccount.getAmount().stripTrailingZeros().toPlainString());

        getAnalytics().send(AppEvent.SendCoinsUseMaxButton);
        if (view != null && view.getContext() instanceof Activity) {
            KeyboardHelper.hideKeyboard((Activity) view.getContext());
        }
    }

    private Optional<CoinBalance> findAccountByCoin(String coin) {
        return accountStorage.getEntity().getMainWallet().findCoinByName(coin);
    }

    private TransactionSign createPreTx(OperationType type) throws OperationInvalidDataException {
        final Transaction preTx;
        Transaction.Builder builder = new Transaction.Builder(new BigInteger("1"))
                .setGasCoin(mGasCoin)
                .setGasPrice(mGasPrice);

        if (mPayload != null && mPayload.length > 0) {
            builder.setPayload(mPayload);
        }

        if (type == OperationType.Delegate) {
            preTx = builder
                    .delegate()
                    .setCoin(mFromAccount.coin)
                    .setPublicKey(mRecipient.address)
                    .setStake(mAmount)
                    .build();

        } else {
            preTx = builder
                    .sendCoin()
                    .setCoin(mFromAccount.coin)
                    .setTo(mRecipient.address)
                    .setValue(mAmount)
                    .build();
        }

        final PrivateKey dummyPrivate = new PrivateKey("F000000000000000000000000000000000000000000000000000000000000000");
        return preTx.signSingle(dummyPrivate);
    }

    private Transaction createFinalTx(BigInteger nonce, OperationType type, BigDecimal amountToSend) throws OperationInvalidDataException {
        Transaction tx;
        Transaction.Builder builder = new Transaction.Builder(nonce)
                .setGasCoin(mGasCoin)
                .setGasPrice(mGasPrice);

        if (mPayload != null && mPayload.length > 0) {
            builder.setPayload(mPayload);
        }

        if (type == OperationType.Delegate) {
            tx = builder
                    .delegate()
                    .setCoin(mFromAccount.coin)
                    .setPublicKey(mRecipient.address)
                    .setStake(amountToSend)
                    .build();
        } else {
            tx = builder
                    .sendCoin()
                    .setCoin(mFromAccount.coin)
                    .setTo(mRecipient.address)
                    .setValue(amountToSend)
                    .build();
        }

        return tx;
    }

    private BigDecimal getFee() {
        return getTransactionTypeByAddress().getFee().add(getPayloadFee());
    }

    private BigInteger getFeeNormalized() {
        return getFee().multiply(Transaction.VALUE_MUL_DEC).toBigInteger();
    }

    /**
     * This is a complex sending method, read carefully, almost each line is commented, i don't know how
     * to simplify all of this
     * <p>
     * Base logic in that, if we have enough BIP to send amount to your friend and you have some additional
     * value on your account to pay fee, it's ok. But you can have enough BIP to send, but not enough to pay fee.
     * Also, as we are minting coins, user can have as much coins as Minter's blockchain have.
     * So user can send his CUSTOM_COIN to his friend and don't have enough BIP to pay fee, we must switch GAS_COIN
     * to user's custom coin, or vice-versa.
     * <p>
     * So first: we detecting what account used
     * Second: calc balance on it, and compare with input amount
     * Third: if user wants to send CUSTOM_COIN, we don't know the price of it, and we should ask node about it,
     * so, we're creating preliminary transaction and requesting "estimate transaction commission"
     * Next: combining all prepared data and finalizing our calculation. For example, if your clicked "use max",
     * we must subtract commission sum from account balance.
     * Read more in body..
     */
    private void onStartExecuteTransaction() {
        idlingManager.setNeedsWait(SendTabFragment.IDLE_SEND_COMPLETE_DIALOG, true);

        getViewState().startDialog(ctx -> {
            final WalletProgressDialog dialog = new WalletProgressDialog.Builder(ctx, R.string.please_wait)
                    .setText(R.string.tx_send_in_progress)
                    .create();
            dialog.setCancelable(false);

            // BIP account exists anyway, no need
            CoinBalance baseAccount = findAccountByCoin(MinterSDK.DEFAULT_COIN).get();
            CoinBalance sendAccount = mFromAccount;
            boolean isBaseAccount = sendAccount.getCoin().equals(MinterSDK.DEFAULT_COIN);

            OperationType type = getTransactionTypeByAddress();
            final boolean enoughBaseForFee;

            // default coin for pay fee - MNT (base coin)
            final GateResult<TransactionCommissionValue> txFeeValue = new GateResult<>();
            txFeeValue.result = new TransactionCommissionValue();
            txFeeValue.result.value = getFeeNormalized();
            enoughBaseForFee = bdGTE(baseAccount.getAmount(), getFee());

            Observable<GateResult<TransactionCommissionValue>> txFeeValueResolver = Observable.just(txFeeValue);
            Observable<GateResult<TxCount>> txNonceResolver = rxGate(estimateRepo.getTransactionCount(mFromAccount.address)).onErrorResumeNext(toGateError());

            // if enough balance on base BIP account, set it as gas coin
            if (enoughBaseForFee) {
                Timber.tag("TX Send").d("Using base coin commission %s", MinterSDK.DEFAULT_COIN);
                mGasCoin = baseAccount.getCoin();
            }
            // if sending account is not base coin, set gas coin to CUSTOM
            else if (!isBaseAccount) {
                Timber.tag("TX Send").d("Not enough balance in %s to pay fee, using %s coin", MinterSDK.DEFAULT_COIN, sendAccount.getCoin());
                mGasCoin = sendAccount.getCoin();
                // otherwise getting
                Timber.tag("TX Send").d("Resolving REAL fee value in custom coin %s relatively to base coin", mFromAccount.getCoin());
                // resolving fee currency for custom currency
                // creating tx
                try {
                    final TransactionSign preSign = createPreTx(type);
                    txFeeValueResolver = rxGate(estimateRepo.getTransactionCommission(preSign)).onErrorResumeNext(toGateError());
                } catch (OperationInvalidDataException e) {
                    Timber.w(e);
                    final GateResult<TransactionCommissionValue> commissionValue = new GateResult<>();
                    txFeeValue.result.value = getFeeNormalized();
                    txFeeValueResolver = Observable.just(commissionValue);
                }
            }

            // creating preparation result to send transaction
            Disposable d = Observable
                    .combineLatest(
                            txFeeValueResolver,
                            txNonceResolver,
                            TxInitData::new
                    )
                    .switchMap((Function<TxInitData, ObservableSource<GateResult<TransactionSendResult>>>) txInitData -> {
                        // if in previous request we've got error, returning it
                        if (!txInitData.isSuccess()) {
                            return Observable.just(GateResult.copyError(txInitData.errorResult));
                        }

                        final BigDecimal amountToSend;

                        // don't calc fee if enough balance in base coin and we are sending not a base coin (MNT or BIP)
                        if (enoughBaseForFee && !isBaseAccount) {
                            txInitData.commission = BigDecimal.ZERO;
                        }

                        // if balance enough to send required sum + fee, do nothing
                        // (mAmount + txInitData.commission) <= mFromAccount.getBalance()
                        if (bdGTE(/*total*/mFromAccount.getAmount(), /*send+fee*/mAmount.add(txInitData.commission))) {
                            Timber.tag("TX Send").d("Don't change sending amount - balance enough to send");
                            amountToSend = mAmount;
                        }
                        // if balance not enough to send required sum + fee - subtracting fee from sending sum ("use max" for example)
                        else {
                            if (!mUseMax.get()) {
                                txInitData.commission = BigDecimal.ZERO;
                            }
                            amountToSend = mAmount.subtract(txInitData.commission);
                            Timber.tag("TX Send").d("Subtracting sending amount (-%s): balance not enough to send", txInitData.commission);
                        }


                        // if after subtracting fee from sending sum has become less than account balance at all, returning error with message "insufficient funds"
                        // although, this case must handles the blockchain node, nevertheless we handle it to show user more friendly error
                        // amountToSend < 0
                        if (bdLT(amountToSend, 0)) {
                            // follow the my guideline, return result instead of throwing error, it's easily to handle errors
                            // creating error result, in it we'll write error message with required sum
                            GateResult<TransactionSendResult> errorRes;

                            final BigDecimal balanceMustBe = txInitData.commission.add(mAmount);
                            // this means user sending less than his balance, but it's still not enough to pay fee
                            // mAmount < mFromAccount.getBalance()
                            if (bdLT(mAmount, mFromAccount.getAmount())) {
                                // special for humans - calculate how much balance haven't enough balance
                                final BigDecimal notEnough = txInitData.commission.subtract(mFromAccount.getAmount().subtract(mAmount));
                                Timber.tag("TX Send").d("Amount: %s, fromAcc: %s, diff: %s", bdHuman(mAmount), bdHuman(mFromAccount.getAmount()), bdHuman(notEnough));
                                errorRes = createGateErrorPlain(
                                        format("Insufficient funds: not enough %s %s, wanted: %s %s", bdHuman(notEnough), mFromAccount.getCoin(), bdHuman(balanceMustBe), mFromAccount.getCoin()),
                                        BCResult.ResultCode.InsufficientFunds.getValue(),
                                        400
                                );
                            } else {
                                // sum bigger than account balance, so, just show full required sum
                                Timber.tag("TX Send").d("Amount: %s, fromAcc: %s, diff: %s", bdHuman(mAmount), bdHuman(mFromAccount.getAmount()), bdHuman(balanceMustBe));
                                errorRes = createGateErrorPlain(
                                        format("Insufficient funds: wanted %s %s", bdHuman(balanceMustBe), mFromAccount.getCoin()),
                                        BCResult.ResultCode.InsufficientFunds.getValue(),
                                        400
                                );
                            }

                            return Observable.just(errorRes);
                        }

                        return signSendTx(dialog, txInitData.nonce, type, amountToSend);

                    })
                    .doFinally(this::onExecuteComplete)
                    .subscribe(this::onSuccessExecuteTransaction, this::onFailedExecuteTransaction);
            unsubscribeOnDestroy(d);

            return dialog;
        });
    }

    private ObservableSource<GateResult<TransactionSendResult>> signSendTx(WalletProgressDialog dialog, BigInteger nonce, OperationType type, BigDecimal amountToSend) throws OperationInvalidDataException {
        // creating tx
        final Transaction tx = createFinalTx(nonce.add(BigInteger.ONE), type, amountToSend);

        // if user created account with ledger, use it to sign tx
        if (session.getRole() == AuthSession.AuthType.Hardware) {
            dialog.setText("Please, compare transaction hashes: %s", tx.getUnsignedTxHash());
            Timber.d("Unsigned tx hash: %s", tx.getUnsignedTxHash());
            return signSendTxExternally(dialog, tx);
        } else {
            // old school signing
            return signSendTxInternally(tx);
        }
    }

    private ObservableSource<GateResult<TransactionSendResult>> signSendTxInternally(Transaction tx) {
        final SecretData data = secretStorage.getSecret(mFromAccount.address);
        final TransactionSign sign = tx.signSingle(data.getPrivateKey());

        return safeSubscribeIoToUi(
                rxGate(gateTxRepo.sendTransaction(sign))
                        .onErrorResumeNext(toGateError())
        );
    }

    private ObservableSource<GateResult<TransactionSendResult>> signSendTxExternally(WalletProgressDialog dialog, Transaction tx) {
        RxMinterLedger devInstance = Wallet.app().ledger();
        if (!devInstance.isReady()) {
            dialog.setText("Connect ledger and open Minter Application");
        }

        return RxMinterLedger
                .initObserve(devInstance)
                .flatMap(dev -> {
                    dialog.setText("Compare hashes: " + tx.getUnsignedTxHash().toHexString());
                    return dev.signTxHash(tx.getUnsignedTxHash());
                })
                .toObservable()
                .switchMap(signatureSingleData -> {
                    final TransactionSign sign = tx.signExternal(signatureSingleData);
                    dialog.setText(R.string.tx_send_in_progress);
                    return safeSubscribeIoToUi(
                            rxGate(gateTxRepo.sendTransaction(sign))
                                    .onErrorResumeNext(toGateError())
                    );
                })
                .doFinally(devInstance::destroy)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io());
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

    private void onErrorExecuteTransaction(GateResult<?> errorResult) {
        Timber.e(errorResult.getMessage(), "Unable to send transaction");
        getViewState().startDialog(ctx -> new WalletConfirmDialog.Builder(ctx, "Unable to send transaction")
                .setText((errorResult.getMessage()))
                .setPositiveAction("Close")
                .create());
    }

    private void onSuccessExecuteTransaction(final GateResult<TransactionSendResult> result) {
        if (!result.isOk()) {
            onErrorExecuteTransaction(result);
            return;
        }

        getViewState().hidePayload();

        accountStorage.update(true);
        cachedTxRepo.update(true);
        getViewState().startDialog(ctx -> {
            getAnalytics().send(AppEvent.SentCoinPopupScreen);

            WalletTxSendSuccessDialog.Builder builder = new WalletTxSendSuccessDialog.Builder(ctx, "Success!")
                    .setRecipientName(mRecipient.name)
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
        mRecipient = null;
        mSendFee = null;
    }

    private void onInputTextChanged(EditText editText, boolean valid) {
        final String s = editText.getText().toString();
        switch (editText.getId()) {
            case R.id.input_recipient:
                unsubscribeOnDestroy(
                        addressBookRepo.findByNameOrAddress(s)
                                .subscribe(res -> {
                                    mRecipient = res;
                                    mAddressChange.onNext(mRecipient.name);
                                    getViewState().hideAutocomplete();
                                }, t -> {
                                    mRecipient = null;
                                    getViewState().setSubmitEnabled(false);
                                    addressBookRepo.findSuggestionsByNameOrAddress(s)
                                            .subscribe(suggestions -> {
                                                getViewState().setRecipientAutocompleteItems(suggestions);
                                            }, Timber::w);
                                }));

                break;
            case R.id.input_amount:
                mInputChange.onNext(editText.getText().toString());
                break;
        }
    }

    private void onAutocompleteSelected(AddressContact contact, int pos) {
        getViewState().setRecipient(contact);
    }

    private void onClickAccountSelector(View view) {
        getAnalytics().send(AppEvent.SendCoinsChooseCoinButton);
        getViewState().startAccountSelector(accountStorage.getEntity().getMainWallet().getCoinsList(), this::onAccountSelected);
    }

    private void onAccountSelected(CoinBalance coinAccount) {
        mFromAccount = coinAccount;
        mLastAccount = coinAccount;
        getViewState().setAccountName(format("%s (%s)", coinAccount.coin.toUpperCase(), bdHuman(coinAccount.getAmount())));
    }
}
