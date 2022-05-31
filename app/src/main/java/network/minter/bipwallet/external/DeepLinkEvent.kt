/*
 * Copyright (C) by MinterTeam. 2022
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
package network.minter.bipwallet.external

import android.app.Activity
import android.content.Intent
import com.airbnb.deeplinkdispatch.DeepLink
import com.airbnb.deeplinkdispatch.DeepLinkEntry
import com.airbnb.deeplinkdispatch.DeepLinkHandler
import com.airbnb.deeplinkdispatch.DeepLinkUri

/**
 * Minter. 2019
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
class DeepLinkEvent(val intent: Intent) {
    var uri: String? = null
    private val mModuleLoader: DeepLinkModuleRegistry = DeepLinkModuleRegistry()
    val entry: DeepLinkEntry?
    private val mModuleHelper: DeepLinkModuleRegistryHelper<DeepLinkModuleRegistry> = DeepLinkModuleRegistryHelper<DeepLinkModuleRegistry>(mModuleLoader)
    val supportedActivity: Class<*>?
        get() = entry?.activityClass

    fun isSupportedActivity(handler: Activity): Boolean {
        return isSupportedActivity(handler.javaClass)
    }

    fun isSupportedActivity(handlerClass: Class<*>): Boolean {
        return if (entry == null) false else entry.activityClass == handlerClass
    }

    init {
        val isCorrectIntent = intent.extras!!.getBoolean(DeepLink.IS_DEEP_LINK, false)
        if (isCorrectIntent) {
            uri = intent.extras!!.getString(DeepLink.URI)
        } else {
            uri = intent.extras!!.getString(DeepLinkHandler.EXTRA_URI)
        }
        entry = mModuleHelper.parseUri(uri)
        if (entry != null && !isCorrectIntent) {
            intent.putExtra(DeepLink.IS_DEEP_LINK, true)
            intent.putExtra(DeepLink.URI, uri)

            entry.getParameters(DeepLinkUri.parse(uri))
                    .forEach { entry ->
                        intent.putExtra(entry.key, entry.value)
                    }
        }
    }
}