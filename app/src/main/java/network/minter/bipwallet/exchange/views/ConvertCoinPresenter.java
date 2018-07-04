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

package network.minter.bipwallet.exchange.views;

import android.content.Context;
import android.view.View;
import android.widget.EditText;

import com.arellomobile.mvp.InjectViewState;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import network.minter.bipwallet.R;
import network.minter.bipwallet.advanced.models.AccountItem;
import network.minter.bipwallet.advanced.models.SecretData;
import network.minter.bipwallet.advanced.models.UserAccount;
import network.minter.bipwallet.advanced.repo.AccountStorage;
import network.minter.bipwallet.advanced.repo.SecretStorage;
import network.minter.bipwallet.apis.explorer.CachedExplorerTransactionRepository;
import network.minter.bipwallet.exchange.ExchangeModule;
import network.minter.bipwallet.internal.Wallet;
import network.minter.bipwallet.internal.data.CachedRepository;
import network.minter.bipwallet.internal.dialogs.WalletConfirmDialog;
import network.minter.bipwallet.internal.dialogs.WalletDialog;
import network.minter.bipwallet.internal.dialogs.WalletProgressDialog;
import network.minter.bipwallet.internal.exceptions.BCResponseException;
import network.minter.bipwallet.internal.mvp.MvpBasePresenter;
import network.minter.blockchainapi.models.BCResult;
import network.minter.blockchainapi.models.operational.Transaction;
import network.minter.blockchainapi.models.operational.TransactionSign;
import network.minter.blockchainapi.models.operational.TxConvertCoin;
import network.minter.blockchainapi.repo.BlockChainAccountRepository;
import network.minter.blockchainapi.repo.BlockChainCoinRepository;
import network.minter.explorerapi.models.HistoryTransaction;
import network.minter.mintercore.crypto.BytesData;
import timber.log.Timber;

import static network.minter.bipwallet.internal.ReactiveAdapter.convertToBcErrorResult;
import static network.minter.bipwallet.internal.ReactiveAdapter.rxCallBc;
import static network.minter.mintercore.MinterSDK.PREFIX_TX;

