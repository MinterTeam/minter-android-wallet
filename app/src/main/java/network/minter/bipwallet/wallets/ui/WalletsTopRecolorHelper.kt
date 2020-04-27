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
package network.minter.bipwallet.wallets.ui

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.view.View
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.AppBarLayout
import network.minter.bipwallet.R
import network.minter.bipwallet.databinding.FragmentTabWalletsBinding
import network.minter.bipwallet.internal.helpers.MathHelper.blendColors
import network.minter.bipwallet.internal.helpers.MathHelper.clamp
import network.minter.bipwallet.internal.helpers.ViewHelper
import java.lang.ref.WeakReference

class WalletsTopRecolorHelper internal constructor(fragment: WalletsTabFragment) : AppBarOffsetChangedListener() {
    private val mCollapsedStatusColor = -0x1
    private val mCollapsedTextColor = -0x1000000
    private val mCollapsedDropdownColor: Int
    private val mElevation: Float
    private val mExpandedToolbarIconsColor = -0x1
    private val mCollapsedToolbarIconsColor: Int
    private val mRef: WeakReference<WalletsTabFragment> = WeakReference(fragment)
    private var mStatusColor = -1
    private var mEnableRecolor = true
    private var mExpandedStatusColor: Int
    private var mLightStatus = false
    private var mToolbarIconsColor = mCollapsedStatusColor
    private var waitForExpandPercent: Float = -1f

    init {
        ViewHelper.setSystemBarsLightness(fragment, false)
        mExpandedStatusColor = ContextCompat.getColor(mRef.get()!!.activity!!, R.color.colorPrimary)
        mCollapsedToolbarIconsColor = ContextCompat.getColor(mRef.get()!!.activity!!, R.color.colorPrimaryLight)
        mStatusColor = mExpandedStatusColor
        mCollapsedDropdownColor = ContextCompat.getColor(mRef.get()!!.activity!!, R.color.grey)
        mElevation = fragment.resources.getDimension(R.dimen.card_elevation)
        fragment.binding.appbar.setLiftable(true)
    }

    private val view: FragmentTabWalletsBinding by lazy {
        mRef.get()!!.binding
    }


    override fun onStateChanged(appbar: AppBarLayout, state: State, verticalOffset: Int, percent: Float) {
        if (!mEnableRecolor || mRef.get() == null) {
            return
        }
        val textColor = blendColors(mCollapsedStatusColor, mCollapsedTextColor, 1.0f - percent)
        val dropdownColor = blendColors(mCollapsedStatusColor, mCollapsedDropdownColor, 1.0f - percent)
        mStatusColor = blendColors(mCollapsedStatusColor, mExpandedStatusColor, percent)
        mLightStatus = percent < 0.5f
        ViewHelper.setSystemBarsLightness(mRef.get()!!.activity, mLightStatus)
        ViewHelper.setStatusBarColor(mRef.get()!!.activity, mStatusColor)
        view.walletSelector.setNameColor(textColor)
        view.walletSelector.setDropdownTint(dropdownColor)
        if (mLightStatus) {
            if (mToolbarIconsColor != mExpandedStatusColor) {
                mToolbarIconsColor = mCollapsedToolbarIconsColor
                for (i in 0 until view.toolbar.menu.size()) {
                    view.toolbar.menu.getItem(i).icon.colorFilter = PorterDuffColorFilter(mToolbarIconsColor, PorterDuff.Mode.SRC_IN)
                }
            }
        } else {
            if (mToolbarIconsColor != mCollapsedStatusColor) {
                mToolbarIconsColor = mExpandedToolbarIconsColor
                for (i in 0 until view.toolbar.menu.size()) {
                    view.toolbar.menu.getItem(i).icon.colorFilter = PorterDuffColorFilter(mToolbarIconsColor, PorterDuff.Mode.SRC_IN)
                }
            }
        }
        if (percent < 1.0f && percent > 0.0f) {
            view.overlay.visibility = View.VISIBLE
            val overlayAlpha: Float = if (percent <= 0.5f) {
                clamp((1.0f - percent) * 1.1f, 0f, 1f)
            } else {
                1.0f - percent
            }
            view.overlay.alpha = overlayAlpha
        } else if (percent == 0f) {
            view.overlay.visibility = View.VISIBLE
            view.overlay.alpha = 1.0f
        } else {
            view.overlay.visibility = View.GONE
        }
        appbar.elevation = mElevation * (1 - percent)


        /*
        // does not work correctly for now
        Timber.d("Percent: %f", percent)
        if (waitForExpandPercent == -1f) {
            if (percent <= 0.8f) {
                waitForExpandPercent = 0f
                appbar.setExpanded(false)
                appbar.isSaveEnabled
            } else if (percent >= 0.2f) {
                waitForExpandPercent = 1f
                appbar.setExpanded(true)
            }
        } else {
            if (percent == waitForExpandPercent) {
                waitForExpandPercent = -1f
            }
        }
        */

    }

    fun setEnableRecolor(enable: Boolean) {
        mEnableRecolor = enable
    }

    fun setTabSelected() {
        if (mRef.get() == null) return
        ViewHelper.setSystemBarsLightness(mRef.get(), mLightStatus)
        ViewHelper.setStatusBarColorAnimate(mRef.get(), mStatusColor)
    }


}