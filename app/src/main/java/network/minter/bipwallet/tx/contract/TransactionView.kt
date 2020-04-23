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

package network.minter.bipwallet.tx.contract

import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import moxy.MvpView
import moxy.viewstate.strategy.AddToEndSingleStrategy
import moxy.viewstate.strategy.StateStrategyType

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
@StateStrategyType(AddToEndSingleStrategy::class)
interface TransactionView : MvpView {
    fun setTitle(@StringRes resId: Int)
    fun setTitle(title: CharSequence)
    fun inflateDetails(@LayoutRes layoutRes: Int, l: (View) -> Unit)
    fun setFromAddress(address: String?)
    fun setFromName(name: String?)
    fun setFromAvatar(fromAvatar: String)
    fun setFromAvatar(@DrawableRes resId: Int)
    fun setToAddress(recipient: String?)
    fun setToName(name: String?)
    fun setToAvatar(toAvatar: String?)
    fun setToAvatar(toAvatar: String?, fallback: Int)
    fun setToAvatar(@DrawableRes resId: Int)
    fun showTo(show: Boolean)
    fun setPayload(payload: String?)
    fun setTimestamp(format: String)
    fun setFee(fee: String)
    fun setBlockNumber(blockNum: String)


}