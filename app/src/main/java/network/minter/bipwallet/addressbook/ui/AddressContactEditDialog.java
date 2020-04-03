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

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputEditText;

import org.parceler.Parcels;

import javax.inject.Inject;
import javax.inject.Provider;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import butterknife.BindView;
import butterknife.ButterKnife;
import dagger.android.support.AndroidSupportInjection;
import moxy.presenter.InjectPresenter;
import moxy.presenter.ProvidePresenter;
import network.minter.bipwallet.R;
import network.minter.bipwallet.addressbook.contract.AddressContactEditView;
import network.minter.bipwallet.addressbook.models.AddressContact;
import network.minter.bipwallet.addressbook.views.AddressContactEditPresenter;
import network.minter.bipwallet.internal.dialogs.BaseBottomSheetDialog;
import network.minter.bipwallet.internal.helpers.forms.InputGroup;
import network.minter.bipwallet.internal.helpers.forms.validators.EmptyValidator;
import network.minter.bipwallet.internal.helpers.forms.validators.LengthValidator;
import network.minter.bipwallet.internal.helpers.forms.validators.MinterUsernameValidator;
import network.minter.bipwallet.internal.helpers.forms.validators.RegexValidator;
import network.minter.core.crypto.MinterAddress;
import network.minter.core.crypto.MinterPublicKey;

import static network.minter.bipwallet.internal.helpers.ViewHelper.tryToPasteMinterAddressFromCB;

public class AddressContactEditDialog extends BaseBottomSheetDialog implements AddressContactEditView {
    public final static String ARG_CONTACT = "ARG_CONTACT";

    @Inject Provider<AddressContactEditPresenter> presenterProvider;
    @InjectPresenter AddressContactEditPresenter presenter;
    @BindView(R.id.dialog_title) TextView title;
    @BindView(R.id.input_address) TextInputEditText inputAddress;
    @BindView(R.id.input_title) TextInputEditText inputTitle;
    @BindView(R.id.error_text_address) TextView errorTextAddress;
    @BindView(R.id.error_text_title) TextView errorTextTitle;
    @BindView(R.id.action_submit) Button actionSubmit;
    private InputGroup mInputGroup;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidSupportInjection.inject(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        AndroidSupportInjection.inject(this);
        super.onAttach(context);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_addresscontact_edit, container, false);
        ButterKnife.bind(this, view);
        presenter.handleExtras(getArguments());
        mInputGroup = new InputGroup();
        mInputGroup.addInput(inputAddress);
        mInputGroup.addInput(inputTitle);

        mInputGroup.addValidator(inputAddress,
                new RegexValidator(
                        // address or username with @ at begin or email
                        String.format("%s|%s", MinterAddress.ADDRESS_PATTERN + "|" + MinterPublicKey.PUB_KEY_PATTERN, MinterUsernameValidator.PATTERN),
                        "Incorrect recipient format"
                ));
        mInputGroup.addValidator(inputTitle, new EmptyValidator("Title can't be empty"));
        mInputGroup.addValidator(inputTitle, new LengthValidator("Title length can't be more than 18 symbols", 0, 18));

        mInputGroup.setErrorView(inputAddress, errorTextAddress);
        mInputGroup.setErrorView(inputTitle, errorTextTitle);

        inputAddress.setOnClickListener(v -> tryToPasteMinterAddressFromCB(v, inputAddress));

        return view;
    }

    @Override
    public void setOnSubmitListener(View.OnClickListener listener) {
        actionSubmit.setOnClickListener(listener);
    }

    @Override
    public void addTextChangedListener(InputGroup.OnTextChangedListener listener) {
        mInputGroup.addTextChangedListener(listener);
    }

    @Override
    public void addFormValidatorListener(InputGroup.OnFormValidateListener listener) {
        mInputGroup.addFormValidateListener(listener);
    }

    @Override
    public void setEnableSubmit(boolean enable) {
        actionSubmit.setEnabled(enable);
    }

    @Override
    public void close() {
        dismiss();
    }

    @Override
    public void setInputAddress(String address) {
        inputAddress.setText(address);
    }

    @Override
    public void setInputTitle(String title) {
        inputTitle.setText(title);
    }

    @Override
    public void submitDialog() {
        if (mOnSubmitListener != null) {
            mOnSubmitListener.onSubmit();
        }
    }

    @Override
    public void setTitle(int titleRes) {
        title.setText(titleRes);
    }

    @ProvidePresenter
    AddressContactEditPresenter providePresenter() {
        return presenterProvider.get();
    }

    public static class Builder {
        private Bundle mArgs = new Bundle();
        private AddressContact mContact;

        public Builder() {

        }

        public Builder setContact(AddressContact contact) {
            if (contact != null) {
                mArgs.putParcelable(ARG_CONTACT, Parcels.wrap(contact));
            }

            return this;
        }

        public AddressContactEditDialog build() {
            AddressContactEditDialog dialog = new AddressContactEditDialog();
            dialog.setArguments(mArgs);
            return dialog;
        }
    }
}
