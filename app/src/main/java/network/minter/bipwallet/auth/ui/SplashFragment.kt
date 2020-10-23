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
package network.minter.bipwallet.auth.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import network.minter.bipwallet.R
import network.minter.bipwallet.databinding.FragmentSplashBinding
import network.minter.bipwallet.home.ui.HomeActivity
import network.minter.bipwallet.internal.BaseInjectFragment
import network.minter.bipwallet.internal.BaseMvpInjectActivity
import network.minter.bipwallet.internal.Wallet
import network.minter.bipwallet.internal.auth.AuthSession
import network.minter.bipwallet.internal.exceptions.FirebaseSafe
import network.minter.bipwallet.internal.storage.SecretStorage
import network.minter.bipwallet.security.SecurityModule
import network.minter.bipwallet.security.ui.PinEnterActivity
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock></edward.vstock>@gmail.com>
 */
class SplashFragment : BaseInjectFragment() {
    @Inject lateinit var session: AuthSession
    @Inject lateinit var secretStorage: SecretStorage
    private lateinit var b: FragmentSplashBinding
    private var mAuthSwitchActivity: AuthSwitchActivity? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        b = FragmentSplashBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_START_PIN_ENTER) {
            if (resultCode == Activity.RESULT_OK) {
                startHome()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Observable.timer(1, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    if (mAuthSwitchActivity == null) {
                        return@subscribe
                    }
                    if (!session.isLoggedIn(true) || Wallet.app().secretStorage().addresses.isEmpty()) {
                        Wallet.app().secretStorage().destroy()
                        Wallet.app().storageCache().deleteAll()
                        startAuth()
                        return@subscribe
                    }
                    if (secretStorage.hasPinCode()) {
                        startPinEnter()
                        return@subscribe
                    }
                    startHome()
                }) { t: Throwable? -> Timber.w(t, "Interrupted auth") }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        check(context is AuthSwitchActivity) { "Activity must implement AuthSwitchActivity" }
        mAuthSwitchActivity = context
    }

    override fun onDetach() {
        super.onDetach()
        mAuthSwitchActivity = null
    }

    private fun startPinEnter() {
        PinEnterActivity.Builder(activity!!, SecurityModule.PinMode.Validation)
                .startHomeOnSuccess()
                .startClearTop()
        activity!!.finish()
    }

    private fun startAuth() {
        ViewCompat.setTransitionName(b.logo, getString(R.string.transaction_auth_logo))
        mAuthSwitchActivity!!.showAuth(b.logo)
    }

    private fun startHome() {
        if (secretStorage.addresses.isNotEmpty()) {
            FirebaseSafe.setUserId(secretStorage.mainWallet.toString())
        }

        (activity as BaseMvpInjectActivity?)!!.startActivityClearTop(activity, HomeActivity::class.java)
        activity!!.finish()
    }

    interface AuthSwitchActivity {
        fun showAuth(sharedView: View)
    }

    companion object {
        private const val REQUEST_START_PIN_ENTER = 1030
    }
}