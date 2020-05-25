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

import android.content.Context
import android.view.View
import network.minter.bipwallet.R
import network.minter.bipwallet.databinding.ItemListDialogAccountSelectorSearchViewBinding
import network.minter.bipwallet.internal.adapter.AutocompleteListAdapter
import network.minter.bipwallet.internal.helpers.ViewExtensions.visible
import network.minter.explorer.models.ValidatorItem

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
class ValidatorsAcAdapter(
        context: Context
) : AutocompleteListAdapter<ValidatorItem, ValidatorsAcAdapter.ViewHolder>(context) {

    class ViewHolder(itemView: View) : AutocompleteListAdapter.ViewHolder(itemView) {
        val b = ItemListDialogAccountSelectorSearchViewBinding.bind(itemView)
    }

    override val layoutItemId: Int
        get() = R.layout.item_list_dialog_account_selector_search_view

    override fun viewHolderClass(): Class<ViewHolder> {
        return ViewHolder::class.java
    }

    override fun onBindViewHolder(item: ValidatorItem, holder: ViewHolder, position: Int) {
        super.onBindViewHolder(item, holder, position)
        val name = item.meta?.name ?: item.pubKey.toShortString()
        val subtitle = if (item.meta?.name.isNullOrEmpty()) null else item.pubKey.toShortString()

        holder.b.searchItemTitle.text = name
        holder.b.searchItemIconLeft.setImageUrlFallback(item.meta?.iconUrl, R.drawable.img_avatar_delegate)
        holder.b.searchItemSubtitle.visible = subtitle != null
        holder.b.searchItemSubtitle.text = subtitle
    }

    override fun isItemMatchesConstraint(item: ValidatorItem, constraint: CharSequence?): Boolean {
        if (constraint == null) return false

        if (item.meta == null || item.meta.name.isNullOrEmpty()) {
            return item.pubKey.toString().toLowerCase().startsWith(constraint.toString().toLowerCase())
        }

        return item.meta.name.toLowerCase().startsWith(constraint.toString().toLowerCase())
    }

    override fun resultToString(item: ValidatorItem): String {
        return item.meta?.name ?: item.pubKey.toShortString()
    }
}