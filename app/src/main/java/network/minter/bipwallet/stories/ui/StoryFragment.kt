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

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import io.reactivex.schedulers.Schedulers
import network.minter.bipwallet.R
import network.minter.bipwallet.apis.reactive.dp
import network.minter.bipwallet.databinding.FragmentStoryBinding
import network.minter.bipwallet.home.ui.HomeActivity
import network.minter.bipwallet.internal.BaseInjectFragment
import network.minter.bipwallet.internal.Wallet
import network.minter.bipwallet.internal.common.DeferredCall
import network.minter.bipwallet.internal.helpers.ViewExtensions.visible
import network.minter.bipwallet.internal.helpers.ViewHelper
import network.minter.bipwallet.stories.StoriesScope
import network.minter.bipwallet.stories.models.StorySlide
import network.minter.bipwallet.stories.repo.RepoCachedStories
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.abs

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
@StoriesScope
class StoryFragment : BaseInjectFragment() {
    companion object {
        const val ARG_SLIDES = "ARG_SLIDES"
        const val ARG_POSITION = "ARG_POSITION"
        fun newInstance(slides: List<StorySlide>, storyPosition: Int): StoryFragment {
            val args = Bundle()
            args.putParcelableArrayList(ARG_SLIDES, ArrayList(slides))
            args.putInt(ARG_POSITION, storyPosition)

            val fragment = StoryFragment()
            fragment.arguments = args
            return fragment
        }
    }

    @Inject lateinit var repoCachedStories: RepoCachedStories
    private lateinit var b: FragmentStoryBinding
    private val deferView = DeferredCall.create<FragmentStoryBinding>()
    private var slides: List<StorySlide> = ArrayList()
    private var slidePosition: Int = 0
    private var isFirstStory = false
    private var blockPagerTouches = false
    private var startProgress = false
    private var pauseByUser = false
    private var storyPosition: Int = 0

    private fun goNext() {
        slidePosition++
        if (slidePosition > slides.size - 1) {
            Timber.d("Story: last slide reached. Going to next story")
            slidePosition = 0
            goNextStory()
            return
        }
        Timber.d("Story: next slide [$slidePosition]")
        b.pager.currentItem = slidePosition
    }

    private fun goPrev() {
        slidePosition--
        if (slidePosition < 0) {
            Timber.d("Story: first slide reached. Going to prev story")
            slidePosition = 0
            if (!goPrevStory()) {
                Timber.d("Story: can't go prev story. Restarting progress...")
            }
            return
        }
        Timber.d("Story: next (prev) slide [$slidePosition]")
        if (b.pager.currentItem == slidePosition) {
            return
        }
        b.pager.currentItem = slidePosition
    }

    private fun goNextStory() {
        (parentFragment as StoriesPagerFragment?)?.goNext()
    }

