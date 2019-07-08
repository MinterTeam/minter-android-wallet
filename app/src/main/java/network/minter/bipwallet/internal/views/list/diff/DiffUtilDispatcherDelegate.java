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

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

/**
 * minter-android-wallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public interface DiffUtilDispatcherDelegate<Data> {
    /**
     * @param diffUtilCallbackCls Class must contain constructor with 2 lists (old and new)
     * @param items
     * @param <T>
     */
    <T extends DiffUtil.Callback> void dispatchChanges(Class<T> diffUtilCallbackCls, @NonNull List<Data> items, boolean detectMoves);

    /**
     * @param diffUtilCallbackCls Object must contains construct with 2 lists (old and new)
     * @param items
     * @param <T>
     */
    <T extends DiffUtil.Callback> void dispatchChanges(Class<T> diffUtilCallbackCls, @NonNull List<Data> items);
    /**
     * Get data
     *
     * @return
     */
    List<Data> getItems();
    /**
     * Set data
     *
     * @param items
     */
    void setItems(List<Data> items);
    void clear();
}
