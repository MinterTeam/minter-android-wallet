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
import android.widget.Switch
import android.widget.TextView
import androidx.core.view.ViewCompat
import butterknife.BindView
import butterknife.ButterKnife
import network.minter.bipwallet.R
import network.minter.bipwallet.internal.common.DeferredCall
import network.minter.bipwallet.internal.common.Lazy
import network.minter.bipwallet.internal.views.list.multirow.MultiRowAdapter.RowViewHolder
import network.minter.bipwallet.internal.views.list.multirow.MultiRowContract

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */

typealias SettingsSwitchClickListener = (view: View, checked: Boolean) -> Unit

class SettingsSwitchRow : MultiRowContract.Row<SettingsSwitchRow.ViewHolder> {
    private var mKey: CharSequence?
    private var mValue: Lazy<Boolean>
    private var mListener: SettingsSwitchClickListener? = null
    private var mEnabled: Lazy<Boolean> = Lazy { true }

    private val mDefer = DeferredCall.createWithSize<ViewHolder>(1)

    constructor(key: CharSequence, value: Boolean, listener: SettingsSwitchClickListener? = null) {
        mKey = key
        mValue = Lazy { value }
        mListener = listener
    }

    constructor(key: CharSequence?, value: Lazy<Boolean>, listener: SettingsSwitchClickListener? = null) {
        mKey = key
        mValue = value
        mListener = listener
    }

    fun setValue(value: Lazy<Boolean>, listener: SettingsSwitchClickListener? = null): SettingsSwitchRow {
        mValue = value
        mListener = listener
        mDefer.call { vh: ViewHolder -> this.fill(vh) }
        return this
    }

    fun setValue(value: Lazy<Boolean>): SettingsSwitchRow {
        mValue = value
        mDefer.call { vh: ViewHolder -> this.fill(vh) }
        return this
    }

    override fun getItemView(): Int {
        return R.layout.row_item_switch_settings
    }

    override fun getRowPosition(): Int {
        return 0
    }

    override fun isVisible(): Boolean {
        return true
    }

    override fun onBindViewHolder(viewHolder: ViewHolder) {
        fill(viewHolder)
        mDefer.attach(viewHolder)
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        mDefer.detach()
    }

    override fun getViewHolderClass(): Class<ViewHolder> {
        return ViewHolder::class.java
    }

    fun setEnabled(enabled: Lazy<Boolean>) {
        mEnabled = enabled
    }

    fun setEnabled(enabled: Boolean) {
        mEnabled = Lazy { enabled }
    }

    private fun fill(vh: ViewHolder) {
        if (mKey != null && mKey!!.isNotEmpty()) {
            ViewCompat.setTransitionName(vh.key!!, "settings_field")
        }

        vh.key!!.text = mKey
        vh.value!!.isEnabled = mEnabled.get()
        vh.value!!.isChecked = mValue.get()
        vh.value!!.setOnCheckedChangeListener { buttonView: CompoundButton, isChecked: Boolean ->
            mListener?.invoke(buttonView, isChecked)
        }
    }

//    interface OnClickListener {
//        fun onClick(view: View?, value: Boolean?)
//    }

    class ViewHolder(itemView: View) : RowViewHolder(itemView) {
        @JvmField @BindView(R.id.item_key)
        var key: TextView? = null

        @JvmField @BindView(R.id.item_value)
        var value: Switch? = null

        init {
            ButterKnife.bind(this, itemView)
        }
    }
}