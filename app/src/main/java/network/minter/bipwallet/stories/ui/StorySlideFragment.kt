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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import network.minter.bipwallet.R
import network.minter.bipwallet.apis.reactive.dp
import network.minter.bipwallet.databinding.FragmentStorySlideBinding
import network.minter.bipwallet.internal.BaseInjectFragment
import network.minter.bipwallet.internal.helpers.HtmlCompat
import network.minter.bipwallet.internal.helpers.ViewExtensions.visible
import network.minter.bipwallet.internal.helpers.ViewHelper
import network.minter.bipwallet.internal.helpers.loadPicasso
import network.minter.bipwallet.stories.StoriesScope
import network.minter.bipwallet.stories.models.StorySlide
import timber.log.Timber


/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
@StoriesScope
class StorySlideFragment : BaseInjectFragment() {

    companion object {
        const val ARG_SLIDE = "ARG_SLIDE"
        const val ARG_POSITION = "ARG_POSITION"
        fun newInstance(slide: StorySlide, position: Int): StorySlideFragment {
            val args = Bundle()
            args.putParcelable(ARG_SLIDE, slide)
            args.putInt(ARG_POSITION, position)

            val fragment = StorySlideFragment()
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var b: FragmentStorySlideBinding
    private lateinit var slide: StorySlide
    private var position: Int = -1
    private var slideLoaded = false
    private var isLightImage = false

    override fun onStop() {
        super.onStop()
        slideLoaded = false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        b = FragmentStorySlideBinding.inflate(inflater, container, false)
        b.progress.visible = true
        slide = arguments!!.getParcelable(ARG_SLIDE)!!
        position = arguments!!.getInt(ARG_POSITION)
        b.image.transitionName = "story_${slide.storyId}_slide_${position}_image"

        var showContentRoot = false
        if (!slide.title.isNullOrEmpty()) {
            b.title.text = HtmlCompat.fromHtml(slide.title)
            b.title.visible = true
            showContentRoot = true
        } else {
            b.title.visible = false
        }

        if (!slide.text.isNullOrEmpty()) {
            b.text.text = HtmlCompat.fromHtml(slide.text)
            b.text.visible = true
            showContentRoot = true
        } else {
            b.text.visible = false
        }

        var additionalOffset = 0
        if (!slide.link.isNullOrEmpty()) {
            additionalOffset = 90.dp().toInt()
        }

        b.contentRoot.setPadding(
                b.contentRoot.paddingLeft,
                b.contentRoot.paddingTop,
                b.contentRoot.paddingRight,
                ViewHelper.getNavigationBarHeight(requireContext()) + resources.getDimension(R.dimen.margin_edges).toInt() + additionalOffset
        )
        b.contentRoot.visible = showContentRoot

        return b.root
    }

    override fun onResume() {
        super.onResume()
        if (slideLoaded) {
            (parentFragment as StoryFragment?)?.onSlideLoaded(isLightImage)
        }
    }

    private val pagerFragment: StoriesPagerFragment?
        get() {
            if (parentFragment != null && parentFragment is StoryFragment) {
                return parentFragment!!.parentFragment as StoriesPagerFragment?
            }

            return null
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        b.image.loadPicasso(slide.file, {
            slideLoaded = true
            b.progress.visible = false
//            isLightImage = ImageHelper.isLightImage(b.image)
            (parentFragment as StoryFragment?)?.onSlideLoaded(isLightImage)

        }, {
            b.progress.visible = false
            b.errorText.visible = true
            Timber.w(it, "Unable to load story slide image: ${slide.file}")
        })

    }
}
