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

import network.minter.bipwallet.internal.common.Preconditions
import network.minter.bipwallet.internal.views.widgets.RemoteImageContainer
import network.minter.core.crypto.MinterPublicKey
import network.minter.explorer.models.CoinDelegation
import network.minter.explorer.models.ValidatorMeta
import java.math.BigDecimal
import java.util.*

/**
 * Created by Alexander Kolpakov (jquickapp@gmail.com) on 06-Jun-19
 */
class DelegatedValidator : DelegatedItem, RemoteImageContainer, Comparable<DelegatedValidator> {
    @JvmField var pubKey: MinterPublicKey?
    @JvmField var name: String?
    override var imageUrl: String?
    @JvmField var description: String?
    var delegatedBips: BigDecimal = BigDecimal.ZERO

    constructor(info: CoinDelegation) {
        pubKey = info.publicKey
        name = info.meta!!.name
        imageUrl = info.meta!!.iconUrl
        description = info.meta!!.description
    }

    constructor(publicKey: MinterPublicKey?, meta: ValidatorMeta) {
        pubKey = publicKey
        name = meta.name
        imageUrl = meta.iconUrl
        description = meta.description
    }

    override fun toString(): String {
        return String.format("DelegatedValidator{name=%s}", Preconditions.firstNonNull(name, pubKey!!.toShortString()))
    }

    override fun isSameOf(item: DelegatedItem?): Boolean {
        return viewType == item!!.viewType && pubKey == (item as DelegatedValidator?)!!.pubKey
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DelegatedValidator) return false
        val that = other
        return pubKey == that.pubKey &&
                name == that.name &&
                imageUrl == that.imageUrl &&
                description == that.description
    }

    override val viewType: Int
        get() = DelegatedItem.ITEM_VALIDATOR

    override fun hashCode(): Int {
        return Objects.hash(pubKey, name, imageUrl, description)
    }

    override fun compareTo(other: DelegatedValidator): Int {
        return other.delegatedBips.compareTo(delegatedBips)
    }
}