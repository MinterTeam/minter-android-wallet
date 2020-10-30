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

import android.app.Activity
import android.app.Service
import android.app.SharedElementCallback
import android.content.Intent
import android.os.Bundle
import android.transition.TransitionInflater
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import network.minter.bipwallet.R
import network.minter.bipwallet.databinding.ActivityStoriesBinding
import network.minter.bipwallet.internal.BaseInjectActivity
import network.minter.bipwallet.internal.system.ActivityBuilder
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
class StoriesPagerActivity : BaseInjectActivity() {
    private lateinit var b: ActivityStoriesBinding
    private var stories: List<Story> = ArrayList()
    private var startPosition: Int = 0
    private var position: Int = 0
    private var tabs: WeakHashMap<Int, StoryFragment> = WeakHashMap()
//    private var firstTab: WeakReference<StoryFragment>? = null

    companion object {
        const val EXTRA_STORIES = "EXTRA_STORIES"
        const val EXTRA_START_POSITION = "EXTRA_START_POSITION"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        postponeEnterTransition()

        b = ActivityStoriesBinding.inflate(layoutInflater)
        setContentView(b.root)

        stories = intent?.getParcelableArrayListExtra(EXTRA_STORIES) ?: ArrayList()
        startPosition = intent?.getIntExtra(EXTRA_START_POSITION, 0) ?: 0
        position = startPosition

        b.pager.adapter = StorySlidesAdapter(this)
        b.pager.setPageTransformer(DepthPageTransformer())
        b.pager.setCurrentItem(startPosition.coerceAtMost(stories.size - 1), false)


        val transition = TransitionInflater.from(this).inflateTransition(R.transition.image_shared_element_transition)
        window.sharedElementEnterTransition = transition

        setEnterSharedElementCallback(object : SharedElementCallback() {
            override fun onMapSharedElements(names: MutableList<String>, sharedElements: MutableMap<String, View>) {
                super.onMapSharedElements(names, sharedElements)
                if (tabs.containsKey(position) && tabs[position] != null) {
                    val iv = tabs[position]!!.view?.findViewById<ImageView>(R.id.image)
                    if (iv != null) {
                        Timber.d("Map shared view image")
                        sharedElements[names[0]] = iv
                    }
                }
            }
        })

//        startPostponedEnterTransition()
    }

    override fun onDestroy() {
        super.onDestroy()
        tabs.clear()
    }

    fun goNext() {
        position++
        if (position > stories.size - 1) {
            finishAfterTransition()
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

    class StorySlidesAdapter(private val fragment: StoriesPagerActivity) : FragmentStateAdapter(fragment) {

        override fun getItemId(position: Int): Long {
            return fragment.stories[position].id
        }

        override fun getItemCount(): Int {
            return fragment.stories.size
        }

        override fun createFragment(position: Int): Fragment {
            val instance = StoryFragment.newInstance(fragment.stories[position].slides!!, position)
            fragment.tabs[position] = instance
            return instance
        }
    }

    class Builder : ActivityBuilder {
        private var stories: List<Story>
        private var position: Int = 0

        constructor(from: Activity, items: List<Story>) : super(from) {
            stories = items
        }

        constructor(from: Fragment, items: List<Story>) : super(from) {
            stories = items
        }

        constructor(from: Service, items: List<Story>) : super(from) {
            stories = items
        }

        fun setStartPosition(position: Int): Builder {
            this.position = position
            return this
        }

        override fun onBeforeStart(intent: Intent) {
            super.onBeforeStart(intent)
            intent.putParcelableArrayListExtra(EXTRA_STORIES, ArrayList(stories))
            intent.putExtra(EXTRA_START_POSITION, position)
        }

        override fun getActivityClass(): Class<*> {
            return StoriesPagerActivity::class.java
        }

    }
}