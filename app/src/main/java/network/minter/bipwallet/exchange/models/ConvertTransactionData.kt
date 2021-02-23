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
package network.minter.bipwallet.exchange.models

import com.google.common.base.MoreObjects
import network.minter.blockchain.models.operational.OperationInvalidDataException
import network.minter.blockchain.models.operational.Transaction
import network.minter.core.MinterSDK
import network.minter.explorer.models.CoinItemBase
import java.math.BigDecimal
import java.math.BigInteger

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock></edward.vstock>@gmail.com>
 */
class ConvertTransactionData(private val mType: Type, private val mGasCoin: BigInteger, private val mSellCoin: CoinItemBase, private val mBuyCoin: CoinItemBase, private val mAmount: BigDecimal, private val mEstimate: BigDecimal) {
    enum class Type {
        Sell, SellAll, Buy
    }

    var isBasicExchange: Boolean = true

    @Throws(OperationInvalidDataException::class)
    fun build(nonce: BigInteger, gasPrice: BigInteger, balance: BigDecimal?): Transaction {
        val tx: Transaction

        isBasicExchange = !(mSellCoin.type != CoinItemBase.CoinType.Coin || mBuyCoin.type != CoinItemBase.CoinType.Coin)
//        val exchangeViaSwapPool = false

        // if sellAll AND selling coin is a custom coin AND calculator says enough MNT to use as gas coin
        val customToCustom = mType == Type.SellAll && mSellCoin.id != MinterSDK.DEFAULT_COIN_ID && mGasCoin == MinterSDK.DEFAULT_COIN_ID
        if (mType == Type.Sell || customToCustom) {
            // SELL
            val tb = Transaction.Builder(nonce)
                    .setGasCoinId(mGasCoin)
                    .setGasPrice(gasPrice)

            val txData = if (!isBasicExchange) {
                tb.sellSwapPool()
                        .addCoinId(mSellCoin.id)
                        .addCoinId(mBuyCoin.id)
                        .setValueToSell(mAmount)
                        .setMinValueToBuy(BigDecimal("0"))
            } else {
                tb.sellCoin()
                        .setCoinIdToSell(mSellCoin.id)
                        .setCoinIdToBuy(mBuyCoin.id)
                        .setValueToSell(mAmount)
                        .setMinValueToBuy("0")
            }

            tx = txData.build()
        } else if (mType == Type.Buy) {
            // BUY
            val tb = Transaction.Builder(nonce)
                    .setGasCoinId(mGasCoin)
                    .setGasPrice(gasPrice)

            val txData = if (!isBasicExchange) {
                tb.buySwapPool()
                        .addCoinId(mSellCoin.id)
                        .setValueToBuy(mAmount)
                        .addCoinId(mBuyCoin.id)
                        .setMaxValueToSell(mEstimate.multiply(BigDecimal("1.1")))
            } else {
                tb.buyCoin()
                        .setCoinIdToSell(mSellCoin.id)
                        .setValueToBuy(mAmount)
                        .setCoinIdToBuy(mBuyCoin.id)
                        .setMaxValueToSell(mEstimate.multiply(BigDecimal("1.1")))
            }
            tx = txData.build()

        } else {
            // this case used ONLY: when not enough mnt to pay fee with mnt
            // SELL ALL
            val tb = Transaction.Builder(nonce)
                    .setGasCoinId(mGasCoin)
                    .setGasPrice(gasPrice)
            val txData = if (!isBasicExchange) {
                tb.sellAllSwapPool()
                        .addCoinId(mSellCoin.id)
                        .addCoinId(mBuyCoin.id)
                        .setMinValueToBuy("0")
            } else {
                tb.sellAllCoins()
                        .setCoinIdToSell(mSellCoin.id)
                        .setCoinIdToBuy(mBuyCoin.id)
                        .setMinValueToBuy("0")
            }
            tx = txData.build()

        }
        return tx
    }

    private val estimate: BigDecimal
        private get() = MoreObjects.firstNonNull(mEstimate, BigDecimal(0))
}
