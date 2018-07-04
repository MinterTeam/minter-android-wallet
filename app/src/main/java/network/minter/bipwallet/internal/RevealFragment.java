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

package network.minter.bipwallet.internal;

import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.view.ViewGroup;

import network.minter.bipwallet.internal.views.anim.CircularReveal;
import timber.log.Timber;

//@formatter:off

/**
 * RevealAnimation. 2017
 * <p>
 * <p>
 * Usage:
 * 1. Inheritance:
 * public static IntentBuilder<MyRevealFragment> newBuilder() {
 * return newBuilder(MyRevealFragment.class);
 * }
 * <p>
 * 2. Instancing:
 * button.setOnClickListener(v->{
 * MyRevealFragment fragment = MyRevealFragment.newInstance().fromAnchor(v).build();
 * fragment.show(getFragmentManager(), R.id.fragment_container);
 * // or with custom fragment tag
 * fragment.show(getFragmentManager(), R.id.fragment_container, MyRevealFragment.TAG);
 * });
 * <p>
 * 3. Dismissing:
 * 3.1 activity:
 * void onBackPressed() {
 * if(myRevealFragment.isShowing()) {
 * myRevealFragment.dismiss();
 * return;
 * }
 * super.onBackPressed();
 * }
 * <p>
 * 3.2 click listener:
 * closeFragButton.setOnClickListener(v->{
 * myRevealFragment.dismiss();
 * });
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
//@formatter:on
public class RevealFragment extends BaseInjectFragment {
    public final static String TAG = RevealFragment.class.getSimpleName();
    private boolean mIsShowing = false;
    private CircularReveal mReveal;
    private int mStartX, mStartY, mEndX, mEndY;
    private VisibilityListener mVisibilityListener;

    protected static <T extends RevealFragment> Builder<T> newBuilder(Class<T> clazz) {
        return new Builder<>(clazz);
    }

    public void setVisibilityListener(VisibilityListener listener) {
        mVisibilityListener = listener;
    }

    /**
     * @return True whether if fragment not removed from fragment manager, otherwise false
     */
    public boolean isShowing() {
        return mIsShowing;
    }

    /**
     * Reveal animation helper
     *
     * @return null on pre-lollipop android
     */
    public CircularReveal getReveal() {
        return mReveal;
    }

    @CallSuper
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        assert getArguments() != null;

        mStartX = getArguments().getInt("startX");
        mStartY = getArguments().getInt("startY");
        mEndX = getArguments().getInt("endX");
        mEndY = getArguments().getInt("endY");

        if (isRevealSupported()) {
            mReveal = new CircularReveal(((View) view.getParent())).setRevealDuration(500).setRevealColor(0xFF0000FF);

            onConfigureReveal(mReveal);
            mReveal.startReveal(getStartX(), getStartY(), new CircularReveal.RevealListener() {
                @Override
                public void onStart() {
                    onRevealStart();
                }

                @Override
                public void onEnd() {
                    mIsShowing = true;
                    onRevealEnd();
                }
            });
        }
        mIsShowing = true;
    }

    /**
     * Hide and delete fragment with loss state
     */
    public void dismiss() {
        onDismiss();
        if (mVisibilityListener != null) mVisibilityListener.onDismiss();

        if (isDetached()) {
            mIsShowing = false;
            if (isRevealSupported()) {
                onUnRevealEnd();
            }

            dismissInternal();
            return;
        }

        if (isRevealSupported()) {
            mReveal.startUnReveal(getEndX(), getEndY(), new CircularReveal.UnRevealListener() {
                @Override
                public void onStart() {
                    onUnRevealStart();
                }

                @Override
                public void onEnd() {
                    mIsShowing = false;
                    onUnRevealEnd();
                    dismissInternal();
                }
            });
            return;
        }
        dismissInternal();
    }

    /**
     * Show fragment with reveal animation
     *
     * @param fragmentManager FragmentManager
     * @param container       id of fragment container view
     * @param tag             fragment tag
     */
    public void show(final FragmentManager fragmentManager, int container, String tag) {
        if (fragmentManager.findFragmentByTag(tag) == null) {
            fragmentManager.beginTransaction().add(container, this, tag).commitAllowingStateLoss();
            fragmentManager.executePendingTransactions();
        } else {
            fragmentManager.beginTransaction().show(this).commitAllowingStateLoss();
            Timber.w("Fragment already exists");
        }

        if (mVisibilityListener != null) mVisibilityListener.onShow();
    }

    /**
     * Show fragment with reveal animation (default tag used)
     *
     * @param fragmentManager FragmentManager
     * @param container       id of fragment container view
     */
    public void show(final FragmentManager fragmentManager, int container) {
        show(fragmentManager, container, TAG);
    }

    protected boolean isRevealSupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    /**
     * @return Reveal start X position
     */
    protected int getStartX() {
        return mStartX;
    }

    /**
     * @return Reveal start Y position
     */
    protected int getStartY() {
        return mStartY;
    }

    /**
     * @return UnReveal start X position
     */
    protected int getEndX() {
        return mEndX;
    }

    /**
     * @return UnReveal start Y position
     */
    protected int getEndY() {
        return mEndY;
    }

    /**
     * Can configure reveal object (like set interpolator, durations, et cetera)
     *
     * @param reveal Reveal helper object
     * @see CircularReveal
     */
    protected void onConfigureReveal(CircularReveal reveal) {
    }

    /**
     * Called on
     *
     * @see #dismiss()
     */
    protected void onDismiss() {

    }

    /**
     * Called on reveal animation start
     */
    protected void onRevealStart() {
    }

    /**
     * Called on reveal animation end
     */
    protected void onRevealEnd() {
    }

    /**
     * Called on un-reveal animation start
     */
    protected void onUnRevealStart() {
    }

    /**
     * Called on un-reveal animation end
     */
    protected void onUnRevealEnd() {
    }

    private void dismissInternal() {
        if (getFragmentManager() == null) {
            Timber.w("Fragment manager is null!");
            return;
        }

        getFragmentManager().beginTransaction().remove(this).commitAllowingStateLoss();
        getFragmentManager().executePendingTransactions();
    }

    public interface VisibilityListener {
        void onShow();
        void onDismiss();
    }

    /**
     * @param <T> RevealFragment type
     */
    public final static class Builder<T extends RevealFragment> {
        private int mStartX = 0;
        private int mStartY = 0;
        private int mEndX = 0;
        private int mEndY = 0;
        private Class<T> mFragmentClass;
        private Bundle mCustomArgs;

        Builder(Class<T> fragmentClass) {
            mFragmentClass = fragmentClass;
        }

        /**
         * Add custom arguments to fragment
         *
         * @param args Bundle params
         * @return IntentBuilder
         */
        public Builder<T> setArguments(Bundle args) {
            mCustomArgs = args;
            return this;
        }

        /**
         * @param cx reveal and un-reveal animation center X
         * @param cy reveal and un-reveal animation center Y
         * @return IntentBuilder
         */
        public Builder<T> setCenter(int cx, int cy) {
            setCenterStart(cx, cy);
            setCenterEnd(cx, cy);
            return this;
        }

        /**
         * @param cx Start reveal animation center X
         * @param cy Start reveal animation center Y
         * @return IntentBuilder
         */
        public Builder<T> setCenterStart(int cx, int cy) {
            mStartX = cx;
            mStartY = cy;
            return this;
        }

        /**
         * @param cx un-reveal animation target center X
         * @param cy un-reveal animation target center X
         * @return IntentBuilder
         */
        public Builder<T> setCenterEnd(int cx, int cy) {
            mEndX = cx;
            mEndY = cy;
            return this;
        }

        public Builder<T> fromAnchor(final View anchorView) {
            Rect in = new Rect();
            ViewGroup.MarginLayoutParams lp = ((ViewGroup.MarginLayoutParams) anchorView.getLayoutParams());
            in.set((int) anchorView.getX() + lp.leftMargin, (int) anchorView.getY() + lp.topMargin, (int) anchorView.getX() + anchorView.getWidth() - lp.rightMargin,
                   (int) anchorView.getY() + anchorView.getHeight() - lp.bottomMargin);

            return setCenter(in.centerX(), in.centerY());
        }

        @NonNull
        public T build() {
            try {
                final T in = mFragmentClass.newInstance();
                Bundle bundle = new Bundle();
                bundle.putInt("startX", mStartX);
                bundle.putInt("startY", mStartY);
                bundle.putInt("endX", mEndX);
                bundle.putInt("endY", mEndY);

                if (mCustomArgs != null) {
                    bundle.putAll(mCustomArgs);
                }
                in.setArguments(bundle);
                return in;
            } catch (InstantiationException e) {
                Timber.e(e);
            } catch (IllegalAccessException e) {
                Timber.e(e);
            } catch (java.lang.InstantiationException e) {
                Timber.e(e);
            }

            // impossible
            return null;
        }
    }
}
