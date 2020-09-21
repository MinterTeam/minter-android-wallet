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
import android.graphics.*
import android.net.Uri
import android.widget.ImageView
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.annotation.Px
import coil.Coil
import coil.ImageLoader
import coil.load

import coil.size.Scale
import coil.util.CoilUtils
import kotlinx.coroutines.Dispatchers
import network.minter.bipwallet.internal.common.annotations.Dp
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * minter-android-wallet. 2020
 *
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
class ImageHelper(mContext: Context, private val mDisplay: DisplayHelper) {
    private val imageLoader: ImageLoader

    init {
        val httpClient = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .cache(CoilUtils.createDefaultCache(mContext))
                .build()

        imageLoader = ImageLoader.Builder(mContext)
                .crossfade(160)
                .okHttpClient { httpClient }
                .dispatcher(Dispatchers.IO)
                .build()

//        CoilLogger.setEnabled(BuildConfig.DEBUG)
        Coil.setImageLoader(imageLoader)
//        Coil.setDefaultImageLoader(imageLoader)
    }

    fun loadResize(iv: ImageView, imageUrl: Uri?, @Dp resizeDp: Float) {
        val widthHeight = mDisplay.getWidthAndHeightWithRatio(100f, resizeDp, resizeDp)
        iv.load(imageUrl) {
            size(widthHeight.getWidth(), widthHeight.getHeight())
        }
    }

    fun loadResize(iv: ImageView, @DrawableRes imageId: Int, @Dp resizeDp: Float) {
        val widthHeight = mDisplay.getWidthAndHeightWithRatio(100f, resizeDp, resizeDp)
        iv.load(imageId) {
            size(widthHeight.getWidth(), widthHeight.getHeight())
            scale(Scale.FILL)
        }
    }

    fun loadResize(iv: ImageView, @DrawableRes imageId: Int, @Px resizePx: Int) {
        iv.load(imageId) {
            size(resizePx, resizePx)
        }
    }

    fun loadResizeRes(iv: ImageView, imageUrl: Uri?, @DimenRes resId: Int) {
        val pxs = mDisplay.getDimen(resId)
        val widthHeight = mDisplay.getWidthAndHeightWithRatio(100f, pxs, pxs)
        iv.load(imageUrl) {
            size(widthHeight.getWidth(), widthHeight.getHeight())
        }
    }

    fun loadResize(iv: ImageView, imageUrl: Uri?, @Px resizePx: Int) {
        iv.load(imageUrl) {
            size(resizePx, resizePx)
        }
    }

    fun loadResize(iv: ImageView, imageUrl: Uri?, @Dp widthDp: Float, @Dp heightDp: Float) {
        val widthHeight = mDisplay.getWidthAndHeightWithRatio(100f, widthDp, heightDp)
        iv.load(imageUrl) {
            size(widthHeight.getWidth(), widthHeight.getHeight())
        }
    }

    fun loadResize(iv: ImageView, @DrawableRes imageId: Int, @Dp widthDp: Float, @Dp heightDp: Float) {
        val widthHeight = mDisplay.getWidthAndHeightWithRatio(100f, widthDp, heightDp)
        iv.load(imageId) {
            size(widthHeight.getWidth(), widthHeight.getHeight())
        }
    }

    fun loadResize(iv: ImageView, imageUrl: Uri?, @Px widthPx: Int, @Px heightPx: Int) {
        iv.load(imageUrl) {
            size(widthPx, heightPx)
        }
    }

    fun loadResize(iv: ImageView, @DrawableRes imageId: Int, @Px widthPx: Int, @Px heightPx: Int) {
        iv.load(imageId) {
            size(widthPx, heightPx)
        }
    }

    companion object {
        @JvmStatic
        fun makeBitmapCircle(bitmap: Bitmap): Bitmap {
            val output = Bitmap.createBitmap(
                    bitmap.width,
                    bitmap.height,
                    Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(output)
            val color = Color.RED
            val paint = Paint()
            val rect = Rect(0, 0, bitmap.width, bitmap.height)
            val rectF = RectF(rect)
            paint.isAntiAlias = true
            canvas.drawARGB(0, 0, 0, 0)
            paint.color = color
            canvas.drawOval(rectF, paint)
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(bitmap, rect, rect, paint)
            bitmap.recycle()
            return output
        }
    }
}