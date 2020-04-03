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
import network.minter.bipwallet.internal.common.Lazy;
import network.minter.bipwallet.internal.exceptions.GateResponseException;
import network.minter.blockchain.models.operational.OperationType;
import network.minter.core.MinterSDK;
import network.minter.explorer.models.CoinBalance;
import network.minter.explorer.models.GateResult;
import network.minter.explorer.repo.GateEstimateRepository;
import timber.log.Timber;

import static network.minter.bipwallet.apis.reactive.ReactiveGate.rxGate;
import static network.minter.bipwallet.apis.reactive.ReactiveGate.toGateError;
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
        final GateEstimateRepository repo = mBuilder.mCoinsRepo;

        final Consumer<String> errFunc = onErrorMessage == null ? e -> {
        } : onErrorMessage;

        // this may happens when user has slow internet or something like this
        if (mBuilder.mAccount.get() == null) {
            if (onErrorMessage != null) {
                try {
                    onErrorMessage.accept("Account not loaded yet");
                } catch (Exception e) {
                    Timber.w(e, "Unable to print error while calculating exchange currency");
                }
            }
            return;
        }

        final String sourceCoin = mBuilder.mAccount.get().getCoin();
        final String targetCoin = mBuilder.mGetCoin.get();

        if (opType == OperationType.BuyCoin) {
            // get (buy)
            rxGate(repo.getCoinExchangeCurrencyToBuy(sourceCoin, mBuilder.mGetAmount.get(), targetCoin))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .onErrorResumeNext(toGateError())
                    .doOnSubscribe(mBuilder.mDisposableConsumer)
                    .doFinally(firstNonNull(mBuilder.mOnCompleteListener, () -> {
                    }))
                    .subscribe(res -> {
                        if (!res.isOk()) {
                            if (checkCoinNotExistError(res)) {
                                errFunc.accept(firstNonNull(res.getMessage(), "Coin to buy not exists"));
                                return;
                            } else {
                                Timber.w(new GateResponseException(res));
                                errFunc.accept(firstNonNull(res.getMessage(), String.format("Error::%s", res.error.getResultCode().name())));
                                return;
                            }
                        }

                        final CalculationResult out = new CalculationResult();


                        out.mAmount = res.result.getAmount();
                        out.mCommission = res.result.getCommission();
                        errFunc.accept(null);

                        final Optional<CoinBalance> mntAccount = findAccountByCoin(MinterSDK.DEFAULT_COIN);
                        final Optional<CoinBalance> getAccount = findAccountByCoin(sourceCoin);
                        // if enough (exact) MNT ot pay fee, gas coin is MNT
                        if (bdGTE(mntAccount.get().getAmount(), OperationType.BuyCoin.getFee())) {
                            Timber.d("Enough %s to pay fee using %s", MinterSDK.DEFAULT_COIN, MinterSDK.DEFAULT_COIN);
                            out.mGasCoin = mntAccount.get().getCoin();
                            out.mEstimate = res.result.getAmount();
                            out.mCalculation = String.format("%s %s", bdHuman(res.result.getAmount()), sourceCoin);
                        }
                        // if enough selected account coin ot pay fee, gas coin is selected coin
                        else if (getAccount.isPresent() && bdGTE(getAccount.get().getAmount(), res.result.getAmountWithCommission())) {
                            Timber.d("Enough %s to pay fee using instead %s", getAccount.get().getCoin(), MinterSDK.DEFAULT_COIN);
                            out.mGasCoin = getAccount.get().getCoin();
                            out.mEstimate = res.result.getAmountWithCommission();
                            out.mCalculation = String.format("%s %s", bdHuman(res.result.getAmountWithCommission()), sourceCoin);
                        }
                        // if not enough, break, error
                        else {
                            //@todo logic duplication to synchronize with iOS app
                            Timber.d("Not enough balance in %s and %s to pay fee", MinterSDK.DEFAULT_COIN, getAccount.get().getCoin());
                            out.mGasCoin = getAccount.get().getCoin();
                            out.mEstimate = res.result.getAmountWithCommission();
                            out.mCalculation = String.format("%s %s", bdHuman(res.result.getAmountWithCommission()), sourceCoin);
//                            Timber.d("Not enough balance in %s and %s to pay fee", MinterSDK.DEFAULT_COIN, getAccount.get().getCoin());
//                            out.mGasCoin = mntAccount.get().getCoin();
//                            out.mEstimate = res.result.getAmount();
//                            out.mCalculation = String.format("%s %s", bdHuman(res.result.getAmount()), sourceCoin);
//                            errFunc.accept("Not enough balance");
                        }

                        onResult.accept(out);
                    }, t -> {
                        Timber.e(t, "Unable to get currency");
                        errFunc.accept("Unable to get currency");
                    });
        } else {
            // spend (sell or sellAll)
            rxGate(repo.getCoinExchangeCurrencyToSell(sourceCoin, mBuilder.mSpendAmount.get(), targetCoin))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .onErrorResumeNext(toGateError())
                    .doOnSubscribe(mBuilder.mDisposableConsumer)
                    .doFinally(firstNonNull(mBuilder.mOnCompleteListener, () -> {
                    }))
                    .subscribe(res -> {
                        if (!res.isOk()) {
                            if (checkCoinNotExistError(res)) {
                                errFunc.accept(firstNonNull(res.getMessage(), "Coin to buy not exists"));
                                return;
                            } else {
                                Timber.w(new GateResponseException(res), "Unable to calculate sell/sellAll currency");
                                errFunc.accept(firstNonNull(res.getMessage(), String.format("Error::%s", res.error.getResultCode().name())));
                                return;
                            }
                        }
                        errFunc.accept(null);

                        CalculationResult out = new CalculationResult();

                        out.mCalculation = String.format("%s %s", bdHuman(res.result.getAmount()), targetCoin);
                        out.mAmount = res.result.getAmount();
                        out.mCommission = res.result.getCommission();


                        final Optional<CoinBalance> mntAccount = findAccountByCoin(MinterSDK.DEFAULT_COIN);
                        final Optional<CoinBalance> getAccount = findAccountByCoin(sourceCoin);

                        out.mEstimate = res.result.getAmount();

                        // if enough (exact) MNT ot pay fee, gas coin is MNT
                        if (bdGTE(mntAccount.get().getAmount(), OperationType.SellCoin.getFee())) {
                            Timber.d("Enough %s to pay fee using %s", MinterSDK.DEFAULT_COIN, MinterSDK.DEFAULT_COIN);
                            out.mGasCoin = mntAccount.get().getCoin();
                        }
                        // if enough selected account coin ot pay fee, gas coin is selected account coin
                        else if (getAccount.isPresent() && bdGTE(getAccount.get().getAmount(), res.result.getCommission())) {
                            Timber.d("Enough %s to pay fee using instead %s", getAccount.get().getCoin(), MinterSDK.DEFAULT_COIN);
                            out.mGasCoin = getAccount.get().getCoin();
                        } else {
                            Timber.d("Not enough balance in %s and %s to pay fee", MinterSDK.DEFAULT_COIN, getAccount.get().getCoin());
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

    // Error hell TODO
    private boolean checkCoinNotExistError(GateResult<?> res) {
        if (res == null) {
            return false;
        }

        if (res.error != null) {
            return res.error.code == 404 || res.error.getResultCode() == CoinNotExists;
        }

        return res.statusCode == 404 || res.statusCode == 400;
    }

    private Optional<CoinBalance> findAccountByCoin(String coin) {
        return Stream.of(mBuilder.mAccounts.get())
                .filter(item -> item.getCoin().equals(coin.toUpperCase()))
                .findFirst();
    }

    public static final class Builder {
        private final GateEstimateRepository mCoinsRepo;
        private Action mOnCompleteListener;
        private Consumer<? super Disposable> mDisposableConsumer;
        private Lazy<List<CoinBalance>> mAccounts;
        private Lazy<CoinBalance> mAccount;
        private Lazy<BigDecimal> mGetAmount, mSpendAmount;
        private Lazy<String> mGetCoin;

        public Builder(GateEstimateRepository repo) {
            mCoinsRepo = repo;
        }

        public Builder setAccount(Lazy<List<CoinBalance>> accounts, Lazy<CoinBalance> account) {
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
