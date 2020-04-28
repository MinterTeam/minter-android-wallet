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
package network.minter.bipwallet.tx.views

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.view.View
import com.airbnb.deeplinkdispatch.DeepLink
import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Function
import io.reactivex.schedulers.Schedulers
import moxy.InjectViewState
import network.minter.bipwallet.R
import network.minter.bipwallet.apis.explorer.RepoTransactions
import network.minter.bipwallet.apis.reactive.ReactiveGate
import network.minter.bipwallet.apis.reactive.rxGate
import network.minter.bipwallet.internal.Wallet
import network.minter.bipwallet.internal.common.Preconditions.firstNonNull
import network.minter.bipwallet.internal.dialogs.ConfirmDialog
import network.minter.bipwallet.internal.dialogs.WalletProgressDialog
import network.minter.bipwallet.internal.exceptions.InvalidExternalTransaction
import network.minter.bipwallet.internal.helpers.DeepLinkHelper
import network.minter.bipwallet.internal.helpers.MathHelper.clamp
import network.minter.bipwallet.internal.helpers.MathHelper.humanize
import network.minter.bipwallet.internal.mvp.MvpBasePresenter
import network.minter.bipwallet.internal.storage.RepoAccounts
import network.minter.bipwallet.internal.storage.SecretStorage
import network.minter.bipwallet.internal.views.list.multirow.MultiRowAdapter
import network.minter.bipwallet.tx.contract.ExternalTransactionView
import network.minter.bipwallet.tx.contract.TxInitData
import network.minter.bipwallet.tx.ui.ExternalTransactionActivity
import network.minter.bipwallet.tx.ui.InputFieldRow
import network.minter.blockchain.models.TransactionSendResult
import network.minter.blockchain.models.operational.*
import network.minter.blockchain.utils.Base64UrlSafe
import network.minter.core.MinterSDK
import network.minter.core.crypto.BytesData
import network.minter.core.crypto.MinterAddress
import network.minter.core.internal.helpers.StringHelper
import network.minter.core.util.RLPBoxed
import network.minter.explorer.models.AddressBalance
import network.minter.explorer.models.GasValue
import network.minter.explorer.models.GateResult
import network.minter.explorer.models.GateResult.copyError
import network.minter.explorer.models.TxCount
import network.minter.explorer.repo.GateEstimateRepository
import network.minter.explorer.repo.GateGasRepository
import network.minter.explorer.repo.GateTransactionRepository
import timber.log.Timber
import java.math.BigDecimal
import java.math.BigInteger
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * minter-android-wallet. 2019
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
@InjectViewState
class ExternalTransactionPresenter @Inject constructor() : MvpBasePresenter<ExternalTransactionView>() {
    @Inject lateinit var secretStorage: SecretStorage
    @Inject lateinit var estimateRepo: GateEstimateRepository
    @Inject lateinit var gasRepo: GateGasRepository
    @Inject lateinit var gateTxRepo: GateTransactionRepository
    @Inject lateinit var accountStorage: RepoAccounts
    @Inject lateinit var cachedTxRepo: RepoTransactions
    @Inject lateinit var res: Resources

    private var mExtTx: ExternalTransaction? = null
    private var mFrom: MinterAddress? = null
    private var mPayload: BytesData? = null
    private var mCheckPassword: String? = null
    private var mGasPrice = BigInteger.ONE
    private val adapter = MultiRowAdapter()

    override fun attachView(view: ExternalTransactionView) {
        super.attachView(view)
        accountStorage.update()
        mFrom = secretStorage.addresses[0]
        viewState.setOnCancelListener(View.OnClickListener { onCancel() })
    }

