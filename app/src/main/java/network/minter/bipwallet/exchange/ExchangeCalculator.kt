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
package network.minter.bipwallet.exchange

import com.annimon.stream.Optional
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import network.minter.bipwallet.apis.reactive.ReactiveGate
import network.minter.bipwallet.apis.reactive.rxGate
import network.minter.bipwallet.internal.exceptions.GateResponseException
import network.minter.bipwallet.internal.helpers.MathHelper.bdHuman
import network.minter.bipwallet.internal.helpers.MathHelper.humanize
import network.minter.bipwallet.internal.helpers.data.CollectionsHelper.firstOptional
import network.minter.blockchain.models.BCResult.ResultCode
import network.minter.blockchain.models.ExchangeBuyValue
import network.minter.blockchain.models.ExchangeSellValue
import network.minter.blockchain.models.operational.OperationType
import network.minter.core.MinterSDK
import network.minter.explorer.models.CoinBalance
import network.minter.explorer.models.GateResult
import network.minter.explorer.repo.GateEstimateRepository
import timber.log.Timber
import java.math.BigDecimal

/**
 * minter-android-wallet. 2019
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
class ExchangeCalculator private constructor(private val mBuilder: Builder) {
    fun calculate(opType: OperationType, onResult: (CalculationResult) -> Unit, onErrorMessage: (String) -> Unit) {
        val repo = mBuilder.estimateRepo


        // this may happens when user has slow internet or something like this
        val sourceCoin = mBuilder.account().coin
        val targetCoin = mBuilder.getCoin()
        if (opType == OperationType.BuyCoin) {
            // get (buy)
            repo.getCoinExchangeCurrencyToBuy(sourceCoin!!, mBuilder.getAmount(), targetCoin)
                    .rxGate()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .onErrorResumeNext(ReactiveGate.toGateError())
                    .doOnSubscribe(mBuilder.disposableConsumer)
                    .doFinally {
                        mBuilder.onCompleteListener?.invoke()
                    }
                    .subscribe({ res: GateResult<ExchangeBuyValue> ->
                        val out = CalculationResult()
                        if (!res.isOk) {
                            if (checkCoinNotExistError(res)) {
                                onErrorMessage(res.message ?: "Coin to buy not exists")
                                return@subscribe
                            } else {
                                Timber.w(GateResponseException(res))
                                onErrorMessage(res.message ?: "Error:${res.error.resultCode.name}")
                                return@subscribe
                            }
                        }
                        out.amount = res.result.amount
                        out.commission = res.result.getCommission()

                        val mntAccount = findAccountByCoin(MinterSDK.DEFAULT_COIN)
                        val getAccount = findAccountByCoin(sourceCoin)
                        // if enough (exact) MNT ot pay fee, gas coin is MNT

                        if (mntAccount.get().amount >= OperationType.BuyCoin.fee) {
                            Timber.d("Enough %s to pay fee using %s", MinterSDK.DEFAULT_COIN, MinterSDK.DEFAULT_COIN)

                            out.gasCoin = mntAccount.get().coin
                            out.estimate = res.result.amount
                            out.calculation = String.format("%s %s", bdHuman(res.result.amount), sourceCoin)

                        } else if (getAccount.isPresent && getAccount.get().amount >= res.result.amountWithCommission) {
                            Timber.d("Enough %s to pay fee using instead %s", getAccount.get().coin, MinterSDK.DEFAULT_COIN)
                            out.gasCoin = getAccount.get().coin
                            out.estimate = res.result.amountWithCommission
                            out.calculation = String.format("%s %s", res.result.amountWithCommission.humanize(), sourceCoin)
                        } else {
                            //@todo logic duplication to synchronize with iOS app
                            Timber.d("Not enough balance in %s and %s to pay fee", MinterSDK.DEFAULT_COIN, getAccount.get().coin)
                            out.gasCoin = getAccount.get().coin
                            out.estimate = res.result.amountWithCommission
                            out.calculation = String.format("%s %s", bdHuman(res.result.amountWithCommission), sourceCoin)
                        }
                        onResult(out)
                    }) { t: Throwable? ->
                        Timber.e(t, "Unable to get currency")
                        onErrorMessage("Unable to get currency")
                    }
        } else {
            // spend (sell or sellAll)
            repo.getCoinExchangeCurrencyToSell(sourceCoin!!, mBuilder.spendAmount(), targetCoin)
                    .rxGate()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .onErrorResumeNext(ReactiveGate.toGateError())
                    .doOnSubscribe(mBuilder.disposableConsumer)
                    .doFinally {
                        mBuilder.onCompleteListener?.invoke()
                    }
                    .subscribe({ res: GateResult<ExchangeSellValue> ->
                        if (!res.isOk) {
                            if (checkCoinNotExistError(res)) {
                                onErrorMessage(res.message ?: "Coin to buy not exists")
                                return@subscribe
                            } else {
                                Timber.w(GateResponseException(res), "Unable to calculate sell/sellAll currency")
                                onErrorMessage(res.message ?: "Error::${res.error.resultCode.name}")
                                return@subscribe
                            }
                        }

                        val out = CalculationResult()
                        out.calculation = String.format("%s %s", bdHuman(res.result.amount), targetCoin)
                        out.amount = res.result.amount
                        out.commission = res.result.getCommission()
                        val mntAccount = findAccountByCoin(MinterSDK.DEFAULT_COIN)
                        val getAccount = findAccountByCoin(sourceCoin)
                        out.estimate = res.result.amount

                        // if enough (exact) MNT ot pay fee, gas coin is MNT

                        if (mntAccount.get().amount >= OperationType.SellCoin.fee) {
                            Timber.d("Enough %s to pay fee using %s", MinterSDK.DEFAULT_COIN, MinterSDK.DEFAULT_COIN)
                            out.gasCoin = mntAccount.get().coin
                        } else if (getAccount.isPresent && getAccount.get().amount >= res.result.getCommission()) {
                            Timber.d("Enough %s to pay fee using instead %s", getAccount.get().coin, MinterSDK.DEFAULT_COIN)
                            out.gasCoin = getAccount.get().coin
                        } else {
                            Timber.d("Not enough balance in %s and %s to pay fee", MinterSDK.DEFAULT_COIN, getAccount.get().coin)
                            out.gasCoin = mntAccount.get().coin
                            onErrorMessage("Not enough balance")
                        }
                        onResult(out)
                    }) { t: Throwable? ->
                        Timber.e(t, "Unable to get currency")
                        onErrorMessage("Unable to get currency")
                    }
        }
    }

    // Error hell TODO
    private fun checkCoinNotExistError(res: GateResult<*>?): Boolean {
        if (res == null) {
            return false
        }
        return if (res.error != null) {
            res.error.code == 404 || res.error.resultCode == ResultCode.CoinNotExists
        } else res.statusCode == 404 || res.statusCode == 400
    }

    private fun findAccountByCoin(coin: String?): Optional<CoinBalance> {
        return mBuilder.accounts()
                .firstOptional { it.coin == coin!! }
    }

    class Builder(
            internal val estimateRepo: GateEstimateRepository,
            var accounts: () -> List<CoinBalance>,
            var account: () -> CoinBalance,
            var getCoin: () -> String,
            var getAmount: () -> BigDecimal,
            var spendAmount: () -> BigDecimal
    ) {

        var onCompleteListener: (() -> Unit)? = null
        var disposableConsumer: ((Disposable) -> Unit)? = null

        fun setOnCompleteListener(action: () -> Unit): Builder {
            onCompleteListener = action
            return this
        }

        fun setGetAmount(getAmount: () -> BigDecimal): Builder {
            this.getAmount = getAmount
            return this
        }

        fun setSpendAmount(spendAmount: () -> BigDecimal): Builder {
            this.spendAmount = spendAmount
            return this
        }

        fun setGetCoin(getCoin: () -> String): Builder {
            this.getCoin = getCoin
            return this
        }

        fun doOnSubscribe(disposableConsumer: (Disposable) -> Unit): Builder {
            this.disposableConsumer = disposableConsumer
            return this
        }

        fun build(): ExchangeCalculator {
            return ExchangeCalculator(this)
        }

    }


    data class CalculationResult(
            var gasCoin: String? = null,
            var estimate: BigDecimal = BigDecimal.ZERO,
            var amount: BigDecimal = BigDecimal.ZERO,
            var calculation: String? = null,
            var commission: BigDecimal = BigDecimal.ZERO
    )

}