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
import network.minter.bipwallet.databinding.TxDetailsSendBinding
import network.minter.bipwallet.internal.helpers.MathHelper.humanize
import network.minter.bipwallet.internal.storage.SecretStorage
import network.minter.bipwallet.tx.adapters.TransactionFacade
import network.minter.bipwallet.tx.contract.TransactionView
import network.minter.explorer.models.CoinItemBase
import network.minter.explorer.models.HistoryTransaction

class TxMultiSendViewBinder(
        txFacade: TransactionFacade,
        viewState: TransactionView,
        private val isIncoming: Boolean,
        private val secretStorage: SecretStorage) :
        TxViewBinder(txFacade, viewState) {
    override fun bind() {
        safeViewState {
            if (isIncoming) {
                val data: HistoryTransaction.TxMultisendResult = tx.getData()
                val sendItem = data.items.filter { it.to == secretStorage.mainWallet }.toList()

                if (sendItem.isNotEmpty()) {
                    val entry = sendItem.iterator().next()
                    it.setToName(null)
                    it.setToAddress(entry.to.toString())
                    it.setToAvatar(entry.to.avatar)

                    it.inflateDetails(R.layout.tx_details_send) { view ->
                        val b = TxDetailsSendBinding.bind(view)
                        b.valueAmount.text = entry.amount.humanize()
                        b.valueCoin.text = entry.coin.symbol
                        if (entry.coin.type != CoinItemBase.CoinType.Coin) {
                            b.labelCoin.setText(R.string.label_token)
                        }
                    }
                } else {
                    it.showTo(false)
                }
            } else {
                it.showTo(false)
            }
        }

    }


}