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

package network.minter.bipwallet.wallets.views.rows

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import network.minter.bipwallet.R
import network.minter.bipwallet.databinding.RowWalletsListBinding
import network.minter.bipwallet.internal.views.list.BorderedItemSeparator
import network.minter.bipwallet.internal.views.list.NonScrollableLinearLayoutManager
import network.minter.bipwallet.internal.views.list.multirow.MultiRowAdapter
import network.minter.bipwallet.internal.views.list.multirow.MultiRowContract

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
class RowWalletsList(
        private val adapter: RecyclerView.Adapter<*>,
        private val listConfig: ((RecyclerView) -> Unit)? = null
) : MultiRowContract.Row<RowWalletsList.ViewHolder> {

    private var itemSeparator: BorderedItemSeparator? = null

    class ViewHolder(v: View) : MultiRowAdapter.RowViewHolder(v)

    override fun getItemView(): Int {
        return R.layout.row_wallets_list
    }

    override fun onBindViewHolder(viewHolder: ViewHolder) {
        val b = RowWalletsListBinding.bind(viewHolder.itemView)
        listConfig?.invoke(b.list)
        b.list.layoutManager = NonScrollableLinearLayoutManager(b.root.context)
        b.list.isNestedScrollingEnabled = false
        b.list.adapter = adapter
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
    }

    override fun getViewHolderClass(): Class<ViewHolder> {
        return ViewHolder::class.java
    }
}