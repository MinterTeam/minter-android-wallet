/*
 * Copyright (C) by MinterTeam. 2018
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

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import network.minter.core.MinterSDK;

import static network.minter.bipwallet.internal.common.Preconditions.firstNonNull;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@Parcel
public class UserAccount implements Serializable, Cloneable {
    List<CoinAccount> mCoinAccounts;
    BigDecimal mAvailableBalanceBase;
    BigDecimal mAvailableBalanceUSD;
    BigDecimal mTotalBalanceBase;
    BigDecimal mTotalBalanceUSD;
    int mHashCode;

    public UserAccount(List<CoinAccount> coinAccounts, BigDecimal availableBalanceBase, BigDecimal availableBalanceUSD, BigDecimal totalBalanceBase, BigDecimal totalBalanceUSD) {
        mCoinAccounts = new ArrayList<>(coinAccounts);
        mAvailableBalanceBase = firstNonNull(availableBalanceBase, BigDecimal.ZERO);
        mAvailableBalanceUSD = firstNonNull(availableBalanceUSD, BigDecimal.ZERO);
        mTotalBalanceBase = firstNonNull(totalBalanceBase, BigDecimal.ZERO);
        mTotalBalanceUSD = firstNonNull(totalBalanceUSD, BigDecimal.ZERO);
        mHashCode = Objects.hash(mCoinAccounts, mAvailableBalanceBase, mAvailableBalanceUSD, mTotalBalanceBase, mTotalBalanceUSD);
    }

    public UserAccount(List<CoinAccount> coinAccounts) {
        mCoinAccounts = coinAccounts;
        mAvailableBalanceBase = BigDecimal.ZERO;
        mAvailableBalanceUSD = BigDecimal.ZERO;
        mTotalBalanceBase = BigDecimal.ZERO;
        mTotalBalanceUSD = BigDecimal.ZERO;
        mHashCode = Objects.hash(mCoinAccounts, mAvailableBalanceBase, mAvailableBalanceUSD, mTotalBalanceBase, mTotalBalanceUSD);
    }

    public UserAccount() {
        mCoinAccounts = Collections.emptyList();
        mAvailableBalanceBase = BigDecimal.ZERO;
        mAvailableBalanceUSD = BigDecimal.ZERO;
        mTotalBalanceBase = BigDecimal.ZERO;
        mTotalBalanceUSD = BigDecimal.ZERO;
        mHashCode = Objects.hash(mCoinAccounts, mAvailableBalanceBase, mAvailableBalanceUSD, mTotalBalanceBase, mTotalBalanceUSD);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserAccount that = (UserAccount) o;
        return Objects.equals(mCoinAccounts, that.mCoinAccounts) &&
                Objects.equals(mAvailableBalanceBase, that.mAvailableBalanceBase) &&
                Objects.equals(mAvailableBalanceUSD, that.mAvailableBalanceUSD) &&
                Objects.equals(mTotalBalanceBase, that.mTotalBalanceBase) &&
                Objects.equals(mTotalBalanceUSD, that.mTotalBalanceUSD);
    }

    @Override
    public int hashCode() {
        return mHashCode;
    }

    public int size() {
        return mCoinAccounts != null ? mCoinAccounts.size() : 0;
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public BigDecimal getAvailableBalanceBase() {
        if (mAvailableBalanceBase == null) {
            mAvailableBalanceBase = BigDecimal.ZERO;
        }
        return mAvailableBalanceBase;
    }

    public BigDecimal getTotalBalanceBase() {
        if (mTotalBalanceBase == null) {
            mTotalBalanceBase = BigDecimal.ZERO;
        }
        return mTotalBalanceBase;
    }

    public BigDecimal getAvailableBalanceUSD() {
        if (mAvailableBalanceUSD == null) {
            mAvailableBalanceUSD = BigDecimal.ZERO;
        }
        return mAvailableBalanceUSD;
    }

    public BigDecimal getTotalBalanceUSD() {
        if (mTotalBalanceUSD == null) {
            mTotalBalanceUSD = BigDecimal.ZERO;
        }

        return mTotalBalanceUSD;
    }

    public Optional<CoinAccount> findAccountByCoin(String coin) {
        return Stream.of(getCoinAccounts())
                .filter(item -> item.getCoin().equals(coin))
                .findFirst();
    }

    public List<CoinAccount> getCoinAccounts() {
        if (mCoinAccounts == null) {
            mCoinAccounts = Collections.emptyList();
        }

        List<CoinAccount> items = mCoinAccounts;
        Collections.sort(items, new Comparator<CoinAccount>() {
            @Override
            public int compare(CoinAccount a, CoinAccount b) {
                if (a.getCoin().toUpperCase().equals(MinterSDK.DEFAULT_COIN)) {
                    return -1;
                }

                return a.getCoin().compareTo(b.getCoin());

            }
        });

        return mCoinAccounts;
    }

}
