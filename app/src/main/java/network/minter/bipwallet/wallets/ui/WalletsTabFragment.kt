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
package network.minter.bipwallet.wallets.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.google.android.material.appbar.AppBarLayout
import moxy.presenter.InjectPresenter
import moxy.presenter.ProvidePresenter
import network.minter.bipwallet.BuildConfig
import network.minter.bipwallet.R
import network.minter.bipwallet.addressbook.models.AddressContact
import network.minter.bipwallet.databinding.FragmentTabWalletsBinding
import network.minter.bipwallet.delegation.ui.DelegationListActivity
import network.minter.bipwallet.exchange.ui.ConvertCoinActivity
import network.minter.bipwallet.home.HomeModule
import network.minter.bipwallet.home.HomeTabFragment
import network.minter.bipwallet.home.ui.HomeActivity
import network.minter.bipwallet.internal.Wallet
import network.minter.bipwallet.internal.dialogs.ActionListener
import network.minter.bipwallet.internal.dialogs.BaseBottomSheetDialogFragment
import network.minter.bipwallet.internal.dialogs.ConfirmDialog
import network.minter.bipwallet.internal.dialogs.WalletDialog
import network.minter.bipwallet.internal.dialogs.WalletDialog.Companion.switchDialogWithExecutor
import network.minter.bipwallet.internal.views.utils.SingleCallHandler
import network.minter.bipwallet.sending.ui.QRCodeScannerActivity
import network.minter.bipwallet.sending.ui.SendTabFragment
import network.minter.bipwallet.share.ShareDialog
import network.minter.bipwallet.tx.ui.ExternalTransactionActivity
import network.minter.bipwallet.tx.ui.TransactionListActivity
import network.minter.bipwallet.wallets.contract.WalletsTabView
import network.minter.bipwallet.wallets.dialogs.ui.AddWalletDialog
import network.minter.bipwallet.wallets.dialogs.ui.CreateWalletDialog
import network.minter.bipwallet.wallets.dialogs.ui.EditWalletDialog
import network.minter.bipwallet.wallets.selector.WalletItem
import network.minter.bipwallet.wallets.selector.WalletListAdapter.*
import network.minter.bipwallet.wallets.views.WalletsTabPresenter
import permissions.dispatcher.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Provider

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
@RuntimePermissions
class WalletsTabFragment : HomeTabFragment(), WalletsTabView {
    companion object {
        const val REQUEST_CODE_QR_SCAN_TX = 2002
    }

    @JvmField @Inject
    var presenterProvider: Provider<WalletsTabPresenter>? = null

    @JvmField @InjectPresenter
    var presenter: WalletsTabPresenter? = null

    lateinit var binding: FragmentTabWalletsBinding
    private val mSwipeRefreshHacker = SwipeRefreshHacker()
    private var mCurrentDialog: WalletDialog? = null
    private var mBottomDialog: BaseBottomSheetDialogFragment? = null


    private lateinit var mRecolorHelper: WalletsTopRecolorHelper

    override fun onAttach(context: Context) {
        HomeModule.getComponent().inject(this)
        super.onAttach(context)
    }

    override fun setOnRefreshListener(listener: OnRefreshListener) {
        binding.containerSwipeRefresh.setOnRefreshListener(listener)
        mSwipeRefreshHacker.setOnRefreshStartListener(this::onStartRefresh)
        mSwipeRefreshHacker.hack(binding.containerSwipeRefresh)
    }

    override fun showRefreshProgress() {
        binding.containerSwipeRefresh.isRefreshing = true
    }

    override fun hideRefreshProgress() {
        binding.containerSwipeRefresh.isRefreshing = false
    }

