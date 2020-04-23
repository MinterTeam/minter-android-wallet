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
package network.minter.bipwallet.wallets.contract

import android.view.View
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import moxy.MvpView
import moxy.viewstate.strategy.AddToEndSingleStrategy
import moxy.viewstate.strategy.StateStrategyType

@StateStrategyType(AddToEndSingleStrategy::class)
interface BaseWalletsPageView : MvpView {
    enum class ViewStatus {
        Normal, Progress, Empty, Error
    }

    fun setViewStatus(status: ViewStatus)
    fun setViewStatus(status: ViewStatus, error: CharSequence?)
    fun setListTitle(title: CharSequence)
    fun setListTitle(@StringRes title: Int)
    fun setAdapter(adapter: RecyclerView.Adapter<*>)
    fun setActionTitle(title: CharSequence)
    fun setActionTitle(@StringRes title: Int)
    fun setOnActionClickListener(listener: View.OnClickListener)
    fun showProgress(show: Boolean)
    fun setEmptyTitle(title: CharSequence)
    fun setEmptyTitle(@StringRes title: Int)
    fun showEmpty(show: Boolean)
}