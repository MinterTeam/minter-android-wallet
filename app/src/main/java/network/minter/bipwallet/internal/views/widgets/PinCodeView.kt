/*
 * Copyright (C) by MinterTeam. 2022
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

package network.minter.bipwallet.internal.views.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.StringRes
import network.minter.bipwallet.databinding.ViewPincodeKbdBinding
import java.util.*

/**
 * minter-android-wallet. 2019
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */

typealias PinFingerprintClickListener = (View) -> Unit
typealias PinInputListener = (String) -> Unit
typealias PinValidateErrorListener = (String) -> Unit
typealias PinValueListener = (value: String, len: Int, valid: Boolean) -> Unit


class PinCodeView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val b: ViewPincodeKbdBinding
    private val pinKeys: MutableList<View> = ArrayList(12)

    /**
     * Sateful drawables:
     * checked = error
     * active = success
     * empty state = default
     */
    private val indicators: MutableList<View> = ArrayList(4)
    private var onInputListener: PinInputListener? = null
    private var onValueListener: PinValueListener? = null
    private var onValidationErrorListener: PinValidateErrorListener? = null
    private var onFingerprintClickListener: PinFingerprintClickListener? = null
    private val valueInternal = Stack<String>()
    private val validValue: MutableList<String> = ArrayList(4)
    private var enableValidation = false
    private var clearReset = false
    private var enableFingerprint = false

    init {
        val inflater = LayoutInflater.from(context)
        b = ViewPincodeKbdBinding.inflate(inflater, this, true)
        network.minter.bipwallet.Paris.style(b.root).apply(attrs)


        for (i in 0 until b.pinKeyContainer.childCount) {
            pinKeys.add(b.pinKeyContainer.getChildAt(i))
        }

        for (i in 0 until b.pinIndicatorContainer.childCount) {
            indicators.add(b.pinIndicatorContainer.getChildAt(i))
        }

        for (key in pinKeys) {
            when {
                key.tag == null -> {
                }
                isDigitKey(key.tag as String) -> {
                    key.setOnClickListener {
                        if (clearReset) {
                            valueInternal.clear()
                            indicators.forEach {
                                it.background.state = IntArray(0)
                            }
                            clearReset = false
                        }
                        val value = getKey(key.tag as String)
                        addValue(value)
                        updateIndicator()
                        onInputListener?.invoke(value ?: "")
                    }
                }
                isBackspaceKey(key.tag as String) -> {
                    key.setOnClickListener {
                        popValue()
                        updateIndicator()
                    }
                }
                isFingerprintKey(key.tag as String) -> {
                    key.setOnClickListener {
                        if (enableFingerprint) {
                            onFingerprintClickListener?.invoke(it)
                        }
                    }
                }
            }
        }
    }

    fun setOnFingerprintClickListener(listener: PinFingerprintClickListener?) {
        onFingerprintClickListener = listener
    }

    fun setEnableFingerprint(enable: Boolean) {
        enableFingerprint = enable
        T.isVisible = v = enableFingerprint
    }

    fun reset() {
        isEnabled = true
        valueInternal.clear()
        validValue.clear()

        indicators.forEach { it.background.state = IntArray(0) }
        setPinHint(null)
        setError(null)
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        pinKeys.forEach {
            it.isEnabled = enabled
            it.alpha = if (enabled) 1f else .5f
        }
    }

    fun setOnValidationErrorListener(listener: PinValidateErrorListener) {
        onValidationErrorListener = listener
    }

    fun setError(error: CharSequence?) {
        if (error == null) {
            b.pinError.text = null
            T.isVisible = v = false
        } else {
            b.pinError.text = error
            T.isVisible = v = true
        }
    }

    fun setError(@StringRes resId: Int) {
        setError(context.resources.getString(resId))
    }

    fun setPinHint(@StringRes resId: Int) {
        b.pinHint.setText(resId)
    }

    fun setPinHint(hint: CharSequence?) {
        b.pinHint.text = hint
    }

    fun setOnValueListener(listener: PinValueListener?) {
        onValueListener = listener
    }

    fun setOnInputListener(listener: PinInputListener?) {
        onInputListener = listener
    }

    var value: String
        get() {
            if (valueInternal.isEmpty()) {
                return ""
            }
            val sb = StringBuilder()
            for (v in valueInternal) {
                sb.append(v)
            }
            return sb.toString()
        }
        set(value) {
            enableValidation = true
            validValue.clear()
            require(value.length == 4) { "Value length must contains exactly 4 digits" }
            for (v in value.toCharArray()) {
                validValue.add(String(charArrayOf(v)))
            }
        }

    fun setEnableValidation(enable: Boolean) {
        enableValidation = enable
    }

    private fun popValue() {
        if (valueInternal.isEmpty()) return
        valueInternal.pop()
    }

    private fun addValue(value: String?) {
        if (valueInternal.size >= 4) return
        valueInternal.push(value)
    }

    private fun updateIndicator() {
        if (valueInternal.isEmpty()) {
            indicators.forEach { it.background.state = IntArray(0) }
            onValueListener?.invoke("", valueInternal.size, false)
            return
        }
        var validCount = 0
        for (i in 0..3) {
            val d = indicators[i].background
            if (i > valueInternal.size - 1) {
                d.state = IntArray(0)
                onValueListener?.invoke("", valueInternal.size, false)
                break
            }
            val inputValue = valueInternal[i]
            var validValue: String?
            validValue = if (!enableValidation) {
                inputValue
            } else {
                check(this.validValue.size == 4) { "You must set 4 digits value" }
                this.validValue[i]
            }
            val valid = inputValue == validValue
            if (valid) {
                validCount++
            }
            // success
            d.state = intArrayOf(android.R.attr.state_active)
        }

        if (valueInternal.size == 4 && enableValidation && validCount != validValue.size) {
            for (i in 0..3) {
                val d = indicators[i].background
                // error
                d.state = intArrayOf(android.R.attr.state_checked)
            }
            onValidationErrorListener?.invoke(value)
            clearReset = true
        }
        onValueListener?.invoke(value, valueInternal.size, validCount == validValue.size)
    }

    private fun isValidKey(tag: String?): Boolean {
        return tag != null && tag.isNotEmpty() && tag.length > 4
    }

    private fun isDigitKey(tag: String): Boolean {
        return if (!isValidKey(tag)) {
            false
        } else tag.substring(4).matches("^[0-9]$".toRegex())
    }

    private fun isBackspaceKey(tag: String): Boolean {
        return if (!isValidKey(tag)) false else tag.substring(4) == "bsp"
    }

    private fun isFingerprintKey(tag: String): Boolean {
        return if (!isValidKey(tag)) false else tag.substring(4) == "fp"
    }

    private fun getKey(tag: String): String? {
        return if (!isValidKey(tag)) null else tag.substring(4)
    }


}