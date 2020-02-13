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
package network.minter.bipwallet.exchange.views;

import android.app.Activity;
import android.view.View;
import android.widget.EditText;

import com.annimon.stream.Optional;
import com.annimon.stream.Stream;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.CallSuper;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import network.minter.bipwallet.R;
import network.minter.bipwallet.advanced.models.CoinAccount;
import network.minter.bipwallet.advanced.models.SecretData;
import network.minter.bipwallet.advanced.models.UserAccount;
import network.minter.bipwallet.advanced.repo.AccountStorage;
import network.minter.bipwallet.advanced.repo.SecretStorage;
import network.minter.bipwallet.analytics.AppEvent;
import network.minter.bipwallet.apis.explorer.CacheTxRepository;
import network.minter.bipwallet.exchange.ExchangeCalculator;
import network.minter.bipwallet.exchange.contract.BaseCoinTabView;
import network.minter.bipwallet.exchange.models.ConvertTransactionData;
import network.minter.bipwallet.exchange.ui.BaseCoinTabFragment;
import network.minter.bipwallet.exchange.ui.dialogs.WalletTxConvertStartDialog;
import network.minter.bipwallet.internal.Wallet;
import network.minter.bipwallet.internal.auth.AuthSession;
import network.minter.bipwallet.internal.data.CachedRepository;
import network.minter.bipwallet.internal.dialogs.WalletConfirmDialog;
import network.minter.bipwallet.internal.dialogs.WalletProgressDialog;
import network.minter.bipwallet.internal.helpers.KeyboardHelper;
import network.minter.bipwallet.internal.helpers.MathHelper;
import network.minter.bipwallet.internal.mvp.MvpBasePresenter;
import network.minter.bipwallet.internal.system.testing.IdlingManager;
import network.minter.bipwallet.tx.contract.TxInitData;
import network.minter.blockchain.models.TransactionSendResult;
import network.minter.blockchain.models.operational.OperationInvalidDataException;
import network.minter.blockchain.models.operational.OperationType;
import network.minter.blockchain.models.operational.Transaction;
import network.minter.blockchain.models.operational.TransactionSign;
import network.minter.core.MinterSDK;
import network.minter.explorer.models.CoinItem;
import network.minter.explorer.models.GateResult;
import network.minter.explorer.models.HistoryTransaction;
import network.minter.explorer.repo.ExplorerCoinsRepository;
import network.minter.explorer.repo.GateEstimateRepository;
import network.minter.explorer.repo.GateGasRepository;
import network.minter.explorer.repo.GateTransactionRepository;
import network.minter.ledger.connector.rxjava2.RxMinterLedger;
import timber.log.Timber;

