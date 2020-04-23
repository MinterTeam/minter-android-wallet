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
package network.minter.bipwallet.tx.adapters

import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import androidx.paging.PageKeyedDataSource
import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.disposables.CompositeDisposable
import network.minter.bipwallet.R
import network.minter.bipwallet.apis.explorer.CacheValidatorsRepository
import network.minter.bipwallet.apis.reactive.ReactiveExplorer.toExpError
import network.minter.bipwallet.apis.reactive.ReactiveMyMinter
import network.minter.bipwallet.apis.reactive.ReactiveMyMinter.rxProfile
import network.minter.bipwallet.apis.reactive.rxExp
import network.minter.bipwallet.internal.Wallet
import network.minter.bipwallet.internal.adapter.DataSourceMeta
import network.minter.bipwallet.internal.adapter.LoadState
import network.minter.bipwallet.internal.data.CachedRepository
import network.minter.bipwallet.internal.helpers.DateHelper
import network.minter.bipwallet.internal.storage.SecretStorage
import network.minter.bipwallet.tx.adapters.TransactionFacade.UserMeta
import network.minter.core.crypto.MinterAddress
import network.minter.core.crypto.MinterPublicKey
import network.minter.explorer.models.ExpResult
import network.minter.explorer.models.HistoryTransaction
import network.minter.explorer.models.ValidatorItem
import network.minter.explorer.models.ValidatorMeta
import network.minter.explorer.repo.ExplorerTransactionRepository
import network.minter.profile.models.AddressInfoResult
import network.minter.profile.models.ProfileResult
import network.minter.profile.repo.ProfileInfoRepository
import okhttp3.internal.toImmutableList
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import timber.log.Timber
import java.util.*
import javax.inject.Inject

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
class TransactionDataSource(private val factory: Factory) : PageKeyedDataSource<Long, TransactionItem>() {
    private val mDisposables = CompositeDisposable()
    private var mLastDate: DateTime? = null

    override fun loadInitial(params: LoadInitialParams<Long>, callback: LoadInitialCallback<Long, TransactionItem?>) {
        factory.loadState?.postValue(LoadState.Loading)

        if (factory.addressList.isEmpty()) {
            Timber.w("Unanble to load transactions list(page: 1): user address list is empty")
            factory.loadState?.postValue(LoadState.Loaded)
            callback.onResult(emptyList(), null, null)
            return
        }

        resolveInfo(factory.repo.getTransactions(factory.addressList, 1).rxExp())
                .map { groupByDate(it) }
                .doOnSubscribe { mDisposables.add(it) }
                .subscribe {
                    factory.loadState?.postValue(LoadState.Loaded)
                    callback.onResult(it.items, null, if (it.meta.lastPage == 1) null else it.meta.currentPage + 1L)
                }
    }

    override fun loadBefore(params: LoadParams<Long>, callback: LoadCallback<Long, TransactionItem>) {
        factory.loadState?.postValue(LoadState.Loading)
        if (factory.addressList.isEmpty()) {
            Timber.w("Unable to load previous transactions list (page: %s): user address list is empty", params.key)
            factory.loadState?.postValue(LoadState.Loaded)
            callback.onResult(emptyList(), null)
            return
        }

        resolveInfo(factory.repo.getTransactions(factory.addressList, params.key).rxExp())
                .map { groupByDate(it) }
                .doOnSubscribe { mDisposables.add(it) }
                .subscribe {
                    factory.loadState?.postValue(LoadState.Loaded)
                    callback.onResult(it.items, if (params.key == 1L) null else params.key - 1)
                }
    }

    override fun loadAfter(params: LoadParams<Long>, callback: LoadCallback<Long, TransactionItem?>) {
        factory.loadState?.postValue(LoadState.Loading)

        resolveInfo(factory.repo.getTransactions(factory.addressList, params.key).rxExp())
                .map { res: ExpResult<List<TransactionFacade>> -> groupByDate(res) }
                .doOnSubscribe { mDisposables.add(it) }
                .subscribe { res: DataSourceMeta<TransactionItem> ->
                    factory.loadState?.postValue(LoadState.Loaded)
                    callback.onResult(res.items, if (params.key + 1L > res.meta.lastPage) null else params.key + 1)
                }
    }

