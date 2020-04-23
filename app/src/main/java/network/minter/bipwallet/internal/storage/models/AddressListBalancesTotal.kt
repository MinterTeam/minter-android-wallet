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

import com.annimon.stream.Optional
import com.annimon.stream.Stream
import network.minter.core.crypto.MinterAddress
import network.minter.explorer.models.AddressBalance
import org.parceler.Parcel
import java.math.BigDecimal
import java.util.*

@Parcel
class AddressListBalancesTotal {
    @JvmField var balances: MutableList<AddressBalanceTotal> = ArrayList()

    constructor(addresses: List<MinterAddress>) {
        for (address in addresses) {
            balances.add(AddressBalanceTotal(address))
        }
    }

    constructor()

    fun size(): Int {
        return balances.size
    }

    operator fun get(i: Int): AddressBalance {
        return balances[i]
    }

    fun find(address: MinterAddress): Optional<AddressBalanceTotal> {
        return Stream.of(balances)
                .filter { item: AddressBalanceTotal -> item.address != null && item.address == address }
                .findFirst()
    }

    val isEmpty: Boolean
        get() = size() == 0

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

    fun getCoinBalance(address: MinterAddress, coin: String?): BigDecimal {
        return getBalance(address).getCoin(coin).amount
    }
}