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
package network.minter.bipwallet.delegation.adapter

import network.minter.bipwallet.internal.views.widgets.RemoteImageContainer
import network.minter.core.crypto.MinterPublicKey
import network.minter.explorer.models.CoinDelegation
import network.minter.explorer.models.ValidatorMeta
import network.minter.profile.MinterProfileApi
import org.parceler.Parcel
import java.math.BigDecimal

@Parcel
class DelegatedStake : DelegatedItem, RemoteImageContainer, Comparable<DelegatedStake> {
    @JvmField
    var coin: String? = null

    @JvmField
    var amount: BigDecimal = BigDecimal.ZERO

    @JvmField
    var amountBIP: BigDecimal = BigDecimal.ZERO

    @JvmField
    var publicKey: MinterPublicKey? = null

    @JvmField
    var validatorMeta: ValidatorMeta? = null

    constructor()

    constructor(info: CoinDelegation) {
        coin = info.coin!!
        amount = info.amount
        amountBIP = info.bipValue
        publicKey = info.publicKey!!
        validatorMeta = info.meta!!
    }

    override fun isSameOf(item: DelegatedItem): Boolean {
        return viewType == item.viewType && coin == (item as DelegatedStake).coin && publicKey == item.publicKey
    }

    override val viewType: Int
        get() = DelegatedItem.ITEM_STAKE

    override val imageUrl: String
        get() = MinterProfileApi.getCoinAvatarUrl(coin!!)

    override fun compareTo(other: DelegatedStake): Int {
        return other.amountBIP.compareTo(amountBIP)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DelegatedStake) return false

        if (coin != other.coin) return false
        if (amount != other.amount) return false
        if (amountBIP != other.amountBIP) return false
        if (publicKey != other.publicKey) return false
        if (validatorMeta != other.validatorMeta) return false

        return true
    }

    override fun hashCode(): Int {
        var result = coin.hashCode()
        result = 31 * result + amount.hashCode()
        result = 31 * result + amountBIP.hashCode()
        result = 31 * result + publicKey.hashCode()
        result = 31 * result + validatorMeta.hashCode()
        return result
    }
}