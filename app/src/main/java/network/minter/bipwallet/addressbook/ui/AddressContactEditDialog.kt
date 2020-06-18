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

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.edwardstock.inputfield.form.InputGroup
import com.edwardstock.inputfield.form.InputWrapper
import com.edwardstock.inputfield.form.validators.LengthValidator
import com.edwardstock.inputfield.form.validators.RegexValidator
import dagger.android.support.AndroidSupportInjection
import moxy.presenter.InjectPresenter
import moxy.presenter.ProvidePresenter
import network.minter.bipwallet.addressbook.contract.AddressContactEditView
import network.minter.bipwallet.addressbook.models.AddressContact
import network.minter.bipwallet.addressbook.views.AddressContactEditPresenter
import network.minter.bipwallet.databinding.DialogAddresscontactEditBinding
import network.minter.bipwallet.internal.dialogs.BaseBottomSheetDialogFragment
import network.minter.bipwallet.internal.helpers.ViewHelper
import network.minter.bipwallet.internal.helpers.forms.validators.MinterAddressValidator
import network.minter.bipwallet.internal.helpers.forms.validators.TitleInputFilter
import network.minter.bipwallet.internal.helpers.forms.validators.UniqueContactNameValidator
import network.minter.bipwallet.internal.views.list.ViewElevationOnScrollNestedScrollView
import org.parceler.Parcels
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Provider

class AddressContactEditDialog : BaseBottomSheetDialogFragment(), AddressContactEditView {

    companion object {
        const val ARG_CONTACT = "ARG_CONTACT"
        const val ARG_ADDRESS = "ARG_ADDRESS"
    }

    @Inject lateinit var presenterProvider: Provider<AddressContactEditPresenter>
    @InjectPresenter lateinit var presenter: AddressContactEditPresenter

    private lateinit var binding: DialogAddresscontactEditBinding
    private var inputGroup: InputGroup = InputGroup()
    var onContactAddedOrUpdated: ((AddressContact) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidSupportInjection.inject(this)
        super.onCreate(savedInstanceState)
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DialogAddresscontactEditBinding.inflate(inflater, container, false)

        inputGroup.enableInputDebounce = false
        inputGroup.clearErrorBeforeValidate = false

        binding.scroll.setOnScrollChangeListener(ViewElevationOnScrollNestedScrollView(binding.dialogTop))

        inputGroup.addInput(binding.inputAddress)
        inputGroup.addValidator(binding.inputAddress, MinterAddressValidator())

        inputGroup.addInput(binding.inputTitle)
        inputGroup.addFilter(binding.inputTitle, TitleInputFilter())
        inputGroup.addValidator(binding.inputTitle, RegexValidator("^[^\\s].*$").apply {
            errorMessage = "Title can't starts from whitespace"
            isRequired = false
        })

        inputGroup.addValidator(binding.inputTitle, LengthValidator(1, 18).apply {
            errorMessage = "Title length should be from 1 to 18 symbols"
        })

        binding.inputAddress.input.setOnClickListener { ViewHelper.tryToPasteMinterAddressFromCB(it, binding.inputAddress.input) }

        presenter.handleExtras(arguments)

        return binding.root
    }

    override fun setUniqueValidatorForAddress(exclude: String?) {
        inputGroup.addValidator(binding.inputAddress, UniqueContactNameValidator(exclude, UniqueContactNameValidator.FindBy.Address).apply {
            errorMessage = "Contact with this address already exist"
        })
    }

    override fun setUniqueValidatorForTitle(exclude: String?) {
        inputGroup.addValidator(binding.inputTitle, UniqueContactNameValidator(exclude, UniqueContactNameValidator.FindBy.Name).apply {
            errorMessage = "Contact with this title already exist"
        })
    }

    override fun validate() {
        inputGroup.validate(true).subscribe(
                { _ ->

                },
                { t ->
                    Timber.e(t, "Unable to validate form")
                }
        )
    }

    override fun setOnSubmitListener(listener: View.OnClickListener) {
        binding.actionSubmit.setOnClickListener(listener)
    }

    override fun addTextChangedListener(listener: (input: InputWrapper, valid: Boolean) -> Unit) {
        inputGroup.addTextChangedListener(listener)
    }

    override fun addFormValidatorListener(listener: (Boolean) -> Unit) {
        inputGroup.addFormValidateListener(listener)
    }

    override fun setEnableSubmit(enable: Boolean) {
        binding.actionSubmit.isEnabled = enable
    }

    override fun close() {
        dismiss()
    }

    override fun setInputAddress(address: String?) {
        binding.inputAddress.setText(address)
    }

    override fun setInputTitle(title: String?) {
        binding.inputTitle.setText(title)
    }

    override fun submitDialog(contact: AddressContact) {
        onContactAddedOrUpdated?.invoke(contact)
    }

    override fun setTitle(titleRes: Int) {
        binding.dialogTitle.setText(titleRes)
    }

    @ProvidePresenter
    fun providePresenter(): AddressContactEditPresenter {
        return presenterProvider.get()
    }

    class Builder {
        private val args = Bundle()
        fun setAddress(address: String?): Builder {
            args.putString(ARG_ADDRESS, address)
            return this
        }

        fun setContact(contact: AddressContact?): Builder {
            if (contact != null) {
                args.putParcelable(ARG_CONTACT, Parcels.wrap(contact))
            }
            return this
        }

        fun build(): AddressContactEditDialog {
            val dialog = AddressContactEditDialog()
            dialog.arguments = args
            return dialog
        }
    }


}