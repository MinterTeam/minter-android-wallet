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

package network.minter.bipwallet.internal.storage;

import com.orhanobut.hawk.Hawk;
import com.orhanobut.hawk.Storage;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

/**
 * MinterWallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public final class KVStorage implements Storage {
    @Override
    public <T> boolean put(String key, T value) {
        return Hawk.put(key, value);
    }

    @Override
    public <T> T get(String key) {
        try {
            return Hawk.get(key);
        } catch (Throwable t) {
            try {
                Hawk.delete(key);
            } catch (Throwable ignore) {
            }
            return null;
        }
    }

    public <T> Queue<T> getQueue(String key) {
        return new LinkedList<>(Hawk.<ArrayList<T>>get(key));
    }

    public <T> boolean putQueue(String key, Queue<T> queue) {
        return Hawk.put(key, queue);
    }

    public <T> T get(String key, T defaultValue) {
        try {
            return Hawk.get(key, defaultValue);
        } catch (Throwable t) {
            try {
                Hawk.delete(key);
            } catch (Throwable ignore) {
            }
            return defaultValue;
        }
    }

    @Override
    public boolean delete(String key) {
        return Hawk.delete(key);
    }

    @Override
    public boolean deleteAll() {
        return Hawk.deleteAll();
    }

    @Override
    public long count() {
        return Hawk.count();
    }

    @Override
    public boolean contains(String key) {
        return Hawk.contains(key);
    }
}
