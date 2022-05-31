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
package network.minter.bipwallet.wallets.ui

import android.animation.Animator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import network.minter.bipwallet.internal.helpers.ViewExtensions.postApply
import network.minter.bipwallet.internal.helpers.ViewExtensions.visible
import network.minter.bipwallet.internal.system.BroadcastReceiverManager
import network.minter.bipwallet.internal.views.utils.SingleCallHandler
import network.minter.bipwallet.sending.contract.QRLauncher
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
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Provider

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */

class WalletsTabFragment : HomeTabFragment(), WalletsTabView {
    companion object {
        const val REQUEST_CODE_QR_SCAN_TX = 2002
    }

    @Inject
    lateinit var presenterProvider: Provider<WalletsTabPresenter>
    @InjectPresenter
    lateinit var presenter: WalletsTabPresenter

    private val swipeRefreshHacker = SwipeRefreshHacker()
    private lateinit var mRecolorHelper: WalletsTopRecolorHelper
    lateinit var binding: FragmentTabWalletsBinding
    private var storiesListFragment: StoriesListFragment? = null
    private var storiesListLock = Any()

    private val qrLauncher = QRLauncher(this, { requireActivity() }) {
        presenter.handleQRResult(it)
    }

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
        requireActivity().startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(Wallet.urlExplorerFront() + "/transactions/" + hash)
            )
        )
    }

//    override fun onActivityCreated(savedInstanceState: Bundle?) {
//        presenter.onRestoreInstanceState(savedInstanceState)
//        super.onActivityCreated(savedInstanceState)
//    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentTabWalletsBinding.inflate(inflater, container, false)
        presenter.onRestoreInstanceState(savedInstanceState)
        mRecolorHelper = WalletsTopRecolorHelper(this)
        binding.appbar.addOnOffsetChangedListener(object : AppBarOffsetChangedListener() {
            override fun onStateChanged(
                appBarLayout: AppBarLayout?,
                state: State?,
                verticalOffset: Int,
                expandedPercent: Float
            ) {
                binding.containerSwipeRefresh.isEnabled = expandedPercent == 1.0f
            }
        })
        binding.walletSelector.registerLifecycle(requireActivity())
//        CollapsingToolbarScrollDisabler(binding)

        LastBlockHandler.handle(binding.balanceUpdatedLabel, null, LastBlockHandler.ViewType.Main)
        val broadcastManager = BroadcastReceiverManager(requireActivity())
        broadcastManager.add(RTMBlockReceiver {
            LastBlockHandler.handle(binding.balanceUpdatedLabel, it, LastBlockHandler.ViewType.Main)
        })
        broadcastManager.add(StoriesStateReceiver {
            presenter.setShowStories(it)
        })
        broadcastManager.register()

        binding.appbar.addOnOffsetChangedListener(mRecolorHelper)
        setHasOptionsMenu(true)
        requireActivity().menuInflater.inflate(R.menu.menu_wallets_toolbar, binding.toolbar.menu)
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
        RTMBlockReceiver.send(requireActivity(), DateTime().plusSeconds(Wallet.timeOffset()))
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
            SingleCallHandler.call(item) { startScanQR() }
        } else if (item.itemId == R.id.menu_share) {
            ShareDialog().show(parentFragmentManager, "share")
        }
        return super.onOptionsItemSelected(item)
    }

    override fun startExternalTransaction(rawData: String) {
        ExternalTransactionActivity.Builder(requireActivity(), rawData)
            .start()
    }

    override fun setOnClickScanQR(listener: View.OnClickListener) {

    }

    override fun startScanQR() {
        qrLauncher.launch()
    }

    override fun showSendAndSetAddress(address: String) {
        runOnUiThread {
            (activity as? HomeActivity)?.setCurrentPage(1)
            ((activity as? HomeActivity)?.currentTabFragment as? SendTabFragment)?.setRecipient(AddressContact(address))
        }
    }

    override fun setMainWallet(walletItem: WalletItem) {
        activity?.let {
            WalletSelectorBroadcastReceiver.setMainWallet(requireActivity(), walletItem)
        }
    }

    override fun setWallets(walletItems: List<WalletItem>) {
        activity?.let {
            WalletSelectorBroadcastReceiver.setWallets(requireActivity(), walletItems)
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
        requireActivity().startActivity(Intent(activity, ConvertCoinActivity::class.java))
    }

    override fun startTab(@IdRes tab: Int) {
        if (activity is HomeActivity) {
            (activity as HomeActivity?)!!.setCurrentPageById(tab)
        }
    }

    @ProvidePresenter
    fun providePresenter(): WalletsTabPresenter {
        return presenterProvider.get()
    }

    private val pagerAdapter: PagerAdapter by lazy(LazyThreadSafetyMode.NONE) {
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
                    try {
                        childFragmentManager.beginTransaction()
                            .add(R.id.fragment_stories, storiesListFragment!!, "stories_list")
                            .commit()
                    } catch (illegal: java.lang.IllegalStateException) {
                        Timber.w(illegal, "Unable to show stories fragment")
                    }

                }
            } else {
                runOnUiThread {
                    storiesListFragment!!.setData(stories, smoothScroll)
                }
            }
        }
    }

    override fun hideStoriesList() {
        synchronized(storiesListLock) {
            if (storiesListFragment == null) {
                return
            }

            childFragmentManager.beginTransaction()
                .remove(storiesListFragment!!)
                .commit()

            storiesListFragment = null
        }
    }

    override fun setStoriesListData(items: List<Story>) {
        synchronized(storiesListLock) {
            storiesListFragment?.setData(items)
        }
    }

    fun startStoriesPager(stories: List<Story>, startPosition: Int, sharedImage: View) {
        (activity as HomeActivity?)?.startStoriesPager(stories, startPosition, sharedImage)

    }

    fun closeStoriesPager() {
        (activity as HomeActivity?)?.closeStoriesPager()
    }
}
