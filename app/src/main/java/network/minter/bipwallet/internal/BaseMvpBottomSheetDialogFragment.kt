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

package network.minter.bipwallet.internal

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import moxy.MvpDelegate

open class BaseMvpBottomSheetDialogFragment : BottomSheetDialogFragment() {
    private var mIsStateSaved = false

    /**
     * @return The [MvpDelegate] being used by this Fragment.
     */
    var mvpDelegate: MvpDelegate<out BaseMvpBottomSheetDialogFragment>? = null
        get() {
            if (field == null) {
                field = MvpDelegate(this)
            }
            return field
        }
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mvpDelegate!!.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        mIsStateSaved = false
        mvpDelegate!!.onAttach()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mIsStateSaved = true
        mvpDelegate!!.onSaveInstanceState(outState)
        mvpDelegate!!.onDetach()
    }

    override fun onStop() {
        super.onStop()
        mvpDelegate!!.onDetach()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mvpDelegate!!.onDetach()
        mvpDelegate!!.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()

        //We leave the screen and respectively all fragments will be destroyed
        if (activity!!.isFinishing) {
            mvpDelegate!!.onDestroy()
            return
        }

        // When we rotate device isRemoving() return true for fragment placed in backstack
        // http://stackoverflow.com/questions/34649126/fragment-back-stack-and-isremoving
        if (mIsStateSaved) {
            mIsStateSaved = false
            return
        }
        var anyParentIsRemoving = false
        var parent = parentFragment
        while (!anyParentIsRemoving && parent != null) {
            anyParentIsRemoving = parent.isRemoving
            parent = parent.parentFragment
        }
        if (isRemoving || anyParentIsRemoving) {
            mvpDelegate!!.onDestroy()
        }
    }

    fun runOnUiThread(r: () -> Unit) {
        Handler(Looper.getMainLooper()).post(r)
    }

}