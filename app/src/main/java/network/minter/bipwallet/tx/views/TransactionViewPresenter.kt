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

package network.minter.bipwallet.tx.views

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import network.minter.bipwallet.R
import network.minter.bipwallet.apis.reactive.avatar
import network.minter.bipwallet.databinding.*
import network.minter.bipwallet.internal.exceptions.FirebaseSafe
import network.minter.bipwallet.internal.helpers.DateHelper.DATE_FORMAT_WITH_TZ
import network.minter.bipwallet.internal.helpers.DateHelper.fmt
import network.minter.bipwallet.internal.helpers.IntentHelper
import network.minter.bipwallet.internal.helpers.MathHelper.humanize
import network.minter.bipwallet.internal.helpers.ResTextFormat
import network.minter.bipwallet.internal.helpers.ViewExtensions.copyOnClick
import network.minter.bipwallet.internal.helpers.ViewExtensions.tr
import network.minter.bipwallet.internal.mvp.MvpBasePresenter
import network.minter.bipwallet.internal.storage.SecretStorage
import network.minter.bipwallet.share.ShareManager
import network.minter.bipwallet.share.SharingText
import network.minter.bipwallet.tx.adapters.TransactionFacade
import network.minter.bipwallet.tx.contract.TransactionView
import network.minter.bipwallet.tx.ui.TransactionViewDialog
import network.minter.core.MinterSDK
import network.minter.explorer.MinterExplorerSDK
import network.minter.explorer.models.CoinItemBase
import network.minter.explorer.models.HistoryTransaction
import org.joda.time.DateTime
import timber.log.Timber
import java.math.BigDecimal
import javax.inject.Inject

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
class TransactionViewPresenter @Inject constructor() : MvpBasePresenter<TransactionView>() {
    @Inject lateinit var secretStorage: SecretStorage

    private var _tx: TransactionFacade? = null
    private val tx: HistoryTransaction
        get() = _tx!!.tx

    override fun handleExtras(bundle: Bundle?) {
        super.handleExtras(bundle)
        _tx = IntentHelper.getParcelExtra(bundle, TransactionViewDialog.ARG_TX)

        fillData()
    }

    private fun String?.fromBase64(): String? {
        if (this == null || isEmpty()) return null
        return String(Base64.decode(this, 0))
    }

    private val titleTypedMap: HashMap<HistoryTransaction.Type, Int> = hashMapOf(
            Pair(HistoryTransaction.Type.Delegate, R.string.tx_type_delegate),
            Pair(HistoryTransaction.Type.Unbond, R.string.tx_type_unbond),
            Pair(HistoryTransaction.Type.SellCoin, R.string.tx_type_exchange),
            Pair(HistoryTransaction.Type.SellAllCoins, R.string.tx_type_exchange),
            Pair(HistoryTransaction.Type.BuyCoin, R.string.tx_type_exchange),
            Pair(HistoryTransaction.Type.CreateCoin, R.string.tx_type_create_coin),
            Pair(HistoryTransaction.Type.DeclareCandidacy, R.string.tx_type_declare_candidacy),
            Pair(HistoryTransaction.Type.RedeemCheck, R.string.tx_type_redeem_check),
            Pair(HistoryTransaction.Type.SetCandidateOnline, R.string.tx_type_set_candidate_online),
            Pair(HistoryTransaction.Type.SetCandidateOffline, R.string.tx_type_set_candidate_offline),
            Pair(HistoryTransaction.Type.CreateMultisigAddress, R.string.tx_type_create_multisig),
            Pair(HistoryTransaction.Type.EditCandidate, R.string.tx_type_edit_candidate),
            Pair(HistoryTransaction.Type.SetHaltBlock, R.string.tx_type_set_halt_block),
            Pair(HistoryTransaction.Type.RecreateCoin, R.string.tx_type_recreate_coin),
            Pair(HistoryTransaction.Type.EditCoinOwner, R.string.tx_type_change_coin_owner),
            Pair(HistoryTransaction.Type.EditMultisig, R.string.tx_type_edit_multisig),
            Pair(HistoryTransaction.Type.PriceVote, R.string.tx_type_price_vote),
            Pair(HistoryTransaction.Type.EditCandidatePublicKey, R.string.tx_type_edit_candidate_pub_key),
            Pair(HistoryTransaction.Type.AddLiquidity, R.string.tx_type_add_liquidity),
            Pair(HistoryTransaction.Type.RemoveLiquidity, R.string.tx_type_remove_liquidity),
            Pair(HistoryTransaction.Type.BuySwapPool, R.string.tx_type_buy_swap_pool),
            Pair(HistoryTransaction.Type.SellSwapPool, R.string.tx_type_sell_swap_pool),
            Pair(HistoryTransaction.Type.SellAllSwapPool, R.string.tx_type_sell_all_swap_pool),
            Pair(HistoryTransaction.Type.MoveStake, R.string.tx_type_move_stake),
            Pair(HistoryTransaction.Type.EditCandidateCommission, R.string.tx_type_edit_candidate_commission),
            Pair(HistoryTransaction.Type.MintToken, R.string.tx_type_mint_token),
            Pair(HistoryTransaction.Type.BurnToken, R.string.tx_type_burn_token),
            Pair(HistoryTransaction.Type.CreateToken, R.string.tx_type_create_token),
            Pair(HistoryTransaction.Type.RecreateToken, R.string.tx_type_recreate_token),
            Pair(HistoryTransaction.Type.VoteCommission, R.string.tx_type_vote_commission),
            Pair(HistoryTransaction.Type.VoteUpdate, R.string.tx_type_vote_update),
            Pair(HistoryTransaction.Type.CreateSwapPool, R.string.tx_type_create_swap_pool),
    )

