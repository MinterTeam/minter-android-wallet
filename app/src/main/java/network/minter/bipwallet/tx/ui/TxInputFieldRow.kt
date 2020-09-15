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

package network.minter.bipwallet.tx.ui

import android.text.InputFilter
import android.view.View
import com.edwardstock.inputfield.InputField
import com.edwardstock.inputfield.form.DecimalInputFilter
import com.edwardstock.inputfield.form.InputGroup
import com.edwardstock.inputfield.form.validators.BaseValidator
import com.edwardstock.inputfield.form.validators.EmptyValidator
import network.minter.bipwallet.R
import network.minter.bipwallet.databinding.RowInputDecimalFieldBinding
import network.minter.bipwallet.databinding.RowInputFieldBinding
import network.minter.bipwallet.internal.helpers.forms.validators.CoinFilter
import network.minter.bipwallet.internal.helpers.forms.validators.DbCoinValidatorWithSuffix
import network.minter.bipwallet.internal.helpers.forms.validators.MinterAddressValidator
import network.minter.bipwallet.internal.helpers.forms.validators.MinterPubKeyValidator
import network.minter.bipwallet.internal.views.list.multirow.MultiRowAdapter
import network.minter.bipwallet.internal.views.list.multirow.MultiRowContract
import network.minter.blockchain.models.operational.ExternalTransaction
import network.minter.blockchain.models.operational.Operation

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
class TxInputFieldRow<T : Operation> internal constructor(
        val builder: Builder<T>
) : MultiRowContract.Row<TxInputFieldRow.ViewHolder> {

    private var inputGroup: InputGroup? = null

    class ViewHolder(itemView: View) : MultiRowAdapter.RowViewHolder(itemView)

    override fun getItemView(): Int {
        return when (builder.fieldType) {
            FieldType.Basic -> R.layout.row_input_field
            FieldType.Decimal -> R.layout.row_input_decimal_field
        }
    }

    override fun onBindViewHolder(viewHolder: ViewHolder) {
        val inputField: InputField = when (builder.fieldType) {
            FieldType.Basic -> RowInputFieldBinding.bind(viewHolder.itemView).rowInput
            FieldType.Decimal -> RowInputDecimalFieldBinding.bind(viewHolder.itemView).rowInput
        }

        inputField.apply {
            input.isFocusable = builder.enabled
            input.isFocusableInTouchMode = builder.enabled
            label = builder.label
            hint = builder.hint
            input.isEnabled = builder.enabled
            setText(builder.text)
        }

        if (inputGroup == null) {
            inputGroup = InputGroup()
            inputGroup!!.addInput(inputField)
            builder.validators.forEach {
                inputGroup!!.addValidator(inputField, it)
            }
            builder.inputCallback?.invoke(inputGroup!!, inputField)
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {

    }

    override fun getViewHolderClass(): Class<ViewHolder> {
        return ViewHolder::class.java
    }

    enum class FieldType {
        Basic,
        Decimal
    }

    class Builder<T : Operation> {
        var cls: Class<T>? = null
        var extTx: ExternalTransaction? = null
        var label: CharSequence? = null
        var hint: CharSequence? = null
        var text: CharSequence? = null
        var enabled: Boolean = false
        var fieldType: FieldType = FieldType.Basic
        private var onValidateCallback: ((Boolean) -> Unit)? = null
        internal var inputCallback: ((InputGroup, InputField) -> Unit)? = null
        internal var validators: MutableList<BaseValidator> = ArrayList()
        internal var validCallback: ((ValidState, InputField) -> Unit)? = null

        constructor()

        constructor(c: Class<T>, tx: ExternalTransaction, cb: ((ValidState, InputField) -> Unit)? = null) {
            cls = c
            extTx = tx
            validCallback = cb
        }

        fun onValidate(callback: (Boolean) -> Unit): Builder<T> {
            onValidateCallback = callback
            return this
        }

        fun addValidator(validator: BaseValidator): Builder<T> {
            validators.add(validator)
            return this
        }

        fun configureInput(cb: (InputGroup, InputField) -> Unit): Builder<T> {
            inputCallback = cb
            return this
        }

        fun tplCoin(onChanged: (T, String) -> Unit): Builder<T> {
            return tplCoin(onChanged, null)
        }

        fun tplCoin(onChanged: (T, String) -> Unit, afterChange: (() -> Unit)? = null): Builder<T> {
            configureInput { inputGroup, inputField ->
                validCallback?.invoke(ValidState.Undefined, inputField)
//                inputGroup.addFilter(inputField, InputFilter.LengthFilter(10))
                inputGroup.addFilter(inputField, CoinFilter())
                inputGroup.addValidator(inputField, DbCoinValidatorWithSuffix())
                handleTextChange(inputGroup, inputField, onChanged, afterChange)
            }
            return this
        }

        fun tplAddress(onChanged: (T, String) -> Unit): Builder<T> {
            return tplAddress(onChanged, null)
        }

        fun tplAddress(onChanged: (T, String) -> Unit, afterChange: (() -> Unit)? = null): Builder<T> {
            configureInput { inputGroup, inputField ->
                validCallback?.invoke(ValidState.Undefined, inputField)

                inputGroup.addValidator(inputField, MinterAddressValidator())
                inputGroup.addFilter(inputField, InputFilter.LengthFilter(42))
                handleTextChange(inputGroup, inputField, onChanged, afterChange)
            }
            return this
        }

        fun tplPublicKey(onChanged: (T, String) -> Unit): Builder<T> {
            return tplPublicKey(onChanged, null)
        }

        fun tplPublicKey(onChanged: (T, String) -> Unit, afterChange: (() -> Unit)? = null): Builder<T> {
            configureInput { inputGroup, inputField ->
                validCallback?.invoke(ValidState.Undefined, inputField)
                inputGroup.addValidator(inputField, MinterPubKeyValidator())
                inputGroup.addFilter(inputField, InputFilter.LengthFilter(66))
                handleTextChange(inputGroup, inputField, onChanged, afterChange)
            }
            return this
        }

        fun tplDecimal(onChanged: (T, String) -> Unit): Builder<T> {
            return tplDecimal(onChanged, null)
        }

        fun tplDecimal(onChanged: (T, String) -> Unit, afterChange: (() -> Unit)? = null): Builder<T> {
            fieldType = FieldType.Decimal
            configureInput { inputGroup, inputField ->
                validCallback?.invoke(ValidState.Undefined, inputField)

                inputGroup.addFilter(inputField, DecimalInputFilter(inputField, 18))
                inputGroup.addValidator(inputField, EmptyValidator("Value can't be empty", true))
                handleTextChange(inputGroup, inputField, onChanged, afterChange)
            }
            return this
        }

        private fun handleTextChange(inputGroup: InputGroup, inputField: InputField, onChanged: (T, String) -> Unit, afterChange: (() -> Unit)? = null) {
            inputGroup.addTextChangedListener { input, valid ->
                validCallback?.invoke(if (valid) ValidState.Valid else ValidState.Invalid, inputField)
                if (valid) {
                    val data: T = extTx!!.getData(cls!!)
                    onChanged(data, input.text.toString())
                    extTx!!.resetData(data)
                    afterChange?.invoke()
                }
            }
        }

        fun build() = TxInputFieldRow(this)
    }


    class MultiBuilder<T : Operation>(
            val cls: Class<T>,
            val extTx: ExternalTransaction
    ) {
        var builders = ArrayList<TxInputFieldRow<T>>()
        private var onValidCallback: ((Boolean) -> Unit)? = null
        private var validMap: MutableMap<String, ValidState> = HashMap()

        fun onValid(callback: (Boolean) -> Unit): MultiBuilder<T> {
            onValidCallback = callback
            return this
        }

        fun add(applier: Builder<T>.() -> Unit): MultiBuilder<T> {
            builders.add(Builder(cls, extTx) { validState, input ->
                validMap[input.label!!.toString()] = validState

                if (onValidCallback != null) {
                    val mustBeValid = validMap.size
                    var currentlyValid = 0
                    validMap.forEach {
                        if (it.value != ValidState.Invalid) {
                            currentlyValid++
                        }
                    }
                    onValidCallback!!.invoke(mustBeValid == currentlyValid)
                }

            }.apply(applier).build())
            return this
        }

        fun build(): MutableList<TxInputFieldRow<T>> = builders

    }

    enum class ValidState {
        Valid,
        Invalid,
        Undefined
    }

}

