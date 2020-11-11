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

import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import androidx.core.app.SharedElementCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import network.minter.bipwallet.R
import network.minter.bipwallet.databinding.ActivityStoriesBinding
import network.minter.bipwallet.home.ui.HomeActivity
import network.minter.bipwallet.internal.BaseInjectFragment
import network.minter.bipwallet.internal.helpers.ViewHelper
import network.minter.bipwallet.stories.StoriesScope
import network.minter.bipwallet.stories.models.Story
import timber.log.Timber
import java.util.*
import kotlin.collections.ArrayList

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
@StoriesScope
class StoriesPagerFragment : BaseInjectFragment() {
    companion object {
        const val ARG_STORIES = "EXTRA_STORIES"
        const val ARG_START_POSITION = "EXTRA_START_POSITION"
        fun newInstance(stories: List<Story>, position: Int): StoriesPagerFragment {
            val args = Bundle()
            args.putParcelableArrayList(ARG_STORIES, ArrayList(stories))
            args.putInt(ARG_START_POSITION, position)

            val fragment = StoriesPagerFragment()
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var b: ActivityStoriesBinding
    private var stories: List<Story> = ArrayList()
    private var startPosition: Int = 0
    private var position: Int = 0
    private val tabs = WeakHashMap<Int, StoryFragment>()
    private var lastSelectedTab: Int = -1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        ViewHelper.setSystemBarsLightness(activity, true)
        activity!!.window.navigationBarColor = ContextCompat.getColor(context!!, R.color.transparent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity!!.window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            activity!!.window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
            activity!!.window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION, WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
        }

        b = ActivityStoriesBinding.inflate(layoutInflater, container, false)

        stories = arguments?.getParcelableArrayList(ARG_STORIES) ?: ArrayList()
        startPosition = arguments?.getInt(ARG_START_POSITION, 0) ?: 0
        position = startPosition

        b.pager.adapter = StorySlidesAdapter(this)
        b.pager.setPageTransformer(DepthPageTransformer())
        b.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (lastSelectedTab != -1 && lastSelectedTab != position) {
                    Timber.tag("StoryFragment").d("onPageSelected: $position , prev: $lastSelectedTab")
                    tabs[lastSelectedTab]?.onPageUnselected()
                }
                lastSelectedTab = position
                tabs[position]?.onPageSelected()
                this@StoriesPagerFragment.position = position
            }
        })
        b.pager.setCurrentItem(startPosition.coerceAtMost(stories.size - 1), false)

        val cb: SharedElementCallback = object : SharedElementCallback() {
            override fun onSharedElementsArrived(sharedElementNames: MutableList<String>, sharedElements: MutableList<View>, listener: OnSharedElementsReadyListener) {
                super.onSharedElementsArrived(sharedElementNames, sharedElements, listener)
                Timber.d("onSharedElementsArrived")
            }

            override fun onRejectSharedElements(rejectedSharedElements: MutableList<View>) {
                super.onRejectSharedElements(rejectedSharedElements)
                rejectedSharedElements.forEach {
                    Timber.d("Rejected shared element ${it.context.resources.getResourceEntryName(it.id)}")
                }
            }

            override fun onMapSharedElements(names: MutableList<String>, sharedElements: MutableMap<String, View>) {
                super.onMapSharedElements(names, sharedElements)
                if (tabs.size > 0 && tabs[0] != null) {
                    val iv = tabs[0]!!.view?.findViewById<ImageView>(R.id.image)
                    if (iv != null) {
                        Timber.d("Map shared view image")
                        sharedElements[names[0]] = iv
                    }
                }
            }
        }

        setEnterSharedElementCallback(cb)

        return b.root
    }

    fun goNext() {
        position++
        if (position > stories.size - 1) {
            (activity as HomeActivity?)?.closeStoriesPager()
            return
        }

        b.pager.currentItem = position
    }

    fun goPrev(): Boolean {
        position--
        if (position < 0) {
            position = 0
            return false
        }

        b.pager.currentItem = position
        return true
    }

    class StorySlidesAdapter(private val fragment: StoriesPagerFragment) : FragmentStateAdapter(fragment) {

        override fun getItemId(position: Int): Long {
            return fragment.stories[position].id
        }

        override fun getItemCount(): Int {
            return fragment.stories.size
        }

        override fun createFragment(position: Int): Fragment {
            val instance = StoryFragment.newInstance(fragment.stories[position].slides!!, position)
            if (!fragment.tabs.containsKey(position)) {
                fragment.tabs[position] = instance
            }
            return instance
        }
    }
}