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
package network.minter.bipwallet.exchange.views;

import android.support.annotation.CallSuper;
import android.view.View;
import android.widget.EditText;

import com.annimon.stream.Optional;
import com.annimon.stream.Stream;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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
import network.minter.bipwallet.exchange.ExchangeModule;
import network.minter.bipwallet.exchange.models.ConvertTransactionData;
import network.minter.bipwallet.exchange.ui.dialogs.WalletTxConvertStartDialog;
import network.minter.bipwallet.internal.Wallet;
import network.minter.bipwallet.internal.data.CachedRepository;
import network.minter.bipwallet.internal.dialogs.WalletConfirmDialog;
import network.minter.bipwallet.internal.dialogs.WalletProgressDialog;
import network.minter.bipwallet.internal.exceptions.BCExplorerResponseException;
import network.minter.bipwallet.internal.mvp.MvpBasePresenter;
import network.minter.blockchain.models.CountableData;
import network.minter.blockchain.models.TransactionSendResult;
import network.minter.blockchain.models.operational.OperationType;
import network.minter.blockchain.models.operational.Transaction;
import network.minter.blockchain.models.operational.TransactionSign;
import network.minter.core.MinterSDK;
import network.minter.explorer.models.BCExplorerResult;
import network.minter.explorer.models.HistoryTransaction;
import network.minter.explorer.repo.ExplorerCoinsRepository;
import timber.log.Timber;

