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

package network.minter.bipwallet.addressbook.ui;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import org.parceler.Parcels;

import javax.inject.Inject;
import javax.inject.Provider;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import moxy.presenter.InjectPresenter;
import moxy.presenter.ProvidePresenter;
import network.minter.bipwallet.R;
import network.minter.bipwallet.addressbook.contract.AddressBookView;
import network.minter.bipwallet.addressbook.models.AddressContact;
import network.minter.bipwallet.addressbook.views.AddressBookPresenter;
import network.minter.bipwallet.internal.BaseMvpInjectActivity;
import network.minter.bipwallet.internal.dialogs.BaseBottomSheetDialog;
import network.minter.bipwallet.internal.system.ActivityBuilder;

public class AddressBookActivity extends BaseMvpInjectActivity implements AddressBookView {
    public final static String EXTRA_CONTACT = "EXTRA_CONTACT";
    @Inject Provider<AddressBookPresenter> presenterProvider;
    @InjectPresenter AddressBookPresenter presenter;
    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.list) RecyclerView list;

    public static AddressContact getResult(Intent data) {
        if (!data.hasExtra(EXTRA_CONTACT)) {
            return null;
        }

        return Parcels.unwrap(data.getParcelableExtra(EXTRA_CONTACT));
    }

    @Override
    public void setAdapter(RecyclerView.Adapter<?> adapter) {
        list.setAdapter(adapter);
    }

    @Override
    public void startAddContact(BaseBottomSheetDialog.OnSubmitListener onSubmit, BaseBottomSheetDialog.OnDismissListener onDismiss) {
        AddressContactEditDialog dialog = new AddressContactEditDialog.Builder().build();
        dialog.setOnSubmitListener(onSubmit);
        dialog.setOnDismissListener(onDismiss);
        startBottomDialog(dialog, "add_contact");
    }

    @Override
    public void startEditContact(AddressContact contact, BaseBottomSheetDialog.OnSubmitListener onSubmit, BaseBottomSheetDialog.OnDismissListener onDismiss) {
        AddressContactEditDialog dialog = new AddressContactEditDialog.Builder().setContact(contact).build();
        dialog.setOnSubmitListener(onSubmit);
        dialog.setOnDismissListener(onDismiss);
        startBottomDialog(dialog, "edit_contact");
    }

    @Override
    public void finishSuccess(AddressContact contact) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_CONTACT, Parcels.wrap(contact));
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_add_address) {
            presenter.onAddAddress();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_address_book, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @ProvidePresenter
    AddressBookPresenter presenterProvider() {
        return presenterProvider.get();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address_book);
        ButterKnife.bind(this);
        setupToolbar(toolbar);
        setResult(RESULT_CANCELED);

        list.setLayoutManager(new LinearLayoutManager(this));
        presenter.handleExtras(getIntent());

        toolbar.setOnMenuItemClickListener(this::onOptionsItemSelected);
    }

    public static class Builder extends ActivityBuilder {
        private AddressContact mContact = null;

        public Builder(@NonNull Activity from) {
            super(from);
        }

        public Builder(@NonNull Fragment from) {
            super(from);
        }

        public Builder(@NonNull Service from) {
            super(from);
        }

        @Override
        protected void onBeforeStart(Intent intent) {
            super.onBeforeStart(intent);
            if (mContact != null) {
                intent.putExtra(EXTRA_CONTACT, Parcels.wrap(mContact));
            }
        }

        @Override
        protected Class<?> getActivityClass() {
            return AddressBookActivity.class;
        }
    }
}
