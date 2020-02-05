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
import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

import com.annimon.stream.Optional;
import com.annimon.stream.Stream;

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
import network.minter.bipwallet.advanced.models.CoinAccount;
import network.minter.bipwallet.advanced.models.SecretData;
import network.minter.bipwallet.advanced.models.UserAccount;
import network.minter.bipwallet.advanced.repo.AccountStorage;
import network.minter.bipwallet.advanced.repo.SecretStorage;
import network.minter.bipwallet.analytics.AppEvent;
import network.minter.bipwallet.apis.explorer.CacheTxRepository;
import network.minter.bipwallet.internal.Wallet;
import network.minter.bipwallet.internal.auth.AuthSession;
import network.minter.bipwallet.internal.data.CacheManager;
import network.minter.bipwallet.internal.data.CachedRepository;
import network.minter.bipwallet.internal.dialogs.WalletConfirmDialog;
import network.minter.bipwallet.internal.dialogs.WalletProgressDialog;
import network.minter.bipwallet.internal.exceptions.ProfileResponseException;
import network.minter.bipwallet.internal.helpers.KeyboardHelper;
import network.minter.bipwallet.internal.helpers.forms.validators.ByteLengthValidator;
import network.minter.bipwallet.internal.mvp.MvpBasePresenter;
import network.minter.bipwallet.internal.system.SimpleTextWatcher;
import network.minter.bipwallet.internal.system.testing.IdlingManager;
import network.minter.bipwallet.sending.contract.SendView;
import network.minter.bipwallet.sending.models.RecipientItem;
import network.minter.bipwallet.sending.repo.RecipientAutocompleteStorage;
import network.minter.bipwallet.sending.ui.QRCodeScannerActivity;
import network.minter.bipwallet.sending.ui.SendTabFragment;
import network.minter.bipwallet.sending.ui.dialogs.WalletTxSendStartDialog;
import network.minter.bipwallet.sending.ui.dialogs.WalletTxSendSuccessDialog;
import network.minter.bipwallet.tx.contract.TxInitData;
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
import network.minter.explorer.models.ExpResult;
import network.minter.explorer.models.GateResult;
import network.minter.explorer.models.HistoryTransaction;
import network.minter.explorer.models.TxCount;
import network.minter.explorer.models.ValidatorItem;
import network.minter.explorer.repo.ExplorerCoinsRepository;
import network.minter.explorer.repo.ExplorerValidatorsRepository;
import network.minter.explorer.repo.GateEstimateRepository;
import network.minter.explorer.repo.GateGasRepository;
import network.minter.explorer.repo.GateTransactionRepository;
import network.minter.ledger.connector.rxjava2.RxMinterLedger;
import network.minter.profile.MinterProfileApi;
import network.minter.profile.models.ProfileResult;
import network.minter.profile.repo.ProfileInfoRepository;
import timber.log.Timber;

