/*******************************************************************************
 * Copyright (C) by MinterTeam. 2018
 * @link https://github.com/MinterTeam
 * @link https://github.com/edwardstock
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
 ******************************************************************************/

package network.minter.bipwallet.advanced.models;

import org.parceler.Parcel;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static network.minter.bipwallet.internal.common.Preconditions.firstNonNull;

/**
 * MinterWallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@Parcel
public class UserAccount implements Serializable, Cloneable {
    List<AccountItem> mAccounts;
    BigDecimal mTotalBalance;
    BigDecimal mTotalBalanceBase;
    BigDecimal mTotalBalanceUsd;
    int mHashCode;

    public UserAccount(List<AccountItem> accounts) {
        mAccounts = new ArrayList<>(accounts);
        mTotalBalance = new BigDecimal(0);
        mTotalBalanceUsd = new BigDecimal(0);
        mTotalBalanceBase = new BigDecimal(0);
        for (AccountItem item : mAccounts) {
            mTotalBalance = mTotalBalance.add(item.balance);
            mTotalBalanceUsd = mTotalBalanceUsd.add(item.balanceUsd);
            mTotalBalanceBase = mTotalBalance.add(item.balanceBase);
        }
        mHashCode = Objects.hash(mAccounts, mTotalBalance, mTotalBalanceUsd, mTotalBalanceBase);
    }

    public UserAccount(List<AccountItem> accounts, BigDecimal totalBalance, BigDecimal totalBalanceUsd, BigDecimal totalBalanceBase) {
        mAccounts = new ArrayList<>(accounts);
        mTotalBalance = firstNonNull(totalBalance, new BigDecimal(0));
        mTotalBalanceUsd = firstNonNull(totalBalanceUsd, new BigDecimal(0));
        mTotalBalanceBase = firstNonNull(totalBalanceBase, new BigDecimal(0));
        mHashCode = Objects.hash(mAccounts, mTotalBalance, mTotalBalanceUsd, mTotalBalanceBase);
    }

    UserAccount() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserAccount that = (UserAccount) o;
        return Objects.equals(mAccounts, that.mAccounts) &&
                Objects.equals(mTotalBalance, that.mTotalBalance) &&
                Objects.equals(mTotalBalanceUsd, that.mTotalBalanceUsd);
    }

    @Override
    public int hashCode() {
        return mHashCode;
    }

    public int size() {
        return mAccounts != null ? mAccounts.size() : 0;
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public BigDecimal getTotalBalance() {
        if (mTotalBalance == null) {
            mTotalBalance = new BigDecimal(0);
        }
        return mTotalBalance;
    }

    public BigDecimal getTotalBalanceBase() {
        if (mTotalBalanceBase == null) {
            mTotalBalanceBase = new BigDecimal(0);
        }
        return mTotalBalanceBase;
    }

    public BigDecimal getTotalBalanceUsd() {
        if (mTotalBalanceUsd == null) {
            mTotalBalanceUsd = new BigDecimal(0);
        }
        return mTotalBalanceUsd;
    }

    public List<AccountItem> getAccounts() {
        if (mAccounts == null) {
            mAccounts = Collections.emptyList();
        }

        return mAccounts;
    }

}
