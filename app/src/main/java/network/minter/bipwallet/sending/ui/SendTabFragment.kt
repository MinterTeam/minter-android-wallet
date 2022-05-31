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
package network.minter.bipwallet.sending.ui

import android.animation.Animator
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.edwardstock.inputfield.form.DecimalInputFilter
import com.edwardstock.inputfield.form.InputGroup
import com.edwardstock.inputfield.form.InputWrapper
import com.edwardstock.inputfield.form.validators.CustomValidator
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import me.saket.bettermovementmethod.BetterLinkMovementMethod
import moxy.presenter.InjectPresenter
import moxy.presenter.ProvidePresenter
import network.minter.bipwallet.R
import network.minter.bipwallet.addressbook.contract.GetAddressBookContact
import network.minter.bipwallet.addressbook.models.AddressContact
import network.minter.bipwallet.addressbook.ui.AddressContactEditDialog
import network.minter.bipwallet.databinding.FragmentTabSendBinding
import network.minter.bipwallet.delegation.ui.DelegateUnbondActivity
import network.minter.bipwallet.home.HomeModule
import network.minter.bipwallet.home.HomeTabFragment
import network.minter.bipwallet.internal.Wallet
import network.minter.bipwallet.internal.helpers.KeyboardHelper
import network.minter.bipwallet.internal.helpers.MathHelper.parseBigDecimal
import network.minter.bipwallet.internal.helpers.ViewExtensions.postApply
import network.minter.bipwallet.internal.helpers.ViewExtensions.tr
import network.minter.bipwallet.internal.helpers.ViewExtensions.visibleForTestnet
import network.minter.bipwallet.internal.helpers.ViewHelper
import network.minter.bipwallet.internal.helpers.forms.validators.IsNotMnemonicValidator
import network.minter.bipwallet.internal.helpers.forms.validators.NewLineInputFilter
import network.minter.bipwallet.internal.helpers.forms.validators.PayloadValidator
import network.minter.bipwallet.internal.system.BroadcastReceiverManager
import network.minter.bipwallet.internal.views.utils.SingleCallHandler
import network.minter.bipwallet.sending.account.SelectorData
import network.minter.bipwallet.sending.account.SelectorDialog
import network.minter.bipwallet.sending.adapters.RecipientListAdapter
import network.minter.bipwallet.sending.contract.QRLauncher
import network.minter.bipwallet.sending.contract.SendView
import network.minter.bipwallet.sending.views.SendTabPresenter
import network.minter.bipwallet.services.livebalance.broadcast.RTMBlockReceiver
import network.minter.bipwallet.tx.ui.ExternalTransactionActivity
import network.minter.bipwallet.wallets.selector.WalletItem
import network.minter.bipwallet.wallets.selector.WalletListAdapter
import network.minter.bipwallet.wallets.selector.WalletSelectorBroadcastReceiver
import network.minter.bipwallet.wallets.ui.WalletsTopRecolorHelper
import network.minter.bipwallet.wallets.utils.LastBlockHandler
import network.minter.core.crypto.MinterPublicKey
import network.minter.explorer.models.CoinBalance
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Provider

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
class SendTabFragment : HomeTabFragment(), SendView {
    @Inject
    lateinit var presenterProvider: Provider<SendTabPresenter>
    @InjectPresenter
    lateinit var presenter: SendTabPresenter

    private val inputGroup: InputGroup = InputGroup()
    private var recipientListAdapter: RecipientListAdapter? = null

    private lateinit var binding: FragmentTabSendBinding
    private var tabIsActive: Boolean = false

    private val qrLauncher = QRLauncher(this, { requireActivity() }) {
        presenter.handleQRResult(it)
    }
//    private var launchQRScannerDeniedDialog: Dialog? = null
//    private var launchQRScannerRationaleDialog: Dialog? = null
//    private val launchQRScanner = registerForActivityResult(GetQRResultString()) {
//        presenter.handleQRResult(it)
//    }
//    private val launchQRScannerPermissions = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
//        if (isGranted) {
//            launchQRScanner.launch(Unit)
//        } else {
//            launchQRScannerDeniedDialog = ConfirmDialog.Builder(requireContext(), R.string.dialog_title_camera_permission)
//                    .setText(R.string.dialog_text_camera_permission)
//                    .setPositiveAction(R.string.btn_open_settings) { d: DialogInterface, _: Int ->
//                        val intent = Intent()
//                        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
//                        val uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
//                        intent.data = uri
//                        startActivity(intent)
//                        d.dismiss()
//                    }
//                    .setNegativeAction(R.string.btn_cancel) { d: DialogInterface, _: Int -> d.dismiss() }
//                    .create()
//                    .also { it.show() }
//        }
//    }

