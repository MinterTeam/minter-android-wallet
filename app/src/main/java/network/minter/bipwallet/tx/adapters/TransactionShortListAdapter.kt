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
package network.minter.bipwallet.tx.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.annimon.stream.Stream
import network.minter.bipwallet.internal.helpers.ViewExtensions.visible
import network.minter.bipwallet.internal.views.list.diff.DiffUtilDispatcher
import network.minter.bipwallet.internal.views.list.diff.DiffUtilDispatcherDelegate
import network.minter.bipwallet.tx.adapters.TxItem
import network.minter.bipwallet.tx.adapters.TxItem.Companion.bindViewHolder
import network.minter.bipwallet.tx.adapters.TxItem.Companion.createViewHolder
import network.minter.bipwallet.tx.adapters.vh.TxAllViewHolder
import network.minter.core.crypto.MinterAddress
import java.util.*

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock></edward.vstock>@gmail.com>
 */
class TransactionShortListAdapter(
        private val myAddress: () -> MinterAddress
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), DiffUtilDispatcherDelegate<TransactionFacade> {

    private var mItems: MutableList<TxItem> = ArrayList(0)
    private var mInflater: LayoutInflater? = null
    private var mOnExpandDetailsListener: ((View, TransactionFacade) -> Unit)? = null
    fun setOnExpandDetailsListener(listener: (View, TransactionFacade) -> Unit) {
        mOnExpandDetailsListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (mInflater == null) {
            mInflater = LayoutInflater.from(parent.context)
        }
        return createViewHolder(mInflater!!, parent, viewType)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        bindViewHolder(myAddress, holder, mItems[position])

        (holder as TxAllViewHolder).let {
            it.itemView.setOnClickListener { v ->
                mOnExpandDetailsListener?.invoke(v, getItem(holder.bindingAdapterPosition).tx)
            }
            it.binding.separator.visible = true
        }
    }

    fun getItem(position: Int): TxItem {
        return mItems[position]
    }

    override fun getItemViewType(position: Int): Int {
        return mItems[position].viewType
    }

    override fun getItemCount(): Int {
        return mItems.size
    }

    override fun <T : DiffUtil.Callback> dispatchChanges(diffUtilCallbackCls: Class<T>, items: List<TransactionFacade>, detectMoves: Boolean) {
        DiffUtilDispatcher.dispatchChanges(this, diffUtilCallbackCls, items, detectMoves)
    }

    override fun <T : DiffUtil.Callback> dispatchChanges(diffUtilCallbackCls: Class<T>, items: List<TransactionFacade>) {
        DiffUtilDispatcher.dispatchChanges(this, diffUtilCallbackCls, items)
    }

    override fun getItems(): List<TransactionFacade> {
        return Stream.of(mItems)
                .map(TxItem::tx)
                .toList()
    }

    override fun setItems(items: List<TransactionFacade>) {
        mItems = Stream.of(items)
                .map { TxItem(it) }
                .toList()
    }

    override fun clear() {
        mItems.clear()
        notifyDataSetChanged()
    }

}