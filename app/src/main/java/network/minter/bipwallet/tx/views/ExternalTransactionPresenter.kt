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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Resources
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
import network.minter.bipwallet.apis.gate.TxInitDataRepository
import network.minter.bipwallet.apis.reactive.ReactiveGate
import network.minter.bipwallet.apis.reactive.castErrorResultTo
import network.minter.bipwallet.apis.reactive.nameValueMap
import network.minter.bipwallet.coins.CoinMapper
import network.minter.bipwallet.coins.RepoCoins
import network.minter.bipwallet.exchange.ui.ConvertCoinActivity
import network.minter.bipwallet.internal.Wallet
import network.minter.bipwallet.internal.common.Preconditions.firstNonNull
import network.minter.bipwallet.internal.dialogs.ConfirmDialog
import network.minter.bipwallet.internal.dialogs.WalletProgressDialog
import network.minter.bipwallet.internal.exceptions.ErrorManager
import network.minter.bipwallet.internal.exceptions.InvalidExternalTransaction
import network.minter.bipwallet.internal.exceptions.RetryListener
import network.minter.bipwallet.internal.helpers.DeepLinkHelper
import network.minter.bipwallet.internal.helpers.HtmlCompat
import network.minter.bipwallet.internal.helpers.MathHelper.clamp
import network.minter.bipwallet.internal.helpers.MathHelper.humanize
import network.minter.bipwallet.internal.helpers.MathHelper.humanizeDecimal
import network.minter.bipwallet.internal.helpers.MathHelper.parseBigDecimal
import network.minter.bipwallet.internal.helpers.MathHelper.toPlain
import network.minter.bipwallet.internal.helpers.NetworkHelper
import network.minter.bipwallet.internal.helpers.ViewExtensions.tr
import network.minter.bipwallet.internal.helpers.forms.validators.NumberNotZeroValidator
import network.minter.bipwallet.internal.helpers.forms.validators.PayloadValidator
import network.minter.bipwallet.internal.mvp.MvpBasePresenter
import network.minter.bipwallet.internal.storage.AccountStorage
import network.minter.bipwallet.internal.storage.RepoAccounts
import network.minter.bipwallet.internal.storage.SecretStorage
import network.minter.bipwallet.sending.account.selectorDataFromSecrets
import network.minter.bipwallet.sending.ui.dialogs.TxSendSuccessDialog
import network.minter.bipwallet.services.livebalance.RTMService
import network.minter.bipwallet.services.livebalance.ServiceConnector
import network.minter.bipwallet.services.livebalance.broadcast.RTMBalanceUpdateReceiver
import network.minter.bipwallet.services.livebalance.broadcast.RTMBlockReceiver
import network.minter.bipwallet.tx.contract.ExternalTransactionView
import network.minter.bipwallet.tx.contract.TxInitData
import network.minter.bipwallet.tx.ui.ExternalTransactionActivity
import network.minter.bipwallet.tx.ui.TxInputFieldRow
import network.minter.bipwallet.tx.ui.WalletSelectorDialog
import network.minter.blockchain.models.operational.*
import network.minter.blockchain.utils.Base64UrlSafe
import network.minter.core.MinterSDK
import network.minter.core.crypto.BytesData
import network.minter.core.crypto.MinterAddress
import network.minter.core.crypto.MinterPublicKey
import network.minter.explorer.models.GasValue
import network.minter.explorer.models.GateResult
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
        var coinId: BigInteger,
        var amount: BigDecimal
)

/**
 * @TODO: total refactor needed
 */
@InjectViewState
class ExternalTransactionPresenter @Inject constructor() : MvpBasePresenter<ExternalTransactionView>(), ErrorManager.ErrorGlobalHandlerListener {

    companion object {
        const val REQUEST_EXCHANGE_COINS = 6000
    }

    @Inject lateinit var secretStorage: SecretStorage
    @Inject lateinit var estimateRepo: GateEstimateRepository
    @Inject lateinit var gasRepo: GateGasRepository
    @Inject lateinit var gateTxRepo: GateTransactionRepository
    @Inject lateinit var accountStorage: RepoAccounts
    @Inject lateinit var cachedTxRepo: RepoTransactions
    @Inject lateinit var coins: RepoCoins
    @Inject lateinit var coinMapper: CoinMapper
    @Inject lateinit var resources: Resources
    @Inject lateinit var networkHelper: NetworkHelper
    @Inject lateinit var errorManager: ErrorManager
    @Inject lateinit var initDataRepo: TxInitDataRepository

    private var extTx: ExternalTransaction? = null
    private var from: MinterAddress? = null
    private var payload: BytesData? = null
    private var checkPassword: String? = null
    private var gasPrice = BigInteger.ONE
    private var initFeeData: TxInitData? = null
    private var isWsBound = false
    private var buyRequiredAmount: BuyRequiredAmount? = null
    private var enableEditInput = false

