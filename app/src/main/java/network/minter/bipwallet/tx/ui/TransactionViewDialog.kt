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

package network.minter.bipwallet.tx.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import dagger.android.support.AndroidSupportInjection
import moxy.presenter.InjectPresenter
import moxy.presenter.ProvidePresenter
import network.minter.bipwallet.R
import network.minter.bipwallet.databinding.DialogTxViewBinding
import network.minter.bipwallet.internal.dialogs.BaseBottomSheetDialogFragment
import network.minter.bipwallet.internal.helpers.IntentHelper.toParcel
import network.minter.bipwallet.internal.helpers.ResTextFormat
import network.minter.bipwallet.internal.helpers.ViewExtensions.copyOnClick
import network.minter.bipwallet.internal.helpers.ViewExtensions.setTextFormat
import network.minter.bipwallet.internal.helpers.ViewExtensions.visible
import network.minter.bipwallet.tx.adapters.TransactionFacade
import network.minter.bipwallet.tx.contract.TransactionView
import network.minter.bipwallet.tx.views.TransactionViewPresenter
import javax.inject.Inject
import javax.inject.Provider

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
class TransactionViewDialog : BaseBottomSheetDialogFragment(), TransactionView {
    companion object {
        const val ARG_TX = "ARG_TX"
    }

    @Inject lateinit var presenterProvider: Provider<TransactionViewPresenter>

    @InjectPresenter
    lateinit var presenter: TransactionViewPresenter
    private lateinit var binding: DialogTxViewBinding


    @ProvidePresenter
    fun providePresenter(): TransactionViewPresenter {
        return presenterProvider.get()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidSupportInjection.inject(this)
        super.onCreate(savedInstanceState)
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun inflateDetails(@LayoutRes layoutRes: Int, l: (View) -> Unit) {
        binding.detailsStub.layoutResource = layoutRes
        val view = binding.detailsStub.inflate()
        l(view)
    }

    override fun setFromAddress(address: String?) {
        binding.fromAddress.visible = !address.isNullOrEmpty()
        binding.fromAddress.copyOnClick()
        binding.fromAddress.text = address
    }

    override fun setFromName(name: String?) {
        binding.fromName.visible = !name.isNullOrEmpty()
        binding.fromName.text = name
    }

    override fun setFromAvatar(fromAvatar: String) {
        binding.fromAvatar.setImageUrl(fromAvatar)
    }

    override fun setFromAvatar(resId: Int) {
        binding.fromAvatar.setImageResource(resId)
    }

    override fun setToAddress(recipient: String?) {
        binding.toAddress.visible = !recipient.isNullOrEmpty()
        binding.toAddress.copyOnClick()
        binding.toAddress.text = recipient
    }

    override fun setToName(name: String?) {
        binding.toName.visible = !name.isNullOrEmpty()
        binding.toName.text = name
    }

    override fun setToAvatar(toAvatar: String?) {
        binding.toAvatar.setImageUrl(toAvatar)
    }

    override fun setToAvatar(toAvatar: String?, fallback: Int) {
        binding.toAvatar.setImageUrlFallback(toAvatar, fallback)
    }

    override fun setToAvatar(resId: Int) {
        binding.toAvatar.setImageResource(resId)
    }

    override fun showTo(show: Boolean) {
        binding.apply {
            labelTo.visible = show
            toAvatar.visible = show
            toAddress.visible = show
        }
    }

    override fun setPayload(payload: String?) {
        binding.detailsPayload.visible = !payload.isNullOrEmpty()
        binding.valuePayload.text = payload
        binding.valuePayload.copyOnClick()
    }

    override fun setTimestamp(format: String) {
        binding.valueTimestamp.text = format
    }

    override fun setFee(fee: String) {
        binding.valueFee.text = fee
    }

    override fun setBlockNumber(blockNum: String) {
        binding.valueBlock.text = blockNum
    }

    override fun setBlockClickListener(listener: View.OnClickListener) {
        binding.valueBlock.setOnClickListener(listener)
    }

    override fun startIntent(intent: Intent) {
        activity?.startActivity(intent)
    }

    override fun setOnClickShare(listener: View.OnClickListener) {
        binding.action.setOnClickListener(listener)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DialogTxViewBinding.inflate(layoutInflater, container, false)

        val args = arguments
        presenter.handleExtras(args)

        return binding.root
    }

    override fun setTitle(resId: Int) {
        binding.dialogTitle.setText(resId)
    }

    override fun setTitle(fmt: ResTextFormat) {
        binding.dialogTitle.setTextFormat(fmt)
    }

    override fun setTitle(title: CharSequence) {
        binding.dialogTitle.text = title
    }

    override fun setTitleTyped(@StringRes txTypeRes: Int) {
        binding.dialogTitle.setTextFormat(ResTextFormat(R.string.fmt_type_of_transaction, getString(txTypeRes)))
    }

    data class Builder(
            var tx: TransactionFacade
    ) {

        fun tx(tx: TransactionFacade): Builder = apply { this.tx = tx }

        fun build(): TransactionViewDialog {
            val args = Bundle()
            args.putParcelable(ARG_TX, tx.toParcel())
            val dialog = TransactionViewDialog()
            dialog.arguments = args
            return dialog
        }
    }
}