package network.minter.bipwallet.internal.views.anim;

import android.animation.Animator;
import android.animation.TimeInterpolator;
import android.annotation.TargetApi;
import android.os.Build;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.animation.DecelerateInterpolator;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Collections;

import timber.log.Timber;


public class CircularReveal {
    private TimeInterpolator mRevealInterpolator = new DecelerateInterpolator(2f);
    private TimeInterpolator mUnRevealInterpolator = new DecelerateInterpolator(2f);
    private Animator mRevealAnimator;
    private Animator mUnRevealAnimator;
    private int mRevealDuration = 200;
    private int mUnRevealDuration = 200;
    private int mRevealColor = 0xFFFFFFFF;
    private boolean mIsRevealing, mIsUnRevealing;
    private WeakReference<View> mRootView;
    private RevealListener mOnListener;
    /**
     * Reveal Animator Listener
     */
    private final Animator.AnimatorListener mRevealAnimListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animation) {
            mOnListener.onStart();
            mIsRevealing = true;
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            mOnListener.onEnd();
            mIsRevealing = false;
        }

        @Override
        public void onAnimationCancel(Animator animation) {
            mIsRevealing = false;
        }

        @Override
        public void onAnimationRepeat(Animator animation) {
        }
    };
    private int mRootBackgroundColor = -1;
    private UnRevealListener mOffListener;
    /**
     * UnReveal Animator Listener
     */
    private final Animator.AnimatorListener mUnRevealAnimListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animation) {
            mOffListener.onStart();
            mIsUnRevealing = true;
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            mOffListener.onEnd();
            mIsUnRevealing = false;

            // restoring old bg color
            if (mRootView.get() != null) {
                mRootView.get().setBackgroundColor(mRootBackgroundColor);
            }

        }

        @Override
        public void onAnimationCancel(Animator animation) {
            mIsUnRevealing = false;
        }

        @Override
        public void onAnimationRepeat(Animator animation) {
        }
    };


    public CircularReveal(final View rootView) {
        mRootView = new WeakReference<>(rootView);
    }

    /**
     * Start Circular unReveal animation
     *
     * @param centerX  Animation Center X
     * @param centerY  Animation Center Y
     * @param listener Unreveal listener
     */
    public void startReveal(int centerX, int centerY, @NonNull RevealListener listener) {
        mOnListener = listener;
        startRevealAnimation(centerX, centerY);
    }

    /**
     * Cancel Reveal animator
     */
    public void cancelReveal() {
        mRevealAnimator.cancel();
    }

    public void canceUnReveal() {
        mUnRevealAnimator.cancel();
    }

    /**
     * Current animation state
     */
    public boolean isRevealing() {
        return mIsRevealing;
    }

    public boolean isUnrevealing() {
        return mIsUnRevealing;
    }

    /**
     * Start Circular unReveal animation
     *
     * @param centerX  Animation Center X
     * @param centerY  Animation Center Y
     * @param listener Unreveal listener
     */
    public void startUnReveal(int centerX, int centerY, UnRevealListener listener) {
        mOffListener = listener;
        try {
            mUnRevealAnimator = prepareUnRevealAnimator(centerX, centerY);
            mUnRevealAnimator.start();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public CircularReveal setRevealDuration(int duration) {
        mUnRevealDuration = mRevealDuration = duration;
        return this;
    }

    public CircularReveal setUnRevealDuration(int duration) {
        mUnRevealDuration = duration;
        return this;
    }

    public CircularReveal setRevealColor(int color) {
        mRevealColor = color;
        return this;
    }

    public CircularReveal setRevealInterpolator(TimeInterpolator interpolator) {
        mRevealInterpolator = interpolator;
        return this;
    }

    public CircularReveal setUnrevealInterpolator(TimeInterpolator interpolator) {
        mUnRevealInterpolator = interpolator;
        return this;
    }

    /**
     * Configures and start Reveal animation
     */
    private void startRevealAnimation(final int cx, final int cy) {
        // saving old bg color
//        if (mRootView.get().getBackground() instanceof ColorDrawable) {
//            mRootBackgroundColor = ((ColorDrawable) mRootView.get().getBackground()).getColor();
//        }
        // define BG color
//        mRootView.get().setBackgroundColor(mRevealColor);

        if (Build.VERSION.SDK_INT >= 21) {
            // To run the animation as soon as the view is layout in the view hierarchy we add this
            // listener and delete it
            // as soon as it runs to prevent multiple animations if the view changes bounds
            mRootView.get().addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                           int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    int radius = (int) Math.hypot(right, bottom);

                    mRevealAnimator = ViewAnimationUtils.createCircularReveal(v, cx, cy, 0, radius);
                    mRevealAnimator.setInterpolator(mRevealInterpolator);
                    mRevealAnimator.setDuration(mRevealDuration);
                    mRevealAnimator.addListener(mRevealAnimListener);
                    mRevealAnimator.start();
                    mRootView.get().removeOnLayoutChangeListener(this);
                }
            });
        } else {
            //@TODO где-то была порт-либа для старых сдк
            Timber.w("Cannot run CircularReveal on this SDK version[" + Build.VERSION.SDK_INT + "]. " +
                    "CircularReveal run on version 21 and greater.");
        }
    }

    /**
     * Prepare UnReveal animation
     */
    private Animator prepareUnRevealAnimator(float cx, float cy) throws IllegalAccessException {
        if (Build.VERSION.SDK_INT >= 21) {
            int radius = getEnclosingCircleRadius(mRootView.get(), (int) cx, (int) cy);
            Animator animator = ViewAnimationUtils.createCircularReveal(
                    mRootView.get(), (int) cx, (int) cy, radius, 0
            );
            animator.setInterpolator(mUnRevealInterpolator);
            animator.setDuration(mUnRevealDuration);
            animator.addListener(mUnRevealAnimListener);
            return animator;
        } else
            throw new IllegalAccessException(
                    "Cannot prepare UnReveal with version[" + Build.VERSION.SDK_INT + "]");
    }

    private int getEnclosingCircleRadius(View v, int cx, int cy) {
        int realCenterX = cx + v.getLeft();
        int realCenterY = cy + v.getTop();
        int distTopLeft = (int) Math.hypot(realCenterX - v.getLeft(), realCenterY - v.getTop());
        int distTopRight = (int) Math.hypot(v.getRight() - realCenterX, realCenterY - v.getTop());
        int distBottomLeft = (int) Math.hypot(realCenterX - v.getLeft(),
                v.getBottom() - realCenterY);
        int distBottomRight = (int) Math.hypot(v.getRight() - v.getLeft(),
                v.getBottom() - realCenterY);

        Integer[] distances = new Integer[]{
                distTopLeft,
                distTopRight,
                distBottomLeft,
                distBottomRight
        };
        return Collections.max(Arrays.asList(distances));
    }


    public interface RevealListener {
        void onStart();
        void onEnd();
    }

    public interface UnRevealListener {
        void onStart();
        void onEnd();
    }

}
