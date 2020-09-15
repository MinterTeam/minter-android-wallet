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
package network.minter.bipwallet.home.ui

import android.app.Activity
import android.app.ActivityManager
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.annotation.IdRes
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import butterknife.ButterKnife
import com.annimon.stream.Stream
import com.google.android.material.bottomnavigation.LabelVisibilityMode
import moxy.presenter.InjectPresenter
import moxy.presenter.ProvidePresenter
import network.minter.bipwallet.databinding.ActivityHomeBinding
import network.minter.bipwallet.home.HomeModule
import network.minter.bipwallet.home.HomeTabFragment
import network.minter.bipwallet.home.HomeTabsClasses
import network.minter.bipwallet.home.contract.HomeView
import network.minter.bipwallet.home.views.HomePresenter
import network.minter.bipwallet.internal.BaseMvpActivity
import network.minter.bipwallet.internal.Wallet
import network.minter.bipwallet.internal.system.ActivityBuilder

import network.minter.bipwallet.internal.system.BackPressedListener
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import javax.inject.Provider

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock></edward.vstock>@gmail.com>
 */
class HomeActivity : BaseMvpActivity(), HomeView {
    @Inject lateinit var presenterProvider: Provider<HomePresenter>
    @InjectPresenter lateinit var presenter: HomePresenter

    @Inject
    @HomeTabsClasses
    lateinit var tabsClasses: @JvmSuppressWildcards List<Class<out HomeTabFragment>>

    private val mActiveTabs: WeakHashMap<Int, HomeTabFragment?> = WeakHashMap()
    private val mBackPressedListeners: MutableList<BackPressedListener> = ArrayList(1)
    private var mIsLowRamDevice = false
    private lateinit var b: ActivityHomeBinding

    @get:VisibleForTesting val activeTabs: Map<Int, HomeTabFragment?>
        get() = mActiveTabs

    override fun setCurrentPage(position: Int) {
        runOnUiThread {
            try {
                b.homePager.currentItem = position
            } catch (e: IllegalStateException) {
                Timber.w(e, "Unable to set current item")
            }
        }
    }

    val currentTabFragment: HomeTabFragment?
        get() = mActiveTabs[b.homePager.currentItem]

    override fun startUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    override fun startRemoteTransaction(txHash: String) {}


    override fun onBackPressed() {
        for (listener in mBackPressedListeners) {
            if (!listener.onBackPressed()) {
                return
            }
        }
        super.onBackPressed()
    }

    override fun showProgress() {}
    override fun hideProgress() {}
    fun setCurrentPageById(@IdRes tab: Int) {
        val pos = presenter.getBottomPositionById(tab)
        setCurrentPage(pos)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        if (b.homePager.offscreenPageLimit > 1) {
            b.homePager.offscreenPageLimit = 1
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        for (i in 0 until mActiveTabs.size) {
            mActiveTabs[i]?.onTrimMemory(level)
        }
    }

    @ProvidePresenter
    fun providePresenter(): HomePresenter {
        return presenterProvider.get()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        for ((key, f) in mActiveTabs) {
            if (f != null) {
                Timber.d("%s.onActivityResult", f.javaClass)
                f.onActivityResult(requestCode, resultCode, data)
            } else {
                Timber.d("Fragment %d is null", key)
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onDestroy() {
        super.onDestroy()
        HomeModule.destroy()
        Wallet.app().ledger().destroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        HomeModule.create(this).inject(this)
        super.onCreate(savedInstanceState)
        b = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(b.root)

        ButterKnife.bind(this)
        val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager?
        if (am != null) {
            mIsLowRamDevice = am.isLowRamDevice
        }
        presenter.handleExtras(intent)
        setupTabAdapter()
        setupBottomNavigation()
    }

    private fun setupTabAdapter() {
        val adapter: FragmentStateAdapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int {
                return tabsClasses.size
            }

            override fun createFragment(position: Int): Fragment {
                Timber.d("Get item by position: %d", position)
                var fragment: HomeTabFragment? = null
                try {
                    fragment = tabsClasses[position].newInstance()
                } catch (e: InstantiationException) {
                    e.printStackTrace()
                } catch (e: IllegalAccessException) {
                    e.printStackTrace()
                }
                if (fragment == null) {
                    throw NullPointerException("Wtf?")
                }
                if (intent != null && intent.extras != null) {
                    fragment.arguments = intent.extras
                }
                mActiveTabs[position] = fragment
                return fragment
            }

        }
        b.homePager.offscreenPageLimit = if (mIsLowRamDevice) 1 else 4
        b.homePager.registerOnPageChangeCallback(object : OnPageChangeCallback() {

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                presenter.onPageSelected(position)
                Stream.of<Map.Entry<Int, HomeTabFragment?>>(mActiveTabs.entries)
                        .filter { item: Map.Entry<Int, HomeTabFragment?> -> item.value != null }
                        .filter { item: Map.Entry<Int, HomeTabFragment?> -> item.key != position }
                        .forEach { item: Map.Entry<Int, HomeTabFragment?> -> item.value!!.onTabUnselected() }
                if (mActiveTabs[position] != null && mActiveTabs[position] != null) {
                    val f = mActiveTabs[position]
                    f?.onTabSelected()
                }
                b.navigationBottom.selectedItemId = presenter.getBottomIdByPosition(position)
            }

        })
        b.homePager.adapter = adapter
    }

    private fun setupBottomNavigation() {
        b.navigationBottom.labelVisibilityMode = LabelVisibilityMode.LABEL_VISIBILITY_LABELED
        b.navigationBottom.isItemHorizontalTranslationEnabled = false
        b.navigationBottom.enableAnimation(true)
        b.navigationBottom.setTextVisibility(true)
        b.navigationBottom.setOnNavigationItemSelectedListener { item ->
            runOnUiThread { b.homePager.currentItem = presenter.getBottomPositionById(item.itemId) }
            true
        }
    }

    class Builder : ActivityBuilder {
        constructor(from: Activity) : super(from)
        constructor(from: Fragment) : super(from)
        constructor(from: Service) : super(from)

        override fun getActivityClass(): Class<*> {
            return HomeActivity::class.java
        }
    }
}