/*
 * Copyright (C) by MinterTeam. 2021
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
package network.minter.bipwallet.internal.helpers

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import network.minter.bipwallet.internal.common.Acceptor
import org.parceler.Parcels

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
object IntentHelper {
    @JvmStatic
    fun <T> getParcelExtraOrError(intent: Intent, name: String): T {
        return getParcelExtraOrError(intent, name, String.format("Intent does not have extra: %s", name))
    }

    @JvmStatic
    fun <T> getParcelExtraOrError(intent: Intent, name: String, errorMessage: String?): T {
        return getParcelExtra(intent, name) ?: throw IllegalArgumentException(errorMessage)
    }

    @JvmStatic
    fun <T> getParcelExtra(intent: Intent, name: String): T? {
        return getParcelExtra(intent, name, null)
    }

    @JvmStatic
    fun <T> getParcelExtraOrError(bundle: Bundle?, name: String, errorMessage: String?): T {
        return getParcelExtra(bundle, name, null) ?: throw IllegalArgumentException(errorMessage)
    }

    @JvmStatic
    fun <T> getParcelExtra(bundle: Bundle?, name: String): T? {
        return getParcelExtra(bundle, name, null)
    }

    @JvmStatic
    fun <T> getParcelExtra(bundle: Bundle?, name: String, callback: Acceptor<T>?): T? {
        if (bundle == null || !bundle.containsKey(name)) {
            return null
        }
        val data: T = Parcels.unwrap(bundle.getParcelable(name))
        callback?.accept(data)
        return data
    }

    @JvmStatic
    fun <T> getParcelExtra(intent: Intent?, name: String, callback: Acceptor<T>?): T? {
        if (intent == null || !intent.hasExtra(name)) {
            return null
        }
        val data: T = Parcels.unwrap(intent.getParcelableExtra(name))
        callback?.accept(data)
        return data
    }

    @JvmStatic
    fun newUrl(url: String?): Intent {
        return Intent(Intent.ACTION_VIEW, Uri.parse(url))
    }

    fun <T> T.toParcel(): Parcelable {
        return Parcels.wrap(this)
    }
}