    private fun dumpTx(tx: HistoryTransaction?): String {
        if (tx == null) {
            return "{null}"
        }

        return """{
            |txn: ${tx.txn}
            |hash: ${tx.hash}
            |nonce; ${tx.nonce}
            |height: ${tx.block}
            |timestamp: ${tx.timestamp}
            |fee: ${tx.fee}
            |gasCoin: ${tx.gasCoin}
            |type: ${tx.type.name}
            |from: ${tx.from}
            |data: ${if (tx.data == null) "null" else tx.data.javaClass.simpleName}
            |payload: ${tx.payload}
            |}""".trimMargin()
    }

    @Suppress("DEPRECATION")
    @SuppressLint("SetTextI18n")
    private fun fillData() {
        viewState.setOnClickShare { startShare() }
        viewState.setFromName(_tx?.fromName)
        viewState.setFromAddress(tx.from.toString())
        viewState.setFromAvatar(tx.from.avatar)
        viewState.setToName(null)
        viewState.setToAddress(null)

        viewState.setPayload(tx.payload.fromBase64())
        viewState.setTimestamp(DateTime(tx.timestamp).fmt(DATE_FORMAT_WITH_TZ))
        if (tx.gasCoin.id != MinterSDK.DEFAULT_COIN_ID) {
            viewState.setFee("${tx.gasCoin} (${tx.fee.humanize()} ${MinterSDK.DEFAULT_COIN})")
        } else {
            viewState.setFee("${tx.fee.humanize()} ${tx.gasCoin}")
        }

        viewState.setBlockNumber(tx.block.toString())
        viewState.setBlockClickListener { onClickBlockNumber() }

        if (tx.data == null) {
            FirebaseSafe.setCustomKey("tx_id", tx.hash?.toHexString() ?: "null")
            Timber.e("Decoded transaction data is null! Transaction: %s", dumpTx(tx))
        }

        val isIncoming = tx.isIncoming(listOf(secretStorage.mainWallet))

        if (tx.type == HistoryTransaction.Type.Send || tx.type == HistoryTransaction.Type.MultiSend) {
            if (isIncoming) {
                viewState.setTitle(R.string.dialog_title_tx_view_incoming)
            } else {
                viewState.setTitle(R.string.dialog_title_tx_view_outgoing)
            }
        } else {
            if (titleTypedMap.containsKey(tx.type)) {
                viewState.setTitleTyped(titleTypedMap[tx.type]!!)
            } else {
                viewState.setTitle(ResTextFormat(R.string.fmt_type_of_transaction, tx.type.name))
            }
        }

        when (tx.type) {
            HistoryTransaction.Type.Send -> {
                val data: HistoryTransaction.TxSendCoinResult = tx.getData()
                viewState.setToName(_tx!!.toName)
                viewState.setFromName(_tx!!.fromName)

                viewState.setToAddress(data.to.toString())
                viewState.setToAvatar(data.to.avatar)

                viewState.inflateDetails(R.layout.tx_details_send) {
                    val b = TxDetailsSendBinding.bind(it)
                    b.valueAmount.text = data.amount.humanize()
                    b.valueCoin.text = data.coin.symbol
                    if (data.coin.type != CoinItemBase.CoinType.Coin) {
                        b.labelCoin.setText(R.string.label_token)
                    }
                }
            }
            HistoryTransaction.Type.MultiSend -> {
                if (isIncoming) {
                    val data: HistoryTransaction.TxMultisendResult = tx.getData()
                    val sendItem = data.items.filter { it.to == secretStorage.mainWallet }.toList()

                    if (sendItem.isNotEmpty()) {
                        val entry = sendItem.iterator().next()
                        viewState.setToName(null)
                        viewState.setToAddress(entry.to.toString())
                        viewState.setToAvatar(entry.to.avatar)

                        viewState.inflateDetails(R.layout.tx_details_send) {
                            val b = TxDetailsSendBinding.bind(it)
                            b.valueAmount.text = entry.amount.humanize()
                            b.valueCoin.text = entry.coin.symbol
                            if (entry.coin.type != CoinItemBase.CoinType.Coin) {
                                b.labelCoin.setText(R.string.label_token)
                            }
                        }
                    } else {
                        viewState.showTo(false)
                    }
                } else {
                    viewState.showTo(false)
                }
            }
            HistoryTransaction.Type.SellCoin,
            HistoryTransaction.Type.SellAllCoins,
            HistoryTransaction.Type.BuyCoin -> {
                viewState.showTo(false)

                viewState.inflateDetails(R.layout.tx_details_exchange) {
                    val b = TxDetailsExchangeBinding.bind(it)
                    val data: HistoryTransaction.TxConvertCoinResult = tx.getData()
                    b.valueFromCoin.text = data.coinToSell.symbol
                    b.valueToCoin.text = data.coinToBuy.symbol
                    b.valueAmountReceived.text = data.valueToBuy.humanize()
                    b.valueAmountSpent.text = data.valueToSell.humanize()
                }
            }
            HistoryTransaction.Type.CreateCoin,
            HistoryTransaction.Type.RecreateCoin -> {
                viewState.showTo(false)

                viewState.inflateDetails(R.layout.tx_details_create_coin) {
                    val b = TxDetailsCreateCoinBinding.bind(it)
                    val data: HistoryTransaction.TxCreateCoinResult = tx.getData()
                    b.valueCoinName.text = data.name
                    b.valueCoinSymbol.text = data.symbol
                    b.valueInitialAmount.text = data.initialAmount.humanize()
                    b.valueInitialReserve.text = data.initialReserve.humanize()
                    b.valueCrr.text = "${data.constantReserveRatio}%"

                    b.valueMaxSupply.text = if (data.maxSupply < BigDecimal("10").pow(15)) {
                        data.maxSupply.humanize()
                    } else {
                        "10¹⁵ (max)"
                    }
                }
            }
            HistoryTransaction.Type.DeclareCandidacy -> {
                val data: HistoryTransaction.TxDeclareCandidacyResult = tx.getData()

                viewState.setToAvatar(_tx?.toAvatar, R.drawable.img_avatar_candidate)
                viewState.setToName(_tx?.toName)
                viewState.setToAddress(data.publicKey.toString())

                viewState.inflateDetails(R.layout.tx_details_declare_candidacy) {
                    val b = TxDetailsDeclareCandidacyBinding.bind(it)

                    b.valueCommission.text = "${data.commission}%"
                    b.valueCoin.text = data.coin.symbol
                    b.valueStake.text = data.stake.humanize()
                }
            }
            HistoryTransaction.Type.Delegate,
            HistoryTransaction.Type.Unbond -> {
                val data: HistoryTransaction.TxDelegateUnbondResult? = tx.getData()

                viewState.setToName(_tx?.toName)

                if (data != null) {
                    viewState.setToAddress(data.publicKey?.toString())
                } else {
                    viewState.setToAddress("<unknown>")
                }


                if (tx.type == HistoryTransaction.Type.Delegate) {
                    viewState.setToAvatar(_tx?.toAvatar, R.drawable.img_avatar_delegate)
                } else {
                    viewState.setToAvatar(_tx?.toAvatar, R.drawable.img_avatar_unbond)
                }

                viewState.inflateDetails(R.layout.tx_details_delegate_unbond) {
                    val b = TxDetailsDelegateUnbondBinding.bind(it)
                    if (data != null) {
                        b.valueCoin.text = data.coin.symbol
                        b.valueStake.text = data.value.humanize()
                    } else {
                        b.valueCoin.text = "<unknown>"
                        b.valueStake.text = "<unknown>"
                    }

                }
            }
            HistoryTransaction.Type.RedeemCheck -> {
                viewState.showTo(true)

                // to = from
                viewState.setToLabel(R.string.label_from)
                viewState.setToName(_tx?.fromName)
                viewState.setToAddress(tx.from.toString())
                viewState.setToAvatar(_tx?.fromAvatar)

                viewState.inflateDetails(R.layout.tx_details_redeem_check) {
                    val b = TxDetailsRedeemCheckBinding.bind(it)
                    val data: HistoryTransaction.TxRedeemCheckResult = tx.getData()
                    val check = data.check

                    // from = to = issuer
                    viewState.setFromLabel(R.string.label_check_issuer)

                    if (check != null) {
                        viewState.setFromAddress(check.sender.toString())
                        viewState.setFromName(_tx?.toName)
                        viewState.setFromAvatar(check.sender.avatar)

                        b.valueAmount.text = check.value.humanize()
                        b.valueCoin.text = check.coin.symbol
                    } else {
                        viewState.setFromAddress("<unknown>")
                        viewState.setFromName(null)
                        b.valueAmount.text = "<unknown>"
                        b.valueCoin.text = "<unknown>"
                    }
                }
            }
            HistoryTransaction.Type.SetCandidateOnline,
            HistoryTransaction.Type.SetCandidateOffline -> {
                val data: HistoryTransaction.TxSetCandidateOnlineOfflineResult = tx.getData()

                if (data.publicKey == null) {
                    viewState.showTo(false)
                } else {
                    viewState.setToAvatar(_tx?.toAvatar, R.drawable.img_avatar_candidate)
                    viewState.setToAddress(data.publicKey?.toString() ?: "<unknown>")
                    viewState.setToName(_tx?.toName)
                }
            }
            HistoryTransaction.Type.EditCandidate -> {
                val data: HistoryTransaction.TxEditCandidateResult = tx.getData()

                viewState.setToAvatar(_tx?.toAvatar, R.drawable.img_avatar_candidate)
                viewState.setToAddress(data.publicKey.toString())
                viewState.setToName(_tx?.toName)

                viewState.inflateDetails(R.layout.tx_details_edit_candidate) {
                    val b = TxDetailsEditCandidateBinding.bind(it)

                    b.valueOwnerAddress.text = data.ownerAddress.toString()
                    b.valueOwnerAddress.copyOnClick()
                    b.valueRewardAddress.text = data.rewardAddress.toString()
                    b.valueRewardAddress.copyOnClick()
                    b.valueControlAddress.text = data.controlAddress.toString()
                    b.valueControlAddress.copyOnClick()
                }
            }
            HistoryTransaction.Type.CreateMultisigAddress,
            HistoryTransaction.Type.EditMultisig -> {
                viewState.showTo(false)

                viewState.inflateDetails(R.layout.tx_details_create_multisig_address) {
                    val data: HistoryTransaction.TxCreateMultisigResult = tx.getData()
                    val b = TxDetailsCreateMultisigAddressBinding.bind(it)
                    b.valueMultisigAddress.text = data.multisigAddress?.toString() ?: "<none>"
                    b.valueMultisigAddress.copyOnClick()
                }

            }
            HistoryTransaction.Type.SetHaltBlock -> {
                val data: HistoryTransaction.TxSetHaltBlockResult = tx.getData()

                viewState.setToAvatar(_tx?.toAvatar, R.drawable.img_avatar_candidate)
                viewState.setToName(_tx?.toName)
                viewState.setToAddress(data.publicKey.toString())

                viewState.inflateDetails(R.layout.tx_details_set_halt_block) {

                    val b = TxDetailsSetHaltBlockBinding.bind(it)
                    b.valuePublicKey.text = data.publicKey.toString()
                    b.valuePublicKey.copyOnClick()
                    b.valueHeight.text = data.height.toString()
                }
            }
            HistoryTransaction.Type.EditCoinOwner -> {
                viewState.showTo(false)

                viewState.inflateDetails(R.layout.tx_details_change_coin_owner) {
                    val data: HistoryTransaction.TxChangeCoinOwnerResult = tx.getData()
                    val b = TxDetailsChangeCoinOwnerBinding.bind(it)
                    b.valueCoinSymbol.text = data.symbol
                    b.valueNewOwner.text = data.newOwner.toString()
                    b.valueNewOwner.copyOnClick()
                }

            }
            HistoryTransaction.Type.PriceVote -> {
                viewState.showTo(false)

                viewState.inflateDetails(R.layout.tx_details_price_vote) {
                    val data: HistoryTransaction.TxPriceVoteResult = tx.getData()
                    val b = TxDetailsPriceVoteBinding.bind(it)
                    b.valuePrice.text = data.price.toString()
                }
            }
            HistoryTransaction.Type.EditCandidatePublicKey -> {

                val data: HistoryTransaction.TxEditCandidatePublicKeyResult = tx.getData()

                viewState.setToAvatar(_tx?.toAvatar, R.drawable.img_avatar_candidate)
                viewState.setToName(_tx?.toName)
                viewState.setToAddress(data.publicKey.toString())

                viewState.inflateDetails(R.layout.tx_details_edit_candidate_public_key) {
                    val b = TxDetailsEditCandidatePublicKeyBinding.bind(it)

                    b.valueOldPublicKey.text = data.publicKey.toString()
                    b.valueOldPublicKey.copyOnClick()
                    b.valueNewPublicKey.text = data.newPublicKey.toString()
                    b.valueNewPublicKey.copyOnClick()
                }
            }

            // @since Minter 2.0

            HistoryTransaction.Type.AddLiquidity -> {
                viewState.showTo(false)
                viewState.inflateDetails(R.layout.tx_details_pool) {
                    val b = TxDetailsPoolBinding.bind(it)
                    val data: HistoryTransaction.TxAddLiquidityResult = tx.getData()

                    b.valueCoin0.text = data.coin0.symbol
                    b.valueCoin1.text = data.coin1.symbol
                    b.valueVolume0.text = data.volume0.humanize()
                    b.valueVolume1.text = data.volume1.humanize()
                    b.valuePoolToken.text = data.poolToken.symbol
                    b.valueLiquidity.text = data.liquidity.humanize()
                }
            }
            HistoryTransaction.Type.RemoveLiquidity -> {
                viewState.showTo(false)
                viewState.showTo(false)
                viewState.inflateDetails(R.layout.tx_details_pool) {
                    val b = TxDetailsPoolBinding.bind(it)
                    val data: HistoryTransaction.TxRemoveLiquidityResult = tx.getData()

                    b.valueCoin0.text = data.coin0.symbol
                    b.valueCoin1.text = data.coin1.symbol
                    b.valueVolume0.text = data.volume0.humanize()
                    b.valueVolume1.text = data.volume1.humanize()
                    b.valuePoolToken.text = data.poolToken.symbol
                    b.valueLiquidity.text = data.liquidity.humanize()
                }
            }

            HistoryTransaction.Type.SellSwapPool -> {
                viewState.showTo(false)
                viewState.inflateDetails(R.layout.tx_details_exchange_pool) {
                    val b = TxDetailsExchangePoolBinding.bind(it)

                    val data: HistoryTransaction.TxSellSwapPoolResult = tx.getData()
                    val sb = StringBuilder()

                    data.coins.forEachIndexed { idx, coin ->
                        sb.append(coin.symbol)
                        if (idx < data.coins.size - 1) {
                            sb.append(" > ")
                        }
                    }
                    b.valueCoinChain.text = sb.toString()
                    b.valueAmountReceived.text = data.valueToBuy.humanize()
                    b.valueAmountSpent.text = data.valueToSell.humanize()
                }
            }
            HistoryTransaction.Type.SellAllSwapPool -> {
                viewState.showTo(false)
                viewState.inflateDetails(R.layout.tx_details_exchange_pool) {
                    val b = TxDetailsExchangePoolBinding.bind(it)

                    val data: HistoryTransaction.TxSellAllSwapPoolResult = tx.getData()
                    val sb = StringBuilder()
                    data.coins.forEachIndexed { idx, coin ->
                        sb.append(coin.symbol)
                        if (idx < data.coins.size - 1) {
                            sb.append(" > ")
                        }
                    }
                    b.valueCoinChain.text = sb.toString()
                    b.valueAmountReceived.text = data.valueToBuy.humanize()
                    b.valueAmountSpent.text = data.valueToSell.humanize()
                }
            }
            HistoryTransaction.Type.BuySwapPool -> {
                viewState.showTo(false)
                viewState.inflateDetails(R.layout.tx_details_exchange_pool) {
                    val b = TxDetailsExchangePoolBinding.bind(it)

                    val data: HistoryTransaction.TxBuySwapPoolResult = tx.getData()
                    val sb = StringBuilder()
                    data.coins.forEachIndexed { idx, coin ->
                        sb.append(coin.symbol)
                        if (idx < data.coins.size - 1) {
                            sb.append(" > ")
                        }
                    }
                    b.valueCoinChain.text = sb.toString()
                    b.valueAmountReceived.text = data.valueToBuy.humanize()
                    b.valueAmountSpent.text = data.valueToSell.humanize()
                }
            }
            HistoryTransaction.Type.EditCandidateCommission -> {
                val data: HistoryTransaction.TxEditCandidateCommissionResult = tx.getData()

                viewState.setToAvatar(_tx?.toAvatar, R.drawable.img_avatar_candidate)
                viewState.setToName(_tx?.toName)
                viewState.setToAddress(data.pubKey.toString())

                viewState.inflateDetails(R.layout.tx_details_edit_candidate_commission) {
                    val b = TxDetailsEditCandidateCommissionBinding.bind(it)
                    b.valueCommission.text = "${data.commission}%"
                }
            }
            HistoryTransaction.Type.MoveStake -> {
                viewState.showTo(false)

                viewState.inflateDetails(R.layout.tx_details_move_stake) {
                    val b = TxDetailsMoveStakeBinding.bind(it)
                    val data: HistoryTransaction.TxMoveStakeResult = tx.getData()
                    b.valueCoin.text = data.coin.symbol
                    b.valueStake.text = data.stake.humanize()
                    b.valueFrom.text = data.from.toString()
                    b.valueTo.text = data.to.toString()
                }
            }

            HistoryTransaction.Type.MintToken,
            HistoryTransaction.Type.BurnToken -> {
                viewState.showTo(false)

                viewState.inflateDetails(R.layout.tx_details_send) {
                    val b = TxDetailsSendBinding.bind(it)
                    val data: HistoryTransaction.TxMintTokenResult = tx.getData()
                    b.valueAmount.text = data.value.humanize()
                    b.labelAmount.setText(R.string.value)
                    b.valueCoin.text = data.coin.symbol
                }
            }
            HistoryTransaction.Type.CreateToken,
            HistoryTransaction.Type.RecreateToken -> {
                viewState.showTo(false)

                viewState.inflateDetails(R.layout.tx_details_create_token) {
                    val b = TxDetailsCreateTokenBinding.bind(it)
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
            HistoryTransaction.Type.VoteCommission -> {
                val data: HistoryTransaction.TxVoteCommissionResult = tx.getData()

                viewState.setToAvatar(_tx?.toAvatar, R.drawable.img_avatar_candidate)
                viewState.setToName(_tx?.toName)
                viewState.setToAddress(data.pubKey.toString())

            }
            HistoryTransaction.Type.VoteUpdate -> {
                val data: HistoryTransaction.TxVoteUpdateResult = tx.getData()

                viewState.setToAvatar(_tx?.toAvatar, R.drawable.img_avatar_candidate)
                viewState.setToName(_tx?.toName)
                viewState.setToAddress(data.pubKey.toString())

                viewState.inflateDetails(R.layout.tx_details_vote_update) {
                    val b = TxDetailsVoteUpdateBinding.bind(it)
                    b.valueVersion.text = data.version
                }
            }
            HistoryTransaction.Type.CreateSwapPool -> {
                viewState.showTo(false)
                viewState.inflateDetails(R.layout.tx_details_pool) {
                    val b = TxDetailsPoolBinding.bind(it)
                    val data: HistoryTransaction.TxCreateSwapPoolResult = tx.getData()

                    b.valueCoin0.text = data.coin0.symbol
                    b.valueCoin1.text = data.coin1.symbol
                    b.valueVolume0.text = data.volume0.humanize()
                    b.valueVolume1.text = data.volume1.humanize()
                    b.valuePoolToken.text = data.poolToken.symbol
                    b.valueLiquidity.text = data.liquidity.humanize()
                }
            }

            else -> {
                viewState.showTo(false)
                // nothing to do
            }
        }
    }

    private fun onClickBlockNumber() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(
                MinterExplorerSDK.newFrontUrl().addPathSegment("transactions").addPathSegment(tx.hash.toString()).build().toString()
        ))
        viewState.startIntent(intent)
    }

    private fun startShare() {
        val text = SharingText()
        text.title = "${tr(R.string.share_transaction_title_prefix)} " + tx.hash.toShortString()
        text.url = MinterExplorerSDK.newFrontUrl().addPathSegment("transactions").addPathSegment(tx.hash.toString()).build().toString()

        val shareIntent = ShareManager.getInstance().createCommonIntent(text, tr(R.string.chooser_share_transaction))
        viewState.startIntent(shareIntent)
    }

}
