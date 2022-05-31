/*
 * Copyright (C) by MinterTeam. 2022
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

import io.reactivex.disposables.Disposable
import network.minter.bipwallet.R
import network.minter.bipwallet.apis.reactive.castErrorResultTo
import network.minter.bipwallet.internal.helpers.ViewExtensions.tr
import network.minter.bipwallet.tx.contract.TxInitData
import network.minter.blockchain.api.EstimateSwapFrom
import network.minter.blockchain.models.BlockchainStatus
import network.minter.blockchain.models.ExchangeBuyValue
import network.minter.blockchain.models.ExchangeSellValue
import network.minter.blockchain.models.NodeResult
import network.minter.explorer.models.CoinBalance
import network.minter.explorer.models.CoinItemBase
import network.minter.explorer.models.GateResult
import network.minter.explorer.models.PoolRoute
import network.minter.explorer.repo.ExplorerPoolsRepository
import network.minter.explorer.repo.GateEstimateRepository
import java.math.BigDecimal
import java.math.BigInteger

/**
 * minter-android-wallet. 2019
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
class EstimateResult {
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

                swapFrom = (src.result as PoolRoute).swapIn
                route = src.result as PoolRoute
            }
        }
    }
}

interface CalculatorVariant {
    fun calculate(buyCoins: Boolean, onResult: (ExchangeCalculator.CalculationResult) -> Unit, onErrorMessage: (String, NodeResult.Error?) -> Unit)
}


class ExchangeCalculator private constructor(internal val builder: Builder) {

    fun calculate(buyCoins: Boolean, onResult: (CalculationResult) -> Unit, onErrorMessage: (String, NodeResult.Error?) -> Unit) {
//        val repo = builder.estimateRepo
        val poolsRepo = builder.poolsRepo

        val sourceCoin = builder.account().coin
        val targetCoin = builder.getCoin()
//        val fees = builder.initFeeData().priceCommissions

        // this may happens when user has slow internet or something like this
        if (targetCoin.id == null) {
            onErrorMessage(tr(R.string.exchange_err_coin_to_buy_not_exists), null)
            return
        }

//        val impl = GateAndExplorerEstimateVariantImpl(this, repo, poolsRepo, sourceCoin, targetCoin)
        val impl = ExplorerEstimateVariantImpl(this, poolsRepo, sourceCoin, targetCoin)
        impl.calculate(buyCoins, onResult, onErrorMessage)
    }


    // Error hell TODO
    internal fun checkCoinNotExistError(res: GateResult<*>?): Boolean {
        if (res == null) {
            return false
        }
        return if (res.error != null) {
            res.error.code == 404 || res.status == BlockchainStatus.CoinNotExists
        } else res.code == 404 || res.code == 400
    }

    internal fun checkSwapPoolNotExists(res: GateResult<*>?): Boolean {
        if (res == null) {
            return false
        }

        return res.error != null && res.error.code == 119
    }

    internal fun findAccountByCoin(id: BigInteger?): CoinBalance? {
        return builder.accounts()
                .firstOrNull { it.coin.id == id!! }
    }

    internal fun findAccountByCoin(coin: CoinItemBase?): CoinBalance? {
        return builder.accounts()
                .firstOrNull { it.coin == coin }
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
