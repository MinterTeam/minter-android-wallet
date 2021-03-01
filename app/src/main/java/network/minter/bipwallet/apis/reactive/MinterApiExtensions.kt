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

package network.minter.bipwallet.apis.reactive

import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import io.reactivex.Observable
import network.minter.bipwallet.BuildConfig
import network.minter.bipwallet.internal.Wallet
import network.minter.bipwallet.internal.helpers.MathHelper.humanize
import network.minter.bipwallet.internal.helpers.RegexReplaceData
import network.minter.bipwallet.internal.helpers.StringsHelper
import network.minter.blockchain.models.operational.Transaction
import network.minter.blockchain.models.operational.TxVoteCommission
import network.minter.core.MinterSDK
import network.minter.core.crypto.MinterAddress
import network.minter.core.crypto.MinterPublicKey
import network.minter.explorer.models.CoinItemBase
import network.minter.explorer.models.ExpResult
import network.minter.explorer.models.GateResult
import network.minter.explorer.models.HistoryTransaction
import retrofit2.Call
import java.math.BigDecimal
import java.math.BigInteger

val GATE_UNHANDLED_ERRORS = listOf(
        RegexReplaceData(
                """(coin\s[a-zA-Z0-9]{3,10}\sreserve\sis\stoo\ssmall\s\()([0-9]+)(\,\srequired\sat\sleast\s)([0-9]+)(\))""".toRegex(),
                listOf(2, 4)
        ) { _, v -> BigDecimal(v).divide(Transaction.VALUE_MUL_DEC).humanize() + " " + MinterSDK.DEFAULT_COIN }
)

fun GateResult<*>.humanError(defValue: String? = "Caused unknown error"): String? {
    for (d in GATE_UNHANDLED_ERRORS) {
        if (d.pattern.matches(message!!)) {
            return StringsHelper.replaceGroups(message, d)
        }
    }

    return message ?: defValue
}


fun <T> GateResult<*>.castErrorResultTo(): GateResult<T> {
    return ReactiveGate.copyError(this)
}

fun <T> Call<ExpResult<T>>.rxExp(): Observable<ExpResult<T>> {
    return ReactiveExplorer.rxExp(this)
            .onErrorResumeNext(ReactiveExplorer.toExpError())
}

fun <T> T.toObservable(): Observable<T> {
    return Observable.just(this)
}

fun <R, T : Throwable> T.toObservable(): Observable<R> {
    return Observable.error<R>(this)
}

val CoinItemBase.avatar: String
    get() {
        return BuildConfig.COIN_AVATAR_BASE_URL + symbol
    }

val HistoryTransaction.TxChangeCoinOwnerResult.avatar: String
    get() {
        return BuildConfig.COIN_AVATAR_BASE_URL + symbol
    }

val MinterAddress.avatar: String
    get() {
        return BuildConfig.ADDRESS_AVATAR_BASE_URL + toString()
    }

val MinterPublicKey.avatar: String
    get() = "${BuildConfig.EXPLORER_STATIC_URL}/validators/${toString()}.png"

fun Context?.sendLocalBroadcast(intent: Intent) {
    if (this == null) return
    LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
}

fun Float.dp(): Float {
    return Wallet.app().display().dpToPx(this).toFloat()
}

fun Int.dp(): Float {
    return Wallet.app().display().dpToPx(this).toFloat()
}

fun TxVoteCommission.nameValueMap(): Map<String, BigInteger> {
    return hashMapOf(
            Pair("Payload Byte", payloadByte),
            Pair("Send", send),
            Pair("Buy Bancor", buyBancor),
            Pair("Sell Bancor", sellBancor),
            Pair("Sell All Bancor", sellAllBancor),
            Pair("Buy Pool Base", buyPoolBase),
            Pair("Buy Pool Delta", buyPoolDelta),
            Pair("Buy Pool Base", buyPoolBase),
            Pair("Buy Pool", buyPoolDelta),
            Pair("Sell Pool Base", sellPoolBase),
            Pair("Sell Pool Delta", sellPoolDelta),
            Pair("Sell All Pool Base", sellAllPoolBase),
            Pair("Sell All Pool Delta", sellAllPoolDelta),
            Pair("Add Liquidity", addLiquidity),
            Pair("Remove Liquidity", removeLiquidity),
            Pair("Create Swap Pool", createSwapPool),
            Pair("Create Ticker 3", createTicker3),
            Pair("Create Ticker 4", createTicker4),
            Pair("Create Ticker 5", createTicker5),
            Pair("Create Ticker 6", createTicker6),
            Pair("Create Ticker 7 to 10", createTicker7to10),
            Pair("Create Coin", createCoin),
            Pair("Recreate Coin", recreateCoin),
            Pair("Create Token", createToken),
            Pair("Recreate Token", recreateToken),
            Pair("Mint Token", mintToken),
            Pair("Burn Token", burnToken),
            Pair("Declare Candidacy", declareCandidacy),
            Pair("Delegate", delegate),
            Pair("Unbond", unbond),
            Pair("Move Stake", moveStake),
            Pair("Redeem Check", redeemCheck),
            Pair("Set Candidate On", setCandidateOn),
            Pair("Set Candidate Off", setCandidateOff),
            Pair("Create Multisig", createMultisig),
            Pair("Edit Multisig", editMultisig),
            Pair("Multisend Base", multisendBase),
            Pair("Multisend Delta", multisendDelta),
            Pair("Edit Candidate", editCandidate),
            Pair("Edit Candidate Public Key", editCandidatePubKey),
            Pair("Edit Candidate Commission", editCandidateCommission),
            Pair("Set Halt Block", setHaltBlock),
            Pair("Edit Ticker Owner", editTickerOwner),
            Pair("Vote for Price", priceVote),
            Pair("Vote for Update", voteUpdate),
            Pair("Vote for Commissions", voteCommission),
    )
}
