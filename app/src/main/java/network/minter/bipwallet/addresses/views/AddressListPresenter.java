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

package network.minter.bipwallet.addresses.views;

import android.arch.paging.PagedList;
import android.arch.paging.RxPagedListBuilder;
import android.content.Intent;

import com.arellomobile.mvp.InjectViewState;

import javax.inject.Inject;

import io.reactivex.disposables.Disposable;
import network.minter.bipwallet.addresses.AddressManageModule;
import network.minter.bipwallet.addresses.adapters.AddressListAdapter;
import network.minter.bipwallet.addresses.adapters.AddressListFactory;
import network.minter.bipwallet.addresses.models.AddressItem;
import network.minter.bipwallet.advanced.repo.SecretStorage;
import network.minter.bipwallet.internal.auth.AuthSession;
import network.minter.bipwallet.internal.mvp.MvpBasePresenter;
import network.minter.explorerapi.repo.ExplorerAddressRepository;
import network.minter.my.repo.MyAddressRepository;

import static network.minter.bipwallet.internal.ReactiveAdapter.rxCallMy;

/**
 * MinterWallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@InjectViewState
public class AddressListPresenter extends MvpBasePresenter<AddressManageModule.AddressListView> {
    private final static int REQUEST_ADDRESS_ITEM = 100;
    private final static int REQUEST_FOR_RESULT = 200;
    @Inject SecretStorage secretRepo;
    @Inject AuthSession session;
    @Inject MyAddressRepository myAddressRepo;
    @Inject ExplorerAddressRepository explorerAddressRepository;
    private AddressListAdapter mAdapter;
    private RxPagedListBuilder<Integer, AddressItem> mPageBuilder;
    private Disposable mPageDisposable;

    @Inject
    public AddressListPresenter() {
        mAdapter = new AddressListAdapter();
        mAdapter.setOnAddressClickListener((v, address) -> getViewState().startAddressItem(REQUEST_ADDRESS_ITEM, address));
        mAdapter.setOnSetMainListener(this::onSetMain);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        reload();
    }

    public void onClickAddAddress() {
        if (session.getRole() == AuthSession.AuthType.Advanced) {
            getViewState().startCreateAddress(REQUEST_FOR_RESULT);
        } else {
            /*
//            getViewState().showProgress("Please, wait", "Creating address...");
            final SecretData secretData = SecretStorage.generateAddress();
            secretStorage.add(secretData);
            boolean isMain = Stream.of(mItems).filter(item -> item.isMain).count() == 0;
            safeSubscribeIoToUi(
                    rxCallMy(myAddressRepo.addAddress(secretData.toAddressData(isMain, true, secretStorage.getEncryptionKey())))
            ).subscribe(res -> {
                reload();
                getViewState().hideProgress();
//                mAdapter.clear();
//                loadAddresses();
            });
            */
        }
    }

    @Override
    public void attachView(AddressManageModule.AddressListView view) {
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
        if (!addressItem.isServerSecured || addressItem.myAddressData == null) {
            return;
        }

        safeSubscribeIoToUi(rxCallMy(myAddressRepo.setAddressMain(isMain, addressItem.myAddressData)))
                .subscribe(res -> {
                    getViewState().scrollToPosition(0);
                    reload();
                });

    }

}
