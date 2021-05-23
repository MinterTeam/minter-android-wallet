/*
 * Copyright (C) by MinterTeam. 2021
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
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import network.minter.bipwallet.R
import network.minter.bipwallet.apis.reactive.castErrorResultTo
import network.minter.bipwallet.apis.reactive.toObservable
import network.minter.bipwallet.internal.exceptions.GateResponseException
import network.minter.bipwallet.internal.helpers.MathHelper.bdHuman
import network.minter.bipwallet.internal.helpers.MathHelper.humanize
import network.minter.bipwallet.internal.helpers.MathHelper.humanizeDecimal
import network.minter.bipwallet.internal.helpers.ViewExtensions.tr
import network.minter.bipwallet.internal.helpers.data.CollectionsHelper.firstOptional
import network.minter.bipwallet.tx.contract.TxInitData
import network.minter.blockchain.api.EstimateSwapFrom
import network.minter.blockchain.models.*
import network.minter.core.MinterSDK
import network.minter.explorer.models.*
import network.minter.explorer.repo.ExplorerPoolsRepository
import network.minter.explorer.repo.GateEstimateRepository
import timber.log.Timber
import java.math.BigDecimal
import java.math.BigInteger

/**
 * minter-android-wallet. 2019
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
private class EstimateResult {
    var amount: BigDecimal = BigDecimal.ZERO
    var commission: BigDecimal = BigDecimal.ZERO
    var amountWithCommission: BigDecimal = BigDecimal.ZERO
    var swapFrom: EstimateSwapFrom = EstimateSwapFrom.Default
    var route: PoolRoute? = null
    var errorResult: GateResult<*>? = null
    var poolSwapType: PoolRoute.SwapType? = null

    constructor(err: GateResult<*>?) {
        errorResult = err
    }

    constructor(swapType: PoolRoute.SwapType, vararg values: GateResult<*>) {
        poolSwapType = swapType
        for (item in values) {
            if (!item.isOk) {
                errorResult = item.castErrorResultTo<Any>()
                return
            }
        }
        setValues(*values)
    }

    constructor(vararg values: GateResult<*>) {
        for (item in values) {
            if (!item.isOk) {
                errorResult = item.castErrorResultTo<Any>()
                return
            }
        }
        setValues(*values)
    }

    val isOk: Boolean
        get() {
            val tmp = errorResult
            return tmp?.error == null || tmp.isOk
        }

    private fun setValues(vararg values: GateResult<*>) {
        for (item in values) {
            setValue(item)
        }
    }

    private fun setValue(src: GateResult<*>) {
        when (src.result) {
            is ExchangeBuyValue -> {
                amount = (src.result as ExchangeBuyValue).amount
                commission = (src.result as ExchangeBuyValue).getCommission()
                amountWithCommission = (src.result as ExchangeBuyValue).amountWithCommission
                swapFrom = (src.result as ExchangeBuyValue).swapFrom
            }
            is ExchangeSellValue -> {
                amount = (src.result as ExchangeSellValue).amount
                commission = (src.result as ExchangeSellValue).getCommission()
                amountWithCommission = (src.result as ExchangeSellValue).amountWithCommission
                swapFrom = (src.result as ExchangeSellValue).swapFrom
            }
            is PoolRoute -> {
                if (poolSwapType == PoolRoute.SwapType.Sell) {
                    amount = (src.result as PoolRoute).amountOut
                } else if (poolSwapType == PoolRoute.SwapType.Buy) {
                    amount = (src.result as PoolRoute).amountIn
                }

                swapFrom = EstimateSwapFrom.Pool
                route = src.result as PoolRoute
            }
        }
    }
}


class ExchangeCalculator private constructor(private val mBuilder: Builder) {
    fun calculate(buyCoins: Boolean, onResult: (CalculationResult) -> Unit, onErrorMessage: (String, NodeResult.Error?) -> Unit) {
        val repo = mBuilder.estimateRepo
        val poolsRepo = mBuilder.poolsRepo


        // this may happens when user has slow internet or something like this
        val sourceCoin = mBuilder.account().coin
        val targetCoin = mBuilder.getCoin()
        val fees = mBuilder.initFeeData().priceCommissions

        if (targetCoin.id == null) {
            onErrorMessage(tr(R.string.exchange_err_coin_to_buy_not_exists), null)
            return
        }

        if (buyCoins) {
            // get (buy)


//            Observable.combineLatest(
//                    repo.getCoinExchangeCurrencyToBuy(sourceCoin!!, mBuilder.getAmount(), targetCoin),
//                    poolsRepo.getRoute(sourceCoin, targetCoin, mBuilder.getAmount(), PoolRoute.SwapType.Buy),
//                    { simpleEstimateRes: GateResult<ExchangeBuyValue>, poolEstimateRes: GateResult<PoolRoute> ->
//                        // hack for coins that does not have pools between to avoid error
//                        if(sourceCoin.type == CoinItemBase.CoinType.Coin && targetCoin.type == CoinItemBase.CoinType.Coin) {
//                            if(poolEstimateRes.error != null) {
//                                poolEstimateRes.error = null
//                            }
//                        }
//                        EstimateResult(simpleEstimateRes, poolEstimateRes)
//                    }
//            )
            repo.getCoinExchangeCurrencyToBuy(sourceCoin!!, mBuilder.getAmount(), targetCoin)
                    .switchMap { simpleEstimateResult ->

                        if (simpleEstimateResult.isOk) {
                            if (simpleEstimateResult.result.swapFrom == EstimateSwapFrom.Bancor) {
                                Observable.just(
                                        EstimateResult(simpleEstimateResult)
                                )
                            } else {
                                poolsRepo.getRoute(sourceCoin, targetCoin, mBuilder.getAmount(), PoolRoute.SwapType.Buy)
                                        .map { poolEstimateResult ->
                                            EstimateResult(PoolRoute.SwapType.Buy, simpleEstimateResult, poolEstimateResult)
                                        }
                            }
                        } else {
                            if (checkSwapPoolNotExists(simpleEstimateResult)) {
                                poolsRepo.getRoute(sourceCoin, targetCoin, mBuilder.getAmount(), PoolRoute.SwapType.Buy)
                                        .map { poolEstimateResult ->
                                            EstimateResult(PoolRoute.SwapType.Buy, poolEstimateResult)
                                        }
                            } else {
                                EstimateResult(simpleEstimateResult).toObservable()
                            }
                        }
                    }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe(mBuilder.disposableConsumer)
                    .doFinally {
                        mBuilder.onCompleteListener?.invoke()
                    }
                    .subscribe(
                            { res: EstimateResult ->
                                val out = CalculationResult()
                                if (!res.isOk) {
                                    if (checkCoinNotExistError(res.errorResult)) {
                                        onErrorMessage(res.errorResult?.message
                                                ?: tr(R.string.exchange_err_coin_to_buy_not_exists), res.errorResult?.error)
                                        return@subscribe
                                    } else {
                                        if (res.errorResult?.message?.equals("not possible to exchange") == true) {
                                            Timber.d(GateResponseException(res.errorResult))
                                        } else {
                                            Timber.w(GateResponseException(res.errorResult))
                                        }

                                        if (res.errorResult != null) {
                                            onErrorMessage(res.errorResult!!.message
                                                    ?: "Error::${res.errorResult!!.status.name}", res.errorResult?.error)
                                        } else {
                                            onErrorMessage(res.errorResult!!.message
                                                    ?: "Error::UNKNOWN", res.errorResult?.error)
                                        }

                                        return@subscribe
                                    }
                                }
                                out.amount = res.amount
                                out.commissionBIP = res.commission
                                out.swapFrom = res.swapFrom
                                out.route = res.route

                                val bipAccount = findAccountByCoin(MinterSDK.DEFAULT_COIN_ID)
                                val getAccount = findAccountByCoin(sourceCoin)


                                // if exchanging via pool, calculate (intermediate coins count of route * delta) + base swap fee
                                var buyFee: BigDecimal = if (out.swapFrom == EstimateSwapFrom.Pool) {
                                    (fees.buyPoolBase + (res.route!!.extraCoinsCount().toBigInteger() * fees.buyPoolDelta)).humanizeDecimal()
                                } else {
                                    fees.buyBancor.humanizeDecimal()
                                }
                                // if default gas coin isn't BIP - multiplying default gas coin rate to BIP amount to get real BIP fee value
                                if (mBuilder.initFeeData().gasRepresentingCoin.id != MinterSDK.DEFAULT_COIN_ID) {
                                    out.commissionBase = buyFee
                                    buyFee = buyFee.multiply(mBuilder.initFeeData().gasBaseCoinRate)
                                    out.commissionBIP = buyFee
                                } else {
                                    out.commissionBase = buyFee
                                    out.commissionBIP = buyFee
                                }
                                val getAmount = mBuilder.getAmount()
                                val inCoinFee = ((res.amount * buyFee) / getAmount)

                                // if enough (exact) MNT ot pay fee, gas coin is MNT
                                if (bipAccount.get().amount >= buyFee) {
                                    Timber.d("Enough %s to pay fee using %s", MinterSDK.DEFAULT_COIN, MinterSDK.DEFAULT_COIN)

                                    out.gasCoin = bipAccount.get().coin.id
                                    out.estimate = res.amount
                                    out.calculation = String.format("%s %s", bdHuman(out.amount), sourceCoin)

                                } else if (getAccount.isPresent && getAccount.get().amount >= (out.amount + inCoinFee)) {
                                    Timber.d("Enough %s to pay fee using instead %s", getAccount.get().coin, MinterSDK.DEFAULT_COIN)
                                    //(0.000070727942238907*1.5)/1
                                    out.gasCoin = getAccount.get().coin.id
                                    out.estimate = out.amount + inCoinFee
                                    out.calculation = String.format("%s %s", (out.amount + inCoinFee).humanize(), sourceCoin)
                                } else {
                                    //@todo logic duplication to synchronize with iOS app
                                    Timber.d("Not enough balance in %s and %s to pay fee", MinterSDK.DEFAULT_COIN, getAccount.get().coin)
                                    out.gasCoin = getAccount.get().coin.id
                                    out.estimate = out.amount + buyFee
                                    out.calculation = String.format("%s %s", bdHuman(out.amount + inCoinFee), sourceCoin)
                                }
                                onResult(out)
                            },
                            { t ->
                                Timber.e(t, "Unable to get exchange rate")
                                onErrorMessage(tr(R.string.exchange_err_unable_to_get_exchange_rate), null)
                            }
                    )
        } else {
            // spend (sell or sellAll)


//            Observable.combineLatest(
//                    repo.getCoinExchangeCurrencyToSell(sourceCoin!!, mBuilder.spendAmount(), targetCoin),
//                    poolsRepo.getRoute(sourceCoin, targetCoin, mBuilder.spendAmount(), PoolRoute.SwapType.Sell),
//                    { simpleEstimateRes: GateResult<ExchangeSellValue>, poolEstimateRes: GateResult<PoolRoute> ->
//                        // hack for coins that does not have pools between to avoid error
//                        if(sourceCoin.type == CoinItemBase.CoinType.Coin && targetCoin.type == CoinItemBase.CoinType.Coin) {
//                            if(poolEstimateRes.error != null) {
//                                poolEstimateRes.error = null
//                            }
//                        }
//                        EstimateResult(simpleEstimateRes, poolEstimateRes)
//                    }
//            )
            repo.getCoinExchangeCurrencyToSell(sourceCoin!!, mBuilder.spendAmount(), targetCoin)
                    .switchMap { simpleEstimateResult ->
                        if (simpleEstimateResult.isOk) {
                            if (simpleEstimateResult.result.swapFrom == EstimateSwapFrom.Bancor) {
                                EstimateResult(simpleEstimateResult).toObservable()
                            } else {
                                poolsRepo.getRoute(sourceCoin, targetCoin, mBuilder.spendAmount(), PoolRoute.SwapType.Sell)
                                        .map { poolEstimateResult ->
                                            EstimateResult(PoolRoute.SwapType.Sell, simpleEstimateResult, poolEstimateResult)
                                        }
                            }
                        } else {
                            if (checkSwapPoolNotExists(simpleEstimateResult)) {
                                poolsRepo.getRoute(sourceCoin, targetCoin, mBuilder.spendAmount(), PoolRoute.SwapType.Sell)
                                        .map { poolEstimateResult ->
                                            EstimateResult(PoolRoute.SwapType.Sell, poolEstimateResult)
                                        }
                            } else {
                                EstimateResult(simpleEstimateResult).toObservable()
                            }

                        }
                    }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe(mBuilder.disposableConsumer)
                    .doFinally {
                        mBuilder.onCompleteListener?.invoke()
                    }
                    .subscribe(
                            { res: EstimateResult ->
                                if (!res.isOk) {
                                    if (checkCoinNotExistError(res.errorResult)) {
                                        onErrorMessage(res.errorResult?.message
                                                ?: tr(R.string.exchange_err_coin_to_buy_not_exists), res.errorResult?.error)
                                        return@subscribe
                                    } else {
                                        Timber.w(GateResponseException(res.errorResult), "Unable to calculate sell/sellAll currency")

                                        if (res.errorResult != null) {
                                            onErrorMessage(res.errorResult!!.message
                                                    ?: "Error::${res.errorResult!!.status.name}", res.errorResult?.error)
                                        } else {
                                            onErrorMessage(res.errorResult!!.message
                                                    ?: "Error::UNKNOWN", res.errorResult?.error)
                                        }
                                        return@subscribe
                                    }
                                }

                                val out = CalculationResult()
                                out.calculation = String.format("%s %s", bdHuman(res.amount), targetCoin)
                                out.amount = res.amount
                                out.commissionBIP = res.commission
                                val mntAccount = findAccountByCoin(MinterSDK.DEFAULT_COIN_ID)
                                val getAccount = findAccountByCoin(sourceCoin)
                                out.estimate = res.amount
                                out.swapFrom = res.swapFrom
                                out.route = res.route

                                if (out.swapFrom == EstimateSwapFrom.Pool) {
                                    out.calculation = String.format("%s %s", (res.route?.amountOut
                                            ?: res.route?.amountOut ?: res.amount).humanize(), targetCoin)
                                    out.amount = res.route?.amountOut ?: res.amount
                                }

                                // if exchanging via pool, calculate (intermediate coins count of route * delta) + base swap fee
                                var sellFee: BigDecimal = if (out.swapFrom == EstimateSwapFrom.Pool) {
                                    (fees.sellPoolBase + ((res.route?.extraCoinsCount()
                                            ?: 0).toBigInteger() * fees.sellPoolDelta)).humanizeDecimal()
                                } else {
                                    fees.sellBancor.humanizeDecimal()
                                }
                                // if default gas coin isn't BIP - multiplying default gas coin rate to BIP amount to get real BIP fee value
                                if (mBuilder.initFeeData().gasRepresentingCoin.id != MinterSDK.DEFAULT_COIN_ID) {
                                    out.commissionBase = sellFee
                                    sellFee = sellFee.multiply(mBuilder.initFeeData().gasBaseCoinRate)
                                    out.commissionBIP = sellFee
                                } else {
                                    out.commissionBase = sellFee
                                    out.commissionBIP = sellFee
                                }

                                // if enough (exact) MNT ot pay fee, gas coin is MNT
                                if (mntAccount.get().amount >= sellFee) {
                                    Timber.d("Enough %s to pay fee using %s", MinterSDK.DEFAULT_COIN, MinterSDK.DEFAULT_COIN)
                                    out.gasCoin = mntAccount.get().coin.id
                                }
                                // if enough spending coin at least to pay fee
                                else if (getAccount.isPresent && getAccount.get().bipValue >= sellFee) {
                                    Timber.d("Enough %s to pay fee using instead %s", getAccount.get().coin, MinterSDK.DEFAULT_COIN)
                                    out.gasCoin = getAccount.get().coin.id
                                } else {
                                    Timber.d("Not enough balance in %s and %s to pay fee", MinterSDK.DEFAULT_COIN, getAccount.get().coin)
                                    out.gasCoin = mntAccount.get().coin.id
                                    onErrorMessage(tr(R.string.account_err_insufficient_funds), res.errorResult?.error)
                                }
                                onResult(out)
                            },
                            { t: Throwable? ->
                                Timber.e(t, "Unable to get exchange rate")
                                onErrorMessage(tr(R.string.exchange_err_unable_to_get_exchange_rate), null)
                            }
                    )
        }
    }

    // Error hell TODO
    private fun checkCoinNotExistError(res: GateResult<*>?): Boolean {
        if (res == null) {
            return false
        }
        return if (res.error != null) {
            res.error.code == 404 || res.status == BlockchainStatus.CoinNotExists
        } else res.code == 404 || res.code == 400
    }

    private fun checkSwapPoolNotExists(res: GateResult<*>?): Boolean {
        if (res == null) {
            return false
        }

        return res.error != null && res.error.code == 119
    }

    private fun findAccountByCoin(id: BigInteger?): Optional<CoinBalance> {
        return mBuilder.accounts()
                .firstOptional { it.coin.id == id!! }
    }

    private fun findAccountByCoin(coin: CoinItemBase?): Optional<CoinBalance> {
        return mBuilder.accounts()
                .firstOptional { it.coin == coin!! }
    }

    class Builder(
            internal val estimateRepo: GateEstimateRepository,
            internal val poolsRepo: ExplorerPoolsRepository,
            var accounts: () -> List<CoinBalance>,
            var account: () -> CoinBalance,
            var getCoin: () -> CoinItemBase,
            var getAmount: () -> BigDecimal,
            var spendAmount: () -> BigDecimal,
            var initFeeData: () -> TxInitData
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

        fun setGetCoin(getCoin: () -> CoinItemBase): Builder {
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
            var gasCoin: BigInteger? = null,
            var estimate: BigDecimal = BigDecimal.ZERO,
            var amount: BigDecimal = BigDecimal.ZERO,
            var calculation: String? = null,
            var commissionBIP: BigDecimal = BigDecimal.ZERO,
            var commissionBase: BigDecimal = BigDecimal.ZERO,
            var swapFrom: EstimateSwapFrom = EstimateSwapFrom.Default,
            var route: PoolRoute? = null
    )

}
