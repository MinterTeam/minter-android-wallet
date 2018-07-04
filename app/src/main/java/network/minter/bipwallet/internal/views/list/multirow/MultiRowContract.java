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

package network.minter.bipwallet.internal.views.list.multirow;

import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;

/**
 * Mds. 2017
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */

public interface MultiRowContract {


    interface Row<T extends MultiRowAdapter.RowViewHolder> {
        int POSITION_FIRST = -1;
        int POSITION_LAST = 999;
        int POSITION_ORDERED = 0;

        /**
         * views ID
         *
         * @return int
         * @see MultiRowAdapter#makeHoldersCache()
         */
        @LayoutRes
        int getItemView();

        /**
         * views position index
         *
         * @return int
         * @see MultiRowAdapter
         * @see MultiRowContract.Row
         */
        @Deprecated
        int getRowPosition();

        /**
         * If view should be visible
         *
         * @return bool
         */
        boolean isVisible();

        /**
         * Вызывается когда адаптер биндит вьюху,
         * соответственно в этом методе заполняем TxSendCoinViewHolder
         *
         * @param viewHolder Row view holder
         * @see MultiRowAdapter#makeHoldersCache()
         */
        void onBindViewHolder(@NonNull T viewHolder);

        /**
         * Вызывается когда холдер отцепляется от окна
         *
         * @param viewHolder
         * @see RecyclerView#onDetachedFromWindow()
         */
        void onUnbindViewHolder(@NonNull T viewHolder);

        /**
         * Класс TxSendCoinViewHolder'а который отражает вьюху
         *
         * @return Class
         * @see MultiRowAdapter.RowViewHolder
         */
        @NonNull
        Class<T> getViewHolderClass();
    }
}
