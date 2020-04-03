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

package network.minter.bipwallet.addressbook.views;

import android.os.Bundle;
import android.view.View;

import javax.inject.Inject;

import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import moxy.InjectViewState;
import network.minter.bipwallet.R;
import network.minter.bipwallet.addressbook.contract.AddressContactEditView;
import network.minter.bipwallet.addressbook.db.AddressBookRepository;
import network.minter.bipwallet.addressbook.models.AddressContact;
import network.minter.bipwallet.addressbook.ui.AddressContactEditDialog;
import network.minter.bipwallet.internal.helpers.IntentHelper;
import network.minter.bipwallet.internal.mvp.MvpBasePresenter;
import network.minter.core.crypto.MinterPublicKey;
import timber.log.Timber;

@InjectViewState
public class AddressContactEditPresenter extends MvpBasePresenter<AddressContactEditView> {
    @Inject AddressBookRepository repo;
    private AddressContact mContact;
    private boolean mIsNew = false;

    @Inject
    public AddressContactEditPresenter() {

    }

    @Override
    public void handleExtras(Bundle bundle) {
        super.handleExtras(bundle);

        AddressContact c = IntentHelper.getParcelExtra(bundle, AddressContactEditDialog.ARG_CONTACT);
        mContact = new AddressContact();
        if (c == null) {
            mIsNew = true;
            getViewState().setTitle(R.string.dialog_title_add_contact);

        } else {
            mContact.id = c.id;
            mContact.name = c.name;
            mContact.address = c.address;
            mContact.type = c.type;

            getViewState().setTitle(R.string.dialog_title_edit_contact);
            getViewState().setInputAddress(mContact.address);
            getViewState().setInputTitle(mContact.name);
        }
    }

    @Override
    public void attachView(AddressContactEditView view) {
        super.attachView(view);
    }

    @Override
    protected void onFirstViewAttach() {
        super.onFirstViewAttach();
        getViewState().setOnSubmitListener(this::onSubmit);
        getViewState().addFormValidatorListener(valid -> getViewState().setEnableSubmit(valid));
        getViewState().addTextChangedListener((editText, valid) -> {
            if (!valid) return;

            String s = editText.getText().toString();
            switch (editText.getId()) {
                case R.id.input_address:
                    boolean isPubKey = s.matches(MinterPublicKey.PUB_KEY_PATTERN);
                    mContact.address = s;
                    mContact.type = isPubKey ? AddressContact.AddressType.ValidatorPubKey : AddressContact.AddressType.Address;
                    break;
                case R.id.input_title:
                    mContact.name = s;
                    break;
            }
        });
    }

    private void onSubmit(View view) {
        Completable res;
        if (mIsNew) {
            res = repo.insert(mContact);
        } else {
            res = repo.update(mContact);
        }

        res.observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(() -> {
                    getViewState().submitDialog();
                    getViewState().close();
                }, Timber::e);
    }
}
