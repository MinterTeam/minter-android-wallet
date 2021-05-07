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
package network.minter.bipwallet.auth.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import moxy.presenter.InjectPresenter
import moxy.presenter.ProvidePresenter
import network.minter.bipwallet.R
import network.minter.bipwallet.auth.contract.AuthView
import network.minter.bipwallet.auth.views.AuthPresenter
import network.minter.bipwallet.databinding.FragmentAuthBinding
import network.minter.bipwallet.internal.BaseInjectFragment
import network.minter.bipwallet.internal.helpers.IntentHelper.newUrl
import network.minter.bipwallet.wallets.dialogs.ui.CreateWalletDialog
import network.minter.bipwallet.wallets.dialogs.ui.SignInMnemonicDialog
import javax.inject.Inject
import javax.inject.Provider

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock></edward.vstock>@gmail.com>
 */
class AuthFragment : BaseInjectFragment(), AuthView {
    @Inject lateinit var authPresenterProvider: Provider<AuthPresenter>
    @InjectPresenter lateinit var presenter: AuthPresenter

    private lateinit var b: FragmentAuthBinding
    private var mDialog: BottomSheetDialogFragment? = null

    @ProvidePresenter
    fun providePresenter(): AuthPresenter {
        return authPresenterProvider.get()
    }

    override fun setOnClickSignIn(listener: View.OnClickListener) {
        b.actionSignin.setOnClickListener(listener)
    }

    override fun setOnClickCreateWallet(listener: View.OnClickListener) {
        b.actionCreateWallet.setOnClickListener(listener)
    }

    override fun setOnHelp(listener: View.OnClickListener) {
        b.actionHelp.setOnClickListener(listener)
    }

    override fun startSignIn() {
        if (mDialog != null) {
            try {
                mDialog!!.dismiss()
            } catch (ignore: Throwable) {
            }
            mDialog = null
        }
        mDialog = SignInMnemonicDialog()
        mDialog!!.show(parentFragmentManager, null)
    }

    override fun startHelp() {
        requireActivity().startActivity(newUrl("https://help.minter.network"))
    }

    override fun startCreateWallet() {
        if (mDialog != null) {
            try {
                mDialog!!.dismiss()
            } catch (ignore: Throwable) {
            }
            mDialog = null
        }
        mDialog = CreateWalletDialog.Builder()
                .build()
        mDialog!!.show(parentFragmentManager, null)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        postponeEnterTransition()

        b = FragmentAuthBinding.inflate(inflater, container, false)
        ViewCompat.setTransitionName(b.logo, getString(R.string.transaction_auth_logo))
        startPostponedEnterTransition()
        return b.root
    }


}