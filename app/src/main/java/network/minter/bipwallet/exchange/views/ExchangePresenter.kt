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
import network.minter.bipwallet.R
import network.minter.bipwallet.analytics.AppEvent
import network.minter.bipwallet.apis.explorer.RepoTransactions
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
import network.minter.bipwallet.internal.Wallet
import network.minter.bipwallet.internal.auth.AuthSession
import network.minter.bipwallet.internal.dialogs.ConfirmDialog
import network.minter.bipwallet.internal.dialogs.WalletProgressDialog
import network.minter.bipwallet.internal.exceptions.ErrorManager
import network.minter.bipwallet.internal.exceptions.RetryListener
import network.minter.bipwallet.internal.exceptions.humanDetailsMessage
import network.minter.bipwallet.internal.helpers.MathHelper.bdHuman
import network.minter.bipwallet.internal.helpers.MathHelper.bdNull
import network.minter.bipwallet.internal.helpers.MathHelper.bigDecimalFromString
import network.minter.bipwallet.internal.helpers.MathHelper.humanize
import network.minter.bipwallet.internal.helpers.MathHelper.toPlain
import network.minter.bipwallet.internal.mvp.MvpBasePresenter
import network.minter.bipwallet.internal.storage.RepoAccounts
import network.minter.bipwallet.internal.storage.SecretStorage
import network.minter.bipwallet.internal.storage.models.AddressListBalancesTotal
import network.minter.bipwallet.sending.ui.dialogs.TxSendSuccessDialog
import network.minter.bipwallet.tx.contract.TxInitData
import network.minter.blockchain.api.EstimateSwapFrom
import network.minter.blockchain.models.operational.*
import network.minter.core.MinterSDK
import network.minter.core.crypto.MinterAddress
import network.minter.explorer.models.*
import network.minter.explorer.repo.GateEstimateRepository
import network.minter.explorer.repo.GateGasRepository
import network.minter.explorer.repo.GateTransactionRepository
import network.minter.ledger.connector.rxjava2.RxMinterLedger
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
        private val mSession: AuthSession,
        protected val mSecretStorage: SecretStorage,
        protected val mAccountStorage: RepoAccounts,
        protected val mTxRepo: RepoTransactions,
        protected val mExplorerCoinsRepo: RepoCoins,
        protected val gasRepo: GateGasRepository,
        protected val estimateRepository: GateEstimateRepository,
        protected val mGateTxRepo: GateTransactionRepository,
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

    // used to detect which type of coin is exchanging: simple coin, token or pool token
    private var isBasicExchange = true

    protected abstract val isBuying: Boolean

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

        mAccountStorage.update()

        inputChangeSubject = BehaviorSubject.create()
        inputChangeSubject!!
                .toFlowable(BackpressureStrategy.LATEST)
                .debounce(200, TimeUnit.MILLISECONDS)
                .subscribe(
                        { buying: Boolean -> onAmountChangedInternal(buying) },
                        { t -> Timber.w(t, "Error after exchange amount changed") }
                )
