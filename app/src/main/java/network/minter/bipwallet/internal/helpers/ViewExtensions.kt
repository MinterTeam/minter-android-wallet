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

import android.content.res.ColorStateList
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import network.minter.bipwallet.R

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
object ViewExtensions {

    var View.visible: Boolean
        get() = visibility == View.VISIBLE
        set(v) {
            this.visibility = if (v) View.VISIBLE else View.GONE
        }

    fun <T : View> T.postApply(cb: (T) -> Unit) {
        val _this = this
        this.post {
            cb(_this)
        }
    }

    fun <T : TextView> T.copyOnClick(toCopy: String? = null) {
        if (text == null && toCopy == null) {
            return
        }

        val stateList = ColorStateList(arrayOf(
                intArrayOf(android.R.attr.state_pressed),
                intArrayOf()
        ), intArrayOf(
                ContextCompat.getColor(context, R.color.colorPrimaryLighter),
                textColors.defaultColor
        ))

        setTextColor(stateList)

        setOnClickListener { v ->
            ContextHelper.copyToClipboard(v.context, toCopy ?: text.toString())
        }
    }

}