import static network.minter.bipwallet.internal.ReactiveAdapter.convertToBcExpErrorResult;
import static network.minter.bipwallet.internal.ReactiveAdapter.rxCallBcExp;
import static network.minter.bipwallet.internal.ReactiveAdapter.rxCallExp;
import static network.minter.bipwallet.internal.common.Preconditions.firstNonNull;
import static network.minter.bipwallet.internal.helpers.MathHelper.bdGTE;
import static network.minter.bipwallet.internal.helpers.MathHelper.bdHuman;
import static network.minter.bipwallet.internal.helpers.MathHelper.bdNull;
import static network.minter.blockchain.models.BCResult.ResultCode.CoinNotExists;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public abstract class BaseCoinTabPresenter<V extends ExchangeModule.BaseCoinTabView> extends MvpBasePresenter<V> {
    protected final SecretStorage mSecretStorage;
    protected final CachedRepository<UserAccount, AccountStorage> mAccountStorage;
    protected final CachedRepository<List<HistoryTransaction>, CachedExplorerTransactionRepository> mTxRepo;
    protected final ExplorerCoinsRepository mExplorerCoinsRepo;

    private AccountItem mAccount;
    private String mGetCoin = null;
    private BigDecimal mSpendAmount = new BigDecimal(0);
    private BigDecimal mGetAmount = new BigDecimal(0);
    private BehaviorSubject<Boolean> mInputChange;
    private String mGasCoin;
    private List<AccountItem> mAccounts = new ArrayList<>(1);
    private boolean mUseMax = false;
    private AtomicBoolean mEnableUseMax = new AtomicBoolean(false);

    public BaseCoinTabPresenter(
            SecretStorage secretStorage,
            CachedRepository<UserAccount, AccountStorage> accountStorage,
            CachedRepository<List<HistoryTransaction>, CachedExplorerTransactionRepository> txRepo,
            ExplorerCoinsRepository explorerCoinsRepository
    ) {
        mSecretStorage = secretStorage;
        mAccountStorage = accountStorage;
        mTxRepo = txRepo;
        mExplorerCoinsRepo = explorerCoinsRepository;
    }

    @Override
    protected void onFirstViewAttach() {
        super.onFirstViewAttach();

        safeSubscribeIoToUi(mAccountStorage.observe())
                .subscribe(res -> {
                    if (!res.isEmpty()) {
                        mAccounts = res.getAccounts();
                        mAccount = res.getAccounts().get(0);
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
        rxCallExp(mExplorerCoinsRepo.getAll())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(res -> {
                    if (res.result != null) {
                        getViewState().setCoinsAutocomplete(res.result, (item, position) -> getViewState().setIncomingCoin(item.symbol));
                    }
                }, Wallet.Rx.errorHandler(getViewState()));
    }

    private Optional<AccountItem> findAccountByCoin(String coin) {
        return Stream.of(mAccounts)
                .filter(item -> item.getCoin().equals(coin.toUpperCase()))
                .findFirst();
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

        Timber.d("Use max: %b", mUseMax);

        getViewState().startDialog(ctx -> new WalletTxConvertStartDialog.Builder(ctx, "Convert coin")
                .setAmount(isAmountForGetting() ? mGetAmount : mSpendAmount)
                .setLabel(isAmountForGetting() ? "Buy" : "Spend")
                .setAmountPostfix(isAmountForGetting() ? mGetCoin.toUpperCase() : mAccount.getCoin().toUpperCase())
                .setFromCoin(mAccount.getCoin())
                .setToCoin(mGetCoin)
                .setPositiveAction("Convert!", (d, w) -> {
                    final ConvertTransactionData txData = new ConvertTransactionData(
                            mUseMax ? ConvertTransactionData.Type.SellAll : !isAmountForGetting() ? ConvertTransactionData.Type.Sell : ConvertTransactionData.Type.Buy,
                            mGasCoin,
                            mAccount.getCoin(),
                            mGetCoin,
                            isAmountForGetting() ? mGetAmount : mSpendAmount
                    );

                    onStartExecuteTransaction(txData);
                })
                .setNegativeAction("Cancel")
                .create());
    }

    private void onStartExecuteTransaction(final ConvertTransactionData txData) {
        getViewState().startDialog(ctx -> {
            WalletProgressDialog dialog = new WalletProgressDialog.Builder(ctx, "Exchanging")
                    .setText("Please, wait few seconds")
                    .create();

            dialog.setCancelable(false);

            safeSubscribeIoToUi(
                    rxCallBcExp(mTxRepo.getEntity().getTransactionCount(mAccount.address))
                            .onErrorResumeNext(convertToBcExpErrorResult())
            )
                    .doOnSubscribe(this::unsubscribeOnDestroy)
                    .switchMap((Function<BCExplorerResult<CountableData>, ObservableSource<BCExplorerResult<TransactionSendResult>>>) cntRes -> {
                        if (!cntRes.isSuccess()) {
                            return Observable.just(BCExplorerResult.copyError(cntRes));
                        }
                        final BigInteger nonce = cntRes.result.count.add(new BigInteger("1"));
                        final Transaction tx = txData.build(nonce);
                        final SecretData data = mSecretStorage.getSecret(mAccount.address);
                        final TransactionSign sign = tx.signSingle(data.getPrivateKey());
                        data.cleanup();

                        return safeSubscribeIoToUi(
                                rxCallBcExp(mTxRepo.getEntity().sendTransaction(sign))
                                        .onErrorResumeNext(convertToBcExpErrorResult())
                        );

                    })
                    .doOnSubscribe(this::unsubscribeOnDestroy)
                    .onErrorResumeNext(convertToBcExpErrorResult())
                    .subscribe(BaseCoinTabPresenter.this::onSuccessExecuteTransaction, Wallet.Rx.errorHandler(getViewState()));


            return dialog;
        });
    }

    private void onSuccessExecuteTransaction(BCExplorerResult<TransactionSendResult> result) {
        if (!result.isSuccess()) {
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

    private void onErrorExecuteTransaction(BCExplorerResult<?> errorResult) {
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

        getViewState().setAmount(mAccount.balance.stripTrailingZeros().toPlainString());
        mUseMax = true;

        getAnalytics().send(AppEvent.ConvertSpendUseMaxButton);
    }

    private void onClickSelectAccount(View view) {
        getViewState().startAccountSelector(mAccountStorage.getData().getAccounts(), accountItem -> {
            onAccountSelected(accountItem, false);
        });
    }

    private void onInputChanged(EditText editText, boolean valid) {
        String text = editText.getText().toString();

        switch (editText.getId()) {
            case R.id.input_incoming_coin:
                mGetCoin = text;
                mInputChange.onNext(isAmountForGetting());
                break;
            case R.id.input_amount:
                String amountText = text
                        .replaceAll("\\s", "")
                        .replaceAll("[,.]+", ".")
                        .replace(",", ".");
                if (amountText.isEmpty()) {
                    amountText = "0";
                }
                if (amountText.equals(".")) {
                    amountText = "0";
                } else if (amountText.substring(0, 1).equals(".")) {
                    amountText = "0" + amountText;
                }
                if (amountText.substring(amountText.length() - 1).equals(".")) {
                    amountText = amountText + "0";
                }

                Timber.d("Amount: %s", amountText);
                final BigDecimal am = new BigDecimal(amountText);

                if (isAmountForGetting()) {
                    mGetAmount = am;
                } else {
                    mUseMax = false;
                    mSpendAmount = am;
                }

                getViewState().setSubmitEnabled(mAccount != null && am.compareTo(mAccount.balance) <= 0);

                mInputChange.onNext(isAmountForGetting());
                break;
        }
    }

    private void onAmountChangedInternal(Boolean incoming) {
        if (mGetCoin == null) {
            Timber.i("Can't exchange: coin is not set");
            return;
        }

        Disposable d;
        if (incoming) {
            // get
            d = rxCallBcExp(mExplorerCoinsRepo.getCoinExchangeCurrencyToBuy(mAccount.getCoin(), mGetAmount, mGetCoin))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .onErrorResumeNext(convertToBcExpErrorResult())
                    .doOnSubscribe(this::unsubscribeOnDestroy)
                    .subscribe(res -> {
                        if (!res.isSuccess()) {
                            Timber.i(new BCExplorerResponseException(res));
                            if (res.getErrorCode() == null || res.statusCode == 404 || res.getErrorCode() == CoinNotExists) {
                                getViewState().setError("income_coin", firstNonNull(res.getMessage(), "Coin does not exists"));
                                mEnableUseMax.set(false);
                                return;
                            } else {
                                getViewState().setError("income_coin", firstNonNull(res.getMessage(), String.format("Error::%s", res.getErrorCode().name())));
                                mEnableUseMax.set(false);
                                return;
                            }
                        }
                        mEnableUseMax.set(true);
                        mSpendAmount = res.result.getAmount();
                        getViewState().setError("income_coin", null);


                        final Optional<AccountItem> mntAccount = findAccountByCoin(MinterSDK.DEFAULT_COIN);
                        final Optional<AccountItem> getAccount = findAccountByCoin(mAccount.getCoin());
                        if (bdGTE(mntAccount.get().getBalance(), OperationType.BuyCoin.getFee())) {
                            Timber.d("Enough MNT to pay fee using MNT");
                            mGasCoin = mntAccount.get().getCoin();
                            setCalculation(String.format("%s %s", bdHuman(res.result.getAmount()), mAccount.getCoin()));
                        } else if (getAccount.isPresent() && bdGTE(getAccount.get().getBalance(), res.result.getAmountWithCommission())) {
                            Timber.d("Enough " + getAccount.get().getCoin() + " to pay fee using instead MNT");
                            mGasCoin = getAccount.get().getCoin();
                            setCalculation(String.format("%s %s", bdHuman(res.result.getAmountWithCommission()), mAccount.getCoin()));
                        } else {
                            Timber.d("Not enough balance in MNT and " + getAccount.get().getCoin() + " to pay fee");
                            mGasCoin = mntAccount.get().getCoin();
                            setCalculation(String.format("%s %s", bdHuman(res.result.getAmount()), mAccount.getCoin()));
                            getViewState().setError("income_coin", "Not enough balance");
                        }

                    }, t -> {
                        Timber.e(t, "Unable to get currency");
                        getViewState().setError("income_coin", "Unable to get currency");
                    });
        } else {
            // spend
            d = rxCallBcExp(mExplorerCoinsRepo.getCoinExchangeCurrencyToSell(mAccount.getCoin(), mSpendAmount, mGetCoin))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .onErrorResumeNext(convertToBcExpErrorResult())
                    .doOnSubscribe(this::unsubscribeOnDestroy)
                    .subscribe(res -> {
                        if (!res.isSuccess()) {
                            Timber.w(new BCExplorerResponseException(res));
                            if (res.getErrorCode() == null || res.statusCode == 404 || res.getErrorCode() == CoinNotExists) {
                                getViewState().setError("income_coin", firstNonNull(res.getMessage(), "Coin does not exists"));
                                mEnableUseMax.set(false);
                                return;
                            } else {
                                getViewState().setError("income_coin", firstNonNull(res.getMessage(), String.format("Error::%s", res.getErrorCode().name())));
                                mEnableUseMax.set(false);
                                return;
                            }
                        }
                        mEnableUseMax.set(true);
                        getViewState().setError("income_coin", null);
                        setCalculation(String.format("%s %s", bdHuman(res.result.getAmount()), mGetCoin));
                        mGetAmount = res.result.getAmount();


                        final Optional<AccountItem> mntAccount = findAccountByCoin(MinterSDK.DEFAULT_COIN);
                        final Optional<AccountItem> getAccount = findAccountByCoin(mAccount.getCoin());
                        if (bdGTE(mntAccount.get().getBalance(), OperationType.SellCoin.getFee())) {
                            Timber.d("Enough MNT to pay fee using MNT");
                            mGasCoin = mntAccount.get().getCoin();
                            setCalculation(String.format("%s %s", bdHuman(res.result.getAmount()), mGetCoin));
                        } else if (getAccount.isPresent() && bdGTE(getAccount.get().getBalance(), res.result.getCommission())) {
                            Timber.d("Enough " + getAccount.get().getCoin() + " to pay fee using instead MNT");
                            mGasCoin = getAccount.get().getCoin();
                            setCalculation(String.format("%s %s", bdHuman(res.result.getAmount()), mGetCoin));
                        } else {
                            Timber.d("Not enough balance in MNT and " + getAccount.get().getCoin() + " to pay fee");
                            mGasCoin = mntAccount.get().getCoin();
                            setCalculation(String.format("%s %s", bdHuman(res.result.getAmount()), mGetCoin));
                            getViewState().setError("income_coin", "Not enough balance");
                        }

                    }, t -> {
                        Timber.e(t, "Unable to get currency");
                        getViewState().setError("income_coin", "Unable to get currency");
                    });
        }

        unsubscribeOnDestroy(d);
    }

    private void onAccountSelected(AccountItem accountItem, boolean initial) {
        if (accountItem == null) return;

        mGasCoin = accountItem.getCoin();
        mAccount = accountItem;
        getViewState().setMaximumEnabled(accountItem.balance.compareTo(new BigDecimal(0)) > 0);
        getViewState().setOutAccountName(String.format("%s (%s)", accountItem.getCoin().toUpperCase(), bdHuman(accountItem.balance)));

        if (!initial) {
            mInputChange.onNext(isAmountForGetting());
        }
    }


}
