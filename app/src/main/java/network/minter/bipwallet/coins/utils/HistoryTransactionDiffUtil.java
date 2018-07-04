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

package network.minter.bipwallet.coins.utils;

import android.support.v7.util.DiffUtil;

import java.util.List;

import network.minter.explorerapi.models.HistoryTransaction;

/**
 * MinterWallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class HistoryTransactionDiffUtil extends DiffUtil.Callback {
    private final List<HistoryTransaction> mOldItems, mNewItems;

    public HistoryTransactionDiffUtil(List<HistoryTransaction> oldItems, List<HistoryTransaction> newItems) {
        mOldItems = oldItems;
        mNewItems = newItems;
    }

    @Override
    public int getOldListSize() {
        return mOldItems.size();
    }

    @Override
    public int getNewListSize() {
        return mNewItems.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        HistoryTransaction oldItem = mOldItems.get(oldItemPosition);
        HistoryTransaction newItem = mNewItems.get(newItemPosition);

        return oldItem.hash.equals(newItem.hash);
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        HistoryTransaction oldItem = mOldItems.get(oldItemPosition);
        HistoryTransaction newItem = mNewItems.get(newItemPosition);
        return oldItem.equals(newItem);
    }
}
