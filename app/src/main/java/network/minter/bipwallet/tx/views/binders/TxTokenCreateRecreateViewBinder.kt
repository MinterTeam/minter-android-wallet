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
import network.minter.bipwallet.databinding.TxDetailsCreateTokenBinding
import network.minter.bipwallet.internal.helpers.MathHelper.humanize
import network.minter.bipwallet.tx.adapters.TransactionFacade
import network.minter.bipwallet.tx.contract.TransactionView
import network.minter.explorer.models.HistoryTransaction
import java.math.BigDecimal

class TxTokenCreateRecreateViewBinder(
        txFacade: TransactionFacade,
        viewState: TransactionView) :
        TxViewBinder(txFacade, viewState) {
    override fun bind() {
        safeViewState {
            it.showTo(false)

            it.inflateDetails(R.layout.tx_details_create_token) { view ->
                val b = TxDetailsCreateTokenBinding.bind(view)
                val data: HistoryTransaction.TxCreateTokenResult = tx.getData()
                b.valueCoinName.text = data.name
                b.valueCoinSymbol.text = data.symbol
                b.valueInitialAmount.text = data.initialAmount.humanize()
                b.valueMintable.setText(if (data.mintable) R.string.yes else R.string.no)
                b.valueBurnable.setText(if (data.burnable) R.string.yes else R.string.no)

                b.valueMaxSupply.text = if (data.maxSupply < BigDecimal("10").pow(15)) {
                    data.maxSupply.humanize()
                } else {
                    "10¹⁵ (max)"
                }
            }
        }
    }


}