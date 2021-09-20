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
import android.view.View
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
import network.minter.bipwallet.apis.reactive.toObservable
import network.minter.bipwallet.internal.dialogs.ConfirmDialog
import network.minter.bipwallet.internal.dialogs.WalletProgressDialog
import network.minter.bipwallet.internal.exceptions.ErrorManager
import network.minter.bipwallet.internal.helpers.MathHelper.asBigDecimal
import network.minter.bipwallet.internal.helpers.MathHelper.bdNull
import network.minter.bipwallet.internal.helpers.MathHelper.humanize
import network.minter.bipwallet.internal.helpers.MathHelper.percent
import network.minter.bipwallet.internal.helpers.MathHelper.removePercent
import network.minter.bipwallet.internal.helpers.ViewExtensions.tr
import network.minter.bipwallet.internal.mvp.MvpBasePresenter
import network.minter.bipwallet.internal.storage.RepoAccounts
import network.minter.bipwallet.internal.storage.SecretStorage
import network.minter.bipwallet.pools.contracts.PoolRemoveLiquidityView
import network.minter.bipwallet.pools.models.PoolCombined
import network.minter.bipwallet.pools.models.PoolsFilter
import network.minter.bipwallet.pools.repo.RepoCachedUserPools
import network.minter.bipwallet.pools.ui.PoolRemoveLiquidityActivity
import network.minter.bipwallet.pools.ui.TxLiquidityStartDialog
import network.minter.bipwallet.sending.ui.dialogs.TxSendSuccessDialog
import network.minter.bipwallet.tx.TransactionSender
import network.minter.bipwallet.tx.contract.TxInitData
import network.minter.blockchain.models.operational.OperationType
import network.minter.blockchain.models.operational.Transaction
import network.minter.blockchain.models.operational.TransactionSign
import network.minter.core.MinterSDK
import network.minter.explorer.models.CoinItemBase
import network.minter.explorer.models.GateResult
import network.minter.explorer.models.PushResult
import network.minter.explorer.repo.ExplorerPoolsRepository
import network.minter.explorer.repo.GateTransactionRepository
import timber.log.Timber
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

