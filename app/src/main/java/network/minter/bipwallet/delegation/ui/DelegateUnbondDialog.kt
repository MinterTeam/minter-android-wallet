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

package network.minter.bipwallet.delegation.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import com.edwardstock.inputfield.form.DecimalInputFilter
import com.edwardstock.inputfield.form.InputGroup
import com.edwardstock.inputfield.form.InputWrapper
import com.edwardstock.inputfield.form.validators.RegexValidator
import dagger.android.support.AndroidSupportInjection
import moxy.presenter.InjectPresenter
import moxy.presenter.ProvidePresenter
import network.minter.bipwallet.BuildConfig
import network.minter.bipwallet.R
import network.minter.bipwallet.databinding.DialogDelegateUnbondBinding
import network.minter.bipwallet.delegation.contract.DelegateUnbondView
import network.minter.bipwallet.delegation.views.DelegateUnbondPresenter
import network.minter.bipwallet.internal.Wallet
import network.minter.bipwallet.internal.dialogs.BaseBottomSheetDialogFragment
import network.minter.bipwallet.internal.dialogs.DialogExecutor
import network.minter.bipwallet.internal.helpers.ExceptionHelper
import network.minter.bipwallet.internal.helpers.IntentHelper.toParcel
import network.minter.bipwallet.internal.helpers.ViewExtensions.postApply
import network.minter.bipwallet.internal.helpers.ViewExtensions.visible
import network.minter.bipwallet.internal.system.BroadcastReceiverManager
import network.minter.bipwallet.sending.account.SelectorData
import network.minter.bipwallet.sending.account.WalletAccountSelectorDialog
import network.minter.bipwallet.services.livebalance.broadcast.RTMBlockReceiver
import network.minter.bipwallet.wallets.utils.LastBlockHandler
import network.minter.core.crypto.MinterHash
import network.minter.core.crypto.MinterPublicKey
import network.minter.explorer.models.BaseCoinValue
import network.minter.explorer.models.ValidatorItem
import javax.inject.Inject
import javax.inject.Provider


/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
class DelegateUnbondDialog : BaseBottomSheetDialogFragment(), DelegateUnbondView {

    enum class Type(
            val titleRes: Int,
            val firstLabelRes: Int,
            val secondLabelRes: Int,
            val progressTitleRes: Int,
            val resultLabelRes: Int
    ) {
        Delegate(
                R.string.dialog_title_delegate_begin,
                R.string.dialog_label_delegate_you_are_delegating,
                R.string.dialog_label_delegate_to,
                R.string.dialog_label_delegate_progress,
                R.string.dialog_label_delegate_result
        ),
        Unbond(
                R.string.dialog_title_unbond_begin,
                R.string.dialog_label_unbond_you_are_unbonding,
                R.string.dialog_label_unbond_from,
                R.string.dialog_label_unbond_progress,
                R.string.dialog_label_unbond_result
        );
    }

    companion object {
        const val ARG_PUB_KEY = "ARG_PUB_KEY"
        const val ARG_COIN = "ARG_COIN"
        const val ARG_TYPE = "ARG_TYPE"
    }

    @Inject lateinit var presenterProvider: Provider<DelegateUnbondPresenter>
    @InjectPresenter lateinit var presenter: DelegateUnbondPresenter

    private lateinit var binding: DialogDelegateUnbondBinding
    private val inputGroup = InputGroup()

    @ProvidePresenter
    fun providePresenter(): DelegateUnbondPresenter {
        return presenterProvider.get()
    }

