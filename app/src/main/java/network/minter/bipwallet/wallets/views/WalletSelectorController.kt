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

package network.minter.bipwallet.wallets.views

import io.reactivex.disposables.Disposable
import network.minter.bipwallet.apis.explorer.RepoTransactions
import network.minter.bipwallet.internal.storage.RepoAccounts
import network.minter.bipwallet.internal.storage.SecretStorage
import network.minter.bipwallet.internal.storage.models.AddressListBalancesTotal
import network.minter.bipwallet.wallets.contract.WalletSelectorControllerView
import network.minter.bipwallet.wallets.selector.WalletItem
import network.minter.bipwallet.wallets.selector.WalletListAdapter
import timber.log.Timber
import javax.inject.Inject

class WalletSelectorController @Inject constructor() {
    @Inject lateinit var secretStorage: SecretStorage
    @Inject lateinit var accountStorage: RepoAccounts
    @Inject lateinit var txRepo: RepoTransactions
//    @Inject lateinit var dailyRewardsRepo: RepoDailyRewards

    var onWalletSelected: ((WalletItem) -> Unit)? = null

    private var viewState: WalletSelectorControllerView? = null
    private var disposable: Disposable? = null
    private var latestBalances: AddressListBalancesTotal? = null

    fun onFirstViewAttach() {
        fillWalletSelector(accountStorage.data)
    }

    fun attachView(view: WalletSelectorControllerView) {
        viewState = view

        disposable = accountStorage.observe()
                .subscribe(
                        {
                            fillWalletSelector(it)
                        },
                        {
                            Timber.w(it)
                        }
                )

        viewState!!.setOnClickWalletListener(WalletListAdapter.OnClickWalletListener { walletItem: WalletItem ->
            onSelectWallet(walletItem)
        })
        viewState!!.setOnClickAddWalletListener(WalletListAdapter.OnClickAddWalletListener {
            onClickWalletAdd()
        })
        viewState!!.setOnClickEditWalletListener(WalletListAdapter.OnClickEditWalletListener { walletItem: WalletItem ->
            onClickWalletEdit(walletItem)
        })
    }

    private fun fillWalletSelector(res: AddressListBalancesTotal) {
        latestBalances = res
        viewState!!.setWallets(WalletItem.create(secretStorage, res))
        viewState!!.setMainWallet(WalletItem.create(secretStorage, res.getBalance(secretStorage.mainWallet)))
    }

    private fun onClickWalletAdd() {
        viewState!!.startWalletAdd({ onAddedWallet(it) }, null)
    }

    private fun onAddedWallet(walletItem: WalletItem) {
        onWalletSelected?.invoke(walletItem)
        viewState?.setMainWallet(walletItem)
        fillWalletSelector(accountStorage.data)
        accountStorage.update(true)
        txRepo.update(true)
//        dailyRewardsRepo.update(true)
    }

    private fun onSelectWallet(walletItem: WalletItem) {
        secretStorage.setMain(walletItem.address)
        viewState!!.setMainWallet(walletItem)
        viewState!!.setWallets(WalletItem.create(secretStorage, accountStorage.data))

        accountStorage.update(true)
        txRepo.update(true)
//        dailyRewardsRepo.update(true)

        onWalletSelected?.invoke(walletItem)
    }

    private fun onClickWalletEdit(walletItem: WalletItem) {
        viewState!!.startWalletEdit(walletItem, secretStorage.addresses.size > 1,
                {
                    onWalletUpdated(it)
                },
                {
                    onWalletDeleted(it)
                }
        )
    }

    private fun onWalletDeleted(walletItem: WalletItem) {
        accountStorage.entity.remove(walletItem.address)
        fillWalletSelector(accountStorage.data)
        accountStorage.update(true)
        txRepo.update(true)
//        dailyRewardsRepo.update(true)

    }

    @Suppress("UNUSED_PARAMETER")
    private fun onWalletUpdated(walletItem: WalletItem) {
        fillWalletSelector(accountStorage.data)
        accountStorage.update(true)
    }

    fun detachView() {
        disposable?.dispose()
        disposable = null
        viewState = null
    }

}