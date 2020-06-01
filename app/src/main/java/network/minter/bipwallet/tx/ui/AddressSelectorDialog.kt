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
import android.view.WindowManager
import androidx.annotation.StringRes
import network.minter.bipwallet.R
import network.minter.bipwallet.databinding.DialogAddressSelectorBinding
import network.minter.bipwallet.internal.dialogs.WalletDialogFragment
import network.minter.bipwallet.internal.dialogs.WalletDialogFragmentBuilder
import network.minter.bipwallet.internal.helpers.ViewHelper
import network.minter.bipwallet.internal.storage.models.SecretData
import network.minter.bipwallet.sending.account.SelectorData
import network.minter.bipwallet.sending.account.WalletAccountSelectorDialog

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */


class AddressSelectorDialog(
        val builder: Builder
) : WalletDialogFragment() {

    interface OnItemSelected {
        fun onSelected(item: SelectorData<SecretData>)
    }

    private lateinit var binding: DialogAddressSelectorBinding
    private var subDialog: WalletAccountSelectorDialog<SecretData>? = null
    var item: SelectorData<SecretData>? = null
        private set

    private fun fillInput(data: SelectorData<SecretData>) {
        if (data.subtitle != null) {
            binding.inputAddress.setText("${data.title} (${data.subtitle})")
        } else {
            binding.inputAddress.setText("${data.title}")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DialogAddressSelectorBinding.inflate(layoutInflater, container, false)

        ViewHelper.setSystemBarsLightness(dialog, false)

        item = builder.items[0]
        fillInput(item!!)

        builder.bindAction(this, binding.actionConfirm, DialogInterface.BUTTON_POSITIVE)
        builder.bindAction(this, binding.actionDecline, DialogInterface.BUTTON_NEGATIVE)

        binding.title.text = builder.title
        binding.inputAddress.input.isFocusable = false
        binding.inputAddress.input.setOnClickListener {
            subDialog = WalletAccountSelectorDialog.Builder<SecretData>(context!!, R.string.dialog_title_choose_wallet)
                    .setShowTitle(false)
                    .setItems(builder.items)
                    .setOnClickListener {
                        item = it
                        fillInput(item!!)
                    }
                    .create()

            subDialog!!.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            subDialog!!.window?.setWindowAnimations(0)
            subDialog!!.addOnDismissListener {
                binding.root.animate().alpha(1f).setDuration(100).start()
            }
            binding.root.animate().alpha(0f).setDuration(100).start()
            subDialog!!.show()
        }

        return binding.root
    }


    class Builder : WalletDialogFragmentBuilder<AddressSelectorDialog, Builder> {
        internal var items: List<SelectorData<SecretData>> = ArrayList()
        internal var listener: OnItemSelected? = null
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