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

package network.minter.bipwallet.addresses.models;

import android.arch.lifecycle.MutableLiveData;

import org.parceler.Parcel;
import org.parceler.Transient;

import java.math.BigDecimal;

import io.reactivex.Observable;
import network.minter.mintercore.crypto.MinterAddress;
import network.minter.my.models.MyAddressData;

/**
 * MinterWallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@Parcel
public class AddressItem {
    public String id;
    public MinterAddress address;
    public boolean isServerSecured;
    public Balance balance;
    public boolean isMain;
    public MyAddressData myAddressData = null;

    public enum BalanceState {
        Loading,
        Loaded,
        Failed,
    }

    public AddressItem(MyAddressData myMinterData) {
        myAddressData = myMinterData;
        id = myMinterData.id;
        address = myMinterData.address;
        isServerSecured = myMinterData.isServerSecured;
        balance = new Balance();
        isMain = myMinterData.isMain;
    }

    public AddressItem(String id, MinterAddress address) {
        this.id = id;
        this.address = address;
        this.isServerSecured = false;
        this.balance = new Balance();
    }

    AddressItem() {
    }

    @Parcel
    public static class Balance {
        BigDecimal amount;
        @Transient
        private Observable<BigDecimal> mFetcher = null;
        @Transient
        private MutableLiveData<BalanceState> state = new MutableLiveData<>();

        public Balance(Observable<BigDecimal> fetcher) {
            amount = new BigDecimal(0);
            setFetcher(fetcher);
        }

        public Balance() {
            amount = new BigDecimal(0);
            state.postValue(BalanceState.Loading);
        }

        public Balance(BigDecimal amount) {
            this.amount = amount;
            state.postValue(BalanceState.Loaded);
        }

        public void fetch() {
            if (mFetcher == null) {
                state.postValue(BalanceState.Failed);
                return;
            }
            setFetcher(mFetcher);
        }

        public MutableLiveData<BalanceState> getStateLiveData() {
            return state;
        }

        public void setFetcher(Observable<BigDecimal> fetcher) {
            state.postValue(BalanceState.Loading);
            mFetcher = fetcher;
            mFetcher.subscribe(res -> {
                amount = res;
                state.postValue(BalanceState.Loaded);
            }, t -> {
                amount = new BigDecimal(0);
                state.postValue(BalanceState.Failed);
            });
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public BalanceState getState() {
            return state.getValue();
        }

        public boolean isLoading() {
            return getState() == BalanceState.Loading;
        }

        public void notify(BigDecimal amount) {
            this.amount = amount;
            state.postValue(BalanceState.Loaded);
        }

        public void notify(String amount) {
            this.amount = new BigDecimal(amount);
            state.postValue(BalanceState.Loaded);
        }
    }

}
