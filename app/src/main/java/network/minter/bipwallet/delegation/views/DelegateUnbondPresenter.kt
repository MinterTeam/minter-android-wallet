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

package network.minter.bipwallet.delegation.views

import android.content.Context
import android.os.Bundle
import android.view.View
import com.edwardstock.inputfield.form.InputWrapper
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import moxy.InjectViewState
import network.minter.bipwallet.R
import network.minter.bipwallet.apis.explorer.RepoDailyRewards
import network.minter.bipwallet.apis.explorer.RepoMonthlyRewards
import network.minter.bipwallet.apis.explorer.RepoTransactions
import network.minter.bipwallet.apis.explorer.RepoValidators
import network.minter.bipwallet.apis.gate.TxInitDataRepository
import network.minter.bipwallet.apis.reactive.rxGate
import network.minter.bipwallet.apis.reactive.toObservable
import network.minter.bipwallet.databinding.StubMasternodeNameBinding
import network.minter.bipwallet.delegation.contract.DelegateUnbondView
import network.minter.bipwallet.delegation.ui.DelegateUnbondDialog
import network.minter.bipwallet.delegation.ui.DelegateUnbondDialog.Type
import network.minter.bipwallet.exchange.ui.dialogs.TxConfirmStartDialog
import network.minter.bipwallet.internal.dialogs.ConfirmDialog
import network.minter.bipwallet.internal.dialogs.WalletProgressDialog
import network.minter.bipwallet.internal.helpers.IntentHelper
import network.minter.bipwallet.internal.helpers.MathHelper.humanize
import network.minter.bipwallet.internal.helpers.MathHelper.isNotZero
import network.minter.bipwallet.internal.helpers.MathHelper.parseBigDecimal
import network.minter.bipwallet.internal.helpers.ViewExtensions.visible
import network.minter.bipwallet.internal.mvp.MvpBasePresenter
import network.minter.bipwallet.internal.storage.RepoAccounts
import network.minter.bipwallet.internal.storage.SecretStorage
import network.minter.bipwallet.internal.storage.models.AddressListBalancesTotal
import network.minter.bipwallet.sending.account.selectorDataFromCoins
import network.minter.bipwallet.sending.account.selectorDataFromDelegatedAccounts
import network.minter.bipwallet.sending.account.selectorDataFromValidators
import network.minter.bipwallet.sending.ui.dialogs.TxSendSuccessDialog
import network.minter.bipwallet.tx.TransactionSender
import network.minter.bipwallet.tx.contract.TxInitData
import network.minter.blockchain.models.TransactionSendResult
import network.minter.blockchain.models.operational.OperationType
import network.minter.blockchain.models.operational.Transaction
import network.minter.blockchain.models.operational.TransactionSign
import network.minter.core.MinterSDK
import network.minter.core.crypto.MinterPublicKey
import network.minter.core.crypto.PrivateKey
import network.minter.explorer.models.*
import network.minter.explorer.repo.GateTransactionRepository
import timber.log.Timber
import java.math.BigDecimal
import java.math.BigInteger
import javax.inject.Inject

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
@InjectViewState
class DelegateUnbondPresenter @Inject constructor() : MvpBasePresenter<DelegateUnbondView>() {
    @Inject lateinit var validatorsRepo: RepoValidators
    @Inject lateinit var accountStorage: RepoAccounts
    @Inject lateinit var gateRepo: GateTransactionRepository
    @Inject lateinit var initDataRepo: TxInitDataRepository
    @Inject lateinit var secretStorage: SecretStorage
    @Inject lateinit var txRepo: RepoTransactions
    @Inject lateinit var rewardsMonthlyRepo: RepoMonthlyRewards
    @Inject lateinit var rewardsDailyRepo: RepoDailyRewards

    private var selectAccount: String? = null
    private var validators: MutableList<ValidatorItem> = ArrayList()
    private var toValidator: MinterPublicKey? = null
    private var toValidatorName: String? = null
    private var fromAccount: BaseCoinValue? = null
    private var lastAccount: BaseCoinValue? = null
    private var type: Type = Type.Delegate
    private var useMax = false
    private var amount = BigDecimal.ZERO
    private var gas = BigInteger.ONE
    private var fee = BigDecimal.ZERO
    private var clickedUseMax: Boolean = false

