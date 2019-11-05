/*
 * Copyright (C) by MinterTeam. 2019
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
import com.orhanobut.hawk.StorageBatch;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import timber.log.Timber;

/**
 * minter-android-wallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class KVStorage implements Storage {
    private String mDbName = Hawk.DEFAULT_DB_TAG;

    public KVStorage() {
    }

    public KVStorage(String dbTag) {
        mDbName = dbTag;
    }

    @Override
    public <T> boolean put(String key, T value) {
        return Hawk.db(mDbName).put(key, value);
    }

    @Override
    public void batch(StorageBatch batch) {
        Hawk.db(mDbName).batch(batch);
    }

    @Override
    public <T> T get(String key) {
        try {
            return Hawk.db(mDbName).get(key);
        } catch (Throwable t) {
            Timber.w(t);
            try {
                Hawk.db(mDbName).delete(key);
            } catch (Throwable ignore) {
                Timber.w(ignore);
            }
            return null;
        }
    }

    public <T> Queue<T> getQueue(String key) {
        return new LinkedList<>(Hawk.db(mDbName).<ArrayList<T>>get(key));
    }

    public <T> boolean putQueue(String key, Queue<T> queue) {
        return Hawk.db(mDbName).put(key, queue);
    }

    public <T> T get(String key, T defaultValue) {
        try {
            return Hawk.db(mDbName).get(key, defaultValue);
        } catch (Throwable t) {
            Timber.w(t);
            try {
                Hawk.db(mDbName).delete(key);
            } catch (Throwable ignore) {
                Timber.w(ignore);
            }
            return defaultValue;
        }
    }

    @Override
    public boolean delete(String key) {
        return Hawk.db(mDbName).delete(key);
    }

    @Override
    public boolean deleteAll() {
        return Hawk.db(mDbName).deleteAll();
    }

    @Override
    public long count() {
        return Hawk.db(mDbName).count();
    }

    @Override
    public boolean contains(String key) {
        return Hawk.db(mDbName).contains(key);
    }
}
