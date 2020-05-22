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

import android.graphics.Rect
import android.view.ViewTreeObserver
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import network.minter.bipwallet.databinding.FragmentTabWalletsBinding

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
class CollapsingToolbarScrollDisabler(
        binding: FragmentTabWalletsBinding
) {

    companion object {
        private val ENABLE_COLLAPSING: Boolean
            get() {
                if (OVERLAPPED_RECYCLERS_MAP.isEmpty()) {
                    return false
                }
                return OVERLAPPED_RECYCLERS_MAP.map { entry -> if (entry.value) 1 else 0 }.reduce { a, b -> a + b } != 0
            }
        private var OVERLAPPED_RECYCLERS_MAP = HashMap<String, Boolean>()

        fun bindRecycler(list: RecyclerView, clazz: Class<*>) {
            OVERLAPPED_RECYCLERS_MAP.remove(clazz.simpleName)
            list.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (list.childCount > 0) {
                        val listItemsHeight = list.children.map { it.height }.reduce { a, b -> a + b }
                        val rect = Rect()
                        list.getLocalVisibleRect(rect)

                        OVERLAPPED_RECYCLERS_MAP[clazz.simpleName] = listItemsHeight >= rect.height()
                        list.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    } else {
                        OVERLAPPED_RECYCLERS_MAP[clazz.simpleName] = false
                    }
                }

            })
        }

        fun setOverlaps(clazz: Class<*>, overlaps: Boolean) {
            OVERLAPPED_RECYCLERS_MAP[clazz.simpleName] = overlaps
        }
    }

    private var appBarIsExpanded: Boolean = false
    private val appBarScrollFlagsDef: Int = (binding.collapsing.layoutParams as AppBarLayout.LayoutParams).scrollFlags

    init {
        binding.appbar.addOnOffsetChangedListener(object : AppBarOffsetChangedListener() {
            override fun onStateChanged(appBarLayout: AppBarLayout?, state: State?, verticalOffset: Int, expandedPercent: Float) {
                appBarIsExpanded = expandedPercent == 1.0f
            }
        })
        binding.collapsing.viewTreeObserver.addOnGlobalLayoutListener {
            val lp = (binding.collapsing.layoutParams as AppBarLayout.LayoutParams)
            val flags = if (ENABLE_COLLAPSING) {
                appBarScrollFlagsDef
            } else {
                0
            }

            if (lp.scrollFlags == flags) {
                return@addOnGlobalLayoutListener
            }

            if (appBarIsExpanded && flags == 0) {
                binding.appbar.postOnAnimation {
                    lp.scrollFlags = flags
                    binding.collapsing.layoutParams = lp
                }
            } else if (flags != 0) {
                lp.scrollFlags = flags
                binding.collapsing.layoutParams = lp
            }
        }
    }
}