    private val launchAddressBook = registerForActivityResult(GetAddressBookContact()) {
        presenter.handleAddressBookResult(it)
    }

    override fun onTabSelected() {
        super.onTabSelected()
        tabIsActive = true
        if (WalletsTopRecolorHelper.enableRecolorSystemUI()) {
            ViewHelper.setSystemBarsLightness(this, true)
            ViewHelper.setStatusBarColorAnimate(this, 0xFF_FFFFFF.toInt())
        }
    }

    override fun onTabUnselected() {
        super.onTabUnselected()
        tabIsActive = false
        binding.inputRecipient.input.dismissDropDown()
        KeyboardHelper.hideKeyboard(this)
    }

    override fun onAttach(context: Context) {
        HomeModule.component?.inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        HomeModule.component?.inject(this)
        super.onCreate(savedInstanceState)
    }

    @Suppress("UsePropertyAccessSyntax")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentTabSendBinding.inflate(inflater, container, false)

        binding.apply {
            testnetWarning.visibleForTestnet()

            walletSelector.registerLifecycle(requireActivity())

            inputCoin.input.setFocusable(false)

            inputGroup.setup {
                add(inputRecipient, RecipientValidator(tr(R.string.input_validator_invalid_recipient), true))
                add(inputPayload, PayloadValidator())
            }
            inputGroup.addInput(inputAmount)
            inputGroup.addFilter(inputAmount, DecimalInputFilter(inputAmount))
            inputGroup.addFilter(inputRecipient, NewLineInputFilter())
            // add ability to click error link
            inputRecipient.errorView!!.movementMethod = BetterLinkMovementMethod.newInstance().apply {
                setOnLinkClickListener { _, _ ->
                    val tmpPubKey = inputRecipient.text.toString()
                    try {
                        val pubKey = MinterPublicKey(tmpPubKey)
                        DelegateUnbondActivity.Builder(this@SendTabFragment, DelegateUnbondActivity.Type.Delegate)
                                .setPublicKey(pubKey)
                                .start()
                    } catch (err: Throwable) {
                        Timber.w(err, "Unable to handle public key %s", tmpPubKey)
                    }
                    inputRecipient.setText("")
                    true
                }
            }

            val payloadMnemonicValidator = IsNotMnemonicValidator("""
    ATTENTION: You are about to send seed phrase in the message attached to this transaction.

    If you do this, anyone will be able to see it and access your funds!
    """.trimIndent())
            inputGroup.addValidator(inputPayload, payloadMnemonicValidator)


            inputRecipient.input.threshold = 0
            inputRecipient.input.setDropDownBackgroundResource(R.drawable.shape_rounded_white)

            inputRecipient.clearFocus()
            inputAmount.clearFocus()

            LastBlockHandler.handle(binding.lastUpdated)
            val broadcastManager = BroadcastReceiverManager(requireActivity())
            broadcastManager.add(RTMBlockReceiver {
                LastBlockHandler.handle(binding.lastUpdated, it)
            })
            broadcastManager.register()


            setHasOptionsMenu(true)
            requireActivity().menuInflater.inflate(R.menu.menu_tab_send, toolbar.menu)
            toolbar.setOnMenuItemClickListener { item: MenuItem -> onOptionsItemSelected(item) }
            recipientListAdapter = RecipientListAdapter(requireContext())
            inputRecipient.input.setAdapter(recipientListAdapter)
            inputRecipient.input.setDropDownBackgroundResource(R.drawable.shape_rounded_white)
        }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        qrLauncher.release()
//        launchQRScannerRationaleDialog?.dismiss().also { launchQRScannerRationaleDialog = null }
//        launchQRScannerDeniedDialog?.dismiss().also { launchQRScannerDeniedDialog = null }
    }

    override fun setMaxAmountValidator(coinSupplier: () -> CoinBalance?) {
        inputGroup.addValidator(binding.inputAmount, CustomValidator {
            if (it.isNullOrEmpty() || coinSupplier() == null) {
                return@CustomValidator true
            }

            val num = it.parseBigDecimal()
            num <= coinSupplier()!!.amount
        }.apply { errorMessage = tr(R.string.error_not_enough_coins) })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_scan_tx) {
            SingleCallHandler.call(item) { startScanQR() }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun setOnClickAccountSelectedListener(listener: View.OnClickListener) {
        binding.inputCoin.input.setOnClickListener(listener)
        binding.inputCoin.setOnSuffixImageClickListener(listener)
    }

    override fun setOnClickMaximum(listener: View.OnClickListener) {
        binding.inputAmount.setOnSuffixTextClickListener(listener)
    }

    override fun setOnClickAddPayload(listener: View.OnClickListener) {
        binding.inputPayload.inputOverlay!!.setOnClickListener(listener)
    }

    override fun setOnClickClearPayload(listener: View.OnClickListener) {
        binding.inputPayload.setOnSuffixImageClickListener(listener)
    }

    override fun setPayloadChangeListener(listener: TextWatcher) {
        binding.inputPayload.addTextChangedListener(listener)
    }

    override fun setPayload(payload: String?) {
        binding.inputPayload.setText(payload)
        binding.inputPayload.setSelection(payload?.length ?: 0)
    }

    override fun validate(onValidated: (Boolean) -> Unit) {
        inputGroup.validate(true)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe({ onValidated(it) }, { t -> Timber.w(t) })
    }

    //@TODO
    override fun setActionTitle(buttonTitle: Int) {
        binding.action.setText(buttonTitle)
    }

    override fun startExternalTransaction(rawData: String?) {
        ExternalTransactionActivity.Builder(requireActivity(), rawData!!)
                .start()
    }

    override fun showPayload() {
        binding.inputPayload.inputOverlayVisible = false
    }

    override fun hidePayload() {
        KeyboardHelper.hideKeyboard(this)
        binding.inputPayload.text = null
        binding.inputPayload.inputOverlayVisible = true
    }

    override fun setWallets(walletItems: List<WalletItem>) {
        activity?.let {
            runOnUiThread {
                activity?.let {
                    WalletSelectorBroadcastReceiver.setWallets(it, walletItems)
                }
            }
        }
    }

    override fun setMainWallet(walletItem: WalletItem) {
        activity?.let {
            runOnUiThread {
                activity?.let {
                    WalletSelectorBroadcastReceiver.setMainWallet(it, walletItem)
                }
            }
        }
    }

    override fun setOnClickWalletListener(listener: WalletListAdapter.OnClickWalletListener) {
        binding.walletSelector.setOnClickWalletListener(listener)
    }

    override fun setOnClickAddWalletListener(listener: WalletListAdapter.OnClickAddWalletListener) {
        binding.walletSelector.setOnClickAddWalletListener(listener)
    }

    override fun setOnClickEditWalletListener(listener: WalletListAdapter.OnClickEditWalletListener) {
        binding.walletSelector.setOnClickEditWalletListener(listener)
    }

    override fun setPayloadError(error: CharSequence?) {
        inputGroup.setError("payload", error)
    }

    override fun setOnTextChangedListener(listener: (InputWrapper, Boolean) -> Unit) {
        inputGroup.addTextChangedListener(listener)
    }

    override fun setOnContactsClickListener(listener: View.OnClickListener) {
        binding.inputRecipient.setOnSuffixImageClickListener(listener)
    }

    override fun setAccountName(accountName: CharSequence) {
        binding.inputCoin.setText(accountName)
    }

    override fun setOnSubmit(listener: View.OnClickListener) {
        binding.action.setOnClickListener(listener)
    }

    override fun setSubmitEnabled(enabled: Boolean) {
        binding.action.postApply { it.isEnabled = enabled }
    }

    override fun clearInputs() {
        binding.inputRecipient.clearFocus()
        binding.inputAmount.clearFocus()
        inputGroup.clearErrors()
        inputGroup.hideErrors()
        inputGroup.clearData()
    }

    override fun clearAmount() {
        binding.inputAmount.clearFocus()
        binding.inputAmount.text = null
        inputGroup.clearErrors()
        binding.inputAmount.error = null
    }

    override fun startExplorer(txHash: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Wallet.urlExplorerFront() + "/transactions/" + txHash)))
    }

    override fun startScanQR() {
        qrLauncher.launch()
//        val permission = Manifest.permission.CAMERA
//        when {
//            ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED -> {
//                launchQRScanner.launch(Unit)
//            }
//            shouldShowRequestPermissionRationale(permission) -> {
//                launchQRScannerRationaleDialog = ConfirmDialog.Builder(requireActivity(), R.string.dialog_title_camera_permission)
//                        .setText(R.string.dialog_text_camera_permission)
//                        .setPositiveAction(R.string.btn_ok) { d, _ ->
//                            launchQRScannerPermissions.launch(permission)
//                            d.dismiss()
//                        }
//                        .setNegativeAction(R.string.btn_cancel) { d, _ -> d.dismiss() }
//                        .create()
//                        .also { it.show() }
//            }
//            else -> {
//                launchQRScannerPermissions.launch(permission)
//            }
//        }
    }

    override fun startAddressBook() {
        launchAddressBook.launch(Unit)
    }

    override fun setRecipient(to: AddressContact) {
        binding.inputRecipient.postApply {
            it.setText(to.name)
            if (to.name != null) {
                it.setSelection(to.name!!.length)
            }

        }
    }

    override fun setRecipientError(error: CharSequence?) {
        binding.inputRecipient.post { inputGroup.setError("recipient", error) }
    }

    override fun setAmountError(error: CharSequence?) {
        inputGroup.setError("amount", error)
    }


    override fun setCommonError(error: CharSequence?) {
        binding.textError.post {
            T.isVisible = v = !error.isNullOrEmpty()
            binding.textError.text = error
        }
    }

    override fun setError(error: Int) {
        binding.textError.post {
            T.isVisible = v = error != 0
            binding.textError.setText(error)
        }
    }

    override fun setAmount(amount: CharSequence?) {
        binding.inputAmount.setText(amount)
    }

    override fun setFee(fee: CharSequence?) {
        binding.feeValue.postApply {
            it.text = fee
        }
    }

    override fun startDelegate(publicKey: MinterPublicKey) {
        DelegateUnbondActivity.Builder(this, DelegateUnbondActivity.Type.Delegate)
                .setPublicKey(publicKey)
                .start()
    }

    override fun setRecipientAutocompleteItemClickListener(listener: (AddressContact, Int) -> Unit) {
        recipientListAdapter!!.setOnItemClickListener { item, position ->
            listener.invoke(item, position)
            binding.inputRecipient.input.dismissDropDown()
        }
    }

    override fun setRecipientAutocompleteItems(items: List<AddressContact>) {
        binding.inputRecipient.postApply {
            recipientListAdapter!!.setItems(items)
            if (tabIsActive) {
                it.input.showDropDown()
            }
        }
    }

    override fun showBalanceProgress(show: Boolean) {
        binding.balanceProgress.postApply {

            it.animate()
                    .alpha(if (show) 1f else 0f)
                    .setListener(object : Animator.AnimatorListener {
                        override fun onAnimationEnd(animation: Animator?) {
                            T.isVisible = v = show
                        }

                        override fun onAnimationRepeat(animation: Animator?) {}
                        override fun onAnimationCancel(animation: Animator?) {
                            T.isVisible = v = show
                        }

                        override fun onAnimationStart(animation: Animator?) {}
                    })
                    .setDuration(150)
                    .start()

        }
        T.isVisible = v = show
    }

    override fun hideAutocomplete() {
        binding.inputRecipient.postApply { it.input.dismissDropDown() }
    }

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        presenter.onActivityResult(requestCode, resultCode, data)
//    }

    override fun setFormValidationListener(listener: (Boolean) -> Unit) {
        inputGroup.addFormValidateListener(listener)
    }

    override fun startAccountSelector(accounts: List<SelectorData<CoinBalance>>, clickListener: (SelectorData<CoinBalance>) -> Unit) {
        SelectorDialog.Builder<CoinBalance>(requireActivity(), R.string.dialog_title_choose_coin)
                .setItems(accounts)
                .setOnClickListener(clickListener)
                .create()
                .show()
    }

    override fun startAddContact(address: String, onAdded: (AddressContact) -> Unit) {
        if (bottomSheetDialog != null) {
            bottomSheetDialog!!.dismiss()
            bottomSheetDialog = null
        }
        bottomSheetDialog = AddressContactEditDialog.Builder()
                .setAddress(address)
                .build()

        (bottomSheetDialog as AddressContactEditDialog).onContactAddedOrUpdated = onAdded

        bottomSheetDialog!!.show(parentFragmentManager, "contact")
    }

    @ProvidePresenter
    fun providePresenter(): SendTabPresenter {
        return presenterProvider.get()
    }

    companion object {
        const val IDLE_SEND_CONFIRM_DIALOG = "IDLE_SEND_CONFIRM_DIALOG"
        const val IDLE_SEND_COMPLETE_DIALOG = "IDLE_SEND_COMPLETE_DIALOG"
        const val IDLE_SEND_WAIT_GAS = "IDLE_SEND_WAIT_GAS"
        const val REQUEST_CODE_QR_SCAN_TX = 2001
    }
}