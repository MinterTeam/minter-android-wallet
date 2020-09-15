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
package network.minter.bipwallet.internal.storage

import com.orhanobut.hawk.Hawk
import com.orhanobut.hawk.Storage
import com.orhanobut.hawk.StorageBatch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * minter-android-wallet. 2018
 *
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
open class KVStorage : Storage {
    private var mDbName = Hawk.DEFAULT_DB_TAG
    private val dataLock: MutableMap<String, Any> = ConcurrentHashMap()

    constructor()
    constructor(dbTag: String) {
        mDbName = dbTag
    }

    private fun makeLock(key: String): Any {
        if (!dataLock.containsKey(key)) {
            dataLock[key] = Any()
        }
        return dataLock[key]!!
    }

    override fun <T> put(key: String, value: T): Boolean {
        return synchronized(makeLock(key)) {
            Hawk.db(mDbName).put(key, value)
        }
    }

    fun <T> putAsync(key: String, value: T): Job {
        return CoroutineScope(Dispatchers.IO).launch {
            put(key, value)
        }
    }

    override fun batch(batch: StorageBatch) {
        Hawk.db(mDbName).batch(batch)
    }

    override fun <T> get(key: String): T? {
        return synchronized(makeLock(key)) {
            try {
                Hawk.db(mDbName).get<T>(key)
            } catch (t: Throwable) {
                Timber.w(t, "Unable to get value from kvstorage: %s", key)
                try {
                    Hawk.db(mDbName).delete(key)
                } catch (ignore: Throwable) {
                    Timber.w(ignore)
                }
                null
            }
        }
    }

    open fun <T> getQueue(key: String): Queue<T>? {
        return LinkedList(Hawk.db(mDbName).get<ArrayList<T>>(key))
    }

    open fun <T> putQueue(key: String, queue: Queue<T>?): Boolean {
        return Hawk.db(mDbName).put(key, queue)
    }

    open operator fun <T> get(key: String, defaultValue: T): T {
        return get<T>(key) ?: defaultValue
    }

    override fun delete(key: String): Boolean {
        return synchronized(makeLock(key)) {
            if (!contains(key)) {
                false
            } else {
                Hawk.db(mDbName).delete(key)
            }
        }
    }

    fun deleteAsync(key: String) {
        CoroutineScope(Dispatchers.IO).launch {
            delete(key)
        }
    }

    override fun deleteAll(): Boolean {
        return Hawk.db(mDbName).deleteAll()
    }

    override fun count(): Long {
        return Hawk.db(mDbName).count()
    }

    override fun contains(key: String): Boolean {
        return Hawk.db(mDbName).contains(key)
    }
}