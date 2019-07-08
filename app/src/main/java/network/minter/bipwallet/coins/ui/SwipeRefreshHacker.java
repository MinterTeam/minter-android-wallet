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

package network.minter.bipwallet.coins.ui;

import android.view.animation.Animation;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import androidx.swiperefreshlayout.widget.CircularProgressDrawable;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import timber.log.Timber;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
public final class SwipeRefreshHacker {

    /*
    DEFAULT behavior

    this.mRefreshListener = new AnimationListener() {
            public void onAnimationStart(Animation animation) {

            // BUT we need to add listener here
            }

            public void onAnimationRepeat(Animation animation) {
            }

            public void onAnimationEnd(Animation animation) {
            // not only here
                if (SwipeRefreshLayout.this.mRefreshing) {
                    SwipeRefreshLayout.this.mProgress.setAlpha(255);
                    SwipeRefreshLayout.this.mProgress.start();
                    if (SwipeRefreshLayout.this.mNotify && SwipeRefreshLayout.this.mListener != null) {
                        SwipeRefreshLayout.this.mListener.onRefresh();
                    }

                    SwipeRefreshLayout.this.mCurrentTargetOffsetTop = SwipeRefreshLayout.this.mCircleView.getTop();
                } else {
                    SwipeRefreshLayout.this.reset();
                }

            }
        };
     */

    private OnRefreshStartListener mOnRefreshStartListener;

    public void setOnRefreshStartListener(OnRefreshStartListener listener) {
        mOnRefreshStartListener = listener;
    }

    public void hack(SwipeRefreshLayout layout) {
        try {
            Field refreshListener = layout.getClass().getDeclaredField("mRefreshListener");
            refreshListener.setAccessible(true);

            Field refreshing = layout.getClass().getDeclaredField("mRefreshing");
            refreshing.setAccessible(true);

            Field progress = layout.getClass().getDeclaredField("mProgress");
            progress.setAccessible(true);
            CircularProgressDrawable circularProgressDrawable = ((CircularProgressDrawable) progress.get(layout));

            Field notify = layout.getClass().getDeclaredField("mNotify");
            notify.setAccessible(true);

            Field listenerF = layout.getClass().getDeclaredField("mListener");
            listenerF.setAccessible(true);
            SwipeRefreshLayout.OnRefreshListener listener = ((SwipeRefreshLayout.OnRefreshListener) listenerF.get(layout));

            Field currentTargetOffsetTop = layout.getClass().getDeclaredField("mCurrentTargetOffsetTop");
            currentTargetOffsetTop.setAccessible(true);

            Field circleViewF = layout.getClass().getDeclaredField("mCircleView");
            circleViewF.setAccessible(true);

            Method circleViewGetTop = circleViewF.get(layout).getClass().getMethod("getTop");
            circleViewGetTop.setAccessible(true);

            Method reset = layout.getClass().getDeclaredMethod("reset");
            reset.setAccessible(true);

            refreshListener.set(layout, new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                    try {
                        if (refreshing.getBoolean(layout)) {
                            Timber.d("Call animation start listener");
                            if (mOnRefreshStartListener != null) {
                                mOnRefreshStartListener.onStartRefresh();
                            }
                        }
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    try {
                        if (refreshing.getBoolean(layout)) {
                            Timber.d("Call animation end listener");
                            circularProgressDrawable.setAlpha(255);
                            circularProgressDrawable.start();

                            try {
                                if (notify.getBoolean(layout) && listener != null) {
                                    listener.onRefresh();
                                }
                            } catch (IllegalAccessException e) {
                                Timber.e(e);
                            }

                            Object top = circleViewGetTop.invoke(circleViewF.get(layout));
                            Timber.d("Top: %s", top);
                            currentTargetOffsetTop.set(layout, top);
                        } else {
                            try {
                                reset.invoke(layout);
                            } catch (IllegalAccessException | InvocationTargetException e) {
                                Timber.e(e);
                            }
                        }
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        Timber.e(e);
                    }
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
        } catch (NoSuchFieldException | NoSuchMethodException | IllegalAccessException e) {
            Timber.w(e);
        }
    }


    public interface OnRefreshStartListener {
        void onStartRefresh();
    }
}
