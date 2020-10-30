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

package network.minter.bipwallet.stories.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import network.minter.bipwallet.databinding.ItemStoryBinding
import network.minter.bipwallet.internal.helpers.HtmlCompat
import network.minter.bipwallet.internal.views.list.diff.DiffUtilDispatcher
import network.minter.bipwallet.internal.views.list.diff.DiffUtilDispatcherDelegate
import network.minter.bipwallet.stories.models.Story

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
class StoriesListAdapter(
        private val onItemClickListener: (Story, Int, View) -> Unit
) : RecyclerView.Adapter<StoriesListAdapter.ViewHolder>(), DiffUtilDispatcherDelegate<Story> {
    private var inflater: LayoutInflater? = null
    private var items: MutableList<Story> = ArrayList()
    private val roundedCornersSize = 12f

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        if (inflater == null) {
            inflater = LayoutInflater.from(parent.context)
        }

        return ViewHolder(ItemStoryBinding.inflate(inflater!!, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val story = items[position]

        holder.b.root.isSelected = story.isActive
        holder.b.image.load(story.icon) {
            transformations(RoundedCornersTransformation(
                    roundedCornersSize,
                    roundedCornersSize,
                    roundedCornersSize,
                    roundedCornersSize
            ))
        }
        holder.b.title.text = HtmlCompat.fromHtml(story.title)
        holder.b.root.setOnClickListener {
            holder.b.image.transitionName = "story_${story.id}_slide_0_image"
            onItemClickListener(items[holder.absoluteAdapterPosition], holder.absoluteAdapterPosition, holder.b.image)
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    class ViewHolder(val b: ItemStoryBinding) : RecyclerView.ViewHolder(b.root)

    override fun <T : DiffUtil.Callback?> dispatchChanges(diffUtilCallbackCls: Class<T>, items: List<Story>, detectMoves: Boolean) {
        DiffUtilDispatcher.dispatchChanges(this, diffUtilCallbackCls, items, detectMoves)
    }

    override fun <T : DiffUtil.Callback?> dispatchChanges(diffUtilCallbackCls: Class<T>?, items: List<Story>) {
        DiffUtilDispatcher.dispatchChanges(this, diffUtilCallbackCls, items)
    }

    override fun getItems(): MutableList<Story> {
        return items
    }

    override fun setItems(items: MutableList<Story>) {
        this.items = items
    }

    override fun clear() {
        items.clear()
    }
}