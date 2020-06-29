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

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import network.minter.bipwallet.R
import network.minter.bipwallet.databinding.ItemListValidatorSelectorBinding
import network.minter.bipwallet.internal.helpers.ViewExtensions.listItemBackgroundRippleRounded
import network.minter.explorer.models.CoinItem
import network.minter.profile.MinterProfileApi
import timber.log.Timber

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
class CoinsAcAdapter(
        val presenter: CoinsAcPresenter
) : RecyclerView.Adapter<CoinsAcAdapter.ViewHolder>() {
    private var items: List<CoinItem> = ArrayList()
    private var inflater: LayoutInflater? = null

    class ViewHolder(
            val b: ItemListValidatorSelectorBinding
    ) : RecyclerView.ViewHolder(b.root)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        listItemBackgroundRippleRounded(holder.itemView, position, items.size)
        holder.b.root.setOnClickListener {
            presenter.dispatchClick(items[holder.bindingAdapterPosition])
        }

        holder.b.itemTitle.text = item.symbol
        holder.b.itemSubtitle.text = item.name
        holder.b.itemAvatar.setImageUrlFallback(MinterProfileApi.getCoinAvatarUrl(item.symbol), R.drawable.img_avatar_default)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        if (inflater == null) {
            inflater = LayoutInflater.from(parent.context)
        }

        val b = ItemListValidatorSelectorBinding.inflate(inflater!!, parent, false)
        return ViewHolder(b)
    }

    override fun getItemCount(): Int {
        Timber.d("Coins adapter check items count: %d", items.size)
        return items.size
    }

    fun setItems(data: List<CoinItem>) {
        if (items.size == data.size && items == data) {
            return
        }
        items = data
        notifyDataSetChanged()
    }
}