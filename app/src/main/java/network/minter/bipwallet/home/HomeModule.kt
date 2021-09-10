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
package network.minter.bipwallet.home


import androidx.lifecycle.LifecycleOwner
import dagger.Module
import dagger.Provides
import network.minter.bipwallet.home.ui.HomeActivity
import network.minter.bipwallet.internal.Wallet
import network.minter.bipwallet.pools.ui.PoolsTabFragment
import network.minter.bipwallet.sending.ui.SendTabFragment
import network.minter.bipwallet.settings.ui.SettingsTabFragment
import network.minter.bipwallet.wallets.ui.WalletsTabFragment
import okhttp3.internal.immutableListOf
import java.lang.ref.WeakReference

/**
 * minter-android-wallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock></edward.vstock>@gmail.com>
 */
@Module
class HomeModule(activity: HomeActivity) {
    companion object {
        const val EXTRA_TAB = "EXTRA_TAB"
        const val EXTRA_MENU_ID = "EXTRA_MENU_ID"
        val TAB_COINS = WalletsTabFragment::class.java.name
        val TAB_SENDING = SendTabFragment::class.java.name
        val TAB_POOLS = PoolsTabFragment::class.java.name
        val TAB_SETTINGS = SettingsTabFragment::class.java.name
        var component: HomeComponent? = null
            private set

        fun create(rootView: HomeActivity): HomeComponent {
            component = DaggerHomeComponent.builder()
                    .walletComponent(Wallet.app())
                    .homeModule(HomeModule(rootView))
                    .build()
            return component!!
        }

        fun destroy() {
            component = null
        }
    }

    private val tabsClassesClient: List<Class<out HomeTabFragment>> = immutableListOf(
            WalletsTabFragment::class.java,
            SendTabFragment::class.java,
            PoolsTabFragment::class.java,
            SettingsTabFragment::class.java
    )
    private val mActivity: WeakReference<HomeActivity> = WeakReference(activity)

    @Provides
    @HomeScope
    fun provideLifecycleOwner(activity: HomeActivity): LifecycleOwner {
        return activity
    }

    @Provides
    @HomeScope
    fun provideHomeActivity(): HomeActivity {
        return mActivity.get()!!
    }

//    @Provides
//    @HomeScope
//    fun provideFragmentManager(activity: HomeActivity): FragmentManager {
//        return activity.fragmentManager
//    }

    @Provides
    @HomeScope
    fun provideSupportFragmentManager(activity: HomeActivity): androidx.fragment.app.FragmentManager {
        return activity.supportFragmentManager
    }

    @Provides
    @HomeScope
    @HomeTabsClasses
    fun provideTabsClasses(): @JvmSuppressWildcards List<Class<out HomeTabFragment>> {
        return tabsClassesClient
    }


}