    private fun goPrevStory(): Boolean {
        return (parentFragment as StoriesPagerFragment?)?.goPrev() == true
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        b = FragmentStoryBinding.inflate(inflater, container, false)
        deferView.attach(b)
        slides = arguments?.getParcelableArrayList(ARG_SLIDES) ?: ArrayList()
        storyPosition = arguments?.getInt(ARG_POSITION, 0) ?: 0
        isFirstStory = storyPosition == 0


        pauseByUser = savedInstanceState?.getBoolean("state_pause_by_user", false) ?: false


        repoCachedStories.entity.markWatched(slides[0].storyId)
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .subscribe(
                        {
//                            Timber.d("Mark story ${slides[0].storyId} as watched")
                            repoCachedStories.update(true)
                        },
                        { t -> Timber.w(t, "Unable to mark story ${slides[0].storyId} as watched") }
                )

        b.pager.adapter = StorySlideAdapter(this)
        b.pager.setPageTransformer(FadeOutPageTransformer())


        b.pager.setOnTouchListener { _, event ->
            if (!blockPagerTouches) {
                return@setOnTouchListener b.pager.touchDelegate.onTouchEvent(event)
            }
            false
        }

        b.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                slidePosition = position
                onStorySelected(position)
            }
        })
        b.progress.autoStartNext = false
        b.progress.setStoriesCount(slides.size)
        b.progress.setSlideDuration(7000)
        b.progress.setStoriesListener(object : StoriesProgressView.StoriesListener {
            override fun onNext() {
                goNext()
            }

            override fun onPrev() {
                goPrev()
            }

            override fun onComplete() {
                goNext()
            }
        })

        val linkLP = b.linkInfo.layoutParams as ViewGroup.MarginLayoutParams
        linkLP.bottomMargin = ViewHelper.getNavigationBarHeight(context)
        b.linkInfo.layoutParams = linkLP

        b.overlayLeft.setOnTouchListener(OverlayTouchHandler(true, this))
        b.overlayRight.setOnTouchListener(OverlayTouchHandler(false, this))

        b.actionClose.setOnClickListener {
            (activity as HomeActivity?)?.closeStoriesPager()
        }

        b.seeMoreContainer.setPadding(
                b.seeMoreContainer.paddingLeft,
                b.seeMoreContainer.paddingTop,
                b.seeMoreContainer.paddingRight,
                ViewHelper.getNavigationBarHeight(requireContext()) + 4.dp().toInt()
        )

        return b.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        deferView.detach()
    }

    private fun onStorySelected(position: Int) {
        if (!slides[position].link.isNullOrEmpty()) {
            b.actionShare.setOnClickListener {
                val shareIntent = Intent()
                shareIntent.action = Intent.ACTION_SEND
                shareIntent.putExtra(Intent.EXTRA_TEXT, slides[position].link)
                shareIntent.type = "text/plain"
                pauseByUser = true
                startActivity(Intent.createChooser(shareIntent, getString(R.string.title_share_story)))
            }
            b.actionShare.visible = true

            b.seeMoreContainer.visible = true
            b.seeMoreAction.setOnClickListener {
                startStoryUrl(position)
            }
        } else {
            b.seeMoreContainer.visible = false
            b.actionShare.visible = false
        }
    }

    private fun slideHasLink(): Boolean {
        return !slides[slidePosition].link.isNullOrEmpty()
    }

    private fun startStoryUrl(position: Int = -1) {
        val pos = if (position == -1) slidePosition else position
        if (slides[pos].link?.isNotEmpty() == true) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(slides[slidePosition].link))
            startActivity(intent)
            (activity as HomeActivity?)?.closeStoriesPager()
        }
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("state_pause_by_user", pauseByUser)
    }

    override fun onPause() {
        super.onPause()
        b.progress.pause()
    }

    override fun onStop() {
        super.onStop()
        pauseByUser = false
        b.progress.reset()
        slidePosition = 0
        b.pager.setCurrentItem(slidePosition, false)
        Timber.d("Story $storyPosition reset")
    }

    override fun onResume() {
        super.onResume()
        if (pauseByUser) {
            b.progress.resume()
            pauseByUser = false
            Timber.d("Story $storyPosition resumed")
        } else {
            Timber.d("Story $storyPosition reset")
            b.progress.reset()
            b.progress.start()
            if (!startProgress) {
                b.progress.pause()
            }
            Timber.d("Story $storyPosition started")
        }
    }

    fun onSlideLoaded(isLightImage: Boolean) {
        startProgress = true
        b.progress.resume()
    }

    fun onPageUnselected() {
        slidePosition = 0
        deferView.call {
            it.progress.reset()
            it.pager.setCurrentItem(slidePosition, false)
            Timber.d("Story $storyPosition unselected")
        }
    }

    fun onPageSelected() {
        Timber.d("Story $storyPosition selected")
    }

    private class OverlayTouchHandler(
            private val left: Boolean,
            private val fragment: StoryFragment
    ) : View.OnTouchListener {
        private var touchDownTime: Long = 0

        val slideUpThreshold: Float = Wallet.app().display().height.toFloat() / 3f
        val slideUpViewTranslateY: Float = Wallet.app().display().height.toFloat() / 4f
        val clickActionThreshold = 200
        val touchActionThreshold = 450
        val touchSlop = ViewConfiguration.get(fragment.context).scaledTouchSlop
        var startX = 0f
        var startY = 0f

        private fun isSimpleClick(startX: Float, endX: Float, startY: Float, endY: Float): Boolean {
            val differenceX = abs(startX - endX)
            val differenceY = abs(startY - endY)
            return !(differenceX > clickActionThreshold || differenceY > clickActionThreshold)
        }

        private fun isLongPress(startX: Float, endX: Float, startY: Float, endY: Float): Boolean {
            val differenceX = abs(startX - endX)
            val differenceY = abs(startY - endY)
            return !(differenceX > touchActionThreshold || differenceY > touchActionThreshold)
        }

        override fun onTouch(v: View, event: MotionEvent): Boolean {
            return when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.x
                    startY = event.y

                    fragment.b.progress.pause()
                    touchDownTime = event.eventTime
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val endX = event.x
                    val endY = event.y
                    if (isSimpleClick(startX, endX, startY, endY) && (event.eventTime - touchDownTime) < 200) {
                        if (left) {
                            fragment.b.progress.prev(fragment.isFirstStory && fragment.slidePosition == 0)
                        } else {
                            fragment.b.progress.next()
                        }
                        v.performClick()
                    } else if (isLongPress(startX, endX, startY, endY) && (event.eventTime - touchDownTime) > 200) {
                        fragment.b.progress.resume()
                        touchDownTime = 0
                    } else {
                        fragment.b.progress.resume()
                    }

                    fragment.b.linkInfo.animate()
                            .alpha(0f)
                            .setDuration(150).start()
                    fragment.b.seeMoreContainer.animate()
                            .alpha(1f)
                            .setDuration(150).start()

                    val animator = ValueAnimator
                            .ofFloat(fragment.b.pager.translationY, 0f)
                            .setDuration(150)

                    animator.addUpdateListener {
                        fragment.b.pager.translationY = it.animatedValue as Float
                        fragment.b.seeMoreContainer.translationY = it.animatedValue as Float
                    }
                    animator.start()

                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (fragment.slideHasLink()) {
                        val deltaY = startY - event.y
                        // only swipe up

                        if (deltaY > touchSlop) {
                            fragment.blockPagerTouches = true
                            fragment.b.pager.requestDisallowInterceptTouchEvent(true)
                            val percent = deltaY / slideUpThreshold
//                            Timber.d("Alpha percent: $percent")
                            fragment.b.linkInfo.alpha = percent.coerceAtMost(1.0f)
                            fragment.b.pager.translationY = -slideUpViewTranslateY * percent
                            fragment.b.seeMoreContainer.translationY = -slideUpViewTranslateY * percent
                            fragment.b.seeMoreContainer.alpha = 1f - percent

                            if (percent > 0.99f) {
                                fragment.startStoryUrl()
                            }
                        }
                    }
                    false
                }
                MotionEvent.ACTION_CANCEL -> {
                    fragment.b.progress.resume()
                    fragment.b.linkInfo.animate().alpha(0f).setDuration(150).start()
                    fragment.b.seeMoreContainer.animate().alpha(1f).setDuration(150).start()

                    val animator = ValueAnimator
                            .ofFloat(fragment.b.pager.translationY, 0f)
                            .setDuration(150)

                    animator.addUpdateListener {
                        fragment.b.pager.translationY = it.animatedValue as Float
                        fragment.b.seeMoreContainer.translationY = it.animatedValue as Float
                    }
                    animator.start()

                    fragment.blockPagerTouches = false
                    fragment.b.pager.requestDisallowInterceptTouchEvent(false)
                    true
                }
                else -> true
            }
        }
    }

    class StorySlideAdapter(private val fragment: StoryFragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int {
            return fragment.slides.size
        }

        override fun createFragment(position: Int): Fragment {
            return StorySlideFragment.newInstance(fragment.slides[position], position)
        }

    }
}