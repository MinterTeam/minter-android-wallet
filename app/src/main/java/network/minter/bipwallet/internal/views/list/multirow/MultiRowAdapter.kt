/*
 * Copyright (C) by MinterTeam. 2020
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
package network.minter.bipwallet.internal.views.list.multirow

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.collection.SimpleArrayMap
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.annimon.stream.Stream
import network.minter.bipwallet.internal.common.Preconditions
import network.minter.bipwallet.internal.views.list.multirow.MultiRowAdapter.RowViewHolder
import network.minter.bipwallet.internal.views.list.multirow.MultiRowContract.Row
import timber.log.Timber
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Modifier
import java.util.*

/**
 * Mds. 2017
 *
 * @author Eduard Maximovich <edward.vstock></edward.vstock>@gmail.com>
 */
open class MultiRowAdapter : RecyclerView.Adapter<RowViewHolder> {

    protected var mItems: MutableList<Row<RowViewHolder>> = ArrayList()

    private var holderViewIdClassCache = SimpleArrayMap<Int, Class<out RowViewHolder>>()
    private var rowsViewIdClassCache = WeakHashMap<Int, Row<RowViewHolder>>()
    private var layoutInflater: LayoutInflater? = null
    private var mEnableSorting = true

    constructor(builder: Builder) : this(builder.mRows) {
        mEnableSorting = builder.mEnableSort
        if (mEnableSorting) {
            sort()
        }
    }


    @Suppress("UNCHECKED_CAST")
    constructor (items: MutableList<Row<*>>) {
        mItems = items as MutableList<Row<RowViewHolder>>
        makeHoldersCache()
    }

    constructor()

    fun updateRows(newRows: List<Row<*>>) {
        val diffResult = DiffUtil.calculateDiff(DiffCallback(newRows), true)
        diffResult.dispatchUpdatesTo(this)
    }


    @Suppress("UNCHECKED_CAST")
    fun <R : RowViewHolder, T : Row<R>> addRow(row: T, position: Int): MultiRowAdapter {
        if (!row.isVisible()) {
            return this
        }
        val beforeSize = mItems.size
        val newRow = SortableRow(row, position)
        if (mItems.contains(newRow as Row<*>)) {
            Timber.d("Updating row: %s with position %d", row.javaClass.simpleName, position)
            val idx = mItems.indexOf(newRow as Row<*>)
            mItems[idx] = newRow as Row<RowViewHolder>
            notifyItemChanged(idx)
            return this
        }
        Timber.d("Adding row: %s with position %d", row.javaClass.simpleName, position)
        mItems.add(newRow as Row<RowViewHolder>)
        if (mEnableSorting) {
            sort()
        }
        makeHoldersCache()
        if (beforeSize == 0) {
            notifyItemInserted(0)
        } else {
            notifyItemRangeChanged(0, mItems.size)
        }
        return this
    }

    @Suppress("UNCHECKED_CAST")
    fun addRowsTop(rows: Collection<Row<*>>) {
        if (rows.isEmpty()) return

        val targets = rows.filter {
            it.isVisible()
        }.map {
            it as Row<RowViewHolder>
        }

        mItems.addAll(0, targets)
        if (mEnableSorting) {
            sort()
        }
        makeHoldersCache()
        notifyItemRangeInserted(0, targets.size)
    }

    @Suppress("UNCHECKED_CAST")
    fun addRows(rows: Collection<Row<*>>) {
        if (rows.isEmpty()) return

        val targets = rows.filter { it.isVisible() }.map { it as Row<RowViewHolder> }

        val beforeSize = mItems.size
        mItems.addAll(targets)
        if (mEnableSorting) {
            sort()
        }
        makeHoldersCache()
        notifyItemRangeInserted(beforeSize, targets.size)
    }

