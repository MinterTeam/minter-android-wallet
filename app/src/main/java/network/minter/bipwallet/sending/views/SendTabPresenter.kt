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
package network.minter.bipwallet.sending.views

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import com.annimon.stream.Optional
import com.edwardstock.inputfield.form.InputWrapper
import com.google.common.base.MoreObjects.firstNonNull
import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import moxy.InjectViewState
import network.minter.bipwallet.R
import network.minter.bipwallet.addressbook.db.AddressBookRepository
import network.minter.bipwallet.addressbook.models.AddressContact
import network.minter.bipwallet.addressbook.ui.AddressBookActivity
import network.minter.bipwallet.analytics.AppEvent
import network.minter.bipwallet.apis.explorer.RepoTransactions
import network.minter.bipwallet.apis.reactive.rxGate
import network.minter.bipwallet.internal.Wallet
import network.minter.bipwallet.internal.auth.AuthSession
import network.minter.bipwallet.internal.dialogs.ConfirmDialog
import network.minter.bipwallet.internal.dialogs.WalletProgressDialog
import network.minter.bipwallet.internal.helpers.KeyboardHelper
import network.minter.bipwallet.internal.helpers.MathHelper
import network.minter.bipwallet.internal.helpers.MathHelper.humanize
import network.minter.bipwallet.internal.helpers.MathHelper.normalize
import network.minter.bipwallet.internal.helpers.MathHelper.parseBigDecimal
import network.minter.bipwallet.internal.helpers.MathHelper.toPlain
import network.minter.bipwallet.internal.helpers.StringsHelper.utfStringSliceToBytes
import network.minter.bipwallet.internal.helpers.forms.validators.PayloadValidator
import network.minter.bipwallet.internal.mvp.MvpBasePresenter
import network.minter.bipwallet.internal.storage.RepoAccounts
import network.minter.bipwallet.internal.storage.SecretStorage
import network.minter.bipwallet.internal.storage.models.AddressListBalancesTotal
import network.minter.bipwallet.internal.system.SimpleTextWatcher
import network.minter.bipwallet.sending.account.selectorDataFromCoins
import network.minter.bipwallet.sending.contract.SendView
import network.minter.bipwallet.sending.ui.QRCodeScannerActivity
import network.minter.bipwallet.sending.ui.SendTabFragment
import network.minter.bipwallet.sending.ui.dialogs.TxSendStartDialog
import network.minter.bipwallet.sending.ui.dialogs.TxSendSuccessDialog
import network.minter.bipwallet.tx.contract.TxInitData
import network.minter.bipwallet.wallets.views.WalletSelectorController
import network.minter.blockchain.models.TransactionCommissionValue
import network.minter.blockchain.models.operational.*
import network.minter.core.MinterSDK.DEFAULT_COIN
import network.minter.core.crypto.MinterAddress
import network.minter.core.crypto.MinterPublicKey
import network.minter.core.crypto.PrivateKey
import network.minter.explorer.models.*
import network.minter.explorer.repo.*
import network.minter.ledger.connector.rxjava2.RxMinterLedger
import timber.log.Timber
import java.math.BigDecimal
import java.math.BigDecimal.ZERO
import java.math.BigInteger
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */

@InjectViewState
class SendTabPresenter @Inject constructor() : MvpBasePresenter<SendView>() {
    companion object {
        private const val REQUEST_CODE_QR_SCAN_ADDRESS = 101
        private const val REQUEST_CODE_ADDRESS_BOOK_SELECT = 102
        private val PAYLOAD_FEE = BigDecimal.valueOf(0.002)
    }

    @Inject lateinit var secretStorage: SecretStorage
    @Inject lateinit var session: AuthSession
    @Inject lateinit var cachedTxRepo: RepoTransactions
    @Inject lateinit var accountStorage: RepoAccounts
    @Inject lateinit var coinRepo: ExplorerCoinsRepository
    @Inject lateinit var validatorsRepo: ExplorerValidatorsRepository
    @Inject lateinit var gasRepo: GateGasRepository
    @Inject lateinit var estimateRepo: GateEstimateRepository
    @Inject lateinit var gateTxRepo: GateTransactionRepository
    @Inject lateinit var addressBookRepo: AddressBookRepository
    @Inject lateinit var txRepo: RepoTransactions
    @Inject lateinit var walletSelectorController: WalletSelectorController

