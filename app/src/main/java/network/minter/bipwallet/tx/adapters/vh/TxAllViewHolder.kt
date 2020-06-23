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

package network.minter.bipwallet.tx.adapters.vh

import android.annotation.SuppressLint
import androidx.recyclerview.widget.RecyclerView
import network.minter.bipwallet.R
import network.minter.bipwallet.databinding.ItemListTxBinding
import network.minter.bipwallet.internal.helpers.MathHelper.bdHuman
import network.minter.bipwallet.internal.helpers.MathHelper.bdNull
import network.minter.bipwallet.internal.helpers.MathHelper.humanize
import network.minter.bipwallet.internal.helpers.ViewExtensions.visible
import network.minter.bipwallet.tx.adapters.TxItem
import network.minter.core.crypto.MinterAddress
import network.minter.explorer.models.HistoryTransaction
import timber.log.Timber
import java.math.BigDecimal

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
@SuppressLint("SetTextI18n")
class TxAllViewHolder(
        var binding: ItemListTxBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: TxItem, myAddress: () -> MinterAddress) {

        binding.itemTitleType.text = item.tx.type.name
        binding.separator.visible = true

        when (item.tx.type) {
            HistoryTransaction.Type.Send -> bindSend(item, myAddress)
            HistoryTransaction.Type.SellCoin,
            HistoryTransaction.Type.SellAllCoins,
            HistoryTransaction.Type.BuyCoin -> bindExchange(item)
            HistoryTransaction.Type.CreateCoin -> bindCreateCoin(item)
            HistoryTransaction.Type.DeclareCandidacy -> bindDeclareCandidacy(item)
            HistoryTransaction.Type.Delegate,
            HistoryTransaction.Type.Unbond -> bindDelegateUnbond(item)
            HistoryTransaction.Type.RedeemCheck -> bindRedeemCheck(item, myAddress)
            HistoryTransaction.Type.SetCandidateOnline,
            HistoryTransaction.Type.SetCandidateOffline -> bindSetCandidateOnOff(item)
            HistoryTransaction.Type.CreateMultisigAddress -> bindCreateMultisigAddress(item)
            HistoryTransaction.Type.MultiSend -> bindMultisend(item, myAddress)
            HistoryTransaction.Type.EditCandidate -> bindEditCandidate(item)
            else -> {

            }
        }
    }

    private fun bindEditCandidate(item: TxItem) {
        val data: HistoryTransaction.TxEditCandidateResult = item.tx.getData()

        binding.apply {
            itemTitleType.setText(R.string.tx_type_edit_candidate)
            itemAvatar.setImageUrlFallback(item.tx.toAvatar, R.drawable.img_avatar_candidate)
            itemTitle.text = item.tx.toName ?: data.publicKey.toShortString()
            itemAmount.text = ""
            itemSubamount.visible = false
        }
    }


    private fun mapMultisend(result: List<HistoryTransaction.TxSendCoinResult>): Map<String, BigDecimal> {
        val out = HashMap<String, BigDecimal>()
        result.forEach {
            if (out.containsKey(it.coin) && out[it.coin] != null) {
                out[it.coin] = out[it.coin]!! + it.amount
            } else {
                out[it.coin] = it.amount
            }
        }
        return out
    }

    private fun mapIncomingMultisend(myAddress: () -> MinterAddress, result: List<HistoryTransaction.TxSendCoinResult>): Map<String, BigDecimal> {
        val out = HashMap<String, BigDecimal>()
        result.filter { it.to == myAddress() }
                .forEach {
                    if (out.containsKey(it.coin) && out[it.coin] != null) {
                        out[it.coin] = out[it.coin]!! + it.amount
                    } else {
                        out[it.coin] = it.amount
                    }
                }
        return out
    }

    private fun bindMultisend(txItem: TxItem, myAddress: () -> MinterAddress) {
        val item = txItem.tx
        val data: HistoryTransaction.TxMultisendResult = item.tx.getData()

        val isIncoming: Boolean = myAddress() != item.from

        binding.apply {
            itemTitle.text = txItem.tx.fromName ?: item.from.toShortString()
            itemAvatar.setImageUrlFallback(item.toAvatar, R.drawable.img_avatar_multisend)

            val coinsAmount: Map<String, BigDecimal>

            if (isIncoming) {
                coinsAmount = mapIncomingMultisend(myAddress, data.items)

                if (coinsAmount.isEmpty()) {
                    Timber.e("NO one incoming transaction in multisend")
                }
                if (coinsAmount.isEmpty() || coinsAmount.size > 1) {
                    itemAmount.setText(R.string.dots)
                    itemSubamount.setText(R.string.label_multiple_coins)
                } else {
                    val entry = coinsAmount.entries.iterator().next()
                    itemAmount.text = entry.value.humanize()
                    itemSubamount.text = entry.key
                }
            } else {
                coinsAmount = mapMultisend(data.items)

                if (coinsAmount.size > 1) {
                    itemAmount.setText(R.string.dots)
                    itemSubamount.setText(R.string.label_multiple_coins)
                } else {
                    val entry = coinsAmount.entries.iterator().next()
                    itemAmount.text = String.format("- %s", bdHuman(entry.value))
                    itemSubamount.text = entry.key
                }
            }
        }
    }

    private fun bindSetCandidateOnOff(item: TxItem) {
        val data: HistoryTransaction.TxSetCandidateOnlineOfflineResult = item.tx.getData()
        binding.apply {
            if (item.tx.type == HistoryTransaction.Type.SetCandidateOnline) {
                itemTitleType.setText(R.string.tx_type_set_candidate_online)
            } else if (item.tx.type == HistoryTransaction.Type.SetCandidateOffline) {
                itemTitleType.setText(R.string.tx_type_set_candidate_offline)
            }

            itemAvatar.setImageUrlFallback(item.tx.toAvatar, R.drawable.img_avatar_candidate)
            itemTitle.text = item.tx.toName ?: data.publicKey.toShortString()
            itemAmount.text = ""
            itemSubamount.visible = false
        }
    }

    @Suppress("DEPRECATION")
    private fun bindRedeemCheck(item: TxItem, myAddress: () -> MinterAddress) {
        val data: HistoryTransaction.TxRedeemCheckResult = item.tx.getData()
        binding.apply {
            itemTitleType.setText(R.string.tx_type_redeem_check)
            itemAvatar.setImageResource(R.drawable.img_avatar_redeem)
            itemTitle.text = item.tx.hash.toShortString()
            if (item.tx.from == myAddress()) {
                itemAmount.text = data.check.value.humanize()
            } else {
                itemAmount.text = "- ${data.check.value.humanize()}"
            }

            itemSubamount.text = data.check.coin
        }
    }

    private fun bindDelegateUnbond(item: TxItem) {
        val data: HistoryTransaction.TxDelegateUnbondResult = item.tx.getData()
        binding.apply {
            val fallbackAvatar = if (item.tx.type == HistoryTransaction.Type.Delegate) {
                R.drawable.img_avatar_delegate
            } else {
                R.drawable.img_avatar_unbond
            }

            itemAvatar.setImageUrlFallback(item.tx.toAvatar, fallbackAvatar)
            itemTitle.text = item.tx.toName ?: data.publicKey.toShortString()
            itemSubamount.text = data.coin

            if (item.tx.type == HistoryTransaction.Type.Delegate) {
                itemAmount.text = "- ${data.value.humanize()}"
            } else {
                itemAmount.text = data.value.humanize()
            }

        }
    }

    private fun bindCreateCoin(item: TxItem) {
        val data: HistoryTransaction.TxCreateResult = item.tx.getData()
        binding.apply {
            itemTitleType.setText(R.string.tx_type_create_coin)
            itemAvatar.setImageResource(R.drawable.img_avatar_create_coin)
            itemAmount.text = data.initialAmount.humanize()
            itemTitle.text = data.name
            itemSubamount.text = data.symbol
        }
    }

    private fun bindCreateMultisigAddress(item: TxItem) {
        val data: HistoryTransaction.TxCreateMultisigResult = item.tx.getData()

        binding.apply {
            itemTitleType.setText(R.string.tx_type_create_multisig)
            itemAvatar.setImageResource(R.drawable.img_avatar_multisig)
            itemTitle.text = data.multisigAddress?.toShortString() ?: "<none>"
            itemAmount.text = ""
            itemSubamount.visible = false
        }
    }

    private fun bindDeclareCandidacy(item: TxItem) {
        val data: HistoryTransaction.TxDeclareCandidacyResult = item.tx.getData()
        binding.apply {
            itemTitleType.setText(R.string.tx_type_declare_candidacy)
            itemAvatar.setImageUrlFallback(item.tx.toAvatar, R.drawable.img_avatar_candidate)
            itemTitle.text = item.tx.toName ?: data.publicKey.toShortString()
            itemSubamount.text = data.coin
            itemAmount.text = "- ${data.stake.humanize()}"
        }
    }

    private fun bindSend(txItem: TxItem, myAddress: () -> MinterAddress) {
        val item = txItem.tx
        val data: HistoryTransaction.TxSendCoinResult = item.tx.getData()

        val isIncoming: Boolean = item.isIncoming(listOf(myAddress()))
        val isSelfSending = item.from == data.to

        binding.apply {

            if (isIncoming) {
                itemTitleType.setText(R.string.tx_type_send_receive)
            } else {
                itemTitleType.setText(R.string.tx_type_send)
            }

            if (isIncoming) {
                itemAvatar.setImageUrlFallback(txItem.tx.fromAvatar, R.drawable.img_avatar_default)
            } else {
                itemAvatar.setImageUrlFallback(txItem.tx.toAvatar, R.drawable.img_avatar_default)
            }

            if (isSelfSending) {
                itemTitle.text = txItem.tx.fromName ?: item.from.toShortString()
                itemAmount.text = data.amount.humanize()
            } else {
                if (isIncoming) {
                    itemTitle.text = txItem.tx.fromName ?: item.from.toShortString()
                    itemAmount.text = data.amount.humanize()
                } else {
                    itemTitle.text = txItem.tx.toName ?: data.to.toShortString()
                    itemAmount.text = String.format("- %s", bdHuman(data.amount))
                }
            }

            if (bdNull(data.amount)) {
                itemAmount.text = data.amount.humanize()
            }

            itemSubamount.text = data.getCoin()
        }
    }

    private fun bindExchange(item: TxItem) {
        val data: HistoryTransaction.TxConvertCoinResult = item.tx.getData()

        binding.apply {
            itemTitleType.setText(R.string.tx_type_exchange)
            itemTitle.text = "${data.getCoinToSell()} –> ${data.getCoinToBuy()}"
            itemAvatar.setImageResource(R.drawable.img_avatar_exchange)
            itemAmount.text = data.valueToBuy.humanize()
            itemSubamount.text = data.getCoinToBuy()
        }
    }
}