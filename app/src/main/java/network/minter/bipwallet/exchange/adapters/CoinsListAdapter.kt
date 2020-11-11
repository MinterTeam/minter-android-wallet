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
package network.minter.bipwallet.exchange.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.Filterable
import network.minter.bipwallet.R
import network.minter.bipwallet.databinding.SearchItemBinding
import network.minter.bipwallet.internal.helpers.ViewExtensions
import network.minter.bipwallet.internal.helpers.ViewExtensions.visible
import network.minter.explorer.models.CoinItem
import java.util.*

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock></edward.vstock>@gmail.com>
 * @TODO remove adapter duplication, create base autocompletion adapter
 */
class CoinsListAdapter(context: Context, items: List<CoinItem>) : ArrayAdapter<CoinItem>(context, R.layout.search_item, items), Filterable {
    private var mItemsAll: List<CoinItem>? = null
    private var mSuggestions: MutableList<CoinItem>? = null
    private val mViewResourceId: Int
    private var mInflater: LayoutInflater? = null
    private var mOnItemClickListener: ((CoinItem, Int) -> Unit)? = null

    init {
        setItems(items)
        mViewResourceId = R.layout.search_item
    }

    fun setOnItemClickListener(listener: (CoinItem, Int) -> Unit) {
        mOnItemClickListener = listener
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var v = convertView
        val holder: ViewHolder
        if (mInflater == null) {
            mInflater = LayoutInflater.from(parent.context)
        }
        if (v == null) {
            v = mInflater!!.inflate(mViewResourceId, null)
            holder = ViewHolder(SearchItemBinding.bind(v))
            v.tag = holder
        } else {
            holder = v.tag as ViewHolder
        }
        onBindViewHolder(holder, position)
        return v!!
    }

    private val publishLock = Any()
    override fun getFilter(): Filter {
        return object : Filter() {
            override fun convertResultToString(resultValue: Any): String {
                return (resultValue as CoinItem).symbol
            }

            override fun performFiltering(constraint: CharSequence?): FilterResults {
                return if (constraint != null) {
                    mSuggestions!!.clear()
                    for (item in mItemsAll!!) {
                        if (item.symbol != null && item.symbol.toLowerCase().startsWith(constraint.toString().toLowerCase())) {
                            mSuggestions!!.add(item)
                        }
                    }
                    val filterResults = FilterResults()
                    filterResults.values = mSuggestions
                    filterResults.count = mSuggestions!!.size
                    filterResults
                } else {
                    FilterResults()
                }
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                val filteredList = results?.values as List<CoinItem>?
                synchronized(publishLock) {
                    if (results != null && filteredList != null && results.count > 0) {
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
    }

    fun setItems(items: List<CoinItem>) {
        mItemsAll = items
        mSuggestions = ArrayList()
    }

    private fun onBindViewHolder(vh: ViewHolder, position: Int) {
        vh.b.root.setOnClickListener { mOnItemClickListener?.invoke(getItem(position)!!, position) }

        ViewExtensions.listItemBackgroundRippleRounded(vh.b.root, position, count)

        vh.b.separator.visible = !(count == 1 || position == count - 1)

        val item = getItem(position)!!
        vh.b.searchItemTitle.text = item.symbol
        vh.b.searchItemSubtitle.visible = true
        vh.b.searchItemSubtitle.text = item.name
    }

    interface OnItemClickListener {
        fun onClick(item: CoinItem, position: Int)
    }

    class ViewHolder(val b: SearchItemBinding)
}