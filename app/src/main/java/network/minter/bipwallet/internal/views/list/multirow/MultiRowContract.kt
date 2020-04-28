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
package network.minter.bipwallet.internal.views.list.multirow

import androidx.annotation.LayoutRes
import network.minter.bipwallet.internal.views.list.multirow.MultiRowAdapter.RowViewHolder

/**
 * Mds. 2017
 *
 * @author Eduard Maximovich <edward.vstock></edward.vstock>@gmail.com>
 */
interface MultiRowContract {
    interface Row<T : RowViewHolder> {
        /**
         * views ID
         *
         * @return int
         * @see MultiRowAdapter.makeHoldersCache
         */
        @LayoutRes
        fun getItemView(): Int

        /**
         * views position index
         *
         * @return int
         * @see MultiRowAdapter
         *
         * @see MultiRowContract.Row
         */
        fun getRowPosition(): Int {
            return 0
        }

        /**
         * If view should be visible
         *
         * @return bool
         */
        fun isVisible(): Boolean {
            return true
        }

        /**
         * Вызывается когда адаптер биндит вьюху,
         * соответственно в этом методе заполняем RowViewHolder
         * @param viewHolder Row view holder
         * @see MultiRowAdapter.makeHoldersCache
         */
        fun onBindViewHolder(viewHolder: T)

        /**
         * Вызывается когда холдер отцепляется от окна
         *
         * @param viewHolder
         * @see RecyclerView.onDetachedFromWindow
         */
        fun onUnbindViewHolder(viewHolder: T)

        /**
         * Класс RowViewHolder'а который отражает вьюху
         * @return Class
         * @see MultiRowAdapter.RowViewHolder
         */
        fun getViewHolderClass(): Class<T>

        companion object {
            const val POSITION_FIRST = -1
            const val POSITION_LAST = 999
            const val POSITION_ORDERED = 0
        }
    }
}