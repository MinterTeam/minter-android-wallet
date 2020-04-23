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
package network.minter.bipwallet.internal.views.widgets

import android.content.Context
import android.net.Uri
import android.widget.ImageView
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import coil.api.load
import coil.size.Scale
import network.minter.bipwallet.internal.Wallet
import network.minter.bipwallet.internal.common.annotations.Dp
import timber.log.Timber
import java.lang.ref.WeakReference


/**
 * minter-android-wallet. 2018
 *
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
internal class RemoteImageViewDelegate(imageView: ImageView) : RemoteImageView {
    private val mImage: WeakReference<ImageView> = WeakReference(imageView)
    private val mIsLowRamDevice: Boolean = false

    init {
//        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
//        mIsLowRamDevice = am.isLowRamDevice
    }

    override fun setImageUrl(url: String?, @Dp size: Float) {
        setImageUrl(Uri.parse(url), size)
    }

    override fun setImageUrl(uri: Uri?, @DimenRes resId: Int) {
        setImageUrl(uri, context.resources.getDimension(resId))
    }

    override fun setImageUrl(url: String?, @DimenRes resId: Int) {
        setImageUrl(Uri.parse(url), resId)
    }

    override fun setImageUrl(uri: Uri?, @Dp size: Float) {
        val sizeLocal = if (mIsLowRamDevice) size / 2 else size
        Wallet.app().image()
                .loadResize(mImage.get()!!, uri, sizeLocal)
    }

    override fun setImageUrl(uri: Uri?) {
        mImage.get()?.load(uri) {
            scale(Scale.FIT)
        }
    }

    override fun setImageUrl(url: String?) {
        if (url == null) {
            Timber.w("Image url is null")
            return
        }
        mImage.get()?.load(url) {
            scale(Scale.FIT)
        }
    }

    override fun setImageUrlFallback(url: String?, fallbackResId: Int) {
        mImage.get()!!.load(url) {
            scale(Scale.FIT)
            fallback(fallbackResId)
            error(fallbackResId)
        }
    }

    override fun setImageUrlFallback(url: String?, fallbackUrl: String?) {
        if (url == null || url.isEmpty()) {
            setImageUrl(fallbackUrl)
            return
        }
        setImageUrl(url)
    }

    override fun setImageUrl(imageUrlContainer: RemoteImageContainer?) {
        setImageUrl(imageUrlContainer?.imageUrl)
    }

    override fun setImageUrl(imageUrlContainer: RemoteImageContainer?, size: Float) {
        setImageUrl(imageUrlContainer?.imageUrl, size)
    }

    override fun setImageUrl(imageUrlContainer: RemoteImageContainer?, resId: Int) {
        setImageUrl(imageUrlContainer?.imageUrl, resId)
    }

    override fun setImageResource(@DrawableRes resId: Int) {
        mImage.get()!!.setImageResource(resId)
    }

    private val context: Context
        get() = mImage.get()!!.context

}