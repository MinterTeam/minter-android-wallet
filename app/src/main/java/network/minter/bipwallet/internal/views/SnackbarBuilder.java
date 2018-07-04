package network.minter.bipwallet.internal.views;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.ColorRes;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import network.minter.bipwallet.R;

import static network.minter.bipwallet.internal.common.Preconditions.checkNotNull;

/**
 * Stars. 2017
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */

public class SnackbarBuilder {
    private Context mContext;
    private View mContainerView;
    private CharSequence mMessage;
    private int mDuration = 3 * 1000;
    private int mActionColor = -1;
    //так как при получения цвета через getResource().getColor() мы получаем маску, надо дополнительно опеределять пользовательский цвет
    private boolean mActionColorSet = false;
    private int mBackgroundColor = -1;
    private boolean mBackgroundColorSet = false;
    private View.OnClickListener mOnClickListener;
    private CharSequence mActionName;
    private Snackbar mSnackbar;
    private int mMessageColor = -1;
    private boolean mMessageColorSet = false;
    private TextView mTextView;
    private float mMessageTextSizeRes = -1.f;
    private Button mAction;
    private boolean mEnableAction = true;

    public SnackbarBuilder(Activity activity, ViewGroup containerView) {
        mContext = activity;
        mContainerView = containerView;
    }

    public SnackbarBuilder(Activity activity) {
        this(activity, android.R.id.content);
    }

    public SnackbarBuilder(Activity activity, @IdRes int containerId) {
        mContext = activity;
        mContainerView = activity.findViewById(containerId);
    }

    public SnackbarBuilder(android.support.v4.app.Fragment fragment) {
        mContext = fragment.getActivity();
        mContainerView = fragment.getView();
    }

    public SnackbarBuilder(Fragment fragment) {
        mContext = fragment.getActivity();
        mContainerView = fragment.getView();
    }

    public boolean isShown() {
        return mSnackbar != null && mSnackbar.isShown();
    }

    /**
     * Sets or update existing message
     *
     * @param message
     * @return
     */
    public SnackbarBuilder setMessage(@NonNull CharSequence message) {
        if (mTextView != null) {
            mTextView.setText(message);
            return this;
        }

        mMessage = message;
        return this;
    }

    /**
     * Sets or update existing message
     *
     * @param resId
     * @return
     */
    public SnackbarBuilder setMessage(@StringRes int resId) {
        if (mTextView != null) {
            mTextView.setText(resId);
            return this;
        }
        mMessage = mContext.getResources().getString(resId);
        return this;
    }

    public SnackbarBuilder setDurationShort() {
        return setDuration(Snackbar.LENGTH_SHORT);
    }

    public SnackbarBuilder setDuration(int duration) {
        mDuration = duration;
        return this;
    }

    public SnackbarBuilder setDurationLong() {
        return setDuration(Snackbar.LENGTH_LONG);
    }

    public SnackbarBuilder setDurationIndefinite() {
        return setDuration(Snackbar.LENGTH_INDEFINITE);
    }

    public SnackbarBuilder setAction(@StringRes int actionName,
                                     View.OnClickListener onClickListener) {
        mActionName = mContext.getResources().getString(actionName);
        mOnClickListener = onClickListener;
        return this;
    }

    public SnackbarBuilder setActionTextColorRes(@ColorRes int resId) {
        mActionColor = mContext.getResources().getColor(resId);
        mActionColorSet = true;
        return this;
    }

    public SnackbarBuilder setActionTextColor(int color) {
        mActionColor = color;
        mActionColorSet = true;
        return this;
    }

    public SnackbarBuilder setAction(CharSequence actionName, View.OnClickListener onClickListener) {
        mActionName = actionName;
        mOnClickListener = onClickListener;
        return this;
    }

    public SnackbarBuilder setMessageTextColorRes(@ColorRes int colorRes) {
        mMessageColor = mContext.getResources().getColor(colorRes);
        mMessageColorSet = true;
        return this;
    }

    public SnackbarBuilder setBackgroundColorRes(@ColorRes int backgroundColorRes) {
        mBackgroundColor = mContext.getResources().getColor(backgroundColorRes);
        mBackgroundColorSet = true;
        return this;
    }

    public SnackbarBuilder setBackgroundColor(int color) {
        mBackgroundColor = color;
        mBackgroundColorSet = true;
        return this;
    }

    public SnackbarBuilder setMessageTextColor(int color) {
        mMessageColor = color;
        mMessageColorSet = true;
        return this;
    }

    public SnackbarBuilder show() {
        checkNotNull(mMessage, "Message is required");
        mSnackbar = Snackbar.make(mContainerView, mMessage, mDuration);
        if (mOnClickListener != null) {
            mSnackbar.setAction(mActionName, v -> {
                mSnackbar = null;
                mOnClickListener.onClick(v);
            });
            if (!mActionColorSet) {
                mSnackbar.setActionTextColor(mContext.getResources().getColor(R.color.errorColor));
            } else {
                mSnackbar.setActionTextColor(mActionColor);
            }

            if (mBackgroundColorSet) {
                mSnackbar.getView().setBackground(new ColorDrawable(mBackgroundColor));
            }
        }

        mTextView = mSnackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
        mAction = mSnackbar.getView().findViewById(android.support.design.R.id.snackbar_action);
        mAction.setEnabled(mEnableAction);
        mAction.setAlpha(mEnableAction ? 1f : 0.3f);

        mTextView.setMaxLines(10);

        if (mMessageColorSet) {
            mTextView.setTextColor(mMessageColor);
        } else {
            mTextView.setTextColor(mContext.getResources().getColor(android.R.color.white));
        }

        if (mMessageTextSizeRes > 0) {
            mTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mMessageTextSizeRes);
        }

        mSnackbar.show();

        return this;
    }

    public void dismiss() {
        if (mSnackbar == null) return;
        mSnackbar.dismiss();
        mTextView = null;
    }

    public SnackbarBuilder setMessageTextSizeRes(int messageTextSizeRes) {
        mMessageTextSizeRes =
                mContext.getResources().getDimension(messageTextSizeRes);
        return this;
    }

    public SnackbarBuilder setActionEnable(boolean enable) {
        if (mAction != null) {
            mAction.setEnabled(enable);
            mAction.setAlpha(enable ? 1f : 0.3f);
        } else {
            mEnableAction = enable;
        }

        return this;
    }
}
