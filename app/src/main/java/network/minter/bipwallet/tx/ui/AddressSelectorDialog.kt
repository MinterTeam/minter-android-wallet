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

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListPopupWindow
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import network.minter.bipwallet.R
import network.minter.bipwallet.databinding.DialogAddressSelectorBinding
import network.minter.bipwallet.internal.dialogs.WalletDialogFragment
import network.minter.bipwallet.internal.dialogs.WalletDialogFragmentBuilder
import network.minter.bipwallet.internal.helpers.ViewHelper
import network.minter.bipwallet.internal.storage.models.SecretData
import network.minter.bipwallet.sending.account.SelectorData

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */


class AddressSelectorDialog(
        private val builder: Builder
) : WalletDialogFragment() {
    private lateinit var binding: DialogAddressSelectorBinding
    private var popup: ListPopupWindow? = null
    var item: SelectorData<SecretData>? = null
        private set

    private fun fillInput(data: SelectorData<SecretData>) {
        if (data.subtitle != null) {
            binding.inputAddress.setText("${data.title} (${data.subtitle})")
        } else {
            binding.inputAddress.setText("${data.title}")
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        popup?.dismiss()
        super.onDismiss(dialog)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DialogAddressSelectorBinding.inflate(layoutInflater, container, false)

        ViewHelper.setSystemBarsLightness(dialog, false)

        item = builder.items[0]
        fillInput(item!!)

        builder.bindAction(this, binding.actionConfirm, DialogInterface.BUTTON_POSITIVE)
        builder.bindAction(this, binding.actionDecline, DialogInterface.BUTTON_NEGATIVE)

        val selectOnClick = View.OnClickListener {
            popup = ListPopupWindow(context!!)
            popup!!.anchorView = binding.inputAddress.input
            popup!!.width = binding.inputAddress.input.width
            popup!!.height = ViewGroup.LayoutParams.WRAP_CONTENT

            val arItems: Array<String> = builder.items.map {
                if (it.subtitle != null) {
                    "${it.title} (${it.subtitle})"
                } else {
                    "${it.title}"
                }
            }.toTypedArray()

            val arAdapter = ArrayAdapter(context!!, R.layout.list_item_textview, R.id.text, arItems)

            popup!!.setAdapter(arAdapter)
            popup!!.isModal = true
            popup!!.setOnItemClickListener { _, _, position, _ ->
                item = builder.items[position]
                fillInput(item!!)
                popup?.dismiss()
            }

            popup!!.setBackgroundDrawable(ContextCompat.getDrawable(context!!, R.drawable.shape_rounded_white))
            popup!!.show()
        }

        binding.title.text = builder.title
        binding.inputAddress.input.isFocusable = false
        binding.inputAddress.input.setOnClickListener(selectOnClick)
        binding.inputAddress.setOnSuffixImageClickListener(selectOnClick)

        return binding.root
    }


    class Builder : WalletDialogFragmentBuilder<AddressSelectorDialog, Builder> {
        internal var items: List<SelectorData<SecretData>> = ArrayList()
        internal var desc: CharSequence? = null

        @JvmOverloads
        constructor(context: Context, title: CharSequence? = null) : super(context, title)

        constructor(context: Context, @StringRes titleRes: Int) : super(context, titleRes)

        fun setItems(data: List<SelectorData<SecretData>>): Builder {
            items = data
            return this
        }

        fun setDescription(resId: Int): Builder {
            desc = context.getString(resId)
            return this
        }

        override fun create(): AddressSelectorDialog {
            return AddressSelectorDialog(this)
        }

    }
}