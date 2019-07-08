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

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;

import androidx.annotation.DimenRes;
import androidx.annotation.DrawableRes;
import de.hdodenhof.circleimageview.CircleImageView;
import network.minter.bipwallet.internal.common.annotations.Dp;


/**
 * minter-android-wallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class BipCircleImageView extends CircleImageView implements RemoteImageView {
    private final RemoteImageViewDelegate mRemoteDelegate;

    public BipCircleImageView(Context context) {
        super(context);
        mRemoteDelegate = new RemoteImageViewDelegate(this);
    }

    public BipCircleImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mRemoteDelegate = new RemoteImageViewDelegate(this);
    }

    public BipCircleImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mRemoteDelegate = new RemoteImageViewDelegate(this);
    }

    @Override
    public void setImageUrl(String url, @Dp float size) {
        mRemoteDelegate.setImageUrl(url, size);
    }

    @Override
    public void setImageUrl(Uri uri, @DimenRes int resizeResId) {
        mRemoteDelegate.setImageUrl(uri, resizeResId);
    }

    @Override
    public void setImageUrl(String url, @DimenRes int resizeResId) {
        mRemoteDelegate.setImageUrl(url, resizeResId);
    }

    @Override
    public void setImageUrl(Uri uri, float size) {
        mRemoteDelegate.setImageUrl(uri, size);
    }

    @Override
    public void setImageUrl(Uri uri) {
        mRemoteDelegate.setImageUrl(uri);
    }

    @Override
    public void setImageUrl(String url) {
        mRemoteDelegate.setImageUrl(url);
    }

    @Override
    public void setImageUrlFallback(String url, @DrawableRes int fallbackResId) {
        mRemoteDelegate.setImageUrlFallback(url, fallbackResId);
    }

    @Override
    public void setImageUrlFallback(String url, String fallbackUrl) {
        mRemoteDelegate.setImageUrlFallback(url, fallbackUrl);
    }

    @Override
    public void setImageUrl(RemoteImageContainer imageUrlContainer) {
        mRemoteDelegate.setImageUrl(imageUrlContainer);
    }

    @Override
    public void setImageUrl(RemoteImageContainer imageUrlContainer, float size) {
        mRemoteDelegate.setImageUrl(imageUrlContainer, size);
    }

    @Override
    public void setImageUrl(RemoteImageContainer imageUrlContainer, int resId) {
        mRemoteDelegate.setImageUrl(imageUrlContainer, resId);
    }
}
