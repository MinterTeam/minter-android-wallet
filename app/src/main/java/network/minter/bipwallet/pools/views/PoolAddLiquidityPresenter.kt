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

package network.minter.bipwallet.pools.views

import android.content.Context
import android.content.Intent
import com.edwardstock.inputfield.form.InputWrapper
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.launch
import moxy.InjectViewState
import moxy.presenterScope
import network.minter.bipwallet.R
import network.minter.bipwallet.apis.explorer.RepoTransactions
import network.minter.bipwallet.apis.gate.TxInitDataRepository
import network.minter.bipwallet.apis.reactive.ReactiveExplorer
import network.minter.bipwallet.apis.reactive.toObservable
import network.minter.bipwallet.internal.dialogs.ConfirmDialog
import network.minter.bipwallet.internal.dialogs.WalletProgressDialog
import network.minter.bipwallet.internal.exceptions.ErrorManager
import network.minter.bipwallet.internal.helpers.MathHelper.addPercent
import network.minter.bipwallet.internal.helpers.MathHelper.asCurrency
import network.minter.bipwallet.internal.helpers.MathHelper.bdNull
import network.minter.bipwallet.internal.helpers.MathHelper.humanize
import network.minter.bipwallet.internal.helpers.MathHelper.isNotZero
import network.minter.bipwallet.internal.helpers.MathHelper.plain
import network.minter.bipwallet.internal.helpers.ViewExtensions.tr
import network.minter.bipwallet.internal.mvp.MvpBasePresenter
import network.minter.bipwallet.internal.storage.RepoAccounts
import network.minter.bipwallet.internal.storage.SecretStorage
import network.minter.bipwallet.internal.storage.models.AddressListBalancesTotal
import network.minter.bipwallet.pools.contracts.PoolAddLiquidityView
import network.minter.bipwallet.pools.models.PoolCombined
import network.minter.bipwallet.pools.models.PoolsFilter
import network.minter.bipwallet.pools.repo.RepoCachedUserPools
import network.minter.bipwallet.pools.ui.PoolAddLiquidityActivity
import network.minter.bipwallet.pools.ui.TxLiquidityStartDialog
import network.minter.bipwallet.sending.ui.dialogs.TxSendSuccessDialog
import network.minter.bipwallet.tx.TransactionSender
import network.minter.bipwallet.tx.contract.TxInitData
import network.minter.blockchain.models.operational.OperationType
import network.minter.blockchain.models.operational.Transaction
import network.minter.blockchain.models.operational.TransactionSign
import network.minter.core.MinterSDK
import network.minter.explorer.models.*
import network.minter.explorer.repo.ExplorerPoolsRepository
import network.minter.explorer.repo.GateTransactionRepository
import timber.log.Timber
import java.math.BigDecimal
import java.math.BigDecimal.ZERO
import java.math.BigInteger
import java.math.RoundingMode
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

