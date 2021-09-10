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
import network.minter.explorer.models.CoinItem
import network.minter.explorer.models.CoinItemBase
import java.math.BigInteger

object CoinItemChainikParceler : Parceler<CoinItem?> {
    override fun create(parcel: android.os.Parcel): CoinItem? {
        val out = CoinItem()
        val isPresent = parcel.readByte() == 1.toByte()
        if(isPresent) {
            out.id = parcel.readValue(BigInteger::class.java.classLoader) as BigInteger
            out.name = parcel.readString()
            out.symbol = parcel.readString()
            out.type = CoinItemBase.CoinType.Token
            return out
        }

        return null
    }

    override fun CoinItem?.write(parcel: android.os.Parcel, flags: Int) {
        if(this == null) {
            parcel.writeByte(0)
            return
        }
        parcel.writeByte(1)
        parcel.writeValue(id)
        parcel.writeString(name)
        parcel.writeString(symbol)
    }

}
