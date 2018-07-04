/*
 * Copyright (C) 2018 by MinterTeam
 * @link https://github.com/MinterTeam
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

package network.minter.bipwallet.internal.helpers;

import android.view.View;

import com.squareup.picasso.Callback;

/**
 * Stars. 2017
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */

public class ShowHideViewRequestListener<R> implements Callback {
    private final View mView;
    private Callback mPicassoCallback;

    public ShowHideViewRequestListener(final View view) {
        mView = view;
        if (mView != null) {
            mView.setVisibility(View.VISIBLE);
        }
    }

    public ShowHideViewRequestListener(final View view, Callback callback) {
        this(view);
        mPicassoCallback = callback;
    }

    @Override
    public void onSuccess() {
        hideView();
        if (mPicassoCallback != null)
            mPicassoCallback.onSuccess();
    }

    @Override
    public void onError(Exception t) {
        hideView();
        if (mPicassoCallback != null) mPicassoCallback.onError(t);
    }

    private void hideView() {
        if (mView != null) mView.setVisibility(View.GONE);
    }
}