/**
 * minter-android-wallet. 2021
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
@InjectViewState
class PoolAddLiquidityPresenter @Inject constructor() : MvpBasePresenter<PoolAddLiquidityView>() {

    @Inject lateinit var poolsRepo: ExplorerPoolsRepository
    @Inject lateinit var userPoolsRepo: RepoCachedUserPools
    @Inject lateinit var accountStorage: RepoAccounts
    @Inject lateinit var txRepo: RepoTransactions
    @Inject lateinit var secretStorage: SecretStorage
    @Inject lateinit var initDataRepo: TxInitDataRepository
    @Inject lateinit var gateRepo: GateTransactionRepository
    @Inject lateinit var errorManager: ErrorManager

    private var pool: PoolCombined? = null
    private var price0: BigDecimal = BigDecimal.ONE
    private var price1: BigDecimal = BigDecimal.ONE

    private var coin0: CoinItemBase? = null
    private var coin1: CoinItemBase? = null
    private var amount0: BigDecimal = ZERO
    private var amount1: BigDecimal = ZERO
    private var slippage: BigDecimal = BigDecimal("5.0")
    private var maxAmount: BigDecimal = BigDecimal("0")
    private var coin0Account: CoinBalance? = null
    private var coin1Account: CoinBalance? = null
    private var txSender: TransactionSender? = null
    private var useMax: Boolean = false
    private val clickedUseMax = AtomicBoolean(false)
    private var gas = BigInteger.ONE
    private var initFeeData: TxInitData? = null
    private var ignoreInputAmount: AtomicBoolean = AtomicBoolean(false)
    private var ignoreInputSlippage: AtomicBoolean = AtomicBoolean(false)

    override fun handleExtras(intent: Intent?) {
        super.handleExtras(intent)

        if (intent == null) {
            throw IllegalStateException("Can't start view without arguments")
        }

        pool = intent.getParcelableExtra(PoolAddLiquidityActivity.ARG_POOL)
        if (pool == null) {
            throw IllegalStateException("PoolCombined must be set")
        }

        // farming does not have full information about pool, so we must load it from explorer
        if (pool!!.filter != PoolsFilter.None) {
            poolsRepo.getPoolByPair(pool!!.pool.coin0.id, pool!!.pool.coin1.id)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            { res ->
                                if(res.isOk) {
                                    pool!!.pool = res.result
                                    init()
                                } else {
                                    onErrorLoadPool(res)
                                }
                            },
                            { t ->
                                Timber.e(t, "Unable to get pool by coins")
                                onErrorLoadPool(ReactiveExplorer.createExpErrorPlain<Any>(t))
                            }
                    )
        } else {
            init()
        }

        loadAndSetFee()
    }

    private fun onErrorLoadPool(errorResult: ExpResult<*>) {
        viewState.startDialog {
            val d = ConfirmDialog.Builder(it, R.string.error_unable_load_pool)
                    .setDescription("${pool!!.pool.coin0} / ${pool!!.pool.coin1}")
                    .setText(errorResult.message?:"Unknown error")
                    .setNegativeAction(R.string.btn_close) { d, _ ->
                        d.dismiss()
                        viewState.finishCancel()
                    }
                    .create()
            d.setCancelable(false)
            d
        }
    }

    private fun loadAndSetFee() {
        initDataRepo.loadFeeWithTx()
                .retryWhen(errorManager.retryWhenHandler)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(
                        { res: TxInitData ->
                            gas = res.gas!!
                            initFeeData = res
                            viewState.setFee(res.calculateFeeText(OperationType.AddLiquidity))
                        },
                        { e ->
                            gas = BigInteger.ONE
                            Timber.w(e, "Unable to get min gas price for sending")
                        }
                )
                .disposeOnDestroy()
    }

    override fun attachView(view: PoolAddLiquidityView) {
        super.attachView(view)
        viewState.setOnTextChangedListener(this::onInputTextChanged)
        viewState.setOnSwapCoins {
            val tmpCoin0 = coin0!!
            val tmpAmount0 = amount0
            val tmpPrice0 = price0
            val tmpAccount0 = coin0Account
            coin0 = coin1
            coin1 = tmpCoin0
            amount0 = amount1
            amount1 = tmpAmount0
            price0 = price1
            price1 = tmpPrice0
            coin0Account = coin1Account
            coin1Account = tmpAccount0

            viewState.setCoin0SuffixEnable(coin0Account!!.amount.isNotZero())
            viewState.setCoin1SuffixEnable(coin1Account!!.amount.isNotZero())

            checkEnoughBalance()

            ignoreInputAmount.set(true)
            setupData()
        }
        viewState.setOnSubmit {
            onSubmit()
        }
    }


    private fun init() {
        price0 = pool!!.pool.amount1.divide(pool!!.pool.amount0, RoundingMode.HALF_UP)
        price1 = pool!!.pool.amount0.divide(pool!!.pool.amount1, RoundingMode.HALF_UP)

        coin0 = pool!!.pool.coin0
        coin1 = pool!!.pool.coin1

        accountStorage
                .observe()
                .joinToUi()
                .subscribe(
                        { res: AddressListBalancesTotal ->
                            if (!res.isEmpty) {
                                val acc = accountStorage.entity.mainWallet
                                coin0Account = acc
                                        .findCoin(coin0!!.id)
                                        .orElse(CoinBalance(coin0!!, ZERO, ZERO, secretStorage.mainWallet))
                                coin1Account = acc
                                        .findCoin(coin1!!.id)
                                        .orElse(CoinBalance(coin1!!, ZERO, ZERO, secretStorage.mainWallet))

                                viewState.setCoin0SuffixEnable(coin0Account!!.amount.isNotZero())
                                viewState.setCoin1SuffixEnable(coin1Account!!.amount.isNotZero())

                                viewState.setOnClickUseMax0 {
                                    viewState.setCoin0(coin0Account!!.amount, coin0!!.symbol)
                                    useMax = true
                                    clickedUseMax.set(true)
                                }
                                viewState.setOnClickUseMax1 {
                                    viewState.setCoin1(coin1Account!!.amount, coin1!!.symbol)
                                    useMax = true
                                    clickedUseMax.set(true)
                                }
                            }
//                            viewState.showBalanceProgress(false)
                        },
                        { t: Throwable ->
                            Timber.w(t, "Unable to load balance for sending")
                        }
                )
                .disposeOnDestroy()

        setupData()
    }

    private fun setupData() {
        ignoreInputAmount.set(true)
        viewState.setCoin0(amount0, coin0!!.symbol)
        ignoreInputAmount.set(true)
        viewState.setCoin1(amount1, coin1!!.symbol)
        ignoreInputSlippage.set(true)
        viewState.setSlippage(slippage.asCurrency())
        ignoreInputSlippage.set(true)
        viewState.setMaxAmount(maxAmount.plain())

        viewState.setSlippageLabel(tr(R.string.label_slippage, coin1!!.symbol))
        viewState.setMaxAmountLabel(tr(R.string.label_max_amount_spend, coin1!!.symbol))
    }

    private fun checkEnoughBalance() {
        if(coin0Account?.amount?: ZERO < amount0) {
            viewState.setCoin0Error(tr(R.string.account_err_insufficient_funds_for_amount, (coin0Account?.amount?:ZERO).humanize(), coin0!!.symbol ))
        } else {
            viewState.setCoin0Error(null)
        }
        if(coin1Account?.amount?: ZERO < amount1) {
            viewState.setCoin1Error(tr(R.string.account_err_insufficient_funds_for_amount, (coin1Account?.amount?:ZERO).humanize(), coin1!!.symbol ))
        } else {
            viewState.setCoin1Error(null)
        }
        if(coin1Account?.amount?: ZERO < maxAmount) {
            viewState.setMaxAmountError(tr(R.string.account_err_insufficient_funds_for_amount, (coin1Account?.amount?:ZERO).humanize(), coin1!!.symbol ))
        } else {
            viewState.setMaxAmountError(null)
        }
    }

    private fun onInputTextChanged(input: InputWrapper, valid: Boolean) {
        presenterScope.launch {
            val s = input.text.toString()
            val sNum = if (s.isEmpty()) BigDecimal("0") else BigDecimal(s)
            when (input.id) {
                R.id.input_coin0 -> {
                    if (ignoreInputAmount.getAndSet(false)) {
                        return@launch
                    }
                    viewState.calcCoin0Padding(coin0!!.symbol)
                    ignoreInputAmount.set(true)
                    amount0 = sNum
                    amount1 = amount0 * price0
                    if (bdNull(slippage)) {
                        slippage = BigDecimal("5.0")
                    }
                    maxAmount = amount1.addPercent(slippage)
                    viewState.setCoin1(amount1, coin1!!.symbol)
                    viewState.setMaxAmount(maxAmount.plain())

                    checkEnoughBalance()

                    if (!clickedUseMax.get()) {
                        useMax = false
                    }

                    clickedUseMax.set(false)
                }
                R.id.input_coin1 -> {
                    if (ignoreInputAmount.getAndSet(false)) {
                        return@launch
                    }
                    ignoreInputAmount.set(true)
                    amount1 = sNum
                    amount0 = amount1 * price1
                    if (bdNull(slippage)) {
                        slippage = BigDecimal("5.0")
                    }
                    maxAmount = amount1.addPercent(slippage)
                    viewState.calcCoin1Padding(coin1!!.symbol)
                    viewState.setCoin0(amount0, coin0!!.symbol)
                    viewState.setMaxAmount(maxAmount.plain())

                    checkEnoughBalance()

                    if (!clickedUseMax.get()) {
                        useMax = false
                    }

                    clickedUseMax.set(false)
                }
                R.id.input_slippage -> {
                    if (ignoreInputSlippage.getAndSet(false)) {
                        return@launch
                    }
                    ignoreInputSlippage.set(true)
                    slippage = sNum.setScale(18, RoundingMode.HALF_UP)
                    maxAmount = amount1.setScale(18, RoundingMode.HALF_UP).addPercent(slippage)
                    viewState.setMaxAmount(maxAmount.plain())

                    if (slippage > BigDecimal("100.00")) {
                        viewState.setSlippageError(tr(R.string.input_pool_error_slippage_exceed))
                    }
                }
                R.id.input_max_spend -> {
                    if (ignoreInputSlippage.getAndSet(false)) {
                        return@launch
                    }
                    ignoreInputSlippage.set(true)
                    maxAmount = sNum
                    if (maxAmount < amount1) {
                        maxAmount = amount1
                        viewState.setMaxAmountError(tr(R.string.input_pool_error_max_amount_too_low))
                    } else {
                        viewState.setMaxAmountError(null)
                    }

                    slippage = if (maxAmount.isNotZero()) {
                        ((maxAmount.setScale(18, RoundingMode.HALF_UP).divide(amount1.setScale(18, RoundingMode.HALF_UP), RoundingMode.HALF_UP)) * BigDecimal("100.0")) - BigDecimal("100.0")
                    } else {
                        ZERO
                    }
                    viewState.setSlippage(slippage.asCurrency())
                }
            }
            viewState.setEnableSubmit(valid && coin0Account?.amount.isNotZero() && coin1Account?.amount.isNotZero())
        }
    }

    private fun onSubmit() {
        txSender = TransactionSender(secretStorage.mainWallet, initDataRepo, gateRepo)
        txSender!!.startListener = {
            viewState.startDialog { ctx ->
                TxLiquidityStartDialog.Builder(ctx, R.string.tx_send_overall_title)
                        .setTxTitle(R.string.dialog_title_add_liquidity)
                        .setCoins(coin0!!, coin1!!)
                        .setAmounts(amount0, amount1)
                        .setSubAmount1(maxAmount.humanize() + " max")
                        .setPositiveAction(R.string.btn_confirm) { _, _ ->
                            txSender!!.next()
                        }
                        .setNegativeAction(R.string.btn_cancel) { d, _ ->
                            d.dismiss()
                        }
                        .create()
            }
        }
        txSender!!.progressListener = {
            viewState.startDialog { ctx ->
                val dialog = WalletProgressDialog.Builder(ctx, R.string.tx_send_in_progress)
                        .setText(R.string.tx_convert_began)
                        .create()

                dialog.setCancelable(false)
                dialog
            }
        }
        txSender!!.txCreatorObservable = ::signTx
        txSender!!.successCallback = ::onSuccessExecuteTransaction
        txSender!!.errorCallback = ::onErrorExecuteTransaction

        txSender!!.start()
    }

    private fun onSuccessExecuteTransaction(result: GateResult<PushResult>) {
        accountStorage.update(true)
        txRepo.update(true)
        userPoolsRepo.update(true)

        viewState.startDialog {
            TxSendSuccessDialog.Builder(it)
                    .setLabel(R.string.dialog_subtitle_liquidity_added)
                    .setValue(tr(R.string.dialog_subtitle_liquidity_pool_name, coin0!!, coin1!!))
                    .setPositiveAction(R.string.btn_view_tx) { d, _ ->
                        d.dismiss()
                        viewState.startExplorer(result.result!!.hash)
                        viewState.finishSuccess()
                    }
                    .setNegativeAction(R.string.btn_close) { d, _ ->
                        d.dismiss()
                        viewState.finishSuccess()
                    }
                    .create()
        }
    }

    private fun onErrorExecuteTransaction(errorResult: GateResult<*>) {
        Timber.e(errorResult.message, "Unable to send transaction")
        viewState.startDialog { ctx: Context ->
            ConfirmDialog.Builder(ctx, R.string.dialog_title_err_unable_to_send_tx)
                    .setText((errorResult.message?: "Unknown error"))
                    .setPositiveAction(R.string.btn_close) { d, _ ->
                        d.dismiss()
                    }
                    .create()
        }
    }


    private fun signTx(initData: TxInitData): Observable<TransactionSign> {
        val txBuilder = Transaction.Builder(initData.nonce)
        txBuilder.setGasPrice(initData.gas ?: BigInteger.ONE)
        txBuilder.setGasCoinId(MinterSDK.DEFAULT_COIN_ID)

        val txPre = txBuilder.addLiquidity()
        txPre.coin0 = coin0!!.id
        txPre.coin1 = coin1!!.id

        if (useMax) {
            txPre.volume = coin0Account!!.amount

            if (coin0!!.id == MinterSDK.DEFAULT_COIN_ID) {
                txPre.volume = txPre.volume - initData.calculateFeeInBip(OperationType.AddLiquidity)
            }
        } else {
            txPre.volume = amount0
        }

        txPre.maximumVolume = maxAmount

        val tx = txPre.build().signSingle(secretStorage.mainSecret.privateKey)!!

        return tx.toObservable()
    }
}