/**
 * minter-android-wallet. 2021
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
@InjectViewState
class PoolRemoveLiquidityPresenter @Inject constructor() : MvpBasePresenter<PoolRemoveLiquidityView>() {
    @Inject lateinit var poolsRepo: ExplorerPoolsRepository
    @Inject lateinit var userPoolsRepo: RepoCachedUserPools
    @Inject lateinit var accountStorage: RepoAccounts
    @Inject lateinit var txRepo: RepoTransactions
    @Inject lateinit var secretStorage: SecretStorage
    @Inject lateinit var initDataRepo: TxInitDataRepository
    @Inject lateinit var gateRepo: GateTransactionRepository
    @Inject lateinit var errorManager: ErrorManager

    private val percentError: BigDecimal = BigDecimal("100")- BigDecimal(1e-2)

    private var gas = BigInteger.ONE
    private var initFeeData: TxInitData? = null

    private var pool: PoolCombined? = null

    private var userLiquidity: BigDecimal = BigDecimal.ZERO
    private var liquidityPercent: BigDecimal = BigDecimal("100")

    private var txSender: TransactionSender? = null

    private val amount0: BigDecimal
        get() {
            return pool?.stake?.amount0?: BigDecimal.ZERO
        }

    private val amount1: BigDecimal
        get() {
            return pool?.stake?.amount1?: BigDecimal.ZERO
        }

    private val coin0: CoinItemBase
        get() {
            return pool!!.stake!!.coin0
        }

    private val coin1: CoinItemBase
        get() {
            return pool!!.stake!!.coin1
        }

    private val liquidity: BigDecimal
        get() {
            return pool?.stake?.liquidity?: BigDecimal.ZERO
        }



    override fun handleExtras(intent: Intent?) {
        super.handleExtras(intent)

        if (intent == null) {
            throw IllegalStateException("Can't start view without arguments")
        }

        pool = intent.getParcelableExtra(PoolRemoveLiquidityActivity.ARG_POOL)
        if (pool == null) {
            throw IllegalStateException("PoolCombined must be set")
        }

        // farming does not have full information about pool, so we need to load it from explorer
        if (pool!!.filter == PoolsFilter.Farming) {
            poolsRepo.getPoolByPair(coin0.id, coin1.id)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            { res ->
                                pool!!.pool = res.result
                                init()
                            },
                            { t ->
                                Timber.e(t)
                            }
                    )
        } else {
            init()
        }

        loadAndSetFee()

    }

    private fun init() {
        userLiquidity = liquidity
        viewState.setCoin0(amount0, coin0)
        viewState.setCoin0(amount1, coin1)
        viewState.setTokenAmount(userLiquidity)
        viewState.setTokenAmountValidator(userLiquidity)
        viewState.setLiquidityPercent(liquidityPercent)
        viewState.setOnUseMaxClickListener {
            viewState.setTokenAmount(liquidity)
        }
    }

    private fun loadAndSetFee() {
        initDataRepo.loadFeeWithTx()
                .retryWhen(errorManager.retryWhenHandler)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(
                        { res: TxInitData ->
                            gas = res.gas?: BigInteger.ONE
                            initFeeData = res
                            viewState.setFee(res.calculateFeeText(OperationType.RemoveLiquidity))
                        },
                        { e ->
                            gas = BigInteger.ONE
                            Timber.w(e, "Unable to get min gas price for sending")
                        }
                )
                .disposeOnDestroy()
    }

    override fun attachView(view: PoolRemoveLiquidityView) {
        super.attachView(view)
        viewState.setOnTextChangedListener(this::onInputTextChanged)
        viewState.setOnSubmit(this::onSubmit)
    }

    private val ignoreInput: AtomicBoolean = AtomicBoolean(false)

    private fun onInputTextChanged(input: InputWrapper, valid: Boolean) {
        presenterScope.launch {
            val s = input.text.toString()
            val sNum = s.asBigDecimal()

            when (input.id) {
                R.id.input_token -> {
                    if (ignoreInput.getAndSet(false)) {
                        return@launch
                    }
                    ignoreInput.set(true)

                    if (bdNull(sNum)) {
                        userLiquidity = sNum
                        viewState.setLiquidityPercent(BigDecimal.ZERO)
                    } else {
                        userLiquidity = sNum
                        liquidityPercent = BigDecimal("100").setScale(18, RoundingMode.HALF_UP).divide(liquidity, RoundingMode.HALF_UP).multiply(userLiquidity)
                        if(liquidityPercent >= percentError) {
                            liquidityPercent = BigDecimal("100")
                        }
                        viewState.setLiquidityPercent(liquidityPercent)
                    }
                    viewState.setCoin0(amount0.percent(liquidityPercent), coin0)
                    viewState.setCoin1(amount1.percent(liquidityPercent), coin1)
                }
                R.id.input_liquidity -> {
                    if (ignoreInput.getAndSet(false)) {
                        return@launch
                    }
                    ignoreInput.set(true)
                    liquidityPercent = sNum
                    userLiquidity = pool!!.stake!!.liquidity.percent(liquidityPercent)
                    viewState.setTokenAmount(userLiquidity)
                    viewState.setCoin0(amount0.percent(liquidityPercent), coin0)
                    viewState.setCoin1(amount1.percent(liquidityPercent), coin1)
                }
            }
        }

        viewState.setEnableSubmit(valid)
    }

    private fun onSubmit(v: View) {
        txSender = TransactionSender(secretStorage.mainWallet, initDataRepo, gateRepo)
        txSender!!.startListener = {
            viewState.startDialog { ctx ->
                TxLiquidityStartDialog.Builder(ctx, R.string.tx_send_overall_title)
                        .setTxTitle(R.string.dialog_title_remove_liquidity)
                        .setCoins(coin0, coin1)
                        .setAmounts(amount0.percent(liquidityPercent), amount1.percent(liquidityPercent))
                        .setSubAmount0(amount0.percent(liquidityPercent).removePercent(BigDecimal("5")).humanize() + " min")
                        .setSubAmount1(amount1.percent(liquidityPercent).removePercent(BigDecimal("5")).humanize() + " min")
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
                    .setLabel(R.string.dialog_subtitle_liquidity_removed)
                    .setValue(tr(R.string.dialog_subtitle_liquidity_pool_name, pool!!.stake!!.coin0, pool!!.stake!!.coin1))
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
                    .setText((errorResult.message))
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

        val txPre = txBuilder.removeLiquidity()
        txPre.coin0 = coin0.id
        txPre.coin1 = coin1.id
        txPre.minVolume0 = amount0.percent(liquidityPercent).removePercent(BigDecimal("5"))
        txPre.minVolume1 = amount1.percent(liquidityPercent).removePercent(BigDecimal("5"))

        // use max case
        if (liquidityPercent >= percentError) {
            txPre.liquidity = liquidity
        } else {
            txPre.liquidity = userLiquidity
        }

        val tx = txPre.build().signSingle(secretStorage.mainSecret.privateKey)!!

        return tx.toObservable()
    }

}