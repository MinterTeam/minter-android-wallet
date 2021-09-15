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

package network.minter.bipwallet.tx.views.binders

import network.minter.bipwallet.tx.adapters.TransactionFacade
import network.minter.bipwallet.tx.contract.TransactionView
import network.minter.explorer.models.HistoryTransaction
import java.lang.ref.WeakReference

/**
 * minter-android-wallet. 2021
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
abstract class TxViewBinder(
        protected val txWrapper: TransactionFacade,
        viewState: TransactionView
) {
    private val vs = WeakReference(viewState)

    protected fun safeViewState(cb: (TransactionView)->Unit) {
        if(vs.get() != null) {
            val local = vs.get()!!
            cb(local)
        }
    }

    protected val tx: HistoryTransaction
        get()
        {
            return txWrapper.tx

        }
    abstract fun bind()
}