import static java.lang.String.format;
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
import static network.minter.bipwallet.internal.helpers.MathHelper.bigDecimalFromString;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@InjectViewState
public class SendTabPresenter extends MvpBasePresenter<SendView> {
    private static final int REQUEST_CODE_QR_SCAN_ADDRESS = 101;
    private static final BigDecimal PAYLOAD_FEE = BigDecimal.valueOf(0.002);
    @Inject SecretStorage secretStorage;
    @Inject AuthSession session;
    @Inject CachedRepository<List<HistoryTransaction>, CacheTxRepository> cachedTxRepo;
    @Inject CachedRepository<UserAccount, AccountStorage> accountStorage;
    @Inject ExplorerCoinsRepository coinRepo;
    @Inject BlockChainTransactionRepository bcTxRepo;
    @Inject ExplorerValidatorsRepository validatorsRepo;
    @Inject ProfileInfoRepository infoRepo;
    @Inject GateGasRepository gasRepo;
    @Inject CacheManager cache;
    @Inject RecipientAutocompleteStorage recipientStorage;
    @Inject IdlingManager idlingManager;
    @Inject GateEstimateRepository estimateRepo;
    @Inject GateTransactionRepository gateTxRepo;
    private CoinAccount mFromAccount = null;
    private BigDecimal mAmount = null;
    private CharSequence mToMxAddress = null;
    private CharSequence mToMpAddress = null;
    private CharSequence mToName = null;
    private String mAvatar = null;
    private @DrawableRes int mAvatarRes;
    private AtomicBoolean mEnableUseMax = new AtomicBoolean(false);
    private BehaviorSubject<String> mInputChange;
    private BehaviorSubject<String> mAddressChange;
    private String mGasCoin = MinterSDK.DEFAULT_COIN;
    private BigInteger mGasPrice = new BigInteger("1");
    private CoinAccount mLastAccount = null;
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
            setupFee();
        }
    };
    private boolean mFormValid = false;

    private enum SearchByType {
        Address, Username, Email
    }

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
        getViewState().setPayloadChangeListener(mPayloadChangeListener);
        loadAndSetFee();
        accountStorage.update();
    }

    private void onClickAddPayload(View view) {
        getViewState().showPayload();
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
                            onAccountSelected(res.findAccountByCoin(mLastAccount.getCoin()).orElse(res.getCoinAccounts().get(0)));
                        } else {
                            onAccountSelected(res.getCoinAccounts().get(0));
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
        checkEnableSubmit();
        getViewState().setFormValidationListener(valid -> {
            mFormValid = valid;
            checkEnableSubmit();
        });
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
        BigDecimal payloadFee = new BigDecimal(0);
        if (mPayload != null) {
            payloadFee = PAYLOAD_FEE.multiply(BigDecimal.valueOf(mPayload.length));
        }

        if (mSendFee != null) {
            mSendFee = mSendFee.add(payloadFee);
        } else {
            mSendFee = payloadFee;
        }

        fee = format("%s %s", bdHuman(mSendFee), MinterSDK.DEFAULT_COIN.toUpperCase());

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

    private boolean checkEnoughBalance(BigDecimal amount) {
        boolean enough = bdGT(amount, OperationType.SendCoin.getFee());
        if (!enough) {
            getViewState().setAmountError("Insufficient balance");
        } else {
            getViewState().setAmountError(null);
        }
        return enough;
    }

    private boolean checkAmountIsZero(BigDecimal amount) {
        return bdGTE(amount, BigDecimal.ZERO);
    }

    private boolean checkAmountIsZero(String amount) {
        if(amount == null || amount.isEmpty()) {
            getViewState().setAmountError("Amount can't be empty");
            return false;
        }

        boolean valid = bdGTE(bigDecimalFromString(amount), BigDecimal.ZERO);
        if (!valid) {
            getViewState().setAmountError("Amount must be greater or equals to 0");
        } else {
            getViewState().setAmountError(null);
        }

        return valid;
    }

    private void onAmountChanged(String amount) {
        if(amount == null || amount.isEmpty()) {
            mAmount = BigDecimal.ZERO;
        } else {
            mAmount = bigDecimalFromString(amount);
        }

        checkAmountIsZero(amount);
        mEnableUseMax.set(false);
        loadAndSetFee();
    }

    private void onAddressChanged(String address) {
        if (address.isEmpty()) getViewState().setFee("");
        else setupFee();
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
                                    mToName = format("@%s", result.data.user.username);
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
                    .setText(format("Please, wait, we are searching address for user \"%s\"", searchBy))
                    .create();
        });
    }

    private void resolveValidator(final String searchBy, final boolean failOnNotFound) {
        idlingManager.setNeedsWait(SendTabFragment.IDLE_SEND_CONFIRM_DIALOG, true);
        getViewState().startDialog(ctx -> {
            rxProfile(validatorsRepo.getValidators())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(result -> {
                        if (result.isOk()) {
                            ValidatorItem validator = Stream.of(result.result)
                                    .filter(v -> {
                                        if (mToMpAddress.toString().substring(0, 2).equals(MinterSDK.PREFIX_PUBLIC_KEY))
                                            return v.pubKey.toString().equals(mToMpAddress.toString());
                                        else
                                            return v.pubKey.toString().substring(2).equals(mToMpAddress.toString());
                                    })
                                    .findFirst().orElse(null);

                            if (validator != null) {
                                mAvatar = validator.meta.iconUrl;
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
                    .setText(format("Please, wait, we are searching address for validator \"%s\"", searchBy))
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
                            onStartExecuteTransaction();
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

    private void checkEnableSubmit() {
        boolean formFullyValid = checkAmountIsZero(mAmount)
                && mFormValid
                && checkEnoughBalance(mFromAccount.getBalance());

        getViewState().setSubmitEnabled(formFullyValid);
    }

    private void onClickMaximum(View view) {
        if (mFromAccount == null) {
            getViewState().setError("Account didn't loaded yet...");
            return;
        }
        mEnableUseMax.set(true);
        checkEnableSubmit();
        mAmount = mFromAccount.getBalance();
        getViewState().setAmount(mFromAccount.getBalance().stripTrailingZeros().toPlainString());

        getAnalytics().send(AppEvent.SendCoinsUseMaxButton);
        if (view.getContext() instanceof Activity) {
            KeyboardHelper.hideKeyboard((Activity) view.getContext());
        }
    }

    private Optional<CoinAccount> findAccountByCoin(String coin) {
        return accountStorage.getData().findAccountByCoin(coin);
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

        return tx;
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
            CoinAccount baseAccount = findAccountByCoin(MinterSDK.DEFAULT_COIN).get();
            CoinAccount sendAccount = mFromAccount;
            boolean isBaseAccount = sendAccount.getCoin().equals(MinterSDK.DEFAULT_COIN);

            OperationType type = getTransactionTypeByAddress();
            final boolean enoughBaseForFee;

            // default coin for pay fee - MNT (base coin)
            final GateResult<TransactionCommissionValue> txFeeValue = new GateResult<>();
            txFeeValue.result = new TransactionCommissionValue();
            if (type == OperationType.Delegate) {
                enoughBaseForFee = bdGTE(baseAccount.getBalance(), OperationType.Delegate.getFee());
                txFeeValue.result.value = OperationType.Delegate.getFee().multiply(Transaction.VALUE_MUL_DEC).toBigInteger();
            } else {
                enoughBaseForFee = bdGTE(baseAccount.getBalance(), OperationType.SendCoin.getFee());
                txFeeValue.result.value = OperationType.SendCoin.getFee().multiply(Transaction.VALUE_MUL_DEC).toBigInteger();
            }

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
                    if (type == OperationType.Delegate) {
                        txFeeValue.result.value = OperationType.Delegate.getFee().multiply(Transaction.VALUE_MUL_DEC).toBigInteger();
                    } else {
                        txFeeValue.result.value = OperationType.SendCoin.getFee().multiply(Transaction.VALUE_MUL_DEC).toBigInteger();
                    }
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
                            txInitData.commission = new BigDecimal(0);
                        }

                        // if balance enough to send required sum + fee, do nothing
                        // (mAmount + txInitData.commission) <= mFromAccount.getBalance()
                        if (bdLTE(mAmount.add(txInitData.commission), mFromAccount.getBalance())) {
                            Timber.tag("TX Send").d("Don't change sending amount - balance enough to send");
                            amountToSend = mAmount;
                        }
                        // if balance not enough to send required sum + fee - subtracting fee from sending sum ("use max" for example)
                        else {
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
                            if (bdLT(mAmount, mFromAccount.getBalance())) {
                                // special for humans - calculate how much balance haven't enough balance
                                final BigDecimal notEnough = txInitData.commission.subtract(mFromAccount.getBalance().subtract(mAmount));
                                Timber.tag("TX Send").d("Amount: %s, fromAcc: %s, diff: %s", bdHuman(mAmount), bdHuman(mFromAccount.getBalance()), bdHuman(notEnough));
                                errorRes = createGateErrorPlain(
                                        format("Insufficient funds: not enough %s %s, wanted: %s %s", bdHuman(notEnough), mFromAccount.getCoin(), bdHuman(balanceMustBe), mFromAccount.getCoin()),
                                        BCResult.ResultCode.InsufficientFunds.getValue(),
                                        400
                                );
                            } else {
                                // sum bigger than account balance, so, just show full required sum
                                Timber.tag("TX Send").d("Amount: %s, fromAcc: %s, diff: %s", bdHuman(mAmount), bdHuman(mFromAccount.getBalance()), bdHuman(balanceMustBe));
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
        Timber.tag("TX Send").d("Send data: gasCoin=%s, coin=%s, to=%s, from=%s, amount=%s",
                mFromAccount.getCoin(),
                mFromAccount.getCoin(),
                mToName,
                mFromAccount.getAddress().toString(),
                amountToSend
        );

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

    private void onErrorSearchUser(ProfileResult<?> errorResult) {
        Timber.e(errorResult.getError().message, "Unable to find address");
        getViewState().startDialog(ctx -> new WalletConfirmDialog.Builder(ctx, "Error")
                .setText(format("Unable to find user address for user \"%s\": %s", mToName, errorResult.getError().message))
                .setPositiveAction("Close")
                .create());
    }

    private void onErrorSearchValidator(ExpResult<?> errorResult) {
        Timber.e(errorResult.error.message, "Unable to find address");
        getViewState().startDialog(ctx -> new WalletConfirmDialog.Builder(ctx, "Error")
                .setText(format("Unable to find validator for address \"%s\": %s", mToName, errorResult.error.message))
                .setPositiveAction("Close")
                .create());
    }

    private void onErrorSearchValidator() {
        getViewState().startDialog(ctx -> new WalletConfirmDialog.Builder(ctx, "Error")
                .setText(format("Unable to find validator for address \"%s\"", mToName))
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

        final CharSequence to;
        if (getTransactionTypeByAddress() == OperationType.Delegate) {
            to = mToMpAddress;
        } else {
            to = mToMxAddress;
        }
        recipientStorage.add(new RecipientItem(to, mToName), this::setRecipientAutocomplete);

        getViewState().hidePayload();

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
        mSendFee = null;
    }

    private void onInputTextChanged(EditText editText, boolean valid) {
        switch (editText.getId()) {
            case R.id.input_recipient:
                mToName = editText.getText();
                mToMxAddress = null;
                mAddressChange.onNext(mToName.toString());
                break;
            case R.id.input_amount:
                mInputChange.onNext(editText.getText().toString());
                break;
        }
    }

    private void onClickAccountSelector(View view) {
        getAnalytics().send(AppEvent.SendCoinsChooseCoinButton);
        getViewState().startAccountSelector(accountStorage.getData().getCoinAccounts(), this::onAccountSelected);
    }

    private void onAccountSelected(CoinAccount coinAccount) {
        mFromAccount = coinAccount;
        mLastAccount = coinAccount;
        getViewState().setAccountName(format("%s (%s)", coinAccount.coin.toUpperCase(), bdHuman(coinAccount.getBalance())));

    }
}
