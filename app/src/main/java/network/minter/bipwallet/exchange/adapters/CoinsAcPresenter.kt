/*
 * Copyright (C) by MinterTeam. 2021
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

import android.content.Context
import android.text.Spannable
import androidx.recyclerview.widget.RecyclerView
import com.otaliastudios.autocomplete.AutocompletePolicy
import io.reactivex.schedulers.Schedulers
import network.minter.bipwallet.coins.RepoCoins
import network.minter.bipwallet.internal.Wallet
import network.minter.bipwallet.internal.autocomplete.RecyclerAcPresenter
import network.minter.explorer.models.CoinItem
import timber.log.Timber
import java.util.*
import kotlin.collections.ArrayList


/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
class CoinsAcPresenter(
        context: Context,
        coinsRepo: RepoCoins,
        val listFilter: (CoinItem) -> Boolean = { _ -> true }
) : RecyclerAcPresenter<CoinItem>(context), AutocompletePolicy {
    private val adapter: CoinsAcAdapter = CoinsAcAdapter(this)
    private var items: List<CoinItem> = ArrayList()
    private val itemsLock: Any = Any()

    init {
        coinsRepo.observe()
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .subscribe(
                        { res ->
                            synchronized(itemsLock) {
                                items = res.filter(listFilter)
                            }
                        },
                        { t ->
                            Timber.w(t, "Unable to load coins list")
                        }
                )
        coinsRepo.update()
    }

    override fun instantiateAdapter(): RecyclerView.Adapter<*> {
        return adapter
    }

    override fun onQuery(query: CharSequence?): Boolean {
        if (query.isNullOrEmpty()) {
            adapter.setItems(ArrayList(0))
            return false
        }

        val filtered: List<CoinItem>
        synchronized(itemsLock) {
            filtered = items
                    .filter(listFilter)
                    .filter {
                        it.symbol.lowercase(Wallet.LC_EN).startsWith(query.toString().lowercase(Locale.getDefault()))
                    }
        }

        adapter.setItems(filtered)
        return filtered.isNotEmpty()
    }

    override fun getPopupDimensions(): PopupDimensions {
        val dims = PopupDimensions()
        dims.calculateHeight = false
        return dims
    }

    override fun shouldShowPopup(text: Spannable, cursorPos: Int): Boolean {
        return text.length > 0
    }

    override fun shouldDismissPopup(text: Spannable, cursorPos: Int): Boolean {
        return text.length == 0 || (adapter.itemCount == 0)
    }

    override fun getQuery(text: Spannable): CharSequence {
        return text
    }

    override fun onDismiss(text: Spannable) {
    }
}
