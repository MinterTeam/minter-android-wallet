
/*
 * Copyright (C) 2018 by MinterTeam
 * @link https://github.com/MinterTeam
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

package network.minter.bipwallet.coins.views;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.annimon.stream.Stream;
import com.arellomobile.mvp.InjectViewState;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.schedulers.Schedulers;
import network.minter.bipwallet.R;
import network.minter.bipwallet.advanced.models.AccountItem;
import network.minter.bipwallet.advanced.models.UserAccount;
import network.minter.bipwallet.advanced.repo.AccountStorage;
import network.minter.bipwallet.advanced.repo.SecretStorage;
import network.minter.bipwallet.apis.explorer.CachedExplorerTransactionRepository;
import network.minter.bipwallet.coins.CoinsTabModule;
import network.minter.bipwallet.coins.utils.HistoryTransactionDiffUtil;
import network.minter.bipwallet.coins.views.rows.ListWithButtonRow;
import network.minter.bipwallet.internal.Wallet;
import network.minter.bipwallet.internal.auth.AuthSession;
import network.minter.bipwallet.internal.data.CacheManager;
import network.minter.bipwallet.internal.data.CachedRepository;
import network.minter.bipwallet.internal.mvp.MvpBasePresenter;
import network.minter.bipwallet.internal.views.list.SimpleRecyclerAdapter;
import network.minter.bipwallet.internal.views.list.multirow.MultiRowAdapter;
import network.minter.bipwallet.internal.views.widgets.BipCircleImageView;
import network.minter.bipwallet.tx.adapters.TransactionDataSource;
import network.minter.blockchainapi.repo.BlockChainAccountRepository;
import network.minter.explorerapi.models.HistoryTransaction;
import network.minter.explorerapi.repo.ExplorerAddressRepository;
import network.minter.mintercore.crypto.MinterAddress;
import network.minter.mintercore.internal.helpers.StringHelper;
import network.minter.my.MyMinterApi;
import network.minter.my.repo.MyInfoRepository;
import timber.log.Timber;

import static network.minter.bipwallet.internal.helpers.Plurals.bips;

/**
 * MinterWallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@InjectViewState
public class CoinsTabPresenter extends MvpBasePresenter<CoinsTabModule.CoinsTabView> {

    @Inject CacheManager cache;
    @Inject AuthSession session;
    @Inject CachedRepository<List<HistoryTransaction>, CachedExplorerTransactionRepository> txRepo;
    @Inject SecretStorage secretRepo;
    @Inject CachedRepository<UserAccount, AccountStorage> accountStorage;
    @Inject BlockChainAccountRepository accountRepo;
    @Inject ExplorerAddressRepository addressRepo;
    @Inject MyInfoRepository infoRepo;
    private List<MinterAddress> myAddresses = new ArrayList<>();
    private MultiRowAdapter mAdapter;
    private SimpleRecyclerAdapter<HistoryTransaction, ItemViewHolder> mTransactionsAdapter;
    private SimpleRecyclerAdapter<AccountItem, ItemViewHolder> mCoinsAdapter;
    private ListWithButtonRow mTransactionsRow, mCoinsRow;

    @Inject
    public CoinsTabPresenter() {
        mAdapter = new MultiRowAdapter();
        mTransactionsAdapter = new SimpleRecyclerAdapter.Builder<HistoryTransaction, ItemViewHolder>()
                .setCreator(R.layout.item_list_with_image, ItemViewHolder.class)
                .setBinder((itemViewHolder, item, position) -> {
                    if (item.type == HistoryTransaction.Type.Send) {
                        HistoryTransaction.TxSendCoinResult sendResult = item.getData();
                        if (item.isIncoming(myAddresses)) {
                            itemViewHolder.amount.setText(String.format("+ %s", sendResult.amount.toPlainString()));
                            itemViewHolder.amount.setTextColor(Wallet.app().res().getColor(R.color.textColorGreen));
                        } else {
                            itemViewHolder.amount.setText(String.format("- %s", sendResult.amount.toPlainString()));
                            itemViewHolder.amount.setTextColor(Wallet.app().res().getColor(R.color.textColorPrimary));
                        }

                        if (item.username != null) {
                            itemViewHolder.title.setText(String.format("@%s", item.username));
                        } else {
                            itemViewHolder.title.setText(sendResult.to.toShortString());
                        }

                        itemViewHolder.avatar.setImageUrl(item.getAvatar());
                        itemViewHolder.subname.setText(sendResult.coin.toUpperCase());
                    } else {
                        itemViewHolder.avatar.setImageUrl(item.getAvatar());
                        itemViewHolder.title.setText(item.hash.toShortString());
                        itemViewHolder.amount.setText(item.type.name());

                    }
                }).build();

        mCoinsAdapter = new SimpleRecyclerAdapter.Builder<AccountItem, ItemViewHolder>()
                .setCreator(R.layout.item_list_with_image, ItemViewHolder.class)
                .setBinder((itemViewHolder, item, position) -> {
                    itemViewHolder.title.setText(item.coin.toUpperCase());
                    itemViewHolder.amount.setText(item.balance.toPlainString());
                    itemViewHolder.avatar.setImageUrl(MyMinterApi.getCoinAvatarUrl(item.coin));
                    itemViewHolder.subname.setVisibility(View.GONE);


                }).build();
    }

    @Override
    public void attachView(CoinsTabModule.CoinsTabView view) {
        super.attachView(view);
        myAddresses = secretRepo.getAddresses();

        txRepo.update();
        accountStorage.update();

        getViewState().setAdapter(mAdapter);
    }

    @Override
    protected void onFirstViewAttach() {
        super.onFirstViewAttach();
        if (session.getRole() == AuthSession.AuthType.Advanced) {
            getViewState().hideAvatar();
        }
        getViewState().setAvatar(session.getUser().getData().getAvatar().getUrl());
        mTransactionsRow = new ListWithButtonRow.Builder("Latest transactions")
                .setAction("All transactions", this::onClickStartTransactionList)
                .setAdapter(mTransactionsAdapter)
                .setEmptyTitle("You have no transactions")
                .build();

        mCoinsRow = new ListWithButtonRow.Builder("My Coins")
                .setAction("Convert", this::onClickConvertCoins)
                .setAdapter(mCoinsAdapter)
                .setEmptyTitle("You have no one address. Nothing to show.")
                .build();

        safeSubscribeIoToUi(accountStorage.observe())
                .map(AccountStorage.groupAccountByCoin())
                .subscribe(res -> {
                    Timber.d("Update coins list");
                    mCoinsAdapter.dispatchChanges(AccountItem.DiffUtilImpl.class, res.getAccounts());
                    final StringHelper.DecimalFraction num = StringHelper.splitDecimalFractions(res.getTotalBalance());
                    getViewState().setBalance(num.intPart, num.fractionalPart, bips(num.intPart));

                    mCoinsRow.setStatus(ListWithButtonRow.Status.Normal);
                });

        safeSubscribeIoToUi(
                txRepo.observe().switchMap(items -> TransactionDataSource.mapAddressesInfo(myAddresses, infoRepo, items)).subscribeOn(Schedulers.io())
        )
                .subscribe(res -> {
                    mTransactionsAdapter.dispatchChanges(HistoryTransactionDiffUtil.class, Stream.of(res).limit(5).toList(), true);
                    if (mTransactionsAdapter.getItemCount() == 0) {
                        mTransactionsRow.setStatus(ListWithButtonRow.Status.Empty);
                    } else {
                        mTransactionsRow.setStatus(ListWithButtonRow.Status.Normal);
                    }
                });


        mAdapter.addRow(mTransactionsRow);
        mAdapter.addRow(mCoinsRow);
    }

    private void onClickConvertCoins(View view) {
        getViewState().startConvertCoins();
    }

    private void onClickStartTransactionList(View view) {
        getViewState().startTransactionList();
    }

    private boolean isMyAddress(MinterAddress address) {
        for (MinterAddress add : secretRepo.getAddresses()) {
            if (add.equals(address)) {
                return true;
            }
        }

        return false;
    }

    public final static class ItemViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.item_avatar) BipCircleImageView avatar;
        @BindView(R.id.item_title) TextView title;
        @BindView(R.id.item_amount) TextView amount;
        @BindView(R.id.item_subamount) TextView subname;

        public ItemViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

}
