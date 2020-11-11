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
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.zerobranch.layout.SwipeLayout
import network.minter.bipwallet.R
import network.minter.bipwallet.addressbook.models.AddressBookItem
import network.minter.bipwallet.addressbook.models.AddressBookItemHeader
import network.minter.bipwallet.addressbook.models.AddressContact
import network.minter.bipwallet.apis.reactive.avatar
import network.minter.bipwallet.databinding.ItemListAddressBookBinding
import network.minter.bipwallet.databinding.ItemListHeaderBinding
import network.minter.bipwallet.internal.helpers.ViewExtensions.visible
import network.minter.bipwallet.internal.views.list.diff.DiffUtilDispatcher
import network.minter.bipwallet.internal.views.list.diff.DiffUtilDispatcherDelegate
import java.util.*

class AddressBookAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(), DiffUtilDispatcherDelegate<AddressBookItem> {
    private var mItems: MutableList<AddressBookItem> = ArrayList()
    private var inflater: LayoutInflater? = null
    private var mOnEditContactListener: ((AddressContact) -> Unit)? = null
    private var mOnDeleteContactListener: ((AddressContact) -> Unit)? = null
    private var mOnItemClickListener: ((AddressContact) -> Unit)? = null
    private var lastOpened: Int = -1
    private var lastHeaderPos: Int = -1

    fun setOnItemClickListener(listener: (AddressContact) -> Unit) {
        mOnItemClickListener = listener
        registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()
                lastHeaderPos = -1
            }

            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                super.onItemRangeRemoved(positionStart, itemCount)
                lastHeaderPos = -1
            }

            override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
                super.onItemRangeMoved(fromPosition, toPosition, itemCount)
                lastHeaderPos = -1
            }

            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                lastHeaderPos = -1
            }

            override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
                super.onItemRangeChanged(positionStart, itemCount)
                lastHeaderPos = -1
            }

            override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
                super.onItemRangeChanged(positionStart, itemCount, payload)
                lastHeaderPos = -1
            }
        })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (inflater == null) {
            inflater = LayoutInflater.from(parent.context)
        }

        return if (viewType == AddressBookItem.TYPE_HEADER) {
            HeaderViewHolder(ItemListHeaderBinding.inflate(inflater!!, parent, false))
        } else {
            ItemViewHolder(ItemListAddressBookBinding.inflate(inflater!!, parent, false))
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


    fun closeOpened() {
        if (lastOpened >= 0) {
            notifyItemChanged(lastOpened)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is HeaderViewHolder) {
            val item = mItems[position] as AddressBookItemHeader
            holder.b.title.text = item.header
            lastHeaderPos = position
        } else {
            val vh = holder as ItemViewHolder
            vh.b.separator.visible = true
            val item = mItems[position] as AddressContact

            val enableSwipe = !item.isLastUsed

            vh.b.root.isEnabledSwipe = enableSwipe
            vh.b.root.setOnActionsListener(object : SwipeLayout.SwipeActionsListener {
                override fun onOpen(direction: Int, isContinuous: Boolean) {
                    closeOpened()
                    lastOpened = vh.absoluteAdapterPosition
                }

                override fun onClose() {}
            })
            if (vh.absoluteAdapterPosition == lastOpened) {
                vh.itemView.post {
                    lastOpened = -1
                    vh.b.root.close(true)
                }
            }

            vh.b.main.setOnClickListener {
                mOnItemClickListener?.invoke(mItems[holder.bindingAdapterPosition] as AddressContact)
            }
            item.applyAddressIcon(vh.b.avatar)
            vh.b.avatar.setImageUrlFallback(item.minterAddress.avatar, R.drawable.img_avatar_default)
            vh.b.title.text = item.name
            vh.b.subtitle.text = item.address
            vh.b.actionEdit.setOnClickListener {
                closeOpened()
                mOnEditContactListener?.invoke(mItems[holder.bindingAdapterPosition] as AddressContact)
            }
            vh.b.actionDelete.setOnClickListener {
                closeOpened()
                mOnDeleteContactListener?.invoke(mItems[holder.bindingAdapterPosition] as AddressContact)
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

    class HeaderViewHolder(val b: ItemListHeaderBinding) : RecyclerView.ViewHolder(b.root)

    class ItemViewHolder(val b: ItemListAddressBookBinding) : RecyclerView.ViewHolder(b.root)
}