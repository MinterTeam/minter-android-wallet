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

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Rect
import android.net.Uri
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import com.edwardstock.inputfield.InputField
import network.minter.bipwallet.BuildConfig
import network.minter.bipwallet.R
import network.minter.bipwallet.databinding.IncludeTestnetWarningViewBinding

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */

class ResTextFormat(
        @StringRes val resId: Int,
        vararg format: Any?
) {
    @Suppress("UNCHECKED_CAST")
    val data: Array<Any?> = format as Array<Any?>
}

object ViewExtensions {

    var View.visible: Boolean
        get() = visibility == View.VISIBLE
        set(v) {
            this.visibility = if (v) View.VISIBLE else View.GONE
        }

    var View.nvisible: Boolean
        get() = visibility == View.VISIBLE
        set(v) {
            this.visibility = if (v) View.VISIBLE else View.INVISIBLE
        }

    fun <T : View?> T.postApply(cb: (T) -> Unit) {
        val _this = this
        this?.post {
            cb(_this)
        }
    }

    fun <T : TextView> T.setTextFormat(resId: Int, vararg format: Any?) {
        this.text = context.getString(resId, *format)
    }

    fun <T : TextView> T.setTextFormat(format: ResTextFormat) {
        setTextFormat(format.resId, *format.data)
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

    fun IncludeTestnetWarningViewBinding.visibleForTestnet() {
        root.visibleForTestnet()
    }

    fun View?.visibleForTestnet() {
        if (this == null) return

        if (BuildConfig.FLAVOR == "netTest") {
            visible = true
            setOnClickListener {
                try {
                    val goToMarket = Intent(Intent.ACTION_VIEW).setData(Uri.parse("market://details?id=network.minter.bipwallet.mainnet"))
                    it.context.startActivity(goToMarket)
                } catch (e: ActivityNotFoundException) {
                    val goToMarket = Intent(Intent.ACTION_VIEW).setData(Uri.parse("https://play.google.com/store/apps/details?id=network.minter.bipwallet.mainnet"))
                    it.context.startActivity(goToMarket)
                }
            }
        } else {
            visible = false
        }
    }

    fun listItemBackgroundRippleRounded(view: View, position: Int, size: Int) {
        val isFirst = position == 0
        val isLast = position == size - 1
        val isMiddle = !isFirst && !isLast

        if (isFirst && isLast) {
            view.setBackgroundResource(R.drawable.bg_ripple_white_rounded)
        } else if (isFirst && !isLast) {
            view.setBackgroundResource(R.drawable.bg_ripple_white_top_rounded)
        } else if (isMiddle) {
            view.setBackgroundResource(R.drawable.bg_ripple_white)
        } else if (!isFirst && isLast) {
            view.setBackgroundResource(R.drawable.bg_ripple_white_bot_rounded)
        }
    }

    /**
     * Scroll down the minimum needed amount to show [descendant] in full. More
     * precisely, reveal its bottom.
     */
    fun ViewGroup.scrollDownTo(descendant: View) {
        // Could use smoothScrollBy, but it sometimes over-scrolled a lot
        howFarDownIs(descendant)?.let {
            if (it == 0) return@let
            when (this) {
                is ScrollView -> {
                    this.smoothScrollBy(0, it)
                }
                is NestedScrollView -> {
                    this.smoothScrollBy(0, it)
                }
                else -> {
                    scrollBy(0, it)
                }
            }
        }
    }

    fun InputField.inputNoSuggestions() {
        this.input.inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
    }

    /**
     * Calculate how many pixels below the visible portion of this [ViewGroup] is the
     * bottom of [descendant].
     *
     * In other words, how much you need to scroll down, to make [descendant]'s bottom
     * visible.
     */
    fun ViewGroup.howFarDownIs(descendant: View): Int? {
        val bottom = Rect().also {
            // See https://stackoverflow.com/a/36740277/1916449
            descendant.getDrawingRect(it)
            offsetDescendantRectToMyCoords(descendant, it)
        }.bottom
        return (bottom - height - scrollY).takeIf { it > 0 }
    }

}