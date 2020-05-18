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

package network.minter.bipwallet.wallets.selector

import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import network.minter.bipwallet.BuildConfig
import network.minter.bipwallet.internal.system.BaseBroadcastReceiver

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
class WalletSelectorBroadcastReceiver(
        private val listener: Listener
) : BaseBroadcastReceiver() {


    companion object {
        const val BROADCAST_ACTION = BuildConfig.APPLICATION_ID + ".WALLET_SELECTOR_UPDATE"
        private const val EXTRA_ACTION = "ACTION"
        private const val EXTRA_WALLETS = "WALLET_LIST"
        private const val EXTRA_MAIN_WALLET = "MAIN_WALLET"

        fun setWallets(context: Context, wallets: List<WalletItem>) {
            val intent = Intent(BROADCAST_ACTION)
            intent.putExtra(EXTRA_ACTION, Action.FillWallets.ordinal)
            val al = ArrayList(wallets)
            intent.putParcelableArrayListExtra(EXTRA_WALLETS, al)

            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
        }

        fun setMainWallet(context: Context, mainWallet: WalletItem) {
            val intent = Intent(BROADCAST_ACTION)
            intent.putExtra(EXTRA_ACTION, Action.SetMain.ordinal)
            intent.putExtra(EXTRA_MAIN_WALLET, mainWallet)

            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
        }
    }

    enum class Action {
        FillWallets,
        SetMain
    }

    override fun getActionName(): String {
        return BROADCAST_ACTION
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)

        if (intent?.hasExtra(EXTRA_ACTION) == true) {
            val actionInt = intent.getIntExtra(EXTRA_ACTION, -1)
            if (actionInt >= 0) {
                when (Action.values()[actionInt]) {
                    Action.FillWallets -> {
                        val wallets = intent.getParcelableArrayListExtra<WalletItem>(EXTRA_WALLETS)
                        listener.onFillWallets(wallets!!)
                    }
                    Action.SetMain -> {
                        val wallet = intent.getParcelableExtra<WalletItem>(EXTRA_MAIN_WALLET)
                        listener.onSetMainWallet(wallet!!)
                    }
                }
            }
        }
    }

    interface Listener {
        fun onFillWallets(wallets: java.util.ArrayList<WalletItem>)
        fun onSetMainWallet(wallet: WalletItem)
    }
}