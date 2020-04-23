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
package network.minter.bipwallet.internal.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.FrameLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import network.minter.bipwallet.internal.BaseMvpBottomSheetDialogFragment

typealias ActionListener = () -> Unit

abstract class BaseBottomSheetDialogFragment : BaseMvpBottomSheetDialogFragment() {
    var onSubmitListener: ActionListener? = null
    var onDismissListener: ActionListener? = null
    private var sheetWasExpanded: Boolean = false

    protected val behavior: BottomSheetBehavior<FrameLayout>
        get() = (dialog as BottomSheetDialog).behavior

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (onDismissListener != null) {
            onDismissListener!!.invoke()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        expand(true)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val d = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        d.requestWindowFeature(Window.FEATURE_NO_TITLE)


        if (d.window != null) {
            val params = d.window!!.attributes
            params.width = ViewGroup.LayoutParams.MATCH_PARENT
            params.height = ViewGroup.LayoutParams.MATCH_PARENT
            d.window!!.attributes = params
            d.window!!.setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
            d.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            d.window!!.statusBarColor = Color.TRANSPARENT
        }
        return d
    }

    fun collapse() {
        behavior.setPeekHeight(0, true)

        if (behavior.state == BottomSheetBehavior.STATE_EXPANDED) {
            sheetWasExpanded = true
            behavior.state = BottomSheetBehavior.STATE_COLLAPSED
        } else {
            sheetWasExpanded = false
        }
    }

    fun expand(force: Boolean = false) {
        if (sheetWasExpanded || force) {
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
        behavior.setPeekHeight(BottomSheetBehavior.PEEK_HEIGHT_AUTO, true)
    }

    protected fun <T : Dialog> T.fixNestedDialogBackgrounds(): T {
        window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window?.setWindowAnimations(0)

        collapse()

        if (this is WalletDialog) {
            addDismissListener {
                expand()
            }
        } else {
            setOnDismissListener {
                expand()
            }
        }

        return this
    }
}