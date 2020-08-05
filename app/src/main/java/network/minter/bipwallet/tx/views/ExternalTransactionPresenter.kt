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

import android.app.Activity
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
import io.reactivex.functions.Consumer
import io.reactivex.functions.Function
import io.reactivex.schedulers.Schedulers
import moxy.InjectViewState
import network.minter.bipwallet.R
import network.minter.bipwallet.apis.explorer.RepoTransactions
import network.minter.bipwallet.apis.reactive.ReactiveGate
import network.minter.bipwallet.apis.reactive.rxGate
import network.minter.bipwallet.exchange.ui.ConvertCoinActivity
import network.minter.bipwallet.internal.Wallet
import network.minter.bipwallet.internal.common.Preconditions.firstNonNull
import network.minter.bipwallet.internal.dialogs.ConfirmDialog
import network.minter.bipwallet.internal.dialogs.WalletProgressDialog
import network.minter.bipwallet.internal.exceptions.InvalidExternalTransaction
import network.minter.bipwallet.internal.helpers.DeepLinkHelper
import network.minter.bipwallet.internal.helpers.HtmlCompat
import network.minter.bipwallet.internal.helpers.MathHelper.clamp
import network.minter.bipwallet.internal.helpers.MathHelper.humanize
import network.minter.bipwallet.internal.mvp.MvpBasePresenter
import network.minter.bipwallet.internal.storage.RepoAccounts
import network.minter.bipwallet.internal.storage.SecretStorage
import network.minter.bipwallet.internal.storage.models.AddressListBalancesTotal
import network.minter.bipwallet.internal.views.list.multirow.MultiRowAdapter
import network.minter.bipwallet.sending.account.selectorDataFromSecrets
import network.minter.bipwallet.sending.ui.dialogs.TxSendSuccessDialog
import network.minter.bipwallet.services.livebalance.RTMService
import network.minter.bipwallet.services.livebalance.ServiceConnector
import network.minter.bipwallet.services.livebalance.broadcast.RTMBalanceUpdateReceiver
import network.minter.bipwallet.services.livebalance.broadcast.RTMBlockReceiver
import network.minter.bipwallet.tx.contract.ExternalTransactionView
import network.minter.bipwallet.tx.contract.TxInitData
import network.minter.bipwallet.tx.ui.ExternalTransactionActivity
import network.minter.bipwallet.tx.ui.InputFieldRow
import network.minter.bipwallet.tx.ui.WalletSelectorDialog
import network.minter.blockchain.models.operational.*
import network.minter.blockchain.utils.Base64UrlSafe
import network.minter.core.MinterSDK
import network.minter.core.crypto.BytesData
import network.minter.core.crypto.MinterAddress
import network.minter.core.internal.helpers.StringHelper
import network.minter.core.util.RLPBoxed
import network.minter.explorer.models.GasValue
import network.minter.explorer.models.GateResult
import network.minter.explorer.models.GateResult.copyError
import network.minter.explorer.models.PushResult
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

private data class BuyRequiredAmount(
        var coin: String,
        var amount: BigDecimal
)


@InjectViewState
class ExternalTransactionPresenter @Inject constructor() : MvpBasePresenter<ExternalTransactionView>() {

    companion object {
        const val REQUEST_EXCHANGE_COINS = 6000
    }

    @Inject lateinit var secretStorage: SecretStorage
    @Inject lateinit var estimateRepo: GateEstimateRepository
    @Inject lateinit var gasRepo: GateGasRepository
    @Inject lateinit var gateTxRepo: GateTransactionRepository
    @Inject lateinit var accountStorage: RepoAccounts
    @Inject lateinit var cachedTxRepo: RepoTransactions
    @Inject lateinit var res: Resources

