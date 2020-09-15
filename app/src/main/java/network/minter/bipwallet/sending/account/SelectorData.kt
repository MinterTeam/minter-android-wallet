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

import network.minter.bipwallet.R
import network.minter.bipwallet.apis.reactive.avatar
import network.minter.bipwallet.internal.helpers.MathHelper.humanize
import network.minter.bipwallet.internal.storage.models.SecretData
import network.minter.explorer.models.CoinBalance
import network.minter.explorer.models.CoinDelegation
import network.minter.explorer.models.ValidatorItem

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */

data class SelectorData<out Data>(
        val data: Data,
        val title: CharSequence,
        val subtitle: CharSequence? = null,
        val avatar: String? = null,
        val avatarFallback: Int = R.drawable.img_avatar_default
)

fun selectorDataFromSecrets(input: List<SecretData>): List<SelectorData<SecretData>> {
    return input.map {
        SelectorData(
                it,
                it.title!!,
                if (!it.hasTitle) null else it.minterAddress.toShortString(),
                it.minterAddress.avatar
        )
    }
}

fun selectorDataFromCoins(input: List<CoinBalance>): List<SelectorData<CoinBalance>> {
    return input.map {
        SelectorData(
                it,
                "${it.coin!!} (${it.amount.humanize()})",
                null,
                it.coin!!.avatar
        )
    }
}

fun selectorDataFromDelegatedAccounts(input: List<CoinDelegation>): List<SelectorData<CoinDelegation>> {
    return input.map {
        SelectorData(
                it,
                "${it.coin!!} (${it.amount.humanize()})",
                it.publicKey!!.toShortString(),
                it.coin!!.avatar
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