    override fun handleExtras(intent: Intent?) {
        super.handleExtras(intent)
        mFrom = secretStorage.addresses[0]
        if (intent!!.getBooleanExtra(DeepLink.IS_DEEP_LINK, false)) {
            val params = intent.extras
            if (params == null) {
                viewState.finishCancel()
                return
            }
            var hash: String? = null
            // this value represents that we are parsing old-style deeplinks
            var support = true
            if (!params.containsKey("d") && !params.containsKey("data")) {
                viewState.disableAll()
                showTxErrorDialog("Unable to parse deeplink: No transaction data passed")
                return
            } else if (params.containsKey("d")) {
                hash = params.getString("d", null)
            } else if (params.containsKey("data")) {
                support = false
                hash = BytesData(
                        Base64UrlSafe.decode(params.getString("data", null).toByteArray())
                ).toHexString()
            }
            if (params.containsKey("p")) {
                mCheckPassword = try {
                    val rawPass = params.getString("p", null)
                    if (rawPass == null) {
                        null
                    } else {
                        if (support) {
                            RLPBoxed.decodeString(StringHelper.hexStringToChars(rawPass))
                        } else {
                            Base64UrlSafe.decodeString(rawPass)
                        }
                    }
                } catch (t: Throwable) {
                    Timber.w(t, "Unable to decode check password")
                    null
                }
            }
            try {
                mExtTx = DeepLinkHelper.parseRawTransaction(hash)
                if (!validateTx()) {
                    return
                }
            } catch (t: StringIndexOutOfBoundsException) {
                viewState.disableAll()
                Timber.w(t, "Unable to parse remote transaction: %s", hash)
                showTxErrorDialog("Invalid transaction data: non-hex string passed")
                return
            } catch (t: Throwable) {
                viewState.disableAll()
                Timber.w(t, "Unable to parse remote transaction: %s", hash)
                showTxErrorDialog("Invalid transaction data: %s", t.message!!)
                return
            }
        } else {
            val rawTx = intent.getStringExtra(ExternalTransactionActivity.EXTRA_RAW_DATA)
            try {
                if (rawTx == null) {
                    throw InvalidExternalTransaction("Empty transaction data", InvalidExternalTransaction.CODE_INVALID_TX)
                }
                val out = DeepLinkHelper.rawTxToIntent(rawTx)
                handleExtras(out)
                return
            } catch (t: Throwable) {
                viewState.disableAll()
                Timber.w(t, "Unable to parse remote transaction: %s", rawTx)
                showTxErrorDialog("Invalid transaction data: %s", t.message!!)
                return
            }
        }
        mPayload = mExtTx?.payload

        if (mExtTx != null) {
            calculateFee(mExtTx!!)
        }

        try {
            fillData(mExtTx!!)
        } catch (t: Throwable) {
            showTxErrorDialog("Invalid transaction data: %s", t.message!!)
        }
    }

    private fun getTxInitData(address: MinterAddress?): Observable<TxInitData> {
        return Observable.combineLatest(
                estimateRepo.getTransactionCount(address!!).rxGate(),
                gasRepo.minGas.rxGate(),
                BiFunction { txCountGateResult: GateResult<TxCount>, gasValueGateResult: GateResult<GasValue> ->

                    // if some request failed, returning error result
                    if (!txCountGateResult.isOk) {
                        return@BiFunction TxInitData(copyError<Any>(txCountGateResult))
                    } else if (!gasValueGateResult.isOk) {
                        return@BiFunction TxInitData(copyError<Any>(gasValueGateResult))
                    }
                    TxInitData(
                            txCountGateResult.result.count.add(BigInteger.ONE),
                            gasValueGateResult.result.gas
                    )
                }
        )
    }

    private fun showTxErrorDialog(message: String, vararg args: Any) {
        viewState.startDialog(false) { ctx: Context ->
            ConfirmDialog.Builder(ctx, "Unable to scan transaction")
                    .setText(message, *args)
                    .setPositiveAction(R.string.btn_close) { d, _ ->
                        d.dismiss()
                        viewState.finishCancel()
                    }
                    .create()
        }
    }

