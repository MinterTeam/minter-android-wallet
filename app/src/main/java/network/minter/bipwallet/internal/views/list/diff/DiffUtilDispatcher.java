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

package network.minter.bipwallet.internal.views.list.diff;

import android.support.annotation.NonNull;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static network.minter.bipwallet.internal.common.Preconditions.checkNotNull;

/**
 * minter-android-wallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public final class DiffUtilDispatcher {

    public static <Data, CallBack extends DiffUtil.Callback, Adapter extends RecyclerView.Adapter<?> & DiffUtilDispatcherDelegate<Data>>
    void dispatchChanges(Adapter adapter, Class<CallBack> diffUtilCallbackCls, @NonNull List<Data> items, boolean detectMoves) {
        checkNotNull(items, "Data can't be null. Provide empty list if you have no data");

        if (adapter.getItemCount() == 0 && !items.isEmpty()) {
            adapter.setItems(items);
            adapter.notifyItemRangeInserted(0, items.size());
            return;
        }

        final List<Data> old = adapter.getItems();
        adapter.setItems(items);
        DiffUtil.Callback cb;

        try {
            cb = diffUtilCallbackCls.getDeclaredConstructor(List.class, List.class).newInstance(old, items);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(cb, detectMoves);
        diffResult.dispatchUpdatesTo(adapter);
    }

    public static <Data, CallBack extends DiffUtil.Callback, Adapter extends RecyclerView.Adapter<?> & DiffUtilDispatcherDelegate<Data>>
    void dispatchChanges(Adapter adapter, Class<CallBack> diffUtilCallbackCls, @NonNull List<Data> items) {
        dispatchChanges(adapter, diffUtilCallbackCls, items, false);
    }


}
