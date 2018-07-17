/*
 * Copyright (C) by MinterTeam. 2018
 * @link https://github.com/MinterTeam
 * @link https://github.com/edwardstock
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

import android.view.View;
import android.widget.EditText;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
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
import network.minter.bipwallet.exchange.ExchangeModule;
import network.minter.bipwallet.exchange.ui.WalletTxConvertStartDialog;
import network.minter.bipwallet.internal.Wallet;
import network.minter.bipwallet.internal.data.CachedRepository;
import network.minter.bipwallet.internal.dialogs.WalletConfirmDialog;
import network.minter.bipwallet.internal.dialogs.WalletProgressDialog;
import network.minter.bipwallet.internal.exceptions.BCResponseException;
import network.minter.bipwallet.internal.mvp.MvpBasePresenter;
import network.minter.blockchain.models.BCResult;
import network.minter.blockchain.models.operational.OperationType;
import network.minter.blockchain.models.operational.Transaction;
import network.minter.blockchain.models.operational.TransactionSign;
import network.minter.blockchain.models.operational.TxCoinBuy;
import network.minter.blockchain.models.operational.TxCoinSell;
import network.minter.blockchain.repo.BlockChainAccountRepository;
import network.minter.blockchain.repo.BlockChainCoinRepository;
import network.minter.core.crypto.BytesData;
import network.minter.explorer.models.HistoryTransaction;
import timber.log.Timber;

import static network.minter.bipwallet.internal.ReactiveAdapter.convertToBcErrorResult;
import static network.minter.bipwallet.internal.ReactiveAdapter.rxCallBc;
import static network.minter.bipwallet.internal.common.Preconditions.firstNonNull;
import static network.minter.bipwallet.internal.helpers.MathHelper.bdLTE;
import static network.minter.blockchain.models.BCResult.ResultCode.CoinDoesNotExists;
import static network.minter.core.MinterSDK.PREFIX_TX;

/**
 * minter-android-wallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public abstract class BaseCoinTabPresenter<V extends ExchangeModule.BaseCoinTabView> extends MvpBasePresenter<V> {
    protected final SecretStorage mSecretStorage;
    protected final CachedRepository<UserAccount, AccountStorage> mAccountStorage;
    protected final CachedRepository<List<HistoryTransaction>, CachedExplorerTransactionRepository> mTxRepo;
    protected final BlockChainCoinRepository mCoinRepo;
    protected final BlockChainAccountRepository mAccountRepo;

    private AccountItem mAccount;
    private String mGetCoin = null;
    private BigDecimal mSpendAmount = new BigDecimal(0);
    private BigDecimal mGetAmount = new BigDecimal(0);
    private BehaviorSubject<Boolean> mInputChange;

    public BaseCoinTabPresenter(
            SecretStorage secretStorage,
            CachedRepository<UserAccount, AccountStorage> accountStorage,
            CachedRepository<List<HistoryTransaction>, CachedExplorerTransactionRepository> txRepo,
            BlockChainCoinRepository coinRepo,
            BlockChainAccountRepository accountRepo
    ) {
        mSecretStorage = secretStorage;
        mAccountStorage = accountStorage;
        mAccountRepo = accountRepo;
        mTxRepo = txRepo;
        mCoinRepo = coinRepo;
    }

    @Override
    protected void onFirstViewAttach() {
        super.onFirstViewAttach();

        safeSubscribeIoToUi(mAccountStorage.observe())
                .subscribe(res -> {
                    if (!res.isEmpty()) {
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
        getViewState().setFormValidationListener(valid -> getViewState().setSubmitEnabled(valid));
        getViewState().setTextChangedListener(this::onInputChanged);
        getViewState().setOnClickSelectAccount(this::onClickSelectAccount);
        getViewState().setOnClickMaximum(this::onClickMaximum);
        getViewState().setOnClickSubmit(this::onClickSubmit);
    }

    protected abstract boolean isAmountForGetting();

    private void onClickSubmit(View view) {
        if (mAccount == null || mGetCoin == null || mGetAmount == null || mSpendAmount == null) {
            return;
        }

        getViewState().startDialog(ctx -> new WalletTxConvertStartDialog.Builder(ctx, "Convert coin")
                .setAmount(mSpendAmount)
                .setFromCoin(mAccount.coin)
                .setToCoin(mGetCoin)
                .setPositiveAction("Convert!", (d, w) -> onStartExecuteTransaction())
                .setNegativeAction("Cancel")
                .create());
    }

    private void onStartExecuteTransaction() {
        getViewState().startDialog(ctx -> {
            WalletProgressDialog dialog = new WalletProgressDialog.Builder(ctx, "Exchanging")
                    .setText("Please, wait few seconds")
                    .create();

            dialog.setCancelable(false);

            safeSubscribeIoToUi(rxCallBc(mAccountRepo.getTransactionCount(mAccount.address)))
                    .onErrorResumeNext(convertToBcErrorResult())
                    .switchMap((Function<BCResult<BigInteger>, ObservableSource<BCResult<BytesData>>>) cntRes -> {
                        if (!cntRes.isSuccess()) {
                            return Observable.just(BCResult.copyError(cntRes));
                        }
                        final BigInteger nonce = cntRes.result.add(new BigInteger("1"));


                        final TransactionSign sign;
                        if (isAmountForGetting()) {
                            final Transaction<TxCoinBuy> tx = Transaction.newBuyCoinTransaction(nonce)
                                    .setCoinToSell(mAccount.coin)
                                    .setValueToBuy(mGetAmount)
                                    .setCoinToBuy(mGetCoin)
                                    .build();

                            final SecretData data = mSecretStorage.getSecret(mAccount.address);
                            sign = tx.sign(data.getPrivateKey());
                            data.cleanup();
                        } else {
                            // mAccount.coin, mSpendAmount, mGetCoin
                            final Transaction<TxCoinSell> tx = Transaction.newSellCoinTransaction(nonce)
                                    .setCoinToSell(mAccount.coin)
                                    .setValueToSell(mSpendAmount)
                                    .setCoinToBuy(mGetCoin)
                                    .build();

                            final SecretData data = mSecretStorage.getSecret(mAccount.address);
                            sign = tx.sign(data.getPrivateKey());
                            data.cleanup();
                        }

                        return safeSubscribeIoToUi(rxCallBc(mAccountRepo.sendTransaction(sign)))
                                .onErrorResumeNext(convertToBcErrorResult());

                    }).subscribe(BaseCoinTabPresenter.this::onSuccessExecuteTransaction, Wallet.Rx.errorHandler(getViewState()));


            return dialog;
        });
    }

    private void onSuccessExecuteTransaction(BCResult<BytesData> result) {
        if (!result.isSuccess()) {
            onErrorExecuteTransaction(result);
            return;
        }

        mAccountStorage.update(true);
        mTxRepo.update(true);
        getViewState().startDialog(ctx -> new WalletConfirmDialog.Builder(ctx, "Success!")
                .setText("Coins successfully converted!")
                .setPositiveAction("View transaction", (d, v) -> {
                    getViewState().startExplorer(result.result.toHexString(PREFIX_TX));
                    getViewState().finish();
                    d.dismiss();
                })
                .setNegativeAction("Close", (d, w) -> {
                    getViewState().finish();
                    d.dismiss();
                })
                .create());
    }

    private void onErrorExecuteTransaction(BCResult<?> errorResult) {
        Timber.e(errorResult.message, "Unable to send transaction");
        getViewState().startDialog(ctx -> new WalletConfirmDialog.Builder(ctx, "Unable to send transaction")
                .setText(errorResult.message)
                .setPositiveAction("Close")
                .create());
    }

    private void onClickMaximum(View view) {
        if (isAmountForGetting()) {
            getViewState().setAmount(mAccount.balance.subtract(OperationType.BuyCoin.getFee()).toPlainString());
        } else {
            getViewState().setAmount(mAccount.balance.subtract(OperationType.SellCoin.getFee()).toPlainString());
        }
    }

    private void onClickSelectAccount(View view) {
        getViewState().startAccountSelector(mAccountStorage.getData().getAccounts(), accountItem -> {
            onAccountSelected(accountItem, false);
        });
    }

    private void onInputChanged(EditText editText, boolean valid) {
        final String text = editText.getText().toString();

        switch (editText.getId()) {
            case R.id.input_incoming_coin:
                mGetCoin = text;
                mInputChange.onNext(isAmountForGetting());
                break;
            case R.id.input_amount:
                final BigDecimal am;
                if (!text.isEmpty()) {
                    am = new BigDecimal(text);
                } else {
                    am = new BigDecimal(0);
                }

                if (isAmountForGetting()) {
                    mGetAmount = am;
                } else {
                    mSpendAmount = am;
                }
                getViewState().setSubmitEnabled(am.compareTo(mAccount.balance) <= 0);
                mInputChange.onNext(isAmountForGetting());
                break;
        }
    }

    private void onAmountChangedInternal(Boolean incoming) {
        if (mGetCoin == null) {
            Timber.i("Can't exchange: coin is not set");
            return;
        }

        if (incoming) {
            rxCallBc(mCoinRepo.getCoinExchangeCurrencyToBuy(mAccount.coin, mGetAmount, mGetCoin))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .onErrorResumeNext(convertToBcErrorResult())
                    .doOnSubscribe(this::unsubscribeOnDestroy)
                    .subscribe(res -> {
                        if (!res.isSuccess()) {
                            if (res.code == BCResult.ResultCode.EmptyResponse || res.statusCode == 404 || res.code == CoinDoesNotExists) {
                                getViewState().setError("income_coin", firstNonNull(res.message, "Coin does not exists"));
                                return;
                            }
                            Timber.w(new BCResponseException(res));
                            return;
                        }
                        getViewState().setError("income_coin", null);
                        BigDecimal amount = new BigDecimal(res.result).divide(Transaction.VALUE_MUL_DEC);
                        getViewState().setCalculation(amount.toPlainString());
                        mSpendAmount = amount;

                        getViewState().setSubmitEnabled(bdLTE(mSpendAmount, mAccount.balance));
                    }, t -> {
                        Timber.e(t, "Unable to get currency for in amount");
                    });
        } else {
            rxCallBc(mCoinRepo.getCoinExchangeCurrencyToSell(mAccount.coin, mSpendAmount, mGetCoin))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .onErrorResumeNext(convertToBcErrorResult())
                    .doOnSubscribe(this::unsubscribeOnDestroy)
                    .subscribe(res -> {
                        if (!res.isSuccess()) {
                            //FIXME: check all possible codes
                            if (res.code == BCResult.ResultCode.EmptyResponse || res.statusCode == 404 || res.code == CoinDoesNotExists) {
                                getViewState().setError("income_coin", firstNonNull(res.message, "Coin does not exists"));
                                return;
                            }
                            Timber.w(new BCResponseException(res));
                            return;
                        }
                        getViewState().setError("income_coin", null);
                        BigDecimal amount = new BigDecimal(res.result).divide(Transaction.VALUE_MUL_DEC);
                        Timber.d("%s %s = %s %s", mSpendAmount.toPlainString(), mAccount.coin, amount, mGetAmount);
                        getViewState().setCalculation(amount.toPlainString());
                        mGetAmount = amount;
                        getViewState().setSubmitEnabled(bdLTE(mSpendAmount, mAccount.balance));
                    }, t -> {
                        Timber.e(t, "Unable to get currency for out amount");
                    });
        }
    }

    private void onAccountSelected(AccountItem accountItem, boolean initial) {
        if (accountItem == null) return;

        mAccount = accountItem;
        getViewState().setMaximumEnabled(accountItem.balance.compareTo(new BigDecimal(0)) > 0);
        getViewState().setOutAccountName(String.format("%s (%s)", accountItem.coin.toUpperCase(), accountItem.balance.toString()));

        if (!initial) {
            mInputChange.onNext(false);
        }
    }


}
