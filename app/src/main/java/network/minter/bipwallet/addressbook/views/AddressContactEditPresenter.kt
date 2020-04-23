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

import android.os.Bundle
import android.view.View
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import moxy.InjectViewState
import network.minter.bipwallet.R
import network.minter.bipwallet.addressbook.contract.AddressContactEditView
import network.minter.bipwallet.addressbook.db.AddressBookRepository
import network.minter.bipwallet.addressbook.models.AddressContact
import network.minter.bipwallet.addressbook.ui.AddressContactEditDialog
import network.minter.bipwallet.internal.helpers.IntentHelper.getParcelExtra
import network.minter.bipwallet.internal.mvp.MvpBasePresenter
import network.minter.core.crypto.MinterPublicKey
import timber.log.Timber
import javax.inject.Inject

@InjectViewState
class AddressContactEditPresenter @Inject constructor() : MvpBasePresenter<AddressContactEditView>() {
    @Inject lateinit var repo: AddressBookRepository

    private var mContact: AddressContact? = null
    private var mIsNew = false

    override fun handleExtras(bundle: Bundle?) {
        super.handleExtras(bundle)
        val c = getParcelExtra<AddressContact>(bundle, AddressContactEditDialog.ARG_CONTACT)
        mContact = AddressContact()
        if (c == null) {
            mIsNew = true
            viewState.setTitle(R.string.dialog_title_add_contact)
            if (bundle!!.containsKey(AddressContactEditDialog.ARG_ADDRESS)) {
                viewState.setInputAddress(bundle.getString(AddressContactEditDialog.ARG_ADDRESS, null))
            }
        } else {
            mContact!!.apply {
                id = c.id
                name = c.name
                address = c.address
                type = c.type
            }

            viewState.setTitle(R.string.dialog_title_edit_contact)
            viewState.setInputAddress(mContact!!.address)
            viewState.setInputTitle(mContact!!.name)
        }
    }

    override fun attachView(view: AddressContactEditView) {
        super.attachView(view)
    }

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()
        viewState.setOnSubmitListener(View.OnClickListener { view: View ->
            onSubmit(view)
        })
        viewState.addFormValidatorListener { valid: Boolean ->
            viewState.setEnableSubmit(valid)
        }
        viewState.addTextChangedListener { input, valid ->
            if (!valid) return@addTextChangedListener
            val s = input.text.toString()
            when (input.id) {
                R.id.input_address -> {
                    val isPubKey = s.matches(MinterPublicKey.PUB_KEY_PATTERN.toRegex())
                    mContact!!.address = s
                    mContact!!.type = if (isPubKey) AddressContact.AddressType.ValidatorPubKey else AddressContact.AddressType.Address
                }
                R.id.input_title -> mContact!!.name = s
            }
        }
    }

    private fun onSubmit(view: View) {
        val res: Completable = if (mIsNew) {
            repo.insert(mContact)
        } else {
            repo.update(mContact)
        }

        res.observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe({
                    viewState.submitDialog()
                    viewState.close()
                }) { t: Throwable? -> Timber.e(t) }
    }
}