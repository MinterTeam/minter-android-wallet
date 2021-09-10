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

package network.minter.bipwallet.pools.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.WriteWith
import network.minter.bipwallet.apis.parcel.PoolParceler
import network.minter.bipwallet.apis.parcel.PoolProviderParceler
import network.minter.bipwallet.apis.reactive.calculateAPY
import network.minter.bipwallet.internal.Wallet
import network.minter.bipwallet.internal.helpers.MathHelper.asCurrency
import network.minter.bipwallet.internal.helpers.MathHelper.humanize
import network.minter.explorer.models.Pool
import network.minter.explorer.models.PoolProvider
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * minter-android-wallet. 2021
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
@Parcelize
data class PoolCombined(
        var pool: @WriteWith<PoolParceler> Pool,
        val farm: FarmingItem? = null,
        val stake: @WriteWith<PoolProviderParceler> PoolProvider? = null,
        val filter: PoolsFilter = PoolsFilter.None
) : Parcelable {
    val volume1dUsd: CharSequence
        get() {
            if(pool.volumeBip1d == null) {
                return "$0.00"
            }
            return "$" + (Wallet.app().bipUsdRateCachedRepo().data * pool.volumeBip1d).asCurrency()
        }

    fun getApy(): CharSequence {
        return pool.calculateAPY().humanize() + "%"
    }

    fun getFarmingPercent(): CharSequence? {
        if(farm == null) {
            return null
        }
        return BigDecimal(farm.percent).setScale(2, RoundingMode.HALF_UP).toPlainString() + "%"
    }
}
