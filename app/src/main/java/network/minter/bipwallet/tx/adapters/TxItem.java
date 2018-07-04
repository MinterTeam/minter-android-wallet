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

package network.minter.bipwallet.tx.adapters;

import android.annotation.SuppressLint;

import network.minter.explorerapi.models.HistoryTransaction;

/**
 * MinterWallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class TxItem implements TransactionItem {
    private final HistoryTransaction mTx;
    private String mAvatar;
    private String mUsername;

    public TxItem(HistoryTransaction tx) {
        mTx = tx;
    }

    public String getAvatar() {
        if (mAvatar == null) {
            return "https://my.beta.minter.network/api/v1/avatar/by/user/1";
        }

        return mAvatar;
    }

    public TxItem setAvatar(String avatar) {
        mAvatar = avatar;
        return this;
    }

    public String getUsername() {
        return mUsername;
    }

    public TxItem setUsername(String username) {
        mUsername = username;
        return this;
    }

    @SuppressLint("WrongConstant")
    @Override
    public int getViewType() {
        return mTx.type != null ? mTx.type.ordinal() + 1 : 0xFF;
    }

    public HistoryTransaction getTx() {
        return mTx;
    }

    @Override
    public boolean isSameOf(TransactionItem item) {
        return item.getViewType() == TX_SEND && ((TxItem) item).getTx().hash.equals(mTx.hash);
    }
}