    override fun startExplorer(txHash: MinterHash) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Wallet.urlExplorerFront() + "/transactions/" + txHash.toString())))
    }

    override fun expand() {
        super.expand(false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidSupportInjection.inject(this)
        super.onCreate(savedInstanceState)
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DialogDelegateUnbondBinding.inflate(inflater, container, false)

        inputGroup.setup {
            add(binding.inputAmount, RegexValidator("^(\\d*)(\\.)?(\\d{1,18})?$").apply {
                errorMessage = "Invalid number"
            })
        }
        inputGroup.addFilter(binding.inputAmount, DecimalInputFilter(binding.inputAmount))
        presenter.handleExtras(arguments)

        binding.inputMasternode.input.isFocusable = false
        binding.inputCoin.input.isFocusable = false
        binding.inputAmount.input.setOnClickListener {
            binding.inputAmount.scrollToInput()
        }

        LastBlockHandler.handle(binding.lastUpdated)
        val broadcastManager = BroadcastReceiverManager(activity!!)
        broadcastManager.add(RTMBlockReceiver {
            LastBlockHandler.handle(binding.lastUpdated, it)
        })
        broadcastManager.register()

        return binding.root
    }

    override fun setTextChangedListener(listener: (input: InputWrapper, valid: Boolean) -> Unit) {
        inputGroup.addTextChangedListener(listener)
    }

    override fun startDialog(executor: DialogExecutor) {
        collapse()
        super.startDialog {
            val d = executor(it)
            d.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            d.window?.setWindowAnimations(0)
            d
        }
    }

    override fun setEnableSubmit(enable: Boolean) {
        binding.action.postApply { it.isEnabled = enable }
    }

    override fun setAccountSelectorError(error: CharSequence?) {
        binding.inputCoin.error = error
    }

    override fun setValidator(validator: ValidatorItem, onInflated: (View) -> Unit) {
        binding.apply {
            inputMasternode.inputOverlayVisible = true
            onInflated(inputMasternode.inputOverlay!!)
        }
    }

    override fun setValidator(validator: MinterPublicKey, onInflated: (View) -> Unit) {
        binding.apply {
            inputMasternode.inputOverlayVisible = true
            onInflated(inputMasternode.inputOverlay!!)
        }
    }

    override fun setOnValidatorSelectListener(listener: View.OnClickListener) {
        binding.inputMasternode.setOnClickListener(listener)
    }

    override fun setOnAccountSelectListener(listener: View.OnClickListener) {
        binding.inputCoin.setOnClickListener(listener)
    }

    override fun startValidatorSelector(items: List<SelectorData<ValidatorItem>>, listener: (SelectorData<ValidatorItem>) -> Unit) {
        WalletAccountSelectorDialog.Builder<ValidatorItem>(context!!, R.string.title_select_masternode)
                .setItems(items)
                .setOnClickListener(listener)
                .create()
                .fixNestedDialogBackgrounds()
                .show()
    }

    override fun startAccountSelector(items: List<SelectorData<BaseCoinValue>>, listener: (SelectorData<BaseCoinValue>) -> Unit) {
        WalletAccountSelectorDialog.Builder<BaseCoinValue>(activity!!, R.string.dialog_title_choose_coin)
                .setItems(items)
                .setOnClickListener(listener)
                .create()
                .fixNestedDialogBackgrounds()
                .show()
    }

    override fun setAccountName(accountName: CharSequence?) {
        binding.inputCoin.postApply { it.setText(accountName) }
    }

    override fun onError(err: CharSequence?) {
        binding.textError.postApply {
            it.visible = err != null
            it.text = err
        }
    }

    override fun setFee(fee: CharSequence) {
        binding.feeValue.postApply { it.text = fee }
    }


    override fun onError(t: Throwable?) {
        binding.textError.postApply {
            binding.textError.visible = t != null
            if (t == null) {
                return@postApply
            }

            if (BuildConfig.DEBUG) {
                binding.textError.text = "${t.message}\n${ExceptionHelper.getStackTrace(t)}"
            } else {
                binding.textError.text = t.message
            }
        }
    }

    override fun setOnClickUseMax(listener: View.OnClickListener) {
        binding.inputAmount.setOnSuffixTextClickListener(listener)
    }

    override fun setAmount(amount: String) {
        binding.inputAmount.setText(amount)
    }

    override fun setSubtitle(resId: Int) {
        binding.subtitle.setText(resId)
    }

    override fun setTitle(resId: Int) {
        binding.dialogTitle.setText(resId)
    }

    override fun setOnSubmitListener(listener: View.OnClickListener) {
        binding.action.setOnClickListener(listener)
    }

    override fun dismiss() {
        super.dismiss()
    }

    class Builder(type: Type) {
        private val args = Bundle()

        init {
            args.putSerializable(ARG_TYPE, type)
        }

        fun setPublicKey(pubKey: MinterPublicKey): Builder {
            args.putParcelable(ARG_PUB_KEY, pubKey.toParcel())
            return this
        }

        fun setSelectedCoin(coin: String): Builder {
            args.putString(ARG_COIN, coin)
            return this
        }

        fun build(): DelegateUnbondDialog {
            val dialog = DelegateUnbondDialog()
            dialog.arguments = args
            return dialog
        }
    }
}