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

package network.minter.bipwallet.apis.parcel

import kotlinx.parcelize.Parceler
import network.minter.bipwallet.apis.parcel.CoinItemBaseParceler.write
import network.minter.explorer.models.Pool
import java.math.BigDecimal

object PoolParceler : Parceler<Pool> {
    override fun create(parcel: android.os.Parcel): Pool {
        val pool = Pool()
        pool.coin0 = CoinItemBaseParceler.create(parcel)
        pool.coin1 = CoinItemBaseParceler.create(parcel)
        pool.token = CoinItemBaseParceler.create(parcel)

        pool.amount0 = parcel.readValue(BigDecimal::class.java.classLoader) as BigDecimal
        pool.amount1 = parcel.readValue(BigDecimal::class.java.classLoader) as BigDecimal
        pool.liquidity = parcel.readValue(BigDecimal::class.java.classLoader) as BigDecimal
        pool.liquidityBip = parcel.readValue(BigDecimal::class.java.classLoader) as BigDecimal
        pool.volumeBip1d = parcel.readValue(BigDecimal::class.java.classLoader) as BigDecimal?
        pool.volumeBip30d = parcel.readValue(BigDecimal::class.java.classLoader) as BigDecimal?

        return pool
    }

    override fun Pool.write(parcel: android.os.Parcel, flags: Int) {
        coin0.write(parcel, flags)
        coin1.write(parcel, flags)
        token.write(parcel, flags)
        parcel.writeValue(amount0)
        parcel.writeValue(amount1)
        parcel.writeValue(liquidity)
        parcel.writeValue(liquidityBip)
        parcel.writeValue(volumeBip1d)
        parcel.writeValue(volumeBip30d)
    }

}
