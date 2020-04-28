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

import network.minter.bipwallet.internal.views.list.multirow.MultiRowAdapter.RowViewHolder

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
class SortableRow<V : RowViewHolder, T : MultiRowContract.Row<V>>(
        val row: T, position: Int
) : MultiRowContract.Row<V> {

    private var position = 0
    var previousPosition = -1
        private set

    fun setPosition(position: Int) {
        previousPosition = this.position
        this.position = position
    }

    val isInserted: Boolean
        get() = previousPosition == -1

    override fun getItemView(): Int {
        return row.getItemView()
    }

    override fun getRowPosition(): Int {
        return position
    }

    override fun isVisible(): Boolean {
        return row.isVisible()
    }

    override fun onBindViewHolder(viewHolder: V) {
        row.onBindViewHolder(viewHolder)
    }

    override fun onUnbindViewHolder(viewHolder: V) {}
    override fun getViewHolderClass(): Class<V> {
        return row.getViewHolderClass()
    }

    override fun hashCode(): Int {
        return row.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return if (other is MultiRowContract.Row<*>) {
            other.getRowPosition() == getRowPosition() && other.getItemView() == getItemView() && other.isVisible() == isVisible() && other.getViewHolderClass() == getViewHolderClass()
        } else row == other
    }

    init {
        this.position = position
    }
}