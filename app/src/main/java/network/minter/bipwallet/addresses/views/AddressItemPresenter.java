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

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.View;

import com.arellomobile.mvp.InjectViewState;

import javax.inject.Inject;

import io.reactivex.schedulers.Schedulers;
import network.minter.bipwallet.addresses.AddressManageModule;
import network.minter.bipwallet.addresses.models.AddressItem;
import network.minter.bipwallet.addresses.ui.AddressItemActivity;
import network.minter.bipwallet.internal.helpers.ContextHelper;
import network.minter.bipwallet.internal.helpers.IntentHelper;
import network.minter.bipwallet.internal.mvp.MvpBasePresenter;
import network.minter.my.repo.MyAddressRepository;

import static network.minter.bipwallet.internal.ReactiveAdapter.rxCallMy;

/**
 * MinterWallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@InjectViewState
public class AddressItemPresenter extends MvpBasePresenter<AddressManageModule.AddressItemView> {
    @Inject MyAddressRepository addressRepo;

    private AddressItem mAddress;

    @Inject
    public AddressItemPresenter() {
    }

    @Override
    public void handleExtras(Intent intent) {
        super.handleExtras(intent);
        mAddress = IntentHelper.getParcelExtraOrError(intent, AddressItemActivity.EXTRA_ADDRESS_DATA, "AddressItem required");

        if (!mAddress.isServerSecured) {
            getViewState().hideActions();
            getViewState().setDescription("This address is secured by You");
        }
    }

    @Override
    public void attachView(AddressManageModule.AddressItemView view) {
        super.attachView(view);

        getViewState().setAddress(mAddress.address.toString());
        getViewState().setSecuredBy(mAddress.isServerSecured ? "Bip Wallet" : "You");
        getViewState().setOnClickDelete(this::onClickDelete);
        getViewState().setOnCopy(this::onClickCopy);
    }

    private void onClickCopy(View v) {
        ContextHelper.copyToClipboard(v.getContext(), mAddress.toString());
    }

    private void onClickDelete(View view) {
        getViewState().startRemoveDialog("Attention", "Once you have to deleted your address, it can't be restored. Are you sure to proceed?", "Yes", "No", this::onClickDeleteAddress);
    }

    private void onClickDeleteAddress(DialogInterface dialogInterface, int which) {
        getViewState().showProgress("Deleting in progress");
        if (mAddress.isServerSecured) {
            rxCallMy(addressRepo.delete(mAddress.id))
                    .observeOn(Schedulers.io())
                    .subscribeOn(Schedulers.io())
                    .subscribe(res -> {
                        getViewState().hideProgress();
                        getViewState().finishWithResult(Activity.RESULT_FIRST_USER);
                    });
        }


    }
}
