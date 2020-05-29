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

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import network.minter.bipwallet.R
import network.minter.bipwallet.databinding.ItemListTxBinding
import network.minter.bipwallet.tx.adapters.vh.TxAllViewHolder
import network.minter.bipwallet.tx.adapters.vh.TxHeaderViewHolder
import network.minter.bipwallet.tx.adapters.vh.TxProgressViewHolder
import network.minter.core.crypto.MinterAddress

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock></edward.vstock>@gmail.com>
 */
class TxItem(val tx: TransactionFacade) : TransactionItem {
//    val avatar: String
//        get() = tx.toAvatar

//    fun setMeta(meta: UserMeta?) {
//        tx.userMeta = meta
//    }
//
//    fun setMeta(url: String?) {
//        if (tx.userMeta != null) {
//            tx.userMeta.avatarUrl = url
//        } else {
//            tx.userMeta = UserMeta()
//            tx.userMeta.avatarUrl = url
//        }
//    }

//    val username: String
//        get() = tx.name

    @SuppressLint("WrongConstant")
    override fun getViewType(): Int {
        return if (tx.get().type != null) tx.get().type.ordinal + 1 else 0xFF
    }

    override fun equals(other: Any?): Boolean {
        return if (other is TxItem) {
            tx == other.tx
        } else false
    }

    override fun isSameOf(item: TransactionItem): Boolean {
        return if (item.viewType != TransactionItem.ITEM_TX) {
            false
        } else (item as TxItem).tx.get().hash == tx.get().hash
    }

    companion object {
        @JvmStatic
        fun createViewHolder(inflater: LayoutInflater, parent: ViewGroup?, @TransactionItem.ListType viewType: Int): RecyclerView.ViewHolder {
            val view: View
            val out: RecyclerView.ViewHolder
            when (viewType) {
                TransactionItem.ITEM_HEADER -> {
                    view = inflater.inflate(R.layout.item_list_transaction_header, parent, false)
                    out = TxHeaderViewHolder(view)
                }
                TransactionItem.ITEM_PROGRESS -> {
                    view = inflater.inflate(R.layout.item_list_transaction_progress, parent, false)
                    out = TxProgressViewHolder(view)
                }
                TransactionItem.ITEM_TX -> {
                    val b = ItemListTxBinding.inflate(inflater, parent, false)
                    out = TxAllViewHolder(b)
                }
                else -> {
                    val b = ItemListTxBinding.inflate(inflater, parent, false)
                    out = TxAllViewHolder(b)
                }
            }
            return out
        }

        @JvmStatic
        fun bindViewHolder(myAddress: () -> MinterAddress, holder: RecyclerView.ViewHolder?, data: TransactionItem) {
            if (holder is TxHeaderViewHolder) {
                val item = data as HeaderItem
                holder.header.text = item.header
            } else if (holder is TxAllViewHolder) {
                val item = data as TxItem
                holder.bind(item, myAddress)
            }
        }
    }

}