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
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.Unbinder
import com.edwardstock.inputfield.InputField
import com.edwardstock.inputfield.InputFieldAutocomplete
import com.edwardstock.inputfield.form.DecimalInputFilter
import com.edwardstock.inputfield.form.InputGroup
import com.edwardstock.inputfield.form.InputWrapper
import com.edwardstock.inputfield.form.validators.RegexValidator
import moxy.presenter.InjectPresenter
import moxy.presenter.ProvidePresenter
import network.minter.bipwallet.BuildConfig
import network.minter.bipwallet.R
import network.minter.bipwallet.addressbook.models.AddressContact
import network.minter.bipwallet.addressbook.ui.AddressBookActivity
import network.minter.bipwallet.addressbook.ui.AddressContactEditDialog
import network.minter.bipwallet.home.HomeModule
import network.minter.bipwallet.home.HomeTabFragment
import network.minter.bipwallet.internal.Wallet
import network.minter.bipwallet.internal.dialogs.BaseBottomSheetDialogFragment
import network.minter.bipwallet.internal.dialogs.ConfirmDialog
import network.minter.bipwallet.internal.dialogs.DialogExecutor
import network.minter.bipwallet.internal.dialogs.WalletDialog
import network.minter.bipwallet.internal.helpers.ViewHelper
import network.minter.bipwallet.internal.helpers.forms.validators.PayloadValidator
import network.minter.bipwallet.internal.views.utils.SingleCallHandler
import network.minter.bipwallet.sending.account.SelectorData
import network.minter.bipwallet.sending.account.WalletAccountSelectorDialog
import network.minter.bipwallet.sending.adapters.RecipientListAdapter
import network.minter.bipwallet.sending.contract.SendView
import network.minter.bipwallet.sending.views.SendTabPresenter
import network.minter.bipwallet.tx.ui.ExternalTransactionActivity
import network.minter.bipwallet.wallets.selector.WalletItem
import network.minter.bipwallet.wallets.selector.WalletSelector
import network.minter.explorer.models.CoinBalance
import permissions.dispatcher.*
import javax.inject.Inject
import javax.inject.Provider

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich (edward.vstock>@gmail.com)
 */
@RuntimePermissions
class SendTabFragment : HomeTabFragment(), SendView {
    @Inject lateinit var presenterProvider: Provider<SendTabPresenter>
    @InjectPresenter lateinit var presenter: SendTabPresenter

    @BindView(R.id.toolbar) lateinit var toolbar: Toolbar
    @BindView(R.id.input_coin) lateinit var coinInput: InputField
    @BindView(R.id.input_recipient) lateinit var recipientInput: InputFieldAutocomplete
    @BindView(R.id.input_amount) lateinit var amountInput: InputField
    @BindView(R.id.input_payload) lateinit var payloadInput: InputField
    @BindView(R.id.action) lateinit var actionSend: Button
    @BindView(R.id.text_error) lateinit var errorView: TextView
    @BindView(R.id.fee_value) lateinit var feeValue: TextView
    @BindView(R.id.wallet_selector) lateinit var walletSelector: WalletSelector

    private val inputGroup: InputGroup = InputGroup()
    private var unbinder: Unbinder? = null
    private var mCurrentDialog: WalletDialog? = null
    private var mAutocompleteAdapter: RecipientListAdapter? = null
    private var bottomSheetDialog: BaseBottomSheetDialogFragment? = null

    override fun onTabSelected() {
        super.onTabSelected()
        ViewHelper.setSystemBarsLightness(this, true)
        ViewHelper.setStatusBarColorAnimate(this, 0xFF_FFFFFF.toInt())
    }

    override fun onAttach(context: Context) {
        HomeModule.getComponent().inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        HomeModule.getComponent().inject(this)
        super.onCreate(savedInstanceState)
    }

    override fun onDestroyView() {
        WalletDialog.releaseDialog(mCurrentDialog)
        super.onDestroyView()
        unbinder!!.unbind()
    }