    private fun validateTx(): Boolean {
        if (mExtTx!!.type == OperationType.RedeemCheck) {
            val d = mExtTx!!.getData<TxRedeemCheck>()
            if (mCheckPassword != null) {
                d.proof = CheckTransaction.makeProof(mFrom, mCheckPassword)
            } else if (d.proof.size() == 0 && mCheckPassword == null) {
                viewState.disableAll()
                viewState.startDialog(false) { ctx: Context? ->
                    ConfirmDialog.Builder(ctx!!, "Unable to scan transaction")
                            .setText("This check given without proof and password. One of parameters is required.")
                            .setPositiveAction(R.string.btn_close) { d, _ ->
                                d.dismiss()
                                viewState.finishCancel()
                            }
                            .create()
                }
                return false
            }
        } else if (mExtTx!!.type == OperationType.SellAllCoins) {
            val data = mExtTx!!.getData(TxCoinSellAll::class.java)
            val mainWallet: AddressBalance = accountStorage.entity.mainWallet
            val acc = mainWallet.findCoinByName(data.coinToSell)
            if (acc.isEmpty) {
                viewState.disableAll()
                viewState.startDialog(false) { ctx: Context? ->
                    ConfirmDialog.Builder(ctx!!, "Unable to send transaction")
                            .setText("You are trying to sell all %s for %s, but your %s balance is 0", data.coinToSell, data.coinToBuy, data.coinToSell)
                            .setPositiveAction(R.string.btn_close) { d, _ ->
                                d.dismiss()
                                viewState.finishCancel()
                            }
                            .create()
                }
                return false
            }
        }
        return true
    }

    private fun calculateFee(tx: ExternalTransaction) {
        val bytesLen = firstNonNull(mPayload, tx.payload, BytesData(CharArray(0))).size().toLong()
        var baseFee: BigDecimal

        when (tx.type) {
            OperationType.Multisend -> {
                val txData = tx.getData(TxMultisend::class.java)
                if (txData.items.size == 0) {
                    onFailedExecuteTransaction(Exception("Multisend transaction must contains at least 1 target address"))
                    return
                }

                //10+(n-1)*5 units
                baseFee = OperationType.SendCoin.fee
                baseFee = baseFee.add(
                        BigDecimal(clamp(txData.items.size - 1, 0)).multiply(OperationType.FEE_BASE.multiply(BigDecimal("5")))
                )
            }
            OperationType.CreateCoin -> {
                // https://docs.minter.network/#section/Commissions
                val txData = tx.getData(TxCreateCoin::class.java)
                baseFee = TxCreateCoin.calculateCreatingCost(txData.symbol)
            }
            OperationType.RedeemCheck -> {
                viewState.setFee("You don't pay transaction fee.")
                return
            }
            else -> {
                baseFee = tx.type.fee
            }
        }

        var fee = baseFee.add(BigDecimal(bytesLen).multiply(BigDecimal("0.002")))
        fee = fee.multiply(BigDecimal(mGasPrice))
        viewState.setFee(String.format("%s %s", fee, MinterSDK.DEFAULT_COIN))
    }

    private fun fillData(tx: ExternalTransaction) {


//        viewState.setPayload(tx.payloadString)
        gasRepo.minGas
                .rxGate()
                .subscribeOn(Schedulers.io())
                .toFlowable(BackpressureStrategy.LATEST)
                .debounce(200, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ res: GateResult<GasValue> ->
                    if (res.isOk) {
                        mGasPrice = res.result.gas
                        Timber.d("Min Gas price: %s", mGasPrice.toString())
                        calculateFee(tx)
                    }
                }) { t: Throwable -> Timber.w(t) }

        viewState.setOnConfirmListener(View.OnClickListener { onSubmit() })

