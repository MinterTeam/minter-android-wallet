/*
 * Copyright (C) by MinterTeam. 2019
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

package network.minter.bipwallet.coins.views;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.annimon.stream.Optional;
import com.annimon.stream.Stream;

import org.parceler.Parcel;
import org.parceler.Parcels;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import moxy.InjectViewState;
import network.minter.bipwallet.R;
import network.minter.bipwallet.advanced.models.AccountItem;
import network.minter.bipwallet.advanced.models.UserAccount;
import network.minter.bipwallet.advanced.repo.AccountStorage;
import network.minter.bipwallet.advanced.repo.SecretStorage;
import network.minter.bipwallet.analytics.AppEvent;
import network.minter.bipwallet.apis.explorer.CachedExplorerTransactionRepository;
import network.minter.bipwallet.apis.reactive.ReactiveExplorerGate;
import network.minter.bipwallet.coins.contract.CoinsTabView;
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
import network.minter.bipwallet.settings.repo.CachedMyProfileRepository;
import network.minter.bipwallet.settings.views.SettingsTabPresenter;
import network.minter.bipwallet.tx.adapters.TransactionShortListAdapter;
import network.minter.blockchain.repo.BlockChainAccountRepository;
import network.minter.core.MinterSDK;
import network.minter.core.crypto.MinterAddress;
import network.minter.core.internal.helpers.StringHelper;
import network.minter.explorer.models.HistoryTransaction;
import network.minter.explorer.repo.ExplorerAddressRepository;
import network.minter.profile.MinterProfileApi;
import network.minter.profile.models.User;
import network.minter.profile.repo.ProfileInfoRepository;
import timber.log.Timber;

import static network.minter.bipwallet.internal.helpers.MathHelper.bdHuman;
import static network.minter.bipwallet.internal.helpers.Plurals.bips;
import static network.minter.bipwallet.tx.adapters.TransactionDataSource.mapAddressesInfo;

/**
 * minter-android-wallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@InjectViewState
public class CoinsTabPresenter extends MvpBasePresenter<CoinsTabView> {

    private final BalanceCurrentState mBalanceCurrentState = new BalanceCurrentState();
    @Inject
    CacheManager cache;
    @Inject
    AuthSession session;
    @Inject
    CachedRepository<List<HistoryTransaction>, CachedExplorerTransactionRepository> txRepo;
    @Inject
    CachedRepository<User.Data, CachedMyProfileRepository> profileCachedRepo;
    @Inject
    SecretStorage secretRepo;
    @Inject
    CachedRepository<UserAccount, AccountStorage> accountStorage;
    @Inject
    BlockChainAccountRepository accountRepo;
    @Inject
    ExplorerAddressRepository addressRepo;
    @Inject
    ProfileInfoRepository infoRepo;
    private List<MinterAddress> myAddresses = new ArrayList<>();
    private MultiRowAdapter mAdapter;
    private TransactionShortListAdapter mTransactionsAdapter;
    private SimpleRecyclerAdapter<AccountItem, ItemViewHolder> mCoinsAdapter;
    private ListWithButtonRow mTransactionsRow, mCoinsRow;
    private boolean mUseAvatars = true;
    private boolean mLowMemory = false;

    @Inject
    public CoinsTabPresenter() {
    }

    @Override
    public void attachView(CoinsTabView view) {
        super.attachView(view);
        myAddresses = secretRepo.getAddresses();
        if (!mLowMemory) {
            txRepo.update();
            accountStorage.update();
        }

        unsubscribeOnDestroy(
                SettingsTabPresenter.AVATAR_CHANGE_SUBJECT
                        .subscribeOn(AndroidSchedulers.mainThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(res -> {
                            if (res.getUrl() != null) {
                                getViewState().setAvatar(res.getUrl());
                            }
                        }, Timber::w)
        );

        getViewState().setOnRefreshListener(this::onRefresh);
        getViewState().setAdapter(mAdapter);

        if (session.getUser() != null && session.getUser().getData() != null && session.getUser().getData().getAvatar() != null) {
            getViewState().setAvatar(session.getUser().getData().getAvatar().getUrl());
        }

        mBalanceCurrentState.applyTo(getViewState());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mBalanceCurrentState.onSaveInstanceState(outState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mBalanceCurrentState.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Strange thing on some devices, OOM ?

        if (mAdapter != null) {
            mAdapter.clear();
        }
        if (mTransactionsAdapter != null) {
            mTransactionsAdapter.clear();
        }
        if (mCoinsAdapter != null) {
            mCoinsAdapter.clear();
        }
    }

    @Override
    protected void onFirstViewAttach() {
        super.onFirstViewAttach();

        profileCachedRepo.observe().subscribe(res -> {
            setUsername();
        });

        myAddresses = secretRepo.getAddresses();
        mAdapter = new MultiRowAdapter();
        mTransactionsAdapter = new TransactionShortListAdapter(myAddresses);
        mTransactionsAdapter.setOnExplorerOpenClickListener(this::onExplorerClick);
        session.getUserUpdate().doOnSubscribe(this::unsubscribeOnDestroy).subscribe(res -> setUsername());

        mCoinsAdapter = new SimpleRecyclerAdapter.Builder<AccountItem, ItemViewHolder>()
                .setCreator(R.layout.item_list_with_image, ItemViewHolder.class)
                .setBinder((itemViewHolder, item, position) -> {
                    itemViewHolder.title.setText(item.coin.toUpperCase());
                    itemViewHolder.amount.setText(bdHuman(item.balance));
                    if (mUseAvatars) {
                        itemViewHolder.avatar.setImageUrlFallback(MinterProfileApi.getCoinAvatarUrl(item.coin), R.drawable.img_avatar_default);
                    } else {
                        itemViewHolder.avatar.setImageDrawable(null);
                    }

                    itemViewHolder.subname.setVisibility(View.GONE);


                }).build();

        setUsername();

        getViewState().setOnAvatarClick(this::onAvatarClick);

        mTransactionsRow = new ListWithButtonRow.Builder(Wallet.app().res().getString(R.string.frag_coins_last_transactions_title))
                .setAction("All transactions", this::onClickStartTransactionList)
                .setAdapter(mTransactionsAdapter)
                .setEmptyTitle("You have no transactions")
                .build();

        mCoinsRow = new ListWithButtonRow.Builder(Wallet.app().res().getString(R.string.frag_coins_my_coins_title))
                .setAction("Convert", v -> {
                    Wallet.app().sounds().play(R.raw.bip_beep_digi_octave);
                    onClickConvertCoins(v);
                })
                .setAdapter(mCoinsAdapter)
                .setEmptyTitle("You have no one address. Nothing to show.")
                .build();

        safeSubscribeIoToUi(accountStorage.observe())
                .map(AccountStorage.groupAccountByCoin())
                .subscribe(res -> {
                    Timber.d("Update coins list");
                    mCoinsAdapter.dispatchChanges(AccountItem.DiffUtilImpl.class, res.getAccounts());

                    Optional<AccountItem> defAccount = Stream.of(res.getAccounts())
                            .filter(item -> item.getCoin().equals(MinterSDK.DEFAULT_COIN))
                            .findFirst();

                    final StringHelper.DecimalStringFraction num;
                    if (!defAccount.isPresent()) {
                        num = StringHelper.splitDecimalStringFractions(new BigDecimal("0").setScale(4, RoundingMode.DOWN));
                    } else {
                        num = StringHelper.splitDecimalStringFractions(defAccount.get().getBalance().setScale(4, RoundingMode.DOWN));
                    }

                    mBalanceCurrentState.set(num.intPart, num.fractionalPart, bips(Long.parseLong(num.intPart)));

                    mBalanceCurrentState.applyTo(getViewState());

                    mCoinsRow.setStatus(ListWithButtonRow.Status.Normal);
                    getViewState().hideRefreshProgress();
                }, t -> {
                    mCoinsRow.setStatus(ListWithButtonRow.Status.Error);
                    getViewState().hideRefreshProgress();
                });

        safeSubscribeIoToUi(
                txRepo.observe()
                        .switchMap(items -> mapAddressesInfo(myAddresses, infoRepo, items))
                        .subscribeOn(Schedulers.io())
        )
                .subscribe(res -> {
                    updateDelegation();
                    mTransactionsAdapter.dispatchChanges(HistoryTransactionDiffUtil.class, Stream.of(res).limit(5).toList(), true);
                    if (mTransactionsAdapter.getItemCount() == 0) {
                        mTransactionsRow.setStatus(ListWithButtonRow.Status.Empty);
                    } else {
                        mTransactionsRow.setStatus(ListWithButtonRow.Status.Normal);
                    }
                    getViewState().hideRefreshProgress();
                    getViewState().scrollTop();
                }, t -> {
                    mTransactionsRow.setStatus(ListWithButtonRow.Status.Error);
                    getViewState().hideRefreshProgress();
                });

        mAdapter.addRow(mTransactionsRow);
        mAdapter.addRow(mCoinsRow);
    }

    private void updateDelegation() {
        safeSubscribeIoToUi(ReactiveExplorerGate.rxExpGate(addressRepo.getDelegations(myAddresses.get(0), 0))
                .subscribeOn(Schedulers.io())).subscribe(
                res -> {
                    if (res.meta.additional.delegatedAmount.compareTo(BigDecimal.ZERO) > 0) {
                        getViewState().setDelegationAmount(bdHuman(res.meta.additional.delegatedAmount));
                    }
                },
                Timber::d);
    }

    private void onAvatarClick(View view) {
        getAnalytics().send(AppEvent.CoinsUsernameButton);
        getViewState().startTab(R.id.bottom_settings);
    }

    private void onExplorerClick(View view, HistoryTransaction historyTransaction) {
        getViewState().startExplorer(historyTransaction.hash.toString());
    }

    private void onRefresh() {
        txRepo.update(true);
        accountStorage.update(true);
    }

    private void setUsername() {
        if (session.getRole() == AuthSession.AuthType.Advanced) {
            getViewState().hideAvatar();
        } else {
            if (session.getUser().data.username == null) {
                getViewState().setUsername("");
            } else {
                getViewState().setUsername(String.format("@%s", session.getUser().data.username));
            }
        }
    }

    private void onClickConvertCoins(View view) {
        getViewState().startConvertCoins();
    }

    private void onClickStartTransactionList(View view) {
        getViewState().startTransactionList();
    }

    private void onClickStartDelegationList(View view) {
        getViewState().startDelegationList();
    }

    private boolean isMyAddress(MinterAddress address) {
        for (MinterAddress add : secretRepo.getAddresses()) {
            if (add.equals(address)) {
                return true;
            }
        }

        return false;
    }

    @Parcel
    final static class BalanceCurrentState {
        private final static String sStateBalance = "BalanceCurrentState::CURRENT";
        String mIntPart = "0";
        String mFractPart = "0000";
        String mBips = bips(0L);

        void set(String intPart, String fractPart, String bips) {
            mIntPart = intPart;
            mFractPart = fractPart;
            mBips = bips;
        }

        void applyTo(CoinsTabView view) {
            view.setBalance(mIntPart, mFractPart, mBips);
        }

        void onSaveInstanceState(Bundle outState) {
            outState.putParcelable(sStateBalance, Parcels.wrap(this));
        }

        void onRestoreInstanceState(Bundle savedInstanceState) {
            if (savedInstanceState != null && savedInstanceState.containsKey(sStateBalance)) {
                final BalanceCurrentState saved = Parcels.unwrap(savedInstanceState.getParcelable(sStateBalance));
                if (saved != null) {
                    set(saved.mIntPart, saved.mFractPart, saved.mBips);
                }
            }
        }
    }

    public final static class ItemViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.item_avatar)
        BipCircleImageView avatar;
        @BindView(R.id.item_title)
        TextView title;
        @BindView(R.id.item_amount)
        TextView amount;
        @BindView(R.id.item_subamount)
        TextView subname;

        public ItemViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

}
