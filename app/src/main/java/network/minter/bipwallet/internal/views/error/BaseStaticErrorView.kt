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

package network.minter.bipwallet.internal.views.error

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.StringRes
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import network.minter.bipwallet.R
import network.minter.bipwallet.internal.helpers.ViewExtensions.visible
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
abstract class BaseStaticErrorView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    protected abstract fun getMessageView(): TextView
    protected abstract fun getActionView(): TextView
    private var isShowing = false

    open fun setText(text: CharSequence): BaseStaticErrorView {
        getMessageView().text = text
        return this
    }

    open fun setText(text: String, vararg args: Any?): BaseStaticErrorView {
        getMessageView().text = String.format(text, args)
        return this
    }

    open fun setText(@StringRes textRes: Int): BaseStaticErrorView {
        getMessageView().setText(textRes)
        return this
    }

    open fun setText(@StringRes textRes: Int, vararg args: Any?): BaseStaticErrorView {
        getMessageView().text = context.getString(textRes, args)
        return this
    }

    open fun setActionName(name: CharSequence?): BaseStaticErrorView {
        getMessageView().text = name ?: context.getString(R.string.btn_retry)
        return this
    }

    open fun setActionName(resId: Int): BaseStaticErrorView {
        getActionView().setText(resId)
        return this
    }

    open fun setActionListener(listener: View.OnClickListener?): BaseStaticErrorView {
        getActionView().setOnClickListener {
            listener?.onClick(it)
            dismiss()
        }
        return this
    }

    open fun show() {
        if (isShowing) {
            return
        }
        isShowing = true
        Timber.d("Show error")
        visible = true

//        alpha = 0f
//        visible = true
//        animate()
//                .alpha(1f)
//                .setDuration(150)
//                .start()
    }

    open fun showFor(delay: Long = 3, unit: TimeUnit = TimeUnit.SECONDS) {
        Observable.timer(delay, unit)
                .observeOn(Schedulers.io())
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    dismiss()
                }
    }

    open fun dismiss() {
        if (!isShowing) {
            return
        }
        Timber.d("Hide error")
        visible = false
        isShowing = false
//        alpha = 1f
//        animate()
//                .alpha(0f)
//                .setDuration(150)
//                .setListener(object : Animator.AnimatorListener {
//                    override fun onAnimationStart(animation: Animator?) {
//                    }
//
//                    override fun onAnimationEnd(animation: Animator?) {
//                        this@BaseStaticErrorView.visible = false
//                    }
//
//                    override fun onAnimationCancel(animation: Animator?) {
//                        this@BaseStaticErrorView.visible = false
//                    }
//
//                    override fun onAnimationRepeat(animation: Animator?) {
//                    }
//                })
//                .start()
    }
}