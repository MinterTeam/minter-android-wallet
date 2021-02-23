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

package network.minter.bipwallet.coins

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import network.minter.core.crypto.MinterAddress
import network.minter.explorer.models.CoinItem
import network.minter.explorer.models.CoinItemBase
import org.parceler.Parcel
import java.math.BigDecimal
import java.math.BigInteger

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
@Parcel
@Entity(
        tableName = "minter_coins",
        indices = [
            Index(value = ["coinId", "symbol"], unique = true)
        ]
)
class DbCoin {
    @JvmField
    @PrimaryKey(autoGenerate = true)
    var id = 0

    @JvmField
    var coinId: BigInteger? = null

    @JvmField
    var symbol: String? = null

    @JvmField
    var crr: Int? = null

    @JvmField
    var reserveBalance: BigDecimal? = null

    @JvmField
    var maxSupply: BigDecimal? = null

    @JvmField
    var owner: MinterAddress? = null

    @JvmField
    var mintable: Boolean = false

    @JvmField
    var burnable: Boolean = false

    @JvmField
    var type: CoinItemBase.CoinType = CoinItemBase.CoinType.Coin

    constructor()

    constructor(coinItem: CoinItem) {
        coinId = coinItem.id
        symbol = coinItem.symbol
        crr = coinItem.crr
        reserveBalance = coinItem.reserveBalance
        maxSupply = coinItem.maxSupply
        owner = coinItem.owner
        mintable = coinItem.mintable
        burnable = coinItem.burnable
        type = coinItem.type
    }

    fun asCoin(): CoinItem {
        val item = CoinItem()
        item.id = coinId
        item.symbol = symbol
        item.crr = crr!!
        item.reserveBalance = reserveBalance
        item.maxSupply = maxSupply
        item.owner = owner
        item.mintable = mintable
        item.burnable = burnable
        item.type = type
        return item
    }

    class BigIntConverter {
        @TypeConverter
        fun stringToBigint(value: String?): BigInteger? {
            return value?.let { BigInteger(it, 10) }
        }

        @TypeConverter
        fun bigintToString(value: BigInteger?): String? {
            return value?.toString(10)
        }
    }

    class BigDecimalConverter {
        @TypeConverter
        fun stringToBigDecimal(value: String?): BigDecimal? {
            return value?.let { BigDecimal(it) }
        }

        @TypeConverter
        fun bigDecimalToString(value: BigDecimal?): String? {
            return value?.toPlainString()
        }
    }

    class CoinTypeConverter {
        @TypeConverter
        fun intToType(value: Int?): CoinItemBase.CoinType {
            if (value == null) {
                return CoinItemBase.CoinType.Coin
            }

            return CoinItemBase.CoinType.values()[value]
        }

        @TypeConverter
        fun typeToInt(value: CoinItemBase.CoinType?): Int {
            if (value == null) return 0
            return value.ordinal
        }
    }

    class MinterAddressConverter {
        @TypeConverter
        fun stringToAddress(value: String?): MinterAddress? {
            return value?.let { MinterAddress(it) }
        }

        @TypeConverter
        fun addressToString(value: MinterAddress?): String? {
            return value?.toString()
        }
    }


}
