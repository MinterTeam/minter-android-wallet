/*
 * Copyright (C) by MinterTeam. 2019
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

package network.minter.bipwallet.tests.ui;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.test.espresso.ViewInteraction;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import network.minter.bipwallet.R;
import network.minter.bipwallet.tests.internal.TestWallet;
import network.minter.core.bip39.MnemonicResult;
import network.minter.core.bip39.NativeBip39;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static network.minter.bipwallet.internal.helpers.MathHelper.bdEQ;
import static network.minter.bipwallet.internal.helpers.MathHelper.bdGT;
import static network.minter.bipwallet.internal.helpers.MathHelper.bdGTE;
import static network.minter.bipwallet.internal.helpers.MathHelper.bdHuman;
import static network.minter.bipwallet.internal.helpers.MathHelper.bdLT;
import static network.minter.bipwallet.internal.helpers.MathHelper.bdLTE;
import static network.minter.bipwallet.tests.ui.actions.BottomNavigationExAction.selectCurrentItem;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
@SuppressWarnings("SameParameterValue")
public abstract class BaseUiTest {

    private ViewInteraction mBottomNavigation;
    private BigDecimal mMntBalance;
    private Disposable mDisposable;
    private AtomicBoolean mUpdatedBalance = new AtomicBoolean(false);

    public void setUp() {
        mMntBalance = new BigDecimal(0);
        mDisposable = TestWallet.app().accountStorageCache()
                .observe()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(res -> {
                    mMntBalance = res.getAccounts().get(0).getBalance();
                    mUpdatedBalance.set(true);
                });

    }

    public void tearDown() {
        mMntBalance = null;
        mDisposable.dispose();
        mUpdatedBalance.set(false);
    }

    protected void selectTab(int pos) {
        if (mBottomNavigation == null) {
            mBottomNavigation = onView(withId(R.id.navigation_bottom));
        }

        mBottomNavigation.perform(selectCurrentItem(pos));
    }

    protected MnemonicResult generateMnemonic() {
        SecureRandom random = new SecureRandom();
        return NativeBip39.encodeBytes(random.generateSeed(16));
    }

    protected void waitForBalance(BigDecimal balance) {
        waitForBalance(120, balance);
    }

    protected void waitForBalance(double balance) {
        waitForBalance(120, new BigDecimal(balance));
    }

    protected void waitForBalanceUpdate() {
        while (!mUpdatedBalance.get()) {
            try {
                Thread.sleep(1000);
                updateAccounts();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    protected void updateAccounts() {
        TestWallet.app().accountStorageCache().update(true);
    }

    protected void waitForBalance(int seconds, BigDecimal balance) {
        int times = 0;

        while (!bdEQ(mMntBalance, balance)) {
            try {
                Thread.sleep(1000);
                updateAccounts();
                times++;
                if (times >= seconds) {
                    throw new IllegalStateException(String.format("Balance is invalid: expected %s, given %s", bdHuman(balance), bdHuman(mMntBalance)));
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    protected void waitForBalanceGT(BigDecimal balance) {
        waitForBalanceGT(120, balance);
    }

    protected void waitForBalanceGT(double balance) {
        waitForBalanceGT(120, new BigDecimal(balance));
    }

    protected void waitForBalanceGT(int seconds, BigDecimal balance) {
        int times = 0;
        while (!bdGT(mMntBalance, balance)) {
            try {
                Thread.sleep(1000);
                updateAccounts();
                times++;
                if (times >= seconds) {
                    throw new IllegalStateException(String.format("Balance is invalid: expected %s > %s", bdHuman(balance), bdHuman(mMntBalance)));
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    protected void waitForBalanceLTE(double balance) {
        waitForBalanceLTE(120, new BigDecimal(balance));
    }

    protected void waitForBalanceLTE(BigDecimal balance) {
        waitForBalanceLTE(120, balance);
    }

    protected void waitForBalanceLTE(int seconds, BigDecimal balance) {
        int times = 0;
        while (!bdLTE(mMntBalance, balance)) {
            try {
                Thread.sleep(1000);
                updateAccounts();
                times++;
                if (times >= seconds) {
                    throw new IllegalStateException(String.format("Balance is invalid: expected %s ≤ %s", bdHuman(balance), bdHuman(mMntBalance)));
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    protected void waitForBalanceLT(double balance) {
        waitForBalanceLT(120, new BigDecimal(balance));
    }

    protected void waitForBalanceLT(BigDecimal balance) {
        waitForBalanceLT(120, balance);
    }

    protected void waitForBalanceLT(int seconds, BigDecimal balance) {
        int times = 0;
        while (!bdLT(mMntBalance, balance)) {
            try {
                Thread.sleep(1000);
                updateAccounts();
                times++;
                if (times >= seconds) {
                    throw new IllegalStateException(String.format("Balance is invalid: expected %s < %s", bdHuman(balance), bdHuman(mMntBalance)));
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    protected void waitForBalanceGTE(double balance) {
        waitForBalanceGTE(120, new BigDecimal(balance));
    }

    protected void waitForBalanceGTE(BigDecimal balance) {
        waitForBalanceGTE(120, balance);
    }

    protected void waitForBalanceGTE(int seconds, BigDecimal balance) {
        int times = 0;
        while (!bdGTE(mMntBalance, balance)) {
            try {
                Thread.sleep(1000);
                updateAccounts();
                times++;
                if (times >= seconds) {
                    throw new IllegalStateException(String.format("Balance is invalid: expected %s ≥ %s", bdHuman(balance), bdHuman(mMntBalance)));
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    protected void waitForBalanceMoreThanZero() {
        waitForBalanceMoreThanZero(120);
    }

    protected void waitForBalanceMoreThanZero(int seconds) {
        int times = 0;

        while (!bdGT(mMntBalance, BigDecimal.ZERO)) {
            try {
                Thread.sleep(1000);
                updateAccounts();
                times++;
                if (times >= seconds) {
                    throw new IllegalStateException(String.format("Balance is invalid: expected %s > 0", bdHuman(mMntBalance)));
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

