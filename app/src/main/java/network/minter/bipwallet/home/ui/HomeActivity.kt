/*
 * Copyright (C) by MinterTeam. 2022
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
import android.os.Build
import android.os.Bundle
import android.transition.TransitionInflater
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import androidx.annotation.IdRes
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.google.android.material.bottomnavigation.BottomNavigationView
import moxy.presenter.InjectPresenter
import moxy.presenter.ProvidePresenter
import network.minter.bipwallet.R
import network.minter.bipwallet.databinding.ActivityHomeBinding
import network.minter.bipwallet.home.HomeModule
import network.minter.bipwallet.home.HomeTabFragment
import network.minter.bipwallet.home.HomeTabsClasses
import network.minter.bipwallet.home.contract.HomeView
import network.minter.bipwallet.home.views.HomePresenter
import network.minter.bipwallet.internal.BaseMvpActivity
import network.minter.bipwallet.internal.helpers.ErrorViewHelper
import network.minter.bipwallet.internal.helpers.ViewExtensions.visible
import network.minter.bipwallet.internal.helpers.ViewHelper
import network.minter.bipwallet.internal.system.ActivityBuilder
import network.minter.bipwallet.internal.system.BackPressedListener
import network.minter.bipwallet.stories.models.Story
import network.minter.bipwallet.stories.ui.StoriesPagerFragment
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import javax.inject.Provider

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock></edward.vstock>@gmail.com>
 */
class HomeActivity : BaseMvpActivity(), HomeView {
    @Inject
    lateinit var presenterProvider: Provider<HomePresenter>
    @InjectPresenter
    lateinit var presenter: HomePresenter

    @Inject
    @HomeTabsClasses
    lateinit var tabsClasses: @JvmSuppressWildcards List<Class<out HomeTabFragment>>

    private val mActiveTabs: WeakHashMap<Int, HomeTabFragment?> = WeakHashMap()
    private val mBackPressedListeners: MutableList<BackPressedListener> = ArrayList(1)
    private var mIsLowRamDevice = false
    private lateinit var b: ActivityHomeBinding
    private var storiesPagerFragment: StoriesPagerFragment? = null

    @get:VisibleForTesting
    val activeTabs: Map<Int, HomeTabFragment?>
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

    override fun onError(t: Throwable?) {
        ErrorViewHelper(b.errorView).onError(t)
    }

    override fun onError(err: String?) {
        ErrorViewHelper(b.errorView).onError(err)
    }

    override fun onErrorWithRetry(errorMessage: String?, errorResolver: View.OnClickListener?) {
        ErrorViewHelper(b.errorView).onErrorWithRetry(errorMessage, errorResolver)
    }

    override fun onErrorWithRetry(errorMessage: String?, actionName: String?, errorResolver: View.OnClickListener?) {
        ErrorViewHelper(b.errorView).onErrorWithRetry(errorMessage, actionName, errorResolver)
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
        if (closeStoriesPager()) {
            return
        }
        super.onBackPressed()
    }

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
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        HomeModule.create(this).inject(this)
        super.onCreate(savedInstanceState)
        b = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(b.root)

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
                mActiveTabs
                        .filter { it.value != null }
                        .filter { it.key != position }
                        .forEach { it.value?.onTabUnselected() }

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
        b.navigationBottom.labelVisibilityMode = BottomNavigationView.LABEL_VISIBILITY_LABELED
        b.navigationBottom.isItemHorizontalTranslationEnabled = false
//        b.navigationBottom.enableAnimation(true)
//        b.navigationBottom.setTextVisibility(true)
        b.navigationBottom.setOnItemSelectedListener { item ->
            runOnUiThread { b.homePager.currentItem = presenter.getBottomPositionById(item.itemId) }
            true
        }
    }

    fun startStoriesPager(stories: List<Story>, startPosition: Int, @Suppress("UNUSED_PARAMETER") sharedImage: View) {
        if (storiesPagerFragment != null) {
            return
        }
        b.navigationBottom.animate().translationY(b.navigationBottom.height.toFloat() * 2f).setDuration(150).start()
        b.storiesPager.visible = true

        storiesPagerFragment = StoriesPagerFragment.newInstance(stories, startPosition)
        storiesPagerFragment!!.sharedElementEnterTransition = TransitionInflater.from(this).inflateTransition(R.transition.image_shared_element_transition)

        supportFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
//                .addSharedElement(sharedImage, sharedImage.transitionName)
                .setCustomAnimations(
                        R.anim.stories_pager_enter,
                        R.anim.stories_pager_exit,
                        R.anim.stories_pager_enter,
                        R.anim.stories_pager_exit
                )
                .add(R.id.stories_pager, storiesPagerFragment!!, "stories_pager")
                .commit()
    }


    fun closeStoriesPager(): Boolean {
        if (storiesPagerFragment == null) return false
//        ViewHelper.setSystemBarsTranslucent(this)
        b.navigationBottom.animate().translationY(0f).setDuration(150).start()
        window.navigationBarColor = ContextCompat.getColor(this, R.color.greyBackground)
        ViewHelper.setSystemBarsLightness(this, false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.show(WindowInsets.Type.statusBars())
        } else {
            @Suppress("DEPRECATION")
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN or WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
        }
//        supportFragmentManager.popBackStack()
        supportFragmentManager.beginTransaction()
                .setCustomAnimations(
                        R.anim.stories_pager_enter,
                        R.anim.stories_pager_exit,
                        R.anim.stories_pager_enter,
                        R.anim.stories_pager_exit)
                .remove(storiesPagerFragment!!)
                .commit()
//        b.storiesPager.visible = false
        storiesPagerFragment = null
        return true
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