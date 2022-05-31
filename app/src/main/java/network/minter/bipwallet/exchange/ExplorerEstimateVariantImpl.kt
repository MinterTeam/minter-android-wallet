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

import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import network.minter.bipwallet.R
import network.minter.bipwallet.internal.exceptions.GateResponseException
import network.minter.bipwallet.internal.helpers.MathHelper.bdHuman
import network.minter.bipwallet.internal.helpers.MathHelper.bdNull
import network.minter.bipwallet.internal.helpers.MathHelper.humanize
import network.minter.bipwallet.internal.helpers.MathHelper.humanizeDecimal
import network.minter.bipwallet.internal.helpers.ViewExtensions.tr
import network.minter.blockchain.api.EstimateSwapFrom
import network.minter.blockchain.models.NodeResult
import network.minter.core.MinterSDK
import network.minter.explorer.models.CoinItemBase
import network.minter.explorer.models.PoolRoute
import network.minter.explorer.repo.ExplorerPoolsRepository
import timber.log.Timber
import java.math.BigDecimal

class ExplorerEstimateVariantImpl(
        private val inst: ExchangeCalculator,
        private val poolsRepo: ExplorerPoolsRepository,
        private val sourceCoin: CoinItemBase,
        private val targetCoin: CoinItemBase,
) : CalculatorVariant {

    override fun calculate(buyCoins: Boolean, onResult: (ExchangeCalculator.CalculationResult) -> Unit, onErrorMessage: (String, NodeResult.Error?) -> Unit) {
        val fees = inst.builder.initFeeData().priceCommissions
        val initData = inst.builder.initFeeData()
        val amount = if (buyCoins) inst.builder.getAmount() else inst.builder.spendAmount()

        if (buyCoins) {
            poolsRepo.getEstimate(sourceCoin, targetCoin, amount, PoolRoute.SwapType.Buy)
                    .map {
                        EstimateResult(PoolRoute.SwapType.Buy, it)
                    }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            { res ->
                                if (!res.isOk) {
                                    if (inst.checkCoinNotExistError(res.errorResult)) {
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

                                val out = ExchangeCalculator.CalculationResult()
                                out.amount = res.amount
                                out.commissionBIP = res.commission
                                out.swapFrom = res.swapFrom
                                out.route = res.route
                                if (res.swapFrom == EstimateSwapFrom.Bancor) {
                                    out.route = PoolRoute()
                                    out.route!!.coins = ArrayList()
                                    out.route!!.coins.add(sourceCoin)
                                    out.route!!.coins.add(targetCoin)
                                }

                                val bipAccount = inst.findAccountByCoin(MinterSDK.DEFAULT_COIN_ID)
                                val getAccount = inst.findAccountByCoin(sourceCoin)


                                // if exchanging via pool, calculate (intermediate coins count of route * delta) + base swap fee
                                var buyFee: BigDecimal = if (out.swapFrom == EstimateSwapFrom.Pool) {
                                    (fees.buyPoolBase + (res.route!!.extraCoinsCount().toBigInteger() * fees.buyPoolDelta)).humanizeDecimal()
                                } else {
                                    fees.buyBancor.humanizeDecimal()
                                }
                                // if default gas coin isn't BIP - multiplying default gas coin rate to BIP amount to get real BIP fee value
                                if (initData.gasRepresentingCoin.id != MinterSDK.DEFAULT_COIN_ID) {
                                    out.commissionBase = buyFee
                                    buyFee = buyFee.multiply(initData.gasBaseCoinRate)
                                    out.commissionBIP = buyFee
                                } else {
                                    out.commissionBase = buyFee
                                    out.commissionBIP = buyFee
                                }
                                val inCoinFee = if (bdNull(amount)) BigDecimal.ZERO else ((res.amount * buyFee) / amount)

                                // if enough (exact) MNT ot pay fee, gas coin is MNT
                                if (bipAccount != null && bipAccount.amount >= buyFee) {
                                    Timber.d("Enough %s to pay fee using %s", MinterSDK.DEFAULT_COIN, MinterSDK.DEFAULT_COIN)

                                    out.gasCoin = bipAccount.coin.id
                                    out.estimate = res.amount
                                    out.calculation = String.format("%s %s", bdHuman(out.amount), sourceCoin)

                                } else if (getAccount != null && getAccount.amount >= (out.amount + inCoinFee)) {
                                    Timber.d("Enough %s to pay fee using instead %s", getAccount.coin, MinterSDK.DEFAULT_COIN)
                                    //(0.000070727942238907*1.5)/1
                                    out.gasCoin = getAccount.coin.id
                                    out.estimate = out.amount + inCoinFee
                                    out.calculation = String.format("%s %s", (out.amount + inCoinFee).humanize(), sourceCoin)
                                } else {
                                    //@todo logic duplication to synchronize with iOS app
                                    Timber.d("Not enough balance in %s and %s to pay fee", MinterSDK.DEFAULT_COIN, getAccount?.coin)
                                    out.gasCoin = getAccount?.coin?.id
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
            poolsRepo.getEstimate(sourceCoin, targetCoin, amount, PoolRoute.SwapType.Sell)
                    .map {
                        EstimateResult(PoolRoute.SwapType.Sell, it)
                    }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe(inst.builder.disposableConsumer)
                    .subscribe(
                            { res ->
                                if (!res.isOk) {
                                    if (inst.checkCoinNotExistError(res.errorResult)) {
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

                                val out = ExchangeCalculator.CalculationResult()
                                out.calculation = String.format("%s %s", bdHuman(res.amount), targetCoin)
                                out.amount = res.amount
                                out.commissionBIP = res.commission
                                val mntAccount = inst.findAccountByCoin(MinterSDK.DEFAULT_COIN_ID)
                                val getAccount = inst.findAccountByCoin(sourceCoin)
                                out.estimate = res.amount
                                out.swapFrom = res.swapFrom
                                out.route = res.route

                                if (res.swapFrom == EstimateSwapFrom.Bancor) {
                                    out.route = PoolRoute()
                                    out.route!!.coins = ArrayList()
                                    out.route!!.coins.add(sourceCoin)
                                    out.route!!.coins.add(targetCoin)
                                }

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
                                if (initData.gasRepresentingCoin.id != MinterSDK.DEFAULT_COIN_ID) {
                                    out.commissionBase = sellFee
                                    sellFee = sellFee.multiply(initData.gasBaseCoinRate)
                                    out.commissionBIP = sellFee
                                } else {
                                    out.commissionBase = sellFee
                                    out.commissionBIP = sellFee
                                }

                                // if enough (exact) MNT ot pay fee, gas coin is MNT
                                if (mntAccount != null && mntAccount.amount >= sellFee) {
                                    Timber.d("Enough %s to pay fee using %s", MinterSDK.DEFAULT_COIN, MinterSDK.DEFAULT_COIN)
                                    out.gasCoin = mntAccount.coin.id
                                }
                                // if enough spending coin at least to pay fee
                                else if (getAccount != null && getAccount.bipValue >= sellFee) {
                                    Timber.d("Enough %s to pay fee using instead %s", getAccount.coin, MinterSDK.DEFAULT_COIN)
                                    out.gasCoin = getAccount.coin.id
                                } else {
                                    Timber.d("Not enough balance in %s and %s to pay fee", MinterSDK.DEFAULT_COIN, getAccount?.coin)
                                    out.gasCoin = mntAccount?.coin?.id
                                    onErrorMessage(tr(R.string.account_err_insufficient_funds), res.errorResult?.error)
                                }
                                onResult(out)
                            },
                            { t ->
                                Timber.e(t, "Unable to get exchange rate")
                                onErrorMessage(tr(R.string.exchange_err_unable_to_get_exchange_rate), null)
                            }
                    )

        }
    }

}