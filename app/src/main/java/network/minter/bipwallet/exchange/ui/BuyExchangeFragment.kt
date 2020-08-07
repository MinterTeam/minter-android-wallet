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

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import moxy.presenter.InjectPresenter
import moxy.presenter.ProvidePresenter
import network.minter.bipwallet.databinding.FragmentExchangeBuyBinding
import network.minter.bipwallet.exchange.contract.BuyExchangeView
import network.minter.bipwallet.exchange.views.BuyExchangePresenter
import network.minter.bipwallet.internal.helpers.KeyboardHelper
import network.minter.bipwallet.internal.system.BroadcastReceiverManager
import network.minter.bipwallet.services.livebalance.broadcast.RTMBlockReceiver
import network.minter.bipwallet.wallets.utils.LastBlockHandler
import javax.inject.Inject
import javax.inject.Provider

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
class BuyExchangeFragment : ExchangeFragment(), BuyExchangeView {
    @JvmField @Inject
    var presenterProvider: Provider<BuyExchangePresenter>? = null

    @JvmField @InjectPresenter
    var presenter: BuyExchangePresenter? = null

    private lateinit var itemBinding: FragmentExchangeBuyBinding

    companion object {
        fun newInstance(intent: Intent?): BuyExchangeFragment {
            val args = Bundle()
            val instance = BuyExchangeFragment()
            if (intent != null && intent.extras != null) {
                args.putAll(intent.extras)
            }

            instance.arguments = args
            return instance

        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        itemBinding = FragmentExchangeBuyBinding.inflate(inflater, container, false)
        itemBinding.apply {
            inputAmount.input.setOnEditorActionListener { _, _, _ ->
                KeyboardHelper.hideKeyboard(this@BuyExchangeFragment)
                false
            }
            inputIncomingCoin.input.setOnEditorActionListener { _, _, _ ->
                inputIncomingCoin.input.requestFocus()
                false
            }


            binding = ExchangeBinding(
                    root,
                    inputOutgoingCoin,
                    inputIncomingCoin,
                    inputAmount,
                    calculationContainer,
                    action,
                    lastUpdated
            )

            presenter!!.handleExtras(arguments)

            LastBlockHandler.handle(lastUpdated)
            val broadcastManager = BroadcastReceiverManager(activity!!)
            broadcastManager.add(RTMBlockReceiver {
                LastBlockHandler.handle(lastUpdated, it)
            })
            broadcastManager.register()
        }

        return binding.root
    }

    @ProvidePresenter
    fun providePresenter(): BuyExchangePresenter {
        return presenterProvider!!.get()
    }


}