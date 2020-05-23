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

import android.animation.Animator
import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.widget.FrameLayout
import androidx.core.view.postOnAnimationDelayed
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import network.minter.bipwallet.internal.BaseMvpBottomSheetDialogFragment
import network.minter.bipwallet.internal.helpers.KeyboardHelper
import timber.log.Timber

typealias ActionListener = () -> Unit

abstract class BaseBottomSheetDialogFragment : BaseMvpBottomSheetDialogFragment() {
    var onSubmitListener: ActionListener? = null
    var onDismissListener: ActionListener? = null
    private var sheetWasExpanded: Boolean = false
    private var rootView: View? = null
    private var maxHeight: Float = 0f
    protected var walletDialog: WalletDialog? = null
    protected var walletDialogFragment: WalletDialogFragment? = null

    protected val behavior: BottomSheetBehavior<FrameLayout>?
        get() = (dialog as BottomSheetDialog?)?.behavior

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (onDismissListener != null) {
            onDismissListener!!.invoke()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rootView = view
        expand(true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        rootView = null
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val d = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        d.requestWindowFeature(Window.FEATURE_NO_TITLE)
        d.behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {}

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                maxHeight = maxHeight.coerceAtLeast(root!!.height.toFloat())
            }

        })
        d.behavior.state = BottomSheetBehavior.STATE_EXPANDED

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

    protected var internalState = BottomSheetBehavior.STATE_EXPANDED

    protected val root: ViewGroup?
        get() {
            return view?.parent as ViewGroup
        }

    open fun collapse() {
        collapse(true)
    }

    open fun collapse(animate: Boolean = true, onCollapsed: (() -> Unit)? = null) {
        Timber.d("Sheet COLLAPSE(anim=%b)", animate)

        if (internalState != BottomSheetBehavior.STATE_EXPANDED || root == null) {
            onCollapsed?.invoke()
            return
        }
        internalState = BottomSheetBehavior.STATE_COLLAPSED

        if (!animate) {
            root!!.translationY += maxHeight.coerceAtLeast(root!!.height.toFloat())
            internalState = BottomSheetBehavior.STATE_COLLAPSED
            onCollapsed?.invoke()
            return
        }
        KeyboardHelper.hideKeyboard(this)

        root!!.animate()
                .alpha(0f)
                .setDuration(120)
                .setInterpolator(AccelerateInterpolator())
                .setListener(object : Animator.AnimatorListener {
                    override fun onAnimationRepeat(animation: Animator?) {}
                    override fun onAnimationCancel(animation: Animator?) {}
                    override fun onAnimationStart(animation: Animator?) {}

                    override fun onAnimationEnd(animation: Animator?) {
                        onCollapsed?.invoke()
                    }

                })
                .start()

//        root!!.animate()
//                .translationYBy(maxHeight.coerceAtLeast(root!!.height.toFloat()))
//                .setDuration(100)
//                .setInterpolator(AccelerateDecelerateInterpolator())
//                .setListener(object : Animator.AnimatorListener {
//                    override fun onAnimationRepeat(animation: Animator?) {}
//                    override fun onAnimationCancel(animation: Animator?) {}
//                    override fun onAnimationStart(animation: Animator?) {}
//
//                    override fun onAnimationEnd(animation: Animator?) {
//                        onCollapsed?.invoke()
//                    }
//
//                })
//                .start()
    }

    open fun expand(force: Boolean = false, onExpanded: (() -> Unit)? = null) {
        if (internalState != BottomSheetBehavior.STATE_COLLAPSED || root == null) {
            onExpanded?.invoke()
            return
        }
        internalState = BottomSheetBehavior.STATE_EXPANDED
//        root!!.animate()
//                .translationYBy(-root!!.translationY)
//                .setDuration(100)
//                .setInterpolator(AccelerateDecelerateInterpolator())
//                .setListener(object : Animator.AnimatorListener {
//                    override fun onAnimationRepeat(animation: Animator?) {}
//                    override fun onAnimationCancel(animation: Animator?) {}
//                    override fun onAnimationStart(animation: Animator?) {}
//
//                    override fun onAnimationEnd(animation: Animator?) {
//                        onExpanded?.invoke()
//                    }
//
//                })
//                .start()
        root!!.animate()
                .alpha(1f)
                .setDuration(120)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .setListener(object : Animator.AnimatorListener {
                    override fun onAnimationRepeat(animation: Animator?) {}
                    override fun onAnimationCancel(animation: Animator?) {}
                    override fun onAnimationStart(animation: Animator?) {}

                    override fun onAnimationEnd(animation: Animator?) {
                        onExpanded?.invoke()
                    }

                })
                .start()
    }

    protected fun postRoot(r: () -> Unit) {
        (view?.parent as ViewGroup?)?.post(r)
    }

    protected fun postRootOnAnimation(r: () -> Unit) {
        (view?.parent as ViewGroup?)?.postOnAnimation(r)
    }

    protected fun postDelayedRootOnAnimation(delay: Long, r: () -> Unit) {
        (view?.parent as ViewGroup?)?.postOnAnimationDelayed(delay, r)
    }

    open fun startDialogFragment(executor: DialogFragmentExecutor) {
        collapse(true) {
            // disable parent dismissing if we're opening second dialog
            if (walletDialogFragment.isVisible()) {
                walletDialogFragment!!.removeOnDismissListener(nestedDialogParentExpandHandler)
            }
            walletDialogFragment = WalletDialogFragment.switchDialogWithExecutor(this, walletDialogFragment) {
                val d = executor(it)
                d.fixBlinkChildDialog()
                d.addOnDismissListener(nestedDialogParentExpandHandler)
                d
            }
        }
    }

    private val nestedDialogParentExpandHandler = DialogInterface.OnDismissListener {
        expand()
    }

    open fun startDialog(executor: DialogExecutor) {
        collapse(true) {

            // disable parent dismissing if we're opening second dialog
            if (walletDialog.isVisible()) {
                walletDialog!!.removeOnDismissListener(nestedDialogParentExpandHandler)
            }
            walletDialog = WalletDialog.switchDialogWithExecutor(this, walletDialog) {
                val d = executor(it)
                d.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                d.window?.setWindowAnimations(0)
                d.addOnDismissListener(nestedDialogParentExpandHandler)
                d
            }
        }
    }

    protected fun <T : BaseBottomSheetDialogFragment> T.fixNestedDialogBackgrounds(): T {
        if (dialog != null) {
            dialog!!.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            dialog!!.window?.setWindowAnimations(0)
        }
        return this
    }

    protected fun <T : Dialog> T.fixNestedDialogBackgrounds(): T {
        window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window?.setWindowAnimations(0)

        collapse()

        if (this is WalletDialog) {
            addOnDismissListener {
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