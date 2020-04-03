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

package network.minter.bipwallet.wallets.views;

import android.view.View;

import com.annimon.stream.Stream;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.schedulers.Schedulers;
import moxy.InjectViewState;
import network.minter.bipwallet.R;
import network.minter.bipwallet.advanced.repo.SecretStorage;
import network.minter.bipwallet.apis.explorer.CacheTxRepository;
import network.minter.bipwallet.apis.explorer.CacheValidatorsRepository;
import network.minter.bipwallet.internal.data.CachedRepository;
import network.minter.bipwallet.internal.mvp.MvpBasePresenter;
import network.minter.bipwallet.tx.adapters.TransactionDataSource;
import network.minter.bipwallet.tx.adapters.TransactionFacade;
import network.minter.bipwallet.tx.adapters.TransactionShortListAdapter;
import network.minter.bipwallet.wallets.contract.BaseWalletsPageView;
import network.minter.bipwallet.wallets.contract.TxsTabPageView;
import network.minter.bipwallet.wallets.utils.HistoryTransactionDiffUtil;
import network.minter.core.crypto.MinterAddress;
import network.minter.explorer.models.HistoryTransaction;
import network.minter.explorer.models.ValidatorItem;

import static network.minter.bipwallet.tx.adapters.TransactionDataSource.mapValidatorsInfo;

@InjectViewState
public class TxsTabPagePresenter extends MvpBasePresenter<TxsTabPageView> {
    @Inject CachedRepository<List<HistoryTransaction>, CacheTxRepository> txRepo;
    @Inject SecretStorage secretRepo;
    @Inject CachedRepository<List<ValidatorItem>, CacheValidatorsRepository> validatorsRepo;
//        @Inject ProfileInfoRepository infoRepo;

    private List<MinterAddress> myAddresses = new ArrayList<>();
    private TransactionShortListAdapter mTransactionsAdapter;

    @Inject
    public TxsTabPagePresenter() {

    }

    @Override
    public void attachView(TxsTabPageView view) {
        super.attachView(view);
        myAddresses = secretRepo.getAddresses();
        txRepo.update();
    }

    @Override
    protected void onFirstViewAttach() {
        super.onFirstViewAttach();
        mTransactionsAdapter = new TransactionShortListAdapter(myAddresses);
        mTransactionsAdapter.setOnExplorerOpenClickListener(this::onExplorerClick);
        getViewState().setAdapter(mTransactionsAdapter);
        getViewState().setListTitle("Latest Transactions");
        getViewState().setActionTitle(R.string.btn_all_txs);
        getViewState().setOnActionClickListener(this::onClickOpenTransactions);

        /*
        //        safeSubscribeIoToUi(
//                txRepo.observe()
//                        .switchMap(TransactionDataSource::mapToFacade)
////                        .switchMap(items -> mapValidatorsInfo(validatorsRepo, items))
////                        .switchMap(items -> mapAddressesInfo(myAddresses, infoRepo, items))
//                        .subscribeOn(Schedulers.io())
//        )
//                .subscribe(res -> {
//                    updateDelegation();
//                    mTransactionsAdapter.dispatchChanges(HistoryTransactionDiffUtil.class, Stream.of(res).limit(5).toList(), true);
//                    if (mTransactionsAdapter.getItemCount() == 0) {
//                        mTransactionsRow.setStatus(ListWithButtonRow.Status.Empty);
//                    } else {
//                        mTransactionsRow.setStatus(ListWithButtonRow.Status.Normal);
//                    }
//                    getViewState().hideRefreshProgress();
////                    getViewState().scrollTop();
//                }, t -> {
//                    mTransactionsRow.setStatus(ListWithButtonRow.Status.Error);
//                    getViewState().hideRefreshProgress();
//                });
         */

        safeSubscribeIoToUi(
                txRepo.observe()
                        .switchMap(TransactionDataSource::mapToFacade)
                        .switchMap(items -> mapValidatorsInfo(validatorsRepo, items))
//                        .switchMap(items -> mapAddressesInfo(myAddresses, infoRepo, items))
                        .subscribeOn(Schedulers.io())
        )
                .subscribe(res -> {
//                    updateDelegation();
                    mTransactionsAdapter.dispatchChanges(HistoryTransactionDiffUtil.class, Stream.of(res).limit(5).toList(), true);
                    if (mTransactionsAdapter.getItemCount() == 0) {
                        getViewState().setViewStatus(BaseWalletsPageView.ViewStatus.Empty);
                    } else {
                        getViewState().setViewStatus(BaseWalletsPageView.ViewStatus.Normal);
                    }
//                    getViewState().hideRefreshProgress();
//                    getViewState().scrollTop();
                }, t -> {
                    getViewState().setViewStatus(BaseWalletsPageView.ViewStatus.Error);
//                    getViewState().hideRefreshProgress();
                });
    }

    private void onClickOpenTransactions(View view) {

    }

    private void onExplorerClick(View view, TransactionFacade transactionFacade) {

    }
}