    private var mFromAccount: CoinBalance? = null
    private var mAmount: BigDecimal? = null
    private var mRecipient: AddressContact? = null
    private var mAvatar: String? = null

    private val mUseMax = AtomicBoolean(false)
    private val mClickedUseMax = AtomicBoolean(false)
    private var mInputChange: BehaviorSubject<String>? = null
    private var mAddressChange: BehaviorSubject<String>? = null
    private var mGasCoin = DEFAULT_COIN
    private var mGasPrice = BigInteger("1")
    private var mLastAccount: CoinBalance? = null
    private var mSendFee: BigDecimal? = null
    private var mPayload: ByteArray? = null
    private var handleAutocomplete: Boolean = true
    private var mFormValid = false
    private var handlePayloadChanges = true

    private val mPayloadChangeListener: TextWatcher = object : SimpleTextWatcher() {
        override fun afterTextChanged(s: Editable) {
            if (!handlePayloadChanges) return
            handlePayloadChanges = false

            mPayload = utfStringSliceToBytes(s.toString(), PayloadValidator.MAX_PAYLOAD_LENGTH)
            if (mPayload?.size ?: 0 > 1018) {
                viewState.setPayload(String(mPayload!!))
            }

            setupFee()
            handlePayloadChanges = true
        }
    }