    @Suppress("UsePropertyAccessSyntax")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_tab_send, container, false)
        unbinder = ButterKnife.bind(this, view)

        coinInput.input.setFocusable(false)

        inputGroup.setup {
            add(amountInput, RegexValidator("^(\\d*)(\\.)?(\\d{1,18})?$").apply {
                errorMessage = "Invalid number"
            })
            add(recipientInput, RecipientValidator("Invalid recipient", true))
            add(payloadInput, PayloadValidator())
        }

        inputGroup.addFilter(amountInput, DecimalInputFilter(amountInput))

        recipientInput.clearFocus()
        amountInput.clearFocus()


        setHasOptionsMenu(true)
        activity!!.menuInflater.inflate(R.menu.menu_send_toolbar, toolbar.menu)
        toolbar.setOnMenuItemClickListener { item: MenuItem -> onOptionsItemSelected(item) }
        mAutocompleteAdapter = RecipientListAdapter(context!!)
        recipientInput.input.setAdapter(mAutocompleteAdapter)
        return view
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_scan_tx) {
            SingleCallHandler.call(item) { startScanQRWithPermissions(REQUEST_CODE_QR_SCAN_TX) }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun setOnClickAccountSelectedListener(listener: View.OnClickListener) {
        coinInput.setOnClickListener(listener)
    }

    override fun setOnClickMaximum(listener: View.OnClickListener) {
        amountInput.setOnSuffixTextClickListener(listener)
    }

    override fun setOnClickAddPayload(listener: View.OnClickListener) {
        payloadInput.inputOverlay!!.setOnClickListener(listener)
    }

    override fun setOnClickClearPayload(listener: View.OnClickListener) {
        payloadInput.setOnSuffixImageClickListener(listener)
    }

    override fun setPayloadChangeListener(listener: TextWatcher) {
        payloadInput.addTextChangedListener(listener)
    }

    override fun setPayload(payload: String?) {
        payloadInput.setText(payload)
        payloadInput.setSelection(payload?.length ?: 0)
    }

    //@TODO
    override fun setActionTitle(buttonTitle: Int) {
        actionSend.setText(buttonTitle)
    }

    override fun startExternalTransaction(rawData: String?) {
        ExternalTransactionActivity.Builder(activity!!, rawData!!)
                .start()
    }

    override fun showPayload() {
        payloadInput.inputOverlayVisible = false
    }

    override fun hidePayload() {
        payloadInput.text = null
        payloadInput.inputOverlayVisible = true
    }

    override fun setWallets(walletItems: List<WalletItem>) {
        walletSelector.setWallets(walletItems)
    }

    override fun setMainWallet(walletItem: WalletItem) {
        walletSelector.setMainWallet(walletItem)
    }

    override fun setPayloadError(error: CharSequence?) {
        inputGroup.setError("payload", error)
    }

    override fun setOnTextChangedListener(listener: (InputWrapper, Boolean) -> Unit) {
        inputGroup.addTextChangedListener(listener)
    }

    override fun setOnContactsClickListener(listener: View.OnClickListener) {
        recipientInput.setOnSuffixImageClickListener(listener)
    }

    override fun setAccountName(accountName: CharSequence) {
        coinInput.setText(accountName)
    }

    override fun setOnSubmit(listener: View.OnClickListener) {
        actionSend.setOnClickListener(listener)
    }

    override fun setSubmitEnabled(enabled: Boolean) {
        actionSend.post { actionSend.isEnabled = enabled }
    }

    override fun clearInputs() {
        inputGroup.reset()
        recipientInput.clearFocus()
        amountInput.clearFocus()
        inputGroup.clearErrors()
    }

    override fun startDialog(executor: DialogExecutor) {
        mCurrentDialog = WalletDialog.switchDialogWithExecutor(this, mCurrentDialog, executor)
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
//        SendTabFra
        startScanQRWithPermissionCheck(requestCode)
    }

    override fun startAddressBook(requestCode: Int) {
        AddressBookActivity.Builder(this)
                .start(requestCode)
    }

    override fun setRecipient(to: AddressContact) {
        recipientInput.post { recipientInput.setText(to.name) }
    }

    override fun setRecipientError(error: CharSequence?) {
        recipientInput.post { inputGroup.setError("recipient", error) }
    }

    override fun setAmountError(error: CharSequence?) {
        inputGroup.setError("amount", error)
    }

    override fun setCommonError(error: CharSequence?) {
        ViewHelper.visible(errorView, !error.isNullOrEmpty())
        errorView.text = error
    }

    override fun setError(error: Int) {
        ViewHelper.visible(errorView, error != 0)
        errorView.setText(error)
    }

    override fun setAmount(amount: CharSequence?) {
        amountInput.setText(amount)
    }

    override fun setFee(fee: CharSequence?) {
        feeValue.text = fee
    }

    override fun setRecipientAutocompleteItemClickListener(listener: RecipientListAdapter.OnItemClickListener) {
        val cl = RecipientListAdapter.OnItemClickListener { item: AddressContact?, position: Int ->
            listener.onClick(item, position)
            recipientInput.input.dismissDropDown()
        }
        mAutocompleteAdapter!!.setOnItemClickListener(cl)
    }

    override fun setRecipientAutocompleteItems(items: List<AddressContact>) {
        recipientInput.post {
            mAutocompleteAdapter!!.setItems(items)
            recipientInput.input.showDropDown()
        }
    }

    override fun hideAutocomplete() {
        recipientInput.post { recipientInput.input.dismissDropDown() }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        presenter.onActivityResult(requestCode, resultCode, data)
    }

    override fun setFormValidationListener(listener: (Boolean) -> Unit) {
        inputGroup.addFormValidateListener(listener)
    }

    override fun startAccountSelector(accounts: List<SelectorData<CoinBalance>>, clickListener: (SelectorData<CoinBalance>) -> Unit) {
        WalletAccountSelectorDialog.Builder<CoinBalance>(activity!!, "Select account")
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

    override fun startAddContact(address: String) {
        if (bottomSheetDialog != null) {
            bottomSheetDialog!!.dismiss()
            bottomSheetDialog = null
        }
        if (fragmentManager == null) {
            return
        }
        bottomSheetDialog = AddressContactEditDialog.Builder()
                .setAddress(address)
                .build()

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