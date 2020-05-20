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

package network.minter.bipwallet.wallets.utils

import android.widget.TextView
import network.minter.bipwallet.R
import network.minter.bipwallet.internal.Wallet
import network.minter.bipwallet.internal.helpers.HtmlCompat
import network.minter.bipwallet.internal.helpers.Plurals
import org.joda.time.DateTime
import org.joda.time.Seconds

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
object LastBlockHandler {

    enum class ViewType {
        Main,
        Others
    }

    fun handle(tv: TextView, timestamp: DateTime? = null, type: ViewType = ViewType.Others) {
        val ctx = tv.context

        if (timestamp == null) {
            val emptyText = if (type == ViewType.Main) {
                ctx.getString(R.string.balance_last_updated_sync_main)
            } else {
                HtmlCompat.fromHtml(ctx.getString(R.string.balance_last_updated_sync))
            }

            tv.text = emptyText
            return
        }


        val diff = Seconds.secondsBetween(timestamp, DateTime().plusSeconds(Wallet.timeOffset()))
        val res = 0.coerceAtLeast(diff.seconds - 5)



        if (type == ViewType.Main) {
            if (res == 0) {
                tv.setText(R.string.balance_last_updated_now_main)
            } else {
                tv.text = HtmlCompat.fromHtml(ctx.getString(
                        R.string.balance_last_updated_main,
                        Plurals.timeValue(res.toLong()),
                        Plurals.timeUnitShort(res.toLong())
                ))
            }
        } else {
            if (res == 0) {
                tv.text = HtmlCompat.fromHtml(ctx.getString(R.string.balance_last_updated_now_others))
            } else {
                tv.text = HtmlCompat.fromHtml(ctx.getString(
                        R.string.balance_last_updated_others,
                        Plurals.timeValue(res.toLong()),
                        Plurals.timeUnit(res.toLong())
                ))
            }
        }
    }

}