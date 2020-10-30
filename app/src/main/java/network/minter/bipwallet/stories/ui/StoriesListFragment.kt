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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import network.minter.bipwallet.databinding.FragmentStoriesHorizontalListBinding
import network.minter.bipwallet.internal.BaseInjectFragment
import network.minter.bipwallet.internal.Wallet
import network.minter.bipwallet.internal.helpers.ViewExtensions.visible
import network.minter.bipwallet.internal.views.list.PaddingItemDecoration
import network.minter.bipwallet.stories.adapter.StoriesDiffUtil
import network.minter.bipwallet.stories.adapter.StoriesListAdapter
import network.minter.bipwallet.stories.models.Story
import network.minter.bipwallet.stories.repo.StoriesRepository
import network.minter.bipwallet.wallets.ui.WalletsTabFragment
import javax.inject.Inject

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
class StoriesListFragment : BaseInjectFragment() {

    companion object {
        const val ARG_STORIES = "ARG_STORIES"
        fun newInstance(stories: List<Story>): StoriesListFragment {
            val args = Bundle()
            args.putParcelableArrayList(ARG_STORIES, ArrayList(stories))

            val fragment = StoriesListFragment()
            fragment.arguments = args
            return fragment
        }
    }

    @Inject lateinit var repo: StoriesRepository

    private lateinit var b: FragmentStoriesHorizontalListBinding
    private val adapter: StoriesListAdapter = StoriesListAdapter(::openStory)
    private var stories: List<Story> = ArrayList()

    fun setData(stories: List<Story>) {
        adapter.dispatchChanges(StoriesDiffUtil::class.java, stories, true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        b = FragmentStoriesHorizontalListBinding.inflate(inflater, container, false)
        b.list.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        b.list.addItemDecoration(PaddingItemDecoration(Wallet.app().display().dpToPx(48f)))
        b.list.adapter = adapter
        b.progress.visible = false

//        sharedElementReturnTransition = TransitionInflater.from(context).inflateTransition(R.transition.image_shared_element_transition)
//        sharedElementEnterTransition = TransitionInflater.from(context).inflateTransition(R.transition.image_shared_element_transition)
        stories = arguments!!.getParcelableArrayList(ARG_STORIES)!!
        setData(stories)

        // todo: mark watched, reload immediately

        return b.root
    }

    fun openStory(story: Story, position: Int, sharedView: View) {
//        StoriesPagerActivity.Builder(this, adapter.items)
//                .setStartPosition(position)
//                .addSharedView(sharedView)
//                .start()
        (parentFragment as WalletsTabFragment?)?.startStoriesPager(adapter.items, position, sharedView)

    }
}