import static network.minter.bipwallet.apis.reactive.ReactiveExplorer.rxExp;
import static network.minter.bipwallet.apis.reactive.ReactiveGate.createGateErrorPlain;
import static network.minter.bipwallet.apis.reactive.ReactiveGate.rxGate;
import static network.minter.bipwallet.apis.reactive.ReactiveGate.toGateError;
import static network.minter.bipwallet.internal.helpers.MathHelper.bdHuman;
import static network.minter.bipwallet.internal.helpers.MathHelper.bdNull;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public abstract class BaseCoinTabPresenter<V extends BaseCoinTabView> extends MvpBasePresenter<V> {
    protected final SecretStorage mSecretStorage;
    protected final CachedRepository<UserAccount, AccountStorage> mAccountStorage;
    protected final CachedRepository<List<HistoryTransaction>, CacheTxRepository> mTxRepo;
    protected final ExplorerCoinsRepository mExplorerCoinsRepo;
    protected final GateEstimateRepository mEstimateRepository;
    protected final IdlingManager mIdlingManager;
    protected final GateGasRepository mGasRepo;
    protected final GateTransactionRepository mGateTxRepo;

    private AuthSession mSession;
    private CoinAccount mAccount;
    private String mCurrentCoin;
    private String mGetCoin = null;
    private BigDecimal mSpendAmount = new BigDecimal(0);
    private BigDecimal mGetAmount = new BigDecimal(0);
    private BehaviorSubject<Boolean> mInputChange;
    private String mGasCoin;
    private List<CoinAccount> mAccounts = new ArrayList<>(1);
    private AtomicBoolean mUseMax = new AtomicBoolean(false);
    private AtomicBoolean mClickedUseMax = new AtomicBoolean(false);
    private BigInteger mGasPrice = new BigInteger("1");
    private BigDecimal mEstimate;

    public BaseCoinTabPresenter(
            AuthSession session,
            SecretStorage secretStorage,
            CachedRepository<UserAccount, AccountStorage> accountStorage,
            CachedRepository<List<HistoryTransaction>, CacheTxRepository> txRepo,
            ExplorerCoinsRepository explorerCoinsRepository,
            IdlingManager idlingManager,
            GateGasRepository gasRepo,
            GateEstimateRepository estimateRepository,
            GateTransactionRepository gateTxRepo
    ) {
        mSession = session;
        mSecretStorage = secretStorage;
        mAccountStorage = accountStorage;
        mTxRepo = txRepo;
        mExplorerCoinsRepo = explorerCoinsRepository;
        mIdlingManager = idlingManager;
        mGasRepo = gasRepo;
        mEstimateRepository = estimateRepository;
        mGateTxRepo = gateTxRepo;
    }

    @Override
    public void attachView(V view) {
        super.attachView(view);

        loadAndSetFee();
    }

    @Override
    protected void onFirstViewAttach() {
        super.onFirstViewAttach();

        safeSubscribeIoToUi(mAccountStorage.observe())
                .subscribe(res -> {
                    if (!res.isEmpty()) {
                        mAccounts = res.getCoinAccounts();
                        mAccount = res.getCoinAccounts().get(0);
                        if (mCurrentCoin != null) {
                            mAccount = Stream.of(mAccounts).filter(value -> value.getCoin().equals(mCurrentCoin)).findFirst().orElse(mAccount);
                        }
                        onAccountSelected(mAccount, true);
                    }
                });

        mInputChange = BehaviorSubject.create();
        unsubscribeOnDestroy(mInputChange
                .toFlowable(BackpressureStrategy.LATEST)
                .debounce(200, TimeUnit.MILLISECONDS)
                .subscribe(this::onAmountChangedInternal));

        getViewState().setSubmitEnabled(false);
        getViewState().setFormValidationListener(valid -> {
            getViewState().setSubmitEnabled(valid && checkZero(isAmountForGetting() ? mGetAmount : mSpendAmount));
        });
        getViewState().setTextChangedListener(this::onInputChanged);
        getViewState().setOnClickSelectAccount(this::onClickSelectAccount);
        getViewState().setOnClickMaximum(this::onClickMaximum);
        getViewState().setOnClickSubmit(this::onClickSubmit);

        setCoinsAutocomplete();
    }

    protected abstract boolean isAmountForGetting();

    @CallSuper
    protected void setCalculation(String calculation) {
        getViewState().setCalculation(calculation);
    }

    private void loadAndSetFee() {
        mIdlingManager.setNeedsWait(BaseCoinTabFragment.IDLE_WAIT_GAS, true);
        rxGate(mGasRepo.getMinGas())
                .subscribeOn(Schedulers.io())
                .toFlowable(BackpressureStrategy.LATEST)
                .debounce(200, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(res -> {
                    if (res.isOk()) {
                        mGasPrice = res.result.gas;
                        Timber.d("Min Gas price: %s", mGasPrice.toString());
                        getViewState().setFee(String.format("%s %s", bdHuman(getOperationType().getFee().multiply(new BigDecimal(mGasPrice))), MinterSDK.DEFAULT_COIN.toUpperCase()));

                        mIdlingManager.setNeedsWait(BaseCoinTabFragment.IDLE_WAIT_GAS, false);
                    }
                }, e -> {
                    mIdlingManager.setNeedsWait(BaseCoinTabFragment.IDLE_WAIT_GAS, false);
                    Timber.w(e);
                });
    }

    private boolean checkZero(BigDecimal amount) {
        boolean valid = amount == null || !bdNull(amount);
        if (!valid) {
            getViewState().setError("amount", "Amount must be greater than 0");
        } else {
            getViewState().setError("amount", null);
        }

        return valid;
    }

    private void setCoinsAutocomplete() {
        rxExp(mExplorerCoinsRepo.getAll())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(res -> {
                    if (res.result != null) {
                        Stream.of(res.result)
                                .filter(item -> item.symbol.toUpperCase().equals(MinterSDK.DEFAULT_COIN))
                                .forEach(item -> item.reserveBalance = new BigDecimal("10e9"));

                        Collections.sort(res.result, new Comparator<CoinItem>() {
                            @Override
                            public int compare(CoinItem coinItem, CoinItem t1) {
                                return t1.reserveBalance.compareTo(coinItem.reserveBalance);
                            }
                        });
                        getViewState().setCoinsAutocomplete(res.result, (item, position) -> getViewState().setIncomingCoin(item.symbol));
                    }
                }, Wallet.Rx.errorHandler(getViewState()));
    }

    private Optional<CoinAccount> findAccountByCoin(String coin) {
        return Stream.of(mAccounts)
                .filter(item -> item.getCoin().equals(coin.toUpperCase()))
                .findFirst();
    }

    private void onStartExecuteTransaction(final ConvertTransactionData txData) {
        getViewState().startDialog(ctx -> {
            WalletProgressDialog dialog = new WalletProgressDialog.Builder(ctx, "Exchanging")
                    .setText(R.string.tx_convert_began)
                    .create();

            dialog.setCancelable(false);


            Observable
                    .combineLatest(
                            rxGate(mEstimateRepository.getTransactionCount(mAccount.address)).onErrorResumeNext(toGateError()),
                            rxGate(mGasRepo.getMinGas()).onErrorResumeNext(toGateError()),
                            TxInitData::new
                    )
                    .switchMap((Function<TxInitData, ObservableSource<GateResult<TransactionSendResult>>>) initData -> {
                        // if error occurred upper, notify error
                        if (!initData.isSuccess()) {
                            return Observable.just(GateResult.copyError(initData.errorResult));
                        }

                        BigDecimal balance = new BigDecimal("0");

                        if (getOperationType() == OperationType.SellCoin || getOperationType() == OperationType.SellAllCoins) {
                            balance = mAccount.getBalance();
                        }

                        return signSendTx(dialog, txData, initData, balance);
                    })
                    .doOnSubscribe(this::unsubscribeOnDestroy)
                    .onErrorResumeNext(toGateError())
                    .subscribe(BaseCoinTabPresenter.this::onSuccessExecuteTransaction, t -> {
                        onErrorExecuteTransaction(createGateErrorPlain(t));
                    });


            return dialog;
        });
    }

    private ObservableSource<GateResult<TransactionSendResult>> signSendTx(
            WalletProgressDialog dialog,
            ConvertTransactionData txData,
            TxInitData initData,
            BigDecimal balance) throws OperationInvalidDataException {

        // creating tx
        final Transaction tx = txData.build(initData.nonce.add(BigInteger.ONE), initData.gas, balance);

        // if user created account with ledger, use it to sign tx
        if (mSession.getRole() == AuthSession.AuthType.Hardware) {
            dialog.setText("Please, compare transaction hashes: %s", tx.getUnsignedTxHash());
            Timber.d("Unsigned tx hash: %s", tx.getUnsignedTxHash());
            return signSendTxExternally(tx, dialog);
        } else {
            // old school signing
            return signSendTxInternally(tx);
        }
    }

    private ObservableSource<GateResult<TransactionSendResult>> signSendTxInternally(Transaction tx) {
        final SecretData data = mSecretStorage.getSecret(mAccount.address);
        final TransactionSign sign = tx.signSingle(data.getPrivateKey());

        return safeSubscribeIoToUi(
                rxGate(mGateTxRepo.sendTransaction(sign))
                        .onErrorResumeNext(toGateError())
        );
    }

    private ObservableSource<GateResult<TransactionSendResult>> signSendTxExternally(Transaction tx, WalletProgressDialog dialog) {
        RxMinterLedger devInstance = Wallet.app().ledger();
        if (!devInstance.isReady()) {
            dialog.setText("Please, connect ledger and open Minter Application");
        }

        return RxMinterLedger
                .initObserve(devInstance)
                .flatMap(dev -> {
                    dialog.setText("Please, compare hashes: " + tx.getUnsignedTxHash().toHexString());
                    return dev.signTxHash(tx.getUnsignedTxHash());
                })
                .toObservable()
                .switchMap(signatureSingleData -> {
                    final TransactionSign sign = tx.signExternal(signatureSingleData);
                    dialog.setText(R.string.tx_convert_in_progress);
                    return safeSubscribeIoToUi(
                            rxGate(mGateTxRepo.sendTransaction(sign))
                                    .onErrorResumeNext(toGateError())
                    );
                })
                .doFinally(devInstance::destroy)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io());
    }

    private void onSuccessExecuteTransaction(GateResult<TransactionSendResult> result) {
        if (!result.isOk()) {
            onErrorExecuteTransaction(result);
            return;
        }

        mAccountStorage.update(true);
        mTxRepo.update(true);
        getViewState().startDialog(ctx -> new WalletConfirmDialog.Builder(ctx, "Success!")
                .setText("Coins successfully converted!")
                .setPositiveAction("View transaction", (d, v) -> {
                    getViewState().startExplorer(result.result.txHash.toString());
                    getViewState().finish();
                    d.dismiss();
                })
                .setNegativeAction("Close", (d, w) -> {
                    getViewState().finish();
                    d.dismiss();
                })
                .create());
    }

    private void onErrorExecuteTransaction(GateResult<?> errorResult) {
        Timber.e(errorResult.getMessage(), "Unable to send transaction");
        getViewState().startDialog(ctx -> new WalletConfirmDialog.Builder(ctx, "Unable to send transaction")
                .setText((errorResult.getMessage()))
                .setPositiveAction("Close")
                .create());
    }

    private void onClickMaximum(View view) {
        if (isAmountForGetting()) {
            return;
        }

        if (mAccount == null) {
            return;
        }

        getViewState().setAmount(mAccount.balance.stripTrailingZeros().toPlainString());
        mUseMax.set(true);
        mClickedUseMax.set(true);

        getAnalytics().send(AppEvent.ConvertSpendUseMaxButton);

        if (view != null && view.getContext() instanceof Activity) {
            KeyboardHelper.hideKeyboard((Activity) view.getContext());
        }
    }

    private void onClickSelectAccount(View view) {
        getViewState().startAccountSelector(mAccountStorage.getData().getCoinAccounts(), accountItem -> {
            onAccountSelected(accountItem, false);
        });
    }

    private void onInputChanged(EditText editText, boolean valid) {
        String text = editText.getText().toString();

        Timber.d("Input changed: %s", editText.getText());

        loadAndSetFee();

        mIdlingManager.setNeedsWait(BaseCoinTabFragment.IDLE_WAIT_ESTIMATE, true);
        switch (editText.getId()) {
            case R.id.input_incoming_coin:
                mGetCoin = text;
                mInputChange.onNext(isAmountForGetting());
                break;
            case R.id.input_amount:
                final BigDecimal am = MathHelper.bigDecimalFromString(text);
                checkZero(am);

                if (isAmountForGetting()) {
                    mGetAmount = am;
                } else {
                    if(!mClickedUseMax.get()) {
                        mUseMax.set(false);
                    }
                    mClickedUseMax.set(false);
                    mSpendAmount = am;
                }

                getViewState().setSubmitEnabled(mAccount != null && am.compareTo(mAccount.balance) <= 0);

                mInputChange.onNext(isAmountForGetting());
                break;
        }
    }

    private void onClickSubmit(View view) {
        if (mAccount == null || mGetCoin == null || mGetAmount == null || mSpendAmount == null) {
            return;
        }

        if (isAmountForGetting()) {
            getAnalytics().send(AppEvent.ConvertGetExchangeButton);
        } else {
            getAnalytics().send(AppEvent.ConvertSpendExchangeButton);
        }

        Timber.d("Use max: %b", mUseMax.get());

        getViewState().startDialog(ctx -> new WalletTxConvertStartDialog.Builder(ctx, "Convert coin")
                .setAmount(isAmountForGetting() ? mGetAmount : mSpendAmount)
                .setLabel(isAmountForGetting() ? "Buy" : "Spend")
                .setAmountPostfix(isAmountForGetting() ? mGetCoin.toUpperCase() : mAccount.getCoin().toUpperCase())
                .setFromCoin(mAccount.getCoin())
                .setToCoin(mGetCoin)
                .setPositiveAction("Convert!", (d, w) -> {
                    final ConvertTransactionData txData = new ConvertTransactionData(
                            mUseMax.get() ? ConvertTransactionData.Type.SellAll : !isAmountForGetting() ? ConvertTransactionData.Type.Sell : ConvertTransactionData.Type.Buy,
                            mGasCoin,
                            mAccount.getCoin(),
                            mGetCoin,
                            isAmountForGetting() ? mGetAmount : mSpendAmount,
                            mEstimate
                    );

                    onStartExecuteTransaction(txData);
                })
                .setNegativeAction("Cancel")
                .create());
    }

    private void onAmountChangedInternalComplete() {
        mIdlingManager.setNeedsWait(BaseCoinTabFragment.IDLE_WAIT_ESTIMATE, false);
    }

    private OperationType getOperationType() {
        OperationType opType;
        if (mUseMax.get()) {
            opType = OperationType.SellAllCoins;
        } else if (!isAmountForGetting()) {
            opType = OperationType.SellCoin;
        } else {
            opType = OperationType.BuyCoin;
        }

        return opType;
    }

    /**
     * @param incoming
     */
    private void onAmountChangedInternal(Boolean incoming) {
        Timber.d("OnAmountChangedInternal");
        if (mGetCoin == null) {
            Timber.i("Can't exchange: coin is not set");
            onAmountChangedInternalComplete();
            return;
        }

        ExchangeCalculator calculator = new ExchangeCalculator.Builder(mEstimateRepository)
                .setAccount(() -> mAccounts, () -> mAccount)
                .setGetAmount(() -> mGetAmount)
                .setSpendAmount(() -> mSpendAmount)
                .setGetCoin(() -> mGetCoin)
                .doOnSubscribe(this::unsubscribeOnDestroy)
                .build();

        calculator.calculate(getOperationType(), res -> {
            mEstimate = res.getEstimate();
            mGasCoin = res.getGasCoin();
            if (incoming) {
                mSpendAmount = res.getAmount();
            } else {
                mGetAmount = res.getAmount();
            }

            getViewState().setCalculation(res.getCalculation());
            onAmountChangedInternalComplete();
        }, err -> {
            getViewState().setError("income_coin", err);
            onAmountChangedInternalComplete();
        });
    }

    private void onAccountSelected(CoinAccount coinAccount, boolean initial) {
        if (coinAccount == null) return;

        mGasCoin = coinAccount.getCoin();
        mAccount = coinAccount;
        getViewState().setMaximumEnabled(coinAccount.balance.compareTo(new BigDecimal(0)) > 0);

        getViewState().setOutAccountName(String.format("%s (%s)", coinAccount.getCoin().toUpperCase(),
                bdHuman(coinAccount.balance)));

        mCurrentCoin = coinAccount.getCoin();

        if (!initial) {
            mInputChange.onNext(isAmountForGetting());
        }
    }
}
