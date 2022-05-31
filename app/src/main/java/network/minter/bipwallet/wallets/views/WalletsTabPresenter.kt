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
package network.minter.bipwallet.wallets.views

import android.content.Context
import android.os.Bundle
import android.view.View
import io.reactivex.disposables.Disposable
import moxy.InjectViewState
import network.minter.bipwallet.R
import network.minter.bipwallet.apis.explorer.RepoTransactions
import network.minter.bipwallet.home.HomeScope
import network.minter.bipwallet.internal.dialogs.ConfirmDialog
import network.minter.bipwallet.internal.exceptions.ErrorManager
import network.minter.bipwallet.internal.helpers.MathHelper.bdIntHuman
import network.minter.bipwallet.internal.helpers.MathHelper.humanize
import network.minter.bipwallet.internal.helpers.Plurals
import network.minter.bipwallet.internal.helpers.ViewExtensions.tr
import network.minter.bipwallet.internal.mvp.MvpBasePresenter
import network.minter.bipwallet.internal.settings.EnableStories
import network.minter.bipwallet.internal.settings.SettingsManager
import network.minter.bipwallet.internal.storage.RepoAccounts
import network.minter.bipwallet.internal.storage.SecretStorage
import network.minter.bipwallet.internal.storage.models.AddressListBalancesTotal
import network.minter.bipwallet.stories.repo.RepoCachedStories
import network.minter.bipwallet.wallets.contract.WalletsTabView
import network.minter.bipwallet.wallets.data.BalanceCurrentState
import network.minter.core.MinterSDK
import network.minter.core.crypto.MinterAddress
import network.minter.core.crypto.MinterPublicKey
import network.minter.core.internal.helpers.StringHelper.splitDecimalStringFractions
import network.minter.explorer.models.RewardStatistics
import network.minter.explorer.repo.ExplorerAddressRepository
import timber.log.Timber
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
@HomeScope
@InjectViewState
class WalletsTabPresenter @Inject constructor() : MvpBasePresenter<WalletsTabView>(),
    ErrorManager.ErrorGlobalReceiverListener {
    @Inject
    lateinit var accountStorage: RepoAccounts
    @Inject
    lateinit var addressRepo: ExplorerAddressRepository
    @Inject
    lateinit var secretStorage: SecretStorage
    @Inject
    lateinit var txRepo: RepoTransactions
    @Inject
    lateinit var walletSelectorController: WalletSelectorController
    @Inject
    lateinit var errorManager: ErrorManager
    @Inject
    lateinit var storiesRepository: RepoCachedStories
    @Inject
    lateinit var settings: SettingsManager

    private val balanceState = BalanceCurrentState()
    private var storiesDisposable: Disposable? = null
    private var smoothScrollStories = false

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()
        walletSelectorController.onFirstViewAttach(viewState)
        walletSelectorController.onWalletSelected = {
            viewState.showBalanceProgress(true)
        }

        if (accountStorage.isDataReady) {
            try {
                Timber.d("BALANCE ready from cache")
                onBalanceReady(accountStorage.data)
            } catch (t: Throwable) {
            }
        }

        accountStorage
            .observe()
            .joinToUi()
            .subscribe(
                { res: AddressListBalancesTotal ->
                    Timber.d("Update coins list")
                    viewState.notifyUpdated()
                    onBalanceReady(res)
                    viewState.hideRefreshProgress()
                    viewState.showBalanceProgress(false)
                },
                {
                    Timber.e(it, "Unable to get balance for Wallets tab")
                    viewState.hideProgress()
                    viewState.showBalanceProgress(false)
                }
            ).disposeOnDestroy()

        accountStorage.update()
    }

    override fun onError(t: Throwable) {
        viewState.hideProgress()
        viewState.showBalanceProgress(false)
    }

    private fun initStories() {
        if (settings[EnableStories]) {
            if (storiesDisposable == null) {
                storiesDisposable = storiesRepository.observe()
                    .joinToUi()
                    .subscribe({
                        if (it.isNotEmpty()) {
                            Timber.d("Show stories")
                            viewState.showStoriesList(it, smoothScrollStories)
                        }
                    }, { t ->
                        Timber.w(t, "Unable to load stories")
                    })
            }

            storiesRepository.update()
        } else {
            storiesDisposable?.dispose()
            storiesDisposable = null
            viewState.hideStoriesList()
        }
    }

    override fun attachView(view: WalletsTabView) {
        walletSelectorController.attachView(view)
        super.attachView(view)
        errorManager.subscribe(this)

        initStories()

        viewState.setOnClickDelegated {
            onClickStartDelegationList(it)
        }

        viewState.setOnRefreshListener {
            onRefresh()
            errorManager.retryListener()
        }
        balanceState.applyTo(viewState)
    }

    override fun detachView(view: WalletsTabView) {
        super.detachView(view)
        walletSelectorController.detachView()
        errorManager.unsubscribe(this)
    }

    private fun onBalanceReady(res: AddressListBalancesTotal) {
        val mainWallet = accountStorage.entity.mainWallet

        val availableBIP = splitDecimalStringFractions(mainWallet.stakeBalanceBIP.setScale(4, RoundingMode.DOWN))
        val totalBIP = splitDecimalStringFractions(mainWallet.totalBalance.setScale(4, RoundingMode.DOWN))
        val totalUSD = splitDecimalStringFractions(mainWallet.totalBalanceUSD.setScale(2, RoundingMode.DOWN))

        balanceState.setAvailableBIP(
            bdIntHuman(availableBIP.intPart),
            availableBIP.fractionalPart,
            MinterSDK.DEFAULT_COIN
        )

        balanceState.setTotalBIP(
            bdIntHuman(totalBIP.intPart),
            totalBIP.fractionalPart,
            MinterSDK.DEFAULT_COIN
        )
        balanceState.setTotalUSD(
            Plurals.usd(bdIntHuman(totalUSD.intPart)),
            totalUSD.fractionalPart
        )
        balanceState.applyTo(viewState)


        // show delegated
        res.find(secretStorage.mainWallet)?.let {
            viewState.setDelegationAmount("${it.delegated.humanize()} ${MinterSDK.DEFAULT_COIN}")
        } ?: kotlin.run {
            viewState.setDelegationAmount("${BigDecimal.ZERO.humanize()} ${MinterSDK.DEFAULT_COIN}")
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        balanceState.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        balanceState.onRestoreInstanceState(savedInstanceState)
    }

    fun handleQRResult(result: String?) {
        result?.let {
            val isMxAddress = it.matches(MinterAddress.ADDRESS_PATTERN.toRegex())
            val isMpAddress = it.matches(MinterPublicKey.PUB_KEY_PATTERN.toRegex())
            if (isMxAddress) {
                viewState.showSendAndSetAddress(it)
                return
            } else if (isMpAddress) {
                viewState.startDelegate(MinterPublicKey(it))
                return
            }
            try {
                viewState.startExternalTransaction(it)
            } catch (t: Throwable) {
                Timber.w(t, "Unable to parse remote transaction: %s", it)
                viewState.startDialog { ctx: Context ->
                    ConfirmDialog.Builder(ctx, R.string.dialog_title_err_unable_scan_qr)
                        .setText(tr(R.string.dialog_title_err_invalid_deeplink, t.message!!))
                        .setPositiveAction(R.string.btn_close)
                        .create()
                }
            }
        }
    }

    private fun onDailyRewardsReady(res: RewardStatistics) {
        viewState.setBalanceRewards("+ ${res.amount.humanize()} ${MinterSDK.DEFAULT_COIN} today")
    }

    private fun forceUpdate() {
        accountStorage.update(true)
//        dailyRewardsRepo.update(true)
        if (settings[EnableStories]) {
            storiesRepository.update(true)
        }
        txRepo.update(true)
    }

    private fun onRefresh() {
        viewState.showBalanceProgress(true)
        smoothScrollStories = true
        forceUpdate()
        smoothScrollStories = false
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onClickStartDelegationList(view: View) {
        viewState.startDelegationList()
    }

    @Suppress("UNUSED_PARAMETER")
    fun setShowStories(enabled: Boolean) {
        initStories()
    }
}