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
import network.minter.bipwallet.internal.exceptions.FirebaseSafe
import network.minter.bipwallet.internal.helpers.DateHelper.DATE_FORMAT_WITH_TZ
import network.minter.bipwallet.internal.helpers.DateHelper.fmt
import network.minter.bipwallet.internal.helpers.IntentHelper
import network.minter.bipwallet.internal.helpers.MathHelper.humanize
import network.minter.bipwallet.internal.helpers.ResTextFormat
import network.minter.bipwallet.internal.helpers.ViewExtensions.tr
import network.minter.bipwallet.internal.mvp.MvpBasePresenter
import network.minter.bipwallet.internal.storage.SecretStorage
import network.minter.bipwallet.share.ShareManager
import network.minter.bipwallet.share.SharingText
import network.minter.bipwallet.tx.adapters.TransactionFacade
import network.minter.bipwallet.tx.contract.TransactionView
import network.minter.bipwallet.tx.ui.TransactionViewDialog
import network.minter.bipwallet.tx.views.binders.*
import network.minter.core.MinterSDK
import network.minter.explorer.MinterExplorerSDK
import network.minter.explorer.models.HistoryTransaction
import org.joda.time.DateTime
import timber.log.Timber
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
            Pair(HistoryTransaction.Type.AddLimitOrder, R.string.tx_type_add_limit_order),
            Pair(HistoryTransaction.Type.RemoveLimitOrder, R.string.tx_type_remove_limit_order),
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
                TxSendViewBinder(_tx!!, viewState).bind()
            }
            HistoryTransaction.Type.MultiSend -> {
                TxMultiSendViewBinder(_tx!!, viewState, isIncoming, secretStorage).bind()
            }
            HistoryTransaction.Type.SellCoin,
            HistoryTransaction.Type.SellAllCoins,
            HistoryTransaction.Type.BuyCoin -> {
                TxExchangeBancorViewBinder(_tx!!, viewState).bind()
            }
            HistoryTransaction.Type.CreateCoin,
            HistoryTransaction.Type.RecreateCoin -> {
                TxCreateCoinViewBinder(_tx!!, viewState).bind()
            }
            HistoryTransaction.Type.DeclareCandidacy -> {
                TxDeclareCandidacyViewBinder(_tx!!, viewState).bind()
            }
            HistoryTransaction.Type.Delegate,
            HistoryTransaction.Type.Unbond -> {
                TxDelegateUnbondViewBinder(_tx!!, viewState).bind()
            }
            HistoryTransaction.Type.RedeemCheck -> {
                TxRedeemCheckViewBinder(_tx!!, viewState).bind()
            }
            HistoryTransaction.Type.SetCandidateOnline,
            HistoryTransaction.Type.SetCandidateOffline -> {
                TxCandidateSwitchViewBinder(_tx!!, viewState).bind()
            }
            HistoryTransaction.Type.EditCandidate -> {
               TxCandidateEditViewBinder(_tx!!, viewState).bind()
            }
            HistoryTransaction.Type.CreateMultisigAddress,
            HistoryTransaction.Type.EditMultisig -> {
                TxMultisigViewBinder(_tx!!, viewState).bind()
            }
            HistoryTransaction.Type.SetHaltBlock -> {
                TxSetHaltBlockViewBinder(_tx!!, viewState).bind()
            }
            HistoryTransaction.Type.EditCoinOwner -> {
                TxEditCoinOwnerViewBinder(_tx!!, viewState).bind()
            }
            HistoryTransaction.Type.PriceVote -> {
                TxPriceVoteViewBinder(_tx!!, viewState).bind()
            }
            HistoryTransaction.Type.EditCandidatePublicKey -> {
                TxCandidateEditPubKeyViewBinder(_tx!!, viewState).bind()
            }

            // @since Minter 2.0
            HistoryTransaction.Type.AddLiquidity -> {
                TxLiquidityAddViewBinder(_tx!!, viewState).bind()
            }
            HistoryTransaction.Type.RemoveLiquidity -> {
               TxLiquidityRemoveViewBinder(_tx!!, viewState).bind()
            }
            HistoryTransaction.Type.SellSwapPool,
            HistoryTransaction.Type.SellAllSwapPool,
            HistoryTransaction.Type.BuySwapPool -> {
                TxExchangePoolViewBinder(_tx!!, viewState).bind()
            }
            HistoryTransaction.Type.EditCandidateCommission -> {
                TxCandidateEditCommissionViewBinder(_tx!!, viewState).bind()
            }
            HistoryTransaction.Type.MoveStake -> {
                TxMoveStakeViewBinder(_tx!!, viewState).bind()
            }
            HistoryTransaction.Type.MintToken,
            HistoryTransaction.Type.BurnToken -> {
                TxTokenMintBurnViewBinder(_tx!!, viewState).bind()
            }
            HistoryTransaction.Type.CreateToken,
            HistoryTransaction.Type.RecreateToken -> {
                TxTokenCreateRecreateViewBinder(_tx!!, viewState).bind()
            }
            HistoryTransaction.Type.VoteCommission -> {
                TxVoteCommissionViewBinder(_tx!!, viewState).bind()
            }
            HistoryTransaction.Type.VoteUpdate -> {
                TxVoteUpdateViewBinder(_tx!!, viewState).bind()
            }
            HistoryTransaction.Type.CreateSwapPool -> {
                TxCreateSwapPoolViewBinder(_tx!!, viewState).bind()
            }
            // @since Minter 2.6
            HistoryTransaction.Type.AddLimitOrder -> {
                TxLimitOrderAddViewBinder(_tx!!, viewState).bind()
            }
            HistoryTransaction.Type.RemoveLimitOrder -> {
                TxLimitOrderRemoveViewBinder(_tx!!, viewState).bind()
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
