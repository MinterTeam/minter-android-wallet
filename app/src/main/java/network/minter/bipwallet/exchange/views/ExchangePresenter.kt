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
package network.minter.bipwallet.exchange.views

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.annotation.CallSuper
import com.annimon.stream.Stream
import com.edwardstock.inputfield.form.InputWrapper
import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.launch
import moxy.presenterScope
import network.minter.bipwallet.R
import network.minter.bipwallet.analytics.AppEvent
import network.minter.bipwallet.apis.explorer.RepoTransactions
import network.minter.bipwallet.apis.gate.TxInitDataRepository
import network.minter.bipwallet.apis.reactive.*
import network.minter.bipwallet.apis.reactive.ReactiveGate.toGateError
import network.minter.bipwallet.coins.RepoCoins
import network.minter.bipwallet.exchange.ExchangeCalculator
import network.minter.bipwallet.exchange.ExchangeCalculator.CalculationResult
import network.minter.bipwallet.exchange.contract.ExchangeView
import network.minter.bipwallet.exchange.models.ConvertTransactionData
import network.minter.bipwallet.exchange.models.ExchangeAmount
import network.minter.bipwallet.exchange.ui.ConvertCoinActivity
import network.minter.bipwallet.exchange.ui.dialogs.TxConfirmStartDialog
import network.minter.bipwallet.internal.auth.AuthSession
import network.minter.bipwallet.internal.dialogs.ConfirmDialog
import network.minter.bipwallet.internal.dialogs.WalletProgressDialog
import network.minter.bipwallet.internal.exceptions.ErrorManager
import network.minter.bipwallet.internal.exceptions.RetryListener
import network.minter.bipwallet.internal.exceptions.humanDetailsMessage
import network.minter.bipwallet.internal.helpers.MathHelper.asBigDecimal
import network.minter.bipwallet.internal.helpers.MathHelper.bdNull
import network.minter.bipwallet.internal.helpers.MathHelper.humanize
import network.minter.bipwallet.internal.helpers.MathHelper.humanizeDecimal
import network.minter.bipwallet.internal.helpers.MathHelper.toPlain
import network.minter.bipwallet.internal.helpers.ViewExtensions.tr
import network.minter.bipwallet.internal.mvp.MvpBasePresenter
import network.minter.bipwallet.internal.storage.RepoAccounts
import network.minter.bipwallet.internal.storage.SecretStorage
import network.minter.bipwallet.internal.storage.models.AddressListBalancesTotal
import network.minter.bipwallet.sending.ui.dialogs.TxSendSuccessDialog
import network.minter.bipwallet.tx.contract.TxInitData
import network.minter.blockchain.api.EstimateSwapFrom
import network.minter.blockchain.models.NodeResult
import network.minter.blockchain.models.operational.*
import network.minter.core.MinterSDK
import network.minter.core.crypto.MinterAddress
import network.minter.explorer.models.*
import network.minter.explorer.repo.ExplorerPoolsRepository
import network.minter.explorer.repo.GateEstimateRepository
import network.minter.explorer.repo.GateGasRepository
import network.minter.explorer.repo.GateTransactionRepository
import org.parceler.Parcels
import timber.log.Timber
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
abstract class ExchangePresenter<V : ExchangeView>(
        private val session: AuthSession,
        protected val secretStorage: SecretStorage,
        protected val accountStorage: RepoAccounts,
        protected val txRepo: RepoTransactions,
        protected val explorerCoinsRepo: RepoCoins,
        protected val gasRepo: GateGasRepository,
        protected val estimateRepo: GateEstimateRepository,
        protected val poolsRepo: ExplorerPoolsRepository,
        protected val gateTxRepo: GateTransactionRepository,
        protected val initDataRepo: TxInitDataRepository,
        protected val errorManager: ErrorManager
) : MvpBasePresenter<V>(), ErrorManager.ErrorGlobalReceiverListener, ErrorManager.ErrorLocalHandlerListener, ErrorManager.ErrorLocalReceiverListener {
    private var account: CoinBalance? = null
    private var currentCoin: CoinItemBase? = null
    private var buyCoin: CoinItemBase? = null
    private var sellAmount: BigDecimal? = BigDecimal(0)
    private var buyAmount: BigDecimal? = BigDecimal(0)
    private var inputChangeSubject: BehaviorSubject<Boolean>? = null
    private var gasCoin: BigInteger? = null
    private var accounts: List<CoinBalance> = ArrayList(1)
    private val useMax = AtomicBoolean(false)
    private val clickedUseMax = AtomicBoolean(false)
    private var gasPrice = BigInteger.ONE
    private var estimateAmount: BigDecimal? = null
    private var exchangeAmount: ExchangeAmount? = null
    private var buyForResult = false
    private var fromAccount: MinterAddress? = null
    private var swapFrom: EstimateSwapFrom = EstimateSwapFrom.Default
    private var exchangeRoute: PoolRoute? = null
    private var opTypeInternal: OperationType? = null

    // used to detect which type of coin is exchanging: simple coin, token or pool token
    private var isBasicExchange = true

    protected abstract val isBuying: Boolean

    private val fee: BigDecimal
        get() {
            return if (initFeeData != null && initFeeData?.priceCommissions != null) {
                initFeeData!!.priceCommissions.getByType(operationType).humanizeDecimal().multiply(BigDecimal(gasPrice))
            } else {
                operationType.fee.multiply(BigDecimal(gasPrice))
            }
        }

    private var initFeeData: TxInitData? = null

    private val sellAmountSafe: BigDecimal
        get() {
            return sellAmount ?: BigDecimal.ZERO
        }

    private val buyAmountSafe: BigDecimal
        get() {
            return buyAmount ?: BigDecimal.ZERO
        }

    override fun onError(t: Throwable) {
        Timber.w("Unable to get balance for exchanging: %s", t.humanDetailsMessage)
    }

    override fun onError(t: Throwable, retryListener: RetryListener) {
        handlerError(t, retryListener)
    }

    override fun onRetried() {

    }

    override fun handleErrorFor(): Class<*> {
        return GateGasRepository::class.java
    }

    override fun attachView(view: V) {
        super.attachView(view)
        loadAndSetFee()
    }

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()

        accountStorage.update()

        inputChangeSubject = BehaviorSubject.create()
        inputChangeSubject!!
                .toFlowable(BackpressureStrategy.LATEST)
                .debounce(200, TimeUnit.MILLISECONDS)
                .subscribe(
                        { buying: Boolean -> onAmountChangedInternal(buying) },
                        { t -> Timber.w(t, "Error after exchange amount changed") }
                )

        viewState.setSubmitEnabled(false)
        viewState.setFormValidationListener { valid ->
            Handler(Looper.getMainLooper()).post {
                Timber.d("Submit status: valid=${valid}; checkZero=${if (isBuying) buyAmount else sellAmount}")
                viewState.setSubmitEnabled(valid && checkZero(if (isBuying) buyAmount else sellAmount))
            }
        }
        viewState.setTextChangedListener { input, valid ->
            onInputChanged(input, valid)
            if (!valid) {
                Timber.d("Invalid field: ${input.fieldName} - ${input.error}")
            }
        }
        viewState.setOnClickSelectAccount { view: View -> onClickSelectAccount(view) }
        viewState.setOnClickMaximum { onClickMaximum() }
        viewState.setOnClickSubmit { onSubmit() }
        setCoinsAutocomplete()
    }

    override fun handleExtras(bundle: Bundle?) {
        super.handleExtras(bundle)
        if (bundle != null) {
            if (bundle.containsKey(ConvertCoinActivity.EXTRA_COIN_TO_BUY)) {
                buyCoin = Parcels.unwrap(bundle.getParcelable(ConvertCoinActivity.EXTRA_COIN_TO_BUY))
                buyAmount = BigDecimal(bundle.getString(ConvertCoinActivity.EXTRA_VALUE_TO_BUY))
                buyForResult = true
                viewState.setIncomingCoin(buyCoin!!.symbol)
                if (isBuying) {
                    viewState.setAmount(buyAmount!!.toPlainString())
                }

                viewState.validateForm()
            }
        }

        fromAccount = if (bundle?.containsKey(ConvertCoinActivity.EXTRA_ACCOUNT) == true) {
            bundle.getSerializable(ConvertCoinActivity.EXTRA_ACCOUNT) as MinterAddress
        } else {
            secretStorage.mainWallet
        }
        viewState.validateForm()

        accountStorage
                .observe()
                .joinToUi()
                .subscribe(
                        { res: AddressListBalancesTotal ->
                            val acc: AddressBalance = accountStorage.entity.getWalletByAddress(fromAccount!!)

                            if (!res.isEmpty) {
                                accounts = acc.coinsList
                                account = acc.getCoin(MinterSDK.DEFAULT_COIN_ID)
                                if (currentCoin != null) {
                                    account = Stream.of(accounts).filter { value: CoinBalance -> (value.coin == currentCoin) }.findFirst().orElse(account)
                                }
                                onAccountSelected(account, true)

                                if (buyAmount != null && buyCoin != null) {
                                    onAmountChangedInternal(isBuying)
                                }
                                viewState.validateForm()
                            }
                        },
                        {
                            Timber.w(it, "Unable to get balance for exchanging")
                        }
                )
                .disposeOnDestroy()
    }

    @CallSuper
    protected fun setCalculation(calculation: String?) {
        viewState.setCalculation(calculation!!)
    }

    private fun loadAndSetFee() {
        initDataRepo.loadFeeWithTx()
                .retryWhen(errorManager.retryWhenHandler)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(
                        { res: TxInitData ->
                            gasPrice = res.gas!!
                            initFeeData = res
                            viewState.setFee(res.calculateFeeText(operationType))
                        },
                        { e ->
                            gasPrice = BigInteger.ONE
                            Timber.w(e, "Unable to get min gas price for sending")
                        }
                )
    }

    private fun checkZero(amount: BigDecimal?): Boolean {
        val valid = amount == null || !bdNull(amount)
        if (!valid) {
            viewState.setError("amount", tr(R.string.input_validator_amount_must_be_greater_zero))
        } else {
            viewState.setError("amount", null)
        }
        return valid
    }

    private fun setCoinsAutocomplete() {
        viewState.setCoinsAutocomplete { item: CoinItem, _ ->
            buyCoin = item
            viewState.setIncomingCoin(item.symbol)
        }
    }

    private fun onStartExecuteTransaction(txData: ConvertTransactionData) {
        viewState.startDialog { ctx: Context ->
            val dialog = WalletProgressDialog.Builder(ctx, R.string.dialog_title_exchanging)
                    .setText(R.string.tx_convert_began)
                    .create()

            dialog.setCancelable(false)
            Observable
                    .combineLatest(
                            estimateRepo.getTransactionCount(account!!.address!!),
                            gasRepo.minGas,
                            { t1: GateResult<TxCount>, t2: GateResult<GasValue> ->
                                TxInitData(t1, t2)
                            })

                    .switchMap { initData ->
                        // if error occurred upper, notify error
                        if (!initData.isOk) {
                            return@switchMap initData.errorResult!!.castErrorResultTo<PushResult>().toObservable()
                        }
                        var balance = BigDecimal("0")
                        if (operationType == OperationType.SellCoin || operationType == OperationType.SellAllCoins) {
                            balance = account!!.amount
                        }
                        signSendTx(dialog, txData, initData, balance)
                    }
                    .onErrorResumeNext(toGateError())
                    .joinToUi()
                    .subscribe(
                            { result: GateResult<PushResult> ->
                                onSuccessExecuteTransaction(result)
                            },
                            { t: Throwable ->
                                onErrorExecuteTransaction(ReactiveGate.createGateErrorPlain<Any>(t))
                            }
                    )
                    .disposeOnDestroy()
            dialog
        }
    }

    @Throws(OperationInvalidDataException::class)
    private fun signSendTx(
            @Suppress("UNUSED_PARAMETER") dialog: WalletProgressDialog,
            txData: ConvertTransactionData,
            initData: TxInitData,
            balance: BigDecimal): ObservableSource<GateResult<PushResult>> {

        // creating tx
        val tx = txData.build(initData.nonce!!.add(BigInteger.ONE), initData.gas!!, balance)
        isBasicExchange = txData.isBasicExchange

        val data = secretStorage.getSecret(account!!.address!!)
        val sign = tx.signSingle(data.privateKey)!!
        return safeSubscribeIoToUi(
                gateTxRepo.sendTransaction(sign)
                        .onErrorResumeNext(toGateError())
        )
    }

    private fun showSuccessDialog(txHash: String, value: BigDecimal, coin: CoinItemBase) {
        viewState.startDialog { ctx: Context ->
            val dialogBuilder = TxSendSuccessDialog.Builder(ctx)
                    .setLabel(R.string.tx_exchange_success_dialog_description)
                    .setValue("${value.humanize()} $coin")

            if (!buyForResult) {
                dialogBuilder.setPositiveAction(R.string.btn_view_tx) { d, _ ->
                    d.dismiss()
                    viewState.startExplorer(txHash)
                    viewState.finishCancel()
                }
            }
            dialogBuilder.setNegativeAction(R.string.btn_close) { d, _ ->
                d.dismiss()
                viewState.finishSuccess(exchangeAmount!!)
            }
            dialogBuilder.create()
        }
    }

    private fun onSuccessExecuteTransaction(result: GateResult<PushResult>) {
        if (!result.isOk) {
            onErrorExecuteTransaction(result)
            return
        }

        accountStorage.update(true)
        txRepo.update(true)

        if (isBuying) {
            saveExchangeAmount(buyCoin!!, buyAmount!!)

            // for buy tab just show how much we've got
            showSuccessDialog(result.result.hash.toString(), buyAmount!!, buyCoin!!)
            return
        }

        // get transaction from explorer to show exact value what we've got after sell
        // todo: show "Coins have been exchanged" if can't get transaction after 30 seconds
        txRepo.entity.waitTransactionUntilUncommitted(result.result.hash.toString())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(
                        { r ->
                            if (r.isOk) {
                                when (r.result.data) {
                                    is HistoryTransaction.TxConvertCoinResult -> {
                                        val data = r.result.getData<HistoryTransaction.TxConvertCoinResult>()
                                        saveExchangeAmount(data.coinToBuy, data.valueToBuy)
                                        showSuccessDialog(r.result.hash.toString(), data.valueToBuy, data.coinToBuy)
                                    }
                                    is HistoryTransaction.TxConvertSwapPoolResult -> {
                                        val data = r.result.getData<HistoryTransaction.TxConvertSwapPoolResult>()
                                        saveExchangeAmount(data.coinLast, data.valueToBuy)
                                        showSuccessDialog(r.result.hash.toString(), data.valueToBuy, data.coinLast)
                                    }
                                    else -> {
                                        Timber.e("Unknown result type: %s", r.result.data.javaClass.name)
                                    }
                                }
                                return@subscribe
                            }

                            onErrorRequest(r)
                        },
                        { t: Throwable ->
                            onErrorRequest(ReactiveExplorer.createExpErrorPlain<Any>(t))
                        }
                )
    }

    private fun saveExchangeAmount(coin: CoinItemBase, amount: BigDecimal) {
        if (exchangeAmount != null && exchangeAmount!!.coin.id == coin.id) {
            exchangeAmount!!.amount += amount
        } else {
            exchangeAmount = ExchangeAmount(coin, amount)
        }
    }

    private fun onErrorExecuteTransaction(errorResult: GateResult<*>) {
        Timber.e(errorResult.message, "Unable to send transaction")
        viewState.startDialog { ctx: Context ->
            ConfirmDialog.Builder(ctx, R.string.dialog_title_err_unable_to_send_tx)
                    .setText((errorResult.humanError()))
                    .setPositiveAction(R.string.btn_close)
                    .create()
        }
    }

    private fun onErrorRequest(errorResult: ExpResult<*>) {
        Timber.e("Unable to send transaction: %s", errorResult.error?.message)
        viewState.startDialog { ctx: Context ->
            ConfirmDialog.Builder(ctx, R.string.dialog_title_err_unable_to_send_tx)
                    .setText((errorResult.message ?: tr(R.string.dialog_text_err_unknown_error_caused)))
                    .setPositiveAction(R.string.btn_close)
                    .create()
        }
    }

    private fun onClickMaximum() {
        if (isBuying) {
            return
        }
        if (account == null) {
            return
        }
        viewState.setAmount(account!!.amount.toPlain())
        useMax.set(true)
        clickedUseMax.set(true)
        viewState.hideKeyboard()
        analytics.send(AppEvent.ConvertSpendUseMaxButton)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onClickSelectAccount(view: View) {
        viewState.startAccountSelector(accountStorage.entity.getWalletByAddress(fromAccount!!).coinsList) {
            onAccountSelected(it.data, false)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onInputChanged(editText: InputWrapper, valid: Boolean) {
        val text = editText.text.toString()
        Timber.d("Input changed: %s", editText.text)
        when (editText.id) {
            R.id.input_incoming_coin -> {
                explorerCoinsRepo.entity.findByName(text)
                        .filter { it.type != CoinItemBase.CoinType.PoolToken }
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io())
                        .subscribe(
                                {
                                    buyCoin = it
                                    inputChangeSubject!!.onNext(isBuying)
                                },
                                {
                                    buyCoin = null
                                    inputChangeSubject!!.onNext(isBuying)
                                    Timber.d("Unable to find coin by name %s", text)
                                }
                        )
            }
            R.id.input_amount -> {
                val am = text.asBigDecimal()
                checkZero(am)
                if (isBuying) {
                    buyAmount = am
                } else {
                    if (!clickedUseMax.get()) {
                        useMax.set(false)
                    }
                    clickedUseMax.set(false)
                    sellAmount = am
                }
//                viewState.setSubmitEnabled(mAccount != null && am <= mAccount!!.amount)
                inputChangeSubject!!.onNext(isBuying)
            }
        }
    }

    private fun onSubmit() {
        if (account == null || buyCoin == null || buyAmount == null || sellAmount == null) {
            return
        }
        if (isBuying) {
            analytics.send(AppEvent.ConvertGetExchangeButton)
        } else {
            analytics.send(AppEvent.ConvertSpendExchangeButton)
        }

        /*
        sell:
            you will spend
              1000 BIP (mSellAmount + mFromAccount.coin)
               to get
              2308.1661 TEAM (mBuyAmount + mBuyCoin)

         buy:
            you will spend
              185.36151 BIP (mSellAmount + mFromAccount.coin)
               to get
              100 TEAM (mBuyAmount + mBuyCoin)
         */

        viewState.startDialog { ctx ->
            TxConfirmStartDialog.Builder(ctx, R.string.dialog_title_exchange_begin)
                    .setFirstLabel(R.string.dialog_label_exchange_you_will_spend)
                    .setFirstValue(sellAmount!!.humanize())
                    .setFirstCoin(account!!.coin!!.symbol)
                    .setSecondLabel(R.string.dialog_label_exchange_to_get)
                    .setSecondValue(buyAmount!!)
                    .setSecondCoin(buyCoin!!.symbol)
                    .setPositiveAction(R.string.btn_confirm) { _, _ ->
                        val type: ConvertTransactionData.Type = when {
                            useMax.get() -> ConvertTransactionData.Type.SellAll
                            isBuying -> ConvertTransactionData.Type.Buy
                            else -> ConvertTransactionData.Type.Sell
                        }
                        val amount = if (isBuying) buyAmount else sellAmount
                        val txData = ConvertTransactionData(
                                type = type,
                                gasCoin = gasCoin!!,
                                sellCoin = account!!.coin,
                                buyCoin = buyCoin!!,
                                amount = amount!!,
                                estimateAmount = estimateAmount ?: BigDecimal.ZERO,
                                swapFrom = swapFrom,
                                route = exchangeRoute
                        )

                        onStartExecuteTransaction(txData)
                    }
                    .setNegativeAction(R.string.btn_cancel)
                    .create()
        }
    }

    private val operationType: OperationType
        get() {
            return when {
                useMax.get() -> {
                    if (swapFrom == EstimateSwapFrom.Bancor) {
                        OperationType.SellAllCoins
                    } else {
                        OperationType.SellAllSwapPool
                    }
                }
                isBuying -> {
                    if (swapFrom == EstimateSwapFrom.Bancor) {
                        OperationType.BuyCoin
                    } else {
                        OperationType.BuySwapPool
                    }
                }
                else -> {
                    if (swapFrom == EstimateSwapFrom.Bancor) {
                        OperationType.SellCoin
                    } else {
                        OperationType.SellSwapPool
                    }

                }
            }
        }

    /**
     * @param buying
     */
    private fun onAmountChangedInternal(buying: Boolean) {
        Timber.d("OnAmountChangedInternal")
        if (buyCoin == null) {
            Timber.i("Can't exchange: coin is not set")
            return
        }

        if (account == null) {
            Timber.i("Can't exchange until user account is not loaded")
            return
        }

        if (initFeeData == null) {
            Timber.w("Can't exchange until init data not loaded")
            presenterScope.launch {
                viewState.showCalculationProgress(false)
                viewState.hideCalculation()
                viewState.setSubmitEnabled(false)
                viewState.setError("income_coin", tr(R.string.error_init_data_not_loaded))
            }
            return
        }

        viewState.setError("income_coin", null)
        val calculator = ExchangeCalculator.Builder(
                estimateRepo,
                poolsRepo,
                { accounts },
                { account!! },
                { buyCoin!! },
                { buyAmount ?: BigDecimal.ZERO },
                { sellAmount ?: BigDecimal.ZERO },
                { initFeeData!! }
        )
                .doOnSubscribe { it.disposeOnDestroy() }
                .build()

        viewState.setCalculation("")
        viewState.showCalculationProgress(true)
        calculator.calculate(isBuying,
                { res: CalculationResult ->
                    estimateAmount = res.estimate
                    gasCoin = res.gasCoin
                    swapFrom = res.swapFrom
                    exchangeRoute = res.route
                    Timber.d("Exchange Type: ${swapFrom.name}")

                    // we calculating estimate, so values are inverted
                    if (buying) {
                        sellAmount = res.amount
                    } else {
                        buyAmount = res.amount
                    }
                    if (initFeeData!!.gasRepresentingCoin.id == MinterSDK.DEFAULT_COIN_ID) {
                        viewState.setFee("${res.commissionBIP.humanize()} ${MinterSDK.DEFAULT_COIN}")
                    } else {
                        viewState.setFee(String.format("%s %s (%s %s)",
                                res.commissionBIP.humanize(),
                                MinterSDK.DEFAULT_COIN,
                                res.commissionBase.humanize(),
                                initFeeData!!.gasRepresentingCoin.symbol))
                    }

                    viewState.setSubmitEnabled(estimateAmount != null)
                    viewState.setError("income_coin", null)
                    viewState.showCalculationProgress(false)
                    viewState.setCalculation(res.calculation ?: "")
                },
                { errMessage: String, err: NodeResult.Error? ->
                    loadAndSetFee()
                    viewState.showCalculationProgress(false)
                    viewState.hideCalculation()
                    if (errMessage == "not possible to exchange") {
                        if ((buying && (buyAmountSafe == BigDecimal.ZERO)) || (!buying && (sellAmountSafe == BigDecimal.ZERO))) {
                            checkZero(if (!buying) sellAmountSafe else buyAmountSafe)
                        } else {
                            var fullMessage = errMessage
                            if (err?.data?.containsKey("pool") == true) {
                                fullMessage += ": ${err.data["pool"]}"
                            } else if (err?.data?.containsKey("bancor") == true) {
                                fullMessage += ": ${err.data["bancor"]}"
                            }
                            viewState.setError("income_coin", fullMessage)
                        }
                    } else {
                        viewState.setError("income_coin", errMessage)
                    }

                    viewState.validateForm()
                    viewState.setSubmitEnabled(false)
                }
        )


    }

    private fun onAccountSelected(coinAccount: CoinBalance?, initial: Boolean) {
        if (coinAccount == null) return
        gasCoin = coinAccount.coin.id
        account = coinAccount
        viewState.setOutAccountName("${coinAccount.coin} (${coinAccount.amount.humanize()})")
        currentCoin = coinAccount.coin

        useMax.set(false)
        clickedUseMax.set(false)

        if (!initial) {
            inputChangeSubject!!.onNext(isBuying)
        }
    }

}
