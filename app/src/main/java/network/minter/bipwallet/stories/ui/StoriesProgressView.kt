/*
 * Copyright (C) by MinterTeam. 2021
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

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import timber.log.Timber
import java.util.*

class StoriesProgressView : LinearLayout {
    private val progressLP = LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
    private val spaceLP = LayoutParams(4, LayoutParams.MATCH_PARENT)
    private val progressBars: MutableList<PausableProgressBar> = ArrayList()
    private var storiesCount = -1

    var isComplete = false
        private set
    var current = 0
        private set
    private var storiesListener: StoriesListener? = null
    val currentProgress: PausableProgressBar
        get() {
            if (current < 0) {
                current = 0
            }
            if (current > storiesCount - 1) {
                current = storiesCount - 1
            }
            return progressBars[current]
        }

    private val currentIsLast: Boolean
        get() {
            return current >= progressBars.size - 1
        }

    private val currentIsFirst: Boolean
        get() = current <= 0

    var autoStartNext: Boolean = true

    interface StoriesListener {
        fun onNext()
        fun onPrev()
        fun onComplete()
    }

    @JvmOverloads
    constructor(context: Context, attrs: AttributeSet? = null) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context, attrs)
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        init(context, attrs)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun init(context: Context, attrs: AttributeSet?) {
        orientation = HORIZONTAL
        storiesCount = 4
        bindViews()
    }

    private fun bindViews() {
        progressBars.clear()
        removeAllViews()
        for (i in 0 until storiesCount) {
            val p = createProgressBar()
            progressBars.add(p)
            addView(p)
            if (i + 1 < storiesCount) {
                addView(createSpace())
            }
        }
    }

    private fun createProgressBar(): PausableProgressBar {
        val p = PausableProgressBar(context)
        p.layoutParams = progressLP
        return p
    }

    private fun createSpace(): View {
        val v = View(context)
        v.layoutParams = spaceLP
        return v
    }

    /**
     * Set story count and create views
     *
     * @param storiesCount story count
     */
    fun setStoriesCount(storiesCount: Int) {
        this.storiesCount = storiesCount
        bindViews()
    }

    /**
     * Set storiesListener
     *
     * @param storiesListener StoriesListener
     */
    fun setStoriesListener(storiesListener: StoriesListener) {
        this.storiesListener = storiesListener
    }

    /**
     * Start progress animation
     */
    fun start() {
        current = 0
        Timber.d("[$current]Start anim")
        currentProgress.start()
    }

    /**
     * Start progress animation from specific progress
     */
    fun start(from: Int) {
        current = from
        for (i in 0 until from) {
            progressBars[i].finish()
        }
        currentProgress.start()
    }

    /**
     * Pause story
     */
    fun pause() {
        Timber.d("[$current]Pause anim")
        currentProgress.pause()
    }

    /**
     * Resume story
     */
    fun resume() {
        Timber.d("[$current]Resume anim")
        currentProgress.resume()
    }

    /**
     * Skip current story
     */
    fun next() {
        currentProgress.finish()
        Timber.d("[$current]Finish anim")
        if (currentIsLast) {
            Timber.d("[$current]Complete anim")
            storiesListener?.onComplete()
            return
        }
        storiesListener?.onNext()
        current++

        Timber.d("[$current]Start anim")
        currentProgress.start()
        if (!autoStartNext) {
            currentProgress.pause()
        }
    }

    fun prev(restartFirst: Boolean = false, callEvent: Boolean = true) {
        if (callEvent) storiesListener?.onPrev()
        Timber.d("[$current]Reset anim before goin prev")
        currentProgress.reset()
        if (currentIsFirst) {
            if (restartFirst) {
                current = 0
                Timber.d("[$current]Start anim: restart first")
                currentProgress.start()
            }
            return
        }
        current--
        Timber.d("[$current]Start anim (prev)")
        currentProgress.start()
    }

    /**
     * Reverse current story
     */
    fun resetCurrent() {
        Timber.d("[$current]Reset anim")
        currentProgress.reset()
    }

    fun reset() {
        Timber.d("Reset ALL anim")
        current = 0
        progressBars.forEach { it.reset() }
    }

    /**
     * Set a story's duration
     *
     * @param duration millisecond
     */
    fun setSlideDuration(duration: Long) {
        progressBars.forEach {
            it.setDuration(duration)
            it.setCallback(progressCallback())
        }
    }

    private fun progressCallback(): PausableProgressBar.Callback {
        return object : PausableProgressBar.Callback {
            override fun onStartProgress() {
            }

            override fun onFinishProgress() {
                next()
            }
        }
    }

}