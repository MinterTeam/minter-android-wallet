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
package network.minter.bipwallet.sending.account

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import network.minter.bipwallet.databinding.ItemListDialogAccountSelectorBinding
import network.minter.bipwallet.internal.helpers.ViewExtensions.visible
import network.minter.bipwallet.sending.account.AccountSelectedAdapter.ViewHolder

/**
 * minter-android-wallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock></edward.vstock>@gmail.com>
 */
class AccountSelectedAdapter<Data>
constructor(
        private val mItems: List<SelectorData<Data>>
) : RecyclerView.Adapter<ViewHolder<Data>>() {

    private var mInflater: LayoutInflater? = null
    private var mOnClickListener: OnClickListener<Data>? = null

    fun setOnClickListener(listener: (SelectorData<Data>) -> Unit) {
        setOnClickListener(object : OnClickListener<Data> {
            override fun onClick(item: SelectorData<Data>) {
                listener(item)
            }
        })
    }

    fun setOnClickListener(listener: OnClickListener<Data>?) {
        mOnClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder<Data> {
        if (mInflater == null) {
            mInflater = LayoutInflater.from(parent.context)
        }
        val view = ItemListDialogAccountSelectorBinding.inflate(mInflater!!, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder<Data>, position: Int) {
        val item = mItems[position]
        holder.bind(item)
        holder.binding.root.setOnClickListener {
            mOnClickListener?.onClick(item)
        }
    }

    override fun getItemCount(): Int {
        return mItems.size
    }

    interface OnClickListener<Data> {
        fun onClick(item: SelectorData<Data>)
    }

    class ViewHolder<Data>(val binding: ItemListDialogAccountSelectorBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: SelectorData<Data>) {
            binding.apply {
                itemAvatar.setImageUrlFallback(item.avatar, item.avatarFallback)
                itemTitle.text = item.title
                itemSubtitle.visible = !item.subtitle.isNullOrEmpty()
                itemSubtitle.text = item.subtitle
            }
        }
    }

}