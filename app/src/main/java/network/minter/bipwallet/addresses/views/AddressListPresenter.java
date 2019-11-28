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

package network.minter.bipwallet.addresses.views;

import android.content.Intent;
import android.view.View;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.paging.PagedList;
import androidx.paging.RxPagedListBuilder;
import io.reactivex.disposables.Disposable;
import moxy.InjectViewState;
import network.minter.bipwallet.addresses.adapters.AddressListAdapter;
import network.minter.bipwallet.addresses.adapters.AddressListFactory;
import network.minter.bipwallet.addresses.contract.AddressListView;
import network.minter.bipwallet.addresses.models.AddressItem;
import network.minter.bipwallet.advanced.repo.SecretStorage;
import network.minter.bipwallet.analytics.AppEvent;
import network.minter.bipwallet.analytics.base.HasAnalyticsEvent;
import network.minter.bipwallet.internal.auth.AuthSession;
import network.minter.bipwallet.internal.mvp.MvpBasePresenter;
import network.minter.explorer.repo.ExplorerAddressRepository;
import network.minter.profile.repo.ProfileAddressRepository;

import static network.minter.bipwallet.apis.reactive.ReactiveMyMinter.rxProfile;

/**
 * minter-android-wallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@InjectViewState
public class AddressListPresenter extends MvpBasePresenter<AddressListView> implements HasAnalyticsEvent {
    private final static int REQUEST_ADDRESS_ITEM = 100;
    private final static int REQUEST_FOR_RESULT = 200;
    @Inject SecretStorage secretRepo;
    @Inject AuthSession session;
    @Inject ProfileAddressRepository myAddressRepo;
    @Inject ExplorerAddressRepository explorerAddressRepository;
    private AddressListAdapter mAdapter;
    private RxPagedListBuilder<Integer, AddressItem> mPageBuilder;
    private Disposable mPageDisposable;

    @Inject
    public AddressListPresenter() {
        mAdapter = new AddressListAdapter();
        mAdapter.setOnAddressClickListener((v, name, address) -> getViewState().startAddressItem(REQUEST_ADDRESS_ITEM, name, address));
        mAdapter.setOnSetMainListener(this::onSetMain);
        mAdapter.setOnBalanceClickListener(this::onClickBalance);
    }

    @NonNull
    @Override
    public AppEvent getAnalyticsEvent() {
        return AppEvent.AddressesScreen;
    }

    private void onClickBalance(View view, AddressItem addressItem) {
        getViewState().startTransactionsList();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        reload();
    }

    public void onClickAddAddress() {
        if (session.isAdvancedUser()) {
            getViewState().startCreateAddress(REQUEST_FOR_RESULT);
        } else {
            /*
//            getViewState().showProgress("Please, wait", "Creating address...");
            final SecretData secretData = SecretStorage.generateAddress();
            secretStorage.add(secretData);
            boolean isMain = Stream.of(mItems).filter(item -> item.isMain).count() == 0;
            safeSubscribeIoToUi(
                    rxProfile(myAddressRepo.addAddress(secretData.toAddressData(isMain, true, secretStorage.getEncryptionKey())))
            ).calculate(res -> {
                reload();
                getViewState().hideProgress();
//                mAdapter.clear();
//                loadAddresses();
            });
            */
        }
    }

    @Override
    public void attachView(AddressListView view) {
        super.attachView(view);
        getViewState().setAdapter(mAdapter);
    }

    @Override
    protected void onFirstViewAttach() {
        super.onFirstViewAttach();

        final AddressListFactory pageFactory;
        final PagedList.Config cfg;
        if (session.getRole() == AuthSession.AuthType.Basic) {
            pageFactory = new AddressListFactory(myAddressRepo, explorerAddressRepository);
            cfg = new PagedList.Config.Builder()
                    .setEnablePlaceholders(false)
                    .setPageSize(20)
                    .build();
        } else {
            pageFactory = new AddressListFactory(secretRepo, explorerAddressRepository);
            cfg = new PagedList.Config.Builder()
                    .setEnablePlaceholders(false)
                    .setPageSize(secretRepo.getSecrets().size())
                    .build();
        }

        mPageBuilder = new RxPagedListBuilder<>(pageFactory, cfg);


        getViewState().showProgress();
        reload();
    }

    private void reload() {
        if (mPageDisposable != null && !mPageDisposable.isDisposed()) {
            mPageDisposable.dispose();
            getViewState().scrollToPosition(0);
        }

        mPageDisposable = mPageBuilder.buildObservable()
                .subscribe(res -> {
                    getViewState().hideProgress();
                    mAdapter.submitList(res);
                });
    }

    private void onSetMain(boolean isMain, AddressItem addressItem) {
        if (!addressItem.isServerSecured || addressItem.profileAddressData == null) {
            return;
        }

        safeSubscribeIoToUi(rxProfile(myAddressRepo.setAddressMain(isMain, addressItem.profileAddressData)))
                .subscribe(res -> {
                    getViewState().scrollToPosition(0);
                    reload();
                });

    }

}
