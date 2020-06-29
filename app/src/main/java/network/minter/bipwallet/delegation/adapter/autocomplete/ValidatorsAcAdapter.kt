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

package network.minter.bipwallet.delegation.adapter.autocomplete

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import network.minter.bipwallet.R
import network.minter.bipwallet.databinding.ItemListDialogAccountSelectorSearchViewBinding
import network.minter.bipwallet.internal.helpers.ViewExtensions.listItemBackgroundRippleRounded
import network.minter.bipwallet.internal.helpers.ViewExtensions.visible
import network.minter.explorer.models.ValidatorItem

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
class ValidatorsAcAdapter(
        val presenter: ValidatorsAcPresenter,
        private var items: List<ValidatorItem> = ArrayList()
) : RecyclerView.Adapter<ValidatorsAcAdapter.ViewHolder>() {

    private var inflater: LayoutInflater? = null

    class ViewHolder(
            val b: ItemListDialogAccountSelectorSearchViewBinding
    ) : RecyclerView.ViewHolder(b.root)


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val name = item.meta?.name ?: item.pubKey.toShortString()
        val subtitle = if (item.meta?.name.isNullOrEmpty()) null else item.pubKey.toShortString()

        listItemBackgroundRippleRounded(holder.itemView, position, items.size)

        holder.b.separator.visible = items.isNotEmpty() && position < items.size - 1

        holder.b.root.setOnClickListener {
            presenter.dispatchClick(items[holder.bindingAdapterPosition])
        }
        holder.b.searchItemTitle.text = name
        holder.b.searchItemIconLeft.setImageUrlFallback(item.meta?.iconUrl, R.drawable.img_avatar_delegate)
        holder.b.searchItemSubtitle.visible = subtitle != null
        holder.b.searchItemSubtitle.text = subtitle
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        if (inflater == null) {
            inflater = LayoutInflater.from(parent.context)
        }

        val b = ItemListDialogAccountSelectorSearchViewBinding.inflate(inflater!!, parent, false)
        return ViewHolder(b)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun setItems(data: List<ValidatorItem>) {
        if (items.size == data.size && items == data) {
            return
        }
        items = data
        notifyDataSetChanged()
    }
}