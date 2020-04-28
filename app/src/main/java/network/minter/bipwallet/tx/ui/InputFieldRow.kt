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

package network.minter.bipwallet.tx.ui

import android.view.View
import network.minter.bipwallet.R
import network.minter.bipwallet.databinding.RowInputFieldBinding
import network.minter.bipwallet.internal.views.list.multirow.MultiRowAdapter
import network.minter.bipwallet.internal.views.list.multirow.MultiRowContract

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
class InputFieldRow internal constructor(
        val builder: Builder
) : MultiRowContract.Row<InputFieldRow.ViewHolder> {

    class ViewHolder(itemView: View) : MultiRowAdapter.RowViewHolder(itemView) {
        val view = RowInputFieldBinding.bind(itemView)
    }


    override fun getItemView(): Int {
        return R.layout.row_input_field
    }

    override fun onBindViewHolder(viewHolder: ViewHolder) {
        viewHolder.view.rowInput.apply {
            input.isFocusable = false
            label = builder.label
            hint = builder.hint
            setText(builder.text)
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {

    }

    override fun getViewHolderClass(): Class<ViewHolder> {
        return ViewHolder::class.java
    }

    class Builder {
        var label: CharSequence? = null
        var hint: CharSequence? = null
        var text: CharSequence? = null

        fun build() = InputFieldRow(this)
    }

    class MultiBuilder {
        var builders = ArrayList<InputFieldRow>()

        fun add(applier: Builder.() -> Unit): MultiBuilder {
            builders.add(Builder().apply(applier).build())
            return this
        }

        fun build(): MutableList<InputFieldRow> = builders

    }
}