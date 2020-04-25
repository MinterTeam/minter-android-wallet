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
package network.minter.bipwallet.internal.views.widgets

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.Toolbar
import com.airbnb.paris.annotations.Attr
import com.airbnb.paris.annotations.Styleable
import network.minter.bipwallet.R
import network.minter.bipwallet.databinding.ViewToolbarWithProgressBinding
import network.minter.bipwallet.internal.helpers.ViewExtensions.visible

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock></edward.vstock>@gmail.com>
 */
@Styleable("ToolbarProgress")
class ToolbarProgress @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : Toolbar(context, attrs, defStyleAttr) {

    private var mEnableAction = true
    private val b: ViewToolbarWithProgressBinding

    init {
        val inflater = LayoutInflater.from(context)
        b = ViewToolbarWithProgressBinding.inflate(inflater, this, true)
        network.minter.bipwallet.Paris.style(this).apply(attrs)
    }

    @Attr(R.styleable.ToolbarProgress_navigationIcon)
    override fun setNavigationIcon(@DrawableRes navigationIcon: Int) {
        super.setNavigationIcon(navigationIcon)
    }

    @Attr(R.styleable.ToolbarProgress_navigationIcon)
    override fun setNavigationIcon(navigationIcon: Drawable?) {
        super.setNavigationIcon(navigationIcon)
    }

    @Attr(R.styleable.ToolbarProgress_enableAction)
    fun setEnableAction(enableAction: Boolean) {
        mEnableAction = enableAction
        b.tpAction.visible = mEnableAction
    }

    @Attr(R.styleable.ToolbarProgress_progressColor)
    fun setProgressColor(color: Int) {
        b.tpProgress.indeterminateDrawable.colorFilter = PorterDuffColorFilter(
                color,
                PorterDuff.Mode.SRC_IN
        )
    }

    @Attr(R.styleable.ToolbarProgress_title)
    fun setTitleText(@StringRes resId: Int) {
        b.tpTitle.setText(resId)
    }

    @Attr(R.styleable.ToolbarProgress_title)
    fun setTitleText(titleText: CharSequence?) {
        b.tpTitle.text = titleText
    }

    @Attr(R.styleable.ToolbarProgress_titleTextColor)
    override fun setTitleTextColor(color: Int) {
        super.setTitleTextColor(color)
    }

    fun showProgress() {
        post {
            if (mEnableAction) {
                b.tpAction.visible = false
                b.tpAction.isEnabled = false
            }
            b.tpProgress.visible = true
        }
    }

    fun hideProgress() {
        post {
            b.tpProgress.visible = false
            if (mEnableAction) {
                b.tpAction.visible = true
                b.tpAction.isEnabled = true
            }
        }
    }

    fun setOnActionClickListener(listener: OnClickListener?) {
        b.tpAction.setOnClickListener(listener)
    }

//    override fun setTitle(resId: Int) {
//        b.tpTitle.text = context.getString(resId)
//    }
//
//    override fun setTitle(title: CharSequence) {
//        b.tpTitle.text = title
//    }

//    override fun setTitleTextAppearance(context: Context, resId: Int) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && title != null) {
//            b.tpTitle.setTextAppearance(resId)
//        } else {
//            TextViewCompat.setTextAppearance(b.tpTitle, resId)
//        }
//    }

}