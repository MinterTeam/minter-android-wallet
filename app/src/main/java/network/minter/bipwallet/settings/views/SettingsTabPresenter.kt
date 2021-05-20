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
package network.minter.bipwallet.settings.views

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.view.View
import android.widget.Switch
import androidx.biometric.BiometricPrompt
import moxy.InjectViewState
import network.minter.bipwallet.BuildConfig
import network.minter.bipwallet.R
import network.minter.bipwallet.analytics.AppEvent
import network.minter.bipwallet.home.HomeScope
import network.minter.bipwallet.internal.Wallet
import network.minter.bipwallet.internal.auth.AuthSession
import network.minter.bipwallet.internal.dialogs.ConfirmDialog
import network.minter.bipwallet.internal.helpers.FingerprintHelper
import network.minter.bipwallet.internal.mvp.MvpBasePresenter
import network.minter.bipwallet.internal.settings.*
import network.minter.bipwallet.internal.storage.SecretStorage
import network.minter.bipwallet.internal.views.list.multirow.BindingMultiRowAdapter
import network.minter.bipwallet.security.SecurityModule
import network.minter.bipwallet.settings.contract.SettingsTabView
import network.minter.bipwallet.settings.views.rows.SettingsButtonRow
import network.minter.bipwallet.settings.views.rows.SettingsSwitchRow
import network.minter.bipwallet.settings.views.rows.TitleRow
import timber.log.Timber
import java.util.*
import javax.inject.Inject

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock></edward.vstock>@gmail.com>
 */
@HomeScope
@InjectViewState
class SettingsTabPresenter @Inject constructor() : MvpBasePresenter<SettingsTabView>() {
    @Inject lateinit var session: AuthSession
    @Inject lateinit var secretStorage: SecretStorage
    @Inject lateinit var prefs: SharedPreferences
    @Inject lateinit var settings: SettingsManager
    @Inject lateinit var fingerHelper: FingerprintHelper

    private val securityAdapter: BindingMultiRowAdapter = BindingMultiRowAdapter()
    private val mainAdapter: BindingMultiRowAdapter = BindingMultiRowAdapter()
    private val additionalAdapter: BindingMultiRowAdapter = BindingMultiRowAdapter()

    private val isNotificationsEnabled: Boolean
        get() = settings[EnableLiveNotifications]

    private val isSoundsEnabled: Boolean
        get() = settings[EnableSounds]

    private val isPinCodeEnabled: Boolean
        get() = settings[EnablePinCode]

    private val isFingerprintEnabled: Boolean
        get() = settings[EnableFingerprint] && settings[EnablePinCode] && fingerHelper.hasEnrolledFingerprints()

    private val isStoriesEnabled: Boolean
        get() = settings[EnableStories] && BuildConfig.ENABLE_STORIES

