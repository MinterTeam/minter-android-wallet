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
package network.minter.bipwallet.wallets.ui

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Build
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.appbar.AppBarLayout
import network.minter.bipwallet.R
import network.minter.bipwallet.databinding.FragmentTabWalletsBinding
import network.minter.bipwallet.internal.helpers.MathHelper.blendColors
import network.minter.bipwallet.internal.helpers.MathHelper.clamp
import network.minter.bipwallet.internal.helpers.ViewHelper
import java.lang.ref.WeakReference

class WalletsTopRecolorHelper internal constructor(fragment: WalletsTabFragment) : AppBarOffsetChangedListener(), LifecycleEventObserver {
    private val collapsedStatusColor = -0x1
    private val collapsedTextColor = -0x1000000
    private val collapsedSubtitleColor: Int
    private val expandedSubtitleColor: Int
    private val collapsedDropdownColor: Int
    private var elevation: Float = 0.0f
    private val expandedToolbarIconsColor = -0x1
    private val collapsedToolbarIconsColor: Int
    private val ref: WeakReference<WalletsTabFragment> = WeakReference(fragment)
    private var statusColor = -1
    private var enableRecolor = true
    private var expandedStatusColor: Int
    private var lightStatus = false
    private var toolbarIconsColor = collapsedStatusColor

    init {
        ViewHelper.setSystemBarsLightness(fragment, false)
        expandedStatusColor = ContextCompat.getColor(ref.get()!!.requireActivity(), R.color.colorPrimary)
        collapsedToolbarIconsColor = ContextCompat.getColor(ref.get()!!.requireActivity(), R.color.colorPrimaryLight)
        collapsedSubtitleColor = ContextCompat.getColor(ref.get()!!.requireActivity(), R.color.textColorGrey)
        expandedSubtitleColor = ContextCompat.getColor(ref.get()!!.requireActivity(), R.color.white70)
        statusColor = expandedStatusColor
        collapsedDropdownColor = ContextCompat.getColor(ref.get()!!.requireActivity(), R.color.grey)
        elevation = fragment.resources.getDimension(R.dimen.toolbar_elevation)
        fragment.binding.appbar.setLiftable(true)
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when(event) {
            Lifecycle.Event.ON_DESTROY -> ref.clear()
            else -> Unit
        }
    }

    private val view: FragmentTabWalletsBinding by lazy {
        ref.get()!!.binding
    }

    private fun recolorToolbarMenu() {
        for (i in 0 until view.toolbar.menu.size()) {
            view.toolbar.menu.getItem(i).icon.colorFilter = PorterDuffColorFilter(toolbarIconsColor, PorterDuff.Mode.SRC_IN)
        }
    }

    companion object {
        fun enableRecolorSystemUI(): Boolean {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
        }
    }

    override fun onStateChanged(appbar: AppBarLayout, state: State, verticalOffset: Int, percent: Float) {
        val ctx = ref.get()
        if (!enableRecolor || ctx == null || ctx.activity == null) {
            return
        }
        // blend colors between collapsed and expanded state
        val textColor = blendColors(collapsedStatusColor, collapsedTextColor, 1.0f - percent)
        val subtitleColor = blendColors(collapsedSubtitleColor, expandedSubtitleColor, percent)
        val dropdownColor = blendColors(collapsedStatusColor, collapsedDropdownColor, 1.0f - percent)
        statusColor = blendColors(collapsedStatusColor, expandedStatusColor, percent)
        lightStatus = percent < 0.5f
        if (enableRecolorSystemUI()) {
            ViewHelper.setSystemBarsLightness(ctx.activity, lightStatus)
            ViewHelper.setStatusBarColor(ctx.activity, statusColor)
        }

        view.walletSelector.setNameColor(textColor)
        view.walletSelector.setSubtitleColor(subtitleColor)
        view.walletSelector.setDropdownTint(dropdownColor)
        if (lightStatus) {
            if (toolbarIconsColor != expandedStatusColor) {
                toolbarIconsColor = collapsedToolbarIconsColor
                recolorToolbarMenu()
            }
        } else {
            if (toolbarIconsColor != collapsedStatusColor) {
                toolbarIconsColor = expandedToolbarIconsColor
                recolorToolbarMenu()
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
        appbar.elevation = elevation * (1 - percent)
    }

    fun setEnableRecolor(enable: Boolean) {
        enableRecolor = enable
    }

    fun setTabSelected() {
        if (ref.get() == null) return
        if (enableRecolorSystemUI()) {
            ViewHelper.setSystemBarsLightness(ref.get(), lightStatus)
            ViewHelper.setStatusBarColorAnimate(ref.get(), statusColor)
        }

    }


}