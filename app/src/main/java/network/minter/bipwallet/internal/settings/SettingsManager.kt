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
package network.minter.bipwallet.internal.settings

import android.annotation.SuppressLint
import network.minter.bipwallet.BuildConfig
import network.minter.bipwallet.internal.storage.KVStorage

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
class Key<out T> internal constructor(
        name: String,
        val defaultValue: T
) {
    val name: String = BuildConfig.MINTER_STORAGE_VERS + "minter_prefs_" + name

    override fun toString(): String {
        return name
    }
}

@JvmField
val EnableLiveNotifications = Key("enable_live_notifications", false)

@JvmField
val CurrentBalanceCursor = Key("current_balance_cursor", 0)

@JvmField
val EnableSounds = Key("enable_sounds", false)

@JvmField
val EnablePinCode = Key("sec_enable_pin", false)

@JvmField
val EnableFingerprint = Key("sec_enable_fingerprint", false)

@JvmField
val LastBlockTime = Key("last_block_time", 0L)

@JvmField
val EnableStories = Key("enable_stories", true)

@SuppressLint("CommitPrefEdits")
class SettingsManager(private val storage: KVStorage) {

    operator fun <T> set(key: Key<T>, value: T) {
        storage.putAsync(key.name, value)
    }

    fun <T> setSync(key: Key<T>, value: T) {
        storage.put(key.name, value)
    }

    fun <T> remove(key: Key<T>) {
        if (!storage.contains(key.name)) return
        storage.deleteAsync(key.name)
    }

    fun <T> remoteSync(key: Key<T>) {
        if (!storage.contains(key.name)) return
        storage.delete(key.name)
    }

    fun <T> has(key: Key<T>): Boolean {
        return storage.contains(key.name)
    }

    operator fun <T> get(key: Key<T>): T {
        return storage[key.name, key.defaultValue]
    }
}