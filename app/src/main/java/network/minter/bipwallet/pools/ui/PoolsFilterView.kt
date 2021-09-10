/*
 * Copyright (C) by MinterTeam. 2021
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

package network.minter.bipwallet.pools.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.lifecycle.MutableLiveData
import com.airbnb.paris.annotations.Attr
import com.airbnb.paris.annotations.Styleable
import network.minter.bipwallet.R
import network.minter.bipwallet.pools.models.PoolsFilter


/**
 * minter-android-wallet. 2021
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 * @todo make single class for this and TransactionFilter
 */
@Styleable("PoolsFilterView")
class PoolsFilterView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {
    private var onSelectLiveData: MutableLiveData<PoolsFilter>? = null
    private val buttons: MutableList<View> = ArrayList()
    private var selectListener: ((PoolsFilter) -> Unit)? = null
    private val internalClickListener = OnClickListener { btn ->
        buttons.forEach { v ->
            v.isSelected = false
        }
        btn.isSelected = true
        selected = valueFromButton(btn)
        selectListener?.invoke(selected)
        onSelectLiveData?.postValue(selected)
    }

    var selected: PoolsFilter = PoolsFilter.None

    private fun valueFromButton(button: View): PoolsFilter {
        return PoolsFilter.values()[(button.tag as String).toInt()]
    }

    private fun setState(pos: Int, selected: Boolean) {
        buttons[pos].isSelected = selected
    }

    fun setOnSelectListener(listener: (PoolsFilter) -> Unit) {
        selectListener = listener
    }

    fun setSelectObserver(onSelect: MutableLiveData<PoolsFilter>) {
        onSelectLiveData = onSelect
    }

    init {
        inflate(context, R.layout.view_pools_filter, this) as ViewGroup
        val view: ViewGroup = findViewById(R.id.filter_group)
        for (i in 0 until view.childCount) {
            val button = view.getChildAt(i)
            button.setOnClickListener(internalClickListener)
            if (button.tag == null) {
                throw IllegalStateException("Filter view must have tag, indexed by PoolsFilter")
            }
            buttons.add(button)
        }

        network.minter.bipwallet.Paris.style(this).apply(attrs)
    }

    @Attr(R.styleable.PoolsFilterView_pf_def_value)
    internal fun setDefaultValue(value: String) {
        val pos = value.toInt()
        selected = PoolsFilter.values()[pos]
        setState(pos, true)
        selectListener?.invoke(selected)
        onSelectLiveData?.postValue(selected)
    }
}