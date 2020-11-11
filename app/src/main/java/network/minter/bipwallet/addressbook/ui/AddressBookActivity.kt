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
package network.minter.bipwallet.addressbook.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import moxy.presenter.InjectPresenter
import moxy.presenter.ProvidePresenter
import network.minter.bipwallet.R
import network.minter.bipwallet.addressbook.adapter.AddressBookAdapter
import network.minter.bipwallet.addressbook.contract.AddressBookView
import network.minter.bipwallet.addressbook.models.AddressContact
import network.minter.bipwallet.addressbook.views.AddressBookPresenter
import network.minter.bipwallet.databinding.ActivityAddressBookBinding
import network.minter.bipwallet.internal.BaseMvpInjectActivity
import network.minter.bipwallet.internal.dialogs.ActionListener
import network.minter.bipwallet.internal.helpers.ViewExtensions.visible
import network.minter.bipwallet.internal.system.ActivityBuilder
import org.parceler.Parcels
import javax.inject.Inject
import javax.inject.Provider

class AddressBookActivity : BaseMvpInjectActivity(), AddressBookView {
    @Inject lateinit var presenterProvider: Provider<AddressBookPresenter>
    @InjectPresenter lateinit var presenter: AddressBookPresenter

    private lateinit var binding: ActivityAddressBookBinding

    override fun setAdapter(adapter: RecyclerView.Adapter<*>) {
        binding.list.adapter = adapter
    }

    override fun showEmpty(show: Boolean) {
        binding.emptyText.visible = show
    }

    override fun startAddContact(onSubmit: (AddressContact) -> Unit, onDismiss: ActionListener?) {
        val dialog = AddressContactEditDialog.Builder().build()
        dialog.onContactAddedOrUpdated = onSubmit
        dialog.onDismissListener = onDismiss
        startBottomDialog(dialog, "add_contact")
    }

    override fun startEditContact(contact: AddressContact, onSubmit: (AddressContact) -> Unit, onDismiss: ActionListener?) {
        val dialog = AddressContactEditDialog.Builder().setContact(contact).build()
        dialog.onContactAddedOrUpdated = onSubmit
        dialog.onDismissListener = onDismiss
        startBottomDialog(dialog, "edit_contact")
    }

    override fun finishSuccess(contact: AddressContact) {
        val intent = Intent()
        intent.putExtra(EXTRA_CONTACT, Parcels.wrap(contact))
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_add_address) {
            presenter.onAddAddress()
        }
        (binding.list.adapter as AddressBookAdapter).closeOpened()
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_address_book, menu)
        return super.onCreateOptionsMenu(menu)
    }

    @ProvidePresenter
    fun presenterProvider(): AddressBookPresenter {
        return presenterProvider.get()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddressBookBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar(binding.toolbar)


        setResult(Activity.RESULT_CANCELED)
        binding.list.layoutManager = LinearLayoutManager(this)
        binding.list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                (binding.list.adapter as AddressBookAdapter).closeOpened()
            }
        })
        binding.list.setOnTouchListener { _, _ ->
            (binding.list.adapter as AddressBookAdapter).closeOpened()
            false
        }

        presenter.handleExtras(intent)
        binding.toolbar.setOnMenuItemClickListener { item: MenuItem -> onOptionsItemSelected(item) }
    }

    class Builder : ActivityBuilder {
        private val mContact: AddressContact? = null

        constructor(from: Activity) : super(from)
        constructor(from: Fragment) : super(from)
        constructor(from: Service) : super(from)

        override fun onBeforeStart(intent: Intent) {
            super.onBeforeStart(intent)
            if (mContact != null) {
                intent.putExtra(EXTRA_CONTACT, Parcels.wrap(mContact))
            }
        }

        override fun getActivityClass(): Class<*> {
            return AddressBookActivity::class.java
        }
    }

    companion object {
        const val EXTRA_CONTACT = "EXTRA_CONTACT"
        fun getResult(data: Intent): AddressContact? {
            return if (!data.hasExtra(EXTRA_CONTACT)) {
                null
            } else Parcels.unwrap<AddressContact>(data.getParcelableExtra(EXTRA_CONTACT))
        }
    }
}