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
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.StatFs
import android.widget.ImageView
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.annotation.Px
import coil.Coil
import coil.ImageLoader
import coil.load
import coil.size.Scale
import com.squareup.picasso.Callback
import com.squareup.picasso.OkHttp3Downloader
import com.squareup.picasso.Picasso
import com.squareup.picasso.RequestCreator
import kotlinx.coroutines.Dispatchers
import network.minter.bipwallet.internal.Wallet
import network.minter.bipwallet.internal.common.annotations.Dp
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import timber.log.Timber
import java.io.File
import java.math.BigInteger
import java.util.concurrent.TimeUnit

/**
 * minter-android-wallet. 2020
 *
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */

class ImageHelper(private val context: Context, private val mDisplay: DisplayHelper) {
    companion object {
        internal fun calcDiskCacheSize(cacheDirectory: File): Long {
            val cacheDir = StatFs(cacheDirectory.absolutePath)
            val size = 0.1 * cacheDir.blockCountLong * cacheDir.blockSizeLong
            return size.toLong().coerceIn(10L * 1024L * 1024L, 500L * 1024L * 1024L)
        }

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

        fun isLightImage(bitmap: Bitmap): Boolean {
            return try {
                val mask = BigInteger("FFFFFFFF", 16)
                val middle = BigInteger("FF7FFFFF", 16)

                val topLeft = BigInteger.valueOf(bitmap.getPixel(0, 0).toLong()).and(mask)
                val topRight = BigInteger.valueOf(bitmap.getPixel(bitmap.width - 1, 0).toLong()).and(mask)
                val botLeft = BigInteger.valueOf(bitmap.getPixel(0, bitmap.height - 1).toLong()).and(mask)
                val botRight = BigInteger.valueOf(bitmap.getPixel(bitmap.width - 1, bitmap.height - 1).toLong()).and(mask)

                val avg = (topLeft + topRight + botLeft + botRight) / BigInteger("4")
                //Timber.d("Average color: %s", avg.toString(16))
                avg.and(mask) > middle
            } catch (e: Throwable) {
                false
            }
        }

        fun isLightImage(iv: ImageView): Boolean {
            if (iv.drawable != null && iv.drawable is BitmapDrawable && (iv.drawable as BitmapDrawable).bitmap != null) {
                return isLightImage((iv.drawable as BitmapDrawable).bitmap)
            }
            return false
        }
    }

    private val imageLoader: ImageLoader
    val picasso: Picasso

    init {
        val cacheDir = File(context.cacheDir, "image_cache").apply { mkdirs() }
        val httpClient = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor {
                    val nreq = it.request().newBuilder()
                            .cacheControl(CacheControl.Builder()
                                    .maxAge(7, TimeUnit.DAYS)
                                    .maxStale(1, TimeUnit.HOURS)
                                    .build())
                            .build()
                    it.proceed(nreq)
                }
                .cache(Cache(cacheDir, calcDiskCacheSize(cacheDir)))
                .build()

        imageLoader = ImageLoader.Builder(context)
                .crossfade(160)
                .okHttpClient { httpClient }
                .dispatcher(Dispatchers.IO)
                .build()

        picasso = Picasso.Builder(context)
                .downloader(OkHttp3Downloader(httpClient))
                .listener { instance, uri, exception ->
                    Timber.w(exception, "Unable to load image %s", uri.toString())
                }
                .indicatorsEnabled(false)
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
}

fun <T : ImageView> T.loadPicasso(path: String?, onSuccess: (() -> Unit)? = null, onError: ((Exception) -> Unit)? = null, applier: (RequestCreator.() -> Unit)? = null) {
    return Wallet.app().image().picasso.load(path).apply(applier ?: {}).into(this, object : Callback {
        override fun onSuccess() {
            onSuccess?.invoke()
        }

        override fun onError(e: Exception) {
            onError?.invoke(e)
        }
    })
}

//fun <T: ImageView> T.loadPicasso(path: String?, applier: RequestCreator.() -> Unit) {
//    return Wallet.app().image().picasso.load(path).apply(applier).into(this)
//}

fun <T : ImageView> T.loadPicasso(uri: Uri?): RequestCreator {
    return Wallet.app().image().picasso.load(uri)
}

fun <T : ImageView> T.loadPicasso(uri: Uri?, applier: RequestCreator.() -> Unit): RequestCreator {
    return loadPicasso(uri).apply(applier)
}

fun <T : ImageView> T.loadPicasso(file: File): RequestCreator {
    return Wallet.app().image().picasso.load(file)
}

fun <T : ImageView> T.loadPicasso(file: File, applier: RequestCreator.() -> Unit): RequestCreator {
    return loadPicasso(file).apply(applier)
}

fun <T : ImageView> T.loadPicasso(resourceId: Int): RequestCreator {
    return Wallet.app().image().picasso.load(resourceId)
}

fun <T : ImageView> T.loadPicasso(resourceId: Int, applier: RequestCreator.() -> Unit): RequestCreator {
    return loadPicasso(resourceId).apply(applier)
}