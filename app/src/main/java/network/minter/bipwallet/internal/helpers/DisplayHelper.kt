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

import android.content.Context
import android.util.DisplayMetrics
import android.util.TypedValue
import androidx.annotation.DimenRes
import androidx.annotation.FloatRange
import androidx.annotation.Px
import network.minter.bipwallet.internal.common.annotations.Dp
import network.minter.bipwallet.internal.common.annotations.Sp
import network.minter.bipwallet.internal.helpers.data.Vec2

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
class DisplayHelper(private val context: Context) {
    fun getColumns(@Dp columnWidthDp: Float): Int {
        val displayMetrics = context.resources.displayMetrics
        val dpWidth = displayMetrics.widthPixels / displayMetrics.density
        return (dpWidth / columnWidthDp).toInt()
    }

    @get:Px val width: Int
        get() {
            val displayMetrics = context.resources.displayMetrics
            return displayMetrics.widthPixels
        }
    @get:Px val height: Int
        get() {
            val displayMetrics = context.resources.displayMetrics
            return displayMetrics.heightPixels
        }

    @Px
    fun dpToPx(@Dp integer: Int): Int {
        val pixels = metrics.density * integer
        return (pixels + 0.5f).toInt()
    }

    //@TODO return mContext.getResources().getBoolean(R.bool.isTablet);
    val isTablet: Boolean
        get() = false //@TODO return mContext.getResources().getBoolean(R.bool.isTablet);

    /**
     * @param percent From 0.0 to infinite, where 100.0f is 100% of screen width
     * @return
     */
    @Px
    fun widthPercent(@FloatRange(from = 0.0) percent: Float): Int {
        return (width / 100 * percent).toInt()
    }

    fun getWidthAndHeightWithRatio(@DimenRes fromWidthResId: Int, @DimenRes ratioWidthId: Int, @DimenRes ratioHeightId: Int): Vec2 {
        val width = px(fromWidthResId)
        val height = (width /
                (getDimen(ratioWidthId) / getDimen(ratioHeightId))).toInt()
        return Vec2(width, height)
    }

    /**
     * Как работает:
     * К примеру у нас есть ширина: 100% экрана, к примеру 1000px
     * нам нужна пропорциональная высота относительно известного кол-ва пикселей,
     * Требуемый формат 1000x500
     * Далее все просто.
     * Берем 100% ширины делим на пропорцию формата (ширина/высота) и получаем нужную высоту=500px
     * 1000 / (1000/500=2.0) = 500
     *
     *
     * если экран в ширину 800 пикселей, то получим следующую формулу:
     * 800 / (1000/500=2.0) = 400
     * @param widthPercent Ширина от которой отталкиваемся
     * @param ratioWidthId id ширины
     * @param ratioHeightId id высоты
     * @return
     * @see .getWidthAndHeightWithRatio
     */
    fun getWidthAndHeightWithRatio(
            @FloatRange(from = 0.0) widthPercent: Float,
            @DimenRes ratioWidthId: Int,
            @DimenRes ratioHeightId: Int): Vec2 {
        val width = widthPercent(widthPercent)
        val height = (width /
                (getDimen(ratioWidthId) / getDimen(ratioHeightId))).toInt()
        return Vec2(width, height)
    }

    fun getWidthAndHeightWithRatio(
            @FloatRange(from = 0.0) widthPercent: Float,
            ratioWidth: Float,
            ratioHeight: Float): Vec2 {
        val width = widthPercent(widthPercent)
        val height = (width /
                (ratioWidth / ratioHeight)).toInt()
        return Vec2(width, height)
    }

    @Px
    fun heightPercent(@FloatRange(from = 0.0) percent: Float): Int {
        return (height / 100 * percent).toInt()
    }

    @Px
    fun dpToPx(@Dp dps: Float): Int {
        val pixels = metrics.density * dps
        return (pixels + 0.5f).toInt()
    }

    @Dp
    fun pxToDp(@Px px: Int): Float {
        return px.toFloat() / metrics.density
    }

    @Px
    fun px(@DimenRes res: Int): Int {
        return dpToPx(context.resources.getDimension(res))
    }

    val metrics: DisplayMetrics
        get() = context.resources.displayMetrics

    @Px
    fun spToPx(@Sp sp: Int): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp.toFloat(), metrics).toInt()
    }

    val statusBarHeight: Int
        get() {
            var result = 0
            val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
            if (resourceId > 0) {
                result = context.resources.getDimensionPixelSize(resourceId)
            }
            return result
        }

    fun getDimen(@DimenRes resId: Int): Float {
        return context.resources.getDimension(resId)
    }
}