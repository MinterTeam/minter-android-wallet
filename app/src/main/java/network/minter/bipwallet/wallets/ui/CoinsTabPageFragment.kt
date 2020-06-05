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
package network.minter.bipwallet.wallets.ui

import android.content.Intent
import moxy.ktx.moxyPresenter
import network.minter.bipwallet.exchange.ui.ConvertCoinActivity
import network.minter.bipwallet.wallets.contract.CoinsTabPageView
import network.minter.bipwallet.wallets.views.CoinsTabPagePresenter
import javax.inject.Inject
import javax.inject.Provider

class CoinsTabPageFragment : BaseTabPageFragment(), CoinsTabPageView {
    @Inject lateinit var presenterProvider: Provider<CoinsTabPagePresenter>
    private val presenter by moxyPresenter { presenterProvider.get() }

    override fun getTabType(): TabType {
        return TabType.Coins
    }

    override fun startConvert() {
        startActivity(Intent(activity, ConvertCoinActivity::class.java))
    }
}