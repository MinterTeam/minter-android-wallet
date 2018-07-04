/*
 * Copyright (C) 2018 by MinterTeam
 * @link https://github.com/MinterTeam
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

import android.support.v7.util.DiffUtil;

import org.parceler.Parcel;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import network.minter.mintercore.crypto.MinterAddress;

import static network.minter.bipwallet.internal.common.Preconditions.checkNotNull;

/**
 * MinterWallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@Parcel
public class AccountItem implements Serializable, Cloneable {
    public String id;
    public String avatar;
    public String coin;
    public MinterAddress address;
    public BigDecimal balance;
    public BigDecimal balanceUsd;
    int mHashCode;

    public AccountItem(final AccountItem another) {
        id = another.id;
        avatar = another.avatar;
        coin = another.coin;
        address = another.address;
        balanceUsd = another.balanceUsd;
        balance = another.balance;
        mHashCode = another.mHashCode;
    }

    public AccountItem(String avatar, String coin, MinterAddress address, BigDecimal balance, BigDecimal balanceUsd) {
        this(coin, address, balance, balanceUsd);
        this.avatar = avatar;
    }

    public AccountItem(String coin, MinterAddress address, BigDecimal balance, BigDecimal balanceUsd) {
        this.id = UUID.randomUUID().toString();
        this.coin = checkNotNull(coin, "Coin name required");
        this.address = checkNotNull(address, "Address required");
        this.balance = balance;
        this.balanceUsd = balanceUsd;
        mHashCode = Objects.hash(id, avatar, coin, address, balance, balanceUsd);
    }

    AccountItem() {
    }

    public String getAvatar() {
        if (avatar == null) {
            return "https://my.beta.minter.network/api/v1/avatar/by/user/1";
        }
        return avatar;
    }

    @Override
    public AccountItem clone() throws CloneNotSupportedException {
        return (AccountItem) super.clone();
    }

    @Override
    public String toString() {
        return String.format("AccountItem{address=%s, coin=%s, amount=%s}", address, coin, balance.toPlainString());
    }

    public BigDecimal getBalance() {
        if (balance == null) {
            balance = new BigDecimal(0);
        }

        return balance;
    }

    public BigDecimal getBalanceUsd() {
        if (balanceUsd == null) {
            balanceUsd = new BigDecimal(0);
        }
        return balanceUsd;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AccountItem that = (AccountItem) o;
        return Objects.equals(coin, that.coin) &&
                Objects.equals(address, that.address) &&
                Objects.equals(balance, that.balance) &&
                Objects.equals(balanceUsd, that.balanceUsd);
    }

    @Override
    public int hashCode() {
        return mHashCode;
    }

    public static class DiffUtilImpl extends DiffUtil.Callback {
        private final List<AccountItem> mOldList, mNewList;

        public DiffUtilImpl(List<AccountItem> oldList, List<AccountItem> newList) {
            mOldList = oldList;
            mNewList = newList;
        }

        @Override
        public int getOldListSize() {
            return mOldList.size();
        }

        @Override
        public int getNewListSize() {
            return mNewList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            AccountItem oldItem = mOldList.get(oldItemPosition);
            AccountItem newItem = mNewList.get(newItemPosition);

            return oldItem.address.equals(newItem.address) && oldItem.coin.equals(newItem.coin);
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            AccountItem oldItem = mOldList.get(oldItemPosition);
            AccountItem newItem = mNewList.get(newItemPosition);
            return oldItem.equals(newItem);
        }
    }
}
