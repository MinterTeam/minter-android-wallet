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

package network.minter.bipwallet

import network.minter.bipwallet.internal.helpers.MathHelper.addPercent
import network.minter.bipwallet.internal.helpers.MathHelper.humanize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
class BigDecimalHelpersTest {

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

    @Test
    fun testAddPercentToInt() {
        val num = BigDecimal("100")

        val expected1 = BigDecimal("102.1000")
        val value1 = num.addPercent(BigDecimal("2.1"))

        val expected2 = BigDecimal("101.4500")
        val value2 = num.addPercent(BigDecimal("1.45"))

        assertTrue(expected1.compareTo(value1) == 0)
        assertTrue(expected2.compareTo(value2) == 0)
    }

    @Test
    fun testAddPercentTo18Precision() {
        val num = BigDecimal("100.002100000000000000")

        val expected1 = BigDecimal("102.1021441")
        val value1 = num.addPercent(BigDecimal("2.1"))

        val expected2 = BigDecimal("101.45213045")
        val value2 = num.addPercent(BigDecimal("1.45"))

        assertTrue(expected1.compareTo(value1) == 0)
        assertTrue(expected2.compareTo(value2) == 0)
    }

    @Test
    fun testCalcApy() {
        val liquidityBip = BigDecimal("107265103.833506531356317230")
        val volumeBip1d = BigDecimal("21363219.404883212813512660")
        val fee = BigDecimal("0.002")
        val expected = BigDecimal("15.64") // percents


        val year = BigDecimal("365.000").setScale(18)
        val one = BigDecimal.ONE.setScale(18)
        val hund = BigDecimal("100").setScale(18)

        val tradeFee = (volumeBip1d * fee).setScale(18, RoundingMode.HALF_UP)
        val apr = if(liquidityBip > BigDecimal.ZERO) {
            tradeFee.divide(liquidityBip, RoundingMode.HALF_UP).multiply(year)
        } else {
            BigDecimal.ZERO
        }.setScale(18, RoundingMode.HALF_UP)

        val res: BigDecimal
        if(apr.compareTo(BigDecimal.ZERO) == 0) {
            res = BigDecimal.ZERO
        } else {
            res = ((one + apr.divide(year, RoundingMode.HALF_UP)).pow(365) - one) * hund
        }

        assertTrue("expected=${expected}; res=${res}",
                expected.subtract(res).abs() < BigDecimal(1e-2)
        )


    }
}