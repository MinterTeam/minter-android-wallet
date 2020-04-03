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

import android.content.Intent;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import moxy.InjectViewState;
import network.minter.bipwallet.addressbook.adapter.AddressBookAdapter;
import network.minter.bipwallet.addressbook.adapter.AddressContactDiffUtilImpl;
import network.minter.bipwallet.addressbook.contract.AddressBookView;
import network.minter.bipwallet.addressbook.db.AddressBookRepository;
import network.minter.bipwallet.addressbook.models.AddressBookItem;
import network.minter.bipwallet.addressbook.models.AddressBookItemHeader;
import network.minter.bipwallet.addressbook.models.AddressContact;
import network.minter.bipwallet.internal.mvp.MvpBasePresenter;
import timber.log.Timber;

@InjectViewState
public class AddressBookPresenter extends MvpBasePresenter<AddressBookView> {

    @Inject AddressBookRepository addressBookRepo;

    private AddressBookAdapter mAdapter;

    @Inject
    public AddressBookPresenter() {
        mAdapter = new AddressBookAdapter();
    }

    @Override
    public void attachView(AddressBookView view) {
        super.attachView(view);

        updateList();
        mAdapter.setOnEditContactListener(this::onEditContact);
        mAdapter.setOnDeleteContactListener(this::onDeleteContact);
        mAdapter.setOnItemClickListener(this::onSelectContact);
    }

    @Override
    public void handleExtras(Intent intent) {
        super.handleExtras(intent);
    }

    public void onAddAddress() {
        getViewState().startAddContact(this::updateList, null);
    }

    @Override
    protected void onFirstViewAttach() {
        super.onFirstViewAttach();
        getViewState().setAdapter(mAdapter);
    }

    private void onSelectContact(AddressContact contact) {
        getViewState().finishSuccess(contact);
    }

    private void onDeleteContact(AddressContact contact) {
        addressBookRepo.delete(contact);
        updateList();
    }

    private void onEditContact(AddressContact contact) {
        getViewState().startEditContact(contact, this::updateList, null);
    }

    private void updateList() {
        addressBookRepo.findAll()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(this::onDataLoaded, t -> {
                    Timber.e(t);
                });
    }

    private void onDataLoaded(List<AddressContact> addressContacts) {
        String lastLetter = null;
        List<AddressBookItem> out = new ArrayList<>();
        for (AddressContact contact : addressContacts) {
            if (lastLetter == null) {
                lastLetter = contact.name.substring(0, 1);
                out.add(new AddressBookItemHeader(lastLetter));
            } else if (!lastLetter.equals(contact.name.substring(0, 1))) {
                lastLetter = contact.name.substring(0, 1);
                out.add(new AddressBookItemHeader(lastLetter));
            }

            out.add(contact);
        }

        mAdapter.dispatchChanges(AddressContactDiffUtilImpl.class, out, true);
    }
}
