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

import network.minter.bipwallet.R
import network.minter.bipwallet.internal.Wallet

data class RegexReplaceData(
        val pattern: Regex,
        val groups: List<Int>,
        val replacer: (Int, String) -> String
)

object StringsHelper {
    fun isNullOrEmpty(s: String?): Boolean {
        return s == null || s.isEmpty()
    }

    fun boolToString(b: Boolean?): String {
        return if (b == null || !b) {
            Wallet.app().res().getString(R.string.no)
        } else Wallet.app().res().getString(R.string.yes)
    }

    fun glue(input: List<String?>, glue: String?): String {
        val sb = StringBuilder()
        for (i in input.indices) {
            sb.append(input[i])
            if (i + 1 < input.size) {
                sb.append(glue)
            }
        }
        return sb.toString()
    }

    fun repeat(c: Char, n: Int): String {
        if (n <= 0) {
            return ""
        }
        val out = StringBuilder()
        for (i in 0 until n) {
            out.append(c)
        }
        return out.toString()
    }

    fun replaceGroups(text: String?, data: RegexReplaceData): String? {
        if (text == null) return null

        return replaceGroups(text, data.pattern, data.groups, data.replacer)
    }

    fun replaceGroups(text: String, pattern: Regex, groups: List<Int>, replacer: (Int, String) -> String): String? {
        val res = pattern.matchEntire(text)

        if (res != null) {
            val sb = StringBuilder()
            for ((i, gval) in res.groupValues.withIndex()) {
                if (i == 0) continue

                if (groups.contains(i)) {
                    sb.append(replacer(i, gval))
                } else {
                    sb.append(gval)
                }
            }
            return sb.toString()
        }
        return null
    }
}