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

package network.minter.bipwallet.wallets.selector;

import com.annimon.stream.Optional;

import org.parceler.Parcel;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import network.minter.bipwallet.advanced.models.AddressBalanceTotal;
import network.minter.bipwallet.advanced.models.AddressListBalancesTotal;
import network.minter.bipwallet.advanced.models.SecretData;
import network.minter.bipwallet.advanced.repo.SecretStorage;
import network.minter.core.crypto.MinterAddress;

@Parcel
public class WalletItem {
    String mTitle;
    MinterAddress mAddress;
    WalletSelector.WalletWeight mWeight;
    boolean mIsMain = false;

    public WalletItem(MinterAddress address, String title, WalletSelector.WalletWeight weight, boolean isMain) {
        mTitle = title;
        mAddress = address;
        mWeight = weight;
        mIsMain = isMain;
    }

    WalletItem() {
    }

    public static WalletItem create(SecretStorage secretStorage, AddressBalanceTotal balance) {
        boolean isMain = secretStorage.isMainWallet(balance.address);
        SecretData data = secretStorage.getSecret(balance.address);

        WalletSelector.WalletWeight weight = WalletSelector.WalletWeight.detect(balance.getTotalBalance().add(balance.delegated));
        return new WalletItem(data.getMinterAddress(), data.getTitle(), weight, isMain);
    }

    public static List<WalletItem> create(SecretStorage secretStorage, AddressListBalancesTotal balances) {
        List<WalletItem> out = new ArrayList<>();
        boolean isMain;
        for (SecretData data : secretStorage.getSecretsListSafe()) {
            Optional<AddressBalanceTotal> balance = balances.find(data.getMinterAddress());

            isMain = secretStorage.isMainWallet(data.getMinterAddress());

            if (balance.isPresent()) {
                final BigDecimal total = balance.get().getTotalBalance().add(balance.get().delegated);
                WalletSelector.WalletWeight weight = WalletSelector.WalletWeight.detect(total);
                out.add(new WalletItem(data.getMinterAddress(), data.getTitle(), weight, isMain));
            } else {
                out.add(new WalletItem(data.getMinterAddress(), data.getTitle(), WalletSelector.WalletWeight.Shrimp, isMain));
            }
        }

        return out;
    }

    public boolean isMain() {
        return mIsMain;
    }

    public String getTitle() {
        return mTitle;
    }

    public MinterAddress getAddress() {
        return mAddress;
    }

    public String getAddressShort() {
        return getAddress().toShortString();
    }

    public WalletSelector.WalletWeight getWeight() {
        return mWeight;
    }
}
