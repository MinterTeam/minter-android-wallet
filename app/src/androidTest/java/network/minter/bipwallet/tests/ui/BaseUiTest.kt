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
package network.minter.bipwallet.tests.ui

import androidx.test.espresso.Espresso
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.matcher.ViewMatchers
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import network.minter.bipwallet.R
import network.minter.bipwallet.internal.helpers.MathHelper.bdEQ
import network.minter.bipwallet.internal.helpers.MathHelper.bdGT
import network.minter.bipwallet.internal.helpers.MathHelper.bdGTE
import network.minter.bipwallet.internal.helpers.MathHelper.bdHuman
import network.minter.bipwallet.internal.helpers.MathHelper.bdLT
import network.minter.bipwallet.internal.helpers.MathHelper.bdLTE
import network.minter.bipwallet.internal.storage.models.AddressListBalancesTotal
import network.minter.bipwallet.tests.internal.TestWallet
import network.minter.bipwallet.tests.ui.actions.BottomNavigationExAction
import network.minter.core.bip39.MnemonicResult
import network.minter.core.bip39.NativeBip39
import java.math.BigDecimal
import java.security.SecureRandom
import java.util.concurrent.atomic.AtomicBoolean

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
abstract class BaseUiTest {
    private var mBottomNavigation: ViewInteraction? = null
    private var mMntBalance: BigDecimal? = null
    private var mDisposable: Disposable? = null
    private val mUpdatedBalance = AtomicBoolean(false)
    open fun setUp() {
        mMntBalance = BigDecimal(0)
        mDisposable = TestWallet.app().accountStorageCache()
                .observe()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe { res: AddressListBalancesTotal ->
                    val balance = res.find(TestWallet.app().secretStorage().mainWallet)

                    if (balance.isPresent) {
                        mMntBalance = balance.get().totalBalance
                    }
                    mUpdatedBalance.set(true)
                }
    }

    open fun tearDown() {
        mMntBalance = BigDecimal.ZERO
        mDisposable!!.dispose()
        mUpdatedBalance.set(false)
    }

    protected fun selectTab(pos: Int) {
        if (mBottomNavigation == null) {
            mBottomNavigation = Espresso.onView(ViewMatchers.withId(R.id.navigation_bottom))
        }
        mBottomNavigation!!.perform(BottomNavigationExAction.selectCurrentItem(pos))
    }

    protected fun generateMnemonic(): MnemonicResult {
        val random = SecureRandom()
        return NativeBip39.encodeBytes(random.generateSeed(16))
    }

    protected fun waitForBalance(balance: BigDecimal?) {
        waitForBalance(120, balance)
    }

    protected fun waitForBalance(balance: Double) {
        waitForBalance(120, BigDecimal(balance))
    }

    protected fun waitForBalanceUpdate() {
        while (!mUpdatedBalance.get()) {
            try {
                Thread.sleep(1000)
                updateAccounts()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    protected fun updateAccounts() {
        TestWallet.app().accountStorageCache().update(true)
    }

    protected fun waitForBalance(seconds: Int, balance: BigDecimal?) {
        var times = 0
        while (!bdEQ(mMntBalance, balance)) {
            try {
                Thread.sleep(1000)
                updateAccounts()
                times++
                check(times < seconds) { String.format("Balance is invalid: expected %s, given %s", bdHuman(balance!!), bdHuman(mMntBalance!!)) }
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    protected fun waitForBalanceGT(balance: BigDecimal?) {
        waitForBalanceGT(120, balance)
    }

    protected fun waitForBalanceGT(balance: Double) {
        waitForBalanceGT(120, BigDecimal(balance))
    }

    protected fun waitForBalanceGT(seconds: Int, balance: BigDecimal?) {
        var times = 0
        while (!bdGT(mMntBalance, balance)) {
            try {
                Thread.sleep(1000)
                updateAccounts()
                times++
                check(times < seconds) { String.format("Balance is invalid: expected %s > %s", bdHuman(balance!!), bdHuman(mMntBalance!!)) }
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    protected fun waitForBalanceLTE(balance: Double) {
        waitForBalanceLTE(120, BigDecimal(balance))
    }

    protected fun waitForBalanceLTE(balance: BigDecimal?) {
        waitForBalanceLTE(120, balance)
    }

    protected fun waitForBalanceLTE(seconds: Int, balance: BigDecimal?) {
        var times = 0
        while (!bdLTE(mMntBalance, balance)) {
            try {
                Thread.sleep(1000)
                updateAccounts()
                times++
                check(times < seconds) { String.format("Balance is invalid: expected %s ≤ %s", bdHuman(balance!!), bdHuman(mMntBalance!!)) }
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    protected fun waitForBalanceLT(balance: Double) {
        waitForBalanceLT(120, BigDecimal(balance))
    }

    protected fun waitForBalanceLT(balance: BigDecimal?) {
        waitForBalanceLT(120, balance)
    }

    protected fun waitForBalanceLT(seconds: Int, balance: BigDecimal?) {
        var times = 0
        while (!bdLT(mMntBalance, balance)) {
            try {
                Thread.sleep(1000)
                updateAccounts()
                times++
                check(times < seconds) { String.format("Balance is invalid: expected %s < %s", bdHuman(balance!!), bdHuman(mMntBalance!!)) }
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    protected fun waitForBalanceGTE(balance: Double) {
        waitForBalanceGTE(120, BigDecimal(balance))
    }

    protected fun waitForBalanceGTE(balance: BigDecimal?) {
        waitForBalanceGTE(120, balance)
    }

    protected fun waitForBalanceGTE(seconds: Int, balance: BigDecimal?) {
        var times = 0
        while (!bdGTE(mMntBalance, balance)) {
            try {
                Thread.sleep(1000)
                updateAccounts()
                times++
                check(times < seconds) { String.format("Balance is invalid: expected %s ≥ %s", bdHuman(balance!!), bdHuman(mMntBalance!!)) }
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    protected fun waitForBalanceMoreThanZero(seconds: Int = 120) {
        var times = 0
        while (!bdGT(mMntBalance, BigDecimal.ZERO)) {
            try {
                Thread.sleep(1000)
                updateAccounts()
                times++
                check(times < seconds) { String.format("Balance is invalid: expected %s > 0", bdHuman(mMntBalance!!)) }
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }
}