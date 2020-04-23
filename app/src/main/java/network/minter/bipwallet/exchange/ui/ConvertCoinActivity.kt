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
import network.minter.bipwallet.exchange.views.ConvertCoinPresenter
import network.minter.bipwallet.internal.BaseMvpInjectActivity
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
//        ContextHelper.showTestnetBanner(this, binding.testNetWarning)
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
    }

    override fun setCurrentPage(page: Int) {
        binding.pager.currentItem = page
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
                return sTabs[position].newInstance()
            }

            override fun instantiateItem(container: ViewGroup, position: Int): Any {
                return super.instantiateItem(container, position)
            }

            override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
                super.destroyItem(container, position, `object`)
            }
        }
    }

    companion object {
        private val sTabs: List<Class<out ExchangeFragment>> = object : ArrayList<Class<out ExchangeFragment>>() {
            init {
                add(SellExchangeFragment::class.java)
                add(BuyExchangeFragment::class.java)
            }
        }
    }
}