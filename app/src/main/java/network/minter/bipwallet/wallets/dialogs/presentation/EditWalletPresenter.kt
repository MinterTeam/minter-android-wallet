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
package network.minter.bipwallet.wallets.dialogs.presentation

import android.os.Bundle
import android.view.View
import moxy.InjectViewState
import network.minter.bipwallet.R
import network.minter.bipwallet.internal.dialogs.ConfirmDialogFragment
import network.minter.bipwallet.internal.helpers.HtmlCompat
import network.minter.bipwallet.internal.helpers.IntentHelper.getParcelExtraOrError
import network.minter.bipwallet.internal.mvp.MvpBasePresenter
import network.minter.bipwallet.internal.storage.SecretStorage
import network.minter.bipwallet.wallets.contract.EditWalletView
import network.minter.bipwallet.wallets.dialogs.ui.EditWalletDialog
import network.minter.bipwallet.wallets.selector.WalletItem
import javax.inject.Inject

@InjectViewState
class EditWalletPresenter @Inject constructor() : MvpBasePresenter<EditWalletView>() {
    @Inject lateinit var secretStorage: SecretStorage
    private var walletItem: WalletItem? = null
    private var title: String? = null

    override fun handleExtras(bundle: Bundle?) {
        super.handleExtras(bundle)

        walletItem = getParcelExtraOrError<WalletItem>(bundle, EditWalletDialog.EXTRA_WALLET_ITEM, "WalletItem required")
        if (walletItem!!.title != walletItem!!.addressShort) {
            title = walletItem!!.title
            viewState.setTitle(walletItem!!.title)
            viewState.setUniqueTitleExclude(walletItem!!.title)
        } else {
            viewState.setUniqueTitleExclude(null)
        }
    }

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()
        viewState.setEnableSubmit(true)
        viewState.setEnableRemove(secretStorage.addresses.size > 1)

        viewState.setOnSubmitClickListener(View.OnClickListener {
            onSubmit()
        })
        viewState.setOnRemoveClickListener(View.OnClickListener {
            onDeleteWallet()
        })

        viewState.addInputTextWatcher { inputWrapper, valid ->
            when (inputWrapper.fieldName) {
                "title" -> {
                    viewState.setEnableSubmit(valid)
                    if (valid) {
                        title = if (inputWrapper.text?.trim()?.isEmpty() == true) {
                            null
                        } else {
                            inputWrapper.text.toString()
                        }
                    }
                }
            }
        }
    }

    private fun onDeleteWallet() {
        viewState.startDialogFragment {
            ConfirmDialogFragment.Builder(it, R.string.dialog_title_delete_wallet)
                    .setDescription(HtmlCompat.fromHtml(it.getString(R.string.dialog_description_delete_wallet, walletItem!!.address.toShortString())))
                    .setDescriptionTypeface(R.font._inter_regular)
                    .setText(HtmlCompat.fromHtml(it.getString(R.string.dialog_text_delete_wallet)))
                    .setPositiveActionStyle(R.style.Wallet_Button_Green)
                    .setPositiveAction(R.string.btn_confirm) { d, _ ->
                        secretStorage.delete(walletItem!!.address)
                        viewState.callOnDelete(walletItem!!)
                        d.dismiss()
                        viewState.close()

                    }
                    .setNegativeAction(R.string.btn_cancel) { d, _ ->
                        d.dismiss()
                        viewState.expand()
                    }
                    .create()


        }
    }

    private fun onSubmit() {
        val sd = secretStorage.getSecret(walletItem!!.address)
        sd.title = title
        secretStorage.update(sd)
        viewState.close()
        viewState.callOnSave(walletItem!!)
    }
}