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
package network.minter.bipwallet.security.contract

import android.content.Intent
import androidx.annotation.StringRes
import androidx.biometric.BiometricPrompt
import moxy.MvpView
import moxy.viewstate.strategy.AddToEndSingleStrategy
import moxy.viewstate.strategy.OneExecutionStateStrategy
import moxy.viewstate.strategy.StateStrategyType
import network.minter.bipwallet.internal.dialogs.DialogExecutor
import network.minter.bipwallet.internal.mvp.ErrorViewWithRetry
import network.minter.bipwallet.internal.views.widgets.PinFingerprintClickListener
import network.minter.bipwallet.internal.views.widgets.PinValidateErrorListener
import network.minter.bipwallet.internal.views.widgets.PinValueListener
import network.minter.bipwallet.security.SecurityModule.KeypadListener

/**
 * minter-android-wallet. 2019
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
@StateStrategyType(AddToEndSingleStrategy::class)
interface PinEnterView : MvpView, ErrorViewWithRetry {
    fun setKeypadListener(listener: KeypadListener)
    fun setupTitle(@StringRes title: Int)
    fun setEnableValidation(pin: String?)
    fun setPinHint(@StringRes resId: Int)
    fun setOnPinValueListener(listener: PinValueListener)

    @StateStrategyType(OneExecutionStateStrategy::class)
    fun startConfirmation(requestCode: Int, pin: String?)
    fun startDialog(executor: DialogExecutor)
    fun finishSuccess(intent: Intent?)
    fun setOnPinValidationError(listener: PinValidateErrorListener)
    fun setPinError(error: CharSequence?)
    fun setPinError(@StringRes errorRes: Int)
    fun setPinEnabled(enabled: Boolean)
    fun resetPin()
    fun startBiometricPrompt(callback: BiometricPrompt.AuthenticationCallback)
    fun finishCancel()
    fun startLogin()
    fun setOnFingerprintClickListener(listener: PinFingerprintClickListener)
    fun setFingerprintEnabled(enabled: Boolean)
}