    override fun attachView(view: SendView) {
        walletSelectorController.attachView(view)
        walletSelectorController.onWalletSelected = {
            viewState.showBalanceProgress(true)
            viewState.clearAmount()
        }
        super.attachView(view)
        viewState.setOnClickAccountSelectedListener(View.OnClickListener { onClickAccountSelector() })

        viewState.setOnSubmit(View.OnClickListener { onSubmit() })
        viewState.setOnClickMaximum(View.OnClickListener { v -> onClickMaximum(v) })
        viewState.setOnClickAddPayload(View.OnClickListener { v -> onClickAddPayload(v) })
        viewState.setOnClickClearPayload(View.OnClickListener { onClickClearPayload() })
        viewState.setPayloadChangeListener(mPayloadChangeListener)
        viewState.setOnContactsClickListener(View.OnClickListener { onClickContacts() })
        viewState.setRecipientAutocompleteItemClickListener { contact: AddressContact, pos: Int ->
            onAutocompleteSelected(contact, pos)
        }
        loadAndSetFee()
        accountStorage.update()

        loadAddressBook()

        checkRecipientContactExists()
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
                            Timber.w(t2)
                        }
                )
    }

    /**
     * Check contact exists in database (it can be deleted by user)
     * If it was an address book contact and it not exist more, then convert contact to dummy with name=address
     * and change input
     */
    private fun checkRecipientContactExists() {
        if (mRecipient == null || mRecipient?.id == 0) {
            return
        }

        addressBookRepo.exist(mRecipient!!)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(
                        {
                            if (!it) {
                                val contact = mRecipient!!
                                contact.id = 0
                                contact.name = contact.address
                                handleAutocomplete = false
                                viewState.setRecipientAutocompleteItems(ArrayList(0))
                                mRecipient = contact
                                viewState.setRecipient(mRecipient!!)
                                handleAutocomplete = true
                            }
                        },
                        { t -> Timber.e(t) }
                )

    }

    override fun detachView(view: SendView) {
        super.detachView(view)
        walletSelectorController.detachView()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) {
            return
        }
        //todo: refactor below
        if (requestCode == REQUEST_CODE_QR_SCAN_ADDRESS) {
            if (data != null && data.hasExtra(QRCodeScannerActivity.RESULT_TEXT)) {
                //Getting the passed result
                val result = data.getStringExtra(QRCodeScannerActivity.RESULT_TEXT)
                if (result != null) {
                    val isMxAddress = result.matches(MinterAddress.ADDRESS_PATTERN.toRegex())
                    val isMpAddress = result.matches(MinterPublicKey.PUB_KEY_PATTERN.toRegex())
                    mRecipient = AddressContact(result)
                    if (isMxAddress) {
                        mRecipient!!.type = AddressContact.AddressType.Address
                        viewState.setRecipient(mRecipient!!)
                    } else if (isMpAddress) {
                        viewState.startDelegate(
                                MinterPublicKey(result)
                        )
                    }
                }
            }
        } else if (requestCode == SendTabFragment.REQUEST_CODE_QR_SCAN_TX) {
            val result = data?.getStringExtra(QRCodeScannerActivity.RESULT_TEXT)
            if (result != null) {
                val isMxAddress = result.matches(MinterAddress.ADDRESS_PATTERN.toRegex())
                val isMpAddress = result.matches(MinterPublicKey.PUB_KEY_PATTERN.toRegex())
                if (isMxAddress || isMpAddress) {
                    onActivityResult(REQUEST_CODE_QR_SCAN_ADDRESS, resultCode, data)
                    return
                }
                try {
                    viewState.startExternalTransaction(result)
                } catch (t: Throwable) {
                    Timber.w(t, "Unable to parse remote transaction: %s", result)
                    viewState.startDialog { ctx ->
                        ConfirmDialog.Builder(ctx, "Unable to scan QR")
                                .setText("Invalid transaction data: %s", t.message)
                                .setPositiveAction(R.string.btn_close)
                                .create()
                    }
                }
            }
        } else if (requestCode == REQUEST_CODE_ADDRESS_BOOK_SELECT) {
            val contact = AddressBookActivity.getResult(data!!) ?: return
            handleAutocomplete = false
            viewState.setRecipientAutocompleteItems(ArrayList(0))
            mRecipient = contact
            if (mRecipient!!.id == 0) {
                mRecipient!!.name = mRecipient!!.address
            }
            viewState.setRecipient(mRecipient!!)
            handleAutocomplete = true
        }
    }

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()
        walletSelectorController.onFirstViewAttach(viewState)
        viewState.showBalanceProgress(true)
        accountStorage
                .retryWhen(errorResolver)
                .observe()
                .joinToUi()
                .subscribe(
                        { res: AddressListBalancesTotal ->
                            if (!res.isEmpty) {
                                val acc = accountStorage.entity.mainWallet
                                if (mLastAccount != null) {
                                    onAccountSelected(acc.findCoinByName(mLastAccount!!.coin).orElse(acc.coinsList[0]))
                                } else {
                                    onAccountSelected(acc.coinsList[0])
                                }
                            }
                            viewState.showBalanceProgress(false)
                        },
                        { t: Throwable ->
                            viewState.onError(t)
                            viewState.showBalanceProgress(false)
                        }
                )
                .disposeOnDestroy()

        mInputChange = BehaviorSubject.create()
        mAddressChange = BehaviorSubject.create()

        mInputChange!!
                .toFlowable(BackpressureStrategy.LATEST)
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

        mAddressChange!!
                .toFlowable(BackpressureStrategy.LATEST)
                .debounce(200, TimeUnit.MILLISECONDS)
                .subscribe { address: String -> onAddressChanged(address) }
                .disposeOnDestroy()

        checkEnableSubmit()

        viewState.setFormValidationListener {
            mFormValid = it
            Timber.d("Form is valid: %b", it)
            checkEnableSubmit()
        }

        viewState.setOnTextChangedListener { input, valid ->
            onInputTextChanged(input, valid)
        }

        viewState.setMaxAmountValidator {
            mFromAccount
        }
    }

    private fun onClickClearPayload() {
        viewState.hidePayload()
    }

    private fun onClickContacts() {
        viewState.startAddressBook(REQUEST_CODE_ADDRESS_BOOK_SELECT)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onClickAddPayload(view: View) {
        viewState.showPayload()
    }

    private fun loadAndSetFee() {
        gasRepo.minGas.rxGate()
                .subscribeOn(Schedulers.io())
                .toFlowable(BackpressureStrategy.LATEST)
                .debounce(200, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { res: GateResult<GasValue> ->
                            if (res.isOk) {
                                mGasPrice = res.result.gas
                                Timber.d("Min Gas price: %s", mGasPrice.toString())
                                setupFee()
                            }
                        },
                        { e: Throwable? ->
                            Timber.w(e)
                        }
                )
    }

    private fun setupFee() {
        mSendFee = OperationType.SendCoin.fee.multiply(BigDecimal(mGasPrice))
        mSendFee = mSendFee!! + payloadFee
        val fee: String = String.format("%s %s", mSendFee!!.humanize(), DEFAULT_COIN)
        viewState.setFee(fee)
    }

    private val payloadFee: BigDecimal
        get() = BigDecimal.valueOf(firstNonNull(mPayload, ByteArray(0)).size.toLong()).multiply(PAYLOAD_FEE)

    private fun checkEnoughBalance(amount: BigDecimal): Boolean {
        if (mFromAccount!!.coin!!.toLowerCase() != DEFAULT_COIN.toLowerCase()) {
            return true
        }
        return amount >= fee
    }

    private fun onAmountChanged(amount: String?) {
        mAmount = amount.parseBigDecimal()

        if (!mClickedUseMax.get()) {
            mUseMax.set(false)
        }

        mClickedUseMax.set(false)
        loadAndSetFee()
        checkEnableSubmit()
    }

    private fun onAddressChanged(address: String) {
        if (address.isEmpty()) {
            viewState.setFee("")
        } else {
            setupFee()
            if (mRecipient != null) {
                viewState.setRecipientError(null)
            }

        }
    }

    private fun onSubmit() {
        if (mRecipient == null) {
            viewState.setRecipientError("Recipient required")
            return
        }
        analytics.send(AppEvent.SendCoinsSendButton)
        mAvatar = mRecipient!!.avatar
        startSendDialog()
    }

    private fun startSendDialog() {
        viewState.startDialog { ctx ->
            try {
                analytics.send(AppEvent.SendCoinPopupScreen)

                val dialog = TxSendStartDialog.Builder(ctx, R.string.tx_send_overall_title)
                        .setAmount(mAmount)
                        .setRecipientName(mRecipient!!.name)
                        .setCoin(mFromAccount!!.coin)
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
        if (mFromAccount == null) {
            Timber.d("Account did not loaded yet!")
            viewState.setSubmitEnabled(false)
            return
        }

        Timber.d("Amount did set and it's NOT a NULL")
        val a = MathHelper.bdGTE(mAmount, ZERO)
        val b = mFormValid
        val c = checkEnoughBalance(mFromAccount!!.amount)
        val formFullyValid = a && b && c
        viewState.setSubmitEnabled(formFullyValid)
    }

    private fun onClickMaximum(view: View?) {
        if (mFromAccount == null) {
            viewState.setCommonError("Account didn't loaded yet...")
            return
        }
        mUseMax.set(true)
        mClickedUseMax.set(true)
        mAmount = mFromAccount!!.amount
        viewState.setAmount(mFromAccount!!.amount.toPlain())
        analytics.send(AppEvent.SendCoinsUseMaxButton)
        if (view != null && view.context is Activity) {
            KeyboardHelper.hideKeyboard(view.context as Activity)
        }
    }

    private fun findAccountByCoin(coin: String): Optional<CoinBalance> {
        return accountStorage.entity.mainWallet.findCoinByName(coin)
    }

    @Throws(OperationInvalidDataException::class)
    private fun createPreTx(): TransactionSign {
        val preTx: Transaction
        val builder = Transaction.Builder(BigInteger("1"))
                .setGasCoin(mGasCoin)
                .setGasPrice(mGasPrice)

        if (mPayload != null && mPayload!!.isNotEmpty()) {
            builder.setPayload(mPayload)
        }
        preTx = builder
                .sendCoin()
                .setCoin(mFromAccount!!.coin)
                .setTo(mRecipient!!.address)
                .setValue(mAmount)
                .build()

        val dummyPrivate = PrivateKey("F000000000000000000000000000000000000000000000000000000000000000")
        return preTx.signSingle(dummyPrivate)
    }

    @Throws(OperationInvalidDataException::class)
    private fun createFinalTx(nonce: BigInteger, amountToSend: BigDecimal?): Transaction {
        val tx: Transaction
        val builder = Transaction.Builder(nonce)
                .setGasCoin(mGasCoin)
                .setGasPrice(mGasPrice)
        if (mPayload != null && mPayload!!.isNotEmpty()) {
            builder.setPayload(mPayload)
        }
        tx = builder
                .sendCoin()
                .setCoin(mFromAccount!!.coin)
                .setTo(mRecipient!!.address)
                .setValue(amountToSend)
                .build()

        return tx
    }

    private val fee: BigDecimal
        get() = OperationType.SendCoin.fee + payloadFee

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

        viewState.startDialog { ctx: Context? ->
            val dialog = WalletProgressDialog.Builder(ctx, R.string.tx_send_in_progress)
                    .setText(R.string.please_wait)
                    .create()
            dialog.setCancelable(false)

            // BIP account exists anyway, no need
            val baseAccount = findAccountByCoin(DEFAULT_COIN).get()
            val sendAccount = mFromAccount
            val isBaseAccount = sendAccount!!.coin == DEFAULT_COIN
            val enoughBaseForFee: Boolean

            // default coin for pay fee - MNT (base coin)
            val txFeeValue = GateResult<TransactionCommissionValue>()
            txFeeValue.result = TransactionCommissionValue()
            txFeeValue.result.value = feeNormalized
            enoughBaseForFee = baseAccount.amount >= fee
            var txFeeValueResolver: Observable<GateResult<TransactionCommissionValue>> = Observable.just(txFeeValue)
            val txNonceResolver = estimateRepo.getTransactionCount(mFromAccount!!.address!!).rxGate()

            // if enough balance on base BIP account, set it as gas coin
            if (enoughBaseForFee) {
                Timber.tag("TX Send").d("Using base coin commission %s", DEFAULT_COIN)
                mGasCoin = baseAccount.coin!!
            } else if (!isBaseAccount) {
                Timber.tag("TX Send").d("Not enough balance in %s to pay fee, using %s coin", DEFAULT_COIN, sendAccount.coin)
                mGasCoin = sendAccount.coin!!
                // otherwise getting
                Timber.tag("TX Send").d("Resolving REAL fee value in custom coin %s relatively to base coin", mFromAccount!!.coin)
                // resolving fee currency for custom currency
                // creating tx
                try {
                    val preSign = createPreTx()
                    txFeeValueResolver = estimateRepo.getTransactionCommission(preSign).rxGate()
                } catch (e: OperationInvalidDataException) {
                    Timber.w(e)
                    val commissionValue = GateResult<TransactionCommissionValue>()
                    txFeeValue.result.value = feeNormalized
                    txFeeValueResolver = Observable.just(commissionValue)
                }
            }

            // creating preparation result to send transaction
            Observable
                    .combineLatest(txFeeValueResolver, txNonceResolver, BiFunction { t1: GateResult<TransactionCommissionValue>, t2: GateResult<TxCount> -> TxInitData(t1, t2) })
                    .switchMap { txInitData: TxInitData ->
                        // if in previous request we've got error, returning it
                        if (!txInitData.isSuccess) {
                            return@switchMap Observable.just(GateResult.copyError<network.minter.explorer.models.PushResult>(txInitData.errorResult))
                        }
                        val amountToSend: BigDecimal

                        // don't calc fee if enough balance in base coin and we are sending not a base coin (MNT or BIP)
                        if (enoughBaseForFee && !isBaseAccount) {
                            txInitData.commission = ZERO
                        }

                        // if balance enough to send required sum + fee, do nothing
                        // (mAmount + txInitData.commission) <= mFromAccount.getBalance()

                        if (mFromAccount!!.amount >= (mAmount!! + txInitData.commission!!)) {
                            Timber.tag("TX Send").d("Don't change sending amount - balance enough to send")
                            amountToSend = mAmount!!
                        } else {
                            if (!mUseMax.get()) {
                                txInitData.commission = ZERO
                            }
                            amountToSend = mAmount!! - txInitData.commission!!
                            Timber.tag("TX Send").d("Subtracting sending amount (-%s): balance not enough to send", txInitData.commission)
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
    private fun signSendTx(dialog: WalletProgressDialog, nonce: BigInteger, amountToSend: BigDecimal?): ObservableSource<GateResult<PushResult>> {
        // creating tx
        val tx = createFinalTx(nonce.add(BigInteger.ONE), amountToSend)

        // if user created account with ledger, use it to sign tx
        return if (session.role == AuthSession.AuthType.Hardware) {
            dialog.setText("Please, compare transaction hashes: %s", tx.unsignedTxHash)
            Timber.d("Unsigned tx hash: %s", tx.unsignedTxHash)
            signSendTxExternally(dialog, tx)
        } else {
            // old school signing
            signSendTxInternally(tx)
        }
    }

    private fun signSendTxInternally(tx: Transaction): ObservableSource<GateResult<PushResult>> {
        val data = secretStorage.getSecret(mFromAccount!!.address!!)
        val sign = tx.signSingle(data.privateKey)
        return gateTxRepo.sendTransaction(sign).rxGate().joinToUi()
    }

    private fun signSendTxExternally(dialog: WalletProgressDialog, tx: Transaction): ObservableSource<GateResult<PushResult>> {
        val devInstance = Wallet.app().ledger()
        if (!devInstance.isReady) {
            dialog.setText("Connect ledger and open Minter Application")
        }
        return RxMinterLedger
                .initObserve(devInstance)
                .flatMap { dev: RxMinterLedger ->
                    dialog.setText("Compare hashes: " + tx.unsignedTxHash.toHexString())
                    dev.signTxHash(tx.unsignedTxHash)
                }
                .toObservable()
                .switchMap { signatureSingleData: SignatureSingleData? ->
                    val sign = tx.signExternal(signatureSingleData)
                    dialog.setText(R.string.tx_send_in_progress)
                    gateTxRepo.sendTransaction(sign).rxGate().joinToUi()
                }
                .doFinally { devInstance.destroy() }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
    }

    private fun onExecuteComplete() {
    }

    private fun onFailedExecuteTransaction(throwable: Throwable) {
        Timber.w(throwable, "Uncaught tx error")
        viewState.startDialog { ctx ->
            ConfirmDialog.Builder(ctx, "Unable to send transaction")
                    .setText(throwable.message)
                    .setPositiveAction("Close")
                    .create()
        }
    }

    private fun onErrorExecuteTransaction(errorResult: GateResult<*>) {
        Timber.e(errorResult.message, "Unable to send transaction")
        viewState.startDialog { ctx ->
            ConfirmDialog.Builder(ctx, "Unable to send transaction")
                    .setText(errorResult.message)
                    .setPositiveAction("Close")
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
                    .setValue(mRecipient!!.name)
                    .setPositiveAction(R.string.btn_view_tx) { d, _ ->
                        Wallet.app().sounds().play(R.raw.click_pop_zap)
                        viewState.startExplorer(result.result.txHash.toString())
                        d.dismiss()
                        analytics.send(AppEvent.SentCoinPopupViewTransactionButton)
                    }
                    .setNegativeAction(R.string.btn_close) { d: DialogInterface, _: Int ->
                        d.dismiss()
                        analytics.send(AppEvent.SentCoinPopupCloseButton)
                    }

            if (mRecipient != null && mRecipient!!.id == 0 && mRecipient!!.address != null) {
                val recipientAddress = mRecipient!!.address!!
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

        if (mRecipient != null) {
            if (mRecipient!!.id == 0) {
                mRecipient!!.name = "Anonymous"
            }
            addressBookRepo.writeLastUsed(mRecipient!!)
        }

        // clear form
        mAmount = null
        mRecipient = null
        mSendFee = null
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
                                    mRecipient = res
                                    mAddressChange!!.onNext(mRecipient!!.name!!)
                                },
                                { t: Throwable ->
                                    Timber.d(t)
                                    mRecipient = null
                                    viewState.setSubmitEnabled(false)

                                })
                        .disposeOnDestroy()
            }
            R.id.input_amount -> mInputChange!!.onNext(editText.text.toString())
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
        mFromAccount = coinAccount
        mLastAccount = coinAccount
        viewState.setAccountName(String.format("%s (%s)", coinAccount.coin?.toUpperCase(), coinAccount.amount.humanize()))
    }
}