        when (tx.type) {
            OperationType.SendCoin -> {
                val data = tx.getData(TxSendCoin::class.java)
                val rows = InputFieldRow.MultiBuilder()
                        .add {
                            label = "You're sending"
                            text = data.coin
                        }
                        .add {
                            label = "Amount"
                            text = data.value.humanize()
                        }
                        .add {
                            label = "To"
                            text = data.to.toString()
                        }
                        .build()

                adapter.addRows(rows)
            }
            OperationType.SellCoin -> {
                val data = tx.getData(TxCoinSell::class.java)
                val rows = InputFieldRow.MultiBuilder()
                        .add {
                            label = "You're selling"
                            text = data.coinToSell
                        }
                        .add {
                            label = "Amount"
                            text = data.valueToSell.humanize()
                        }
                        .add {
                            label = "Coin To Buy"
                            text = data.coinToBuy
                        }
                        .build()

                adapter.addRows(rows)

//                viewState.setFirstLabel("You're want selling")
//                viewState.setFirstValue(String.format("%s %s", bdHuman(data.valueToSell), data.coinToSell))
//                viewState.setSecondLabel("To")
//                viewState.setSecondValue(data.coinToBuy)
            }
            OperationType.SellAllCoins -> {
                val data = tx.getData(TxCoinSellAll::class.java)
                val acc = accountStorage.entity.mainWallet.findCoinByName(data.coinToSell)
                val amount = if (acc.isPresent) {
                    acc.get().amount
                } else {
                    BigDecimal.ZERO
                }
                val rows = InputFieldRow.MultiBuilder()
                        .add {
                            label = "You're selling (sell all)"
                            text = data.coinToSell
                        }
                        .add {
                            label = "Amount"
                            text = amount.humanize()
                        }
                        .add {
                            label = "Coin To Buy"
                            text = data.coinToBuy
                        }
                        .build()

                adapter.addRows(rows)


//                viewState.setFirstLabel("You're selling")
//                val acc = accountStorage.entity.mainWallet.findCoinByName(data.coinToSell)
//                if (acc.isPresent) {
//                    viewState.setFirstValue(String.format("%s %s", bdHuman(acc.get().amount), data.coinToSell))
//                } else {
//                    viewState.setFirstValue(String.format("%s %s", bdHuman(0.0), data.coinToSell))
//                }
//                viewState.setSecondLabel("For")
//                viewState.setSecondValue(data.coinToBuy)
            }
            OperationType.BuyCoin -> {
                val data = tx.getData(TxCoinBuy::class.java)

                val rows = InputFieldRow.MultiBuilder()
                        .add {
                            label = "You're buying"
                            text = data.coinToBuy
                        }
                        .add {
                            label = "Amount"
                            text = data.valueToBuy.humanize()
                        }
                        .add {
                            label = "Coin To Sell"
                            text = data.coinToSell
                        }
                        .build()

                adapter.addRows(rows)

//                viewState.setFirstLabel("You're buying")
//                viewState.setFirstValue(String.format("%s %s", bdHuman(data.valueToBuy), data.coinToBuy))
//                viewState.setSecondLabel("For")
//                viewState.setSecondValue(data.coinToSell)
            }
            OperationType.CreateCoin -> {
                val data = tx.getData(TxCreateCoin::class.java)

                val rows = InputFieldRow.MultiBuilder()
                        .add {
                            label = "You're creating coin"
                            text = data.symbol
                        }
                        .add {
                            label = "Initial Amount"
                            text = data.initialAmount.humanize()
                        }
                        .add {
                            label = "Coin Name"
                            text = data.name
                        }
                        .add {
                            label = "CRR (constant reserve ratio)"
                            text = "${data.constantReserveRatio}%"
                        }
                        .add {
                            label = "Reserve"
                            text = data.initialReserve.humanize()
                        }
                        .add {
                            label = "Max Supply"
                            text = data.maxSupply.humanize()
                        }
                        .build()

                adapter.addRows(rows)

//                viewState.setFirstLabel("You're creating coin")
//                viewState.setFirstValue(String.format("%s %s", bdHuman(data.initialAmount), data.symbol))
//                viewState.setSecondLabel("Info")
//                viewState.setSecondValue(String.format("Name: %s\nCRR: %d\nReserve: %s",
//                        data.name,
//                        data.constantReserveRatio,
//                        bdHuman(data.initialReserve)
//                ))
            }
            OperationType.DeclareCandidacy -> {
                val data = tx.getData(TxDeclareCandidacy::class.java)

                val rows = InputFieldRow.MultiBuilder()
                        .add {
                            label = "You're declaring candidacy"
                            text = data.publicKey.toString()
                        }
                        .add {
                            label = "Address"
                            text = data.address.toString()
                        }
                        .add {
                            label = "Coin"
                            text = data.coin
                        }
                        .add {
                            label = "Commission Percent"
                            text = "${data.commission}%"
                        }
                        .build()

                adapter.addRows(rows)

//                viewState.setFirstLabel("You're declaring candidacy")
//                viewState.setFirstValue(data.publicKey.toString())
//                viewState.setSecondLabel("Info")
//                viewState.setSecondValue(String.format("Address: %s\nCoin: %s\nCommission: %d",
//                        data.address,
//                        data.coin,
//                        data.commission
//                ))
            }
            OperationType.Delegate -> {
                val data = tx.getData(TxDelegate::class.java)

                val rows = InputFieldRow.MultiBuilder()
                        .add {
                            label = "You're delegating"
                            text = data.coin
                        }
                        .add {
                            label = "Amount"
                            text = data.stake.humanize()
                        }
                        .add {
                            label = "To"
                            text = data.publicKey.toString()
                        }
                        .build()

                adapter.addRows(rows)

//                viewState.setFirstLabel("You're delegating")
//                viewState.setFirstValue(String.format("%s %s", bdHuman(data.stake), data.coin))
//                viewState.setSecondLabel("To")
//                viewState.setSecondValue(data.publicKey.toString())
            }
            OperationType.Unbound -> {
                val data = tx.getData(TxUnbound::class.java)

                val rows = InputFieldRow.MultiBuilder()
                        .add {
                            label = "You're unbonding"
                            text = data.coin
                        }
                        .add {
                            label = "Amount"
                            text = data.value.humanize()
                        }
                        .add {
                            label = "From"
                            text = data.publicKey.toString()
                        }
                        .build()

                adapter.addRows(rows)

//                viewState.setFirstLabel("You're unbonding")
//                viewState.setFirstValue(String.format("%s %s", bdHuman(data.value), data.coin))
//                viewState.setSecondLabel("From")
//                viewState.setSecondValue(data.publicKey.toString())
            }
            OperationType.RedeemCheck -> {
                val data = tx.getData(TxRedeemCheck::class.java)
                val check = data.decodedCheck
                val rows = InputFieldRow.MultiBuilder()
                        .add {
                            label = "You're using check"
                            text = data.rawCheck.toString()
                        }
                        .add {
                            label = "Coin"
                            text = check.coin
                        }
                        .add {
                            label = "Amount"
                            text = check.value.humanize()
                        }
                        .build()

                adapter.addRows(rows)

//                viewState.setFirstLabel("You're using check")
//                val check = data.decodedCheck
//                viewState.setFirstValue(String.format("%s %s", check.coin, bdHuman(check.value)))
//                viewState.setSecondVisible(View.GONE)
            }
            OperationType.SetCandidateOnline -> {
                val data = tx.getData(TxSetCandidateOnline::class.java)
                val rows = InputFieldRow.MultiBuilder()
                        .add {
                            label = "You're switching on candidate"
                            text = data.publicKey.toString()
                        }
                        .build()

                adapter.addRows(rows)

//                viewState.setFirstLabel("You're switching on candidate")
//                viewState.setFirstValue(data.publicKey.toString())
//                viewState.setSecondVisible(View.GONE)
            }
            OperationType.SetCandidateOffline -> {
                val data = tx.getData(TxSetCandidateOffline::class.java)
                val rows = InputFieldRow.MultiBuilder()
                        .add {
                            label = "You're switching off candidate"
                            text = data.publicKey.toString()
                        }
                        .build()

                adapter.addRows(rows)
//                viewState.setFirstLabel("You're switching off candidate")
//                viewState.setFirstValue(data.publicKey.toString())
//                viewState.setSecondVisible(View.GONE)
            }
            OperationType.Multisend -> {
                val data = tx.getData(TxMultisend::class.java)

                val rowsBuilder = InputFieldRow.MultiBuilder()
                        .add {
                            label = "You're multi-sending"
                            text = "To ${data.items.size} recipients"
                        }

                for (item in data.items) {
                    rowsBuilder.add {
                        label = "${item.to}"
                        text = "${item.value.humanize()} ${item.coin}"
                    }
                }

                adapter.addRows(rowsBuilder.build())

//                viewState.setFirstLabel("You're multi-sending")
//                val sb = StringBuilder()
//                for (item in data.items) {
//                    sb.append(item.to.toShortString()).append(" <- ").append(bdHuman(item.value)).append('\n')
//                }
//                viewState.setFirstValue(sb.toString())
//                viewState.setSecondVisible(View.GONE)
            }
            OperationType.CreateMultisigAddress -> {
                val data = tx.getData<TxCreateMultisigAddress>()

                val rowsBuilder = InputFieldRow.MultiBuilder()
                        .add {
                            label = "You're creating MultiSig address"
                            text = "With ${data.addresses.size} addresses"
                        }
                        .add {
                            label = "Threshold"
                            text = data.threshold.toString()
                        }

                for ((i, item) in data.addresses.withIndex()) {
                    val weight = data.weights[i]
                    rowsBuilder.add {
                        label = "$item"
                        text = "Weight: $weight"
                    }
                }

                adapter.addRows(rowsBuilder.build())

//                viewState.setFirstLabel("You're creating MultiSig address")
//                val sb = StringBuilder()
//                var i = 0
//                while (i < data.addresses.size) {
//                    val address = data.addresses[i]
//                    val weight = data.weights[i]
//                    sb.append("Weight: ").append(weight).append(" - ").append(address.toShortString()).append('\n')
//                    i++
//                }
//                viewState.setFirstValue(sb.toString())
//                viewState.setSecondLabel("Threshold")
//                viewState.setSecondValue(data.threshold.toString())
//                viewState.setSecondVisible(View.VISIBLE)
            }
            OperationType.EditCandidate -> {
                val data = tx.getData(TxEditCandidate::class.java)
                val rows = InputFieldRow.MultiBuilder()
                        .add {
                            label = "You're editing candidate"
                            text = data.pubKey.toString()
                        }
                        .add {
                            label = "Owner address"
                            text = data.ownerAddress.toString()
                        }
                        .add {
                            label = "Reward address"
                            text = data.rewardAddress.toString()
                        }
                        .build()

                adapter.addRows(rows)

//                viewState.setFirstLabel("You're editing candidate")
//                viewState.setFirstValue(data.pubKey.toString())
//                viewState.setSecondLabel("Info")
//                viewState.setSecondValue(String.format("Owner address: %s\nReward address: %s",
//                        data.ownerAddress.toShortString(),
//                        data.rewardAddress.toShortString()
//                ))
            }
            else -> {
                viewState.startDialog(false) { ctx: Context? ->
                    ConfirmDialog.Builder(ctx!!, "Unable to send")
                            .setText("Wallet doesn't support this type of transaction: %s", tx.type.name)
                            .setPositiveAction(R.string.btn_close)
                            .create()
                }
            }
        }

