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
package network.minter.bipwallet.tx.adapters

import androidx.lifecycle.MutableLiveData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.rxjava2.RxPagingSource
import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import network.minter.bipwallet.R
import network.minter.bipwallet.addressbook.db.AddressBookRepository
import network.minter.bipwallet.apis.explorer.CacheValidatorsRepository
import network.minter.bipwallet.apis.explorer.RepoValidators
import network.minter.bipwallet.apis.reactive.ReactiveExplorer.toExpError
import network.minter.bipwallet.apis.reactive.avatar
import network.minter.bipwallet.internal.Wallet
import network.minter.bipwallet.internal.adapter.DataSourceMeta
import network.minter.bipwallet.internal.adapter.LoadState
import network.minter.bipwallet.internal.data.CachedRepository
import network.minter.bipwallet.internal.helpers.DateHelper
import network.minter.bipwallet.internal.storage.SecretStorage
import network.minter.core.crypto.MinterAddress
import network.minter.core.crypto.MinterPublicKey
import network.minter.explorer.models.ExpResult
import network.minter.explorer.models.HistoryTransaction
import network.minter.explorer.models.ValidatorItem
import network.minter.explorer.models.ValidatorMeta
import network.minter.explorer.repo.ExplorerTransactionRepository
import network.minter.explorer.repo.ExplorerTransactionRepository.TxFilter
import network.minter.explorer.repo.TxSearchQuery
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
class TransactionDataSource(private val factory: Factory) : RxPagingSource<Int, TransactionItem>() {
    private val _disposables = CompositeDisposable()
    private var _lastDate: DateTime? = null

    init {
        registerInvalidatedCallback {
            _disposables.dispose()
        }
    }

    override fun getRefreshKey(state: PagingState<Int, TransactionItem>): Int? {
        val ret = state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                    ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }

        Timber.d("TX_LIST: refresh key $ret")

