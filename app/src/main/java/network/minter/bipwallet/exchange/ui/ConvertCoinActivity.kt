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
package network.minter.bipwallet.exchange.ui

import android.app.Activity
import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentStatePagerAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.google.android.material.tabs.TabLayout.TabLayoutOnPageChangeListener
import moxy.presenter.InjectPresenter
import moxy.presenter.ProvidePresenter
import network.minter.bipwallet.databinding.ActivityConvertCoinBinding
import network.minter.bipwallet.exchange.contract.ConvertCoinView
import network.minter.bipwallet.exchange.models.ExchangeAmount
import network.minter.bipwallet.exchange.views.ConvertCoinPresenter
import network.minter.bipwallet.internal.BaseMvpInjectActivity
import network.minter.bipwallet.internal.helpers.ErrorViewHelper
import network.minter.bipwallet.internal.helpers.ViewExtensions.visibleForTestnet
import network.minter.bipwallet.internal.system.ActivityBuilder
import network.minter.core.crypto.MinterAddress
import network.minter.explorer.models.CoinItemBase
import org.parceler.Parcels
import java.math.BigDecimal
import java.util.*
import javax.inject.Inject
import javax.inject.Provider

/**
 * minter-android-wallet. 2018
 *
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
class ConvertCoinActivity : BaseMvpInjectActivity(), ConvertCoinView {
    @Inject lateinit var presenterProvider: Provider<ConvertCoinPresenter>
    @InjectPresenter lateinit var presenter: ConvertCoinPresenter

    private lateinit var binding: ActivityConvertCoinBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConvertCoinBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar(binding.toolbar)
        binding.tabs.tabGravity = TabLayout.GRAVITY_FILL

        binding.testnetWarning.visibleForTestnet()
        setResult(Activity.RESULT_CANCELED)
    }

    override fun setupTabs() {
        binding.pager.adapter = createTabsAdapter()
        binding.pager.addOnPageChangeListener(TabLayoutOnPageChangeListener(binding.tabs))
        binding.pager.offscreenPageLimit = 2
        binding.tabs.addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                binding.pager.currentItem = tab.position
                presenter.onTabSelected(tab.position)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        if (intent.hasExtra(EXTRA_COIN_TO_BUY)) {
            binding.pager.setCurrentItem(1, false)
        }
    }

    override fun setCurrentPage(page: Int) {
        binding.pager.currentItem = page
    }

    override fun onError(t: Throwable?) {
        ErrorViewHelper(binding.errorView).onError(t)
    }

    override fun onError(err: String?) {
        ErrorViewHelper(binding.errorView).onError(err)
    }

    override fun onErrorWithRetry(errorMessage: String?, errorResolver: View.OnClickListener?) {
        ErrorViewHelper(binding.errorView).onErrorWithRetry(errorMessage, errorResolver)
    }

    override fun onErrorWithRetry(errorMessage: String?, actionName: String?, errorResolver: View.OnClickListener?) {
        ErrorViewHelper(binding.errorView).onErrorWithRetry(errorMessage, actionName, errorResolver)
    }

    @ProvidePresenter
    fun providePresenter(): ConvertCoinPresenter {
        return presenterProvider.get()
    }

    @Suppress("DEPRECATION")
    private fun createTabsAdapter(): FragmentStatePagerAdapter {
        return object : FragmentStatePagerAdapter(supportFragmentManager) {
            override fun getCount(): Int {
                return 2
            }

            override fun getItem(position: Int): Fragment {
                return when (position) {
                    0 -> SellExchangeFragment.newInstance(intent)
                    1 -> BuyExchangeFragment.newInstance(intent)
                    else -> throw IllegalStateException("Unknown tab $position")
                }
            }

            override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
                super.destroyItem(container, position, `object`)
            }
        }
    }

    companion object {
        const val EXTRA_COIN_TO_BUY = "EXTRA_COIN_TO_BUY"
        const val EXTRA_VALUE_TO_BUY = "EXTRA_VALUE_TO_BUY"
        const val EXTRA_ACCOUNT = "EXTRA_ACCOUNT"
        const val RESULT_EXCHANGE_AMOUNT = "RESULT_EXCHANGE_AMOUNT"

        fun getResult(data: Intent): ExchangeAmount {
            return data.getParcelableExtra(RESULT_EXCHANGE_AMOUNT)!!
        }

        private val sTabs: List<Class<out ExchangeFragment>> = object : ArrayList<Class<out ExchangeFragment>>() {
            init {
                add(SellExchangeFragment::class.java)
                add(BuyExchangeFragment::class.java)
            }
        }
    }

    class Builder : ActivityBuilder {
        private var mCoinToBuy: CoinItemBase? = null
        private var mValueToBuy: BigDecimal = BigDecimal.ZERO
        private var mFromAccount: MinterAddress? = null

        constructor(from: Activity) : super(from)
        constructor(from: Fragment) : super(from)
        constructor(from: Service) : super(from)

        fun buyCoins(coin: CoinItemBase, value: BigDecimal): Builder {
            mCoinToBuy = coin
            mValueToBuy = value
            return this
        }

        fun withAccount(account: MinterAddress): Builder {
            mFromAccount = account
            return this
        }

        override fun onBeforeStart(intent: Intent) {
            super.onBeforeStart(intent)
            if (mCoinToBuy != null) {
                intent.putExtra(EXTRA_COIN_TO_BUY, Parcels.wrap(mCoinToBuy))
                intent.putExtra(EXTRA_VALUE_TO_BUY, mValueToBuy.toPlainString())
            }
            if (mFromAccount != null) {
                intent.putExtra(EXTRA_ACCOUNT, mFromAccount)
            }
        }

        override fun getActivityClass(): Class<*> {
            return ConvertCoinActivity::class.java
        }
    }
}