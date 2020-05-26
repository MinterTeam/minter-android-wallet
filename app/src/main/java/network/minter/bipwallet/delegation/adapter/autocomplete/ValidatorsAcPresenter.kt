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

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import network.minter.bipwallet.internal.autocomplete.RecyclerAcPresenter
import network.minter.explorer.models.ValidatorItem


/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
class ValidatorsAcPresenter(
        context: Context
) : RecyclerAcPresenter<ValidatorItem>(context) {

    private var items: List<ValidatorItem> = ArrayList()
    private val itemsLock = Any()
    private val adapter: ValidatorsAcAdapter = ValidatorsAcAdapter(this)

    fun setItems(items: List<ValidatorItem>) {
        synchronized(itemsLock) {
            this.items = items
        }
    }

    override fun instantiateAdapter(): RecyclerView.Adapter<*> {
        return adapter
    }

    override fun onQuery(query: CharSequence?) {
        if (query.isNullOrEmpty()) {
            adapter.setItems(ArrayList(0))
            return
        }

        val filtered = ArrayList<ValidatorItem>()

        for (item in items) {
            if (item.meta != null && item.meta?.name != null) {
                if (item.meta!!.name!!.toLowerCase().startsWith(query.toString().toLowerCase())) {
                    filtered.add(item)
                }
            }

            if (item.pubKey.toString().toLowerCase().startsWith(query.toString().toLowerCase())) {
                filtered.add(item)
            }
        }
        adapter.setItems(filtered)

    }

    override fun getPopupDimensions(): PopupDimensions {
        val dims = PopupDimensions()
        dims.width = ViewGroup.LayoutParams.WRAP_CONTENT
        dims.height = ViewGroup.LayoutParams.WRAP_CONTENT
        return dims
    }
}