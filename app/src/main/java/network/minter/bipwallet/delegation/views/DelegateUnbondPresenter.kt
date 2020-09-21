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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import com.edwardstock.inputfield.form.InputWrapper
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import moxy.InjectViewState
import network.minter.bipwallet.R
import network.minter.bipwallet.apis.explorer.RepoDailyRewards
import network.minter.bipwallet.apis.explorer.RepoMonthlyRewards
import network.minter.bipwallet.apis.explorer.RepoTransactions
import network.minter.bipwallet.apis.explorer.RepoValidators
import network.minter.bipwallet.apis.gate.TxInitDataRepository

import network.minter.bipwallet.apis.reactive.toObservable
import network.minter.bipwallet.databinding.StubValidatorNameBinding
import network.minter.bipwallet.delegation.contract.DelegateUnbondView
import network.minter.bipwallet.delegation.ui.DelegateUnbondActivity
import network.minter.bipwallet.delegation.ui.DelegateUnbondActivity.Type
import network.minter.bipwallet.delegation.ui.ValidatorSelectorActivity
import network.minter.bipwallet.exchange.ui.dialogs.TxConfirmStartDialog
import network.minter.bipwallet.internal.dialogs.ConfirmDialog
import network.minter.bipwallet.internal.dialogs.WalletProgressDialog
import network.minter.bipwallet.internal.helpers.IntentHelper
import network.minter.bipwallet.internal.helpers.MathHelper.bdNull
import network.minter.bipwallet.internal.helpers.MathHelper.humanize
import network.minter.bipwallet.internal.helpers.MathHelper.isNotZero
import network.minter.bipwallet.internal.helpers.MathHelper.parseBigDecimal
import network.minter.bipwallet.internal.helpers.MathHelper.toPlain
import network.minter.bipwallet.internal.helpers.Plurals
import network.minter.bipwallet.internal.helpers.ViewExtensions.visible
import network.minter.bipwallet.internal.mvp.MvpBasePresenter
import network.minter.bipwallet.internal.storage.RepoAccounts
import network.minter.bipwallet.internal.storage.SecretStorage
import network.minter.bipwallet.internal.storage.models.AddressListBalancesTotal
import network.minter.bipwallet.sending.account.selectorDataFromCoins
import network.minter.bipwallet.sending.account.selectorDataFromDelegatedAccounts
import network.minter.bipwallet.sending.ui.dialogs.TxSendSuccessDialog
import network.minter.bipwallet.tx.TransactionSender
import network.minter.bipwallet.tx.contract.TxInitData
import network.minter.blockchain.models.operational.OperationType
import network.minter.blockchain.models.operational.Transaction
import network.minter.blockchain.models.operational.TransactionSign
import network.minter.core.MinterSDK
import network.minter.core.crypto.MinterPublicKey
import network.minter.core.crypto.PrivateKey
import network.minter.explorer.models.*
import network.minter.explorer.repo.GateTransactionRepository
import org.parceler.Parcels
import timber.log.Timber
import java.math.BigDecimal
import java.math.BigInteger
import javax.inject.Inject

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */

private const val REQUEST_CODE_SELECT_VALIDATOR = 4000

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

    private var selectAccount: CoinItemBase? = null
    private var validators: MutableList<ValidatorItem> = ArrayList()
    private var toValidatorItem: ValidatorItem? = null
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
    private val pubkeyPattern = MinterPublicKey.PUB_KEY_PATTERN.toRegex()
    private var txSender: TransactionSender? = null

    private val BaseCoinValue.title: String
        get() = "$coin (${amount.humanize()})"

    override fun handleExtras(intent: Intent?) {
        super.handleExtras(intent)
        if (intent == null) {
            throw IllegalStateException("Can't start view without arguments")
        }

        if (!intent.hasExtra(DelegateUnbondActivity.ARG_TYPE)) {
            throw IllegalStateException("View type must be set: Type.Delete or Type.Unbond")
        }

        type = intent.getSerializableExtra(DelegateUnbondActivity.ARG_TYPE) as Type

        if (intent.hasExtra(DelegateUnbondActivity.ARG_PUB_KEY)) {
            toValidator = IntentHelper.getParcelExtra(intent, DelegateUnbondActivity.ARG_PUB_KEY)
        }

        if (intent.hasExtra(DelegateUnbondActivity.ARG_COIN)) {
            selectAccount = Parcels.unwrap(intent.getParcelableExtra(DelegateUnbondActivity.ARG_COIN))
        }

        viewState.setTitle(
                if (type == Type.Delegate) {
                    R.string.title_delegate
                } else {
                    R.string.title_unbond
                }
        )
        viewState.setSubtitle(
                if (type == Type.Delegate) {
                    R.string.dialog_label_delegate
                } else {
                    R.string.dialog_label_unbond
                }
        )
        if (type == Type.Unbond) {
            viewState.setCoinLabel(R.string.label_coin_you_want_unbond)
            viewState.setValidatorSelectDisabled()
        } else {
            viewState.setOnValidatorSelectListener(::startValidatorSelector)

            if (toValidator != null) {
                val existed = validators.find { it.pubKey == toValidator }

                if (existed != null) {
                    onValidatorSelected(existed)
                } else {
                    viewState.setValidatorRaw(toValidator!!)
                    viewState.setValidatorClearSuffix(::clearSelectedValidator)
                }
            }
        }

        setupFee()

        viewState.setOnClickUseMax(View.OnClickListener {
            clickedUseMax = true
            useMax = true
            if (type == Type.Delegate) {
                viewState.setAmount(fromAccount!!.amount.toPlain())
            } else {
                if (accountStorage.entity.mainWallet.hasDelegated(toValidator, fromAccount!!.coin.id)) {
                    viewState.setAmount((
                            accountStorage.entity.mainWallet
                                    .getDelegatedByValidatorAndCoin(toValidator, fromAccount!!.coin.id)?.amount
                                    ?: BigDecimal.ZERO
                            ).toPlain()
                    )
                }
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != Activity.RESULT_OK) {
            return
        }

        if (requestCode == REQUEST_CODE_SELECT_VALIDATOR && data != null) {
            val validator = ValidatorSelectorActivity.getResult(data)
            if (validator != null) {
                onValidatorSelected(validator)
            }
        }
    }

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()
        viewState.setTextChangedListener(::onInputChanged)
        viewState.setOnSubmitListener(View.OnClickListener {
            onSubmit()
        })
        viewState.addValidatorTextChangeListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if ((s == null || s.isEmpty())) {
                    viewState.setValidatorSelectSuffix(::startValidatorSelector)
                    if (toValidator == null) {
                        viewState.setValidatorError("Public key required")
                    } else {
                        viewState.setValidatorError(null)
                    }
                    checkEnableSubmit()
                    return
                }

                viewState.setValidatorClearSuffix(::clearSelectedValidator)

                if (!s.matches(pubkeyPattern)) {
                    viewState.setValidatorError("Invalid public key format")
                    checkEnableSubmit()
                    return
                }

                toValidator = MinterPublicKey(s.toString())
                toValidatorName = null
                toValidatorItem = ValidatorItem().apply {
                    pubKey = toValidator
                }
                viewState.setValidatorError(null)
                checkEnableSubmit()
            }
        })
        validatorsRepo
                .retryWhen(errorResolver)
                .observe()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        {
                            validators = it.toMutableList()
                            validators.sortByDescending { v -> v.stake }

                            if (toValidator != null) {
                                try {
                                    onValidatorSelected(findValidator(toValidator!!))
                                } catch (e: NoSuchElementException) {
                                    viewState.setValidatorRaw(toValidator!!)
                                    viewState.setValidatorClearSuffix(::clearSelectedValidator)
                                }
                            }

                            if (type == Type.Delegate) {
                                viewState.setValidatorsAutocomplete(validators.filter { v -> v.status == ValidatorItem.STATUS_ONLINE }) { validator ->
                                    onValidatorSelected(validator)
                                }
                            }
                        },
                        { t: Throwable ->
                            Timber.w(t, "Unable to update validators")
                            viewState.onError(t)
                        }
                )
                .disposeOnDestroy()

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
                .disposeOnDestroy()
    }

    private fun onBalanceLoaded(res: AddressListBalancesTotal) {
        if (!res.isEmpty) {
            val acc = accountStorage.entity.mainWallet
            if (type == Type.Delegate) {
                if (fromAccount != null) {
                    onAccountSelected(acc.findCoinById(fromAccount?.coin?.id).orElse(acc.coinsList[0]))
                } else {
                    onAccountSelected(acc.coinsList[0])
                }
            } else {
                if (fromAccount == null && selectAccount != null) {
                    fromAccount = acc.getCoin(selectAccount!!.id)
                    selectAccount = null
                }
                if (fromAccount != null && toValidator != null) {
                    onAccountSelected(acc.getDelegatedByValidatorAndCoin(toValidator, fromAccount?.coin?.id))
                }
            }
        }
    }

    override fun attachView(view: DelegateUnbondView) {
        super.attachView(view)
        accountStorage.update()
        validatorsRepo.update()

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

    @Suppress("UNUSED_PARAMETER")
    private fun startValidatorSelector(v: View) {
        viewState.startValidatorSelector(REQUEST_CODE_SELECT_VALIDATOR, ValidatorSelectorActivity.Filter.Online)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun clearSelectedValidator(v: View) {
        toValidatorName = null
        toValidator = null
        toValidatorItem = null
        viewState.hideValidatorOverlay()
        viewState.clearValidatorInput()
        viewState.setValidatorSelectSuffix(::startValidatorSelector)
    }

    private fun setupFee() {
        initDataRepo.gasRepo.minGas
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
                .disposeOnDestroy()
    }

    private fun findValidator(publicKey: MinterPublicKey): ValidatorItem {
        return validators.first { it.pubKey == publicKey }
    }

    private fun viewSetValidator(validator: ValidatorItem) {
        viewState.setValidatorError(null)
        viewState.setValidator(validator) { v ->
            val b = StubValidatorNameBinding.bind(v)
            if (validator.meta?.name.isNullOrEmpty()) {
                b.title.text = validator.pubKey?.toShortString()
                b.subtitle.visible = false
            } else {
                b.title.text = validator.meta?.name
                b.subtitle.text = validator.pubKey?.toShortString()
            }
            b.dropdown.setOnClickListener(::clearSelectedValidator)

            if (type == Type.Unbond) {
                b.root.background.state = intArrayOf(-android.R.attr.state_enabled)
                b.dropdown.visible = false
            }

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

    private fun onSubmit() {
        setupFee()

        txSender = TransactionSender(secretStorage.mainWallet, initDataRepo, gateRepo)
        txSender!!.startListener = {
            viewState.startDialog {
                TxConfirmStartDialog.Builder(it, type.titleRes)
                        .setFirstLabel(type.firstLabelRes)
                        .setFirstValue(amount.humanize())
                        .setFirstCoin(fromAccount!!.coin!!.symbol)
                        .setSecondLabel(type.secondLabelRes)
                        .setSecondValueText(validatorFullName)
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

    private val realFee: BigDecimal
        get() {
            return type.opType.fee * gas.toBigDecimal()
        }

    private fun createPreTx(): TransactionSign {
        val preTx: Transaction
        val builder = Transaction.Builder(BigInteger("1"))
                .setGasCoinId(fromAccount!!.coin.id!!)
                .setGasPrice(gas)

        preTx = if (type == Type.Delegate) {
            builder
                    .delegate()
                    .setCoinId(fromAccount!!.coin.id)
                    .setPublicKey(toValidator!!)
                    .setStake(amount)
                    .build()
        } else {
            builder
                    .unbound()
                    .setCoinId(fromAccount!!.coin.id)
                    .setPublicKey(toValidator!!)
                    .setValue(amount)
                    .build()
        }
        val dummyPrivate = PrivateKey("F000000000000000000000000000000000000000000000000000000000000000")
        return preTx.signSingle(dummyPrivate)!!
    }

    private fun signTx(initData: TxInitData): Observable<TransactionSign> {
        /**
         * delegate
         *  - enough BIP - use BIP as gas coin
         *  - not enough BIP - use CUSTOM as gas coin and caluclate real fee in custom coin
         *  unbond
         *  - anyway, use BIP as gas coin, if not enough - error
         */

        val bipAccountOpt = accountStorage.entity.mainWallet.findCoinByName(MinterSDK.DEFAULT_COIN)

        if (type == Type.Delegate) {
            // check enough a BIP balance to pay fee, even if delegated coin is not the BIP
            if (bipAccountOpt.isPresent && bipAccountOpt.get().amount >= realFee) {
                var amountToSend = amount
                if (useMax && fromAccount!!.coin.id == MinterSDK.DEFAULT_COIN_ID) {
                    amountToSend = amount - realFee
                    if (bdNull(amountToSend)) {
                        return Observable.error<TransactionSign>(IllegalStateException("Can't delegate 0 coins"))
                    }
                }
                val txBuilder = Transaction.Builder(initData.nonce!!)
                txBuilder.setGasPrice(gas)
                txBuilder.setGasCoinId(MinterSDK.DEFAULT_COIN_ID)
                val tx = txBuilder.delegate().apply {
                    coinId = fromAccount!!.coin.id!!
                    publicKey = toValidator!!
                    stake = amountToSend
                }.build()

                return tx.signSingle(secretStorage.mainSecret.privateKey)!!.toObservable()
            }

            // BIP balance not enough to pay fee, trying to calculate custom coin
            val fromAccountOpt = accountStorage.entity.mainWallet.findCoinById(fromAccount!!.coin.id)

            // if balance for custom coin not found - error
            if (!fromAccountOpt.isPresent) {
                return Observable.error(
                        IllegalStateException("Not enough balance to pay fee")
                )
            }
            val fromAcc = fromAccountOpt.get()

            // calculate custom coin fee
            return initDataRepo.estimateRepo.getTransactionCommission(createPreTx())
                    .subscribeOn(Schedulers.io())
                    .switchMap {
                        if (!it.isOk) {
                            return@switchMap Observable.error<TransactionSign>(IllegalStateException(it.error!!.message))
                        }

                        val customFee = it.result.getValue()

                        // to send - amount typed by user
                        var amountToSend = amount

                        if (useMax) {
                            // if clicked "use max", fill with full balance - fee
                            amountToSend = fromAcc.amount - customFee

                            if (amountToSend == BigDecimal.ZERO) {
                                return@switchMap Observable.error<TransactionSign>(IllegalStateException("Can't unbond 0 coins"))
                            }
                        }

                        // we're don't have enough BIP, so check custom coin enough balance to pay at least fee
                        if ((fromAcc.amount - amountToSend - customFee) < 0.toBigDecimal()) {
                            return@switchMap Observable.error<TransactionSign>(IllegalStateException("Insufficient funds for sending transaction"))
                        }

                        val txBuilder = Transaction.Builder(initData.nonce!!)
                        txBuilder.setGasPrice(gas)
                        txBuilder.setGasCoinId(fromAcc.coin.id)
                        val tx = txBuilder.delegate().apply {
                            coinId = fromAccount!!.coin.id!!
                            publicKey = toValidator!!
                            stake = amountToSend
                        }.build()

                        tx.signSingle(secretStorage.mainSecret.privateKey).toObservable()
                    }

        } else {
            // check enough a BIP balance to pay fee, even if delegated coin is not the BIP
            if (!bipAccountOpt.isPresent || bipAccountOpt.get().amount < realFee) {
                val balance = bipAccountOpt.get()?.amount ?: 0.toBigDecimal()
                return Observable.error<TransactionSign>(IllegalStateException("Insufficient funds for unbond: required ${realFee.humanize()} ${MinterSDK.DEFAULT_COIN}, balance: ${balance.humanize()} ${MinterSDK.DEFAULT_COIN}"))
            }

            val txBuilder = Transaction.Builder(initData.nonce!!)
            txBuilder.setGasPrice(gas)
            txBuilder.setGasCoinId(MinterSDK.DEFAULT_COIN_ID)
            val tx = txBuilder.unbound().apply {
                coinId = fromAccount!!.coin.id!!
                publicKey = toValidator!!
                value = if (useMax) fromAccount!!.amount else amount
            }.build()

            return tx.signSingle(secretStorage.mainSecret.privateKey)!!.toObservable()

        }
    }

    private fun onSuccessExecuteTransaction(result: GateResult<PushResult>) {
        accountStorage.update(true)
        txRepo.update(true)
        rewardsDailyRepo.update(true)
        rewardsMonthlyRepo.update(true)
        validatorsRepo.entity.writeLastUsed(toValidatorItem)

        val height = result.result?.transaction?.height?.toLong() ?: 120L

        val leftSeconds: Float = (120f - (height % 120)) * 5f
        val msg = "Please allow ~${Plurals.timeValue(leftSeconds.toLong())} ${Plurals.timeUnitShort(leftSeconds.toLong())} for your delegated balance to update."

        viewState.startDialog {
            TxSendSuccessDialog.Builder(it)
                    .setLabel(type.resultLabelRes)
                    .setValue(msg)
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
            ConfirmDialog.Builder(ctx, "Unable to send transaction")
                    .setText((errorResult.message))
                    .setPositiveAction(R.string.btn_close) { d, _ ->
                        d.dismiss()
                    }
                    .create()
        }
    }

    private fun onAccountSelected(account: BaseCoinValue?) {
        if (account == null) return
        fromAccount = account
        viewState.setAccountTitle(account.title)
        lastAccount = account
        checkEnableSubmit()
    }

    private fun onValidatorSelected(validator: ValidatorItem) {
        toValidatorItem = validator
        toValidatorName = validator.meta?.name
        toValidator = validator.pubKey
        viewSetValidator(validator)

        if (type == Type.Unbond) {
            if (!accountStorage.entity.mainWallet.hasDelegated(toValidator)) {
                viewState.setAccountTitle(null)
                viewState.setAccountError("You didn't delegated to selected validator")
                toValidator = null
            } else {
                val coin = fromAccount?.coin ?: selectAccount
                val account = accountStorage.entity.mainWallet.getDelegatedByValidatorAndCoin(toValidator, coin!!.id)
                if (account != null) {
                    viewState.setAccountError(null)
                    viewState.setAccountTitle(account.title)
                }
            }
        }
        checkEnableSubmit()
    }
}