    private var extTx: ExternalTransaction? = null
    private var from: MinterAddress? = null
    private var payload: BytesData? = null
    private var checkPassword: String? = null
    private var gasPrice = BigInteger.ONE
    private val adapter = MultiRowAdapter()
    private var isWsBound = false
    private var buyRequiredAmount: BuyRequiredAmount? = null

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()
        isWsBound = ServiceConnector.isBound()
        if (!isWsBound) {
            ServiceConnector.bind(Wallet.app().context())
            ServiceConnector.onConnected()
                    .subscribe(
                            { res: RTMService ->
                                res.setOnMessageListener { message: String?, channel: String, address: MinterAddress? ->
                                    if (channel == RTMService.CHANNEL_BLOCKS) {
                                        RTMBlockReceiver.send(Wallet.app().context(), message!!)
                                    } else {
                                        RTMBalanceUpdateReceiver.send(Wallet.app().context(), message)
                                        accountStorage.update(true, Consumer<AddressListBalancesTotal> { account: AddressListBalancesTotal? -> Wallet.app().balanceNotifications().showBalanceUpdate(message, address) })
                                        cachedTxRepo.update(true)
                                        Timber.d("WS ON MESSAGE[%s]: %s", channel, message)
                                    }
                                }
                            }
                    ) { t: Throwable -> Timber.w(t, "Unable to connect to RTM service") }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!isWsBound) {
            ServiceConnector.release(Wallet.app().context())
        }
        from = null
    }

    override fun attachView(view: ExternalTransactionView) {
        super.attachView(view)
        accountStorage.observe()
                .subscribe(
                        {
                            validateTx()
                        },
                        { t -> Timber.w(t) }
                )
                .disposeOnDestroy()

        viewState.setOnCancelListener(View.OnClickListener { onCancel() })
    }

    fun resetAccount() {
        from = null
    }

    override fun handleExtras(intent: Intent?) {
        super.handleExtras(intent)

        if (intent == null) {
            viewState.startDialog { ctx ->
                ConfirmDialog.Builder(ctx, R.string.error)
                        .setText("Unable to send deeplink: no data were passed")
                        .setPositiveAction(R.string.btn_close) { d, _ ->
                            d.dismiss()
                            viewState.finishCancel()
                        }
                        .create()
            }
            return
        }

        if (from == null) {
            if (secretStorage.secretsSize == 1) {
                from = secretStorage.addresses[0]
                init(intent)
            } else {
                viewState.startDialogFragment { ctx ->
                    WalletSelectorDialog.Builder(ctx, R.string.dialog_title_choose_wallet)
                            .setItems(selectorDataFromSecrets(secretStorage.secretsListSafe))
                            .setPositiveAction(R.string.btn_continue) { d, _ ->
                                from = (d as WalletSelectorDialog).item!!.data.minterAddress
                                init(intent)
                                d.dismiss()
                            }
                            .setNegativeAction(R.string.btn_cancel) { d, _ ->
                                viewState.finishCancel()
                                d.dismiss()
                            }
                            .create()
                }
            }
        } else {
            init(intent)
        }
    }

    private fun init(intent: Intent) {
        if (intent.getBooleanExtra(DeepLink.IS_DEEP_LINK, false)) {
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
                try {
                    hash = BytesData(
                            Base64UrlSafe.decode(params.getString("data", null).toByteArray())
                    ).toHexString()
                } catch (e: Throwable) {
                    viewState.disableAll()
                    showTxErrorDialog("Unable to parse deeplink: %s", e.message!!)
                    return
                }
            }
            if (params.containsKey("p")) {
                checkPassword = try {
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
                extTx = DeepLinkHelper.parseRawTransaction(hash)
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
        payload = extTx?.payload

        if (extTx != null) {
            calculateFee(extTx!!)
        }

        try {
            fillData(extTx!!)
        } catch (t: Throwable) {
            showTxErrorDialog("Invalid transaction data: %s", t.message!!)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_EXCHANGE_COINS) {

            if (resultCode == Activity.RESULT_OK) {
                if (buyRequiredAmount != null) {
                    val exchangedAmount = ConvertCoinActivity.getResult(data!!)

                    if (exchangedAmount.coin == buyRequiredAmount!!.coin) {
                        if (exchangedAmount.amount >= buyRequiredAmount!!.amount) {
                            Timber.d("Exchanged enough coins: ${exchangedAmount.amount} ${exchangedAmount.coin}")
                            viewState.hideExchangeBanner()
                        } else {
                            Observable
                                    .create<Boolean> { emitter ->
                                        var run = true
                                        var success = false
                                        var i = 0
                                        do {
                                            if (i == 30) {
                                                run = false
                                                break
                                            }
                                            accountStorage.update(true, Consumer { balance ->
                                                if (balance.getCoinBalance(from!!, exchangedAmount.coin) >= exchangedAmount.amount) {
                                                    Timber.d("Success found balance after buy required coins!")
                                                    run = false
                                                    success = true
                                                }
                                            }, Consumer { err -> Timber.w(err, "Unable to update balance!") })

                                            i++
                                            if (i > 1) {
                                                viewState.showWaitProgress()
                                            }
                                            Thread.sleep(1000)

                                        } while (run)

                                        viewState.hideWaitProgress()
                                        emitter.onNext(success)
                                        emitter.onComplete()
                                    }
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribeOn(Schedulers.io())
                                    .subscribe(
                                            {
                                                validateEnoughCoins()
                                            },
                                            { t -> Timber.w(t, "Unable to fetch required balance for external transaction after 30 attempts") }
                                    )
                        }
                    }
                }
            }
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
        if (extTx == null) {
            return false
        }
        val type = extTx!!.type
        validateEnoughCoins()
        if (type == OperationType.RedeemCheck) {
            val d = extTx!!.getData<TxRedeemCheck>()
            if (checkPassword != null) {
                d.proof = CheckTransaction.makeProof(from, checkPassword)
            } else if (d.proof.size() == 0 && checkPassword == null) {
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
        }
        return true
    }

    private fun getCoinAndAmountFromTx(type: OperationType): BuyRequiredAmount? {
        val out: BuyRequiredAmount?
        when (type) {
            OperationType.SendCoin -> {
                val d = extTx!!.getData<TxSendCoin>()
                out = BuyRequiredAmount(d.coin, d.value)
            }
            OperationType.SellCoin -> {
                val d = extTx!!.getData<TxCoinSell>()
                out = BuyRequiredAmount(d.coinToSell, d.valueToSell)
            }
            OperationType.Delegate -> {
                val d = extTx!!.getData<TxDelegate>()
                out = BuyRequiredAmount(d.coin, d.stake)
            }
            else -> {
                out = null
            }
        }

        return out
    }

    private fun validateEnoughCoins() {
        val type = extTx!!.type
        val txValues = getCoinAndAmountFromTx(type)

        if (txValues != null) {
            val balance = accountStorage.data.getBalance(from!!)
            val coinBalanceSearch = balance.findCoinByName(txValues.coin)

            var balanceSum: BigDecimal = BigDecimal.ZERO
            if (coinBalanceSearch.isPresent) {
                balanceSum = coinBalanceSearch.get().amount
            }
            val notEnough: BigDecimal = txValues.amount - balanceSum

            if (notEnough > BigDecimal.ZERO) {
                buyRequiredAmount = BuyRequiredAmount(txValues.coin, notEnough)
                viewState.showExchangeBanner(
                        HtmlCompat.fromHtml(Wallet.app().res().getString(R.string.description_external_not_enough_coins, notEnough, txValues.coin))
                ) {
                    viewState.startExchangeCoins(REQUEST_EXCHANGE_COINS, txValues.coin, notEnough, from!!)
                }
                return
            }
        }

        buyRequiredAmount = null
        viewState.hideExchangeBanner()
    }

    private fun calculateFee(tx: ExternalTransaction) {
        val bytesLen = firstNonNull(payload, tx.payload, BytesData(CharArray(0))).size().toLong()
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
        fee = fee.multiply(BigDecimal(gasPrice))
        viewState.setFee(String.format("%s %s", fee, MinterSDK.DEFAULT_COIN))
    }

    private fun fillData(tx: ExternalTransaction) {
        adapter.clear()

//        viewState.setPayload(tx.payloadString)
        gasRepo.minGas
                .rxGate()
                .subscribeOn(Schedulers.io())
                .toFlowable(BackpressureStrategy.LATEST)
                .debounce(200, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { res: GateResult<GasValue> ->
                            if (res.isOk) {
                                gasPrice = res.result.gas
                                Timber.d("Min Gas price: %s", gasPrice.toString())
                                calculateFee(tx)
                            }
                        },
                        { t: Throwable ->
                            Timber.w(t)
                        }
                )
                .disposeOnDestroy()

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
                        .add {
                            label = "Minimum Amount To Get"
                            text = data.minValueToBuy.humanize()
                        }
                        .build()

                adapter.addRows(rows)
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
                        .add {
                            label = "Minimum Amount To Get"
                            text = data.minValueToBuy.humanize()
                        }
                        .build()

                adapter.addRows(rows)
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
                        .add {
                            label = "Maximum Amount To Spend"
                            text = data.maxValueToSell.humanize()
                        }
                        .build()

                adapter.addRows(rows)
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
                            text = if (data.maxSupply < BigDecimal("10").pow(15)) {
                                data.maxSupply.humanize()
                            } else {
                                "10¹⁵ (max)"
                            }
                        }
                        .build()

                adapter.addRows(rows)
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
        viewState.startDialog(false) { ctx ->
            val dialog = WalletProgressDialog.Builder(ctx, R.string.please_wait)
                    .setText(R.string.tx_send_in_progress)
                    .create()

            dialog.setCancelable(false)
            val initData: Observable<TxInitData>
            initData = if (extTx!!.nonce != null && extTx!!.nonce != BigInteger.ZERO) {
                val d = TxInitData(extTx!!.nonce, extTx!!.gasPrice)
                Observable.just(d)
            } else {
                getTxInitData(from)
            }
            val d = initData
                    .joinToUi()
                    .switchMap(Function<TxInitData, ObservableSource<GateResult<PushResult>>> { cntRes: TxInitData ->
                        // if in previous request we've got error, returning it
                        if (!cntRes.isSuccess) {
                            return@Function Observable.just(copyError<PushResult>(cntRes.errorResult))
                        }
                        val tx = Transaction.Builder(cntRes.nonce, extTx)
                                .setGasPrice(if (extTx!!.type == OperationType.RedeemCheck) BigInteger.ONE else (cntRes.gas
                                        ?: BigInteger.ONE))
                                .setPayload(payload)
                                .buildFromExternal()
                        val data = secretStorage.getSecret(from!!)
                        val sign = tx.signSingle(data.privateKey)!!
                        safeSubscribeIoToUi(
                                ReactiveGate.rxGate(gateTxRepo.sendTransaction(sign))
                                        .onErrorResumeNext(ReactiveGate.toGateError())
                        )
                    })
                    .doFinally { onExecuteComplete() }
                    .subscribe(
                            { result: GateResult<PushResult> -> onSuccessExecuteTransaction(result) },
                            { t -> onFailedExecuteTransaction(t) }
                    )
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

    private fun onSuccessExecuteTransaction(result: GateResult<PushResult>) {
        if (!result.isOk) {
            onErrorExecuteTransaction(result)
            return
        }
        accountStorage.update(true)
        cachedTxRepo.update(true)
        viewState.startDialog(false) { ctx: Context ->
            TxSendSuccessDialog.Builder(ctx)
                    .setLabel("Transaction successfully sent")
                    .setValue(null)
                    .setPositiveAction(R.string.btn_view_tx) { d, _ ->
                        Wallet.app().sounds().play(R.raw.click_pop_zap)
                        viewState.startExplorer(result.result.txHash.toString())
                        d.dismiss()
                        viewState.finishSuccess()
                    }
                    .setNegativeAction(R.string.btn_close) { d, _ ->
                        d.dismiss()
                        viewState.finishSuccess()
                    }
                    .create()
        }
    }

    private fun onExecuteComplete() {}

    private fun onSubmit() {
        viewState.startDialog { ctx ->
            ConfirmDialog.Builder(ctx, R.string.dialog_title_confirm_transaction)
                    .setText(R.string.dialog_description_external_tx_short)
                    .setPositiveAction(R.string.btn_confirm) { d, _ ->
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