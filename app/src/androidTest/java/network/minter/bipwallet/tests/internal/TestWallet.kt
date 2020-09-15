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
package network.minter.bipwallet.tests.internal

import network.minter.bipwallet.internal.Wallet
import network.minter.bipwallet.internal.di.HelpersModule
import network.minter.bipwallet.internal.di.RepoModule
import network.minter.bipwallet.internal.di.WalletComponent
import network.minter.bipwallet.tests.internal.di.DaggerTestWalletComponent
import network.minter.bipwallet.tests.internal.di.TestWalletModule

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock></edward.vstock>@gmail.com>
 */
class TestWallet : Wallet() {
    companion object {
        init {
            sEnableInject = false
        }

        /**
         * Usage:
         *
         *
         * Wallet.app().display().getWidth()
         * Wallet.app().res(); et cetera
         * @return WalletComponent
         * @see WalletComponent
         */
        fun app(): WalletComponent {
            return app
        }
    }


    override fun onCreate() {
        super.onCreate()
        app = DaggerTestWalletComponent.builder()
                .testWalletModule(TestWalletModule(this))
                .helpersModule(HelpersModule())
                .repoModule(RepoModule())
                .build()
        app.inject(this)
    }
}