    override fun startExplorer(hash: String) {
        activity!!.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Wallet.urlExplorerFront() + "/transactions/" + hash)))
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        presenter!!.onRestoreInstanceState(savedInstanceState)
        super.onActivityCreated(savedInstanceState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        presenter!!.onActivityResult(requestCode, resultCode, data)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = FragmentTabWalletsBinding.inflate(inflater, container, false)
        presenter!!.onRestoreInstanceState(savedInstanceState)
        mRecolorHelper = WalletsTopRecolorHelper(this)
        binding.appbar.addOnOffsetChangedListener(object : AppBarOffsetChangedListener() {
            override fun onStateChanged(appBarLayout: AppBarLayout?, state: State?, verticalOffset: Int, expandedPercent: Float) {
                binding.containerSwipeRefresh.isEnabled = expandedPercent == 1.0f
            }

        })

//        checkLastUpdate();
//        BroadcastReceiverManager bbm = new BroadcastReceiverManager(getActivity());
//        bbm.add(new RTMBlockReceiver(this::checkLastUpdate));
//        bbm.register();
        if (BuildConfig.DEBUG) {
            binding.toolbar.setOnLongClickListener {
                val sb = StringBuilder()
                sb.append("    Env: ").append(BuildConfig.FLAVOR).append("\n")
                sb.append("  Build: ").append(BuildConfig.VERSION_CODE).append("\n")
                sb.append("Version: ").append(BuildConfig.VERSION_NAME).append("\n")
                sb.append("  URole: ").append(Wallet.app().session().role.name).append("\n")
                ConfirmDialog.Builder(activity!!, "About")
                        .setText(sb.toString())
                        .setTextTypeface(Typeface.MONOSPACE)
                        .setTextIsSelectable(true)
                        .setPositiveAction("OK")
                        .create()
                        .show()
                false
            }
        }
        binding.appbar.addOnOffsetChangedListener(mRecolorHelper)
        setHasOptionsMenu(true)
        activity!!.menuInflater.inflate(R.menu.menu_wallets_toolbar, binding.toolbar.menu)
        binding.toolbar.setOnMenuItemClickListener { item: MenuItem -> onOptionsItemSelected(item) }
        setupTabAdapter()
        return binding.root
    }

    override fun onTabUnselected() {
        super.onTabUnselected()
        mRecolorHelper.setEnableRecolor(false)
    }

    override fun onTabSelected() {
        super.onTabSelected()
        mRecolorHelper.setEnableRecolor(true)
        if (activity == null) return
        mRecolorHelper.setTabSelected()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_scan_tx) {
            SingleCallHandler.call(item) { startScanQRWithPermissions(REQUEST_CODE_QR_SCAN_TX) }
        } else if (item.itemId == R.id.menu_share) {
            ShareDialog().show(fragmentManager!!, "share")
        }
        return super.onOptionsItemSelected(item)
    }

    override fun startDialog(executor: Function1<Context, WalletDialog>) {
        mCurrentDialog = switchDialogWithExecutor(this, mCurrentDialog, executor)
    }

    override fun startExternalTransaction(rawData: String) {
        ExternalTransactionActivity.Builder(activity!!, rawData)
                .start()
    }

    override fun setOnClickScanQR(listener: View.OnClickListener) {

    }

    @NeedsPermission(Manifest.permission.CAMERA)
    override fun startScanQR(requestCode: Int) {
        val i = Intent(activity, QRCodeScannerActivity::class.java)
        if (activity == null) {
            return
        }
        activity!!.startActivityForResult(i, requestCode)
    }

    override fun startWalletEdit(walletItem: WalletItem, onSubmitListener: ActionListener) {
        if (mBottomDialog != null) {
            mBottomDialog!!.dismiss()
            mBottomDialog = null
        }
        if (fragmentManager == null) {
            Timber.w("Fragment manager is NULL")
            return
        }
        mBottomDialog = EditWalletDialog.newInstance(walletItem)
        mBottomDialog!!.onSubmitListener = onSubmitListener
        mBottomDialog!!.show(fragmentManager!!, "wallet_edit")
    }

    override fun startWalletAdd(onSubmit: ActionListener, onDismiss: ActionListener?) {
        if (mBottomDialog != null) {
            mBottomDialog!!.dismiss()
            mBottomDialog = null
        }
        if (fragmentManager == null) {
            Timber.w("Fragment manager is NULL")
            return
        }
        val addWalletDialog = AddWalletDialog.newInstance()
        addWalletDialog.onSubmitListener = onSubmit
        addWalletDialog.onDismissListener = onDismiss
        addWalletDialog.setOnGenerateNewWalletListener { submitListener: ActionListener?,
                                                         dismissListener: ActionListener?,
                                                         title: String? ->

            mBottomDialog!!.dismiss()
            mBottomDialog = CreateWalletDialog.Builder()
                    .setEnableDescription(true)
                    .setEnableTitleInput(true)
                    .setWalletTitle(title)
                    .setOnSubmitListener(submitListener)
                    .setOnDismissListener(dismissListener)
                    .setEnableStartHomeOnSubmit(false)
                    .build()
            mBottomDialog!!.show(fragmentManager!!, "wallet_generate")
        }
        mBottomDialog = addWalletDialog
        mBottomDialog!!.show(fragmentManager!!, "wallet_add")
    }

    override fun showSendAndSetAddress(address: String) {
        runOnUiThread {
            try {
                (activity as HomeActivity?)!!.setCurrentPage(1)
                ((activity as HomeActivity?)!!.currentTabFragment as SendTabFragment).setRecipient(AddressContact(address))
            } catch (t: Throwable) {
                Timber.w("Unable to scan address directly to send tab")
            }
        }
    }

    override fun setMainWallet(mainWallet: WalletItem) {
        binding.walletSelector.setMainWallet(mainWallet)
    }

    override fun setWallets(addresses: List<WalletItem>) {
        binding.walletSelector.setWallets(addresses)
    }

    override fun setOnClickWalletListener(listener: OnClickWalletListener) {
        binding.walletSelector.setOnClickWalletListener(listener)
    }

    override fun setOnClickAddWalletListener(listener: OnClickAddWalletListener) {
        binding.walletSelector.setOnClickAddWalletListener(listener)
    }

    override fun setOnClickEditWalletListener(listener: OnClickEditWalletListener) {
        binding.walletSelector.setOnClickEditWalletListener(listener)
    }

    override fun setOnClickDelegated(listener: View.OnClickListener) {
        binding.delegatedLayout.setOnClickListener(listener)
    }

    override fun startScanQRWithPermissions(requestCode: Int) {
        startScanQRWithPermissionCheck(requestCode)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        HomeModule.getComponent().inject(this)
        super.onCreate(savedInstanceState)
        presenter!!.onRestoreInstanceState(savedInstanceState)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        presenter!!.onTrimMemory()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        presenter!!.onLowMemory()
        Timber.d("OnLowMemory")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Timber.d("Destroy")
    }

    @SuppressLint("SetTextI18n")
    override fun setBalance(firstPart: CharSequence?, middlePart: CharSequence?, lastPart: CharSequence?) {
        binding.balanceFirstPart.post {
            binding.apply {
                balanceFirstPart.text = firstPart
                balanceMiddlePart.text = ".$middlePart"
                balanceLastPart.text = lastPart
            }
        }
    }

    override fun setDelegationAmount(amount: String) {
        binding.delegatedBalance.text = amount
    }

    override fun setBalanceClickListener(listener: View.OnClickListener) {
        binding.balanceContainer.setOnClickListener(listener)
    }

    override fun setBalanceTitle(title: Int) {
        binding.balanceLabel.setText(title)
    }

    override fun setBalanceRewards(rewards: String) {
        binding.balanceToday.text = rewards
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        presenter!!.onSaveInstanceState(outState)
    }

    override fun startTransactionList() {
        startActivity(Intent(activity, TransactionListActivity::class.java))
    }

    override fun startDelegationList() {
        startActivity(Intent(activity, DelegationListActivity::class.java))
    }

    override fun startConvertCoins() {
        activity!!.startActivity(Intent(activity, ConvertCoinActivity::class.java))
    }

    override fun startTab(@IdRes tab: Int) {
        if (activity is HomeActivity) {
            (activity as HomeActivity?)!!.setCurrentPageById(tab)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onRequestPermissionsResult(requestCode, grantResults)
    }

    @ProvidePresenter
    fun providePresenter(): WalletsTabPresenter {
        return presenterProvider!!.get()
    }

    @OnShowRationale(Manifest.permission.CAMERA)
    fun showRationaleForCamera(request: PermissionRequest) {
        ConfirmDialog.Builder(activity!!, "Camera request")
                .setText("We need access to your camera to take a shot with Minter Address QR Code")
                .setPositiveAction("Sure") { d, _: Int ->
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
                .setNegativeAction("Cancel")
                .create()
                .show()
    }

    @SuppressLint("ClickableViewAccessibility")
    @Suppress("DEPRECATION")
    private fun setupTabAdapter() {
        binding.tabsPager.adapter = object : FragmentStatePagerAdapter(activity!!.supportFragmentManager) {
            override fun getCount(): Int {
                return 2
            }

            override fun getItem(position: Int): Fragment {
                return when (position) {
                    0 -> {
                        CoinsTabPageFragment()
                    }
                    1 -> {
                        TxsTabPageFragment()
                    }
                    else -> throw IllegalStateException("Unknown tab position: $position")
                }
            }
        }
//
//
        binding.tabsPager.offscreenPageLimit = 2
        binding.tabs.setupWithViewPager(binding.tabsPager)
        binding.tabs.getTabAt(0)!!.setText(R.string.tab_page_coins)
        binding.tabs.getTabAt(1)!!.setText(R.string.tab_page_txs)
    }

    private fun checkLastUpdate() {
//        if (!Wallet.app().settings().has(LastBlockTime)) {
//            lastUpdateText.setText(HtmlCompat.fromHtml(getString(R.string.balance_last_updated_never)));
//            return;
//        }
//        DateTime lastBlockTime = new DateTime((long) Wallet.app().settings().get(LastBlockTime));
//        Seconds diff = Seconds.secondsBetween(lastBlockTime, new DateTime());
//        int res = diff.getSeconds();
//        Timber.d("Diff: now=%s, ts=%s", new DateTime().toString(), lastBlockTime.toString());
//        lastUpdateText.setText(HtmlCompat.fromHtml(getString(R.string.balance_last_updated, Plurals.timeValue((long) res), Plurals.time((long) res))));
    }

    private fun onStartRefresh() {
        Wallet.app().sounds().play(R.raw.refresh_pop_down)
    }
}