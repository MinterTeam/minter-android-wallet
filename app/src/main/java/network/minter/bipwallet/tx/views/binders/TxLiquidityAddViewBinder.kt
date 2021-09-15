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

import network.minter.bipwallet.R
import network.minter.bipwallet.databinding.TxDetailsPoolBinding
import network.minter.bipwallet.internal.helpers.MathHelper.humanize
import network.minter.bipwallet.tx.adapters.TransactionFacade
import network.minter.bipwallet.tx.contract.TransactionView
import network.minter.explorer.models.HistoryTransaction

class TxLiquidityAddViewBinder(
        txFacade: TransactionFacade,
        viewState: TransactionView) :
        TxViewBinder(txFacade, viewState) {
    override fun bind() {
        safeViewState {
            it.showTo(false)
            it.inflateDetails(R.layout.tx_details_pool) { view ->
                val b = TxDetailsPoolBinding.bind(view)
                val data: HistoryTransaction.TxAddLiquidityResult = tx.getData()

                b.valueCoin0.text = data.coin0.symbol
                b.valueCoin1.text = data.coin1.symbol
                b.valueVolume0.text = data.volume0.humanize()
                b.valueVolume1.text = data.volume1.humanize()
                b.valuePoolToken.text = data.poolToken.symbol
                b.valueLiquidity.text = data.liquidity.humanize()
            }
        }
    }


}