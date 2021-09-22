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

package network.minter.bipwallet.tx.adapters.vh

import android.annotation.SuppressLint
import androidx.recyclerview.widget.RecyclerView
import network.minter.bipwallet.R
import network.minter.bipwallet.apis.reactive.avatar
import network.minter.bipwallet.databinding.ItemListTxBinding
import network.minter.bipwallet.internal.helpers.MathHelper.bdHuman
import network.minter.bipwallet.internal.helpers.MathHelper.bdNull
import network.minter.bipwallet.internal.helpers.MathHelper.humanize
import network.minter.bipwallet.internal.helpers.ViewExtensions.tr
import network.minter.bipwallet.internal.helpers.ViewExtensions.visible
import network.minter.bipwallet.tx.adapters.TxItem
import network.minter.core.crypto.MinterAddress
import network.minter.explorer.models.CoinItemBase
import network.minter.explorer.models.HistoryTransaction
import timber.log.Timber
import java.math.BigDecimal

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
@SuppressLint("SetTextI18n")
class TxAllViewHolder(
        var binding: ItemListTxBinding,

        ) : RecyclerView.ViewHolder(binding.root) {

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
            /** @since minter 1.2 */
            HistoryTransaction.Type.SetHaltBlock -> bindSetHaltBlock(item)
            HistoryTransaction.Type.RecreateCoin -> bindCreateCoin(item)
            HistoryTransaction.Type.EditCoinOwner -> bindChangeCoinOwner(item)
            HistoryTransaction.Type.EditMultisig -> bindCreateMultisigAddress(item)
            HistoryTransaction.Type.PriceVote -> bindPriceVote(item)
            HistoryTransaction.Type.EditCandidatePublicKey -> bindEditCandidatePublicKey(item)
            /** @since minter 2.0 */
            HistoryTransaction.Type.AddLiquidity -> bindAddLiquidity(item)
            HistoryTransaction.Type.RemoveLiquidity -> bindRemoveLiquidity(item)
            HistoryTransaction.Type.SellSwapPool,
            HistoryTransaction.Type.SellAllSwapPool,
            HistoryTransaction.Type.BuySwapPool -> bindExchangeSwapPool(item)
            HistoryTransaction.Type.EditCandidateCommission -> bindEditCandidateCommission(item)
            HistoryTransaction.Type.MoveStake -> bindMoveStake(item)
            HistoryTransaction.Type.MintToken -> bindMintToken(item)
            HistoryTransaction.Type.BurnToken -> bindBurnToken(item)
            HistoryTransaction.Type.CreateToken,
            HistoryTransaction.Type.RecreateToken -> bindCreateToken(item)
            HistoryTransaction.Type.VoteCommission -> bindVoteCommission(item)
            HistoryTransaction.Type.VoteUpdate -> bindVoteUpdate(item)
            HistoryTransaction.Type.CreateSwapPool -> bindCreateSwapPool(item)
            /** @since minter 2.6 */
            HistoryTransaction.Type.AddLimitOrder -> bindAddLimitOrder(item)
            HistoryTransaction.Type.RemoveLimitOrder -> bindRemoveLimitOrder(item)
            else -> {

            }
        }
    }

    private fun bindRemoveLimitOrder(item: TxItem) {
        val data: HistoryTransaction.TxLimitOrderRemoveResult = item.tx.getData()

        binding.apply {
            itemTitleType.setText(R.string.tx_type_remove_limit_order)
            itemAvatar.setImageUrlFallback(item.tx.toAvatar, R.drawable.img_avatar_exchange)
            itemTitle.text = "ID: ${data.id}"
        }
    }

    private fun bindAddLimitOrder(item: TxItem) {
        val data: HistoryTransaction.TxLimitOrderAddResult = item.tx.getData()

        binding.apply {
            itemTitleType.setText(R.string.tx_type_add_limit_order)
            itemAvatar.setImageResource(R.drawable.img_avatar_exchange)
            itemTitle.text = "${data.coinToSell} –> ${data.coinToBuy}"
            itemAmount.text = data.valueToBuy.humanize()
            itemSubamount.text = data.coinToBuy.symbol
        }
    }

    private fun bindCreateSwapPool(item: TxItem) {
        val data: HistoryTransaction.TxCreateSwapPoolResult = item.tx.getData()

        binding.apply {
            itemTitleType.setText(R.string.tx_type_create_swap_pool)
            itemAvatar.setImageUrlFallback(item.tx.toAvatar, R.drawable.img_avatar_candidate)
            itemTitle.text = "${data.coin0} / ${data.coin1}"
            itemAmount.text = data.liquidity.humanize()
            itemSubamount.text = data.poolToken.symbol
        }
    }

    private fun bindVoteUpdate(item: TxItem) {
        val data: HistoryTransaction.TxVoteUpdateResult = item.tx.getData()

        binding.apply {
            itemTitleType.setText(R.string.tx_type_vote_update)
            itemAvatar.setImageUrlFallback(item.tx.toAvatar, R.drawable.img_avatar_candidate)
            itemTitle.text = tr(R.string.tx_type_vote_update_title, data.version)
        }
    }

    private fun bindVoteCommission(item: TxItem) {
        binding.apply {
            itemTitleType.setText(R.string.tx_type_vote_commission)
            itemAvatar.setImageUrlFallback(item.tx.toAvatar, R.drawable.img_avatar_candidate)
            itemTitle.setText(R.string.tx_type_vote_commission_title)
        }
    }

    private fun bindCreateToken(item: TxItem) {
        var createCoin = true
        val data = if (item.tx.type == HistoryTransaction.Type.CreateToken) {
            item.tx.getData<HistoryTransaction.TxCreateTokenResult>()
        } else {
            createCoin = false
            item.tx.getData<HistoryTransaction.TxRecreateTokenResult>()
        }
        binding.apply {
            itemTitleType.setText(if (createCoin) R.string.tx_type_create_token else R.string.tx_type_recreate_token)
            itemAvatar.setImageResource(R.drawable.img_avatar_create_coin)
            itemAmount.text = data.initialAmount.humanize()
            itemTitle.text = if (data.name.isEmpty()) data.symbol else data.name
            itemSubamount.text = data.symbol
        }
    }

    private fun bindMintToken(item: TxItem) {
        val data: HistoryTransaction.TxMintTokenResult = item.tx.getData()

        binding.apply {
            itemTitleType.setText(R.string.tx_type_mint_token)
            itemAvatar.setImageUrlFallback(item.tx.toAvatar, R.drawable.img_avatar_candidate)
            itemTitle.text = data.coin.symbol
            itemAmount.text = data.value.humanize()
            itemSubamount.visible = false
        }
    }

    private fun bindBurnToken(item: TxItem) {
        val data: HistoryTransaction.TxBurnTokenResult = item.tx.getData()

        binding.apply {
            itemTitleType.setText(R.string.tx_type_burn_token)
            itemAvatar.setImageUrlFallback(item.tx.toAvatar, R.drawable.img_avatar_candidate)
            itemTitle.text = data.coin.symbol
            itemAmount.text = data.value.humanize()
            itemSubamount.visible = false
        }
    }

    private fun bindMoveStake(item: TxItem) {
        val data: HistoryTransaction.TxMoveStakeResult = item.tx.getData()

        binding.apply {
            itemTitleType.setText(R.string.tx_type_move_stake)
            itemAvatar.setImageUrlFallback(item.tx.toAvatar, R.drawable.img_avatar_delegate)
            itemTitle.text = data.to.toShortString()
            itemAmount.text = data.stake.humanize()
            itemSubamount.text = data.coin.symbol
        }
    }

    private fun bindEditCandidateCommission(item: TxItem) {
        val data: HistoryTransaction.TxEditCandidateCommissionResult = item.tx.getData()

        binding.apply {
            itemTitleType.setText(R.string.tx_type_edit_candidate_commission)
            itemAvatar.setImageUrlFallback(item.tx.toAvatar, R.drawable.img_avatar_candidate)
            itemTitle.text = item.tx.toName ?: data.pubKey.toShortString()
            itemSubamount.visible = false
        }
    }

    private fun bindExchangeSwapPool(item: TxItem) {
        val data: HistoryTransaction.TxConvertSwapPoolResult = item.tx.getData()

        binding.apply {
            itemTitleType.setText(R.string.tx_type_exchange)
            itemTitle.text = "${data.coinFirst} –> ${data.coinLast}"
            itemAvatar.setImageResource(R.drawable.img_avatar_exchange)
            itemAmount.text = data.valueToBuy.humanize()
            itemSubamount.text = data.coinLast?.symbol ?: ""
        }
    }

    private fun bindRemoveLiquidity(item: TxItem) {
        val data: HistoryTransaction.TxRemoveLiquidityResult = item.tx.getData()

        binding.apply {
            itemTitleType.setText(R.string.tx_type_remove_liquidity)
            itemAvatar.setImageUrlFallback(item.tx.toAvatar, R.drawable.img_avatar_candidate)
            itemTitle.text = "${data.coin0} / ${data.coin1}"
            itemAmount.text = "- ${data.liquidity.humanize()}"
            itemSubamount.text = data.poolToken.symbol
        }
    }

    private fun bindAddLiquidity(item: TxItem) {
        val data: HistoryTransaction.TxAddLiquidityResult = item.tx.getData()

        binding.apply {
            itemTitleType.setText(R.string.tx_type_add_liquidity)
            itemAvatar.setImageUrlFallback(item.tx.toAvatar, R.drawable.img_avatar_candidate)
            itemTitle.text = "${data.coin0} / ${data.coin1}"
            itemAmount.text = data.liquidity.humanize()
            itemSubamount.text = data.poolToken.symbol
        }
    }

    private fun bindEditCandidatePublicKey(item: TxItem) {
        val data: HistoryTransaction.TxEditCandidatePublicKeyResult = item.tx.getData()

        binding.apply {
            itemTitleType.setText(R.string.tx_type_edit_candidate_pub_key)
            itemAvatar.setImageUrlFallback(item.tx.toAvatar, R.drawable.img_avatar_candidate)
            itemTitle.text = item.tx.toName ?: data.publicKey.toShortString()
            itemAmount.text = ""
            itemSubamount.visible = false
        }
    }

    private fun bindPriceVote(item: TxItem) {
        val data: HistoryTransaction.TxPriceVoteResult = item.tx.getData()

        binding.apply {
            itemTitleType.setText(R.string.tx_type_price_vote)
            itemAvatar.setImageUrlFallback(item.tx.toAvatar, R.drawable.img_avatar_candidate)
            itemTitle.text = tr(R.string.tx_type_price_vote_title, data.price.toString())
            itemAmount.text = ""
            itemSubamount.visible = false
        }
    }

    private fun bindChangeCoinOwner(item: TxItem) {
        val data: HistoryTransaction.TxChangeCoinOwnerResult = item.tx.getData()

        binding.apply {
            itemTitleType.setText(R.string.tx_type_change_coin_owner)
            itemAvatar.setImageUrlFallback(data.avatar, R.drawable.img_avatar_create_coin)
            itemTitle.text = data.newOwner.toShortString()
            itemAmount.text = ""
            itemSubamount.visible = false
        }
    }

    private fun bindSetHaltBlock(item: TxItem) {
        val data: HistoryTransaction.TxSetHaltBlockResult = item.tx.getData()

        binding.apply {
            itemAvatar.setImageUrlFallback(item.tx.toAvatar, R.drawable.img_avatar_candidate)
            itemTitleType.setText(R.string.tx_type_set_halt_block)
            itemTitle.text = data.height.toString()
            itemAmount.text = ""
            itemSubamount.visible = false
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


    private fun mapMultisend(result: List<HistoryTransaction.TxSendCoinResult>): Map<CoinItemBase, BigDecimal> {
        val out = HashMap<CoinItemBase, BigDecimal>()
        result.forEach {
            if (out.containsKey(it.coin) && out[it.coin] != null) {
                out[it.coin] = out[it.coin]!! + it.amount
            } else {
                out[it.coin] = it.amount
            }
        }
        return out
    }

    private fun mapIncomingMultisend(myAddress: () -> MinterAddress, result: List<HistoryTransaction.TxSendCoinResult>): Map<CoinItemBase, BigDecimal> {
        val out = HashMap<CoinItemBase, BigDecimal>()
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
            itemTitleType.setText(R.string.tx_type_multisend)

            val coinsAmount: Map<CoinItemBase, BigDecimal>

            if (isIncoming) {
                itemAvatar.setImageUrlFallback(item.fromAvatar, R.drawable.img_avatar_multisend)

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
                    itemSubamount.text = entry.key.symbol
                }
            } else {
                itemAvatar.setImageUrlFallback(item.toAvatar, R.drawable.img_avatar_multisend)
                coinsAmount = mapMultisend(data.items)

                if (coinsAmount.size > 1) {
                    itemAmount.setText(R.string.dots)
                    itemSubamount.setText(R.string.label_multiple_coins)
                } else {
                    val entry = coinsAmount.entries.iterator().next()
                    itemAmount.text = String.format("- %s", bdHuman(entry.value))
                    itemSubamount.text = entry.key.symbol
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
            itemTitle.text = item.tx.toName ?: data.publicKey?.toShortString() ?: "<unknown>"
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
            if (data.check != null) {
                if (item.tx.from == myAddress()) {
                    itemAmount.text = data.check.value.humanize()
                } else {
                    itemAmount.text = "- ${data.check.value.humanize()}"
                }

                itemSubamount.text = data.check.coin.symbol
            }
        }
    }

    private fun bindDelegateUnbond(item: TxItem) {
        val data: HistoryTransaction.TxDelegateUnbondResult = item.tx.getData()
        binding.apply {
            val fallbackAvatar: Int
            val typeText: Int
            if (item.tx.type == HistoryTransaction.Type.Delegate) {
                fallbackAvatar = R.drawable.img_avatar_delegate
                typeText = R.string.tx_type_delegate
            } else {
                fallbackAvatar = R.drawable.img_avatar_unbond
                typeText = R.string.tx_type_unbond
            }

            itemAvatar.setImageUrlFallback(item.tx.toAvatar, fallbackAvatar)
            itemTitle.text = item.tx.toName ?: data.publicKey.toShortString()
            itemTitleType.setText(typeText)
            itemSubamount.text = data.coin.symbol

            if (item.tx.type == HistoryTransaction.Type.Delegate) {
                itemAmount.text = "- ${data.value.humanize()}"
            } else {
                itemAmount.text = data.value.humanize()
            }

        }
    }

    private fun bindCreateCoin(item: TxItem) {
        var createCoin = true
        val data = if (item.tx.type == HistoryTransaction.Type.CreateCoin) {
            item.tx.getData<HistoryTransaction.TxCreateCoinResult>()
        } else {
            createCoin = false
            item.tx.getData<HistoryTransaction.TxRecreateCoinResult>()
        }
        binding.apply {
            itemTitleType.setText(if (createCoin) R.string.tx_type_create_coin else R.string.tx_type_recreate_coin)
            itemAvatar.setImageResource(R.drawable.img_avatar_create_coin)
            itemAmount.text = data.initialAmount.humanize()
            itemTitle.text = if (data.name.isEmpty()) data.symbol else data.name
            itemSubamount.text = data.symbol
        }
    }

    private fun bindCreateMultisigAddress(item: TxItem) {
        var create = true
        val data = if (item.tx.type == HistoryTransaction.Type.CreateMultisigAddress) {
            item.tx.getData<HistoryTransaction.TxCreateMultisigResult>()
        } else {
            create = false
            item.tx.getData<HistoryTransaction.TxEditMultisigResult>()
        }

        binding.apply {
            itemTitleType.setText(if (create) R.string.tx_type_create_multisig else R.string.tx_type_edit_multisig)
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
            itemSubamount.text = data.coin.symbol
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
                    itemAmount.text = "- ${data.amount.humanize()}"
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
