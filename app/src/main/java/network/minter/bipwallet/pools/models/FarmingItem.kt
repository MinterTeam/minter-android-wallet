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
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.WriteWith
import network.minter.bipwallet.apis.parcel.CoinItemChainikParceler
import network.minter.core.crypto.MinterAddress
import network.minter.explorer.models.CoinItem
import network.minter.explorer.models.CoinItemBase
import network.minter.explorer.models.Pool
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*

/**
 * minter-android-wallet. 2021
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */

@Parcelize
data class FarmingItem(
        val id: Long,
        val address: MinterAddress,
        @SerializedName("owner_address")
        val ownerAddress: MinterAddress?,
        val pair: String,
        @SerializedName("pool_id")
        val poolId: Long,
        val percent: Double,
        val rewardCoin: @WriteWith<CoinItemChainikParceler> CoinItem?,
        val coin0: @WriteWith<CoinItemChainikParceler> CoinItem,
        val coin1: @WriteWith<CoinItemChainikParceler> CoinItem,
        val period: Int,
        @SerializedName("start_at")
        val startAt: Date?,
        @SerializedName("finish_at")
        val finishAt: Date?
) : Parcelable {
    fun toPool(): Pool {
        val pool = Pool()
        pool.coin0 = coin0
        pool.coin1 = coin1
        pool.token = CoinItemBase()
        pool.token.id = BigInteger.ZERO
        pool.token.symbol = "LP-${poolId}"
        pool.amount0 = BigDecimal.ZERO
        pool.amount1 = BigDecimal.ZERO
        pool.liquidity = BigDecimal.ZERO
        pool.liquidityBip = BigDecimal.ZERO

        return pool
    }
}
