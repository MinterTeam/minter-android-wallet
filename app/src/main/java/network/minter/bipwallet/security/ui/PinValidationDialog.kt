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

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.PromptInfo
import androidx.fragment.app.FragmentManager
import dagger.android.support.AndroidSupportInjection
import moxy.MvpAppCompatDialogFragment
import moxy.presenter.InjectPresenter
import network.minter.bipwallet.R
import network.minter.bipwallet.auth.ui.AuthActivity
import network.minter.bipwallet.databinding.ActivityPinBinding
import network.minter.bipwallet.internal.Wallet
import network.minter.bipwallet.internal.views.widgets.PinFingerprintClickListener
import network.minter.bipwallet.internal.views.widgets.PinValidateErrorListener
import network.minter.bipwallet.internal.views.widgets.PinValueListener
import network.minter.bipwallet.security.contract.PinValidatingView
import network.minter.bipwallet.security.views.PinValidatingPresenter
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Provider

class PinValidationDialog : MvpAppCompatDialogFragment(), PinValidatingView {

    companion object {
        private var sInstance: PinValidationDialog? = null

        fun showIfRequired(fragmentManager: FragmentManager?) {
            if (!Wallet.app().secretStorage().hasPinCode()) {
                return
            }
            if (sInstance != null && sInstance!!.isShowing) {
                return
            }
            sInstance = PinValidationDialog()
            sInstance!!.isCancelable = false
            sInstance!!.mOnDialogDismissListener = {
                releaseDialog(sInstance)
            }
            sInstance!!.show(fragmentManager!!, "pin-validation")
        }

        fun releaseDialog(dialog: PinValidationDialog?) {
            if (dialog != null && dialog.isShowing) {
                dialog.dismiss()
            }
        }
    }

    @Inject lateinit var presenterProvider: Provider<PinValidatingPresenter>
    @InjectPresenter lateinit var presenter: PinValidatingPresenter

    private lateinit var b: ActivityPinBinding
    private var mOnDialogDismissListener: (() -> Unit)? = null

    var isShowing = false
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidSupportInjection.inject(this)
        super.onCreate(savedInstanceState)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        mOnDialogDismissListener?.invoke()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext(), R.style.Wallet_PinDialog)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        if (dialog.window != null) {
            val params = dialog.window!!.attributes
            params.width = ViewGroup.LayoutParams.MATCH_PARENT
            params.height = ViewGroup.LayoutParams.MATCH_PARENT
            dialog.window!!.attributes = params
            dialog.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
        return dialog
    }

    override fun onResume() {
        super.onResume()
        isShowing = true
    }

    override fun onPause() {
        super.onPause()
        isShowing = false
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        b = ActivityPinBinding.inflate(inflater, container, false)

        return b.root
    }

    override fun setOnPinValueListener(listener: PinValueListener) {
        b.pinpad.setOnValueListener(listener)
    }

    override fun setupTitle(title: Int) {
        b.title.setText(title)
    }

    override fun setPinHint(resId: Int) {
        b.pinpad.setPinHint(resId)
    }

    override fun setFingerprintEnabled(enabled: Boolean) {
        b.pinpad.setEnableFingerprint(enabled)
    }

    override fun setOnFingerprintClickListener(listener: PinFingerprintClickListener) {
        b.pinpad.setOnFingerprintClickListener(listener)
    }

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

    override fun setEnableValidation(pin: String) {
        b.pinpad.value = pin
    }

    override fun setOnPinValidationError(listener: PinValidateErrorListener) {
        b.pinpad.setOnValidationErrorListener(listener)
    }

    override fun finishSuccess() {
        dismiss()
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

    override fun startLogin() {
        Toast.makeText(activity, R.string.pin_invalid_logout, Toast.LENGTH_LONG).show()
        val intent = Intent(activity, AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        dismiss()
    }
}