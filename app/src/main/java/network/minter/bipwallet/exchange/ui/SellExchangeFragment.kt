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
package network.minter.bipwallet.exchange.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import moxy.presenter.InjectPresenter
import moxy.presenter.ProvidePresenter
import network.minter.bipwallet.databinding.FragmentExchangeSellBinding
import network.minter.bipwallet.exchange.contract.SellExchangeView
import network.minter.bipwallet.exchange.views.SellExchangePresenter
import network.minter.bipwallet.internal.system.BroadcastReceiverManager
import network.minter.bipwallet.services.livebalance.broadcast.RTMBlockReceiver
import network.minter.bipwallet.wallets.utils.LastBlockHandler
import javax.inject.Inject
import javax.inject.Provider

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
class SellExchangeFragment : ExchangeFragment(), SellExchangeView {
    @Inject lateinit var presenterProvider: Provider<SellExchangePresenter>
    @InjectPresenter lateinit var presenter: SellExchangePresenter

    @ProvidePresenter
    fun providePresenter(): SellExchangePresenter {
        return presenterProvider.get()
    }

    private lateinit var itemBinding: FragmentExchangeSellBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        itemBinding = FragmentExchangeSellBinding.inflate(inflater, container, false)
        itemBinding.apply {
            binding = ExchangeBinding(
                    root,
                    inputOutgoingCoin,
                    inputIncomingCoin,
                    inputAmount,
                    calculationContainer,
                    action,
                    lastUpdated
            )

            LastBlockHandler.handle(lastUpdated)
            val broadcastManager = BroadcastReceiverManager(activity!!)
            broadcastManager.add(RTMBlockReceiver {
                LastBlockHandler.handle(lastUpdated)
            })
            broadcastManager.register()
        }

        return binding.root
    }


}