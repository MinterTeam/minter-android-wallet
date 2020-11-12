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
import android.animation.Animator
import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import androidx.viewpager.widget.PagerAdapter
import com.google.android.material.appbar.AppBarLayout
import moxy.presenter.InjectPresenter
import moxy.presenter.ProvidePresenter
import network.minter.bipwallet.BuildConfig
import network.minter.bipwallet.R
import network.minter.bipwallet.addressbook.models.AddressContact
import network.minter.bipwallet.databinding.FragmentTabWalletsBinding
import network.minter.bipwallet.delegation.ui.DelegateUnbondActivity
import network.minter.bipwallet.delegation.ui.DelegatedListActivity
import network.minter.bipwallet.exchange.ui.ConvertCoinActivity
import network.minter.bipwallet.home.HomeModule
import network.minter.bipwallet.home.HomeTabFragment
import network.minter.bipwallet.home.ui.HomeActivity
import network.minter.bipwallet.internal.Wallet
import network.minter.bipwallet.internal.dialogs.ConfirmDialog
import network.minter.bipwallet.internal.helpers.ViewExtensions.postApply
import network.minter.bipwallet.internal.helpers.ViewExtensions.visible
import network.minter.bipwallet.internal.system.BroadcastReceiverManager
import network.minter.bipwallet.internal.views.utils.SingleCallHandler
import network.minter.bipwallet.sending.ui.QRCodeScannerActivity
import network.minter.bipwallet.sending.ui.SendTabFragment
import network.minter.bipwallet.services.livebalance.broadcast.RTMBlockReceiver
import network.minter.bipwallet.share.ShareDialog
import network.minter.bipwallet.stories.StoriesStateReceiver
import network.minter.bipwallet.stories.models.Story
import network.minter.bipwallet.stories.ui.StoriesListFragment
import network.minter.bipwallet.tx.ui.ExternalTransactionActivity
import network.minter.bipwallet.tx.ui.TransactionListActivity
import network.minter.bipwallet.wallets.contract.WalletsTabView
import network.minter.bipwallet.wallets.selector.WalletItem
import network.minter.bipwallet.wallets.selector.WalletListAdapter.*
import network.minter.bipwallet.wallets.selector.WalletSelectorBroadcastReceiver
import network.minter.bipwallet.wallets.utils.LastBlockHandler
import network.minter.bipwallet.wallets.views.WalletsTabPresenter
import network.minter.core.crypto.MinterPublicKey
import org.joda.time.DateTime
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

    @Inject lateinit var presenterProvider: Provider<WalletsTabPresenter>
    @InjectPresenter lateinit var presenter: WalletsTabPresenter

    private val swipeRefreshHacker = SwipeRefreshHacker()
    private lateinit var mRecolorHelper: WalletsTopRecolorHelper
    lateinit var binding: FragmentTabWalletsBinding
    private var storiesListFragment: StoriesListFragment? = null
    private var storiesListLock = Any()

    override fun onAttach(context: Context) {
        HomeModule.component!!.inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        HomeModule.component!!.inject(this)
        super.onCreate(savedInstanceState)
        presenter.onRestoreInstanceState(savedInstanceState)
    }

    override fun setOnRefreshListener(listener: OnRefreshListener) {
        binding.containerSwipeRefresh.setOnRefreshListener(listener)
        swipeRefreshHacker.setOnRefreshStartListener(this::onStartRefresh)
        swipeRefreshHacker.hack(binding.containerSwipeRefresh)
    }

    override fun showProgress() {
        showRefreshProgress()
    }

    override fun hideProgress() {
        hideRefreshProgress()
        showBalanceProgress(false)
    }

    override fun showRefreshProgress() {
        binding.containerSwipeRefresh.postApply {
            it.isRefreshing = true
        }
    }

    override fun hideRefreshProgress() {
        binding.containerSwipeRefresh.postApply {
            it.isRefreshing = false
        }
    }

    override fun startExplorer(hash: String) {
        activity!!.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Wallet.urlExplorerFront() + "/transactions/" + hash)))
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        presenter.onRestoreInstanceState(savedInstanceState)
        super.onActivityCreated(savedInstanceState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        presenter.onActivityResult(requestCode, resultCode, data)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = FragmentTabWalletsBinding.inflate(inflater, container, false)
        presenter.onRestoreInstanceState(savedInstanceState)
        mRecolorHelper = WalletsTopRecolorHelper(this)
        binding.appbar.addOnOffsetChangedListener(object : AppBarOffsetChangedListener() {
            override fun onStateChanged(appBarLayout: AppBarLayout?, state: State?, verticalOffset: Int, expandedPercent: Float) {
                binding.containerSwipeRefresh.isEnabled = expandedPercent == 1.0f
            }
        })
        binding.walletSelector.registerLifecycle(activity!!)
//        CollapsingToolbarScrollDisabler(binding)

        LastBlockHandler.handle(binding.balanceUpdatedLabel, null, LastBlockHandler.ViewType.Main)
        val broadcastManager = BroadcastReceiverManager(activity!!)
        broadcastManager.add(RTMBlockReceiver {
            LastBlockHandler.handle(binding.balanceUpdatedLabel, it, LastBlockHandler.ViewType.Main)
        })
        broadcastManager.add(StoriesStateReceiver {
            presenter.setShowStories(it)
        })
        broadcastManager.register()

        binding.appbar.addOnOffsetChangedListener(mRecolorHelper)
        setHasOptionsMenu(true)
        activity!!.menuInflater.inflate(R.menu.menu_wallets_toolbar, binding.toolbar.menu)
        binding.toolbar.setOnMenuItemClickListener { item: MenuItem -> onOptionsItemSelected(item) }
        setupTabAdapter()
        return binding.root
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

    override fun notifyUpdated() {
        RTMBlockReceiver.send(activity!!, DateTime().plusSeconds(Wallet.timeOffset()))
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

    override fun setMainWallet(walletItem: WalletItem) {
        activity?.let {
            WalletSelectorBroadcastReceiver.setMainWallet(activity!!, walletItem)
        }
    }

    override fun setWallets(walletItems: List<WalletItem>) {
        activity?.let {
            WalletSelectorBroadcastReceiver.setWallets(activity!!, walletItems)
        }
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

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        presenter.onTrimMemory()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        presenter.onLowMemory()
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
                balanceMiddlePart.text = ".$middlePart "
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
        presenter.onSaveInstanceState(outState)
    }

    override fun startTransactionList() {
        startActivity(Intent(activity, TransactionListActivity::class.java))
    }

    override fun startDelegationList() {
        startActivity(Intent(activity, DelegatedListActivity::class.java))
    }

    override fun startDelegate(publicKey: MinterPublicKey) {
        DelegateUnbondActivity.Builder(this, DelegateUnbondActivity.Type.Delegate)
                .setPublicKey(publicKey)
                .start()
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
        return presenterProvider.get()
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
                .setText("We need access to your camera to take a shot with Minter QR Code")
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

    private val pagerAdapter: PagerAdapter by lazy {
        object : FragmentPagerAdapter(childFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
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
    }

    private fun setupTabAdapter() {
        binding.tabsPager.adapter = pagerAdapter
        binding.tabsPager.offscreenPageLimit = 2
        binding.tabs.setupWithViewPager(binding.tabsPager)
        binding.tabs.getTabAt(0)!!.setText(R.string.tab_page_coins)
        binding.tabs.getTabAt(1)!!.setText(R.string.tab_page_txs)
    }

    private fun onStartRefresh() {
        Wallet.app().sounds().play(R.raw.refresh_pop_down)
    }

    override fun showStoriesList(stories: List<Story>, smoothScroll: Boolean) {
        synchronized(storiesListLock) {
            if (storiesListFragment == null) {
                storiesListFragment = StoriesListFragment.newInstance(stories)

                runOnUiThread {
                    Timber.d("Add stories fragment")
                    childFragmentManager.beginTransaction()
                            .add(R.id.fragment_stories, storiesListFragment!!, "stories_list")
                            .commit()
                }
            } else {
                runOnUiThread {
                    storiesListFragment!!.setData(stories, smoothScroll)
                }
            }
        }
    }

    override fun hideStoriesList() {
        if (storiesListFragment == null) {
            return
        }

        childFragmentManager.beginTransaction()
                .remove(storiesListFragment!!)
                .commit()

        storiesListFragment = null
    }

    override fun setStoriesListData(items: List<Story>) {
        storiesListFragment?.setData(items)
    }

    fun startStoriesPager(stories: List<Story>, startPosition: Int, sharedImage: View) {
        (activity as HomeActivity?)?.startStoriesPager(stories, startPosition, sharedImage)

    }

    fun closeStoriesPager() {
        (activity as HomeActivity?)?.closeStoriesPager()
    }
}