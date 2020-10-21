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
package network.minter.bipwallet.sending.ui

import android.Manifest
import android.animation.Animator
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
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
import network.minter.bipwallet.BuildConfig
import network.minter.bipwallet.R
import network.minter.bipwallet.addressbook.models.AddressContact
import network.minter.bipwallet.addressbook.ui.AddressBookActivity
import network.minter.bipwallet.addressbook.ui.AddressContactEditDialog
import network.minter.bipwallet.databinding.FragmentTabSendBinding
import network.minter.bipwallet.delegation.ui.DelegateUnbondActivity
import network.minter.bipwallet.home.HomeModule
import network.minter.bipwallet.home.HomeTabFragment
import network.minter.bipwallet.internal.Wallet
import network.minter.bipwallet.internal.dialogs.ConfirmDialog
import network.minter.bipwallet.internal.helpers.KeyboardHelper
import network.minter.bipwallet.internal.helpers.MathHelper.parseBigDecimal
import network.minter.bipwallet.internal.helpers.ViewExtensions.postApply
import network.minter.bipwallet.internal.helpers.ViewExtensions.visible
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
import permissions.dispatcher.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Provider

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
@RuntimePermissions
class SendTabFragment : HomeTabFragment(), SendView {
    @Inject lateinit var presenterProvider: Provider<SendTabPresenter>
    @InjectPresenter lateinit var presenter: SendTabPresenter

    private val inputGroup: InputGroup = InputGroup()
    private var recipientListAdapter: RecipientListAdapter? = null

    private lateinit var binding: FragmentTabSendBinding
    private var tabIsActive: Boolean = false

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
        HomeModule.component!!.inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        HomeModule.component!!.inject(this)
        super.onCreate(savedInstanceState)
    }

    @Suppress("UsePropertyAccessSyntax")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentTabSendBinding.inflate(inflater, container, false)

        binding.apply {
            testnetWarning.visibleForTestnet()

            walletSelector.registerLifecycle(activity!!)

            inputCoin.input.setFocusable(false)

            inputGroup.setup {
                add(inputRecipient, RecipientValidator("Invalid recipient", true))
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
            val broadcastManager = BroadcastReceiverManager(activity!!)
            broadcastManager.add(RTMBlockReceiver {
                LastBlockHandler.handle(binding.lastUpdated, it)
            })
            broadcastManager.register()


            setHasOptionsMenu(true)
            activity!!.menuInflater.inflate(R.menu.menu_tab_send, toolbar.menu)
            toolbar.setOnMenuItemClickListener { item: MenuItem -> onOptionsItemSelected(item) }
            recipientListAdapter = RecipientListAdapter(context!!)
            inputRecipient.input.setAdapter(recipientListAdapter)
            inputRecipient.input.setDropDownBackgroundResource(R.drawable.shape_rounded_white)
        }
        return binding.root
    }

    override fun setMaxAmountValidator(coinSupplier: () -> CoinBalance?) {
        inputGroup.addValidator(binding.inputAmount, CustomValidator {
            if (it.isNullOrEmpty() || coinSupplier() == null) {
                return@CustomValidator true
            }

            val num = it.parseBigDecimal()
            num <= coinSupplier()!!.amount
        }.apply { errorMessage = getString(R.string.error_not_enough_coins) })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_scan_tx) {
            SingleCallHandler.call(item) { startScanQRWithPermissions(REQUEST_CODE_QR_SCAN_TX) }
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
        ExternalTransactionActivity.Builder(activity!!, rawData!!)
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
                WalletSelectorBroadcastReceiver.setWallets(activity!!, walletItems)
            }
        }
    }

    override fun setMainWallet(walletItem: WalletItem) {
        activity?.let {
            runOnUiThread {
                WalletSelectorBroadcastReceiver.setMainWallet(activity!!, walletItem)
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

    @NeedsPermission(Manifest.permission.CAMERA)
    override fun startScanQR(requestCode: Int) {
        val i = Intent(activity, QRCodeScannerActivity::class.java)
        activity!!.startActivityForResult(i, requestCode)
    }

    override fun startScanQRWithPermissions(requestCode: Int) {
        startScanQRWithPermissionCheck(requestCode)
    }

    override fun startAddressBook(requestCode: Int) {
        AddressBookActivity.Builder(this)
                .start(requestCode)
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
            binding.textError.visible = !error.isNullOrEmpty()
            binding.textError.text = error
        }
    }

    override fun setError(error: Int) {
        binding.textError.post {
            binding.textError.visible = error != 0
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
                            it.visible = show
                        }

                        override fun onAnimationRepeat(animation: Animator?) {}
                        override fun onAnimationCancel(animation: Animator?) {
                            it.visible = show
                        }

                        override fun onAnimationStart(animation: Animator?) {}
                    })
                    .setDuration(150)
                    .start()

        }
        binding.balanceProgress.visible = show
    }

    override fun hideAutocomplete() {
        binding.inputRecipient.postApply { it.input.dismissDropDown() }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        presenter.onActivityResult(requestCode, resultCode, data)
    }

    override fun setFormValidationListener(listener: (Boolean) -> Unit) {
        inputGroup.addFormValidateListener(listener)
    }

    override fun startAccountSelector(accounts: List<SelectorData<CoinBalance>>, clickListener: (SelectorData<CoinBalance>) -> Unit) {
        SelectorDialog.Builder<CoinBalance>(activity!!, R.string.dialog_title_choose_coin)
                .setItems(accounts)
                .setOnClickListener(clickListener)
                .create()
                .show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onRequestPermissionsResult(requestCode, grantResults)
    }

    @OnShowRationale(Manifest.permission.CAMERA)
    fun showRationaleForCamera(request: PermissionRequest) {
        ConfirmDialog.Builder(activity!!, "Camera request")
                .setText("We need access to your camera to take a shot with Minter Address QR Code")
                .setPositiveAction("Sure") { d, _ ->
                    request.proceed()
                    d.dismiss()
                }
                .setNegativeAction("No, I've change my mind") { d: DialogInterface, _: Int ->
                    request.cancel()
                    d.dismiss()
                }.create()
                .show()
    }

    @OnPermissionDenied(Manifest.permission.CAMERA)
    fun showOpenPermissionsForCamera() {
        ConfirmDialog.Builder(activity!!, "Camera request")
                .setText("We need access to your camera to take a shot with Minter Address QR Code")
                .setPositiveAction("Open settings") { d: DialogInterface, _: Int ->
                    val intent = Intent()
                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    val uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                    intent.data = uri
                    startActivity(intent)
                    d.dismiss()
                }
                .setNegativeAction("Cancel") { d: DialogInterface, _: Int -> d.dismiss() }
                .create()
                .show()
    }

    override fun startAddContact(address: String, onAdded: (AddressContact) -> Unit) {
        if (fragmentManager == null) {
            return
        }

        if (bottomSheetDialog != null) {
            bottomSheetDialog!!.dismiss()
            bottomSheetDialog = null
        }
        bottomSheetDialog = AddressContactEditDialog.Builder()
                .setAddress(address)
                .build()

        (bottomSheetDialog as AddressContactEditDialog).onContactAddedOrUpdated = onAdded

        bottomSheetDialog!!.show(fragmentManager!!, "contact")
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