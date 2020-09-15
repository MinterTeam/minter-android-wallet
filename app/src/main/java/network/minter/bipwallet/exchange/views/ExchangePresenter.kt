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
package network.minter.bipwallet.exchange.views

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.annotation.CallSuper
import com.annimon.stream.Stream
import com.edwardstock.inputfield.form.InputWrapper
import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
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
        protected val mGateTxRepo: GateTransactionRepository
) : MvpBasePresenter<V>() {
    private var mAccount: CoinBalance? = null
    private var mCurrentCoin: CoinItemBase? = null
    private var mBuyCoin: CoinItemBase? = null
    private var mSellAmount: BigDecimal? = BigDecimal(0)
    private var mBuyAmount: BigDecimal? = BigDecimal(0)
    private var mInputChange: BehaviorSubject<Boolean>? = null
    private var mGasCoin: BigInteger? = null
    private var mAccounts: List<CoinBalance> = ArrayList(1)
    private val mUseMax = AtomicBoolean(false)
    private val mClickedUseMax = AtomicBoolean(false)
    private var mGasPrice = BigInteger("1")
    private var mEstimate: BigDecimal? = null
    private var exchangeAmount: ExchangeAmount? = null
    private var buyForResult = false
    private var fromAccount: MinterAddress? = null

    protected abstract val isBuying: Boolean

    override fun attachView(view: V) {
        super.attachView(view)
        loadAndSetFee()
    }

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()

        mAccountStorage.update()

        mInputChange = BehaviorSubject.create()
        mInputChange!!
                .toFlowable(BackpressureStrategy.LATEST)
                .debounce(200, TimeUnit.MILLISECONDS)
                .subscribe(
                        { buying: Boolean -> onAmountChangedInternal(buying) },
                        { t -> Timber.w(t, "Error after exchange amount changed") }
                )
//                .disposeOnDestroy()

        viewState.setSubmitEnabled(false)
        viewState.setFormValidationListener { valid ->
            viewState.setSubmitEnabled(valid && checkZero(if (isBuying) mBuyAmount else mSellAmount))
        }
        viewState.setTextChangedListener { input, valid ->
            onInputChanged(input, valid)
        }
        viewState.setOnClickSelectAccount(View.OnClickListener { view: View -> onClickSelectAccount(view) })
        viewState.setOnClickMaximum(View.OnClickListener { onClickMaximum() })
        viewState.setOnClickSubmit(View.OnClickListener { onSubmit() })
        setCoinsAutocomplete()
    }

    override fun handleExtras(bundle: Bundle?) {
        super.handleExtras(bundle)
        if (bundle != null) {
            if (bundle.containsKey(ConvertCoinActivity.EXTRA_COIN_TO_BUY)) {
                mBuyCoin = Parcels.unwrap(bundle.getParcelable(ConvertCoinActivity.EXTRA_COIN_TO_BUY))
                mBuyAmount = BigDecimal(bundle.getString(ConvertCoinActivity.EXTRA_VALUE_TO_BUY))
                buyForResult = true
                viewState.setIncomingCoin(mBuyCoin!!.symbol)
                if (isBuying) {
                    viewState.setAmount(mBuyAmount!!.toPlainString())
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
                .retryWhen(errorResolver)
                .observe()
                .joinToUi()
                .subscribe(
                        { res: AddressListBalancesTotal ->
                            val acc: AddressBalance = mAccountStorage.entity.getWalletByAddress(fromAccount!!)

                            if (!res.isEmpty) {
                                mAccounts = acc.coinsList
                                mAccount = acc.getCoin(MinterSDK.DEFAULT_COIN_ID)
                                if (mCurrentCoin != null) {
                                    mAccount = Stream.of(mAccounts).filter { value: CoinBalance -> (value.coin == mCurrentCoin) }.findFirst().orElse(mAccount)
                                }
                                onAccountSelected(mAccount, true)

                                if (mBuyAmount != null && mBuyCoin != null) {
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

    private fun loadAndSetFee() {
        gasRepo.minGas
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .debounce(300, TimeUnit.MILLISECONDS)
                .subscribe({ res: GateResult<GasValue> ->
                    if (res.isOk) {
                        mGasPrice = res.result.gas
                        Timber.d("Min Gas price: %s", mGasPrice.toString())
                        viewState.setFee(String.format("%s %s", bdHuman(operationType.fee.multiply(BigDecimal(mGasPrice))), MinterSDK.DEFAULT_COIN))
                    }
                }) { e: Throwable? ->
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
            mBuyCoin = item
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
                            estimateRepository.getTransactionCount(mAccount!!.address!!),
                            gasRepo.minGas,
                            BiFunction { t1: GateResult<TxCount>, t2: GateResult<GasValue> ->
                                TxInitData(t1, t2)
                            })

                    .switchMap { initData ->
                        // if error occurred upper, notify error
                        if (!initData.isSuccess) {
                            return@switchMap initData.errorResult!!.castErrorResultTo<PushResult>().toObservable()
                        }
                        var balance = BigDecimal("0")
                        if (operationType == OperationType.SellCoin || operationType == OperationType.SellAllCoins) {
                            balance = mAccount!!.amount
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
        val tx = txData.build(initData.nonce!!.add(BigInteger.ONE), initData.gas, balance)

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
        val data = mSecretStorage.getSecret(mAccount!!.address!!)
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
            saveExchangeAmount(mBuyCoin!!, mBuyAmount!!)

            // for buy tab just show how much we've got
            showSuccessDialog(result.result.data.hash.toString(), mBuyAmount!!, mBuyCoin!!)
            return
        }

        // get transaction from explorer to show exact value what we've got after sell
        // todo: show "Coins have been exchanged" if can't get transaction after 30 seconds
        mTxRepo.entity.waitTransactionUntilUncommitted(result.result.data.hash.toString())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(
                        { r ->
                            if (r.isOk) {
                                val data = r.result.getData<HistoryTransaction.TxConvertCoinResult>()
                                saveExchangeAmount(data.coinToBuy, data.valueToBuy)
                                showSuccessDialog(r.result.hash.toString(), data.valueToBuy, data.coinToBuy)
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
        if (mAccount == null) {
            return
        }
        viewState.setAmount(mAccount!!.amount.toPlain())
        mUseMax.set(true)
        mClickedUseMax.set(true)
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
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io())
                        .subscribe(
                                {
                                    mBuyCoin = it
                                    mInputChange!!.onNext(isBuying)
                                },
                                { t ->
                                    mBuyCoin = null
                                    mInputChange!!.onNext(isBuying)
                                    Timber.d(t, "Unable to find coin by name %s", text)
                                }
                        )
            }
            R.id.input_amount -> {
                val am = bigDecimalFromString(text)
                checkZero(am)
                if (isBuying) {
                    mBuyAmount = am
                } else {
                    if (!mClickedUseMax.get()) {
                        mUseMax.set(false)
                    }
                    mClickedUseMax.set(false)
                    mSellAmount = am
                }
//                viewState.setSubmitEnabled(mAccount != null && am <= mAccount!!.amount)
                mInputChange!!.onNext(isBuying)
            }
        }
    }

    private fun onSubmit() {
        if (mAccount == null || mBuyCoin == null || mBuyAmount == null || mSellAmount == null) {
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
                    .setFirstValue(mSellAmount!!.humanize())
                    .setFirstCoin(mAccount!!.coin!!.symbol)
                    .setSecondLabel(R.string.dialog_label_exchange_to_get)
                    .setSecondValue(mBuyAmount!!)
                    .setSecondCoin(mBuyCoin!!.symbol)
                    .setPositiveAction(R.string.btn_confirm) { _, _ ->
                        val type: ConvertTransactionData.Type = when {
                            mUseMax.get() -> ConvertTransactionData.Type.SellAll
                            isBuying -> ConvertTransactionData.Type.Buy
                            else -> ConvertTransactionData.Type.Sell
                        }
                        val amount = if (isBuying) mBuyAmount else mSellAmount
                        val txData = ConvertTransactionData(
                                type, mGasCoin, mAccount!!.coin, mBuyCoin, amount, mEstimate)

                        onStartExecuteTransaction(txData)
                    }
                    .setNegativeAction(R.string.btn_cancel)
                    .create()
        }
    }

    private val operationType: OperationType
        get() {
            return when {
                mUseMax.get() -> OperationType.SellAllCoins
                isBuying -> OperationType.BuyCoin
                else -> OperationType.SellCoin
            }
        }

    /**
     * @param buying
     */
    private fun onAmountChangedInternal(buying: Boolean) {
        Timber.d("OnAmountChangedInternal")
        if (mBuyCoin == null) {
            Timber.i("Can't exchange: coin is not set")
            return
        }
        if (mAccount == null) {
            Timber.i("Can't exchange until user account is not loaded")
            return
        }
        val calculator = ExchangeCalculator.Builder(
                estimateRepository,
                { mAccounts },
                { mAccount!! },
                { mBuyCoin!! },
                { mBuyAmount ?: BigDecimal.ZERO },
                { mSellAmount ?: BigDecimal.ZERO })
                .doOnSubscribe { it.disposeOnDestroy() }
                .build()

        viewState.setCalculation("")
        viewState.showCalculationProgress(true)
        calculator.calculate(operationType,
                { res: CalculationResult ->
                    mEstimate = res.estimate
                    mGasCoin = res.gasCoin

                    // we calculating estimate, so values are inverted
                    if (buying) {
                        mSellAmount = res.amount
                    } else {
                        mBuyAmount = res.amount
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
        mGasCoin = coinAccount.coin.id
        mAccount = coinAccount
        viewState.setOutAccountName("${coinAccount.coin} (${coinAccount.amount.humanize()})")
        mCurrentCoin = coinAccount.coin

        if (!initial) {
            mInputChange!!.onNext(isBuying)
        }
    }

}