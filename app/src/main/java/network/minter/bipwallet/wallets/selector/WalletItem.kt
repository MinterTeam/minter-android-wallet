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
package network.minter.bipwallet.wallets.selector

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import network.minter.bipwallet.internal.storage.SecretStorage
import network.minter.bipwallet.internal.storage.models.AddressBalanceTotal
import network.minter.bipwallet.internal.storage.models.AddressListBalancesTotal
import network.minter.bipwallet.wallets.selector.WalletSelector.WalletWeight
import network.minter.core.crypto.MinterAddress
import java.util.*

@Parcelize
data class WalletItem(
        var address: MinterAddress,
        var title: String?,
        var weight: WalletWeight,
        var isMain: Boolean = false
) : Parcelable {

    val addressShort: String
        get() = address.toShortString()

    companion object {
        fun create(secretStorage: SecretStorage, balance: AddressBalanceTotal): WalletItem {
            val isMain = secretStorage.isMainWallet(balance.address)
            val data = secretStorage.getSecret(balance.address)
            val weight = WalletWeight.detect(balance.totalBalance.add(balance.delegated))
            return WalletItem(data.minterAddress, data.title, weight, isMain)
        }

        fun create(secretStorage: SecretStorage, balances: AddressListBalancesTotal): List<WalletItem> {
            val out: MutableList<WalletItem> = ArrayList()
            var isMain: Boolean
            for (data in secretStorage.secretsListSafe) {
                val balance = balances.find(data.minterAddress)
                isMain = secretStorage.isMainWallet(data.minterAddress)
                if (balance.isPresent) {
                    val total = balance.get().totalBalance.add(balance.get().delegated)
                    val weight = WalletWeight.detect(total)
                    out.add(WalletItem(data.minterAddress, data.title, weight, isMain))
                } else {
                    out.add(WalletItem(data.minterAddress, data.title, WalletWeight.Shrimp, isMain))
                }
            }
            return out
        }
    }
}