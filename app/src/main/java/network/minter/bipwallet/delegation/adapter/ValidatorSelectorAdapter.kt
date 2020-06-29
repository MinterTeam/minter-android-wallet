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

package network.minter.bipwallet.delegation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import network.minter.bipwallet.R
import network.minter.bipwallet.apis.reactive.avatar
import network.minter.bipwallet.databinding.ItemListHeaderBinding
import network.minter.bipwallet.databinding.ItemListValidatorSelectorBinding
import network.minter.explorer.models.ValidatorItem
import network.minter.explorer.models.ValidatorMeta

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
private const val ITEM_HEADER = 0
private const val ITEM_VALUE = 1

private interface Item {
    val viewType: Int
}

private class ItemHeader(val title: CharSequence) : Item {
    override val viewType: Int
        get() = ITEM_HEADER
}

private class ItemValue(val item: ValidatorItem, val lastUsed: Boolean = false) : Item {
    override val viewType: Int
        get() = ITEM_VALUE
}

class HeaderViewHolder(
        val binding: ItemListHeaderBinding
) : RecyclerView.ViewHolder(binding.root)

class ItemViewHolder(
        val binding: ItemListValidatorSelectorBinding
) : RecyclerView.ViewHolder(binding.root)


class ValidatorSelectorAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var inflater: LayoutInflater? = null
    private var items: MutableList<Item> = ArrayList()
    private var onClickListener: ((ValidatorItem) -> Unit)? = null

    fun setOnItemClickListener(listener: (ValidatorItem) -> Unit) {
        onClickListener = listener
    }

    fun setItems(lastUsed: List<ValidatorItem>, validators: List<ValidatorItem>) {
        val data = ArrayList<Item>(/*maximum capacity*/validators.size + lastUsed.size + 2)
        if (lastUsed.isNotEmpty()) {
            data.add(ItemHeader("Last Used"))
            data.addAll(lastUsed.map { ItemValue(it, true) })
            data.add(ItemHeader("All Validators"))
        }

        data.addAll(validators.map { ItemValue(it) })
        items = data
    }

    override fun getItemViewType(position: Int): Int {
        return items[position].viewType
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (inflater == null) {
            inflater = LayoutInflater.from(parent.context)
        }

        return when (viewType) {
            ITEM_HEADER -> {
                HeaderViewHolder(
                        ItemListHeaderBinding.inflate(inflater!!, parent, false)
                )
            }
            ITEM_VALUE -> {
                ItemViewHolder(
                        ItemListValidatorSelectorBinding.inflate(inflater!!, parent, false)
                )
            }
            else -> {
                throw RuntimeException("Unknown view type")
            }
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is HeaderViewHolder) {
            val item = items[position] as ItemHeader
            holder.binding.title.text = item.title
        } else if (holder is ItemViewHolder) {
            val item = items[position] as ItemValue
            val validator = item.item
            val meta: ValidatorMeta? = validator.meta

            if (meta?.name.isNullOrEmpty()) {
                holder.binding.itemTitle.text = validator.pubKey.toShortString()
            } else {
                holder.binding.itemTitle.text = meta!!.name
            }
            holder.binding.itemSubtitle.text = validator.pubKey.toString()


            holder.binding.itemAvatar.setImageUrlFallback(validator.pubKey.avatar, R.drawable.img_avatar_delegate)

            holder.itemView.setOnClickListener {
                onClickListener?.invoke(
                        (items[holder.bindingAdapterPosition] as ItemValue).item
                )
            }
        }
    }
}