    override fun attachView(view: SettingsTabView) {
        super.attachView(view)
        viewState.setSecurityAdapter(securityAdapter)
        viewState.setMainAdapter(mainAdapter)
        viewState.setAdditionalAdapter(additionalAdapter)
        viewState.setOnOurChannelClickListener { onOurChannelClickListener() }
        viewState.setOnSupportChatClickListener { onSupportChatClickListener() }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) {
            return
        }
        if (requestCode == REQUEST_CREATE_PIN_CODE) {
            Timber.d("PIN successfully settled")
            securityAdapter.clear()
            mainAdapter.clear()
            addRows()
        } else if (requestCode == REQUEST_REMOVE_PIN_CODE) {
            Timber.d("PIN successfully removed")
            settings[EnableFingerprint] = false
            securityAdapter.clear()
            mainAdapter.clear()
            addRows()
        }
    }

    fun onLogout() {
        viewState.startDialog {
            ConfirmDialog.Builder(it, R.string.dialog_title_logout)
                    .setDescription(R.string.dialog_description_logout)
                    .setText(R.string.dialog_text_logout)
                    .setPositiveAction(R.string.btn_logout) { d, _ ->
                        analytics.send(AppEvent.SettingsLogoutButton)
                        Wallet.app().session().logout()
                        Wallet.app().secretStorage().destroy()
                        Wallet.app().storage().deleteAll()
                        Wallet.app().storageCache().deleteAll()
                        Wallet.app().prefs().edit().clear().apply()

                        d.dismiss()
                        viewState.startLogin()

                    }
                    .setNegativeAction(R.string.btn_cancel)
                    .create()
        }

    }

    private fun addRows() {
        // security row
        val enablePinRow = SettingsSwitchRow(
                R.string.settings_sec_pin_enabled_label,
                { isPinCodeEnabled },
                { _, enabled -> onEnablePinCode(enabled) }
        )

        val fingerprintRow = SettingsSwitchRow(
                R.string.settings_sec_fingerprint_enabled_label,
                { isFingerprintEnabled },
                { view, enabled -> onEnableFingerprint(view, enabled) }
        )

        fingerprintRow.setEnabled { isPinCodeEnabled }

        val changePinRow = SettingsButtonRow(
                R.string.settings_sec_pin_change_label,
                ""
        ) { view, sharedView, value ->
            onChangePinClick(view, sharedView, value)
        }

        securityAdapter.add(TitleRow(R.string.settings_title_security))
        securityAdapter.add(enablePinRow)

        if (secretStorage.hasPinCode()) {
            if (fingerHelper.isHardwareDetected) {
                securityAdapter.add(fingerprintRow)
            }
            securityAdapter.add(changePinRow)
        }

        mainAdapter.add(TitleRow(R.string.settings_title_notifications))

        mainAdapter.add(SettingsSwitchRow(
                R.string.settings_sounds_enabled_label,
                { isSoundsEnabled },
                { _, isChecked -> onSwitchSounds(isChecked) }
        ))
        mainAdapter.add(SettingsSwitchRow(
                R.string.settings_notifications_enabled_label,
                { isNotificationsEnabled },
                { _: View, enabled: Boolean -> onSwitchNotifications(enabled) }
        ))

        if (BuildConfig.ENABLE_STORIES) {
            mainAdapter.add(TitleRow(R.string.settings_title_stories))
            mainAdapter.add(SettingsSwitchRow(
                    R.string.settings_stories_enabled_label,
                    { isStoriesEnabled },
                    { _, enabled -> onSwitchStories(enabled) }
            ))
        }
    }

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()

        // unset if user disabled fingerprint by OS
        if (isFingerprintEnabled) {
            if (!fingerHelper.hasEnrolledFingerprints() || !fingerHelper.isHardwareDetected) {
                settings[EnableFingerprint] = false
            }
        }

        addRows()

    }

    @Suppress("UNUSED_PARAMETER")
    private fun onChangeDefWallet(value: String) {
    }

    private fun onSupportChatClickListener() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/MinterHelp"))
        viewState.startIntent(intent)
    }

    private fun onOurChannelClickListener() {
        if (Locale.getDefault().isO3Language.lowercase(Locale.getDefault()) == "rus") {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/MinterNetwork"))
            viewState.startIntent(intent)
        } else {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/MinterTeam"))
            viewState.startIntent(intent)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onChangePinClick(view: View, view1: View, s: String) {
        viewState.startPinCodeManager(REQUEST_CREATE_PIN_CODE, SecurityModule.PinMode.Change)
    }

    private fun onEnableFingerprint(view: View, enabled: Boolean) {
        if (enabled) {
            if (!fingerHelper.hasEnrolledFingerprints()) {
                (view as Switch).isChecked = false

                viewState.startDialog { ctx: Context ->
                    ConfirmDialog.Builder(ctx, R.string.fingerprint_dialog_enroll_title)
                            .setText(R.string.fingerprint_dialog_enroll_text)
                            .setPositiveAction(R.string.btn_settings) { d, _: Int ->
                                d.dismiss()
                                viewState.startFingerprintEnrollment()
                            }
                            .setNegativeAction(R.string.btn_cancel)
                            .create()
                }

                return
            }
            viewState.startBiometricPrompt(object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    view.post { (view as Switch).isChecked = false }
                    if (errorCode == BiometricPrompt.ERROR_CANCELED || errorCode == BiometricPrompt.ERROR_USER_CANCELED || errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        return
                    }
                    Timber.e("Unable to auth FP: [%d] %s", errorCode, errString)
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    settings[EnableFingerprint] = true
                    Timber.e("Success to auth FP")
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Timber.e("FP auth failed")
                }
            })
            return
        }
        settings[EnableFingerprint] = false
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onEnablePinCode(enabled: Boolean) {
        if (!secretStorage.hasPinCode()) {
            if (enabled) {
                viewState.startPinCodeManager(REQUEST_CREATE_PIN_CODE, SecurityModule.PinMode.Creation)
            }

        } else {
            if (!enabled) {
                viewState.startPinCodeManager(REQUEST_REMOVE_PIN_CODE, SecurityModule.PinMode.Deletion)
            }
        }
    }

    private fun onSwitchNotifications(enabled: Boolean) {
        settings[EnableLiveNotifications] = enabled
        Wallet.app().sounds().play(R.raw.click_pop_zap)
    }

    private fun onSwitchStories(enabled: Boolean) {
        settings[EnableStories] = enabled
        Wallet.app().sounds().play(R.raw.click_pop_zap)
        viewState.notifyStoriesState(enabled)
    }

    private fun onSwitchSounds(isChecked: Boolean) {
        settings[EnableSounds] = isChecked
        if (isChecked) {
            Wallet.app().sounds().playForce(R.raw.click_pop_zap)
        }
    }

    companion object {
        private const val REQUEST_CREATE_PIN_CODE = 1002
        private const val REQUEST_REMOVE_PIN_CODE = 1004
        private const val REQUEST_VERIFY_PIN_CODE = 1003
    }
}