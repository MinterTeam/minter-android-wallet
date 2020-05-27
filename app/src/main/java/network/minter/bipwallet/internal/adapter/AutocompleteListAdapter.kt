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
package network.minter.bipwallet.internal.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.Filterable
import androidx.annotation.LayoutRes
import network.minter.bipwallet.R
import java.lang.reflect.InvocationTargetException
import java.util.*

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
abstract class AutocompleteListAdapter<Item, VH : AutocompleteListAdapter.ViewHolder>(
        context: Context
) : ArrayAdapter<Item>(context, R.layout.search_item, ArrayList<Item>()), Filterable {

    private var mItemsAll: List<Item> = ArrayList()
    private var mSuggestions: MutableList<Item> = ArrayList()
    private val mViewResourceId: Int
    private var mInflater: LayoutInflater? = null
    private var mOnItemClickListener: OnItemClickListener<Item>? = null

    @Suppress("UNCHECKED_CAST")
    override fun getFilter(): Filter {
        return object : Filter() {
            override fun convertResultToString(resultValue: Any): String {
                return resultToString(resultValue as Item)
            }

            override fun performFiltering(constraint: CharSequence?): FilterResults {
                mSuggestions.clear()
                for (item in mItemsAll) {
                    if (isItemMatchesConstraint(item, constraint)) {
                        mSuggestions.add(item)
                    }
                }

                val filterResults = FilterResults()
                filterResults.values = mSuggestions
                filterResults.count = mSuggestions.size
                return filterResults
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults) {
                val filteredList = results.values as List<Item>
                if (results.count > 0) {
                    clear()
                    for (c in filteredList) {
                        add(c)
                    }
                    notifyDataSetChanged()
                } else {
                    clear()
                    notifyDataSetInvalidated()
                }
            }
        }
    }

    fun setOnItemClickListener(listener: OnItemClickListener<Item>) {
        mOnItemClickListener = listener
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var v = convertView
        val holder: VH
        if (mInflater == null) {
            mInflater = LayoutInflater.from(parent.context)
        }
        if (v == null) {
            v = mInflater!!.inflate(mViewResourceId, parent, false)
            try {
                holder = viewHolderClass().getConstructor(View::class.java).newInstance(v)
                v.tag = holder
            } catch (e: IllegalAccessException) {
                throw RuntimeException(e)
            } catch (e: InstantiationException) {
                throw RuntimeException(e)
            } catch (e: InvocationTargetException) {
                throw RuntimeException(e)
            } catch (e: NoSuchMethodException) {
                throw RuntimeException(e)
            }
        } else {
            holder = v.tag as VH
        }
        val item: Item = getItem(position)!!
        onBindViewHolder(item, holder, position)
        return v!!
    }

    fun setItems(items: List<Item>) {
        mItemsAll = items
        mSuggestions = ArrayList()
        notifyDataSetChanged()
    }

    @get:LayoutRes protected abstract val layoutItemId: Int
    protected abstract fun viewHolderClass(): Class<VH>
    protected abstract fun isItemMatchesConstraint(item: Item, constraint: CharSequence?): Boolean
    protected abstract fun resultToString(item: Item): String

    @Suppress("UNCHECKED_CAST")
    protected open fun onBindViewHolder(item: Item, holder: VH, position: Int) {
        if (mOnItemClickListener != null) {
            holder.itemView.setOnClickListener { mOnItemClickListener?.onClick(getItem(position) as Item, position) }
        }
    }

    interface OnItemClickListener<Item> {
        fun onClick(item: Item, position: Int)
    }

    abstract class ViewHolder(var itemView: View)

    init {
        mViewResourceId = layoutItemId
    }
}