    @Suppress("UNCHECKED_CAST")
    fun addRowTop(row: Row<*>?) {
        if (row == null || !row.isVisible()) {
            return
        }
        mItems.add(0, row as Row<RowViewHolder>)
        if (mEnableSorting) {
            sort()
        }
        makeHoldersCache()
        notifyItemInserted(mItems.size)
    }

    @Suppress("UNCHECKED_CAST")
    fun addRow(row: Row<*>) {
        if (!row.isVisible()) {
            return
        }
        mItems.add(row as Row<RowViewHolder>)
        if (mEnableSorting) {
            sort()
        }
        makeHoldersCache()
        notifyItemInserted(mItems.size)
    }

    fun setEnableSorting(enableSorting: Boolean) {
        mEnableSorting = enableSorting
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowViewHolder {
//        val tag = String.format("RowViewHolder instancing: %s", parent.context.resources.getResourceEntryName(viewType))
        //        TimeProfiler.start(tag);
        if (layoutInflater == null) {
            layoutInflater = LayoutInflater.from(parent.context)
        }
        if (viewType == View.NO_ID || viewType == 0) {
            throw RuntimeException("Layout id can't be 0")
        }
        val v = layoutInflater!!.inflate(viewType, parent, false)
        var viewHolder: RowViewHolder? = null
        var cause: Throwable? = null
        try {
            viewHolder = findViewHolder(viewType, v)
        } catch (e: NoSuchMethodException) {
            cause = e
            Timber.e(e, "Error finding view holder")
        } catch (e: IllegalAccessException) {
            cause = e
            Timber.e(e, "Error finding view holder")
        } catch (e: InvocationTargetException) {
            cause = e
            Timber.e(e, "Error finding view holder")
        } catch (e: InstantiationException) {
            cause = e
            Timber.e(e, "Error finding view holder")
        }
        if (viewHolder == null) {
            throw RuntimeException(cause)
        }
        return viewHolder
    }

    override fun onBindViewHolder(holder: RowViewHolder, position: Int) {
        val item: Row<RowViewHolder> = getItemByPosition(position)
        item.onBindViewHolder(holder)
    }

    override fun onViewDetachedFromWindow(holder: RowViewHolder) {
        super.onViewDetachedFromWindow(holder)
        val position = holder.bindingAdapterPosition
        if (position < 0) {
            return
        }
        val item = getItemByPosition(position)
        try {
            item.onUnbindViewHolder(holder)
        } catch (ignore: ClassCastException) {
            // @TODO fix this (throws after removing row)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return getItemByPosition(position).getItemView()
    }

    override fun getItemCount(): Int {
        return mItems.size
    }

    fun clear() {
        if (mItems.isEmpty()) return
        mItems.clear()
        notifyDataSetChanged()
        holderViewIdClassCache.clear()
    }

    fun clearNotify() {
        val size = mItems.size
        if (size == 0) return
        notifyItemRangeRemoved(0, size)
        mItems.clear()
    }

    /**
     * Nullptr safe delete row
     *
     * @param row MultiRowContract.Row
     * @param <T> Result
    </T> */
    fun remove(row: Row<*>) {
        if (mItems.contains(row)) {
            val index = mItems.indexOf(row)
            mItems.removeAt(index)
            notifyItemRemoved(index)
        } else {
            var found: Row<*>? = null
            for (r in mItems) {
                if (r is SortableRow<*, *> && r.row == row) {
                    found = r
                    break
                }
            }
            if (found != null) {
                remove(found)
            }

        }
    }

    fun removeUniqueLayout(@LayoutRes rowLayout: Int) {
        if (rowLayout == View.NO_ID) return
        for (r in mItems) {
            if (r.getItemView() == rowLayout) {
                remove(r)
                return
            }
        }
    }

    fun removeUniqueLayout(row: Row<*>) {
        for (r in mItems) {
            if (r.getItemView() == row.getItemView()) {
                remove(r)
                return
            }
        }
    }

    fun getItemByPosition(position: Int): Row<RowViewHolder> {
        return if (position < 0 || position >= mItems.size) {
            throw IllegalAccessException("Invalid position $position")
        } else mItems[position]
    }

    fun <T> findStream(rowClass: Class<T>): Stream<T> {
        return Stream.of(mItems)
                .filter { item: Row<*>? ->
                    if (item is SortableRow<*, *>) {
                        return@filter rowClass.isInstance(item) || rowClass.isInstance(item.row)
                    }
                    rowClass.isInstance(item)
                }
                .map { obj: Row<*>? -> rowClass.cast(obj) }
    }

    fun sort(c: Comparator<Row<*>?>?) {
        Collections.sort(mItems, c)
    }

    fun sort() {
        Collections.sort(mItems, RowComparator())
    }

    /**
     * Кэшированный список классов RowViewHolder чтоб итеративно каждый раз не искать
     */
    fun makeHoldersCache() {
        Preconditions.checkNotNull<List<Row<*>>>(mItems, "Wow! Rows can't be null")
        holderViewIdClassCache.clear()
        holderViewIdClassCache = SimpleArrayMap(mItems.size)
        rowsViewIdClassCache = WeakHashMap(mItems.size)
        for (item in mItems) {
            Preconditions.checkNotNull(item)
            if (item is SortableRow<*, *>) {
                Preconditions.checkNotNull<Class<*>>(item.getViewHolderClass(),
                        "Row " + item.row.javaClass + " does not have valid RowViewHolder class")
            } else {
                Preconditions.checkNotNull<Class<*>>(item.getViewHolderClass(),
                        "Row " + item.javaClass + " does not have valid RowViewHolder class")
            }
            rowsViewIdClassCache[item.getItemView()] = item
            holderViewIdClassCache.put(item.getItemView(), item.getViewHolderClass())
        }
    }

    private fun isInnerClass(clazz: Class<*>): Boolean {
        return clazz.isMemberClass && !Modifier.isStatic(clazz.modifiers)
    }

    private fun findRow(@LayoutRes viewId: Int): Row<*> {
        val row = rowsViewIdClassCache[viewId]
        if (row == null) {
            makeHoldersCache()
        }
        return rowsViewIdClassCache[viewId]!!
    }

    @Throws(NoSuchMethodException::class, IllegalAccessException::class, InvocationTargetException::class, InstantiationException::class)
    private fun findViewHolder(@LayoutRes viewId: Int, view: View): RowViewHolder {
        var holderClass = holderViewIdClassCache[viewId]
        if (holderClass == null) {
            makeHoldersCache()
        }
        holderClass = holderViewIdClassCache[viewId]
        if (holderClass == null) {
            throw RuntimeException("Can't findStream RowViewHolder for view $viewId")
        }
        if (isInnerClass(holderClass)) {
            throw RuntimeException("Class should be static!")
        }
        return holderClass.getDeclaredConstructor(View::class.java).newInstance(view)
    }

    class Builder {
        val mRows = ArrayList<Row<*>>()
        var mEnableSort = false
        fun addRow(row: Row<*>): Builder {
            mRows.add(row)
            return this
        }

        fun enableSort(enable: Boolean): Builder {
            mEnableSort = enable
            return this
        }

        fun build(): MultiRowAdapter {
            return MultiRowAdapter(this)
        }
    }

    abstract class RowViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    class RowComparator : Comparator<Row<*>> {
        override fun compare(o1: Row<*>, o2: Row<*>): Int {
            return o1.getRowPosition() - o2.getRowPosition()
        }
    }

    internal inner class DiffCallback(var mRows: List<Row<*>>) : DiffUtil.Callback() {
        override fun getOldListSize(): Int {
            return mItems.size
        }

        override fun getNewListSize(): Int {
            return mRows.size
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return (mItems[oldItemPosition].getItemView() == mRows[newItemPosition].getItemView()
                    && mItems[oldItemPosition].getRowPosition() == mRows[newItemPosition].getRowPosition())
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return mItems[oldItemPosition] == mRows[newItemPosition]
        }

    }
}