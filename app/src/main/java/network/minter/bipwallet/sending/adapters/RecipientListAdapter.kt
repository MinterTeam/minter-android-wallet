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
package network.minter.bipwallet.sending.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import network.minter.bipwallet.R
import network.minter.bipwallet.addressbook.models.AddressContact
import network.minter.bipwallet.internal.helpers.ViewExtensions.listItemBackgroundRippleRounded
import java.util.*

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock></edward.vstock>@gmail.com>
 */
class RecipientListAdapter(context: Context) : ArrayAdapter<AddressContact>(context, R.layout.search_item, ArrayList()), Filterable {
    private var mItemsAll: List<AddressContact> = ArrayList()
    private var mSuggestions: MutableList<AddressContact> = ArrayList()
    private val mViewResourceId: Int = R.layout.search_item
    private var mInflater: LayoutInflater? = null
    private var mOnItemClickListener: ((AddressContact, Int) -> Unit)? = null
    fun setOnItemClickListener(listener: (AddressContact, Int) -> Unit) {
        mOnItemClickListener = listener
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var v = convertView
        val holder: ViewHolder
        if (mInflater == null) {
            mInflater = LayoutInflater.from(parent.context)
        }
        if (v == null) {
            v = mInflater!!.inflate(mViewResourceId, parent, false)
            holder = ViewHolder(v)
            v.tag = holder
        } else {
            holder = v.tag as ViewHolder
        }
        onBindViewHolder(holder, position)
        return v!!
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun convertResultToString(resultValue: Any): String {
                return (resultValue as AddressContact).name!!
            }

            override fun performFiltering(constraint: CharSequence?): FilterResults {
                return if (constraint != null) {
                    mSuggestions.clear()
                    for (item in mItemsAll) {
                        if (item.name != null && item.name!!.lowercase(Locale.getDefault()).startsWith(constraint.toString().lowercase(Locale.getDefault()))) {
                            mSuggestions.add(item)
                        } else if (item.address != null && item.address!!.lowercase(Locale.getDefault()).startsWith(constraint.toString().lowercase(Locale.getDefault()))) {
                            mSuggestions.add(item)
                        }
                    }
                    val filterResults = FilterResults()
                    filterResults.values = mSuggestions
                    filterResults.count = mSuggestions.size
                    filterResults
                } else {
                    FilterResults()
                }
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                val filteredList = results?.values as List<AddressContact>?
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

    fun setItems(items: List<AddressContact>) {
        mItemsAll = items
        mSuggestions = ArrayList()
        notifyDataSetChanged()
    }

    private fun onBindViewHolder(vh: ViewHolder, position: Int) {
        vh.itemView!!.setOnClickListener { mOnItemClickListener?.invoke(getItem(position)!!, position) }
        val item = getItem(position)

        listItemBackgroundRippleRounded(vh.itemView!!, position, count)

        if (count == 1 || position == count - 1) {
            vh.separator!!.visibility = View.GONE
        } else {
            vh.separator!!.visibility = View.VISIBLE
        }
        if (item!!.name == null) {
            vh.title!!.text = item.address
            vh.subtitle!!.visibility = View.GONE
            vh.subtitle!!.text = null
        } else {
            vh.title!!.text = item.name
            vh.subtitle!!.visibility = View.VISIBLE
            vh.subtitle!!.text = item.address
        }
    }


    internal class ViewHolder(var itemView: View?) {

        @JvmField @BindView(R.id.search_item_title)
        var title: TextView? = null

        @JvmField @BindView(R.id.search_item_subtitle)
        var subtitle: TextView? = null

        @JvmField @BindView(R.id.separator)
        var separator: View? = null

        init {
            ButterKnife.bind(this, itemView!!)
        }
    }

}