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
package network.minter.bipwallet.internal.helpers

import android.graphics.Color
import network.minter.bipwallet.internal.common.Preconditions
import network.minter.blockchain.models.operational.Transaction
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
object MathHelper {
    @JvmStatic
    fun clamp(input: Float, min: Float, max: Float): Float {
        if (input < min) {
            return min
        } else if (input > max) {
            return max
        }
        return input
    }

    @JvmStatic
    fun clamp(input: Int, min: Int): Int {
        return if (input < min) {
            min
        } else input
    }

    @JvmStatic
    fun clamp(input: Long, min: Long): Long {
        return if (input < min) {
            min
        } else input
    }

    @JvmStatic
    fun clamp(input: Int, min: Int, max: Int): Int {
        if (input < min) {
            return min
        } else if (input > max) {
            return max
        }
        return input
    }

    @JvmStatic
    fun clamp(input: Long, min: Long, max: Long): Long {
        if (input < min) {
            return min
        } else if (input > max) {
            return max
        }
        return input
    }

    // BigDecimal

    @JvmStatic
    fun bdGT(from: BigDecimal?, to: Double): Boolean {
        return bdGT(from, BigDecimal(to))
    }


    @JvmStatic
    fun bdGT(from: BigDecimal?, to: BigDecimal?): Boolean {
        return if (from == null) {
            false
        } else from > to
    }

    @JvmStatic
    fun bdGTE(from: BigDecimal?, to: Double): Boolean {
        return bdGTE(from, BigDecimal(to))
    }

    @JvmStatic
    fun bdGTE(from: BigDecimal?, to: BigDecimal?): Boolean {
        return if (from == null) {
            false
        } else from >= to
    }

    @JvmStatic
    fun bdLT(from: BigDecimal?, to: Double): Boolean {
        return bdLT(from, BigDecimal(to))
    }

    @JvmStatic
    fun bdLT(from: BigDecimal?, to: BigDecimal?): Boolean {
        return if (from == null) {
            false
        } else from < to
    }

    @JvmStatic
    fun bdLTE(from: BigDecimal?, to: Double): Boolean {
        return bdLTE(from, BigDecimal(to))
    }

    @JvmStatic
    fun bdLTE(from: BigDecimal?, to: BigDecimal?): Boolean {
        return if (from == null) {
            false
        } else from <= to
    }

    @JvmStatic
    fun bdIntHuman(source: String?): String {
        return bdIntHuman(BigDecimal(source))
    }

    @JvmStatic
    fun bdIntHuman(source: BigDecimal): String {
        return formatDecimalCurrency(source.setScale(0, BigDecimal.ROUND_UNNECESSARY), 0, true)
    }

    @JvmStatic
    fun bdHuman(source: Double): String {
        return bdHuman(BigDecimal(source))
    }

    @JvmStatic
    fun bdHuman(source: BigDecimal): String {
        val num = Preconditions.firstNonNull(source, BigDecimal(0))
        // if 0 or null = 4 digits
        if (bdNull(num)) {
            return formatDecimalCurrency(num, 4, true)
        }
        // if less than 1 = min 4 digits, max 8
        if (bdLT(num, BigDecimal(1))) {
            // if precision < 4, show 4 digits
            return if (num.stripTrailingZeros().scale() <= 4) {
                formatDecimalCurrency(num.setScale(4, BigDecimal.ROUND_DOWN), 4, true)
            } else {
                // use 8 digits precision
                val v = formatDecimalCurrency(num.setScale(8, BigDecimal.ROUND_UP), 8, false)
                val test = BigDecimal(v)
                // if after rounding, number is still less than 1, return 8 digits
                if (bdLT(test, BigDecimal.ONE)) {
                    v
                } else {
                    // otherwise recurse and format number as > 1
                    formatDecimalCurrency(test.setScale(4, BigDecimal.ROUND_DOWN), 4, true)
                }
            }
        }
        // if number is >= 1 use 4 digits
        val out = num.setScale(4, RoundingMode.DOWN)
        return formatDecimalCurrency(out, 4, true)
    }

    fun BigDecimal.humanize(): String {
        return bdHuman(this)
    }

    fun BigDecimal.toPlain(): String {
        return stripTrailingZeros().toPlainString()
    }

    @JvmStatic
    fun bdEQ(a: Double, b: BigDecimal?): Boolean {
        return bdEQ(BigDecimal(a), b)
    }

    @JvmStatic
    fun bdEQ(a: Double, b: Double): Boolean {
        return bdEQ(BigDecimal(a), BigDecimal(b))
    }

    @JvmStatic
    fun bdEQ(a: BigDecimal?, b: Double): Boolean {
        return bdEQ(a, BigDecimal(b))
    }

