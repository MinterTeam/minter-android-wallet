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
import androidx.lifecycle.MutableLiveData
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import network.minter.bipwallet.internal.adapter.LoadState
import network.minter.bipwallet.tx.adapters.TxItem.Companion.bindViewHolder
import network.minter.bipwallet.tx.adapters.TxItem.Companion.createViewHolder
import network.minter.bipwallet.tx.adapters.vh.TxAllViewHolder
import network.minter.core.crypto.MinterAddress

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock></edward.vstock>@gmail.com>
 */
class TransactionListAdapter : PagedListAdapter<TransactionItem, RecyclerView.ViewHolder> {
    private var myAddress: (() -> MinterAddress)? = null
    private var mInflater: LayoutInflater? = null
    private var mLoadState: MutableLiveData<LoadState>? = null
    private var mOnExpandDetailsListener: ((View, TransactionFacade) -> Unit)? = null

    constructor(addressCallback: () -> MinterAddress) : super(sDiffCallback) {
        myAddress = addressCallback
    }

    protected constructor(config: AsyncDifferConfig<TransactionItem?>) : super(config)

    override fun onCreateViewHolder(parent: ViewGroup, @TransactionItem.ListType viewType: Int): RecyclerView.ViewHolder {
        if (mInflater == null) {
            mInflater = LayoutInflater.from(parent.context)
        }
        return createViewHolder(mInflater!!, parent, viewType)
    }

    override fun getItemViewType(position: Int): Int {
        return if (hasProgressRow() && position == itemCount - 1) {
            TransactionItem.ITEM_PROGRESS
        } else getItem(position)!!.viewType
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        bindViewHolder(myAddress!!, holder, getItem(position)!!)
        if (holder is TxAllViewHolder) {
            holder.itemView.setOnClickListener { v ->
                mOnExpandDetailsListener?.invoke(v, (getItem(holder.getAdapterPosition()) as TxItem).tx)
            }
        }

    }

    fun setLoadState(loadState: MutableLiveData<LoadState>?) {
        mLoadState = loadState
    }

    fun setOnExpandDetailsListener(listener: (View, TransactionFacade) -> Unit) {
        mOnExpandDetailsListener = listener
    }

    private fun hasProgressRow(): Boolean {
        return mLoadState != null && mLoadState!!.value != LoadState.Loaded
    }

    companion object {
        private val sDiffCallback: DiffUtil.ItemCallback<TransactionItem> = object : DiffUtil.ItemCallback<TransactionItem>() {
            override fun areItemsTheSame(oldItem: TransactionItem, newItem: TransactionItem): Boolean {
                return oldItem.isSameOf(newItem)
            }

            override fun areContentsTheSame(oldItem: TransactionItem, newItem: TransactionItem): Boolean {
                return oldItem == newItem
            }
        }
    }
}