    private fun resolveInfo(source: Observable<ExpResult<List<HistoryTransaction>>>): Observable<ExpResult<List<TransactionFacade>>> {
        return source
                .onErrorResumeNext(toExpError())
                .switchMap { mapToFacade(it) }
                .switchMap { mapValidatorsInfo(factory.validatorsRepo, it) }
        // .switchMap(items -> mapAddressesInfo(factory.addressList, factory.infoRepo, items))

    }

    override fun invalidate() {
        super.invalidate()
        mDisposables.dispose()
    }

    private fun lastDay(): String {
        return if (DateHelper.compareFlatDay(mLastDate!!, DateTime())) {
            Wallet.app().res().getString(R.string.today)
        } else mLastDate!!.toString(DateTimeFormat.forPattern("EEEE, dd MMMM").withLocale(Locale.US))
    }

    private fun dt(d: Date): DateTime {
        return DateTime(d)
    }

    private fun groupByDate(res: ExpResult<List<TransactionFacade>>): DataSourceMeta<TransactionItem> {
        val out: MutableList<TransactionItem> = ArrayList()
        for (tx in res.result) {
            if (mLastDate == null) {
                mLastDate = DateTime(tx.get().timestamp)
                out.add(HeaderItem(lastDay()))
            } else if (!DateHelper.compareFlatDay(mLastDate!!, dt(tx.get().timestamp))) {
                mLastDate = dt(tx.get().timestamp)
                out.add(HeaderItem(lastDay()))
            }
            out.add(TxItem(tx))
        }
        val meta = DataSourceMeta<TransactionItem>()
        meta.items = out
        meta.meta = res.getMeta()
        return meta
    }

    class Factory @Inject constructor(secretStorage: SecretStorage) : DataSource.Factory<Long, TransactionItem>() {
        lateinit var repo: ExplorerTransactionRepository
            @Inject set
        lateinit var validatorsRepo: CachedRepository<List<ValidatorItem>, CacheValidatorsRepository>
            @Inject set

        val addressList: List<MinterAddress>
        var loadState: MutableLiveData<LoadState>? = null

        init {
            addressList = secretStorage.addresses
        }

        override fun create(): DataSource<Long, TransactionItem> {
            return TransactionDataSource(this)
        }

        fun observeLoadState(loadState: MutableLiveData<LoadState>) {
            this.loadState = loadState
        }
    }