    @JvmStatic
    fun bdEQ(a: BigDecimal?, b: BigDecimal?): Boolean {
        return if (a == null) false else a.compareTo(b) == 0
    }

    @JvmStatic
    fun bdNull(source: BigDecimal): Boolean {
        val test: BigDecimal
        test = if (source.scale() > 18) {
            source.setScale(18, BigDecimal.ROUND_UP)
        } else {
            source
        }
        return test.setScale(18) == BigDecimal("0e-18")
    }

    // BigInteger
    @JvmStatic
    fun biGT(from: BigInteger?, to: Long): Boolean {
        return if (from == null) {
            false
        } else from.compareTo(BigInteger(to.toString())) > 0
    }

    @JvmStatic
    fun biGTE(from: BigInteger?, to: Long): Boolean {
        return if (from == null) {
            false
        } else from.compareTo(BigInteger(to.toString())) >= 0
    }

    @JvmStatic
    fun biLT(from: BigInteger?, to: Long): Boolean {
        return if (from == null) {
            false
        } else from.compareTo(BigInteger(to.toString())) < 0
    }

    @JvmStatic
    fun biLTE(from: BigInteger?, to: Long): Boolean {
        return if (from == null) {
            false
        } else from.compareTo(BigInteger(to.toString())) <= 0
    }

    @JvmStatic
    fun startsFromNumber(value: CharSequence?): Boolean {
        if (value == null || value.length == 0) {
            return false
        }
        val `val` = value.toString()
        return `val`.matches("^[0-9\\.]+.*".toRegex())
    }

    @JvmStatic
    fun bigDecimalFromString(text: CharSequence?): BigDecimal {
        if (text == null) {
            return BigDecimal.ZERO
        } else if (text.toString() == "0.") {
            return BigDecimal.ZERO
        }
        var amountText = text
                .toString()
                .replace("\\s+".toRegex(), "")
                .replace("[,]+".toRegex(), "")
                .replace(",", ".")
        if (amountText.isEmpty()) {
            amountText = "0"
        }
        if (amountText == ".") {
            amountText = "0"
        } else if (amountText.substring(0, 1) == ".") {
            amountText = "0$amountText"
        }
        if (amountText.substring(amountText.length - 1) == ".") {
            amountText = amountText + "0"
        }
        val out: BigDecimal
        out = try {
            BigDecimal(amountText)
        } catch (e: NumberFormatException) {
            BigDecimal.ZERO
        }
        return out
    }

    fun CharSequence?.parseBigDecimal(): BigDecimal {
        if (this == null) return BigDecimal.ZERO

        return bigDecimalFromString(toString())
    }

    fun String?.parseBigDecimal(): BigDecimal {
        if (isNullOrEmpty()) return BigDecimal.ZERO

        return bigDecimalFromString(this)
    }

    fun BigDecimal?.isNotZero(): Boolean {
        return this != null && this > "0"
    }

    operator fun BigDecimal.compareTo(value: Int): Int {
        return this.compareTo(BigDecimal(value.toString()))
    }

    operator fun BigDecimal.compareTo(value: Float): Int {
        return this.compareTo(BigDecimal(value.toString()))
    }

    operator fun BigDecimal.compareTo(value: Double): Int {
        return this.compareTo(BigDecimal(value.toString()))
    }

    operator fun BigDecimal.compareTo(value: String): Int {
        return this.compareTo(BigDecimal(value))
    }

    fun BigDecimal.normalize(): BigInteger {
        return (this * Transaction.VALUE_MUL_DEC).toBigInteger()
    }


    @JvmStatic
    fun formatDecimalCurrency(bd: BigDecimal, fractions: Int, exactFractions: Boolean): String {
        val fmt = NumberFormat.getInstance(Locale.US) as DecimalFormat
        val symbols = fmt.decimalFormatSymbols
        symbols.groupingSeparator = ' '
        if (exactFractions) {
            fmt.minimumFractionDigits = fractions
            fmt.maximumFractionDigits = fractions
        } else {
            fmt.maximumFractionDigits = fractions
            fmt.minimumFractionDigits = 0
        }
        fmt.decimalFormatSymbols = symbols
        return fmt.format(bd)
    }

    @JvmStatic
    fun blendColors(from: Int, to: Int, ratio: Float): Int {
        val inverseRatio = 1f - ratio
        val r = Color.red(to) * ratio + Color.red(from) * inverseRatio
        val g = Color.green(to) * ratio + Color.green(from) * inverseRatio
        val b = Color.blue(to) * ratio + Color.blue(from) * inverseRatio
        val a = Color.alpha(to) * ratio + Color.alpha(from) * inverseRatio
        return Color.argb(a.toInt(), r.toInt(), g.toInt(), b.toInt())
    }
}