    override fun handleExtras(bundle: Bundle?) {
        super.handleExtras(bundle)
        if (bundle == null) {
            throw IllegalStateException("Can't start view without arguments")
        }

        if (!bundle.containsKey(DelegateUnbondDialog.ARG_TYPE)) {
            throw IllegalStateException("View type must be set: Type.Delete or Type.Unbond")
        }

        type = bundle.getSerializable(DelegateUnbondDialog.ARG_TYPE) as Type

        if (bundle.containsKey(DelegateUnbondDialog.ARG_PUB_KEY)) {
            toValidator = IntentHelper.getParcelExtra(bundle, DelegateUnbondDialog.ARG_PUB_KEY)
        }

        if (bundle.containsKey(DelegateUnbondDialog.ARG_COIN)) {
            selectAccount = bundle.getString(DelegateUnbondDialog.ARG_COIN)
        }

        viewState.setTitle(
                if (type == Type.Delegate) {
                    R.string.dialog_title_delegate
                } else {
                    R.string.dialog_title_unbond
                }
        )
        viewState.setSubtitle(
                if (type == Type.Delegate) {
                    R.string.dialog_label_delegate
                } else {
                    R.string.dialog_label_unbond
                }
        )
        setupFee()

        viewState.setOnClickUseMax(View.OnClickListener {
            clickedUseMax = true
            useMax = true
            if (type == Type.Delegate) {
                viewState.setAmount(fromAccount!!.amount.toPlainString())
            } else {
                if (accountStorage.entity.mainWallet.hasDelegated(toValidator, selectAccount)) {
                    viewState.setAmount(
                            accountStorage
                                    .entity
                                    .mainWallet
                                    .getDelegatedByValidatorAndCoin(toValidator, selectAccount)!!
                                    .amount
                                    .toPlainString()
                    )
                }
            }
        })
    }

    private fun setupFee() {
        initDataRepo.gasRepo.minGas.rxGate()
                .joinToUi()
                .map {
                    val txFee = when (type) {
                        Type.Delegate -> OperationType.Delegate.fee
                        Type.Unbond -> OperationType.Unbound.fee
                    }

                    gas = (it.result?.gas ?: BigInteger.ONE)
                    txFee * gas.toBigDecimal()
                }
                .subscribe {
                    fee = it
                    viewState.setFee("${it.humanize()} ${MinterSDK.DEFAULT_COIN}")
                }
    }

    private fun findValidator(publicKey: MinterPublicKey): ValidatorItem {
        return validators.first { it.pubKey == publicKey }
    }

    private fun setValidator(validator: ValidatorItem) {
        viewState.setValidator(validator) { v ->
            val b = StubMasternodeNameBinding.bind(v)
            if (validator.meta.name.isNullOrEmpty()) {
                b.title.text = validator.pubKey.toShortString()
                b.subtitle.visible = false
            } else {
                b.title.text = validator.meta.name
                b.subtitle.text = validator.pubKey.toShortString()
            }

        }
    }