/**
 * MinterWallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@InjectViewState
public class ConvertCoinPresenter extends MvpBasePresenter<ExchangeModule.ConvertCoinView> {
    @Inject SecretStorage secretStorage;
    @Inject CachedRepository<UserAccount, AccountStorage> accountStorage;
    @Inject CachedRepository<List<HistoryTransaction>, CachedExplorerTransactionRepository> txRepo;
    @Inject BlockChainCoinRepository coinRepo;
    @Inject BlockChainAccountRepository accountRepo;

    private AccountItem mFromAccount;
    private String mToCoin = null;
    private BigDecimal mToAmount = new BigDecimal(0);
    private BigDecimal mFromAmount = new BigDecimal(0);
    private AtomicBoolean mIgnoreAmountChange = new AtomicBoolean(false);

    @Inject
    public ConvertCoinPresenter() {
    }

    @Override
    public void attachView(ExchangeModule.ConvertCoinView view) {
        super.attachView(view);
    }

    @Override
    protected void onFirstViewAttach() {
        super.onFirstViewAttach();
        safeSubscribeIoToUi(accountStorage.observe())
                .subscribe(res -> {
                    if (!res.isEmpty()) {
                        mFromAccount = res.getAccounts().get(0);
                        onAccountSelected(mFromAccount, true);
                    }
                });

        getViewState().setSubmitEnabled(false);
        getViewState().setFormValidationListener(valid -> getViewState().setSubmitEnabled(valid));
        getViewState().setTextChangedListener(this::onInputChanged);
        getViewState().setOnClickSelectAccount(this::onClickSelectAccount);
        getViewState().setMaximumTitle("Use max.");
        getViewState().setOnClickMaximum(this::onClickMaximum);
        getViewState().setOnClickSubmit(this::onClickSubmit);
    }

    private void onClickSubmit(View view) {
        if (mFromAccount == null || mToCoin == null || mToAmount == null || mFromAmount == null) {
            return;
        }

        getViewState().startDialog(new WalletDialog.DialogExecutor() {
            @Override
            public WalletDialog run(Context ctx) {
                WalletProgressDialog dialog = new WalletProgressDialog.Builder(ctx, "Exchanging")
                        .setText("Please, wait few seconds")
                        .create();

                dialog.setCancelable(false);

                safeSubscribeIoToUi(rxCallBc(accountRepo.getTransactionCount(mFromAccount.address)))
                        .onErrorResumeNext(convertToBcErrorResult())
                        .switchMap(new Function<BCResult<BigInteger>, ObservableSource<BCResult<BytesData>>>() {
                            @Override
                            public ObservableSource<BCResult<BytesData>> apply(BCResult<BigInteger> cntRes) {
                                if (!cntRes.isSuccess()) {
                                    return Observable.just(BCResult.copyError(cntRes));
                                }

                                final Transaction<TxConvertCoin> tx = Transaction.newConvertCoinTransaction(cntRes.result.add(new BigInteger("1")))
                                        .setAmount(mFromAmount)
                                        .setFromCoin(mFromAccount.coin)
                                        .setToCoin(mToCoin)
                                        .build();

                                final SecretData data = secretStorage.getSecret(mFromAccount.address);
                                final TransactionSign sign = tx.sign(data.getPrivateKey());

                                return safeSubscribeIoToUi(rxCallBc(accountRepo.sendTransaction(sign)))
                                        .onErrorResumeNext(convertToBcErrorResult());
                            }
                        }).subscribe(ConvertCoinPresenter.this::onSuccessExecuteTransaction, Wallet.Rx.errorHandler(getViewState()));


                return dialog;
            }
        });
    }

    private void onSuccessExecuteTransaction(BCResult<BytesData> result) {
        if (!result.isSuccess()) {
            onErrorExecuteTransaction(result);
            return;
        }

        accountStorage.update(true);
        txRepo.update(true);
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
        getViewState().setAmountSpending(mFromAccount.balance.toPlainString());
    }

    private void onClickSelectAccount(View view) {
        getViewState().startAccountSelector(accountStorage.getData().getAccounts(), accountItem -> {
            onAccountSelected(accountItem, false);
        });
    }

    private void onAccountSelected(AccountItem accountItem, boolean initial) {
        if (accountItem == null) return;

        mFromAccount = accountItem;
        getViewState().setMaximumEnabled(accountItem.balance.compareTo(new BigDecimal(0)) > 0);
        getViewState().setOutAccountName(String.format("%s (%s)", accountItem.coin.toUpperCase(), accountItem.balance.toString()));
        getViewState().setMaximumTitle(String.format("Use max. %s %s", accountItem.balance.toString(), accountItem.coin.toUpperCase()));

        if (!initial) {
            onAmountChanged(false);
        }
    }

    private void onInputChanged(EditText editText, boolean valid) {
        final String text = editText.getText().toString();
        Timber.d("Input changed %s", Wallet.app().context().getResources().getResourceName(editText.getId()));
        switch (editText.getId()) {
            case R.id.input_incoming_coin:
                mToCoin = text;
                onAmountChanged(true);
                break;
            case R.id.input_incoming_amount:
                if (mIgnoreAmountChange.get()) {
                    break;
                }
                if (!text.isEmpty()) {
                    mToAmount = new BigDecimal(text);
                } else {
                    mToAmount = new BigDecimal(0);
                }
                getViewState().setSubmitEnabled(mFromAmount.compareTo(mFromAccount.balance) <= 0);
                onAmountChanged(true);
                break;
            case R.id.input_outgoing_amount:
                if (mIgnoreAmountChange.get()) {
                    break;
                }

                if (!text.isEmpty()) {
                    mFromAmount = new BigDecimal(text);
                } else {
                    mFromAmount = new BigDecimal(0);
                }
                getViewState().setSubmitEnabled(mFromAmount.compareTo(mFromAccount.balance) <= 0);
                onAmountChanged(false);
                break;
        }
    }

    private void onAmountChanged(boolean incoming) {
        if (mToCoin == null || mToCoin.isEmpty()) {
            return;
        }
        getViewState().setSubmitEnabled(mFromAmount.compareTo(mFromAccount.balance) <= 0);
        mIgnoreAmountChange.set(true);

        /*
        to:   OTHER = ?
        from: MNT   = 5
        query: OTHER = fromCoin=MNT&toCoin=OTHER&value=5

        to:   OTHER = 10
        from: MNT   = ?
        query: MNT = fromCoin=OTHER&toCoin=MNT&value=10
         */
        if (mToAmount == null) {
            mToAmount = new BigDecimal(1);
            getViewState().setAmountGetting("1");
        }
        if (mFromAmount == null) {
            mFromAmount = new BigDecimal(0);
            getViewState().setAmountSpending("0");
        }

        if (incoming) {
            rxCallBc(coinRepo.getCoinCurrencyConversion(mFromAccount.coin, mToCoin, mToAmount))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .onErrorResumeNext(convertToBcErrorResult())
                    .debounce(300, TimeUnit.MILLISECONDS)
                    .distinctUntilChanged()
                    .doOnSubscribe(this::unsubscribeOnDestroy)
                    .subscribe(res -> {
                        getViewState().setError("income_coin", null);
                        if (!res.isSuccess()) {
                            if (res.code == BCResult.ResultCode.EmptyResponse) {
                                getViewState().setError("income_coin", "Coin not found");
                                return;
                            }
                            Timber.w(new BCResponseException(res));
                            return;
                        }
                        BigDecimal amount = new BigDecimal(res.result).divide(Transaction.VALUE_MUL_DEC);
                        getViewState().setAmountSpending(amount.toPlainString());
                        mFromAmount = amount;
                        getViewState().setSubmitEnabled(mFromAmount.compareTo(mFromAccount.balance) <= 0);
                        mIgnoreAmountChange.set(false);
                    }, t -> {
                        mIgnoreAmountChange.set(false);
                        Timber.e(t, "Unable to get currency for in amount");
                    });
        } else {
            rxCallBc(coinRepo.getCoinCurrencyConversion(mToCoin, mFromAccount.coin, mFromAmount))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .onErrorResumeNext(convertToBcErrorResult())
                    .debounce(300, TimeUnit.MILLISECONDS)
                    .distinctUntilChanged()
                    .doOnSubscribe(this::unsubscribeOnDestroy)
                    .subscribe(new Consumer<BCResult<BigInteger>>() {
                        @Override
                        public void accept(BCResult<BigInteger> res) {
                            getViewState().setError("income_coin", null);
                            if (!res.isSuccess()) {
                                if (res.code == BCResult.ResultCode.EmptyResponse) {
                                    getViewState().setError("income_coin", "Coin not found");
                                    return;
                                }
                                Timber.w(new BCResponseException(res));
                                return;
                            }
                            BigDecimal amount = new BigDecimal(res.result).divide(Transaction.VALUE_MUL_DEC);
                            Timber.d("%s %s = %s %s", mFromAmount.toPlainString(), mFromAccount.coin, amount, mToCoin);
                            getViewState().setAmountGetting(amount.toPlainString());
                            mToAmount = amount;
                            getViewState().setSubmitEnabled(mFromAmount.compareTo(mFromAccount.balance) <= 0);
                            mIgnoreAmountChange.set(false);
                        }
                    }, t -> {
                        mIgnoreAmountChange.set(false);
                        Timber.e(t, "Unable to get currency for out amount");
                    });
        }
    }
}
