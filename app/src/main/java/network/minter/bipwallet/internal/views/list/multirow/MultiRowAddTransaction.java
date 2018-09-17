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

package network.minter.bipwallet.internal.views.list.multirow;

import android.support.annotation.IntDef;

import com.annimon.stream.Stream;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

import timber.log.Timber;

/**
 * minter-android-wallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
final public class MultiRowAddTransaction {

    private final ArrayList<MultiRowContract.Row> mItems = new ArrayList<>();
    private WeakReference<MultiRowAdapter> mAdapter;
    private int mMode = AddMode.RANDOM;
    private int mBeforeSize = 0;

    public MultiRowAddTransaction(MultiRowAdapter adapter) {
        mAdapter = new WeakReference<>(adapter);
        mBeforeSize = adapter.getItemCount();
    }

    public MultiRowAddTransaction(MultiRowAdapter adapter, @AddMode int addMode) {
        this(adapter);
        mMode = addMode;
    }

    /**
     * @param row
     * @param <V>
     * @param <T>
     * @return inserted position
     */
    public synchronized <V extends MultiRowAdapter.RowViewHolder, T extends MultiRowContract.Row<V>>
    int add(T row) {
        if (row == null || !row.isVisible()) {
            Timber.i("Row is null");
            return -1;
        }

        int position = 0;

        if (mItems.size() > 0) {
            position = mItems.get(mItems.size() - 1).getRowPosition() + 1;
        }

        final SortableRow<V, T> newRow = new SortableRow<>(row, position);
        mItems.add(newRow);

        return position;
    }

    public synchronized <V extends MultiRowAdapter.RowViewHolder, T extends MultiRowContract.Row<V>>
    MultiRowAddTransaction add(T row, int position) {
        if (row == null || !row.isVisible()) {
            Timber.i("Row for position %d is null", position);
            return this;
        }

        final SortableRow<V, T> newRow = new SortableRow<>(row, position);
        mItems.add(newRow);

        return this;
    }

    public void commit() {
        if (mItems.size() == 0) {
            return;
        }

        Stream.of(mItems)
                .forEach(item -> {
                    mAdapter.get().mItems.remove(item);
                    mAdapter.get().mItems.add(item);
                });

        mAdapter.get().sort();
        mAdapter.get().makeHoldersCache();

        // @TODO потестить производительность
        if (mMode == AddMode.RANDOM) {
            mAdapter.get().notifyItemRangeChanged(0, mAdapter.get().getItemCount());
        } else if (mMode == AddMode.ADD_TO_END) {
            mAdapter.get().notifyItemRangeChanged(mBeforeSize - 1, mItems.size());
        } else if (mMode == AddMode.ADD_TO_START) {
            mAdapter.get().notifyItemRangeChanged(0, mItems.size());
            mAdapter.get().notifyItemRangeChanged(mBeforeSize - 1, mAdapter.get().getItemCount());
        } else {
            // радикальная мера
            mAdapter.get().notifyDataSetChanged();
        }

        mItems.clear();
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({AddMode.UNSPECIFIED, AddMode.RANDOM, AddMode.ADD_TO_END, AddMode.ADD_TO_START})
    public @interface AddMode {
        int UNSPECIFIED = -1;
        int RANDOM = 0;
        int ADD_TO_END = 1;
        int ADD_TO_START = 2;
    }
}
