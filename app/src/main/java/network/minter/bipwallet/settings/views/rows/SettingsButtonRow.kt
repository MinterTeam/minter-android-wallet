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
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import network.minter.bipwallet.R
import network.minter.bipwallet.databinding.ItemListSettingsBinding
import network.minter.bipwallet.internal.common.DeferredCall
import network.minter.bipwallet.internal.common.Lazy
import network.minter.bipwallet.internal.views.list.multirow.BindingRow

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
typealias SettingsButtonClickListener = (view: View, sharedView: View, value: String) -> Unit

class SettingsButtonRow : BindingRow<ItemListSettingsBinding> {
    private var key: CharSequence? = null
    private var keyRes: Int = 0
    private var value: Lazy<String>
    private var defValue: String? = null
    private var isEnabledLazy = Lazy { true }
    private var clickListener: SettingsButtonClickListener?
    private var isInactive = false
    private val deferCall = DeferredCall.createWithSize<ItemListSettingsBinding>(1)

    constructor(@StringRes key: Int, value: Lazy<String>, defValue: String, listener: SettingsButtonClickListener?) :
            this(key, value, listener) {
        this.defValue = defValue
    }

    constructor(@StringRes key: Int, value: String, listener: SettingsButtonClickListener?) {
        this.keyRes = key
        this.value = Lazy { value }
        clickListener = listener
    }

    constructor(@StringRes key: Int, value: Lazy<String>, listener: SettingsButtonClickListener?) {
        this.keyRes = key
        this.value = value
        clickListener = listener
    }

    constructor(key: CharSequence?, value: Lazy<String>, defValue: String, listener: SettingsButtonClickListener?) :
            this(key, value, listener) {
        this.defValue = defValue
    }

    constructor(key: CharSequence?, value: String, listener: SettingsButtonClickListener?) {
        this.key = key
        this.value = Lazy { value }
        clickListener = listener
    }

    constructor(key: CharSequence?, value: Lazy<String>, listener: SettingsButtonClickListener?) {
        this.key = key
        this.value = value
        clickListener = listener
    }

    fun setInactive(inactive: Boolean): SettingsButtonRow {
        isInactive = inactive
        return this
    }

    fun setValue(value: Lazy<String>, listener: SettingsButtonClickListener?): SettingsButtonRow {
        this.value = value
        clickListener = listener
        deferCall.call { b: ItemListSettingsBinding -> this.fill(b) }
        return this
    }

    fun setValue(value: Lazy<String>): SettingsButtonRow {
        this.value = value
        deferCall.call { b: ItemListSettingsBinding -> this.fill(b) }
        return this
    }

    fun setEnabled(enabled: Lazy<Boolean>): SettingsButtonRow {
        isEnabledLazy = enabled
        setInactive(!enabled.get())
        return this
    }

    fun setEnabled(enabled: Boolean): SettingsButtonRow {
        isEnabledLazy = Lazy { enabled }
        setInactive(!enabled)
        return this
    }

    override fun onBind(binding: ItemListSettingsBinding) {
        fill(binding)
        deferCall.attach(binding)
    }

    override fun onUnbind(binding: ItemListSettingsBinding) {
        deferCall.detach()
    }

    override fun getBindingClass(): Class<ItemListSettingsBinding> {
        return ItemListSettingsBinding::class.java
    }

    private fun fill(b: ItemListSettingsBinding) {
        if (keyRes > 0) {
            key = b.root.context.getString(keyRes)
        }
        b.root.isEnabled = isEnabledLazy.get()
        b.itemKey.text = key
        b.itemValue.text = value.get()
        if (defValue == null) {
            if (isInactive) {

                b.itemValue.setTextColor(ContextCompat.getColor(b.root.context, R.color.textColorGrey))
            } else {
                b.itemValue.setTextColor(ContextCompat.getColor(b.root.context, R.color.textColorPrimary))
            }
        } else {
            if (value.get() != null && value.get()!!.isEmpty()) {
                b.itemValue.setTextColor(ContextCompat.getColor(b.root.context, R.color.textColorGrey))
                b.itemValue.text = defValue
            } else {
                b.itemValue.setTextColor(ContextCompat.getColor(b.root.context, R.color.textColorPrimary))
                b.itemValue.text = value.get()
            }
        }
        b.root.setOnClickListener {
            clickListener?.invoke(b.root, b.itemKey, value.get())
        }
    }
}