/*
 * Copyright (C) by MinterTeam. 2018
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

package network.minter.bipwallet.internal.views.widgets;

import android.app.ActivityManager;
import android.content.Context;
import android.net.Uri;
import android.widget.ImageView;

import com.squareup.picasso.Callback;

import java.lang.ref.WeakReference;

import androidx.annotation.DimenRes;
import network.minter.bipwallet.internal.common.annotations.Dp;
import timber.log.Timber;

import static android.content.Context.ACTIVITY_SERVICE;
import static network.minter.bipwallet.internal.Wallet.app;

/**
 * minter-android-wallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
final class RemoteImageViewDelegate implements RemoteImageView {
    private final WeakReference<ImageView> mImage;
    private boolean mIsLowRamDevice;

    RemoteImageViewDelegate(ImageView imageView) {
        mImage = new WeakReference<>(imageView);
        final ActivityManager am = ((ActivityManager) getContext().getSystemService(ACTIVITY_SERVICE));
        mIsLowRamDevice = am.isLowRamDevice();
    }

    @Override
    public void setImageUrl(String url, @Dp float size) {
        setImageUrl(Uri.parse(url), size);
    }

    public void setImageUrl(Uri uri, @DimenRes int resId) {
        setImageUrl(uri, getContext().getResources().getDimension(resId));
    }

    public void setImageUrl(String url, @DimenRes int resizeResId) {
        setImageUrl(Uri.parse(url), resizeResId);
    }

    public void setImageUrl(Uri uri, @Dp float size) {
        float sizeLocal = mIsLowRamDevice ? size / 2 : size;
        app().image()
                .loadResize(uri, sizeLocal)
                .into(mImage.get());
    }

    @Override
    public void setImageUrl(Uri uri) {
        app().image()
                .loadFit(uri)
                .into(mImage.get());
    }

    @Override
    public void setImageUrl(String url) {
        app().image()
                .loadFit(url)
                .into(mImage.get(), new Callback() {
                    @Override
                    public void onSuccess() {

                    }

                    @Override
                    public void onError(Exception e) {
                        Timber.w(e, "Unable to load image %s", url);
                    }
                });
    }

    @Override
    public void setImageUrlFallback(String url, int fallbackResId) {
        if (url == null || url.isEmpty()) {
            mImage.get().setImageResource(fallbackResId);
            return;
        }

        setImageUrl(url);
    }

    @Override
    public void setImageUrlFallback(String url, String fallbackUrl) {
        if (url == null || url.isEmpty()) {
            setImageUrl(fallbackUrl);
            return;
        }

        setImageUrl(url);
    }

    @Override
    public void setImageUrl(RemoteImageContainer imageUrlContainer) {
        setImageUrl(imageUrlContainer.getImageUrl());
    }

    @Override
    public void setImageUrl(RemoteImageContainer imageUrlContainer, float size) {
        setImageUrl(imageUrlContainer.getImageUrl(), size);
    }

    @Override
    public void setImageUrl(RemoteImageContainer imageUrlContainer, int resId) {
        setImageUrl(imageUrlContainer.getImageUrl(), resId);
    }

    private Context getContext() {
        return mImage.get().getContext();
    }

}
