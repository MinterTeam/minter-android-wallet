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

package network.minter.bipwallet.internal.helpers.data;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseArray;

import com.annimon.stream.Stream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Dogsy. 2017
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class CollectionsHelper {

    @NonNull
    public static <C> List<C> asList(@Nullable SparseArray<C> sparseArray) {
        if (sparseArray == null) {
            return new ArrayList<>();
        }

        final List<C> arrayList = new ArrayList<>(sparseArray.size());
        for (int i = 0; i < sparseArray.size(); i++) {
            arrayList.add(sparseArray.valueAt(i));
        }

        return arrayList;
    }

    public static HashMap<String, Object> asMap(Bundle bundle) {
        final HashMap<String, Object> out = new HashMap<>(bundle.keySet().size());
        for (String key : bundle.keySet()) {
            out.put(key, bundle.get(key));
        }

        return out;
    }

    public static <K, V> Map<K, V> asMap(List<V> list, IOFunc<V, K> keyMapper) {
        if (list == null || list.isEmpty()) return Collections.emptyMap();

        final Map<K, V> out = new LinkedHashMap<>(list.size());

        for (V item : list) {
            out.put(keyMapper.apply(item), item);
        }

        return out;
    }

    @SafeVarargs
    @NonNull
    public static <C> List<C> asList(@Nullable C... items) {
        if (items == null) return new ArrayList<>();

        final List<C> arrayList = new ArrayList<>(items.length);
        Stream.of(items).forEach(arrayList::add);

        return arrayList;
    }

    public static <T extends Comparable<T>> boolean equalsLists(List<T> first, List<T> second) {
        if (first == null || second == null) {
            return false;
        }
        if (first.isEmpty() && second.isEmpty()) {
            return true;
        }
        if (first.size() != second.size()) {
            return false;
        }

        first = new ArrayList<>(first);
        second = new ArrayList<>(second);

        Collections.sort(first);
        Collections.sort(second);

        return first.equals(second);
    }

    public static <R> R topValue(List<R> list) {
        return list.get(0);
    }

    public static <R> R topValue(List<R> list, R defaultValue) {
        if (list.size() == 0) return defaultValue;
        return list.get(0);
    }

    public static <R> R popValue(List<R> list) {
        final R res = list.get(0);
        list.remove(0);
        return res;
    }

    public static <R> R popValue(List<R> list, R defaultValue) {
        if (list.size() == 0) return defaultValue;
        final R res = list.get(0);
        list.remove(0);
        return res;
    }

    public static <K, V> V topValue(HashMap<K, V> map) {
        if (map.size() == 0)
            throw new IndexOutOfBoundsException("Trying to get sw value from empty map");
        return Stream
                .of(map.values())
                .toList().get(0);
    }

    public static <K, V> V topValue(HashMap<K, V> list, V defaultValue) {
        if (list.size() == 0) return defaultValue;
        return Stream
                .of(list.values())
                .toList().get(0);
    }

    public static Bundle asBundle(Map<String, Object> map) {
        final Bundle bundle = new Bundle();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            bundle.putString(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
        }

        return bundle;
    }

    public static Bundle singletoneBundle(final String key, final Object value) {
        return asBundle(new HashMap<String, Object>() {{
            put(key, value);
        }});
    }

    public interface IOFunc<Input, Output> {
        Output apply(Input input);
    }

    public static void bzero(byte[] arr) {
        if (arr == null || arr.length == 0) {
            return;
        }

        for (int i = 0; i < arr.length; i++) {
            arr[i] = (byte) 0;
        }
    }
}
