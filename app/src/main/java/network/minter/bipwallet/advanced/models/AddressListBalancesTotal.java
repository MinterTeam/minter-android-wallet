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

package network.minter.bipwallet.advanced.models;

import com.annimon.stream.Optional;
import com.annimon.stream.Stream;

import org.parceler.Parcel;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import network.minter.core.crypto.MinterAddress;
import network.minter.explorer.models.AddressBalance;

@Parcel
public class AddressListBalancesTotal {
    public List<AddressBalanceTotal> balances = new ArrayList<>();

    public AddressListBalancesTotal(List<MinterAddress> addresses) {
        for (MinterAddress address : addresses) {
            balances.add(new AddressBalanceTotal(address));
        }
    }

    public AddressListBalancesTotal() {
    }

    public int size() {
        return balances.size();
    }

    public AddressBalance get(int i) {
        return balances.get(i);
    }

    public Optional<AddressBalanceTotal> find(MinterAddress address) {
        return Stream.of(balances)
                .filter(item -> item.address != null)
                .filter(item -> item.address.equals(address))
                .findFirst();
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    @Nonnull
    public AddressBalanceTotal getBalance(MinterAddress address) {
        for (AddressBalanceTotal balance : balances) {
            if (balance.address.equals(address)) {
                return balance;
            }
        }

        AddressBalanceTotal balance = new AddressBalanceTotal();
        balance.address = address;
        balance.fillDefaultsOnEmpty();
        return balance;
    }

    @Nonnull
    public BigDecimal getCoinBalance(MinterAddress address, String coin) {
        return getBalance(address).getCoin(coin).getAmount();
    }

}
