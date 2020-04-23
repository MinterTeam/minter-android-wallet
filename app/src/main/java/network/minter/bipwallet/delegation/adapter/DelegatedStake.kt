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
import java.math.BigDecimal
import java.util.*

class DelegatedStake : DelegatedItem, RemoteImageContainer, Comparable<DelegatedStake> {
    @JvmField var coin: String?
    @JvmField var amount: BigDecimal
    @JvmField var amountBIP: BigDecimal
    @JvmField var pubKey: MinterPublicKey?
    @JvmField var validatorMeta: ValidatorMeta? = null

    constructor(info: CoinDelegation) {
        coin = info.coin
        amount = info.amount
        amountBIP = info.bipValue
        pubKey = info.publicKey
        validatorMeta = info.meta
    }

    internal constructor(coin: String?, amount: BigDecimal, amountBIP: BigDecimal, publicKey: MinterPublicKey?) {
        this.coin = coin
        this.amount = amount
        this.amountBIP = amountBIP
        pubKey = publicKey
    }

    override fun isSameOf(item: DelegatedItem?): Boolean {
        return viewType == item?.viewType && coin == (item as DelegatedStake).coin && pubKey == item.pubKey
    }

    override val viewType: Int
        get() = DelegatedItem.ITEM_STAKE

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DelegatedStake) return false
        val that = other
        return coin == that.coin && amount == that.amount &&
                amountBIP == that.amountBIP && pubKey == that.pubKey
    }

    override fun hashCode(): Int {
        return Objects.hash(coin, amount, amountBIP, pubKey)
    }

    override val imageUrl: String
        get() = MinterProfileApi.getCoinAvatarUrl(coin!!)

    override fun compareTo(other: DelegatedStake): Int {
        return other.amountBIP.compareTo(amountBIP)
    }
}