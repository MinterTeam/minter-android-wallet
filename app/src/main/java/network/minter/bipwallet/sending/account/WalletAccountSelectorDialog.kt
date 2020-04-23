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
package network.minter.bipwallet.sending.account

import android.content.Context
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.recyclerview.widget.LinearLayoutManager
import network.minter.bipwallet.R
import network.minter.bipwallet.databinding.DialogAccountSelectorBinding
import network.minter.bipwallet.internal.dialogs.WalletDialog
import network.minter.bipwallet.internal.dialogs.WalletDialogBuilder
import network.minter.bipwallet.internal.helpers.MathHelper.humanize
import network.minter.bipwallet.internal.views.list.BorderedItemSeparator
import network.minter.explorer.models.CoinBalance
import network.minter.explorer.models.CoinDelegation
import network.minter.explorer.models.ValidatorItem
import network.minter.profile.MinterProfileApi

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock></edward.vstock>@gmail.com>
 */


data class SelectorData<out Data>(
        val data: Data,
        val title: CharSequence,
        val subtitle: CharSequence? = null,
        val avatar: String? = null,
        val avatarFallback: Int = R.drawable.img_avatar_default
)

fun selectorDataFromAccounts(input: List<CoinBalance>): List<SelectorData<CoinBalance>> {
    return input.map {
        SelectorData(
                it,
                "${it.coin!!.toUpperCase()} (${it.amount.humanize()})",
                it.address!!.toShortString(),
                MinterProfileApi.getCoinAvatarUrl(it.coin!!)
        )
    }
}

fun selectorDataFromDelegatedAccounts(input: List<CoinDelegation>): List<SelectorData<CoinDelegation>> {
    return input.map {
        SelectorData(
                it,
                "${it.coin!!.toUpperCase()} (${it.amount.humanize()})",
                it.publicKey!!.toShortString(),
                MinterProfileApi.getCoinAvatarUrl(it.coin!!)
        )
    }
}

fun selectorDataFromValidators(input: List<ValidatorItem>): List<SelectorData<ValidatorItem>> {
    return input.map {
        val name = it.meta?.name ?: it.pubKey.toShortString()
        val subtitle = if (it.meta?.name.isNullOrEmpty()) null else it.pubKey.toShortString()
        SelectorData(
                it,
                name,
                subtitle,
                it.meta.iconUrl,
                R.drawable.img_avatar_delegate
        )
    }
}

class WalletAccountSelectorDialog<out Data> private constructor(
        context: Context,
        private val builder: Builder<Data>
) : WalletDialog(context) {

    private lateinit var binding: DialogAccountSelectorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogAccountSelectorBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.apply {
            title.text = builder.title
            val adapter = AccountSelectedAdapter(builder.items)
            adapter.setOnClickListener {
                builder.clickListener?.onClick(it)
                dismiss()
            }
            list.layoutManager = LinearLayoutManager(context)
            list.addItemDecoration(BorderedItemSeparator(context, R.drawable.shape_bottom_separator, false, false))
            list.adapter = adapter
        }
    }

    class Builder<Data> : WalletDialogBuilder<WalletAccountSelectorDialog<Data>, Builder<Data>> {
        var items: List<SelectorData<Data>> = ArrayList()
        var clickListener: AccountSelectedAdapter.OnClickListener<Data>? = null

        @JvmOverloads
        constructor(context: Context, title: CharSequence? = null) : super(context, title) {
        }

        constructor(context: Context, @StringRes titleRes: Int) : super(context, titleRes) {}

        override fun create(): WalletAccountSelectorDialog<Data> {
            return WalletAccountSelectorDialog(context, this)
        }

        fun setOnClickListener(listener: (SelectorData<Data>) -> Unit): Builder<Data> {
            return setOnClickListener(object : AccountSelectedAdapter.OnClickListener<Data> {
                override fun onClick(item: SelectorData<Data>) {
                    listener(item)
                }
            })
        }

        fun setOnClickListener(listener: AccountSelectedAdapter.OnClickListener<Data>): Builder<Data> {
            clickListener = listener
            return this
        }

        fun setItems(items: List<SelectorData<Data>>): Builder<Data> {
            this.items = items
            return this
        }
    }

}