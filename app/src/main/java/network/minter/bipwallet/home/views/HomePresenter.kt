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
package network.minter.bipwallet.home.views

import android.content.Intent
import com.airbnb.deeplinkdispatch.DeepLink
import moxy.InjectViewState
import network.minter.bipwallet.R
import network.minter.bipwallet.analytics.AppEvent
import network.minter.bipwallet.home.HomeTabFragment
import network.minter.bipwallet.home.HomeTabsClasses
import network.minter.bipwallet.home.contract.HomeView
import network.minter.bipwallet.internal.Wallet
import network.minter.bipwallet.internal.data.CachedRepository
import network.minter.bipwallet.internal.mvp.MvpBasePresenter
import network.minter.bipwallet.internal.storage.AccountStorage
import network.minter.bipwallet.internal.storage.KVStorage
import network.minter.bipwallet.internal.storage.models.AddressListBalancesTotal
import network.minter.bipwallet.services.livebalance.RTMService
import network.minter.bipwallet.services.livebalance.ServiceConnector
import network.minter.bipwallet.services.livebalance.broadcast.RTMBalanceUpdateReceiver
import network.minter.bipwallet.services.livebalance.broadcast.RTMBlockReceiver.Companion.send
import network.minter.core.crypto.MinterAddress
import timber.log.Timber
import java.util.*
import javax.inject.Inject

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock></edward.vstock>@gmail.com>
 */
@InjectViewState
class HomePresenter @Inject constructor() : MvpBasePresenter<HomeView>() {
    private val mBottomIdPositionMap: HashMap<Int, Int> = object : HashMap<Int, Int>() {
        init {
            put(R.id.bottom_wallets, 0)
            put(R.id.bottom_send, 1)
            put(R.id.bottom_settings, 2)
        }
    }

    @Inject lateinit var storage: KVStorage
    @Inject @HomeTabsClasses lateinit var tabsClasses: @JvmSuppressWildcards List<Class<out HomeTabFragment>>

    @Inject
    lateinit var accountStorage: CachedRepository<AddressListBalancesTotal, AccountStorage>

    private var mLastPosition = 0

    override fun attachView(view: HomeView) {
        super.attachView(view)
        viewState!!.setCurrentPage(mLastPosition)
    }

    override fun onDestroy() {
        super.onDestroy()
        ServiceConnector.release(Wallet.app().context())
    }

    fun onPageSelected(position: Int) {
        mLastPosition = position
        when (position) {
            R.id.bottom_wallets -> analytics.send(AppEvent.WalletsSreen)
            R.id.bottom_send -> analytics.send(AppEvent.SendScreen)
            R.id.bottom_settings -> analytics.send(AppEvent.SettingsScreen)
        }
    }

    override fun handleExtras(intent: Intent?) {
        super.handleExtras(intent)
        if (intent!!.getBooleanExtra(DeepLink.IS_DEEP_LINK, false)) {
            val params = intent.extras ?: return
            val uri = params.getString(DeepLink.URI, null) ?: return
            if (uri.startsWith("minter://tx")) {
                viewState!!.setCurrentPage(mBottomIdPositionMap[R.id.bottom_send]!!)
                val hash = params.getString("d", null)
                Timber.d("Deeplink URI: %s", uri)
                Timber.d("Deeplink TX: %s", hash)
                viewState!!.startRemoteTransaction(hash)
            }
        }
    }

    fun getBottomIdByPosition(position: Int): Int {
        for ((key, value) in mBottomIdPositionMap) {
            if (value == position) {
                return key
            }
        }
        return 0
    }

    fun getTabPosition(name: String): Int {
        var position = 0
        for (cls in tabsClasses) {
            if (cls.name == name) {
                break
            }
            position++
        }
        return position
    }

    fun getBottomPositionById(itemId: Int): Int {
        return mBottomIdPositionMap[itemId]!!
    }

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()
        ServiceConnector.bind(Wallet.app().context())
        ServiceConnector.onConnected()
                .subscribe(
                        { res: RTMService ->
                            res.setOnMessageListener { message: String?, channel: String, address: MinterAddress? ->
                                if (channel == RTMService.CHANNEL_BLOCKS) {
                                    send(Wallet.app().context(), message!!)
                                } else {
                                    RTMBalanceUpdateReceiver.send(Wallet.app().context(), message)
                                    accountStorage.update(true, {
                                        Wallet.app().balanceNotifications().showBalanceUpdate(message, address)
                                    })
                                    Wallet.app().explorerTransactionsRepoCache().update(true)
                                    Timber.d("WS ON MESSAGE[%s]: %s", channel, message)
                                }
                            }
                        }
                ) { t: Throwable? -> Timber.w(t, "Unable to connect to RTM service") }
    }
}