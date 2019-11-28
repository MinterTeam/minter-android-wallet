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
import java.util.List;
import java.util.Objects;

import static network.minter.bipwallet.internal.common.Preconditions.firstNonNull;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@Parcel
public class UserAccount implements Serializable, Cloneable {
    List<CoinAccount> mCoinAccounts;
    BigDecimal mAvailableBalanceBIP;
    BigDecimal mTotalBalanceBase;
    BigDecimal mTotalBalanceUSD;
    int mHashCode;

    public UserAccount(List<CoinAccount> coinAccounts, BigDecimal availableBalanceBIP, BigDecimal totalBalanceBase, BigDecimal totalBalanceUSD) {
        mCoinAccounts = new ArrayList<>(coinAccounts);
        mAvailableBalanceBIP = firstNonNull(availableBalanceBIP, BigDecimal.ZERO);
        mTotalBalanceBase = firstNonNull(totalBalanceBase, BigDecimal.ZERO);
        mTotalBalanceUSD = firstNonNull(totalBalanceUSD, BigDecimal.ZERO);
        mHashCode = Objects.hash(mCoinAccounts, mAvailableBalanceBIP, mTotalBalanceBase, mTotalBalanceUSD);
    }

    public UserAccount(List<CoinAccount> coinAccounts) {
        mCoinAccounts = coinAccounts;
        mAvailableBalanceBIP = BigDecimal.ZERO;
        mTotalBalanceBase = BigDecimal.ZERO;
        mTotalBalanceUSD = BigDecimal.ZERO;
        mHashCode = Objects.hash(mCoinAccounts, mAvailableBalanceBIP, mTotalBalanceBase, mTotalBalanceUSD);
    }

    public UserAccount() {
        mCoinAccounts = Collections.emptyList();
        mAvailableBalanceBIP = BigDecimal.ZERO;
        mTotalBalanceBase = BigDecimal.ZERO;
        mTotalBalanceUSD = BigDecimal.ZERO;
        mHashCode = Objects.hash(mCoinAccounts, mAvailableBalanceBIP, mTotalBalanceBase, mTotalBalanceUSD);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserAccount that = (UserAccount) o;
        return Objects.equals(mCoinAccounts, that.mCoinAccounts) &&
                Objects.equals(mAvailableBalanceBIP, that.mAvailableBalanceBIP) &&
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

    public BigDecimal getAvailableBalanceBIP() {
        if (mAvailableBalanceBIP == null) {
            mAvailableBalanceBIP = BigDecimal.ZERO;
        }
        return mAvailableBalanceBIP;
    }

    public BigDecimal getTotalBalanceBase() {
        if (mTotalBalanceBase == null) {
            mTotalBalanceBase = BigDecimal.ZERO;
        }
        return mTotalBalanceBase;
    }

    public BigDecimal getTotalBalanceUSD() {
        if (mTotalBalanceUSD == null) {
            mTotalBalanceUSD = BigDecimal.ZERO;
        }

        return mTotalBalanceUSD;
    }

    public Optional<CoinAccount> findAccountByCoin(String coin) {
        return Stream.of(getCoinAccounts())
                .filter(item -> item.getCoin().toLowerCase().equals(coin.toLowerCase()))
                .findFirst();
    }

    public List<CoinAccount> getCoinAccounts() {
        if (mCoinAccounts == null) {
            mCoinAccounts = Collections.emptyList();
        }

//        List<CoinAccount> items = mCoinAccounts;
//        Collections.sort(items, (a, b) -> {
//            if (a.getCoin().toUpperCase().equals(MinterSDK.DEFAULT_COIN)) {
//                return -1;
//            }
//
//            return a.getCoin().compareTo(b.getCoin());
//
//        });

        return mCoinAccounts;
    }

    public void setCoinAccounts(List<CoinAccount> accounts) {
        mCoinAccounts = accounts;
    }

}