//                .disposeOnDestroy()

        viewState.setSubmitEnabled(false)
        viewState.setFormValidationListener { valid ->
            Handler(Looper.getMainLooper()).post {
                viewState.setSubmitEnabled(valid && checkZero(if (isBuying) buyAmount else sellAmount))
            }
        }
        viewState.setTextChangedListener { input, valid ->
            onInputChanged(input, valid)
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

        fromAccount = if (bundle != null && bundle.containsKey(ConvertCoinActivity.EXTRA_ACCOUNT)) {
            Parcels.unwrap(bundle.getParcelable(ConvertCoinActivity.EXTRA_ACCOUNT))
        } else {
            mSecretStorage.mainWallet
        }

        mAccountStorage
                .observe()
                .joinToUi()
                .subscribe(
                        { res: AddressListBalancesTotal ->
                            val acc: AddressBalance = mAccountStorage.entity.getWalletByAddress(fromAccount!!)

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

    private val fee: BigDecimal
        get() {
            return operationType.fee.multiply(BigDecimal(gasPrice))
        }

    private fun loadAndSetFee() {
        gasRepo.minGas
                .retryWhen(errorManager.createLocalRetryWhenHandler(GateGasRepository::class.java))
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .debounce(300, TimeUnit.MILLISECONDS)
                .subscribe({ res: GateResult<GasValue> ->
                    if (res.isOk) {
                        gasPrice = res.result.gas
                        Timber.d("Min Gas price: %s", gasPrice.toString())
                        viewState.setFee(String.format("%s %s", bdHuman(fee), MinterSDK.DEFAULT_COIN))
                    }
                }) { e: Throwable? ->
                    gasPrice = BigInteger.ONE
                    Timber.w(e, "Unable to load min gas price for exchange")
                }
    }

    private fun checkZero(amount: BigDecimal?): Boolean {
        val valid = amount == null || !bdNull(amount)
        if (!valid) {
            viewState.setError("amount", "Amount must be greater than 0")
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
            val dialog = WalletProgressDialog.Builder(ctx, "Exchanging")
                    .setText(R.string.tx_convert_began)
                    .create()

            dialog.setCancelable(false)
            Observable
                    .combineLatest(
                            estimateRepository.getTransactionCount(account!!.address!!),
                            gasRepo.minGas,
                            { t1: GateResult<TxCount>, t2: GateResult<GasValue> ->
                                TxInitData(t1, t2)
                            })

                    .switchMap { initData ->
                        // if error occurred upper, notify error
                        if (!initData.isSuccess) {
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
            dialog: WalletProgressDialog,
            txData: ConvertTransactionData,
            initData: TxInitData,
            balance: BigDecimal): ObservableSource<GateResult<PushResult>> {

        // creating tx
        val tx = txData.build(initData.nonce!!.add(BigInteger.ONE), initData.gas!!, balance)
        isBasicExchange = txData.isBasicExchange

        // if user created account with ledger, use it to sign tx
        return if (mSession.role == AuthSession.AuthType.Hardware) {
            dialog.setText("Please, compare transaction hashes: %s", tx.unsignedTxHash)
            Timber.d("Unsigned tx hash: %s", tx.unsignedTxHash)
            signSendTxExternally(tx, dialog)
        } else {
            // old school signing
            signSendTxInternally(tx)
        }
    }

    private fun signSendTxInternally(tx: Transaction): ObservableSource<GateResult<PushResult>> {
        val data = mSecretStorage.getSecret(account!!.address!!)
        val sign = tx.signSingle(data.privateKey)!!
        return safeSubscribeIoToUi(
                mGateTxRepo.sendTransaction(sign)
                        .onErrorResumeNext(toGateError())
        )
    }

    private fun signSendTxExternally(tx: Transaction, dialog: WalletProgressDialog): ObservableSource<GateResult<PushResult>> {
        val devInstance = Wallet.app().ledger()
        if (!devInstance.isReady) {
            dialog.setText("Please, connect ledger and open Minter Application")
        }
        return RxMinterLedger
                .initObserve(devInstance)
                .flatMap { dev: RxMinterLedger ->
                    dialog.setText("Please, compare hashes: " + tx.unsignedTxHash.toHexString())
                    dev.signTxHash(tx.unsignedTxHash)
                }
                .toObservable()
                .switchMap { signatureSingleData: SignatureSingleData? ->
                    val sign: TransactionSign = tx.signExternal(signatureSingleData)
                    dialog.setText(R.string.tx_convert_in_progress)
                    safeSubscribeIoToUi(
                            mGateTxRepo.sendTransaction(sign).onErrorResumeNext(toGateError())
                    )
                }
                .doFinally { devInstance.destroy() }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
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

        mAccountStorage.update(true)
        mTxRepo.update(true)

        if (isBuying) {
            saveExchangeAmount(buyCoin!!, buyAmount!!)

            // for buy tab just show how much we've got
            showSuccessDialog(result.result.hash.toString(), buyAmount!!, buyCoin!!)
            return
        }

        // get transaction from explorer to show exact value what we've got after sell
        // todo: show "Coins have been exchanged" if can't get transaction after 30 seconds
        mTxRepo.entity.waitTransactionUntilUncommitted(result.result.hash.toString())
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
            ConfirmDialog.Builder(ctx, "Unable to send transaction")
                    .setText((errorResult.humanError()))
                    .setPositiveAction(R.string.btn_close)
                    .create()
        }
    }

    private fun onErrorRequest(errorResult: ExpResult<*>) {
        Timber.e("Unable to send transaction: %s", errorResult.error?.message)
        viewState.startDialog { ctx: Context ->
            ConfirmDialog.Builder(ctx, "Unable to send transaction")
                    .setText((errorResult.message ?: "Caused unknown error"))
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
        viewState.startAccountSelector(mAccountStorage.entity.getWalletByAddress(fromAccount!!).coinsList) {
            onAccountSelected(it.data, false)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onInputChanged(editText: InputWrapper, valid: Boolean) {
        val text = editText.text.toString()
        Timber.d("Input changed: %s", editText.text)
        loadAndSetFee()
        when (editText.id) {
            R.id.input_incoming_coin -> {
                mExplorerCoinsRepo.entity.findByName(text)
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
                val am = bigDecimalFromString(text)
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
                                type, gasCoin!!, account!!.coin, buyCoin!!, amount!!, estimateAmount!!, swapFrom)

                        onStartExecuteTransaction(txData)
                    }
                    .setNegativeAction(R.string.btn_cancel)
                    .create()
        }
    }

    private val operationType: OperationType
        get() {
            return when {
                useMax.get() -> OperationType.SellAllCoins
                isBuying -> OperationType.BuyCoin
                else -> OperationType.SellCoin
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
        val calculator = ExchangeCalculator.Builder(
                estimateRepository,
                { accounts },
                { account!! },
                { buyCoin!! },
                { buyAmount ?: BigDecimal.ZERO },
                { sellAmount ?: BigDecimal.ZERO })
                .doOnSubscribe { it.disposeOnDestroy() }
                .build()

        viewState.setCalculation("")
        viewState.showCalculationProgress(true)
        calculator.calculate(operationType,
                { res: CalculationResult ->
                    estimateAmount = res.estimate
                    gasCoin = res.gasCoin
                    swapFrom = res.swapFrom

                    // we calculating estimate, so values are inverted
                    if (buying) {
                        sellAmount = res.amount
                    } else {
                        buyAmount = res.amount
                    }
                    viewState.setError("income_coin", null)
                    viewState.showCalculationProgress(false)
                    viewState.setCalculation(res.calculation ?: "")
                },
                { err: String ->
                    viewState.showCalculationProgress(false)
                    viewState.hideCalculation()
                    viewState.setError("income_coin", err)
                }
        )
    }

    private fun onAccountSelected(coinAccount: CoinBalance?, initial: Boolean) {
        if (coinAccount == null) return
        gasCoin = coinAccount.coin.id
        account = coinAccount
        viewState.setOutAccountName("${coinAccount.coin} (${coinAccount.amount.humanize()})")
        currentCoin = coinAccount.coin

        if (!initial) {
            inputChangeSubject!!.onNext(isBuying)
        }
    }

}
