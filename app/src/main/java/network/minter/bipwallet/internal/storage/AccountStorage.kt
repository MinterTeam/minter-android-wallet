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
package network.minter.bipwallet.internal.storage

import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.functions.Function
import io.reactivex.schedulers.Schedulers
import network.minter.bipwallet.BuildConfig
import network.minter.bipwallet.apis.reactive.ReactiveExplorer.rxExp
import network.minter.bipwallet.internal.data.CachedEntity
import network.minter.bipwallet.internal.data.CachedRepository
import network.minter.bipwallet.internal.storage.models.AddressBalanceTotal
import network.minter.bipwallet.internal.storage.models.AddressListBalancesTotal
import network.minter.core.crypto.MinterAddress
import network.minter.explorer.models.AddressBalance
import network.minter.explorer.models.DelegationList
import network.minter.explorer.models.ExpResult
import network.minter.explorer.repo.ExplorerAddressRepository
import timber.log.Timber

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
typealias RepoAccounts = CachedRepository<AddressListBalancesTotal, AccountStorage>

class AccountStorage(
        private val storage: KVStorage,
        private val secretStorage: SecretStorage,
        private val addressRepo: ExplorerAddressRepository
) : CachedEntity<AddressListBalancesTotal> {

    companion object {
        private const val KEY_BALANCE = BuildConfig.MINTER_STORAGE_VERS + "account_storage_balance_"
    }

    override fun getData(): AddressListBalancesTotal {
        return storage[KEY_BALANCE, AddressListBalancesTotal(secretStorage.addresses)]
    }

    val mainWallet: AddressBalanceTotal
        get() = getData().getBalance(secretStorage.mainWallet)

    val wallets: AddressListBalancesTotal
        get() = getData()

    override fun onAfterUpdate(result: AddressListBalancesTotal) {
        storage.putAsync(KEY_BALANCE, result)
    }

    override fun onClear() {
        storage.delete(KEY_BALANCE)
    }

    override fun isDataReady(): Boolean {
        return storage.contains(KEY_BALANCE)
    }

    override fun getUpdatableData(): Observable<AddressListBalancesTotal> {
        return Observable
                .fromIterable(secretStorage.addresses)
                .flatMap(mapBalances())
                .toList()
                .map(aggregate())
                .subscribeOn(Schedulers.io())
                .toObservable()
    }

    private fun mapBalances(): Function<MinterAddress, ObservableSource<ExpResult<AddressBalanceTotal>>> {
        return Function { address: MinterAddress? ->
            rxExp(addressRepo.getAddressData(address, true))
                    .switchMap(mapToDelegations())
        }
    }

    private fun mapToDelegations(): Function<ExpResult<AddressBalance>, ObservableSource<ExpResult<AddressBalanceTotal>>> {
        return Function { res: ExpResult<AddressBalance> ->
            rxExp(addressRepo.getDelegations(res.result.address, 0))
                    .map(mapDelegationsToBalances(res))
        }
    }

    private fun mapDelegationsToBalances(res: ExpResult<AddressBalance>): Function<ExpResult<DelegationList>, ExpResult<AddressBalanceTotal>> {
        return Function { delegatedResult: ExpResult<DelegationList> ->
            val out = ExpResult<AddressBalanceTotal>()
            out.error = delegatedResult.error
            out.result = AddressBalanceTotal(res.result, delegatedResult.meta.additional.delegatedAmount)

            out.result.delegatedCoins = delegatedResult.result.delegations

            out
        }
    }

    private fun aggregate(): Function<List<ExpResult<AddressBalanceTotal>>, AddressListBalancesTotal> {
        return Function { ExpResults: List<ExpResult<AddressBalanceTotal>> ->
            val out = AddressListBalancesTotal()
            for (balance in ExpResults) {
                if (balance.isOk) {
                    out.balances.add(balance.result)
                } else {
                    Timber.w(balance.error.getMessage())
                }
            }
            out
        }
    }

}