    private fun setValidator(validator: MinterPublicKey) {
        viewState.setValidator(validator) { v ->
            val b = StubMasternodeNameBinding.bind(v)
            b.title.text = validator.toShortString()
            b.subtitle.visible = false
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onInputChanged(input: InputWrapper, valid: Boolean) {
        if (!clickedUseMax) {
            useMax = false
        }
        clickedUseMax = false

        when (input.id) {
            R.id.input_amount -> {
                amount = (input.text ?: "").toString().parseBigDecimal()
            }
        }
        checkEnableSubmit()
    }

    private fun checkEnableSubmit() {
        viewState.setEnableSubmit(
                amount.isNotZero()
                        && toValidator != null
                        && fromAccount != null
        )
    }

    val validatorFullName: String
        get() {
            if (toValidatorName.isNullOrEmpty()) {
                return toValidator.toString()
            }

            return "$toValidatorName (${toValidator!!.toShortString()})"
        }

    private var txSender: TransactionSender? = null

    private fun onSubmit() {
        setupFee()

        txSender = TransactionSender(secretStorage.mainWallet, initDataRepo, gateRepo)
        txSender!!.startListener = {
            viewState.startDialog {
                TxConfirmStartDialog.Builder(it, type.titleRes)
                        .setFirstLabel(type.firstLabelRes)
                        .setFirstValue(amount)
                        .setFirstCoin(fromAccount!!.coin!!)
                        .setSecondLabel(type.secondLabelRes)
                        .setSecondValueText(validatorFullName)
                        .setPositiveAction(R.string.btn_confirm) { _, _ ->
                            txSender!!.next()
                        }
                        .setNegativeAction(R.string.btn_cancel) { d, _ ->
                            d.dismiss()
                            viewState.expand()
                        }
                        .create()
            }
        }
        txSender!!.progressListener = {
            viewState.startDialog { ctx ->
                val dialog = WalletProgressDialog.Builder(ctx, type.progressTitleRes)
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

    private fun prepareTx(initData: TxInitData, commission: BigDecimal, amount: BigDecimal, acc: CoinBalance): Observable<Transaction> {
        val txBuilder = Transaction.Builder(initData.nonce!!)
        txBuilder.setGasPrice(gas)
        txBuilder.setGasCoin(fromAccount!!.coin)

        var amountToSend: BigDecimal = amount
        val accBalance = acc.amount

        when {
            // if balance is too small to send any delegate or unbond tx, throwing error
            ((accBalance - commission) < 0.toBigDecimal()) -> {
                return IllegalStateException("Insufficient funds for sending transaction").toObservable()
            }
            // if we're delegating and balance enough only to pay fee - error
            type == Type.Delegate && accBalance == commission -> {
                // todo: make another error text
                return IllegalStateException("Insufficient funds for sending transaction").toObservable()
            }
            // if we're delegating ALL FUNDS, subtract fee from balance
            type == Type.Delegate && useMax -> {
                amountToSend = accBalance - commission
            }
        }

        val tx: Transaction = when (type) {
            Type.Delegate -> {
                txBuilder.delegate().apply {
                    coin = fromAccount!!.coin!!
                    publicKey = toValidator!!
                    stake = amountToSend
                }.build()
            }
            Type.Unbond -> {
                txBuilder.unbound().apply {
                    coin = fromAccount!!.coin!!
                    publicKey = toValidator!!
                    value = amountToSend
                }.build()
            }
        }

        return tx.toObservable()
    }

    private fun createPreTx(): TransactionSign {
        val preTx: Transaction
        val builder = Transaction.Builder(BigInteger("1"))
                .setGasCoin(fromAccount!!.coin!!)
                .setGasPrice(gas)

        preTx = if (type == Type.Delegate) {
            builder
                    .delegate()
                    .setCoin(fromAccount!!.coin)
                    .setPublicKey(toValidator!!)
                    .setStake(amount)
                    .build()
        } else {
            builder
                    .unbound()
                    .setCoin(fromAccount!!.coin)
                    .setPublicKey(toValidator!!)
                    .setValue(amount)
                    .build()
        }
        val dummyPrivate = PrivateKey("F000000000000000000000000000000000000000000000000000000000000000")
        return preTx.signSingle(dummyPrivate)
    }

    private fun signTx(initData: TxInitData): Observable<TransactionSign> {

        val accOptional = accountStorage.entity.mainWallet.findCoinByName(fromAccount!!.coin!!)
        if (!accOptional.isPresent) {
            return Observable.error(IllegalStateException("Balance for coin ${fromAccount!!.coin!!} not found!"))
        }
        val acc = accOptional.get()
        val isBipAccount = acc.coin!! == MinterSDK.DEFAULT_COIN

        if (isBipAccount) {
            return prepareTx(initData, fee, amount, acc)
                    .map { it.signSingle(secretStorage.mainSecret.privateKey) }
        }

        return initDataRepo.estimateRepo.getTransactionCommission(createPreTx()).rxGate()
                .subscribeOn(Schedulers.io())
                .switchMap {
                    if (!it.isOk) {
                        return@switchMap Observable.error<TransactionSign>(IllegalStateException(it.error.message))
                    }

                    prepareTx(initData, it.result.getValue(), amount, acc)
                            .map { tx -> tx.signSingle(secretStorage.mainSecret.privateKey) }
                }
    }

    private fun onSuccessExecuteTransaction(result: GateResult<TransactionSendResult>) {
        accountStorage.update(true)
        txRepo.update(true)
        rewardsDailyRepo.update(true)
        rewardsMonthlyRepo.update(true)

        viewState.startDialog {
            TxSendSuccessDialog.Builder(it)
                    .setLabel(type.resultLabelRes)
                    .setValue(validatorFullName)
                    .setPositiveAction(R.string.btn_view_tx) { d, _ ->
                        d.dismiss()
                        viewState.startExplorer(result.result.txHash)
                        viewState.dismiss()
                    }
                    .setNegativeAction(R.string.btn_close) { d, _ ->
                        d.dismiss()
                        viewState.dismiss()
                    }
                    .create()
        }
    }

    private fun onErrorExecuteTransaction(errorResult: GateResult<*>) {
        Timber.e(errorResult.message, "Unable to send transaction")
        viewState.startDialog { ctx: Context ->
            ConfirmDialog.Builder(ctx, "Unable to send transaction")
                    .setText((errorResult.message))
                    .setPositiveAction(R.string.btn_close) { d, _ ->
                        d.dismiss()
                        viewState.expand()
                    }
                    .create()
        }
    }

    private fun onErrorRequest(errorResult: ExpResult<*>) {
        Timber.e("Unable to send transaction: %s", errorResult.error?.message)
        viewState.startDialog { ctx: Context ->
            ConfirmDialog.Builder(ctx, "Unable to send transaction")
                    .setText((errorResult.error?.message ?: "Caused unknown error"))
                    .setPositiveAction(R.string.btn_close) { d, _ ->
                        d.dismiss()
                        viewState.expand()
                    }
                    .create()
        }
    }

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()
        viewState.setTextChangedListener(::onInputChanged)
        viewState.setOnSubmitListener(View.OnClickListener {
            onSubmit()
        })
        validatorsRepo
                .retryWhen(errorResolver)
                .observe()
                .subscribeOn(Schedulers.io())
                .subscribe(
                        {
                            validators = it.toMutableList()
                            validators.sortByDescending { v -> v.stake }

                            if (toValidator != null) {
                                try {
                                    setValidator(findValidator(toValidator!!))
                                } catch (e: NoSuchElementException) {
                                    setValidator(toValidator!!)
                                }
                            }
                        },
                        { t: Throwable ->
                            Timber.w(t, "Unable to update validators")
                            viewState.onError(t)
                        }
                )

        accountStorage
                .retryWhen(errorResolver)
                .observe()
                .subscribeOn(Schedulers.io())
                .subscribe(
                        { res: AddressListBalancesTotal ->
                            onBalanceLoaded(res)
                        },
                        { t: Throwable ->
                            Timber.w(t, "Unable to update accounts")
                            viewState.onError(t)
                        }
                )
    }

    private fun onBalanceLoaded(res: AddressListBalancesTotal) {
        if (!res.isEmpty) {
            val acc = accountStorage.entity.mainWallet
            if (type == Type.Delegate) {
                if (fromAccount != null) {
                    onAccountSelected(acc.findCoinByName(fromAccount?.coin).orElse(acc.coinsList[0]))
                } else {
                    onAccountSelected(acc.coinsList[0])
                }
            } else {
                if (fromAccount == null && selectAccount != null) {
                    fromAccount = acc.getCoin(selectAccount)
                }
                if (fromAccount != null && toValidator != null) {
                    onAccountSelected(acc.getDelegatedByValidatorAndCoin(toValidator, fromAccount?.coin)!!)
                }
            }
        }
    }


    override fun attachView(view: DelegateUnbondView) {
        super.attachView(view)
        accountStorage.update()
        validatorsRepo.update()

        viewState.setOnValidatorSelectListener(View.OnClickListener {
            viewState.startValidatorSelector(selectorDataFromValidators(validators)) { validator ->
                onValidatorSelected(validator.data)
            }
        })

        viewState.setOnAccountSelectListener(View.OnClickListener {
            if (type == Type.Delegate) {
                viewState.startAccountSelector(selectorDataFromCoins(accountStorage.entity.mainWallet.coinsList)) { account ->
                    onAccountSelected(account.data)
                }
            } else {
                if (toValidator != null) {
                    val validatorDelegations = accountStorage.entity.mainWallet.getDelegatedListByValidator(toValidator!!)
                    if (validatorDelegations.isNotEmpty()) {
                        viewState.startAccountSelector(selectorDataFromDelegatedAccounts(validatorDelegations)) { account ->
                            onAccountSelected(account.data)
                        }
                    }
                }
            }
        })
    }

    private fun onAccountSelected(account: BaseCoinValue?) {
        if (account == null) return
        fromAccount = account
        viewState.setAccountName("${account.coin} (${account.amount.humanize()})")
        lastAccount = account
        checkEnableSubmit()
    }

    private fun onValidatorSelected(validator: ValidatorItem) {
        setValidator(validator)
        toValidatorName = validator.meta?.name
        toValidator = validator.pubKey

        if (type == Type.Unbond) {
            if (!accountStorage.entity.mainWallet.hasDelegated(toValidator)) {
                viewState.setAccountName(null)
                viewState.setAccountSelectorError("You didn't delegated to selected validator")
                toValidator = null
            } else {
                val delegated = accountStorage.entity.mainWallet.getDelegatedListByValidator(toValidator!!)
                val first = delegated[0]
                viewState.setAccountSelectorError(null)
                viewState.setAccountName("${first.coin} (${first.amount.humanize()})")
            }
        }
        checkEnableSubmit()
    }
}