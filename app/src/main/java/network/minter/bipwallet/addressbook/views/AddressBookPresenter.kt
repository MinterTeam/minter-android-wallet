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
package network.minter.bipwallet.addressbook.views

import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import moxy.InjectViewState
import network.minter.bipwallet.R
import network.minter.bipwallet.addressbook.adapter.AddressBookAdapter
import network.minter.bipwallet.addressbook.adapter.AddressContactDiffUtilImpl
import network.minter.bipwallet.addressbook.contract.AddressBookView
import network.minter.bipwallet.addressbook.db.AddressBookRepository
import network.minter.bipwallet.addressbook.models.AddressBookItem
import network.minter.bipwallet.addressbook.models.AddressBookItemHeader
import network.minter.bipwallet.addressbook.models.AddressContact
import network.minter.bipwallet.apis.explorer.RepoValidators
import network.minter.bipwallet.internal.dialogs.ConfirmDialog
import network.minter.bipwallet.internal.helpers.HtmlCompat
import network.minter.bipwallet.internal.mvp.MvpBasePresenter
import network.minter.bipwallet.sending.ui.dialogs.TxSendSuccessDialog
import timber.log.Timber
import java.util.*
import javax.inject.Inject

@InjectViewState
class AddressBookPresenter @Inject constructor() : MvpBasePresenter<AddressBookView>() {
    @Inject lateinit var addressBookRepo: AddressBookRepository
    @Inject lateinit var validatorsRepo: RepoValidators

    private val mAdapter: AddressBookAdapter = AddressBookAdapter()

    override fun attachView(view: AddressBookView) {
        super.attachView(view)
        updateList()
        mAdapter.setOnEditContactListener { contact: AddressContact -> onEditContact(contact) }
        mAdapter.setOnDeleteContactListener { contact: AddressContact -> onDeleteContact(contact) }
        mAdapter.setOnItemClickListener { contact: AddressContact -> onSelectContact(contact) }
    }

    fun onAddAddress() {
        viewState.startAddContact({ contact ->
            viewState.startDialog { ctx ->
                TxSendSuccessDialog.Builder(ctx)
                        .setLabel(R.string.dialog_description_success_add_address)
                        .setValue(contact.name)
                        .setNegativeAction(R.string.btn_close)
                        .create()
            }
            updateList()
        }, null)
    }

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()
        viewState.setAdapter(mAdapter)
    }

    private fun onSelectContact(contact: AddressContact) {
        viewState.finishSuccess(contact)
    }

    private fun onDeleteContact(contact: AddressContact) {
        viewState.startDialog { ctx ->
            ConfirmDialog.Builder(ctx, R.string.dialog_title_remove_address)
                    .setText(HtmlCompat.fromHtml(ctx.getString(R.string.dialog_description_remove_address, contact.shortAddress)))
                    .setTextTypeface(R.font._inter_regular)
                    .setPositiveAction(R.string.btn_confirm) { d, _ ->
                        addressBookRepo.delete(contact)
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribeOn(Schedulers.io())
                                .subscribe(
                                        {
                                            updateList()
                                            d.dismiss()
                                        },
                                        { t ->
                                            Timber.e(t)
                                        }
                                )
                    }
                    .setNegativeAction(R.string.btn_cancel)
                    .create()
        }
    }

    private fun onEditContact(contact: AddressContact) {
        viewState.startEditContact(contact, { updateList() }, null)
    }

    private fun updateList() {
        addressBookRepo.findAll()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(
                        { addressContacts: List<AddressContact> ->
                            viewState.showEmpty(addressContacts.isEmpty())
                            onDataLoaded(addressContacts)
                        },
                        { t: Throwable ->
                            Timber.e(t)
                        }
                )
    }

    private fun onDataLoaded(addressContacts: List<AddressContact>) {
        var lastLetter: String? = null
        val out: MutableList<AddressBookItem> = ArrayList()
        for (contact in addressContacts) {
            if (lastLetter == null) {
                lastLetter = contact.name!!.substring(0, 1)
                out.add(AddressBookItemHeader(lastLetter))
            } else if (lastLetter != contact.name!!.substring(0, 1)) {
                lastLetter = contact.name!!.substring(0, 1)
                out.add(AddressBookItemHeader(lastLetter))
            }
            out.add(contact)
        }
        mAdapter.dispatchChanges(AddressContactDiffUtilImpl::class.java, out, true)
    }

}