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
package network.minter.bipwallet.addressbook.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import com.zerobranch.layout.SwipeLayout
import network.minter.bipwallet.R
import network.minter.bipwallet.addressbook.models.AddressBookItem
import network.minter.bipwallet.addressbook.models.AddressBookItemHeader
import network.minter.bipwallet.addressbook.models.AddressContact
import network.minter.bipwallet.internal.views.list.diff.DiffUtilDispatcher
import network.minter.bipwallet.internal.views.list.diff.DiffUtilDispatcherDelegate
import network.minter.bipwallet.internal.views.widgets.BipCircleImageView
import java.util.*

class AddressBookAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(), DiffUtilDispatcherDelegate<AddressBookItem> {
    private var mItems: MutableList<AddressBookItem> = ArrayList()
    private var mInflater: LayoutInflater? = null
    private var mOnEditContactListener: ((AddressContact) -> Unit)? = null
    private var mOnDeleteContactListener: ((AddressContact) -> Unit)? = null
    private var mOnItemClickListener: ((AddressContact) -> Unit)? = null

    fun setOnItemClickListener(listener: (AddressContact) -> Unit) {
        mOnItemClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (mInflater == null) {
            mInflater = LayoutInflater.from(parent.context)
        }
        val view: View
        return if (viewType == AddressBookItem.TYPE_HEADER) {
            view = mInflater!!.inflate(R.layout.item_list_header, parent, false)
            HeaderViewHolder(view)
        } else {
            view = mInflater!!.inflate(R.layout.item_list_address_book, parent, false)
            ItemViewHolder(view)
        }
    }

    fun setOnEditContactListener(listener: (AddressContact) -> Unit) {
        mOnEditContactListener = listener
    }

    fun setOnDeleteContactListener(listener: (AddressContact) -> Unit) {
        mOnDeleteContactListener = listener
    }

    override fun getItemViewType(position: Int): Int {
        return mItems[position].viewType
    }

    private var mLastOpened: Int = -1

    fun closeOpened() {
        if (mLastOpened >= 0) {
            notifyItemChanged(mLastOpened)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is HeaderViewHolder) {
            val item = mItems[position] as AddressBookItemHeader
            holder.title!!.text = item.header
        } else {
            val vh = holder as ItemViewHolder
            val item = mItems[position] as AddressContact

            (vh.itemView as SwipeLayout).setOnActionsListener(object : SwipeLayout.SwipeActionsListener {
                override fun onOpen(direction: Int, isContinuous: Boolean) {
                    closeOpened()
                    mLastOpened = vh.adapterPosition
                }

                override fun onClose() {}
            })
            if (vh.adapterPosition == mLastOpened) {
                vh.itemView.post {
                    mLastOpened = -1
                    vh.itemView.close(true)
                }
            }


            vh.mainView!!.setOnClickListener {
                mOnItemClickListener?.invoke(mItems[holder.getAdapterPosition()] as AddressContact)
            }
            item.applyAddressIcon(vh.avatar!!)
            vh.title!!.text = item.name
            vh.subtitle!!.text = item.address
            vh.actionEdit!!.setOnClickListener {
                closeOpened()
                mOnEditContactListener?.invoke(mItems[holder.getAdapterPosition()] as AddressContact)
            }
            vh.actionDelete!!.setOnClickListener {
                closeOpened()
                mOnDeleteContactListener?.invoke(mItems[holder.getAdapterPosition()] as AddressContact)
            }
        }
    }

    override fun getItemCount(): Int {
        return mItems.size
    }

    override fun <T : DiffUtil.Callback?> dispatchChanges(diffUtilCallbackCls: Class<T>, items: MutableList<AddressBookItem>, detectMoves: Boolean) {
        DiffUtilDispatcher.dispatchChanges(this, diffUtilCallbackCls, items, detectMoves)
    }

    override fun <T : DiffUtil.Callback?> dispatchChanges(diffUtilCallbackCls: Class<T>, items: MutableList<AddressBookItem>) {
        DiffUtilDispatcher.dispatchChanges(this, diffUtilCallbackCls, items)
    }

    override fun setItems(items: MutableList<AddressBookItem>) {
        mItems = items
    }

    override fun getItems(): List<AddressBookItem> {
        return mItems
    }

    override fun clear() {
        mItems.clear()
        notifyDataSetChanged()
    }

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        @JvmField @BindView(R.id.title)
        var title: TextView? = null

        init {
            ButterKnife.bind(this, itemView)
        }
    }

    class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        @JvmField @BindView(R.id.avatar)
        var avatar: BipCircleImageView? = null

        @JvmField @BindView(R.id.title)
        var title: TextView? = null

        @JvmField @BindView(R.id.subtitle)
        var subtitle: TextView? = null

        @JvmField @BindView(R.id.swipe_actions)
        var swipeActions: View? = null

        @JvmField @BindView(R.id.action_delete)
        var actionDelete: View? = null

        @JvmField @BindView(R.id.action_edit)
        var actionEdit: View? = null

        @JvmField @BindView(R.id.main)
        var mainView: View? = null

        init {
            ButterKnife.bind(this, itemView)
        }
    }
}