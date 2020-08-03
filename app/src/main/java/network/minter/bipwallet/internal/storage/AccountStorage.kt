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
import io.reactivex.Single
import io.reactivex.functions.Function
import io.reactivex.schedulers.Schedulers
import network.minter.bipwallet.BuildConfig
import network.minter.bipwallet.apis.reactive.ReactiveExplorer.createExpErrorPlain
import network.minter.bipwallet.apis.reactive.rxExp
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
import java.math.BigDecimal

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
        private const val KEY_BALANCE_INACTIVE = BuildConfig.MINTER_STORAGE_VERS + "account_storage_balance_inactive_"
        private const val KEY_DELEGATIONS_INACTIVE = BuildConfig.MINTER_STORAGE_VERS + "account_storage_delegations_inactive_"
        private const val KEY_DELEGATIONS_META_INACTIVE = BuildConfig.MINTER_STORAGE_VERS + "account_storage_delegations_meta_inactive_"
    }

    private fun inactiveBalanceKey(address: MinterAddress): String {
        return "${KEY_BALANCE_INACTIVE}$address"
    }

    private fun inactiveDelegationsKey(address: MinterAddress): String {
        return "${KEY_DELEGATIONS_INACTIVE}$address"
    }

    private fun inactiveDelegationsMetaKey(address: MinterAddress): String {
        return "${KEY_DELEGATIONS_META_INACTIVE}$address"
    }

    private fun getBalanceData(address: MinterAddress): Observable<ExpResult<AddressBalance>> {
        return if (secretStorage.mainWallet == address || !storage.contains(inactiveBalanceKey(address))) {
            addressRepo.getAddressData(address, true).rxExp()
        } else {
            val out = ExpResult<AddressBalance>()
            out.result = storage.get(inactiveBalanceKey(address))
            Observable.just(out)
        }
    }

    private fun getDelegationsData(address: MinterAddress): Observable<ExpResult<DelegationList>> {
        return if (secretStorage.mainWallet == address || !storage.contains(inactiveDelegationsKey(address))) {
            addressRepo.getDelegations(address, 0).rxExp()
        } else {
            val out = ExpResult<DelegationList>()
            out.result = storage.get(inactiveDelegationsKey(address))
            out.meta = storage.get(inactiveDelegationsMetaKey(address))
            Observable.just(out)
        }
    }

    override fun getData(): AddressListBalancesTotal {
        return storage[KEY_BALANCE, AddressListBalancesTotal(secretStorage.addresses)]
    }

    val mainWallet: AddressBalanceTotal
        get() = getData().getBalance(secretStorage.mainWallet)

    val wallets: AddressListBalancesTotal
        get() = getData()

    fun getWalletByAddress(address: MinterAddress): AddressBalanceTotal {
        return getData().getBalance(address)
    }

    override fun onAfterUpdate(result: AddressListBalancesTotal) {
        if (result.balances.isEmpty()) {
            storage.put(KEY_BALANCE, AddressListBalancesTotal(secretStorage.addresses))
            return
        }
        storage.put(KEY_BALANCE, result)
    }

    override fun onClear() {
        storage.delete(KEY_BALANCE)
    }

    override fun isDataReady(): Boolean {
        return storage.contains(KEY_BALANCE)
    }

    override fun getUpdatableData(): Observable<AddressListBalancesTotal> {
        val addresses = Observable.fromIterable(secretStorage.addresses)

        return addresses.flatMap(mapBalances())
                .toList()
                .flatMap(aggregate())
                .subscribeOn(Schedulers.io())
                .toObservable()
    }

    private fun mapBalances(): Function<MinterAddress, ObservableSource<ExpResult<AddressBalanceTotal>>> {
        return Function { address: MinterAddress ->
            getBalanceData(address)
                    .map {
                        if (secretStorage.mainWallet == address) {
                            storage.putAsync(inactiveBalanceKey(address), it.result)
                        }
                        it
                    }
                    .switchMap(mapToDelegations())
        }
    }

    private fun mapToDelegations(): Function<ExpResult<AddressBalance>, ObservableSource<ExpResult<AddressBalanceTotal>>> {
        return Function { res: ExpResult<AddressBalance> ->
            if (res.result == null || !res.isOk) {
                return@Function Observable.just(
                        createExpErrorPlain<AddressBalanceTotal>(res.message, res.error?.code ?: 0, 0)
                )
            }
            val address = res.result.address
            getDelegationsData(address)
                    .map {
                        if (secretStorage.mainWallet == address) {
                            storage.putAsync(inactiveDelegationsKey(address), it.result)
                            storage.putAsync(inactiveDelegationsMetaKey(address), it.meta)
                        }

                        it
                    }
                    .map(mapDelegationsToBalances(res))
        }
    }

    private fun mapDelegationsToBalances(res: ExpResult<AddressBalance>): Function<ExpResult<DelegationList>, ExpResult<AddressBalanceTotal>> {
        return Function { delegatedResult: ExpResult<DelegationList> ->
            val out = ExpResult<AddressBalanceTotal>()
            out.error = delegatedResult.error
            out.meta = delegatedResult.meta

            if (res.result == null) {
                return@Function out
            }
            out.result = AddressBalanceTotal(res.result, delegatedResult.meta?.additional?.delegatedAmount
                    ?: BigDecimal.ZERO)
            out.latestBlockTime = res.latestBlockTime
            out.result.delegatedCoins = delegatedResult.result?.delegations ?: HashMap()

            out
        }
    }

    private fun aggregate(): Function<List<ExpResult<AddressBalanceTotal>>, Single<AddressListBalancesTotal>> {
        return Function { ExpResults: List<ExpResult<AddressBalanceTotal>> ->
            val data = AddressListBalancesTotal()
            var error: ExpResult.ErrorResult? = null
            for (balance in ExpResults) {
                if (balance.error != null && error == null) {
                    error = balance.error
                }
                data.latestBlockTime = balance.latestBlockTime
                if (balance.isOk) {
                    data.balances.add(balance.result)
                } else {
                    Timber.w(balance.error.getMessage())
                }
            }

//            if(error != null) {
//                return@Function Single.error<AddressListBalancesTotal>(NetworkException(error.code, error.message, error.message))
//            }

            Single.just(data)
        }
    }

    fun remove(address: MinterAddress) {
        val data = getData()
        if (data.find(address).isPresent) {
            data.remove(address)
            storage.put(KEY_BALANCE, data)
        }
    }

}