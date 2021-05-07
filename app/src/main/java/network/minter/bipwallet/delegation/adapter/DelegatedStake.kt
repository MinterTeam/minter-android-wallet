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
package network.minter.bipwallet.delegation.adapter

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.WriteWith
import network.minter.bipwallet.apis.parcel.CoinItemBaseParceler
import network.minter.bipwallet.apis.parcel.ValidatorMetaParceler
import network.minter.bipwallet.apis.reactive.avatar
import network.minter.bipwallet.internal.views.widgets.RemoteImageContainer
import network.minter.core.crypto.MinterPublicKey
import network.minter.explorer.models.CoinDelegation
import network.minter.explorer.models.CoinItemBase
import network.minter.explorer.models.ValidatorMeta
import java.math.BigDecimal

@Parcelize
class DelegatedStake(
        var coin: @WriteWith<CoinItemBaseParceler>() CoinItemBase,
        var amount: BigDecimal = BigDecimal.ZERO,
        var amountBIP: BigDecimal = BigDecimal.ZERO,
        var publicKey: MinterPublicKey? = null,
        var validatorMeta: @WriteWith<ValidatorMetaParceler>() ValidatorMeta? = null,
        var isKicked: Boolean = false
) : DelegatedItem, RemoteImageContainer, Comparable<DelegatedStake>, Parcelable {

    constructor(info: CoinDelegation) : this(
            info.coin!!,
            info.amount,
            info.bipValue,
            info.publicKey!!,
            info.meta!!,
            info.isInWaitlist
    )

    override fun isSameOf(item: DelegatedItem): Boolean {
        return viewType == item.viewType && coin == (item as DelegatedStake).coin && publicKey == item.publicKey
    }

    override val viewType: Int
        get() = DelegatedItem.ITEM_STAKE

    @Deprecated("Use new variable binding", ReplaceWith("coin.avatar"))
    override val imageUrl: String
        get() = coin.avatar

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
