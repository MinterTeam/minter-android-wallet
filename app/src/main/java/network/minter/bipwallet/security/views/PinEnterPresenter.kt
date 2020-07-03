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
package network.minter.bipwallet.security.views

import android.app.Activity
import android.content.Intent
import androidx.biometric.BiometricPrompt
import moxy.InjectViewState
import network.minter.bipwallet.R
import network.minter.bipwallet.internal.Wallet
import network.minter.bipwallet.internal.mvp.MvpBasePresenter
import network.minter.bipwallet.internal.settings.EnableFingerprint
import network.minter.bipwallet.internal.settings.EnablePinCode
import network.minter.bipwallet.internal.settings.SettingsManager
import network.minter.bipwallet.internal.storage.KVStorage
import network.minter.bipwallet.internal.storage.SecretStorage
import network.minter.bipwallet.security.SecurityModule
import network.minter.bipwallet.security.SecurityModule.PinMode
import network.minter.bipwallet.security.contract.PinEnterView
import timber.log.Timber
import javax.inject.Inject

/**
 * minter-android-wallet. 2019
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
@InjectViewState
class PinEnterPresenter @Inject constructor() : MvpBasePresenter<PinEnterView>() {
    @Inject lateinit var storage: SecretStorage
    @Inject lateinit var settings: SettingsManager
    @Inject lateinit var kvStorage: KVStorage

    private var mode: PinMode? = null
    private var mSuccessIntent: Intent? = null
    private var mPin = ""
    private var mSourcePin: String? = ""

    private fun init() {
        if (storage.hasPinCode() && mode != PinMode.Creation && mode != PinMode.Confirmation) {
            mSourcePin = storage.pinCode
        }

        viewState.setOnPinValueListener { value, len, valid ->
            onPinEntered(value, len, valid)
        }

        when (mode) {
            PinMode.Change -> {
                viewState.setupTitle(R.string.title_pin_enter)
                viewState.setPinHint(R.string.hint_pin_enter)
                viewState.setEnableValidation(mSourcePin)
                viewState.setOnPinValidationError {
                    onValidationError(it)
                }
            }
            PinMode.Creation -> {
                viewState.setEnableValidation(null)
                viewState.setupTitle(R.string.title_pin_set)
                viewState.setPinHint(R.string.hint_pin_enter)
            }
            PinMode.Confirmation -> {
                viewState.setPinHint(R.string.hint_pin_repeat)
                viewState.setEnableValidation(mSourcePin)
            }
            PinMode.EnableFingerprint, PinMode.DisableFingerprint, PinMode.Validation -> {
                viewState.setupTitle(R.string.title_pin_enter)
                viewState.setPinHint(R.string.hint_pin_enter)

                if (settings[EnableFingerprint] && Wallet.app().fingerprint().hasEnrolledFingerprints()) {
                    viewState.setFingerprintEnabled(true)
                    viewState.setOnFingerprintClickListener {
                        onClickFingerprintButton()
                    }
                    startFingerprintValidation()
                }
                viewState.setEnableValidation(mSourcePin)
                viewState.setOnPinValidationError {
                    onValidationError(it)
                }
            }
            PinMode.Deletion -> {
                viewState.setupTitle(R.string.title_pin_enter)
                viewState.setPinHint(R.string.hint_pin_enter)
                viewState.setEnableValidation(mSourcePin)
                viewState.setOnPinValidationError {
                    onValidationError(it)
                }
            }
        }
    }

    override fun handleExtras(intent: Intent?) {
        super.handleExtras(intent)
        mode = PinMode.fromInt(intent!!.getIntExtra(SecurityModule.EXTRA_MODE, PinMode.Creation.ordinal))
        mSourcePin = intent.getStringExtra(SecurityModule.EXTRA_PIN)
        mSuccessIntent = intent.getParcelableExtra(SecurityModule.EXTRA_SUCCESS_INTENT)
        init()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_PIN_CONFIRM) {
            if (mode == PinMode.Creation && resultCode == Activity.RESULT_OK) {
                viewState.finishSuccess(mSuccessIntent)
            }
        }
    }

    private fun startFingerprintValidation() {
        viewState.startBiometricPrompt(object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                viewState.finishSuccess(mSuccessIntent)
            }
        })
    }

    private fun onClickFingerprintButton() {
        startFingerprintValidation()
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onValidationError(value: String) {
        if (mode == PinMode.Confirmation) {
            return
        }
        val timestamp = System.currentTimeMillis() / 1000
        var firstInvalidTime = kvStorage[PREF_PIN_INVALID_TYPE_TIME, 0L]
        var invalidCount = kvStorage[PREF_PIN_INVALID_TYPE_COUNT, 0]
        invalidCount++

        // first invalid
        if (firstInvalidTime == 0L) {
            firstInvalidTime = timestamp
        }
        if (firstInvalidTime + SecurityModule.LOCK_INTERVAL_S < timestamp) {
            firstInvalidTime = timestamp
            invalidCount = 0
        }
        kvStorage.put(PREF_PIN_INVALID_TYPE_TIME, firstInvalidTime)
        kvStorage.put(PREF_PIN_INVALID_TYPE_COUNT, invalidCount)
        if (invalidCount >= SecurityModule.MAX_TRIES_UNTIL_LOCK) {
//            setPinErrorLocked();
//            startLockTimeUpdate();
            logout()
            return
        }
        viewState.setPinError(Wallet.app().res().getString(R.string.error_pin_invalid, SecurityModule.MAX_TRIES_UNTIL_LOCK - invalidCount))
    }

    private fun logout() {
        Wallet.app().session().logout()
        Wallet.app().secretStorage().destroy()
        Wallet.app().storage().deleteAll()
        Wallet.app().storageCache().deleteAll()
        Wallet.app().prefs().edit().clear().apply()
        viewState.startLogin()
    }

    private fun onPinEntered(value: String?, len: Int, valid: Boolean) {
        mPin = if (len == 4) {
            value!!
        } else {
            ""
        }
        if (mode == PinMode.Creation) {
            if (len == 4) {
                viewState.resetPin()
                Timber.d("PIN entered")
                mode = PinMode.Confirmation
                mSourcePin = value
                init()
            }
        } else if (mode == PinMode.Confirmation) {
            if (valid && len == 4) {
                viewState.setPinEnabled(false)
                storage.pinCode = mPin
                settings.setSync(EnablePinCode, true)
                Timber.d("PIN confirmed")
                viewState.finishSuccess(mSuccessIntent)
            }
            if (len == 4 && !valid) {
                viewState.setPinError(R.string.error_pin_invalid_simple)
            }
        } else if (mode == PinMode.Validation) {
            if (valid && len == 4) {
                viewState.setPinEnabled(false)
                Timber.d("PIN validated")
                kvStorage.put(PREF_PIN_INVALID_TYPE_TIME, 0L)
                kvStorage.put(PREF_PIN_INVALID_TYPE_COUNT, 0)
                viewState.finishSuccess(mSuccessIntent)
            }
        } else if (mode == PinMode.Deletion) {
            if (valid && len == 4) {
                viewState.setPinEnabled(false)
                storage.removePinCode()
                settings.remoteSync(EnablePinCode)
                Timber.d("PIN removed")
                viewState.finishSuccess(mSuccessIntent)
            }
        } else if (mode == PinMode.EnableFingerprint) {
            if (valid && len == 4) {
                viewState.setPinEnabled(false)
            }
        } else if (mode == PinMode.DisableFingerprint) {
            if (valid) {
                viewState.setPinEnabled(false)
                settings.setSync(EnableFingerprint, false)
                viewState.finishSuccess(mSuccessIntent)
            }
        } else if (mode == PinMode.Change) {
            if (valid && len == 4) {
                viewState.setPinEnabled(true)
                Timber.d("PIN validated")
                kvStorage.put(PREF_PIN_INVALID_TYPE_TIME, 0L)
                kvStorage.put(PREF_PIN_INVALID_TYPE_COUNT, 0)
                mode = PinMode.Creation
                viewState.resetPin()
                init()
            }
        }
    }

    companion object {
        private const val REQUEST_PIN_CONFIRM = 1010
        private const val PREF_PIN_INVALID_TYPE_COUNT = "pin_type_invalid_type_count"
        private const val PREF_PIN_INVALID_TYPE_TIME = "pin_type_invalid_type_time"
    }
}