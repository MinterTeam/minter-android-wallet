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

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * MinterWallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public interface TransactionItem {
    int ITEM_PROGRESS = -1;
    int ITEM_HEADER = 0;
    int TX_SEND = 1;
    int TX_CONVERT_COIN = 2;
    int TX_CREATE_COIN = 3;
    int TX_DECLARE_CANDIDACY = 4;
    int TX_DELEGATE = 5;
    int TX_UNBOUND = 6;
    int TX_REDEEM_CHECK = 7;
    int TX_SET_CANDIDATE_ONLINE = 8;
    int TX_SET_CANDIDATE_OFFLINE = 9;


    @ListType
    int getViewType();
    boolean isSameOf(TransactionItem item);

    @Retention(SOURCE)
    @IntDef({
            ITEM_PROGRESS,
            ITEM_HEADER,
            TX_SEND,
            TX_CONVERT_COIN,
            TX_CREATE_COIN,
            TX_DECLARE_CANDIDACY,
            TX_DELEGATE,
            TX_UNBOUND,
            TX_REDEEM_CHECK,
            TX_SET_CANDIDATE_ONLINE,
            TX_SET_CANDIDATE_OFFLINE
    })
    @interface ListType {
    }
}
