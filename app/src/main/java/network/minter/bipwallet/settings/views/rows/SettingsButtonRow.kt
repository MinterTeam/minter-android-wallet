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
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
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
typealias SettingsButtonClickListener = (view: View, sharedView: View, value: String) -> Unit

class SettingsButtonRow : MultiRowContract.Row<SettingsButtonRow.ViewHolder> {
    private var mKey: CharSequence?
    private var mValue: Lazy<String>
    private var mEnabled = Lazy { true }
    private var mDefValue: String? = null
    private var mListener: SettingsButtonClickListener?
    private var mInactive = false
    private val mDefer = DeferredCall.createWithSize<ViewHolder>(1)

    constructor(key: CharSequence?, value: Lazy<String>, defValue: String, listener: SettingsButtonClickListener?) :
            this(key, value, listener) {
        mDefValue = defValue
    }

    constructor(key: CharSequence?, value: String, listener: SettingsButtonClickListener?) {
        mKey = key
        mValue = Lazy { value }
        mListener = listener
    }

    constructor(key: CharSequence?, value: Lazy<String>, listener: SettingsButtonClickListener?) {
        mKey = key
        mValue = value
        mListener = listener
    }

    fun setInactive(inactive: Boolean): SettingsButtonRow {
        mInactive = inactive
        return this
    }

    fun setValue(value: Lazy<String>, listener: SettingsButtonClickListener?): SettingsButtonRow {
        mValue = value
        mListener = listener
        mDefer.call { vh: ViewHolder -> this.fill(vh) }
        return this
    }

    fun setValue(value: Lazy<String>): SettingsButtonRow {
        mValue = value
        mDefer.call { vh: ViewHolder -> this.fill(vh) }
        return this
    }

    fun setEnabled(enabled: Lazy<Boolean>): SettingsButtonRow {
        mEnabled = enabled
        setInactive(!enabled.get())
        return this
    }

    fun setEnabled(enabled: Boolean): SettingsButtonRow {
        mEnabled = Lazy { enabled }
        setInactive(!enabled)
        return this
    }

    override fun getItemView(): Int {
        return R.layout.item_list_settings
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

    private fun fill(vh: ViewHolder) {
        if (mKey != null && mKey!!.length > 0) {
            ViewCompat.setTransitionName(vh.key!!, "settings_field")
        }
        vh.itemView.isEnabled = mEnabled.get()
        vh.key!!.text = mKey
        vh.value!!.text = mValue.get()
        if (mDefValue == null) {
            if (mInactive) {

                vh.value!!.setTextColor(ContextCompat.getColor(vh.itemView.context, R.color.textColorGrey))
            } else {
                vh.value!!.setTextColor(ContextCompat.getColor(vh.itemView.context, R.color.textColorPrimary))
            }
        } else {
            if (mValue.get() != null && mValue.get()!!.isEmpty()) {
                vh.value!!.setTextColor(ContextCompat.getColor(vh.itemView.context, R.color.textColorGrey))
                vh.value!!.text = mDefValue
            } else {
                vh.value!!.setTextColor(ContextCompat.getColor(vh.itemView.context, R.color.textColorPrimary))
                vh.value!!.text = mValue.get()
            }
        }
        vh.itemView.setOnClickListener {
            mListener?.invoke(vh.itemView, vh.key!!, mValue.get())
        }
    }
//
//    interface OnClickListener {
//        fun onClick(view: View, sharedView: View, value: String)
//    }

    class ViewHolder(itemView: View) : RowViewHolder(itemView) {
        @JvmField @BindView(R.id.item_key)
        var key: TextView? = null

        @JvmField @BindView(R.id.item_value)
        var value: TextView? = null

        @JvmField @BindView(R.id.item_icon)
        var icon: ImageView? = null

        init {
            ButterKnife.bind(this, itemView)
        }
    }
}