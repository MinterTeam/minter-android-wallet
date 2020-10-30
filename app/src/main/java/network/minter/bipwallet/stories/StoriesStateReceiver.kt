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

package network.minter.bipwallet.stories

import android.content.Context
import android.content.Intent
import network.minter.bipwallet.BuildConfig
import network.minter.bipwallet.apis.reactive.sendLocalBroadcast
import network.minter.bipwallet.internal.system.BaseBroadcastReceiver

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
class StoriesStateReceiver(
        private val stateListener: (Boolean) -> Unit
) : BaseBroadcastReceiver() {

    companion object {
        const val BROADCAST_ACTION = BuildConfig.APPLICATION_ID + ".STORIES_STATE"
        const val EXTRA_STATE = "EXTRA_STATE"

        /**
         * @param context Context
         * @param id if null, will cancel all current tasks
         */
        @JvmStatic
        fun send(context: Context?, state: Boolean) {
            val intent = Intent(BROADCAST_ACTION)
            intent.putExtra(EXTRA_STATE, state)
            context.sendLocalBroadcast(intent)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        stateListener(intent.getBooleanExtra(EXTRA_STATE, true))
    }

    override fun getActionName(): String {
        return BROADCAST_ACTION
    }
}