    companion object {
        fun mapToFacade(result: ExpResult<List<HistoryTransaction>>): ObservableSource<ExpResult<List<TransactionFacade>>> {
            val items: List<TransactionFacade> = if (result.result == null) {
                emptyList()
            } else {
                result.result.map { TransactionFacade(it) }.toList()
            }
            val out = ExpResult<List<TransactionFacade>>()
            out.links = result.meta
            out.meta = result.meta
            out.error = result.error
            out.result = items
            return Observable.just(out)
        }

        fun mapToFacade(result: List<HistoryTransaction>): ObservableSource<List<TransactionFacade>> {
            return if (result.isEmpty()) {
                Observable.just(emptyList())
            } else Observable.just(
                    result.map { TransactionFacade(it) }.toImmutableList()
            )
        }

        fun mapAddressesInfo(
                myAddresses: List<MinterAddress>,
                infoRepo: ProfileInfoRepository,
                items: ExpResult<List<TransactionFacade>>
        ): ObservableSource<ExpResult<List<TransactionFacade>>> {

            if (items.result == null || items.result!!.isEmpty()) {
                return Observable.just(items)
            }

            val toFetch: MutableList<MinterAddress> = ArrayList()
            val toFetchAddresses: MutableMap<MinterAddress?, MutableList<TransactionFacade>> = LinkedHashMap(items.result!!.size)

            for (tx in items.result!!) {
                if (tx.type != HistoryTransaction.Type.Send) {
                    continue
                }
                val add: MinterAddress = if (tx.isIncoming(myAddresses)) {
                    tx.from
                } else {
                    tx.getData<HistoryTransaction.TxSendCoinResult>().getTo()
                }
                if (!toFetch.contains(add)) {
                    toFetch.add(add)
                }
                if (!toFetchAddresses.containsKey(add)) {
                    toFetchAddresses[add] = ArrayList()
                }
                toFetchAddresses[add]!!.add(tx)
            }

            return rxProfile(infoRepo.getAddressesWithUserInfo(toFetch))
                    .onErrorResumeNext(ReactiveMyMinter.toProfileError())
                    .map { listInfoResult: ProfileResult<List<AddressInfoResult>> ->
                        if (listInfoResult.data.isEmpty()) {
                            return@map items
                        }
                        for (info in listInfoResult.data) {
                            for (t in toFetchAddresses[info.address]!!) {
                                t.setUserMeta(info.user.username, info.user.getAvatar().url)
                            }
                        }
                        items
                    }
        }

        fun mapValidatorsInfo(
                validatorRepo: CachedRepository<List<ValidatorItem>, CacheValidatorsRepository>,
                result: ExpResult<List<TransactionFacade>>
        ): ObservableSource<ExpResult<List<TransactionFacade>>> {

            return Observable.just(result)
                    .map { items: ExpResult<List<TransactionFacade>> -> items.result }
                    .switchMap { items: List<TransactionFacade> -> mapValidatorsInfo(validatorRepo, items) }
                    .switchMap { items: List<TransactionFacade> ->
                        result.result = items
                        Observable.just(result)
                    }
        }

        fun mapValidatorsInfo(
                validatorRepo: CachedRepository<List<ValidatorItem>, CacheValidatorsRepository>,
                items: List<TransactionFacade>
        ): ObservableSource<List<TransactionFacade>> {
            return validatorRepo.fetch()
                    .map<Map<MinterPublicKey, ValidatorMeta>> { validatorItems: List<ValidatorItem> ->
                        val out: MutableMap<MinterPublicKey, ValidatorMeta> = HashMap()
                        validatorItems.forEach { out[it.pubKey] = it.meta }
                        out
                    }
                    .map { validators: Map<MinterPublicKey, ValidatorMeta> ->
                        for (tx in items) {
                            val pubKey: MinterPublicKey? = when (tx.type) {
                                HistoryTransaction.Type.Delegate,
                                HistoryTransaction.Type.Unbond -> {
                                    tx.getData<HistoryTransaction.TxDelegateUnbondResult>().publicKey
                                }
                                HistoryTransaction.Type.EditCandidate -> {
                                    tx.getData<HistoryTransaction.TxEditCandidateResult>().publicKey
                                }
                                HistoryTransaction.Type.SetCandidateOnline,
                                HistoryTransaction.Type.SetCandidateOffline -> {
                                    tx.getData<HistoryTransaction.TxSetCandidateOnlineOfflineResult>().publicKey
                                }
                                else -> null
                            }

                            if (pubKey != null && validators.containsKey(pubKey)) {
                                tx.setValidatorMeta(validators[pubKey])
                            }
                        }
                        items
                    }

        }

        fun mapAddressesInfo(
                addresses: List<MinterAddress>,
                infoRepo: ProfileInfoRepository,
                items: List<TransactionFacade>): ObservableSource<List<TransactionFacade>> {

            if (items.isEmpty()) {
                return Observable.just(emptyList())
            }
            val toFetch: MutableList<MinterAddress> = ArrayList()
            val toFetchAddresses: MutableMap<MinterAddress, MutableList<TransactionFacade>> = LinkedHashMap(items.size)
            for (tx in items) {
                if (tx.type != HistoryTransaction.Type.Send) {
                    continue
                }
                val add: MinterAddress = if (tx.isIncoming(addresses)) {
                    tx.from
                } else {
                    tx.getData<HistoryTransaction.TxSendCoinResult>().to
                }
                if (!toFetch.contains(add)) {
                    toFetch.add(add)
                }
                if (!toFetchAddresses.containsKey(add)) {
                    toFetchAddresses[add] = ArrayList()
                }
                toFetchAddresses[add]!!.add(tx)
            }
            return rxProfile(infoRepo.getAddressesWithUserInfo(toFetch))
                    .onErrorResumeNext(ReactiveMyMinter.toProfileError())
                    .map { listInfoResult: ProfileResult<List<AddressInfoResult>?> ->
                        if (listInfoResult.data == null) {
                            listInfoResult.data = emptyList()
                        }
                        if (listInfoResult.data!!.isEmpty()) {
                            return@map items
                        }
                        for (info in listInfoResult.data!!) {
                            for (t in toFetchAddresses[info.address]!!) {
                                t.userMeta = UserMeta()
                                t.userMeta.username = info.user.username
                                t.userMeta.avatarUrl = info.user.getAvatar().url
                            }
                        }
                        items
                    }
        }
    }

}