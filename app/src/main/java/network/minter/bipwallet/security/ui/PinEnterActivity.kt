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
package network.minter.bipwallet.security.ui

import android.app.Activity
import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.PromptInfo
import androidx.fragment.app.Fragment
import butterknife.ButterKnife
import moxy.presenter.InjectPresenter
import moxy.presenter.ProvidePresenter
import network.minter.bipwallet.R
import network.minter.bipwallet.auth.ui.AuthActivity
import network.minter.bipwallet.databinding.ActivityPinBinding
import network.minter.bipwallet.home.ui.HomeActivity
import network.minter.bipwallet.internal.BaseMvpInjectActivity
import network.minter.bipwallet.internal.dialogs.DialogExecutor
import network.minter.bipwallet.internal.dialogs.WalletDialog
import network.minter.bipwallet.internal.dialogs.WalletDialog.Companion.releaseDialog
import network.minter.bipwallet.internal.dialogs.WalletDialog.Companion.switchDialogWithExecutor
import network.minter.bipwallet.internal.system.ActivityBuilder
import network.minter.bipwallet.internal.views.widgets.PinFingerprintClickListener
import network.minter.bipwallet.internal.views.widgets.PinValidateErrorListener
import network.minter.bipwallet.internal.views.widgets.PinValueListener
import network.minter.bipwallet.security.SecurityModule
import network.minter.bipwallet.security.SecurityModule.KeypadListener
import network.minter.bipwallet.security.SecurityModule.PinMode
import network.minter.bipwallet.security.contract.PinEnterView
import network.minter.bipwallet.security.views.PinEnterPresenter
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Provider

/**
 * minter-android-wallet. 2019
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
class PinEnterActivity : BaseMvpInjectActivity(), PinEnterView {
    @Inject lateinit var presenterProvider: Provider<PinEnterPresenter>
    @InjectPresenter lateinit var presenter: PinEnterPresenter

    private lateinit var b: ActivityPinBinding
    private var mCurrentDialog: WalletDialog? = null

    override fun setKeypadListener(listener: KeypadListener) {}
    override fun setupTitle(title: Int) {
        b.title.setText(title)
    }

    override fun setEnableValidation(pin: String?) {
        if (pin == null) {
            b.pinpad.setEnableValidation(false)
        } else {
            b.pinpad.setEnableValidation(true)
            b.pinpad.value = pin
        }
    }

    override fun setPinHint(resId: Int) {
        b.pinpad.setPinHint(resId)
    }

    override fun setOnPinValueListener(listener: PinValueListener) {
        b.pinpad.setOnValueListener(listener)
    }

    override fun startConfirmation(requestCode: Int, pin: String?) {
        Builder(this, PinMode.Confirmation)
                .setPin(pin)
                .start(requestCode)
    }

    override fun finishSuccess(intent: Intent?) {
        if (intent != null) {
            startActivityClearTop(this, intent)
            finish()
            return
        }
        setResult(Activity.RESULT_OK)
        finish()
    }

    override fun setOnPinValidationError(listener: PinValidateErrorListener) {
        b.pinpad.setOnValidationErrorListener(listener)
    }

    override fun setPinError(error: CharSequence?) {
        b.pinpad.setError(error)
    }

    override fun setPinError(errorRes: Int) {
        b.pinpad.setError(errorRes)
    }

    override fun setPinEnabled(enabled: Boolean) {
        b.pinpad.isEnabled = enabled
    }

    override fun resetPin() {
        b.pinpad.reset()
    }

    override fun onErrorWithRetry(errorMessage: String, errorResolver: View.OnClickListener) {}
    override fun onErrorWithRetry(errorMessage: String, actionName: String, errorResolver: View.OnClickListener) {}
    override fun onError(t: Throwable) {}
    override fun onError(err: String) {}

    override fun startBiometricPrompt(callback: BiometricPrompt.AuthenticationCallback) {
        val info = PromptInfo.Builder()
                .setTitle(getString(R.string.pin_fp_title_enable))
                .setDescription("")
                .setSubtitle("")
                .setNegativeButtonText(getString(R.string.btn_cancel))
                .build()

        val executor: Executor = Executors.newSingleThreadExecutor()
        val prompt = BiometricPrompt(this, executor, callback)
        prompt.authenticate(info)
    }

    override fun finishCancel() {
        finish()
    }

    override fun startLogin() {
        Toast.makeText(this, R.string.pin_invalid_logout, Toast.LENGTH_LONG).show()

        val intent = Intent(this, AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun setOnFingerprintClickListener(listener: PinFingerprintClickListener) {
        b.pinpad.setOnFingerprintClickListener(listener)
    }

    override fun setFingerprintEnabled(enabled: Boolean) {
        b.pinpad.setEnableFingerprint(enabled)
    }

    override fun startDialog(executor: DialogExecutor) {
        mCurrentDialog = switchDialogWithExecutor(this, mCurrentDialog, executor)
    }

    @ProvidePresenter
    fun providePresenter(): PinEnterPresenter {
        return presenterProvider.get()
    }

    override fun onStop() {
        super.onStop()
        releaseDialog(mCurrentDialog)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityPinBinding.inflate(layoutInflater)
        setContentView(b.root)
        ButterKnife.bind(this)
        setupToolbar(b.toolbar)
        presenter.handleExtras(intent)
        setResult(Activity.RESULT_CANCELED)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        presenter.onActivityResult(requestCode, resultCode, data)
    }

    class Builder : ActivityBuilder {
        private val mMode: PinMode
        private var mPin: String? = ""
        private var mStartHome = false
        private var mSuccessIntent: Intent? = null

        constructor(from: Activity, mode: PinMode) : super(from) {
            mMode = mode
        }

        constructor(from: Fragment, mode: PinMode) : super(from) {
            mMode = mode
        }

        constructor(from: Service, mode: PinMode) : super(from) {
            mMode = mode
        }

        fun setPin(pin: String?): Builder {
            mPin = pin
            return this
        }

        fun setSuccessIntent(intent: Intent?): Builder {
            mSuccessIntent = intent
            return this
        }

        fun startHomeOnSuccess(): Builder {
            mStartHome = true
            return this
        }

        override fun onBeforeStart(intent: Intent) {
            super.onBeforeStart(intent)
            intent.putExtra(SecurityModule.EXTRA_MODE, mMode.ordinal)
            intent.putExtra(SecurityModule.EXTRA_PIN, mPin)
            if (mStartHome) {
                mSuccessIntent = Intent(activity, HomeActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_CLEAR_TASK or
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
            }
            intent.putExtra(SecurityModule.EXTRA_SUCCESS_INTENT, mSuccessIntent)
        }

        override fun getActivityClass(): Class<*> {
            return PinEnterActivity::class.java
        }
    }
}