        if (tx.payload.size() > 0) {
            adapter.addRow(InputFieldRow.Builder().apply {
                label = res.getString(R.string.label_payload_explicit)
                text = tx.payload.stringValue()
            }.build())
        }

        viewState.setAdapter(adapter)
    }

    private fun startExecuteTransaction() {
        viewState.startDialog(false) { ctx: Context? ->
            val dialog = WalletProgressDialog.Builder(ctx, R.string.please_wait)
                    .setText(R.string.tx_send_in_progress)
                    .create()
            dialog.setCancelable(false)
            val initData: Observable<TxInitData>
            initData = if (mExtTx!!.nonce != null && mExtTx!!.nonce != BigInteger.ZERO) {
                val d = TxInitData(mExtTx!!.nonce, mExtTx!!.gasPrice)
                Observable.just(d)
            } else {
                getTxInitData(mFrom)
            }
            val d = initData
                    .switchMap(Function<TxInitData, ObservableSource<GateResult<TransactionSendResult>>> { cntRes: TxInitData ->
                        // if in previous request we've got error, returning it
                        if (!cntRes.isSuccess) {
                            return@Function Observable.just(copyError<TransactionSendResult>(cntRes.errorResult))
                        }
                        val tx = Transaction.Builder(cntRes.nonce, mExtTx)
                                .setGasPrice(if (mExtTx!!.type == OperationType.RedeemCheck) BigInteger.ONE else cntRes.gas)
                                .setPayload(mPayload)
                                .buildFromExternal()
                        val data = secretStorage.getSecret(mFrom!!)
                        val sign = tx.signSingle(data.privateKey)
                        safeSubscribeIoToUi(
                                ReactiveGate.rxGate(gateTxRepo.sendTransaction(sign))
                                        .onErrorResumeNext(ReactiveGate.toGateError())
                        )
                    })
                    .doFinally { onExecuteComplete() }
                    .subscribe({ result: GateResult<TransactionSendResult> -> onSuccessExecuteTransaction(result) }) { throwable: Throwable -> onFailedExecuteTransaction(throwable) }
            unsubscribeOnDestroy(d)
            dialog
        }
    }

    private fun onErrorExecuteTransaction(errorResult: GateResult<*>) {
        Timber.e(errorResult.message, "Unable to send transaction")
        viewState.startDialog { ctx: Context? ->
            ConfirmDialog.Builder(ctx!!, "Unable to send transaction")
                    .setText(errorResult.message)
                    .setPositiveAction("Close")
                    .create()
        }
    }

    private fun onFailedExecuteTransaction(throwable: Throwable) {
        Timber.w(throwable, "Uncaught tx error")
        viewState.startDialog { ctx: Context? ->
            ConfirmDialog.Builder(ctx!!, "Unable to send transaction")
                    .setText(throwable.message)
                    .setPositiveAction("Close")
                    .create()
        }
    }

    private fun onSuccessExecuteTransaction(result: GateResult<TransactionSendResult>) {
        if (!result.isOk) {
            onErrorExecuteTransaction(result)
            return
        }
        accountStorage.update(true)
        cachedTxRepo.update(true)
        viewState.startDialog(false) { ctx: Context? ->
            val builder = ConfirmDialog.Builder(ctx!!, "Success!")
                    .setText("Transaction successfully sent")
                    .setPositiveAction("View transaction") { d, _ ->
                        Wallet.app().sounds().play(R.raw.click_pop_zap)
                        viewState.startExplorer(result.result.txHash.toString())
                        d.dismiss()
                        viewState.finishSuccess()
                    }
                    .setNegativeAction("Close") { d, _ ->
                        d.dismiss()
                        viewState.finishSuccess()
                    }
            builder.create()
        }
    }

    private fun onExecuteComplete() {}
    private fun onSubmit() {
        viewState.startDialog { ctx: Context? ->
            ConfirmDialog.Builder(ctx!!, "Sign transaction")
                    .setText("Press \"Confirm and send\" to sign and send transaction to the network, or Cancel to dismiss")
                    .setPositiveAction(R.string.btn_confirm_send) { _, _ ->
                        startExecuteTransaction()
                    }
                    .setNegativeAction(R.string.btn_cancel)
                    .create()
        }
    }

    private fun onCancel() {
        viewState.finishCancel()
    }
}