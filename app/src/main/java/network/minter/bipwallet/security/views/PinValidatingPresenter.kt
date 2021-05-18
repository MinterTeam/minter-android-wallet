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
package network.minter.bipwallet.security.views

import androidx.biometric.BiometricPrompt
import moxy.InjectViewState
import network.minter.bipwallet.R
import network.minter.bipwallet.internal.Wallet
import network.minter.bipwallet.internal.mvp.MvpBasePresenter
import network.minter.bipwallet.internal.settings.EnableFingerprint
import network.minter.bipwallet.internal.settings.SettingsManager
import network.minter.bipwallet.internal.storage.KVStorage
import network.minter.bipwallet.internal.storage.SecretStorage
import network.minter.bipwallet.security.SecurityModule
import network.minter.bipwallet.security.contract.PinValidatingView
import javax.inject.Inject

@InjectViewState
class PinValidatingPresenter @Inject constructor() : MvpBasePresenter<PinValidatingView>() {
    companion object {
        private const val PREF_PIN_INVALID_TYPE_COUNT = "pin_type_invalid_type_count"
        private const val PREF_PIN_INVALID_TYPE_TIME = "pin_type_invalid_type_time"
    }

    @Inject lateinit var storage: SecretStorage
    @Inject lateinit var settings: SettingsManager
    @Inject lateinit var kvStorage: KVStorage

    private var mSourcePin: String? = ""
    private var mPin = ""
    private fun init() {
        if (storage.hasPinCode()) {
            mSourcePin = storage.pinCode
        }
        viewState.setOnPinValueListener { value: String, len: Int, valid: Boolean ->
            onPinEntered(value, len, valid)
        }
    }

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()
        init()
    }

    private fun startFingerprintValidation() {
        viewState.startBiometricPrompt(object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                viewState.finishSuccess()
            }
        })
    }

    private fun onClickFingerprintButton() {
        startFingerprintValidation()
    }

    private fun logout() {
        Wallet.app().session().logout()
        Wallet.app().secretStorage().destroy()
        Wallet.app().storage().deleteAll()
        Wallet.app().storageCache().deleteAll()
        Wallet.app().prefs().edit().clear().apply()
        viewState.startLogin()
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onValidationError(value: String) {
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
            logout()
            return
        }
        val leftTries = SecurityModule.MAX_TRIES_UNTIL_LOCK - invalidCount
        viewState.setPinError(Wallet.app().res().getQuantityString(R.plurals.pin_code_left_tries, leftTries, leftTries))
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onPinEntered(value: String, len: Int, valid: Boolean) {
        mPin = if (len == 4) {
            value
        } else {
            ""
        }
        viewState.setupTitle(R.string.title_pin_enter)
        viewState.setPinHint(R.string.hint_pin_enter)
        if (settings[EnableFingerprint] && Wallet.app().fingerprint().hasEnrolledFingerprints()) {
            viewState.setFingerprintEnabled(true)
            viewState.setOnFingerprintClickListener {
                onClickFingerprintButton()
            }
            startFingerprintValidation()
        }
        viewState.setEnableValidation(mSourcePin!!)
        viewState.setOnPinValidationError {
            onValidationError(it)
        }
    }
}