        return ret
    }

    override fun loadSingle(params: LoadParams<Int>): Single<LoadResult<Int, TransactionItem>> {
        factory.loadState?.postValue(LoadState.Loading)

        Timber.d("TX_LIST: loadSingle page=${params.key}")
        val q = TxSearchQuery().apply {
            setPage(params.key?.toLong() ?: 1L)
        }
        return resolveInfo(factory.repo.getTransactions(q))
//        return resolveInfo(factory.repo.getTransactions(factory.myAddress, params.key ?: 1, factory.txFilter))
                .map { groupByDate(it) }
                .doOnSubscribe { _disposables.add(it) }
                .singleOrError()
                .doOnError { factory.loadState?.postValue(LoadState.Failed) }
                .map {
                    factory.loadState?.postValue(LoadState.Loaded)
                    val nextKey = if(it.meta.lastPage > it.meta.currentPage) {
                        it.meta.currentPage + 1
                    } else {
                        null
                    }
                    LoadResult.Page(
                            data = it.items,
                            prevKey = if (it.meta.currentPage == 1) null else it.meta.currentPage - 1,
                            nextKey = nextKey
                    )
                }
    }

    private fun resolveInfo(source: Observable<ExpResult<List<HistoryTransaction>>>): Observable<ExpResult<List<TransactionFacade>>> {
        return source
                .onErrorResumeNext(toExpError())
                .switchMap { mapToFacade(it) }
                .switchMap { mapValidatorsInfo(factory.validatorsRepo, it) }
                .switchMap { mapAddressBook(factory.addressBookRepo, it) }
                .switchMap { mapAvatar(it) }

        // .switchMap(items -> mapAddressesInfo(factory.addressList, factory.infoRepo, items))

    }


    private fun lastDay(): String {
        return if (DateHelper.compareFlatDay(_lastDate!!, DateTime())) {
            Wallet.app().res().getString(R.string.day_today)
        } else {
            if (_lastDate!!.year != DateTime().year().get()) {
                _lastDate!!.toString(DateTimeFormat.forPattern("EEEE, dd MMMM YYYY"))
            } else {
                _lastDate!!.toString(DateTimeFormat.forPattern("EEEE, dd MMMM"))
            }


        }
    }

    private fun dt(d: Date): DateTime {
        return DateTime(d)
    }

    private fun groupByDate(res: ExpResult<List<TransactionFacade>>): DataSourceMeta<TransactionItem> {
        val out: MutableList<TransactionItem> = ArrayList()
        for (tx in res.result) {
            if (_lastDate == null) {
                _lastDate = DateTime(tx.get().timestamp)
                out.add(HeaderItem(lastDay()))
            } else if (!DateHelper.compareFlatDay(_lastDate!!, dt(tx.get().timestamp))) {
                _lastDate = dt(tx.get().timestamp)
                out.add(HeaderItem(lastDay()))
            }
            out.add(TxItem(tx))
        }
        val meta = DataSourceMeta<TransactionItem>()
        meta.items = out
        meta.meta = res.getMeta()
        return meta
    }

    class Factory @Inject constructor(secretStorage: SecretStorage) {
        @Inject lateinit var repo: ExplorerTransactionRepository
        @Inject lateinit var validatorsRepo: RepoValidators
        @Inject lateinit var addressBookRepo: AddressBookRepository
        @Inject lateinit var secretStorage: SecretStorage

        val address: MinterAddress = secretStorage.mainWallet
        var loadState: MutableLiveData<LoadState>? = null
        var txFilter: TxFilter = TxFilter.None

        val myAddress: MinterAddress
            get() {
                return secretStorage.mainWallet
            }

        fun create(): PagingSource<Int, TransactionItem> {
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

        fun mapAvatar(
                result: List<TransactionFacade>
        ): Observable<List<TransactionFacade>> {
            return Observable.just(result)
                    .map {
                        it.forEach { tx ->
                            tx.fromAvatar = tx.from.avatar
                            if (tx.type == HistoryTransaction.Type.Send) {
                                val d = tx.getData<HistoryTransaction.TxSendCoinResult>()
                                tx.toAvatar = d.to.avatar
                            }
                        }
                        it
                    }
        }

        fun mapAvatar(
                result: ExpResult<List<TransactionFacade>>
        ): Observable<ExpResult<List<TransactionFacade>>> {
            return mapAvatar(result.result)
                    .map {
                        val expResult = result
                        expResult.result = it
                        expResult
                    }
        }

        fun mapAddressBook(
                addressBookRepo: AddressBookRepository,
                result: List<TransactionFacade>
        ): Observable<List<TransactionFacade>> {
            return addressBookRepo.findAll()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .map { addresses ->

                        for (tx in result) {
                            for (address in addresses) {

                                if (tx.from.toString().lowercase(Locale.getDefault()) == address.address!!.lowercase(Locale.getDefault())) {
                                    tx.fromName = address.name
                                }

                                when (tx.type) {
                                    HistoryTransaction.Type.Send -> {
                                        val d = tx.getData<HistoryTransaction.TxSendCoinResult>()
                                        if (d.to.toString().lowercase(Locale.getDefault()) == address.address!!.lowercase(Locale.getDefault())) {
                                            tx.toName = address.name
                                        }
                                    }
                                    HistoryTransaction.Type.RedeemCheck -> {
                                        val d = tx.getData<HistoryTransaction.TxRedeemCheckResult>()
                                        @Suppress("DEPRECATION")
                                        if (d.check.sender.toString().lowercase(Locale.getDefault()) == address.address!!.lowercase(Locale.getDefault())) {
                                            tx.toName = address.name
                                        }
                                    }
                                    else -> {
                                    }
                                }
                            }
                        }

                        result
                    }
        }

        fun mapAddressBook(
                addressBookRepo: AddressBookRepository,
                result: ExpResult<List<TransactionFacade>>
        ): ObservableSource<ExpResult<List<TransactionFacade>>> {
            return mapAddressBook(addressBookRepo, result.result)
                    .map {
                        val expResult = result
                        expResult.result = it
                        expResult
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
                                val meta = validators[pubKey]
                                tx.toName = meta?.name
                                tx.toAvatar = meta?.iconUrl
                            }
                        }
                        items
                    }

        }
    }


}