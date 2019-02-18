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

package network.minter.bipwallet.exchange;


import com.annimon.stream.Optional;
import com.annimon.stream.Stream;

import java.math.BigDecimal;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import network.minter.bipwallet.advanced.models.AccountItem;
import network.minter.bipwallet.internal.common.Lazy;
import network.minter.bipwallet.internal.exceptions.BCExplorerResponseException;
import network.minter.blockchain.models.operational.OperationType;
import network.minter.core.MinterSDK;
import network.minter.explorer.repo.ExplorerCoinsRepository;
import timber.log.Timber;

import static network.minter.bipwallet.apis.reactive.ReactiveExplorerGate.rxExpGate;
import static network.minter.bipwallet.apis.reactive.ReactiveExplorerGate.toExpGateError;
import static network.minter.bipwallet.internal.common.Preconditions.firstNonNull;
import static network.minter.bipwallet.internal.helpers.MathHelper.bdGTE;
import static network.minter.bipwallet.internal.helpers.MathHelper.bdHuman;
import static network.minter.blockchain.models.BCResult.ResultCode.CoinNotExists;

/**
 * minter-android-wallet. 2019
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
public class ExchangeCalculator {
    private Builder mBuilder;

    private ExchangeCalculator(Builder builder) {
        mBuilder = builder;
    }

    public void calculate(OperationType opType, Consumer<CalculationResult> onResult, Consumer<String> onErrorMessage) {
        final ExplorerCoinsRepository repo = mBuilder.mCoinsRepo;

        final Consumer<String> errFunc = onErrorMessage == null ? e -> {
        } : onErrorMessage;

        final String sourceCoin = mBuilder.mAccount.get().getCoin();
        final String targetCoin = mBuilder.mGetCoin.get();

        if (opType == OperationType.BuyCoin) {
            // get
            rxExpGate(repo.getCoinExchangeCurrencyToBuy(sourceCoin, mBuilder.mGetAmount.get(), targetCoin))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .onErrorResumeNext(toExpGateError())
                    .doOnSubscribe(mBuilder.mDisposableConsumer)
                    .doFinally(firstNonNull(mBuilder.mOnCompleteListener, () -> {
                    }))
                    .subscribe(res -> {
                        if (!res.isSuccess()) {
                            if (res.statusCode == 404 || res.statusCode == 400 || res.getErrorCode() == CoinNotExists) {
                                errFunc.accept(firstNonNull(res.getMessage(), "Coin to buy not exists"));
                                return;
                            } else {
                                Timber.w(new BCExplorerResponseException(res));
                                errFunc.accept(firstNonNull(res.getMessage(), String.format("Error::%s", res.getErrorCode().name())));
                                return;
                            }
                        }

                        final CalculationResult out = new CalculationResult();


                        out.mAmount = res.result.getAmount();
                        out.mCommission = res.result.getCommission();
                        errFunc.accept(null);

                        final Optional<AccountItem> mntAccount = findAccountByCoin(MinterSDK.DEFAULT_COIN);
                        final Optional<AccountItem> getAccount = findAccountByCoin(sourceCoin);
                        // if enough (exact) MNT ot pay fee, gas coin is MNT
                        if (bdGTE(mntAccount.get().getBalance(), OperationType.BuyCoin.getFee())) {
                            Timber.d("Enough MNT to pay fee using MNT");
                            out.mGasCoin = mntAccount.get().getCoin();
                            out.mEstimate = res.result.getAmount();
                            out.mCalculation = String.format("%s %s", bdHuman(res.result.getAmount()), sourceCoin);
                        }
                        // if enough selected account coin ot pay fee, gas coin is selected coin
                        else if (getAccount.isPresent() && bdGTE(getAccount.get().getBalance(), res.result.getAmountWithCommission())) {
                            Timber.d("Enough " + getAccount.get().getCoin() + " to pay fee using instead MNT");
                            out.mGasCoin = getAccount.get().getCoin();
                            out.mEstimate = res.result.getAmountWithCommission();
                            out.mCalculation = String.format("%s %s", bdHuman(res.result.getAmountWithCommission()), sourceCoin);
                        }
                        // if not enough, break, error
                        else {
                            Timber.d("Not enough balance in MNT and " + getAccount.get().getCoin() + " to pay fee");
                            out.mGasCoin = mntAccount.get().getCoin();
                            out.mEstimate = res.result.getAmount();
                            out.mCalculation = String.format("%s %s", bdHuman(res.result.getAmount()), sourceCoin);
                            errFunc.accept("Not enough balance");
                        }

                        onResult.accept(out);
                    }, t -> {
                        Timber.e(t, "Unable to get currency");
                        errFunc.accept("Unable to get currency");
                    });
        } else {
            // spend
            rxExpGate(repo.getCoinExchangeCurrencyToSell(sourceCoin, mBuilder.mSpendAmount.get(), targetCoin))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .onErrorResumeNext(toExpGateError())
                    .doOnSubscribe(mBuilder.mDisposableConsumer)
                    .doFinally(firstNonNull(mBuilder.mOnCompleteListener, () -> {
                    }))
                    .subscribe(res -> {
                        if (!res.isOk()) {
                            if (res.statusCode == 404 || res.statusCode == 400 || res.getErrorCode() == CoinNotExists) {
                                errFunc.accept(firstNonNull(res.getMessage(), "Coin to buy not exists"));
                                return;
                            } else {
                                Timber.w(new BCExplorerResponseException(res));
                                errFunc.accept(firstNonNull(res.getMessage(), String.format("Error::%s", res.getErrorCode().name())));
                                return;
                            }
                        }
                        errFunc.accept(null);

                        CalculationResult out = new CalculationResult();

                        out.mCalculation = String.format("%s %s", bdHuman(res.result.getAmount()), targetCoin);
                        out.mAmount = res.result.getAmount();
                        out.mCommission = res.result.getCommission();


                        final Optional<AccountItem> mntAccount = findAccountByCoin(MinterSDK.DEFAULT_COIN);
                        final Optional<AccountItem> getAccount = findAccountByCoin(sourceCoin);

                        out.mEstimate = res.result.getAmount();

                        // if enough (exact) MNT ot pay fee, gas coin is MNT
                        if (bdGTE(mntAccount.get().getBalance(), OperationType.SellCoin.getFee())) {
                            Timber.d("Enough MNT to pay fee using MNT");
                            out.mGasCoin = mntAccount.get().getCoin();
                        }
                        // if enough selected account coin ot pay fee, gas coin is selected account coin
                        else if (getAccount.isPresent() && bdGTE(getAccount.get().getBalance(), res.result.getCommission())) {
                            Timber.d("Enough " + getAccount.get().getCoin() + " to pay fee using instead MNT");
                            out.mGasCoin = getAccount.get().getCoin();
                        } else {
                            Timber.d("Not enough balance in MNT and " + getAccount.get().getCoin() + " to pay fee");
                            out.mGasCoin = mntAccount.get().getCoin();
                            errFunc.accept("Not enough balance");
                        }

                        onResult.accept(out);
                    }, t -> {
                        Timber.e(t, "Unable to get currency");
                        errFunc.accept("Unable to get currency");
                    });
        }
    }

    private Optional<AccountItem> findAccountByCoin(String coin) {
        return Stream.of(mBuilder.mAccounts.get())
                .filter(item -> item.getCoin().equals(coin.toUpperCase()))
                .findFirst();
    }

    public static final class Builder {
        private final ExplorerCoinsRepository mCoinsRepo;
        private Action mOnCompleteListener;
        private Consumer<? super Disposable> mDisposableConsumer;
        private Lazy<List<AccountItem>> mAccounts;
        private Lazy<AccountItem> mAccount;
        private Lazy<BigDecimal> mGetAmount, mSpendAmount;
        private Lazy<String> mGetCoin;

        public Builder(ExplorerCoinsRepository repo) {
            mCoinsRepo = repo;
        }

        public Builder setAccount(Lazy<List<AccountItem>> accounts, Lazy<AccountItem> account) {
            mAccounts = accounts;
            mAccount = account;
            return this;
        }

        public Builder setOnCompleteListener(Action action) {
            mOnCompleteListener = action;
            return this;
        }

        public Builder setGetAmount(Lazy<BigDecimal> getAmount) {
            mGetAmount = getAmount;
            return this;
        }

        public Builder setSpendAmount(Lazy<BigDecimal> spendAmount) {
            mSpendAmount = spendAmount;
            return this;
        }

        public Builder setGetCoin(Lazy<String> getCoin) {
            mGetCoin = getCoin;
            return this;
        }


        public Builder doOnSubscribe(Consumer<? super Disposable> disposableConsumer) {
            mDisposableConsumer = disposableConsumer;
            return this;
        }

        public ExchangeCalculator build() {
            return new ExchangeCalculator(this);
        }
    }

    public static final class CalculationResult {
        private String mGasCoin;
        private BigDecimal mEstimate;
        private BigDecimal mAmount;
        private String mCalculation;
        private BigDecimal mCommission;

        public BigDecimal getAmount() {
            return mAmount;
        }

        public BigDecimal getCommission() {
            return mCommission;
        }

        public BigDecimal getEstimate() {
            return mEstimate;
        }

        public String getCalculation() {
            return mCalculation;
        }

        public String getGasCoin() {
            return mGasCoin;
        }
    }

}
