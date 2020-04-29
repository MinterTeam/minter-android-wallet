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
package network.minter.bipwallet.services.livebalance.broadcast

import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import network.minter.bipwallet.BuildConfig
import network.minter.bipwallet.internal.Wallet
import network.minter.bipwallet.internal.system.BaseBroadcastReceiver
import network.minter.bipwallet.services.livebalance.models.RTMBlock
import org.joda.time.DateTime
import java.math.BigDecimal

class RTMBlockReceiver(private val listener: (DateTime) -> Unit) : BaseBroadcastReceiver() {

    companion object {
        const val BROADCAST_ACTION = BuildConfig.APPLICATION_ID + ".RTM_BLOCK_COMMIT_RECEIVER"
        const val EXTRA_BLOCK_DATA = "EXTRA_BLOCK_DATA"

        /**
         * @param context Context
         * @param id if null, will cancel all current tasks
         */
        @JvmStatic
        fun send(context: Context, blockJson: String) {
            val intent = Intent(BROADCAST_ACTION)
            val gson = Wallet.app().gsonBuilder().create()
            val block = gson.fromJson(blockJson, RTMBlock::class.java)
            intent.putExtra(EXTRA_BLOCK_DATA, block)
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
        }

        fun send(context: Context, dt: DateTime) {
            val intent = Intent(BROADCAST_ACTION)
            val block = RTMBlock(0, 0, 0, BigDecimal.ZERO, dt, BigDecimal.ZERO, "")
            intent.putExtra(EXTRA_BLOCK_DATA, block)
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val block: RTMBlock = intent.getParcelableExtra(EXTRA_BLOCK_DATA) ?: return
        listener(block.timestamp)
    }

    override fun getActionName(): String {
        return BROADCAST_ACTION
    }


}