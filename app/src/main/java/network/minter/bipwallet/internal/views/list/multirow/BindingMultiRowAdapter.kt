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

package network.minter.bipwallet.internal.views.list.multirow

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */

class BindingViewHolder<out T : ViewBinding>(val binding: T) : RecyclerView.ViewHolder(binding.root)

interface BindingRow<B : ViewBinding> {
    fun onBind(binding: B)
    fun onUnbind(binding: B) {}
    fun getBindingClass(): Class<B>
}

open class BindingMultiRowAdapter : RecyclerView.Adapter<BindingViewHolder<ViewBinding>>() {
    private var inflater: LayoutInflater? = null
    protected var items = ArrayList<BindingRow<ViewBinding>>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BindingViewHolder<ViewBinding> {
        if (inflater == null) {
            inflater = LayoutInflater.from(parent.context)
        }

        val row = items[viewType]
        val inflateFunc = row.getBindingClass().getMethod("inflate", LayoutInflater::class.java, ViewGroup::class.java, Boolean::class.java)
        val viewBinding = inflateFunc.invoke(null, inflater!!, parent, false) as ViewBinding
        return BindingViewHolder(viewBinding)
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun onBindViewHolder(holder: BindingViewHolder<ViewBinding>, position: Int) {
        val row = items[position]

        row.onBind(row.getBindingClass().cast(holder.binding)!!)
    }

    override fun onViewDetachedFromWindow(holder: BindingViewHolder<ViewBinding>) {
        super.onViewDetachedFromWindow(holder)
        val position = holder.absoluteAdapterPosition
        if (position < 0) {
            return
        }
        val item = items[position]
        try {
            item.onBind(holder.binding)
        } catch (ignore: ClassCastException) {
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    @Suppress("UNCHECKED_CAST")
    fun add(row: BindingRow<*>) {
        items.add(row as BindingRow<ViewBinding>)
        notifyItemInserted(itemCount)
    }

    @Suppress("UNCHECKED_CAST")
    fun addAll(rows: List<BindingRow<*>>) {
        val pos = items.size
        items.addAll(rows.map { it as BindingRow<ViewBinding> })
        notifyItemRangeInserted(pos, rows.size)
    }

    fun remove(position: Int) {
        if (items.size > position) {
            items.removeAt(position)
        }
    }

    fun clear() {
        if (items.isEmpty()) return
        items.clear()
        notifyDataSetChanged()
    }

    fun clearNotify() {
        if (items.isEmpty()) return

        val sz = items.size
        items.clear()
        notifyItemRangeRemoved(0, sz)
    }


}