    override fun onError(t: Throwable, retryListener: RetryListener) {
        viewState.hideWaitProgress()
        viewState.hideExchangeBanner()
        handlerError(t, retryListener)
    }

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()
        AccountStorage.FORCE_ALL = true
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
                                        accountStorage.update(true, { Wallet.app().balanceNotifications().showBalanceUpdate(message, address) })
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
        AccountStorage.FORCE_ALL = false
        if (!isWsBound) {
            ServiceConnector.release(Wallet.app().context())
        }
        from = null
    }

    override fun attachView(view: ExternalTransactionView) {
        super.attachView(view)
        coins.observe()
                .subscribe {
                    viewState.hideProgress()
                }
        coins.update()
        accountStorage.observe()
                .subscribe(
                        {
                            validateTx()
                        },
                        { t -> Timber.w(t) }
                )
                .disposeOnDestroy()

        viewState.setOnCancelListener { onCancel() }
    }

    fun resetAccount() {
        from = null
    }

    override fun handleExtras(intent: Intent?) {
        super.handleExtras(intent)

        if (intent == null) {
            viewState.startDialog { ctx ->
                ConfirmDialog.Builder(ctx, R.string.error)
                        .setText(R.string.dialog_text_err_unable_to_send_deeplink_no_data)
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
                    val d = WalletSelectorDialog.Builder(ctx, R.string.dialog_title_choose_wallet)
                            .setItems(selectorDataFromSecrets(secretStorage.secretsListSafe))
                            .setPositiveAction(R.string.btn_continue) { d, _ ->
                                from = (d as WalletSelectorDialog).item!!.data.minterAddress
                                secretStorage.setMain(from!!)
                                accountStorage.update(true)
                                init(intent)
                                d.dismiss()
                            }
                            .setNegativeAction(R.string.btn_cancel) { d, _ ->
                                d.dismiss()
                                viewState.finishCancel()
                            }
                            .create()
                    d.isCancelable = false
                    d
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
            val hash: String
            try {
                hash = BytesData(
                        Base64UrlSafe.decode(params.getString("data", null).toByteArray())
                ).toHexString()
            } catch (e: Throwable) {
                viewState.disableAll()
                showTxErrorDialog(tr(R.string.dialog_text_err_unable_to_parse_deeplink, e.message!!))
                return
            }
            if (params.containsKey("p")) {
                checkPassword = try {
                    val rawPass = params.getString("p", null)
                    if (rawPass == null) {
                        null
                    } else {
                        Base64UrlSafe.decodeString(rawPass)
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
                showTxErrorDialog(tr(R.string.dialog_text_err_invalid_tx_data_non_hex_string))
                return
            } catch (t: Throwable) {
                viewState.disableAll()
                Timber.w(t, "Unable to parse remote transaction: %s", hash)
                showTxErrorDialog(tr(R.string.dialog_text_err_invalid_tx_data_format, t.message!!))
                return
            }
        } else {
            val rawTx = intent.getStringExtra(ExternalTransactionActivity.EXTRA_RAW_DATA)
            try {
                if (rawTx == null) {
                    throw InvalidExternalTransaction(tr(R.string.dialog_text_err_empty_transaction_data), InvalidExternalTransaction.CODE_INVALID_TX)
                }
                val out = DeepLinkHelper.rawTxToIntent(rawTx)
                handleExtras(out)
                return
            } catch (t: Throwable) {
                viewState.disableAll()
                Timber.w(t, "Unable to parse remote transaction: %s", rawTx)
                showTxErrorDialog(tr(R.string.dialog_text_err_invalid_tx_data_format, t.message!!))
                return
            }
        }
        payload = extTx?.payload

//        if (extTx != null) {
//            calculateFee(extTx!!)
//        }

        viewState.showProgress()

        coinMapper.resolveAllCoins()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe {
                    try {
                        viewState.hideProgress()
                        Timber.d("Coin mapper cache has been loaded")
                        fillData(extTx!!)

                    } catch (t: Throwable) {
                        viewState.hideProgress()
                        showTxErrorDialog(tr(R.string.dialog_text_err_invalid_tx_data_format, t.message!!))
                    }
                }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_EXCHANGE_COINS) {

            if (resultCode == Activity.RESULT_OK) {
                if (buyRequiredAmount != null) {
                    val exchangedAmount = ConvertCoinActivity.getResult(data!!)

                    if (exchangedAmount.coin.id == buyRequiredAmount!!.coinId) {
                        if (exchangedAmount.amount >= buyRequiredAmount!!.amount) {
                            Timber.d("Exchanged enough coins: ${exchangedAmount.amount} ${exchangedAmount.coin.symbol}")
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
                                            accountStorage.update(true, { balance ->
                                                if (balance.getCoinBalance(from!!, exchangedAmount.coin) >= exchangedAmount.amount) {
                                                    Timber.d("Success found balance after buy required coins!")
                                                    run = false
                                                    success = true
                                                }
                                            }, { err -> Timber.w(err, "Unable to update balance!") })

                                            i++
                                            if (i > 1) {
                                                viewState.showProgress()
                                            }
                                            Thread.sleep(1000)

                                        } while (run)

                                        viewState.hideProgress()
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
        return Observable
                .combineLatest(
                        estimateRepo.getTransactionCount(address!!),
                        gasRepo.minGas,
                        BiFunction { txCountGateResult: GateResult<TxCount>, gasValueGateResult: GateResult<GasValue> ->

                            // if some request failed, returning error result
                            if (!txCountGateResult.isOk) {
                                return@BiFunction TxInitData(txCountGateResult.castErrorResultTo<Any>())
                            } else if (!gasValueGateResult.isOk) {
                                return@BiFunction TxInitData(gasValueGateResult.castErrorResultTo<Any>())
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
            ConfirmDialog.Builder(ctx, R.string.dialog_title_err_unable_to_handle_deeplink)
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
                    ConfirmDialog.Builder(ctx!!, R.string.dialog_title_err_unable_to_handle_deeplink)
                            .setText(R.string.dialog_text_err_check_given_without_proof)
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
                out = BuyRequiredAmount(d.coinId, d.value)
            }
            OperationType.SellCoin -> {
                val d = extTx!!.getData<TxCoinSell>()
                out = BuyRequiredAmount(d.coinIdToSell, d.valueToSell)
            }
            OperationType.SellSwapPool -> {
                val d = extTx!!.getData<TxSwapPoolSell>()
                out = BuyRequiredAmount(d.coinIdToSell, d.valueToSell)
            }
            OperationType.Delegate -> {
                val d = extTx!!.getData<TxDelegate>()
                out = BuyRequiredAmount(d.coinId, d.stake)
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
            val coinBalanceSearch = balance.findCoin(txValues.coinId)

            var balanceSum: BigDecimal = BigDecimal.ZERO
            if (coinBalanceSearch.isPresent) {
                balanceSum = coinBalanceSearch.get().amount
            }
            val notEnough: BigDecimal = txValues.amount - balanceSum

            if (notEnough > BigDecimal.ZERO) {
                buyRequiredAmount = BuyRequiredAmount(txValues.coinId, notEnough)

                //@todo: check it's working

                coinMapper.exist(txValues.coinId)
                        .retryWhen(errorManager.retryWhenHandlerCompletable)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeOn(Schedulers.io())
                        .subscribe(
                                { exist ->
                                    if (exist) {
                                        val coin = coinMapper.getById(txValues.coinId)
                                        if (coin != null) {
                                            viewState.showBannerExchangeText(
                                                    HtmlCompat.fromHtml(Wallet.app().res().getString(R.string.description_external_not_enough_coins, notEnough.toPlain(), coin.symbol
                                                            ?: txValues.coinId.toString()))
                                            ) {
                                                viewState.startExchangeCoins(REQUEST_EXCHANGE_COINS, coin, notEnough, from!!)
                                            }
                                        } else {
                                            viewState.hideExchangeBanner()
                                        }
                                    } else {
                                        viewState.showBannerError(R.string.description_external_unknown_coin_id)
                                    }
                                },
                                { t ->
                                    Timber.w(t, "Unable to check coin exists")

                                }
                        )

                return
            }
        }



        buyRequiredAmount = null
        viewState.hideExchangeBanner()
    }

    private fun calculateFee(tx: ExternalTransaction) {
        val payloadLength = firstNonNull(payload, tx.payload, BytesData(CharArray(0))).size().toLong()
        var baseFee: BigDecimal

        when (tx.type) {
            OperationType.Multisend -> {
                val txData = tx.getData(TxMultisend::class.java)
                if (txData.items.size == 0) {
                    onFailedExecuteTransaction(Exception(tr(R.string.deeplink_err_empty_multisend_recipients)))
                    return
                }

                //10+(n-1)*5 units
//                baseFee = OperationType.SendCoin.fee
                baseFee = initFeeData!!.priceCommissions.multisendBase.humanizeDecimal()
                baseFee += (
                        clamp(txData.items.size - 1, 0).toBigDecimal() * initFeeData!!.priceCommissions.multisendDelta.humanizeDecimal()
                        )
            }
            OperationType.CreateCoin,
            OperationType.CreateToken -> {
                // https://docs.minter.network/#section/Commissions
                val symbol = if (tx.type == OperationType.CreateCoin) {
                    tx.getData(TxCoinCreate::class.java).symbol
                } else {
                    tx.getData(TxTokenCreate::class.java).symbol
                }
//                baseFee = TxCoinCreate.calculateCreatingCost(txData.symbol)
                baseFee = initFeeData!!.priceCommissions.calculateCoinCost(symbol).humanizeDecimal()
            }
            OperationType.RedeemCheck -> {
                viewState.setFee(tr(R.string.deeplink_check_no_redeem_fee))
                return
            }
            else -> {
//                baseFee = tx.type.fee
                baseFee = initFeeData!!.priceCommissions.getByType(tx.type).humanizeDecimal()
            }
        }

        // add to fee payload length fee (each byte is 0.200 units)
        var fee = baseFee + (BigDecimal(payloadLength) * initFeeData!!.payloadFee)
        fee *= gasPrice.toBigDecimal()
        if (initFeeData?.gasRepresentingCoin?.id != MinterSDK.DEFAULT_COIN_ID) {
            viewState.setFee(String.format("%s %s (%s %s)",
                    fee.multiply(initFeeData!!.gasBaseCoinRate).humanize(),
                    MinterSDK.DEFAULT_COIN,
                    fee.humanize(),
                    initFeeData!!.gasRepresentingCoin.symbol
            ))
        } else {
            viewState.setFee(String.format("%s %s", fee.humanize(), MinterSDK.DEFAULT_COIN))
        }

    }

    fun toggleEditing() {
        enableEditInput = !enableEditInput
        fillData(extTx!!)
    }

    private fun fillData(tx: ExternalTransaction) {
        initDataRepo.loadFeeWithTx()
                .subscribeOn(Schedulers.io())
                .toFlowable(BackpressureStrategy.LATEST)
                .debounce(200, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { res: TxInitData ->
                            gasPrice = res.gas
                            initFeeData = res
                            Timber.d("Min Gas price: %s", gasPrice.toString())
                            calculateFee(tx)

                            viewState.setOnConfirmListener { onSubmit() }

                            val allRows: MutableList<TxInputFieldRow<*>> = ArrayList()

                            when (tx.type) {
                                OperationType.SendCoin -> {
                                    viewState.enableEditAction(true)
                                    val data = tx.getData(TxSendCoin::class.java)
                                    val rows = TxInputFieldRow.MultiBuilder(TxSendCoin::class.java, extTx!!)
                                            .onValid { viewState.enableSubmit(it) }
                                            .add {
                                                label = tr(R.string.deeplink_label_youre_sending)
                                                //todo: map coin from db
                                                text = coinMapper.idToSymbolDef(data.coinId)
                                                enabled = enableEditInput
                                                tplCoin(
                                                        { data, value ->
                                                            //todo: map coin from db
                                                            data.coinId = coinMapper[value]
                                                        },
                                                        {
                                                            validateEnoughCoins()
                                                        }
                                                )
                                            }
                                            .add {
                                                label = tr(R.string.deeplink_label_amount)
                                                text = data.value.toPlain()
                                                enabled = enableEditInput
                                                tplDecimal({ data, value ->
                                                    data.setValue(value)
                                                }, { validateEnoughCoins() })
                                            }
                                            .add {
                                                label = tr(R.string.deeplink_label_to)
                                                text = data.to.toString()
                                                enabled = enableEditInput
                                                tplAddress { txSendCoin, s ->
                                                    txSendCoin.setTo(s)
                                                }
                                            }
                                            .build()
                                    allRows.addAll(rows)
                                }
                                OperationType.SellCoin -> {
                                    viewState.enableEditAction(true)
                                    val data = tx.getData(TxCoinSell::class.java)
                                    val rows = TxInputFieldRow.MultiBuilder(TxCoinSell::class.java, extTx!!)
                                            .add {
                                                label = tr(R.string.deeplink_label_send_youre_selling)
                                                //todo: map coin from db
                                                text = coinMapper.idToSymbolDef(data.coinIdToSell)
                                                enabled = enableEditInput
                                                tplCoin({ txCoinSell, s ->
                                                    //todo: map coin from db
                                                    txCoinSell.coinIdToSell = coinMapper[s]
                                                }, { validateEnoughCoins() })
                                            }
                                            .add {
                                                label = tr(R.string.deeplink_label_amount)
                                                text = data.valueToSell.toPlain()
                                                enabled = enableEditInput
                                                tplDecimal({ txCoinSell, s ->
                                                    txCoinSell.valueToSell = s.parseBigDecimal()
                                                }, { validateEnoughCoins() })
                                            }
                                            .add {
                                                label = tr(R.string.deeplink_label_coin_to_buy)
                                                //todo: map coin from db
                                                text = coinMapper.idToSymbolDef(data.coinIdToBuy)
                                                enabled = enableEditInput
                                                tplCoin { txCoinSell, s ->
                                                    //todo: map coin from db
                                                    txCoinSell.coinIdToBuy = coinMapper[s]
                                                }
                                            }
                                            .add {
                                                label = tr(R.string.deeplink_label_min_amount_to_get)
                                                text = data.minValueToBuy.toPlain()
                                                enabled = enableEditInput
                                                tplDecimal { txCoinSell, s ->
                                                    txCoinSell.minValueToBuy = s.parseBigDecimal()
                                                }
                                            }
                                            .onValid { viewState.enableSubmit(it) }
                                            .build()
                                    allRows.addAll(rows)
                                }
                                OperationType.SellSwapPool -> {
                                    val data = tx.getData(TxSwapPoolSell::class.java)
                                    if (data.coins.size == 2) {
                                        viewState.enableEditAction(true)
                                        val rows = TxInputFieldRow.MultiBuilder(TxSwapPoolSell::class.java, extTx!!)
                                                .add {
                                                    label = tr(R.string.deeplink_label_youre_selling_pool)
                                                    //todo: map coin from db
                                                    text = coinMapper.idToSymbolDef(data.coinIdToSell)
                                                    enabled = enableEditInput
                                                    tplCoin({ txCoinSell, s ->
                                                        //todo: map coin from db
                                                        txCoinSell.coins[0] = coinMapper[s]
//                                txCoinSell.coinIdToSell = coinMapper[s]
                                                    }, { validateEnoughCoins() })
                                                }
                                                .add {
                                                    label = tr(R.string.deeplink_label_amount)
                                                    text = data.valueToSell.toPlain()
                                                    enabled = enableEditInput
                                                    tplDecimal({ txCoinSell, s ->
                                                        txCoinSell.valueToSell = s.parseBigDecimal()
                                                    }, { validateEnoughCoins() })
                                                }
                                                .add {
                                                    label = tr(R.string.deeplink_label_coin_to_buy)
                                                    //todo: map coin from db
                                                    text = coinMapper.idToSymbolDef(data.coinIdToBuy)
                                                    enabled = enableEditInput
                                                    tplCoin { txCoinSell, s ->
                                                        //todo: map coin from db
                                                        txCoinSell.coins[1] = coinMapper[s]
                                                    }
                                                }
                                                .add {
                                                    label = tr(R.string.deeplink_label_min_amount_to_get)
                                                    text = data.minValueToBuy.toPlain()
                                                    enabled = enableEditInput
                                                    tplDecimal { txCoinSell, s ->
                                                        txCoinSell.minValueToBuy = s.parseBigDecimal()
                                                    }
                                                }
                                                .onValid { viewState.enableSubmit(it) }
                                                .build()
                                        allRows.addAll(rows)
                                    } else {
                                        viewState.enableEditAction(false)
                                        val rows = TxInputFieldRow.MultiBuilder(TxSwapPoolSell::class.java, extTx!!)
                                        data.coins.forEachIndexed { idx, value ->
                                            rows.add {
                                                label = "${tr(R.string.deeplink_label_coin_prefix)} ${idx + 1}"
                                                text = coinMapper.idToSymbolDef(value)
                                            }
                                        }
                                        rows.add {
                                            label = tr(R.string.deeplink_label_amount)
                                            text = data.valueToSell.toPlain()
                                        }
                                        rows.add {
                                            label = tr(R.string.deeplink_label_min_amount_to_get)
                                            text = data.minValueToBuy.toPlain()
                                        }
                                        allRows.addAll(rows.build())
                                    }
                                }
                                OperationType.SellAllSwapPool -> {
                                    val data = tx.getData(TxSwapPoolSellAll::class.java)
                                    if (data.coins.size == 2) {
                                        viewState.enableEditAction(true)
                                        val rows = TxInputFieldRow.MultiBuilder(TxSwapPoolSellAll::class.java, extTx!!)
                                                .add {
                                                    label = tr(R.string.deeplink_label_youre_selling_all_pool)
                                                    //todo: map coin from db
                                                    text = coinMapper.idToSymbolDef(data.coinIdToSell)
                                                    enabled = enableEditInput
                                                    tplCoin({ txCoinSell, s ->
                                                        //todo: map coin from db
                                                        txCoinSell.coins[0] = coinMapper[s]
//                                txCoinSell.coinIdToSell = coinMapper[s]
                                                    }, { validateEnoughCoins() })
                                                }
                                                .add {
                                                    label = tr(R.string.deeplink_label_coin_to_buy)
                                                    //todo: map coin from db
                                                    text = coinMapper.idToSymbolDef(data.coinIdToBuy)
                                                    enabled = enableEditInput
                                                    tplCoin { txCoinSell, s ->
                                                        //todo: map coin from db
                                                        txCoinSell.coins[1] = coinMapper[s]
                                                    }
                                                }
                                                .add {
                                                    label = tr(R.string.deeplink_label_min_amount_to_get)
                                                    text = data.minValueToBuy.toPlain()
                                                    enabled = enableEditInput
                                                    tplDecimal { txCoinSell, s ->
                                                        txCoinSell.minValueToBuy = s.parseBigDecimal()
                                                    }
                                                }
                                                .onValid { viewState.enableSubmit(it) }
                                                .build()
                                        allRows.addAll(rows)
                                    } else {
                                        viewState.enableEditAction(false)
                                        val rows = TxInputFieldRow.MultiBuilder(TxSwapPoolSell::class.java, extTx!!)
                                        data.coins.forEachIndexed { idx, value ->
                                            rows.add {
                                                label = "${tr(R.string.deeplink_label_coin_prefix)} ${idx + 1}"
                                                text = coinMapper.idToSymbolDef(value)
                                            }
                                        }
                                        rows.add {
                                            label = tr(R.string.deeplink_label_min_amount_to_get)
                                            text = data.minValueToBuy.toPlain()
                                        }
                                        allRows.addAll(rows.build())
                                    }
                                }
                                OperationType.SellAllCoins -> {
                                    viewState.enableEditAction(true)
                                    val data = tx.getData(TxCoinSellAll::class.java)
                                    val rows = TxInputFieldRow.MultiBuilder(TxCoinSellAll::class.java, extTx!!)
                                            .add {
                                                label = tr(R.string.deeplink_label_youre_selling_all)
                                                //todo: map coin from db
                                                text = coinMapper.idToSymbolDef(data.coinIdToSell)
                                                enabled = enableEditInput
                                                tplCoin { txCoinSellAll, value ->
                                                    //todo: map coin from db
                                                    txCoinSellAll.coinIdToSell = coinMapper[value]
//                                txCoinSellAll.coinToSell = value
                                                }
                                            }
                                            .add {
                                                label = tr(R.string.deeplink_label_coin_to_buy)
                                                //todo: map coin from db
                                                text = coinMapper.idToSymbolDef(data.coinIdToBuy)
                                                enabled = enableEditInput
                                                tplCoin { txCoinSellAll, value ->
                                                    //todo: map coin from db
                                                    txCoinSellAll.coinIdToBuy = coinMapper[value]
                                                }
                                            }
                                            .add {
                                                label = tr(R.string.deeplink_label_min_amount_to_get)
                                                text = data.minValueToBuy.toPlain()
                                                enabled = enableEditInput
                                                tplDecimal { txCoinSellAll, value ->
                                                    txCoinSellAll.minValueToBuy = value.parseBigDecimal()
                                                }
                                            }
                                            .onValid { viewState.enableSubmit(it) }
                                            .build()
                                    allRows.addAll(rows)
                                }
                                OperationType.BuyCoin -> {
                                    viewState.enableEditAction(true)
                                    val data = tx.getData(TxCoinBuy::class.java)

                                    val rows = TxInputFieldRow.MultiBuilder(TxCoinBuy::class.java, extTx!!)
                                            .add {
                                                label = tr(R.string.deeplink_label_youre_buying)
                                                //todo: map coin from db
                                                text = coinMapper.idToSymbolDef(data.coinIdToBuy)
                                                enabled = enableEditInput
                                                tplCoin { txCoinBuy, s ->
                                                    //todo: map coin from db
                                                    txCoinBuy.coinIdToBuy = coinMapper[s]
                                                }
                                            }
                                            .add {
                                                label = tr(R.string.deeplink_label_amount)
                                                text = data.valueToBuy.toPlain()
                                                enabled = enableEditInput
                                                tplDecimal { txCoinBuy, s ->
                                                    txCoinBuy.valueToBuy = s.parseBigDecimal()
                                                }
                                            }
                                            .add {
                                                label = tr(R.string.deeplink_label_coin_to_sell)
                                                //todo: map coin from db
                                                text = coinMapper.idToSymbolDef(data.coinIdToSell)
                                                enabled = enableEditInput
                                                tplCoin { txCoinBuy, s ->
                                                    //todo: map coin from db
                                                    txCoinBuy.coinIdToSell = coinMapper[s]
                                                }
                                            }
                                            .add {
                                                label = tr(R.string.deeplink_amount_max_amount_to_spend)
                                                text = data.maxValueToSell.toPlain()
                                                enabled = enableEditInput
                                                tplDecimal { txCoinBuy, s ->
                                                    txCoinBuy.maxValueToSell = s.parseBigDecimal()
                                                }
                                            }
                                            .onValid { viewState.enableSubmit(it) }
                                            .build()
                                    allRows.addAll(rows)
                                }
                                OperationType.BuySwapPool -> {
                                    val data = tx.getData(TxSwapPoolBuy::class.java)
                                    if (data.coins.size == 2) {
                                        viewState.enableEditAction(true)
                                        val rows = TxInputFieldRow.MultiBuilder(TxSwapPoolBuy::class.java, extTx!!)
                                                .add {
                                                    label = tr(R.string.deeplink_label_youre_buying_pool)
                                                    //todo: map coin from db
                                                    text = coinMapper.idToSymbolDef(data.coinIdToSell)
                                                    enabled = enableEditInput
                                                    tplCoin({ txCoinSell, s ->
                                                        //todo: map coin from db
                                                        txCoinSell.coins[0] = coinMapper[s]
                                                    }, { validateEnoughCoins() })
                                                }
                                                .add {
                                                    label = tr(R.string.deeplink_label_amount)
                                                    text = data.valueToBuy.toPlain()
                                                    enabled = enableEditInput
                                                    tplDecimal({ txCoinSell, s ->
                                                        txCoinSell.valueToBuy = s.parseBigDecimal()
                                                    }, { validateEnoughCoins() })
                                                }
                                                .add {
                                                    label = tr(R.string.deeplink_label_coin_to_sell)
                                                    text = coinMapper.idToSymbolDef(data.coinIdToBuy)
                                                    enabled = enableEditInput
                                                    tplCoin { txCoinSell, s ->
                                                        txCoinSell.coins[1] = coinMapper[s]
                                                    }
                                                }
                                                .add {
                                                    label = tr(R.string.deeplink_amount_max_amount_to_spend)
                                                    text = data.maxValueToSell.toPlain()
                                                    enabled = enableEditInput
                                                    tplDecimal { txCoinSell, s ->
                                                        txCoinSell.maxValueToSell = s.parseBigDecimal()
                                                    }
                                                }
                                                .onValid { viewState.enableSubmit(it) }
                                                .build()
                                        allRows.addAll(rows)
                                    } else {
                                        viewState.enableEditAction(false)
                                        val rows = TxInputFieldRow.MultiBuilder(TxSwapPoolSell::class.java, extTx!!)
                                        data.coins.forEachIndexed { idx, value ->
                                            rows.add {
                                                label = "${tr(R.string.deeplink_label_coin_prefix)} ${idx + 1}"
                                                text = coinMapper.idToSymbolDef(value)
                                            }
                                        }
                                        rows.add {
                                            label = tr(R.string.deeplink_label_amount)
                                            text = data.valueToBuy.toPlain()
                                        }
                                        rows.add {
                                            label = tr(R.string.deeplink_amount_max_amount_to_spend)
                                            text = data.maxValueToSell.toPlain()
                                        }
                                        allRows.addAll(rows.build())
                                    }
                                }
                                OperationType.CreateSwapPool -> {
                                    viewState.enableEditAction(false)
                                    val data = tx.getData(TxSwapPoolCreate::class.java)
                                    val rows = TxInputFieldRow.MultiBuilder(TxSwapPoolCreate::class.java, extTx!!)
                                            .add {
                                                label = tr(R.string.deeplink_label_youre_creating_pool)
                                                text = "${coinMapper.idToSymbolDef(data.coin0)} / ${coinMapper.idToSymbolDef(data.coin1)}"
                                            }
                                            .add {
                                                label = "${tr(R.string.deeplink_label_volume_of_prefix)} ${coinMapper.idToSymbolDef(data.coin0)}"
                                                text = data.volume0.toPlain()
                                            }
                                            .add {
                                                label = "${tr(R.string.deeplink_label_volume_of_prefix)} ${coinMapper.idToSymbolDef(data.coin1)}"
                                                text = data.volume1.toPlain()
                                            }
                                            .build()

                                    allRows.addAll(rows)
                                }
                                OperationType.CreateCoin,
                                OperationType.RecreateCoin -> {
                                    viewState.enableEditAction(false)
                                    val data = if (tx.type == OperationType.RecreateCoin) {
                                        tx.getData<TxCoinRecreate>()
                                    } else {
                                        tx.getData<TxCoinCreate>()
                                    }

                                    val title = if (tx.type == OperationType.RecreateCoin)
                                        tr(R.string.deeplink_label_youre_recreating_coin)
                                    else
                                        tr(R.string.deeplink_label_youre_creating_coin)

                                    val rows = TxInputFieldRow.MultiBuilder(tx.type.opClass, extTx!!)
                                            .add {
                                                label = title
                                                text = data.symbol
                                            }
                                            .add {
                                                label = tr(R.string.deeplink_label_initial_amount)
                                                text = data.initialAmount.humanize()
                                            }
                                            .add {
                                                label = tr(R.string.deeplink_label_coin_name)
                                                text = data.name
                                            }
                                            .add {
                                                label = tr(R.string.deeplnk_label_crr)
                                                text = "${data.constantReserveRatio}%"
                                            }
                                            .add {
                                                label = tr(R.string.deeplink_label_reserve)
                                                text = data.initialReserve.humanize()
                                            }
                                            .add {
                                                label = tr(R.string.deeplink_label_max_supply)
                                                text = if (data.maxSupply < BigDecimal("10").pow(15)) {
                                                    data.maxSupply.humanize()
                                                } else {
                                                    "10¹⁵ (max)"
                                                }
                                            }
                                            .build()
                                    allRows.addAll(rows)
                                }
                                OperationType.CreateToken,
                                OperationType.RecreateToken -> {
                                    viewState.enableEditAction(false)
                                    val data = if (tx.type == OperationType.RecreateToken) {
                                        tx.getData<TxTokenRecreate>()
                                    } else {
                                        tx.getData<TxTokenCreate>()
                                    }

                                    val title = if (tx.type == OperationType.RecreateToken)
                                        tr(R.string.deeplink_label_youre_recreating_token)
                                    else
                                        tr(R.string.deeplink_label_youre_creating_token)

                                    val rows = TxInputFieldRow.MultiBuilder(tx.type.opClass, extTx!!)
                                            .add {
                                                label = title
                                                text = data.symbol
                                            }
                                            .add {
                                                label = tr(R.string.deeplink_label_initial_amount)
                                                text = data.initialAmount.humanize()
                                            }
                                            .add {
                                                label = tr(R.string.deeplink_label_coin_name)
                                                text = data.name
                                            }
                                            .add {
                                                label = tr(R.string.deeplink_label_max_supply)
                                                text = if (data.maxSupply < BigDecimal("10").pow(15)) {
                                                    data.maxSupply.humanize()
                                                } else {
                                                    "10¹⁵ (max)"
                                                }
                                            }
                                            .add {
                                                label = tr(R.string.deeplink_label_mintable)
                                                text = if (data.isMintable) tr(R.string.yes) else tr(R.string.no)
                                            }
                                            .add {
                                                label = tr(R.string.deeplink_label_burnable)
                                                text = if (data.isBurnable) tr(R.string.yes) else tr(R.string.no)
                                            }
                                            .build()
                                    allRows.addAll(rows)
                                }
                                OperationType.MintToken,
                                OperationType.BurnToken -> {
                                    viewState.enableEditAction(false)
                                    val data = if (tx.type == OperationType.MintToken) {
                                        tx.getData<TxTokenMint>()
                                    } else {
                                        tx.getData<TxTokenBurn>()
                                    }

                                    val title = if (tx.type == OperationType.MintToken)
                                        tr(R.string.deeplink_label_youre_minting_token)
                                    else
                                        tr(R.string.deeplink_label_youre_burning_token)
                                    val rows = TxInputFieldRow.MultiBuilder(tx.type.opClass, extTx!!)
                                            .add {
                                                label = title
                                                text = coinMapper.idToSymbolDef(data.coinId)
                                            }
                                            .add {
                                                label = "Amount"
                                                text = data.value.toPlain()
                                            }
                                            .build()
                                    allRows.addAll(rows)
                                }
                                OperationType.DeclareCandidacy -> {
                                    viewState.enableEditAction(false)
                                    val data = tx.getData(TxDeclareCandidacy::class.java)

                                    val rows = TxInputFieldRow.MultiBuilder(TxDeclareCandidacy::class.java, extTx!!)
                                            .add {
                                                label = tr(R.string.deeplink_label_youre_declaring_candidacy)
                                                text = data.publicKey.toString()
                                            }
                                            .add {
                                                label = tr(R.string.deeplink_label_address)
                                                text = data.address.toString()
                                            }
                                            .add {
                                                label = tr(R.string.deeplink_label_coin)
//                            todo: map coin from db
                                                text = coinMapper.idToSymbolDef(data.coinId)
                                            }
                                            .add {
                                                label = tr(R.string.deeplink_label_commission_percent)
                                                text = "${data.commission}%"
                                            }
                                            .build()
                                    allRows.addAll(rows)
                                }
                                OperationType.Delegate -> {
                                    viewState.enableEditAction(true)
                                    val data = tx.getData(TxDelegate::class.java)

                                    val rows = TxInputFieldRow.MultiBuilder(TxDelegate::class.java, extTx!!)
                                            .add {
                                                label = tr(R.string.deeplink_label_youre_delegating)
                                                //todo: map coin from db
                                                text = coinMapper.idToSymbolDef(data.coinId)
                                                enabled = enableEditInput
                                                tplCoin({ txDelegate, value ->
                                                    //todo: map coin from db
                                                    txDelegate.coinId = coinMapper[value]
                                                }, { validateEnoughCoins() })
                                            }
                                            .add {
                                                label = tr(R.string.deeplink_label_amount)
                                                text = data.stake.toPlain()
                                                enabled = enableEditInput
                                                addValidator(NumberNotZeroValidator())
                                                tplDecimal({ txDelegate, s ->
                                                    txDelegate.stake = s.parseBigDecimal()
                                                }, { validateEnoughCoins() })
                                            }
                                            .add {
                                                label = tr(R.string.deeplink_label_to)
                                                text = data.publicKey.toString()
                                                enabled = enableEditInput
                                                tplPublicKey { txDelegate, s ->
                                                    txDelegate.publicKey = MinterPublicKey(s)
                                                }
                                            }
                                            .onValid { viewState.enableSubmit(it) }
                                            .build()
                                    allRows.addAll(rows)
                                }
                                OperationType.AddLiquidity -> {
                                    viewState.enableEditAction(false)
                                    val data = tx.getData(TxAddLiquidity::class.java)

                                    val rows = TxInputFieldRow.MultiBuilder(TxAddLiquidity::class.java, extTx!!)
                                            .add {
                                                label = tr(R.string.deeplink_label_youre_adding_liquidity)
                                                text = "${coinMapper.idToSymbolDef(data.coin0)} / ${coinMapper.idToSymbolDef(data.coin1)}"
                                            }
                                            .add {
                                                label = tr(R.string.deeplink_label_volume)
                                                text = data.volume.toPlain()
                                            }
                                            .add {
                                                label = tr(R.string.deeplink_label_max_volume)
                                                text = data.maximumVolume.toPlain()
                                            }
                                            .build()

                                    allRows.addAll(rows)
                                }
                                OperationType.RemoveLiquidity -> {
                                    viewState.enableEditAction(false)
                                    val data = tx.getData(TxRemoveLiquidity::class.java)

                                    val rows = TxInputFieldRow.MultiBuilder(TxRemoveLiquidity::class.java, extTx!!)
                                            .add {
                                                label = tr(R.string.deeplink_label_youre_removing_liquidity)
                                                text = "${coinMapper.idToSymbolDef(data.coin0)} / ${coinMapper.idToSymbolDef(data.coin1)}"
                                            }
                                            .add {
                                                label = tr(R.string.deeplink_label_liquidity)
                                                text = "${data.liquidity}%"
                                            }
                                            .add {
                                                label = "${tr(R.string.deeplink_label_min_volume_of_prefix)} ${coinMapper.idToSymbolDef(data.coin0)}"
                                                text = data.minVolume0.toPlain()
                                            }
                                            .add {
                                                label = "${tr(R.string.deeplink_label_min_volume_of_prefix)} ${coinMapper.idToSymbolDef(data.coin1)}"
                                                text = data.minVolume1.toPlain()
                                            }
                                            .build()

                                    allRows.addAll(rows)
                                }

                                OperationType.Unbound -> {
                                    viewState.enableEditAction(true)
                                    val data = tx.getData(TxUnbound::class.java)

                                    val rows = TxInputFieldRow.MultiBuilder(TxUnbound::class.java, extTx!!)
                                            .add {
                                                label = tr(R.string.deeplink_label_youre_unbonding)
                                                //todo: map coin from db
                                                text = coinMapper.idToSymbolDef(data.coinId)
                                                enabled = enableEditInput
                                                tplCoin { txUnbond, value ->
                                                    //todo: map coin from db
                                                    txUnbond.coinId = coinMapper[value]
                                                }
                                            }
                                            .add {
                                                label = tr(R.string.deeplink_label_amount)
                                                text = data.value.toPlain()
                                                enabled = enableEditInput
                                                tplDecimal { txUnbond, s ->
                                                    txUnbond.value = s.parseBigDecimal()
                                                }
                                            }
                                            .add {
                                                label = tr(R.string.deeplink_label_from)
                                                text = data.publicKey.toString()
                                                enabled = enableEditInput
                                                tplPublicKey { txUnbond, s ->
                                                    txUnbond.publicKey = MinterPublicKey(s)
                                                }
                                            }
                                            .onValid { viewState.enableSubmit(it) }
                                            .build()
                                    allRows.addAll(rows)
                                }
                                OperationType.MoveStake -> {
                                    viewState.enableEditAction(false)
                                    val data = tx.getData(TxMoveStake::class.java)
                                    val rows = TxInputFieldRow.MultiBuilder(TxMoveStake::class.java, extTx!!)
                                            .add {
                                                label = tr(R.string.deeplink_label_youre_moving_stake_from)
                                                text = data.from.toString()
                                            }
                                            .add {
                                                label = tr(R.string.deeplink_label_to)
                                                text = data.to.toString()
                                            }
                                            .add {
                                                label = tr(R.string.deeplink_label_amount)
                                                text = data.stake.toPlain()
                                            }
                                            .add {
                                                label = tr(R.string.deeplink_label_coin)
                                                text = coinMapper.idToSymbolDef(data.coinId)
                                            }
                                            .build()

                                    allRows.addAll(rows)
                                }
                                OperationType.RedeemCheck -> {
                                    viewState.enableEditAction(false)
                                    val data = tx.getData(TxRedeemCheck::class.java)
                                    val check = data.decodedCheck
                                    val rows = TxInputFieldRow.MultiBuilder(TxRedeemCheck::class.java, extTx!!)
                                            .add {
                                                label = tr(R.string.deeplink_label_youre_redeeming_check)
                                                text = data.rawCheck.toString()
                                            }
                                            .add {
                                                label = tr(R.string.deeplink_label_coin)
                                                //todo: map coin from db
                                                text = coinMapper.idToSymbolDef(check.coinId)
                                            }
                                            .add {
                                                label = tr(R.string.deeplink_label_amount)
                                                text = check.value.humanize()
                                            }
                                            .build()
                                    allRows.addAll(rows)
                                }
                                OperationType.SetCandidateOnline -> {
                                    viewState.enableEditAction(false)
                                    val data = tx.getData(TxSetCandidateOnline::class.java)
                                    val rows = TxInputFieldRow.MultiBuilder(TxSetCandidateOnline::class.java, extTx!!)
                                            .add {
                                                label = tr(R.string.deeplink_label_youre_switching_cand_on)
                                                text = data.publicKey.toString()
                                            }
                                            .build()
                                    allRows.addAll(rows)
                                }
                                OperationType.SetCandidateOffline -> {
                                    viewState.enableEditAction(false)
                                    val data = tx.getData(TxSetCandidateOffline::class.java)
                                    val rows = TxInputFieldRow.MultiBuilder(TxSetCandidateOffline::class.java, extTx!!)
                                            .add {
                                                label = tr(R.string.deeplink_label_youre_switching_cand_off)
                                                text = data.publicKey.toString()
                                            }
                                            .build()
                                    allRows.addAll(rows)
                                }
                                OperationType.Multisend -> {
                                    viewState.enableEditAction(false)
                                    val data = tx.getData(TxMultisend::class.java)

                                    val rowsBuilder = TxInputFieldRow.MultiBuilder(TxMultisend::class.java, extTx!!)
                                            .add {
                                                label = tr(R.string.deeplink_label_youre_multisending)
                                                text = tr(R.string.deeplink_label_to_n_recipients, data.items.size.toString())
                                            }

                                    for (item in data.items) {
                                        rowsBuilder.add {
                                            label = "${item.to}"
                                            text = "${item.value.humanize()} ${coinMapper.idToSymbolDef(item.coinId, "(coin ID: %s)")}"
                                            //todo: map coin from db
                                        }
                                    }
                                    allRows.addAll(rowsBuilder.build())
                                }
                                OperationType.CreateMultisigAddress,
                                OperationType.EditMultisig -> {
                                    viewState.enableEditAction(false)
                                    val data = if (tx.type == OperationType.CreateMultisigAddress) {
                                        tx.getData<TxCreateMultisigAddress>()
                                    } else {
                                        tx.getData<TxEditMultisig>()
                                    }

                                    val title = if (tx.type == OperationType.CreateMultisigAddress)
                                        tr(R.string.deeplink_label_youre_creating_multisig)
                                    else
                                        tr(R.string.deeplink_label_youre_editing_multisig)

                                    val rowsBuilder = TxInputFieldRow.MultiBuilder(tx.type.opClass, extTx!!)
                                            .add {
                                                label = title
                                                text = tr(R.string.deeplink_label_with_n_addresses, data.addresses.size)
                                            }
                                            .add {
                                                label = tr(R.string.deeplink_label_threshold)
                                                text = data.threshold.toString()
                                            }

                                    for ((i, item) in data.addresses.withIndex()) {
                                        val weight = data.weights[i]
                                        rowsBuilder.add {
                                            label = tr(R.string.deeplink_label_weight_n, weight)
                                            text = "$item"
                                        }
                                    }
                                    allRows.addAll(rowsBuilder.build())
                                }
                                OperationType.EditCandidate -> {
                                    viewState.enableEditAction(false)
                                    val data = tx.getData(TxEditCandidate::class.java)
                                    val rows = TxInputFieldRow.MultiBuilder(TxEditCandidate::class.java, extTx!!)
                                            .add {
                                                label = tr(R.string.deeplink_label_youre_editing_cand)
                                                text = data.publicKey.toString()
                                            }
                                            .add {
                                                label = tr(R.string.deeplink_label_owner_address)
                                                text = data.ownerAddress.toString()
                                            }
                                            .add {
                                                label = tr(R.string.deeplink_label_reward_address)
                                                text = data.rewardAddress.toString()
                                            }
                                            .add {
                                                label = tr(R.string.deeplink_label_control_address)
                                                text = data.controlAddress.toString()
                                            }
                                            .build()

                                    allRows.addAll(rows)
                                }
                                OperationType.EditCandidatePublicKey -> {
                                    viewState.enableEditAction(false)
                                    val data = tx.getData(TxEditCandidatePublicKey::class.java)
                                    val rows = TxInputFieldRow.MultiBuilder(TxEditCandidatePublicKey::class.java, extTx!!)
                                            .add {
                                                label = tr(R.string.deeplink_label_youre_editing_cand_pubkey)
                                                text = data.publicKey.toString()
                                            }
                                            .add {
                                                label = tr(R.string.deeplink_label_new_pubkey)
                                                text = data.newPublicKey.toString()
                                            }
                                            .build()

                                    allRows.addAll(rows)
                                }
                                OperationType.EditCandidateCommission -> {
                                    viewState.enableEditAction(false)
                                    val data = tx.getData(TxEditCandidateCommission::class.java)
                                    val rows = TxInputFieldRow.MultiBuilder(TxEditCandidateCommission::class.java, extTx!!)
                                            .add {
                                                label = tr(R.string.deeplink_label_youre_editing_cand_commission)
                                                text = data.publicKey.toString()
                                            }
                                            .add {
                                                label = tr(R.string.deeplink_label_new_commission)
                                                text = "${data.commission}%"
                                            }
                                            .build()

                                    allRows.addAll(rows)
                                }
                                OperationType.SetHaltBlock -> {
                                    viewState.enableEditAction(false)
                                    val data = tx.getData(TxSetHaltBlock::class.java)
                                    val rows = TxInputFieldRow.MultiBuilder(TxSetHaltBlock::class.java, extTx!!)
                                            .add {
                                                label = tr(R.string.deeplink_label_youre_setting_halt_block)
                                                text = data.publicKey.toString()
                                            }
                                            .add {
                                                label = tr(R.string.deeplink_label_height)
                                                text = data.height.toString()
                                            }
                                            .build()

                                    allRows.addAll(rows)
                                }
                                OperationType.EditCoinOwner -> {
                                    viewState.enableEditAction(false)
                                    val data = tx.getData(TxEditCoinOwner::class.java)
                                    val rows = TxInputFieldRow.MultiBuilder(TxEditCoinOwner::class.java, extTx!!)
                                            .add {
                                                label = tr(R.string.deeplink_label_youre_changing_coin_owner)
                                                text = data.symbol
                                            }
                                            .add {
                                                label = tr(R.string.deeplink_label_new_owner)
                                                text = data.newOwner.toString()
                                            }
                                            .build()

                                    allRows.addAll(rows)
                                }
                                OperationType.PriceVote -> {
                                    viewState.enableEditAction(false)
                                    val data = tx.getData(TxPriceVote::class.java)
                                    val rows = TxInputFieldRow.MultiBuilder(TxPriceVote::class.java, extTx!!)
                                            .add {
                                                label = tr(R.string.deeplink_label_youre_voting_for_price)
                                                text = data.price.toString()
                                            }
                                            .build()

                                    allRows.addAll(rows)
                                }
                                OperationType.VoteUpdate -> {
                                    viewState.enableEditAction(false)
                                    val data = tx.getData(TxVoteUpdate::class.java)
                                    val rows = TxInputFieldRow.MultiBuilder(TxVoteUpdate::class.java, extTx!!)
                                            .add {
                                                label = tr(R.string.deeplink_label_youre_voting_for_update)
                                                text = "${tr(R.string.deeplink_label_version)}: ${data.version}"
                                            }
                                            .add {
                                                label = tr(R.string.deeplink_label_pubkey)
                                                text = data.pubKey.toString()
                                            }
                                            .build()

                                    allRows.addAll(rows)
                                }
                                OperationType.VoteCommission -> {
                                    viewState.enableEditAction(false)
                                    val data = tx.getData(TxVoteCommission::class.java)
                                    val rows = TxInputFieldRow.MultiBuilder(TxVoteCommission::class.java, extTx!!)
                                    data.nameValueMap().forEach { kv ->
                                        rows.add {
                                            label = "${tr(R.string.deeplink_label_commission)}: ${kv.key}"
                                            text = "${kv.value.humanizeDecimal()} ${initFeeData?.gasRepresentingCoin?.symbol ?: MinterSDK.DEFAULT_COIN}"
                                        }
                                    }
                                    allRows.addAll(rows.build())
                                }
                                else -> {
                                    viewState.startDialog(false) { ctx: Context? ->
                                        ConfirmDialog.Builder(ctx!!, R.string.dialog_title_err_unable_to_send_tx)
                                                .setText(tr(R.string.dialog_text_err_unsupported_tx_type, tx.type.name))
                                                .setPositiveAction(R.string.btn_close) { d, _ ->
                                                    d.dismiss()
                                                    viewState.finishCancel()
                                                }
                                                .create()
                                    }
                                }
                            }

                            if (tx.payload.size() > 0 || enableEditInput) {
                                val row = TxInputFieldRow.Builder(tx.type.opClass, extTx!!).apply {
                                    label = resources.getString(R.string.label_payload_explicit)
                                    text = tx.payload.stringValue()
                                    enabled = enableEditInput
                                    configureInput { inputGroup, inputField ->
                                        inputGroup.addValidator(inputField, PayloadValidator())
                                        inputField.hint = Wallet.app().res().getString(R.string.label_payload_type)
                                        inputGroup.addFilter(inputField) { source, _, _, _, _, _ ->
                                            if (inputField.text.toString().toByteArray().size >= PayloadValidator.MAX_PAYLOAD_LENGTH) {
                                                ""
                                            } else {
                                                source
                                            }
                                        }
                                        inputField.addTextChangedSimpleListener {
                                            extTx!!.resetPayload(BytesData(it.toString().toByteArray()))
                                            payload = extTx!!.payload
                                            calculateFee(extTx!!)
                                        }
                                    }
                                }.build()
                                allRows.add(row)
                            }

                            viewState.setData(allRows)
                        },
                        { t: Throwable ->
                            Timber.w(t)
                        }
                )
                .disposeOnDestroy()


    }

    private fun startExecuteTransaction() {
        viewState.startDialog(false) { ctx ->
            val dialog = WalletProgressDialog.Builder(ctx, R.string.please_wait)
                    .setText(R.string.tx_send_in_progress)
                    .create()

            dialog.setCancelable(false)
            val initData: Observable<TxInitData> = if (extTx!!.nonce != null && extTx!!.nonce != BigInteger.ZERO) {
                val d = TxInitData(extTx!!.nonce, extTx!!.gasPrice)
                Observable.just(d)
            } else {
                getTxInitData(from)
            }
            val d = initData
                    .joinToUi()
                    .switchMap(Function<TxInitData, ObservableSource<GateResult<PushResult>>> { cntRes: TxInitData ->
                        // if in previous request we've got error, returning it
                        if (!cntRes.isOk) {
                            return@Function Observable.just(cntRes.errorResult!!.castErrorResultTo<PushResult>())
                        }
                        val gasPrice = if (extTx!!.type == OperationType.RedeemCheck) {
                            BigInteger.ONE
                        } else {
                            cntRes.gas ?: BigInteger.ONE
                        }
                        var gasCoinId = extTx!!.gasCoinId ?: MinterSDK.DEFAULT_COIN_ID
                        if (extTx!!.type == OperationType.RedeemCheck) {
                            gasCoinId = extTx!!.getData<TxRedeemCheck>().decodedCheck.gasCoinId
                        }
                        val tx = Transaction.Builder(cntRes.nonce, extTx)
                                .setGasPrice(gasPrice)
                                .setGasCoinId(gasCoinId)
                                .setPayload(payload)
                                .buildFromExternal()
                        val data = secretStorage.getSecret(from!!)
                        val sign = tx.signSingle(data.privateKey)!!
                        safeSubscribeIoToUi(
                                gateTxRepo.sendTransaction(sign)
                                        .onErrorResumeNext(ReactiveGate.toGateError())
                        )
                    })
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
            ConfirmDialog.Builder(ctx!!, R.string.dialog_title_err_unable_to_send_tx)
                    .setText(errorResult.message)
                    .setPositiveAction(R.string.btn_close) { d, _ ->
                        d.dismiss()
//                        viewState.finishCancel()
                    }
                    .create()
        }
    }

    private fun onFailedExecuteTransaction(throwable: Throwable) {
        Timber.w(throwable, "Uncaught tx error")
        viewState.startDialog { ctx: Context? ->
            ConfirmDialog.Builder(ctx!!, R.string.dialog_title_err_unable_to_send_tx)
                    .setText(throwable.message)
                    .setPositiveAction(R.string.btn_close) { d, _ ->
                        d.dismiss()
                        //viewState.finishCancel()
                    }
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
                    .setLabel(R.string.send_dialog_success_label)
                    .setValue(null)
                    .setPositiveAction(R.string.btn_view_tx) { d, _ ->
                        Wallet.app().sounds().play(R.raw.click_pop_zap)
                        viewState.startExplorer(result.result.hash.toString())
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

    private fun onSubmit() {
        startExecuteTransaction()
    }

    private fun onCancel() {
        viewState.finishCancel()
    }
}
