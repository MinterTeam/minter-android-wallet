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

package network.minter.bipwallet.internal.views

import android.app.Activity
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import network.minter.bipwallet.R
import network.minter.bipwallet.internal.common.Preconditions

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */

fun Fragment.snackbar(): SnackbarBuilder = SnackbarBuilder(this)
fun Activity.snackbar(): SnackbarBuilder = SnackbarBuilder(this)

class SnackbarBuilder {
    private var mContext: Context?
    private var mContainerView: View?
    private var mMessage: CharSequence? = null
    private var mDuration = 3 * 1000
    private var mActionColor = -1

    //так как при получения цвета через getResource().getColor() мы получаем маску, надо дополнительно опеределять пользовательский цвет
    private var mActionColorSet = false
    private var mBackgroundColor = -1
    private var mBackgroundColorSet = false
    private var mOnClickListener: View.OnClickListener? = null
    private var mActionName: CharSequence? = null
    private var mSnackbar: Snackbar? = null
    private var mMessageColor = -1
    private var mMessageColorSet = false
    private var mTextView: TextView? = null
    private var mMessageTextSizeRes = -1f
    private var mAction: Button? = null
    private var mEnableAction = true

    constructor(activity: Activity?, containerView: ViewGroup?) {
        mContext = activity
        mContainerView = containerView
    }

    @JvmOverloads
    constructor(activity: Activity, @IdRes containerId: Int = android.R.id.content) {
        mContext = activity
        mContainerView = activity.findViewById(containerId)
    }

    constructor(fragment: Fragment) {
        mContext = fragment.activity
        mContainerView = fragment.view
    }

    val isShown: Boolean
        get() = mSnackbar != null && mSnackbar!!.isShown

    /**
     * Sets or update existing message
     *
     * @param message
     * @return
     */
    fun setMessage(message: CharSequence): SnackbarBuilder {
        if (mTextView != null) {
            mTextView!!.text = message
            return this
        }
        mMessage = message
        return this
    }

    /**
     * Sets or update existing message
     *
     * @param resId
     * @return
     */
    fun setMessage(@StringRes resId: Int): SnackbarBuilder {
        if (mTextView != null) {
            mTextView!!.setText(resId)
            return this
        }
        mMessage = mContext!!.resources.getString(resId)
        return this
    }

    fun setDurationShort(): SnackbarBuilder {
        return setDuration(Snackbar.LENGTH_SHORT)
    }

    fun setDuration(duration: Int): SnackbarBuilder {
        mDuration = duration
        return this
    }

    fun setDurationLong(): SnackbarBuilder {
        return setDuration(Snackbar.LENGTH_LONG)
    }

    fun setDurationInfinite(): SnackbarBuilder {
        return setDuration(Snackbar.LENGTH_INDEFINITE)
    }

    fun setAction(@StringRes actionName: Int,
                  onClickListener: View.OnClickListener?): SnackbarBuilder {
        mActionName = mContext!!.resources.getString(actionName)
        mOnClickListener = onClickListener
        return this
    }

    fun setActionTextColorRes(@ColorRes resId: Int): SnackbarBuilder {
        mActionColor = ContextCompat.getColor(mContext!!, resId)
        mActionColorSet = true
        return this
    }

    fun setActionTextColor(color: Int): SnackbarBuilder {
        mActionColor = color
        mActionColorSet = true
        return this
    }

    fun setAction(actionName: CharSequence?, onClickListener: View.OnClickListener?): SnackbarBuilder {
        mActionName = actionName
        mOnClickListener = onClickListener
        return this
    }

    fun setMessageTextColorRes(@ColorRes colorRes: Int): SnackbarBuilder {
        mMessageColor = ContextCompat.getColor(mContext!!, colorRes)
        mMessageColorSet = true
        return this
    }

    fun setBackgroundColorRes(@ColorRes backgroundColorRes: Int): SnackbarBuilder {
        mBackgroundColor = ContextCompat.getColor(mContext!!, backgroundColorRes)
        mBackgroundColorSet = true
        return this
    }

    fun setBackgroundColor(color: Int): SnackbarBuilder {
        mBackgroundColor = color
        mBackgroundColorSet = true
        return this
    }

    fun setMessageTextColor(color: Int): SnackbarBuilder {
        mMessageColor = color
        mMessageColorSet = true
        return this
    }

    fun show(): SnackbarBuilder {
        Preconditions.checkNotNull(mMessage, "Message is required")
        mSnackbar = Snackbar.make(mContainerView!!, mMessage!!, mDuration)
        if (mOnClickListener != null) {
            mSnackbar!!.setAction(mActionName) { v: View? ->
                mSnackbar = null
                mOnClickListener!!.onClick(v)
            }
            if (!mActionColorSet) {
                mSnackbar!!.setActionTextColor(ContextCompat.getColor(mContext!!, R.color.errorColor))
            } else {
                mSnackbar!!.setActionTextColor(mActionColor)
            }
            if (mBackgroundColorSet) {
                mSnackbar!!.view.background = ColorDrawable(mBackgroundColor)
            }
        }
        mTextView = mSnackbar!!.view.findViewById(com.google.android.material.R.id.snackbar_text)
        mAction = mSnackbar!!.view.findViewById(com.google.android.material.R.id.snackbar_action)
        mAction!!.isEnabled = mEnableAction
        mAction!!.alpha = if (mEnableAction) 1f else 0.3f
        mTextView!!.maxLines = 10
        if (mMessageColorSet) {
            mTextView!!.setTextColor(mMessageColor)
        } else {
            mTextView!!.setTextColor(ContextCompat.getColor(mContext!!, R.color.white))
        }
        if (mMessageTextSizeRes > 0) {
            mTextView!!.setTextSize(TypedValue.COMPLEX_UNIT_PX, mMessageTextSizeRes)
        }
        mSnackbar!!.show()
        return this
    }

    fun dismiss() {
        if (mSnackbar == null) return
        mSnackbar!!.dismiss()
        mTextView = null
    }

    fun setMessageTextSizeRes(messageTextSizeRes: Int): SnackbarBuilder {
        mMessageTextSizeRes = mContext!!.resources.getDimension(messageTextSizeRes)
        return this
    }

    fun setActionEnable(enable: Boolean): SnackbarBuilder {
        if (mAction != null) {
            mAction!!.isEnabled = enable
            mAction!!.alpha = if (enable) 1f else 0.3f
        } else {
            mEnableAction = enable
        }
        return this
    }
}