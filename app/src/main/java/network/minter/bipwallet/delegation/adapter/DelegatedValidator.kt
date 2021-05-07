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

import network.minter.bipwallet.internal.views.widgets.RemoteImageContainer
import network.minter.core.crypto.MinterPublicKey
import network.minter.explorer.models.CoinDelegation
import network.minter.explorer.models.ValidatorMeta
import java.math.BigDecimal

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
@Parcelize
data class DelegatedValidator(
        val publicKey: MinterPublicKey,
        val name: String? = null,
        override var imageUrl: String? = null,
        val description: String? = null,
        val delegatedBips: BigDecimal = BigDecimal.ZERO,
        var commission: Int = 0,
        var minStake: BigDecimal = BigDecimal.ZERO
) : DelegatedItem, RemoteImageContainer, Comparable<DelegatedValidator>, Parcelable {

    constructor(info: CoinDelegation) : this(
            info.publicKey!!,
            info.meta!!.name,
            info.meta!!.iconUrl,
            info.meta!!.description
    )

    constructor(publicKey: MinterPublicKey, meta: ValidatorMeta) : this(
            publicKey,
            meta.name,
            meta.iconUrl,
            meta.description
    )

    override fun isSameOf(item: DelegatedItem): Boolean {
        return viewType == item.viewType && publicKey == (item as DelegatedValidator).publicKey
    }

    override val viewType: Int
        get() = DelegatedItem.ITEM_VALIDATOR

    override fun compareTo(other: DelegatedValidator): Int {
        return other.delegatedBips.compareTo(delegatedBips)
    }
}