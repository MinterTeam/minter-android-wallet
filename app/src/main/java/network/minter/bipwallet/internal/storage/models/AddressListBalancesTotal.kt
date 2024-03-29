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
package network.minter.bipwallet.internal.storage.models

import network.minter.bipwallet.internal.helpers.data.CollectionsHelper
import network.minter.core.crypto.MinterAddress
import network.minter.explorer.models.AddressBalance
import network.minter.explorer.models.CoinItemBase
import java.math.BigDecimal
import java.util.*

class AddressListBalancesTotal(
        var balances: MutableList<AddressBalanceTotal> = ArrayList(),
        var latestBlockTime: Date? = null
) {

    constructor(addresses: List<MinterAddress>) : this() {
        for (address in addresses) {
            balances.add(AddressBalanceTotal(address))
        }
    }

    fun size(): Int {
        return balances.size
    }

    operator fun get(i: Int): AddressBalance {
        return balances[i]
    }

    fun find(address: MinterAddress): AddressBalanceTotal? {
        return balances.firstOrNull { it.address == address }
    }

    fun remove(address: MinterAddress) {
        if (find(address) == null) {
            return
        }

        balances = CollectionsHelper.removeCopy(balances) {
            it.address == address
        }.toMutableList()
    }

    val isEmpty: Boolean
        get() = size() == 0

    /**
     * Find balance by address. Never returns null. If address not found, function fill it with zero values
     */
    fun getBalance(address: MinterAddress): AddressBalanceTotal {
        for (balance in balances) {
            if (balance.address == address) {
                return balance
            }
        }
        val balance = AddressBalanceTotal()
        balance.address = address
        balance.fillDefaultsOnEmpty()
        return balance
    }

    fun getCoinBalance(address: MinterAddress, coin: String): BigDecimal {
        return getBalance(address).getCoin(coin)?.amount ?: BigDecimal.ZERO
    }

    fun getCoinBalance(address: MinterAddress, coin: CoinItemBase): BigDecimal {
        return getBalance(address).getCoin(coin.id).amount ?: BigDecimal.ZERO
    }
}