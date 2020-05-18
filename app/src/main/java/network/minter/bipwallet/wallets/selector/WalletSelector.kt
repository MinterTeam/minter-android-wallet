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
package network.minter.bipwallet.wallets.selector

import android.content.Context
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.lifecycle.LifecycleOwner
import butterknife.BindView
import butterknife.ButterKnife
import com.airbnb.paris.annotations.Attr
import com.airbnb.paris.annotations.Styleable
import network.minter.bipwallet.Paris
import network.minter.bipwallet.R
import network.minter.bipwallet.internal.common.DeferredCall
import network.minter.bipwallet.internal.common.Preconditions
import network.minter.bipwallet.internal.helpers.MathHelper.bdLT
import network.minter.bipwallet.internal.system.BroadcastReceiverManager
import network.minter.bipwallet.wallets.selector.WalletListAdapter.*
import java.math.BigDecimal
import java.util.*

@Styleable("WalletSelector")
class WalletSelector : FrameLayout {
    @JvmField @BindView(R.id.icon)
    var weight: TextView? = null

    @JvmField @BindView(R.id.title)
    var name: TextView? = null

    @JvmField @BindView(R.id.dropdown)
    var dropdown: ImageView? = null
    private var mAdapter: WalletListAdapter? = null
    private var mPopup: WalletsPopupWindow? = null
    private var mOnClickWallet: OnClickWalletListener? = null
    private var mOnClickAddWallet: OnClickAddWalletListener? = null
    private var mOnClickEditWallet: OnClickEditWalletListener? = null
    private val mPopupDefer = DeferredCall.create<WalletsPopupWindow?>()

    enum class WalletWeight(val emoji: String) {
        Shrimp("\uD83E\uDD90"),
        Shell("\uD83D\uDC1A"),
        Crab("\uD83E\uDD80"),
        TropicalFish("\uD83D\uDC20"),
        Shark("\uD83E\uDD88"),
        Whale("\uD83D\uDC0B"),
        Dolphin("\uD83D\uDC2C");

        companion object {
            fun detect(balance: BigDecimal?): WalletWeight {
                return when {
                    bdLT(balance, BigDecimal("1000")) -> {
                        Shrimp
                    }
                    bdLT(balance, BigDecimal("10000")) -> {
                        Shell
                    }
                    bdLT(balance, BigDecimal("100000")) -> {
                        Crab
                    }
                    bdLT(balance, BigDecimal("1000000")) -> {
                        TropicalFish
                    }
                    bdLT(balance, BigDecimal("10000000")) -> {
                        Shark
                    }
                    bdLT(balance, BigDecimal("100000000")) -> {
                        Whale
                    }
                    else -> {
                        Dolphin
                    }
                }
            }
        }

    }

    constructor(context: Context?) : super(context!!)
    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs) {
        init(attrs, 0, 0)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context!!, attrs, defStyleAttr) {
        init(attrs, defStyleAttr, 0)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context!!, attrs, defStyleAttr, defStyleRes) {
        init(attrs, defStyleAttr, defStyleRes)
    }

    private fun init(attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) {
        mAdapter = WalletListAdapter()
        mAdapter!!.setOnClickWalletListener { addressBalance: WalletItem -> onClickWallet(addressBalance) }
        mAdapter!!.setOnClickEditWalletListener { addressBalance: WalletItem -> onClickEditWallet(addressBalance) }
        mAdapter!!.setOnClickAddWalletListener { onClickAddWallet() }
        val view = View.inflate(context, R.layout.view_wallet_selector, this)
        ButterKnife.bind(this, view)
        view.setOnClickListener { openPopup(it) }
        Paris.style(this).apply(attrs)
    }

    fun <T> registerLifecycle(ctx: T) where T : Context, T : LifecycleOwner {
        val bm = BroadcastReceiverManager(ctx)
        bm.add(WalletSelectorBroadcastReceiver(object : WalletSelectorBroadcastReceiver.Listener {
            override fun onFillWallets(wallets: ArrayList<WalletItem>) {
                this@WalletSelector.setWallets(wallets)
            }

            override fun onSetMainWallet(wallet: WalletItem) {
                this@WalletSelector.setMainWallet(wallet)
            }

        }))
        bm.register()
    }

    fun setOnClickWalletListener(listener: OnClickWalletListener?) {
        mOnClickWallet = listener
    }

    fun setOnClickAddWalletListener(listener: OnClickAddWalletListener?) {
        mOnClickAddWallet = listener
    }

    fun setOnClickEditWalletListener(listener: OnClickEditWalletListener?) {
        mOnClickEditWallet = listener
    }

    @Attr(value = R.styleable.WalletSelector_ws_name_color)
    fun setNameColor(@ColorInt color: Int) {
        name!!.setTextColor(color)
    }

    @Attr(value = R.styleable.WalletSelector_ws_dropdown_tint)
    fun setDropdownTint(@ColorInt color: Int) {
        dropdown!!.setColorFilter(color, PorterDuff.Mode.MULTIPLY)
    }

    fun showPopupProgress(show: Boolean) {
        mPopupDefer.call { t: WalletsPopupWindow? ->
            if (show) {
                t!!.progressBar.visibility = View.VISIBLE
                t.list.isClickable = false
                t.list.alpha = 0.3f
            } else {
                t!!.progressBar.visibility = View.GONE
                t.list.alpha = 1.0f
                t.list.isClickable = true
            }
        }
    }

    fun openPopup(view: View?) {
        mPopup = WalletsPopupWindow.create(this, mAdapter)
        mPopupDefer.attach(mPopup)
    }

    internal fun setMainWallet(wallet: WalletItem) {
        name!!.post { name!!.text = Preconditions.firstNonNull(wallet.title, wallet.addressShort) }
        weight!!.post { weight!!.text = wallet.weight.emoji }
    }

    internal fun setWallets(addresses: List<WalletItem?>?) {
        post {
            mAdapter!!.setWallets(addresses)
            mAdapter!!.notifyDataSetChanged()
        }
    }

    private fun dismissPopup() {
        if (mPopup != null && mPopup!!.isShowing) {
            mPopupDefer.detach()
            mPopup!!.dismiss()
            mPopup = null
        }
    }

    private fun onClickAddWallet() {
        dismissPopup()
        if (mOnClickAddWallet != null) {
            mOnClickAddWallet!!.onClickAddWallet()
        }
    }

    private fun onClickEditWallet(addressBalance: WalletItem) {
        dismissPopup()
        if (mOnClickEditWallet != null) {
            mOnClickEditWallet!!.onClickEdit(addressBalance)
        }
    }

    private fun onClickWallet(addressBalance: WalletItem) {
        dismissPopup()
        setMainWallet(addressBalance)
        if (mOnClickWallet != null) {
            mOnClickWallet!!.onClickWallet(addressBalance)
        }
    }
}