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
package network.minter.bipwallet.internal.helpers.data

import android.os.Bundle
import android.util.SparseArray
import com.annimon.stream.Optional
import com.annimon.stream.Stream
import network.minter.core.MinterSDK
import network.minter.explorer.models.CoinBalance
import java.util.*

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich <edward.vstock></edward.vstock>@gmail.com>
 */
object CollectionsHelper {
    @JvmStatic
    fun <C> asList(sparseArray: SparseArray<C>?): List<C> {
        if (sparseArray == null) {
            return ArrayList()
        }
        val arrayList: MutableList<C> = ArrayList(sparseArray.size())
        for (i in 0 until sparseArray.size()) {
            arrayList.add(sparseArray.valueAt(i))
        }
        return arrayList
    }

    @JvmStatic
    fun asMap(bundle: Bundle): HashMap<String, Any?> {
        val out = HashMap<String, Any?>(bundle.keySet().size)
        for (key in bundle.keySet()) {
            out[key] = bundle[key]
        }
        return out
    }

    @JvmStatic
    fun <K, V> asMap(list: List<V>?, keyMapper: IOFunc<V, K>): Map<K, V> {
        if (list == null || list.isEmpty()) return emptyMap()
        val out: MutableMap<K, V> = LinkedHashMap(list.size)
        for (item in list) {
            out[keyMapper.apply(item)] = item
        }
        return out
    }

    @JvmStatic
    @SafeVarargs
    fun <C> asList(vararg items: C): List<C> {
        val arrayList: MutableList<C> = ArrayList(items.size)
        Stream.of(*items).forEach { e: C -> arrayList.add(e) }
        return arrayList
    }

    @JvmStatic
    fun <T : Comparable<T>?> equalsLists(a: MutableList<T>?, b: MutableList<T>?): Boolean {
        var first = a
        var second = b
        if (first == null || second == null) {
            return false
        }
        if (first.isEmpty() && second.isEmpty()) {
            return true
        }
        if (first.size != second.size) {
            return false
        }
        first = ArrayList(first)
        second = ArrayList(second)
        Collections.sort(first)
        Collections.sort(second)
        return first == second
    }

    @JvmStatic
    fun <R> topValue(list: List<R>): R {
        return list[0]
    }

    @JvmStatic
    fun <R> topValue(list: List<R>, defaultValue: R): R {
        return if (list.isEmpty()) defaultValue else list[0]
    }

    @JvmStatic
    fun <R> popValue(list: MutableList<R>): R {
        val res = list[0]
        list.removeAt(0)
        return res
    }

    @JvmStatic
    fun <R> popValue(list: MutableList<R>, defaultValue: R): R {
        if (list.isEmpty()) return defaultValue
        val res = list[0]

        list.removeAt(0)
        return res
    }

    fun <R> removeCopy(list: List<R>, applier: (R) -> Boolean): List<R> {
        return list.filter {
            !applier(it)
        }
    }

    fun <R> removeMutableCopy(list: MutableList<R>, applier: (R) -> Boolean): MutableList<R> {
        return removeCopy(list, applier).toMutableList()
    }

    @JvmStatic
    fun <K, V> topValue(map: HashMap<K, V>): V {
        if (map.size == 0) throw IndexOutOfBoundsException("Trying to get sw value from empty map")
        return Stream
                .of(map.values)
                .toList()[0]
    }

    @JvmStatic
    fun <K, V> topValue(list: HashMap<K, V>, defaultValue: V): V {
        return if (list.size == 0) defaultValue else Stream
                .of(list.values)
                .toList()[0]
    }

    @JvmStatic
    fun <K, V> sortByValue(map: MutableMap<K, V>, comparator: Comparator<V>): MutableMap<K, V> {
        val list: List<Map.Entry<K, V>> = ArrayList(map.entries)
        Collections.sort(list) { o1, o2 -> comparator.compare(o1.value, o2.value) }
        val result: MutableMap<K, V> = LinkedHashMap()
        for ((key, value) in list) {
            result[key] = value
        }
        return result
    }

    @JvmStatic
    fun <K, V : Comparable<V>?> sortByValue(map: MutableMap<K, V>): MutableMap<K, V> {
        val list: List<Map.Entry<K, V>> = ArrayList(map.entries)
        Collections.sort(list) { o1, o2 -> o1.value!!.compareTo(o2.value) }
        val result: MutableMap<K, V> = LinkedHashMap()
        for ((key, value) in list) {
            result[key] = value
        }
        return result
    }

    @JvmStatic
    fun asBundle(map: Map<String, Any>): Bundle {
        val bundle = Bundle()
        for ((key, value) in map) {
            bundle.putString(key, value.toString())
        }
        return bundle
    }

    @JvmStatic
    fun singletoneBundle(key: String, value: Any): Bundle {
        return asBundle(object : HashMap<String, Any>() {
            init {
                put(key, value)
            }
        })
    }

    @JvmStatic
    fun bzero(arr: ByteArray?) {
        if (arr == null || arr.isEmpty()) {
            return
        }
        for (i in arr.indices) {
            arr[i] = 0.toByte()
        }
    }


    fun <K, VI, VO> List<VI>.toMap(cb: (VI) -> Pair<K, VO>): Map<K, VO> {
        val out = HashMap<K, VO>(size)
        forEach {
            val res = cb(it)
            out[res.first] = res.second
        }
        return out
    }

    fun <T> Iterable<T>.firstOptional(predicate: (T) -> Boolean): Optional<T> {
        for (element in this) {
            if (predicate(element)) {
                return Optional.of(element)
            }
        }
        return Optional.empty()
    }

    interface IOFunc<Input, Output> {
        fun apply(input: Input): Output
    }

    class StableCoinSorting : Comparator<CoinBalance> {
        override fun compare(ac: CoinBalance, bc: CoinBalance): Int {
            val a = ac.coin!!.toLowerCase()
            val b = bc.coin!!.toLowerCase()
            if (a == b) // update to make it stable
                return 0
            if (a == sStable) return -1
            return if (b == sStable) 1 else a.compareTo(b)
        }

        companion object {
            private val sStable = MinterSDK.DEFAULT_COIN.toLowerCase()
        }
    }
}