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
import network.minter.bipwallet.apis.reactive.avatar
import network.minter.bipwallet.databinding.TxDetailsRedeemCheckBinding
import network.minter.bipwallet.internal.helpers.MathHelper.humanize
import network.minter.bipwallet.tx.adapters.TransactionFacade
import network.minter.bipwallet.tx.contract.TransactionView
import network.minter.explorer.models.HistoryTransaction

class TxRedeemCheckViewBinder(
        txFacade: TransactionFacade,
        viewState: TransactionView) :
        TxViewBinder(txFacade, viewState) {
    override fun bind() {
        safeViewState {
            it.showTo(true)

            // to = from
            it.setToLabel(R.string.label_from)
            it.setToName(txWrapper.fromName)
            it.setToAddress(tx.from.toString())
            it.setToAvatar(txWrapper.fromAvatar)

            it.inflateDetails(R.layout.tx_details_redeem_check) { view ->
                val b = TxDetailsRedeemCheckBinding.bind(view)
                val data: HistoryTransaction.TxRedeemCheckResult = tx.getData()
                val check = data.check

                // from = to = issuer
                it.setFromLabel(R.string.label_check_issuer)

                if (check != null) {
                    it.setFromAddress(check.sender.toString())
                    it.setFromName(txWrapper.toName)
                    it.setFromAvatar(check.sender.avatar)

                    b.valueAmount.text = check.value.humanize()
                    b.valueCoin.text = check.coin.symbol
                } else {
                    it.setFromAddress("<unknown>")
                    it.setFromName(null)
                    b.valueAmount.text = "<unknown>"
                    b.valueCoin.text = "<unknown>"
                }
            }
        }
    }


}