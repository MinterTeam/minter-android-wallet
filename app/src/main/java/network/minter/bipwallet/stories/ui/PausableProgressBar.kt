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
package network.minter.bipwallet.stories.ui

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import network.minter.bipwallet.R
import network.minter.bipwallet.internal.helpers.ViewExtensions.visible
import timber.log.Timber

class PausableProgressBar @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet?,
        @AttrRes defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    companion object {
        private const val DEFAULT_PROGRESS_DURATION = 2000
    }

    init {
        LayoutInflater.from(context).inflate(R.layout.view_pausable_progress, this)
    }

    private val progressView: View by lazy { findViewById(R.id.front_progress) }
    private val maxView: View by lazy { findViewById(R.id.max_progress) }
    private var anim: ValueAnimator? = null
    private var duration = DEFAULT_PROGRESS_DURATION.toLong()
    private var callback: Callback? = null

    var progress: Float = 0.0f
        private set

    interface Callback {
        fun onStartProgress()
        fun onFinishProgress()
    }

    constructor(context: Context) : this(context, null)

    fun setDuration(duration: Long) {
        this.duration = duration
    }


    fun setCallback(callback: Callback) {
        this.callback = callback
    }

    fun finish() {
        reset()
        maxView.visible = true
    }

    private val animListener: Animator.AnimatorListener = object : Animator.AnimatorListener {
        override fun onAnimationStart(animation: Animator) {
            callback?.onStartProgress()
        }

        override fun onAnimationEnd(animation: Animator) {
            Timber.d("Anim end")
        }

        override fun onAnimationCancel(animation: Animator?) {
            Timber.d("Anim cancled")
        }

        override fun onAnimationRepeat(animation: Animator?) {
        }

    }

    fun start() {
        // reset before start, user can start animation as much times as he wishes
        reset()

        anim = ValueAnimator.ofFloat(0f, 1f)
        anim!!.duration = duration
        anim!!.interpolator = LinearInterpolator()
        anim!!.addUpdateListener {
            progress = it.animatedFraction
            progressView.scaleX = it.animatedFraction
            if (progress > 0.99f) {
                maxView.visible = true
                callback?.onFinishProgress()
            }
        }
        anim!!.addListener(animListener)

        anim!!.start()
    }

    private var animCanceled = false

    fun pause() {
        anim?.pause()
    }

    fun resume() {
        anim?.resume()
    }

    fun reset() {
        progressView.scaleX = 0f
        maxView.visible = false
        if (anim?.isStarted == true) {
            animCanceled = true
            anim?.removeListener(animListener)
            anim?.cancel()
            anim = null
        }
    }
}