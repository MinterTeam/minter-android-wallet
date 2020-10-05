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

package network.minter.bipwallet

import network.minter.bipwallet.internal.helpers.MathHelper.humanize
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
class BigDecimalFormatTest {

    @Test
    fun testFormatMoreThanOne() {
        val num = BigDecimal("1.1001001001")
        val res = num.humanize()

        assertEquals("1.1001", res)
    }

    @Test
    fun testFormatMoreThanOne2() {
        val num = BigDecimal("1356.4648461")
        val res = num.humanize()

        assertEquals("1 356.4648", res)
    }

    @Test
    fun testFormatBigNumber() {
        val num = BigDecimal("10987654.316698491818644684")
        val res = num.humanize()

        assertEquals("10 987 654.3166", res)
    }

    @Test
    fun testFormatLessThanOneBeforeRounding() {
        val num = BigDecimal("0.9999999999999")
        val res = num.humanize()

        assertEquals("1.0000", res)
    }

    @Test
    fun testFormatLessThanOne() {
        val num = BigDecimal("0.1111666666666")
        val res = num.humanize()

        assertEquals("0.11116667", res)
    }

    @Test
    fun testFormatLessThanOneRound() {
        val num = BigDecimal("0.111199999999")
        val res = num.humanize()

        assertEquals("0.1112", res)
    }

    @Test
    fun testFormatLessThanOneMediumPrecision() {
        val num = BigDecimal("0.111166")
        val res = num.humanize()

        assertEquals("0.111166", res)
    }
}