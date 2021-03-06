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
package network.minter.bipwallet.internal.storage.models

import network.minter.bipwallet.internal.helpers.data.CollectionsHelper
import network.minter.core.MinterSDK
import network.minter.core.crypto.MinterAddress
import network.minter.core.crypto.MinterPublicKey
import network.minter.explorer.models.AddressBalance
import network.minter.explorer.models.CoinDelegation
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class AddressBalanceTotal(
        var delegated: BigDecimal = BigDecimal.ZERO,
        var delegatedCoins: MutableMap<MinterPublicKey, MutableList<CoinDelegation>> = HashMap()
) : AddressBalance() {

    constructor(address: MinterAddress) : this() {
        super.address = address
        fillDefaultsOnEmpty()
    }

    constructor(source: AddressBalance, delegated: BigDecimal) : this() {
        super.address = source.address
        coins = source.coins
        coins = CollectionsHelper.sortByValue(coins, CollectionsHelper.StableCoinSorting())
        // todo: this may be unnecessary, but just in case
        if (source.coinsById == null || source.coinsById.size != coins.size) {
            coins.forEach {
                coinsById[it.value.coin.id] = it.value
            }
        } else {
            coinsById = source.coinsById ?: HashMap()
        }
        coinsById = CollectionsHelper.sortByValue(coinsById, CollectionsHelper.StableCoinSorting())

        totalBalance = source.totalBalance
        totalBalanceUSD = source.totalBalanceUSD
        stakeBalanceBIP = getCoin(MinterSDK.DEFAULT_COIN_ID).amount ?: BigDecimal.ZERO

        this.delegated = delegated
    }

    fun getDelegatedListByValidator(publicKey: MinterPublicKey): MutableList<CoinDelegation> {
        if (!delegatedCoins.containsKey(publicKey)) {
            return ArrayList(0)
        }

        val stakes = delegatedCoins[publicKey] ?: ArrayList(0)
        if (stakes.isEmpty()) {
            return stakes
        }

        Collections.sort(stakes, CollectionsHelper.StableCoinSorting())
        return stakes
    }

    fun getDelegatedByValidatorAndCoin(publicKey: MinterPublicKey?, coin: BigInteger?): CoinDelegation? {
        if (!hasDelegated(publicKey, coin)) return null

        return delegatedCoins[publicKey]!!.first { it.coin.id == coin }
    }

    fun hasDelegated(publicKey: MinterPublicKey?): Boolean {
        if (publicKey == null) return false
        return delegatedCoins.containsKey(publicKey) && delegatedCoins[publicKey] != null
    }

    fun hasDelegated(publicKey: MinterPublicKey?, coin: BigInteger?): Boolean {
        if (publicKey == null || coin == null) return false

        return delegatedCoins[publicKey]?.first { it.coin.id == coin } != null
    }
}