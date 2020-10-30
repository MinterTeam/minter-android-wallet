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
package network.minter.bipwallet.settings.views.rows

import android.view.View
import android.widget.CompoundButton
import androidx.annotation.StringRes
import androidx.core.view.ViewCompat
import network.minter.bipwallet.databinding.RowItemSwitchSettingsBinding
import network.minter.bipwallet.internal.common.DeferredCall
import network.minter.bipwallet.internal.views.list.multirow.BindingRow

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */

typealias SettingsSwitchClickListener = (view: View, checked: Boolean) -> Unit

class SettingsSwitchRow : BindingRow<RowItemSwitchSettingsBinding> {
    private var key: CharSequence? = null
    private var keyRes: Int = 0
    private var value: () -> Boolean
    private var clickListener: SettingsSwitchClickListener? = null
    private var isEnabled: () -> Boolean = { true }

    private val mDefer = DeferredCall.createWithSize<RowItemSwitchSettingsBinding>(1)

    constructor(@StringRes key: Int, value: Boolean, listener: SettingsSwitchClickListener? = null) {
        this.keyRes = key
        this.value = { value }
        clickListener = listener
    }

    constructor(@StringRes key: Int, value: () -> Boolean, listener: SettingsSwitchClickListener? = null) {
        this.keyRes = key
        this.value = value
        clickListener = listener
    }

    constructor(key: CharSequence, value: Boolean, listener: SettingsSwitchClickListener? = null) {
        this.key = key
        this.value = { value }
        clickListener = listener
    }

    constructor(key: CharSequence?, value: () -> Boolean, listener: SettingsSwitchClickListener? = null) {
        this.key = key
        this.value = value
        clickListener = listener
    }

    fun setValue(value: () -> Boolean, listener: SettingsSwitchClickListener? = null): SettingsSwitchRow {
        this.value = value
        clickListener = listener
        mDefer.call { b: RowItemSwitchSettingsBinding -> this.fill(b) }
        return this
    }

    fun setValue(value: () -> Boolean): SettingsSwitchRow {
        this.value = value
        mDefer.call { b: RowItemSwitchSettingsBinding -> this.fill(b) }
        return this
    }

    override fun onBind(binding: RowItemSwitchSettingsBinding) {
        fill(binding)
        mDefer.attach(binding)
    }

    override fun onUnbind(binding: RowItemSwitchSettingsBinding) {
        super.onUnbind(binding)
        mDefer.detach()
    }

    override fun getBindingClass(): Class<RowItemSwitchSettingsBinding> {
        return RowItemSwitchSettingsBinding::class.java
    }

    fun setEnabled(enabled: () -> Boolean) {
        isEnabled = enabled
    }

    fun setEnabled(enabled: Boolean) {
        isEnabled = { enabled }
    }

    private fun fill(b: RowItemSwitchSettingsBinding) {
        if (keyRes != 0) {
            key = b.root.context.getString(keyRes)
        }

        if (key != null && key!!.isNotEmpty()) {
            ViewCompat.setTransitionName(b.itemKey, "settings_field")
        }

        b.itemKey.text = key
        b.itemValue.isEnabled = isEnabled()
        b.itemValue.isChecked = value()
        b.root.setOnClickListener {
            b.itemValue.isChecked = !b.itemValue.isChecked
        }
        b.itemValue.setOnCheckedChangeListener { buttonView: CompoundButton, isChecked: Boolean ->
            clickListener?.invoke(buttonView, isChecked)
        }
    }
}