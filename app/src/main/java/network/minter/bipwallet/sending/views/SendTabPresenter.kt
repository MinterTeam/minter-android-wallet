/*
 * Copyright (C) by MinterTeam. 2022
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
package network.minter.bipwallet.sending.views

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import com.edwardstock.inputfield.form.InputWrapper
import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import moxy.InjectViewState
import network.minter.bipwallet.R
import network.minter.bipwallet.addressbook.db.AddressBookRepository
import network.minter.bipwallet.addressbook.models.AddressContact
import network.minter.bipwallet.analytics.AppEvent
import network.minter.bipwallet.apis.explorer.RepoTransactions
import network.minter.bipwallet.apis.gate.TxInitDataRepository
import network.minter.bipwallet.apis.reactive.castErrorResultTo
import network.minter.bipwallet.home.HomeScope
import network.minter.bipwallet.internal.Wallet
import network.minter.bipwallet.internal.auth.AuthSession
import network.minter.bipwallet.internal.dialogs.ConfirmDialog
import network.minter.bipwallet.internal.dialogs.WalletProgressDialog
import network.minter.bipwallet.internal.exceptions.ErrorManager
import network.minter.bipwallet.internal.exceptions.humanDetailsMessage
import network.minter.bipwallet.internal.helpers.KeyboardHelper
import network.minter.bipwallet.internal.helpers.MathHelper
import network.minter.bipwallet.internal.helpers.MathHelper.humanize
import network.minter.bipwallet.internal.helpers.MathHelper.humanizeDecimal
import network.minter.bipwallet.internal.helpers.MathHelper.normalize
import network.minter.bipwallet.internal.helpers.MathHelper.parseBigDecimal
import network.minter.bipwallet.internal.helpers.MathHelper.toPlain
import network.minter.bipwallet.internal.helpers.StringsHelper.utfStringSliceToBytes
import network.minter.bipwallet.internal.helpers.ViewExtensions.tr
import network.minter.bipwallet.internal.helpers.forms.validators.PayloadValidator
import network.minter.bipwallet.internal.mvp.MvpBasePresenter
import network.minter.bipwallet.internal.storage.RepoAccounts
import network.minter.bipwallet.internal.storage.SecretStorage
import network.minter.bipwallet.internal.storage.models.AddressListBalancesTotal
import network.minter.bipwallet.internal.system.SimpleTextWatcher
import network.minter.bipwallet.sending.account.selectorDataFromCoins
import network.minter.bipwallet.sending.contract.SendView
import network.minter.bipwallet.sending.ui.dialogs.TxSendStartDialog
import network.minter.bipwallet.sending.ui.dialogs.TxSendSuccessDialog
import network.minter.bipwallet.tx.contract.TxInitData
import network.minter.bipwallet.wallets.views.WalletSelectorController
import network.minter.blockchain.models.TransactionCommissionValue
import network.minter.blockchain.models.operational.OperationInvalidDataException
import network.minter.blockchain.models.operational.OperationType
import network.minter.blockchain.models.operational.Transaction
import network.minter.blockchain.models.operational.TransactionSign
import network.minter.core.MinterSDK.DEFAULT_COIN
import network.minter.core.MinterSDK.DEFAULT_COIN_ID
import network.minter.core.crypto.MinterAddress
import network.minter.core.crypto.MinterPublicKey
import network.minter.core.crypto.PrivateKey
import network.minter.core.internal.common.Preconditions.firstNonNull
import network.minter.explorer.models.CoinBalance
import network.minter.explorer.models.GateResult
import network.minter.explorer.models.PushResult
import network.minter.explorer.models.TxCount
import network.minter.explorer.repo.*
import timber.log.Timber
import java.math.BigDecimal
import java.math.BigDecimal.ZERO
import java.math.BigInteger
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.net.ssl.SSLException

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
@HomeScope
@InjectViewState
class SendTabPresenter @Inject constructor() : MvpBasePresenter<SendView>(), ErrorManager.ErrorGlobalReceiverListener {
    companion object {
        private const val REQUEST_CODE_QR_SCAN_ADDRESS = 101
        private const val REQUEST_CODE_ADDRESS_BOOK_SELECT = 102
        private var PAYLOAD_FEE = BigDecimal.valueOf(0.200)
    }

    @Inject
    lateinit var secretStorage: SecretStorage

    @Inject
    lateinit var session: AuthSession

    @Inject
    lateinit var cachedTxRepo: RepoTransactions

    @Inject
    lateinit var accountStorage: RepoAccounts

    @Inject
    lateinit var coinRepo: ExplorerCoinsRepository

    @Inject
    lateinit var validatorsRepo: ExplorerValidatorsRepository

    @Inject
    lateinit var gasRepo: GateGasRepository

    @Inject
    lateinit var estimateRepo: GateEstimateRepository

    @Inject
    lateinit var gateTxRepo: GateTransactionRepository

    @Inject
    lateinit var addressBookRepo: AddressBookRepository

    @Inject
    lateinit var walletSelectorController: WalletSelectorController

    @Inject
    lateinit var errorManager: ErrorManager

    @Inject
    lateinit var initDataRepo: TxInitDataRepository

    private var fromAccount: CoinBalance? = null
    private var sendAmount: BigDecimal? = null
    private var recipient: AddressContact? = null
    private var mAvatar: String? = null

    private val useMax = AtomicBoolean(false)
    private val clickedUseMax = AtomicBoolean(false)
    private var inputChangeSubject: BehaviorSubject<String>? = null
    private var addressChangeSubject: BehaviorSubject<String>? = null
    private var mGasCoinId = DEFAULT_COIN_ID
    private var gasPrice = BigInteger("1")
    private var initFeeData: TxInitData? = null
    private var mLastAccount: CoinBalance? = null
    private var sendFee: BigDecimal? = null
    private var mPayload: ByteArray? = null
    private var handleAutocomplete: Boolean = true
    private var formValid = false
    private var handlePayloadChanges = true

    private val mPayloadChangeListener: TextWatcher = object : SimpleTextWatcher() {
        override fun afterTextChanged(s: Editable) {
            if (!handlePayloadChanges) return
            handlePayloadChanges = false

            mPayload = utfStringSliceToBytes(s.toString(), PayloadValidator.MAX_PAYLOAD_LENGTH)
            if (mPayload?.size ?: 0 > PayloadValidator.MAX_PAYLOAD_LENGTH - 6) {
                viewState.setPayload(String(mPayload!!))
            }

            setupFee()
            handlePayloadChanges = true
        }
    }

    override fun onError(t: Throwable) {
        viewState.showBalanceProgress(false)
    }

    override fun attachView(view: SendView) {
        walletSelectorController.attachView(view)
        walletSelectorController.onWalletSelected = {
            viewState.showBalanceProgress(true)
            viewState.clearAmount()
        }
        super.attachView(view)
        viewState.setOnClickAccountSelectedListener { onClickAccountSelector() }

        viewState.setOnSubmit { onSubmit() }
        viewState.setOnClickMaximum { v -> onClickMaximum(v) }
        viewState.setOnClickAddPayload { viewState.showPayload() }
        viewState.setOnClickClearPayload { viewState.hidePayload() }
        viewState.setPayloadChangeListener(mPayloadChangeListener)
        viewState.setOnContactsClickListener { viewState.startAddressBook() }
        viewState.setRecipientAutocompleteItemClickListener { contact: AddressContact, pos: Int ->
            onAutocompleteSelected(contact, pos)
        }

        errorManager.subscribe(this)

        loadAndSetFee()
        accountStorage.update()

        loadAddressBook()

        checkRecipientContactExists()
    }

    override fun detachView(view: SendView) {
        super.detachView(view)
        errorManager.unsubscribe(this)
        walletSelectorController.detachView()
    }


    private fun loadAddressBook() {
        addressBookRepo
                .findAll()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(
                        { suggestions: List<AddressContact> ->
                            viewState.setRecipientAutocompleteItems(suggestions)
                        },
                        { t2: Throwable ->
                            Timber.w(t2, "Unable to load address book")
                        }
                )
    }

    /**
     * Check contact exists in database (it can be deleted by user)
     * If it was an address book contact and it not exist more, then convert contact to dummy with name=address
     * and change input
     */
    private fun checkRecipientContactExists() {
        if (recipient == null || recipient?.id == 0) {
            return
        }

        addressBookRepo.exist(recipient!!)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(
                        {
                            if (!it) {
                                val contact = recipient!!
                                contact.id = 0
                                contact.name = contact.address
                                handleAutocomplete = false
                                viewState.setRecipientAutocompleteItems(ArrayList(0))
                                recipient = contact
                                viewState.setRecipient(recipient!!)
                                handleAutocomplete = true
                            }
                        },
                        { t -> Timber.e(t, "Unable to check recipient existence") }
                )

    }

    fun handleQRResult(result: String?) {
        result?.let {
            val isMxAddress = result.matches(MinterAddress.ADDRESS_PATTERN.toRegex())
            val isMpAddress = result.matches(MinterPublicKey.PUB_KEY_PATTERN.toRegex())
            recipient = AddressContact(result)
            when {
                isMxAddress -> recipient?.let {
                    it.type = AddressContact.AddressType.Address
                    viewState.setRecipient(it)
                }
                isMpAddress -> viewState.startDelegate(MinterPublicKey(result))
                else -> {
                    try {
                        viewState.startExternalTransaction(result)
                    } catch (t: Throwable) {
                        Timber.w(t, "Unable to parse remote transaction: %s", result)
                        viewState.startDialog { ctx ->
                            ConfirmDialog.Builder(ctx, R.string.dialog_title_err_unable_scan_qr)
                                    .setText(tr(R.string.dialog_title_err_invalid_deeplink), t.message)
                                    .setPositiveAction(R.string.btn_close)
                                    .create()
                        }
                    }
                }
            }
        }
    }

    fun handleAddressBookResult(result: AddressContact?) {
        result?.let { contact ->
            handleAutocomplete = false
            viewState.setRecipientAutocompleteItems(ArrayList(0))
            recipient = contact
            if (recipient!!.id == 0) {
                recipient!!.name = recipient!!.address
            }
            viewState.setRecipient(recipient!!)
            handleAutocomplete = true
        }

    }

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()
        walletSelectorController.onFirstViewAttach(viewState)
        viewState.showBalanceProgress(true)
        accountStorage
                .observe()
                .joinToUi()
                .subscribe(
                        { res: AddressListBalancesTotal ->
                            res.takeUnless { it.isEmpty }.let {
                                val acc = accountStorage.entity.mainWallet
                                if (mLastAccount != null) {
                                    mLastAccount?.let { account ->
                                        acc.findCoin(account.coin.id).orElse(acc.coinsList[0])?.let { balance ->
                                            onAccountSelected(balance)
                                        }
                                    }
                                } else {
                                    acc.coinsList.firstOrNull()?.let { onAccountSelected(it) }
                                }
                            }

                            viewState.showBalanceProgress(false)
                        },
                        { t: Throwable ->
                            Timber.w(t, "Unable to load balance for sending")
                        }
                )
                .disposeOnDestroy()

        inputChangeSubject = BehaviorSubject.create()
        addressChangeSubject = BehaviorSubject.create()


        inputChangeSubject?.let {
            it.toFlowable(BackpressureStrategy.LATEST)
                    .debounce(200, TimeUnit.MILLISECONDS)
                    .subscribe(
                            { amount: String? ->
                                onAmountChanged(amount)
                            },
                            { t ->
                                Timber.e(t, "Unable to handle amount change")
                            }
                    )
                    .disposeOnDestroy()
        }

        addressChangeSubject?.let {
            it.toFlowable(BackpressureStrategy.LATEST)
                    .debounce(200, TimeUnit.MILLISECONDS)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { address: String -> onAddressChanged(address) }
                    .disposeOnDestroy()
        }

        checkEnableSubmit()

        viewState.setFormValidationListener {
            formValid = it
            Timber.d("Form is valid: %b", it)
            checkEnableSubmit()
        }

        viewState.setOnTextChangedListener { input, valid ->
            onInputTextChanged(input, valid)
        }

        viewState.setMaxAmountValidator {
            fromAccount
        }
    }


    private fun loadAndSetFee() {
        initDataRepo.loadFeeWithTx()
                .retryWhen(errorManager.retryWhenHandler)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(
                        { res: TxInitData ->
                            res.gas?.let {
                                gasPrice = it
                            }
                            PAYLOAD_FEE = res.priceCommissions.payloadByte.humanizeDecimal()
                            initFeeData = res
                            setupFee()
                        },
                        { e ->
                            gasPrice = BigInteger.ONE
                            Timber.w(e, "Unable to get min gas price for sending")
                        }
                )
    }

    private fun setupFee() {
        if (initFeeData != null) {
            // if fee represented not in BIP
            if (initFeeData?.gasRepresentingCoin?.id != DEFAULT_COIN_ID) {
                sendFee = initFeeData!!.priceCommissions.send.humanizeDecimal().multiply(gasPrice.toBigDecimal())
                sendFee = sendFee!! + payloadFee
                val fee: String = String.format("%s %s (%s %s)",
                        sendFee!!.multiply(initFeeData!!.gasBaseCoinRate).humanize(),
                        DEFAULT_COIN,
                        sendFee!!.humanize(),
                        initFeeData!!.gasRepresentingCoin.symbol)

                viewState.setFee(fee)
            } else {
                sendFee = initFeeData!!.priceCommissions.send.humanizeDecimal() * gasPrice.toBigDecimal()
                sendFee = sendFee!! + payloadFee
                val fee: String = String.format("%s %s", sendFee!!.humanize(), DEFAULT_COIN)
                viewState.setFee(fee)
            }
        }
    }

    private val payloadFee: BigDecimal
        get() = BigDecimal.valueOf(firstNonNull(mPayload, ByteArray(0)).size.toLong()).multiply(PAYLOAD_FEE)

    private val payloadFeeBIP: BigDecimal
        get() = payloadFee.multiply(initFeeData!!.gasBaseCoinRate)

    private val sendFeeBIP: BigDecimal
        get() = (sendFee ?: OperationType.SendCoin.fee).multiply(initFeeData!!.gasBaseCoinRate)

    private fun checkEnoughBalance(amount: BigDecimal): Boolean {
        if (fromAccount!!.coin!!.id != DEFAULT_COIN_ID) {
            return true
        }
        return amount >= fee
    }

    private fun onAmountChanged(amount: String?) {
        sendAmount = if (amount == null || amount.isEmpty()) {
            null
        } else {
            amount.parseBigDecimal()
        }

        if (!clickedUseMax.get()) {
            useMax.set(false)
        }

        clickedUseMax.set(false)
        loadAndSetFee()
        checkEnableSubmit()
    }

    private fun onAddressChanged(address: String) {
        if (address.isEmpty()) {
            viewState.setFee("")
        } else {
            setupFee()
            if (recipient != null) {
                viewState.setRecipientError(null)
            }

        }
    }

    private fun onSubmit() {
        var valid = true
        if (recipient == null) {
            viewState.setRecipientError(tr(R.string.input_validator_recipient_required))
            valid = false
        }
        if (sendAmount == null) {
            viewState.setAmountError(tr(R.string.input_validator_amount_cant_be_empty))
            valid = false
        }

        if (!valid) {
            return
        }
        analytics.send(AppEvent.SendCoinsSendButton)
        mAvatar = recipient!!.avatar
        startSendDialog()
    }

    private fun startSendDialog() {
        viewState.startDialog { ctx ->
            try {
                analytics.send(AppEvent.SendCoinPopupScreen)

                val dialog = TxSendStartDialog.Builder(ctx, R.string.tx_send_overall_title)
                        .setAmount(sendAmount)
                        .setRecipientName(recipient!!.name)
                        .setCoin(fromAccount!!.coin.symbol)
                        .setPositiveAction(R.string.btn_confirm) { d, _ ->
                            Wallet.app().sounds().play(R.raw.bip_beep_digi_octave)
                            onStartExecuteTransaction()
                            analytics.send(AppEvent.SendCoinPopupSendButton)
                            d.dismiss()
                        }
                        .setNegativeAction(R.string.btn_cancel) { d, _ ->
                            Wallet.app().sounds().play(R.raw.cancel_pop_hi)
                            analytics.send(AppEvent.SendCoinPopupCancelButton)
                            d.dismiss()
                        }
                        .create()
                dialog.setCancelable(true)

                dialog
            } catch (badState: NullPointerException) {
                ConfirmDialog.Builder(ctx, R.string.error)
                        .setText(badState.message)
                        .setPositiveAction(R.string.btn_close)
                        .create()
            }
        }
    }

    private fun checkEnableSubmit() {
        if (fromAccount == null) {
            Timber.d("Account did not loaded yet!")
            viewState.setSubmitEnabled(false)
            return
        }
        if (sendAmount == null) {
            viewState.setSubmitEnabled(false)
            return
        }

        Timber.d("Amount did set and it's NOT a NULL")
        val a = MathHelper.bdGTE(sendAmount, ZERO)
        val b = formValid
        val c = checkEnoughBalance(fromAccount!!.amount)
        val formFullyValid = a && b && c
        viewState.setSubmitEnabled(formFullyValid)
    }

    private fun onClickMaximum(view: View?) {
        if (fromAccount == null) {
            viewState.setCommonError(tr(R.string.account_err_not_loaded_yet))
            return
        }
        useMax.set(true)
        clickedUseMax.set(true)
        sendAmount = fromAccount!!.amount
        viewState.setAmount(fromAccount!!.amount.toPlain())
        analytics.send(AppEvent.SendCoinsUseMaxButton)
        if (view != null && view.context is Activity) {
            KeyboardHelper.hideKeyboard(view.context as Activity)
        }
    }

    private fun findAccountByCoin(coin: BigInteger): CoinBalance? {
        return accountStorage.entity.mainWallet.findCoin(coin)?.get()
    }

    @Throws(OperationInvalidDataException::class)
    private fun createPreTx(): TransactionSign {
        val preTx: Transaction
        val builder = Transaction.Builder(BigInteger("1"))
                .setGasCoinId(mGasCoinId)
                .setGasPrice(gasPrice)

        if (mPayload != null && mPayload!!.isNotEmpty()) {
            builder.setPayload(mPayload)
        }
        preTx = builder
                .sendCoin()
                .setCoinId(fromAccount!!.coin.id)
                .setTo(recipient!!.address)
                .setValue(sendAmount)
                .build()

        val dummyPrivate = PrivateKey("F000000000000000000000000000000000000000000000000000000000000000")
        return preTx.signSingle(dummyPrivate)!!
    }

    @Throws(OperationInvalidDataException::class)
    private fun createFinalTx(nonce: BigInteger, amountToSend: BigDecimal?): Transaction {
        val tx: Transaction
        val builder = Transaction.Builder(nonce)
                .setGasCoinId(mGasCoinId)
                .setGasPrice(gasPrice)
        if (mPayload != null && mPayload!!.isNotEmpty()) {
            builder.setPayload(mPayload)
        }
        tx = builder
                .sendCoin()
                .setCoinId(fromAccount!!.coin.id)
                .setTo(recipient!!.address)
                .setValue(amountToSend)
                .build()

        return tx
    }

    private val fee: BigDecimal
        get() = sendFeeBIP * gasPrice.toBigDecimal()

    private val feeNormalized: BigInteger
        get() = fee.normalize()

    operator fun BigDecimal.compareTo(value: Int): Int {
        return this.compareTo(BigDecimal(value.toString()))
    }

    /**
     * This is a complex sending method, read carefully, almost each line is commented, i don't know how
     * to simplify all of this
     *
     *
     * Base logic in that, if we have enough BIP to send amount to your friend and you have some additional
     * value on your account to pay fee, it's ok. But you can have enough BIP to send, but not enough to pay fee.
     * Also, as we are minting coins, user can have as much coins as Minter's blockchain have.
     * So user can send his CUSTOM_COIN to his friend and don't have enough BIP to pay fee, we must switch GAS_COIN
     * to user's custom coin, or vice-versa.
     *
     *
     * So first: we detecting what account used
     * Second: calc balance on it, and compare with input amount
     * Third: if user wants to send CUSTOM_COIN, we don't know the price of it, and we should ask node about it,
     * so, we're creating preliminary transaction and requesting "estimate transaction commission"
     * Next: combining all prepared data and finalizing our calculation. For example, if your clicked "use max",
     * we must subtract commission sum from account balance.
     * Read more in body..
     */
    private fun onStartExecuteTransaction() {

        viewState.startDialog { ctx: Context ->
            val dialog = WalletProgressDialog.Builder(ctx, R.string.tx_send_in_progress)
                    .setText(R.string.please_wait)
                    .create()
            dialog.setCancelable(false)

            // BIP account exists anyway
            val baseAccount = findAccountByCoin(DEFAULT_COIN_ID)
            // this is the edge case, when coin balance created after some error
            if (baseAccount?.address == null) {
                return@startDialog ConfirmDialog.Builder(ctx, R.string.dialog_title_err_unable_to_send_tx)
                        .setText(R.string.dialog_text_err_cant_get_wallet_balance)
                        .setPositiveAction(R.string.btn_close)
                        .create()
            }
            val sendAccount = fromAccount
            val isBaseAccount = sendAccount?.coin?.id == DEFAULT_COIN_ID

            // default coin for pay fee - MNT (base coin)
            val txFeeValue = GateResult<TransactionCommissionValue>()
            txFeeValue.result = TransactionCommissionValue()
            txFeeValue.result.value = feeNormalized
            val enoughBaseForFee: Boolean = baseAccount.amount >= fee
            var txFeeValueResolver: Observable<GateResult<TransactionCommissionValue>> = Observable.just(txFeeValue)
            val txNonceResolver = estimateRepo.getTransactionCount(fromAccount!!.address!!)

            // if enough balance on base BIP account, set it as gas coin
            if (enoughBaseForFee) {
                Timber.tag("TX Send").d("Using base coin commission %s", DEFAULT_COIN)
                mGasCoinId = baseAccount.coin.id
            }
            // if sending coin is not a BIP AND not enough BIPs to pay fee
            else if (!isBaseAccount) {
                Timber.tag("TX Send").d("Not enough balance in %s to pay fee, using %s coin", DEFAULT_COIN, sendAccount?.coin)
                mGasCoinId = sendAccount?.coin?.id
                // otherwise getting
                Timber.tag("TX Send").d("Resolving REAL fee value in custom coin %s relatively to base coin", fromAccount?.coin)
                // resolving fee currency for custom currency
                // creating tx
                try {
                    val preSign = createPreTx()
                    txFeeValueResolver = estimateRepo.getTransactionCommission(preSign)
                } catch (e: OperationInvalidDataException) {
                    Timber.w(e)
                    val commissionValue = GateResult<TransactionCommissionValue>()
                    txFeeValue.result.value = feeNormalized
                    txFeeValueResolver = Observable.just(commissionValue)
                }
            }
            // there is no else, because here you sending BIP and you don't have enough BIP, nothing to calculate, this leads to error


            // creating preparation result to send transaction
            Observable
                    .combineLatest(txFeeValueResolver, txNonceResolver, { t1: GateResult<TransactionCommissionValue>, t2: GateResult<TxCount> -> TxInitData(t1, t2) })
                    .switchMap { txInitData: TxInitData ->
                        // if in previous request we've got error, returning it
                        if (!txInitData.isOk) {
                            return@switchMap Observable.just(txInitData.errorResult!!.castErrorResultTo<PushResult>())
                        }
                        var amountToSend: BigDecimal

                        // don't calc fee if enough BIP and we are sending not a BIP
                        if (enoughBaseForFee && !isBaseAccount) {
                            // don't subtract fee from final sum
                            txInitData.commission = ZERO
                        }

                        // if balance enough to send required sum + fee, do nothing
                        // (mAmount + txInitData.commission) <= mFromAccount.amount
                        if (fromAccount!!.amount >= (sendAmount!! + txInitData.commission!!)) {
                            Timber.tag("TX Send").d("Don't change sending amount - balance enough to send")
                            amountToSend = sendAmount!!
                        } else {
                            // if user didn't clicked USE MAX, we don't subtract fee from sending amount
                            if (!useMax.get()) {
                                txInitData.commission = ZERO
                            }

                            amountToSend = sendAmount!! - txInitData.commission!!
                            Timber.tag("TX Send").d("Subtracting sending amount (-%s): balance not enough to send", txInitData.commission)
                        }

                        if (amountToSend < ZERO) {
                            amountToSend = sendAmount!!
                        }

                        return@switchMap signSendTx(dialog, txInitData.nonce!!, amountToSend)
                    }
                    .doFinally { onExecuteComplete() }
                    .joinToUi()
                    .subscribe(
                            { result: GateResult<PushResult> ->
                                onSuccessExecuteTransaction(result)
                            },
                            { throwable: Throwable ->
                                onFailedExecuteTransaction(throwable)
                            }
                    )
                    .disposeOnDestroy()

            dialog
        }
    }

    @Throws(OperationInvalidDataException::class)
    private fun signSendTx(@Suppress("UNUSED_PARAMETER") dialog: WalletProgressDialog, nonce: BigInteger, amountToSend: BigDecimal?): ObservableSource<GateResult<PushResult>> {
        // creating tx
        val tx = createFinalTx(nonce.add(BigInteger.ONE), amountToSend)

        val data = secretStorage.getSecret(fromAccount!!.address!!)
        val sign = tx.signSingle(data.privateKey)!!
        return gateTxRepo.sendTransaction(sign).joinToUi()
    }

    private fun onExecuteComplete() {
    }

    private fun onFailedExecuteTransaction(throwable: Throwable) {
        Timber.w(throwable, "Uncaught tx error")
        var errorMessage = throwable.humanDetailsMessage
        if (throwable is SSLException) {
            errorMessage = tr(R.string.dialog_text_err_bad_internet_connection, throwable.message ?: "")
        }
        viewState.startDialog { ctx ->
            ConfirmDialog.Builder(ctx, R.string.dialog_title_err_unable_to_send_tx)
                    .setText(errorMessage)
                    .setPositiveAction(R.string.btn_close)
                    .create()
        }
    }

    private fun onErrorExecuteTransaction(errorResult: GateResult<*>) {
        Timber.e(errorResult.message, "Unable to send transaction")
        viewState.startDialog { ctx ->
            ConfirmDialog.Builder(ctx, R.string.dialog_title_err_unable_to_send_tx)
                    .setText(errorResult.message)
                    .setPositiveAction(R.string.btn_close)
                    .create()
        }
    }

    private fun onSuccessExecuteTransaction(result: GateResult<PushResult>) {
        if (!result.isOk) {
            onErrorExecuteTransaction(result)
            return
        }

        // update data
        accountStorage.update(true)
        cachedTxRepo.update(true)

        // show dialog
        viewState.startDialog { ctx ->
            analytics.send(AppEvent.SentCoinPopupScreen)
            val builder = TxSendSuccessDialog.Builder(ctx)
                    .setLabel(R.string.tx_send_success_dialog_description)
                    .setValue(recipient?.name ?: recipient?.address ?: "")
                    .setPositiveAction(R.string.btn_view_tx) { d, _ ->
                        Wallet.app().sounds().play(R.raw.click_pop_zap)
                        viewState.startExplorer(result.result.hash.toString())
                        d.dismiss()
                        analytics.send(AppEvent.SentCoinPopupViewTransactionButton)
                    }
                    .setNegativeAction(R.string.btn_close) { d: DialogInterface, _: Int ->
                        d.dismiss()
                        analytics.send(AppEvent.SentCoinPopupCloseButton)
                    }

            if (recipient != null && recipient!!.id == 0 && recipient!!.address != null) {
                val recipientAddress = recipient!!.address!!
                builder.setNeutralAction(R.string.btn_save_address) { d, _ ->
                    viewState.startAddContact(recipientAddress) {
                        addressBookRepo.writeLastUsed(it)
                        loadAddressBook()
                    }
                    d.dismiss()
                }
            }
            builder.create()
        }

        if (recipient != null) {
            if (recipient!!.id == 0) {
                recipient!!.name = recipient!!.minterAddress.toShortString()
            }
            addressBookRepo.writeLastUsed(recipient!!)
        }

        // clear form
        sendAmount = null
        recipient = null
        sendFee = null
        viewState.hidePayload()
        viewState.clearInputs()
        viewState.setSubmitEnabled(false)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onInputTextChanged(editText: InputWrapper, valid: Boolean) {
        val s = editText.text.toString()
        when (editText.id) {
            R.id.input_recipient -> {
                if (!handleAutocomplete) {
                    return
                }

                addressBookRepo
                        .findByNameOrAddress(s)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeOn(Schedulers.io())
                        .subscribe(
                                { res: AddressContact? ->
                                    recipient = res
                                    addressChangeSubject!!.onNext(recipient!!.name!!)
                                },
                                { t: Throwable ->
                                    Timber.d("Unable to find recipient %s: %s", s, t.message)
                                    recipient = null
                                    viewState.setSubmitEnabled(false)

                                })
                        .disposeOnDestroy()
            }
            R.id.input_amount -> inputChangeSubject!!.onNext(editText.text.toString())
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onAutocompleteSelected(contact: AddressContact, pos: Int) {
        handleAutocomplete = false
        viewState.setRecipient(contact)
        viewState.hideAutocomplete()
        handleAutocomplete = true
    }

    private fun onClickAccountSelector() {
        analytics.send(AppEvent.SendCoinsChooseCoinButton)
        viewState.startAccountSelector(
                selectorDataFromCoins(accountStorage.entity.mainWallet.coinsList)
        ) { onAccountSelected(it.data) }
    }

    private fun onAccountSelected(coinAccount: CoinBalance) {
        fromAccount = coinAccount
        mLastAccount = coinAccount
        viewState.setAccountName(String.format("%s (%s)", coinAccount.coin?.symbol, coinAccount.amount.humanize()))
        viewState.validate {
            formValid = it
            checkEnableSubmit()
        }
    }
}
