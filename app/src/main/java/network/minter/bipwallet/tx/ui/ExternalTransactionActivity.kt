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

import android.app.Activity
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.Fragment
import moxy.presenter.InjectPresenter
import moxy.presenter.ProvidePresenter
import network.minter.bipwallet.R
import network.minter.bipwallet.databinding.ActivityExternalTransactionBinding
import network.minter.bipwallet.exchange.ui.ConvertCoinActivity
import network.minter.bipwallet.external.*
import network.minter.bipwallet.internal.BaseMvpInjectActivity
import network.minter.bipwallet.internal.Wallet
import network.minter.bipwallet.internal.dialogs.WalletDialog
import network.minter.bipwallet.internal.dialogs.WalletDialog.Companion.releaseDialog
import network.minter.bipwallet.internal.dialogs.WalletDialog.Companion.switchDialogWithExecutor
import network.minter.bipwallet.internal.dialogs.WalletProgressDialog
import network.minter.bipwallet.internal.helpers.HtmlCompat
import network.minter.bipwallet.internal.helpers.MathHelper.startsFromNumber
import network.minter.bipwallet.internal.helpers.ViewExtensions.nvisible
import network.minter.bipwallet.internal.helpers.ViewExtensions.postApply
import network.minter.bipwallet.internal.helpers.ViewExtensions.visible
import network.minter.bipwallet.internal.system.ActivityBuilder
import network.minter.bipwallet.internal.system.BroadcastReceiverManager
import network.minter.bipwallet.internal.views.utils.SingleCallHandler
import network.minter.bipwallet.security.PauseTimer
import network.minter.bipwallet.services.livebalance.broadcast.RTMBlockReceiver
import network.minter.bipwallet.tx.contract.ExternalTransactionView
import network.minter.bipwallet.tx.views.ExternalTransactionPresenter
import network.minter.bipwallet.wallets.utils.LastBlockHandler
import network.minter.core.crypto.MinterAddress
import network.minter.explorer.models.CoinItemBase
import timber.log.Timber
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Provider

/**
 * minter-android-wallet. 2019
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
@AppDeepLink("tx", "tx/{data}")
@WebDeepLink("tx", "tx/{data}")
@WebDeepLinkInsecure("tx", "tx/{data}")
@TestnetWebDeepLink("tx", "tx/{data}")
@TestnetWebDeepLinkInsecure("tx", "tx/{data}")
class ExternalTransactionActivity : BaseMvpInjectActivity(), ExternalTransactionView {
    companion object {
        const val EXTRA_RAW_DATA = "EXTRA_RAW_TX"
    }

    @Inject lateinit var presenterProvider: Provider<ExternalTransactionPresenter>
    @InjectPresenter lateinit var presenter: ExternalTransactionPresenter

    private lateinit var b: ActivityExternalTransactionBinding
    private var enabledEditAction = false

    override fun setData(allRows: MutableList<TxInputFieldRow<*>>) {
        b.inputListLayout.removeAllViews()
        for (fieldRow in allRows) {
            val view = layoutInflater.inflate(fieldRow.getItemView(), null)
            val vh = TxInputFieldRow.ViewHolder(view)
            b.inputListLayout.addView(view)
            fieldRow.onBindViewHolder(vh)
        }
    }

    override fun setFee(fee: CharSequence) {
        if (fee.isEmpty()) {
            return
        }
        b.feeValue.text = fee
        if (startsFromNumber(fee)) {
            b.feeLabel.visibility = View.VISIBLE
            b.feeValue.textAlignment = View.TEXT_ALIGNMENT_TEXT_END
        } else {
            b.feeLabel.visibility = View.GONE
            b.feeValue.textAlignment = View.TEXT_ALIGNMENT_CENTER
        }
    }

    override fun startDialog(cancelable: Boolean, executor: (Context) -> WalletDialog) {
        walletDialog = switchDialogWithExecutor(this, walletDialog, executor)
        walletDialog!!.setCancelable(cancelable)
    }

    override fun setOnConfirmListener(listener: View.OnClickListener) {
        b.action.setOnClickListener(listener)
    }

    override fun setOnCancelListener(listener: View.OnClickListener) {
        b.cancelAction.setOnClickListener(listener)
    }

    override fun hideProgress() {
        releaseDialog(walletDialog)
        walletDialog = null
    }

    override fun showProgress() {
        walletDialog = switchDialogWithExecutor(this, walletDialog, { ctx: Context? ->
            WalletProgressDialog.Builder(ctx, "Please, wait")
                    .setText("We're loading required account information")
                    .create()
        })
    }

    override fun finishSuccess() {
        setResult(Activity.RESULT_OK)
        if (checkIsLastActivity()) {
            try {
                PauseTimer.logout()
                finishAffinity()
            } catch (ignore: Throwable) {
                Timber.e(ignore)
                finish()
            }
        } else {
            finish()
        }
    }

    override fun finishCancel() {
        setResult(Activity.RESULT_CANCELED)
        if (checkIsLastActivity()) {
            try {
                PauseTimer.logout()
                finishAffinity()
            } catch (ignore: Throwable) {
                Timber.e(ignore)
                finish()
            }
        } else {
            finish()
        }
    }

    override fun startExplorer(hash: String?) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Wallet.urlExplorerFront() + "/transactions/" + hash)))
    }

    override fun showBannerExchangeText(text: CharSequence, listener: (View) -> Unit) {
        b.exchangeContainer.visible = true
        b.exchangeText.text = text
        b.exchangeAction.visible = true
        b.exchangeAction.setOnClickListener {
            listener(it)
        }
    }

    override fun showBannerError(text: CharSequence) {
        b.exchangeContainer.visible = true
        b.exchangeText.text = text
        b.exchangeAction.visible = false
    }

    override fun showBannerError(resId: Int) {
        b.exchangeContainer.visible = true
        b.exchangeText.text = HtmlCompat.fromHtml(getString(resId))
        b.exchangeAction.visible = false
    }

    override fun startExchangeCoins(requestCode: Int, coin: CoinItemBase, value: BigDecimal, account: MinterAddress) {
        SingleCallHandler.call("exchange") {
            ConvertCoinActivity.Builder(this)
                    .buyCoins(coin, value)
                    .withAccount(account)
                    .start(requestCode)
        }

    }

    override fun hideExchangeBanner() {
        b.exchangeContainer.visible = false
    }

    override fun showWaitProgress() {
        runOnUiThread {
            b.inputListLayout.alpha = 0.3f
            b.progress.nvisible = true
            b.action.isEnabled = false
            b.action.visible = false
            b.cancelAction.visible = false
            b.feeLabel.visible = false
            b.feeValue.visible = false
        }

    }

    override fun hideWaitProgress() {
        runOnUiThread {
            b.inputListLayout.alpha = 1f
            b.progress.nvisible = false
            b.action.isEnabled = true
            b.action.visible = true
            b.cancelAction.visible = true
            b.feeLabel.visible = true
            b.feeValue.visible = true
        }
    }

    override fun enableSubmit(enable: Boolean) {
        b.action.postApply {
            it.isEnabled = enable
        }
    }

    override fun enableEditAction(enable: Boolean) {
        if (enable && !enabledEditAction) {
            enabledEditAction = true
            b.toolbar.postApply {
                b.toolbar.menu
                        .add(R.string.btn_edit)
                        .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS)
                        .setIcon(R.drawable.ic_edit_grey)
                        .setOnMenuItemClickListener {
                            presenter.toggleEditing()
                            false
                        }
            }

        }
    }

    override fun disableAll() {
        b.action.isEnabled = false
        b.action.isClickable = false
        b.cancelAction.isEnabled = true
        b.cancelAction.isClickable = true
    }

    @ProvidePresenter
    fun providePresenter(): ExternalTransactionPresenter {
        return presenterProvider.get()
    }

    override fun onStop() {
        super.onStop()
        releaseDialog(walletDialog)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityExternalTransactionBinding.inflate(layoutInflater)
        setContentView(b.root)

        setupToolbar(b.toolbar)
        presenter.handleExtras(intent)

        LastBlockHandler.handle(b.lastUpdated)
        val broadcastManager = BroadcastReceiverManager(this)
        broadcastManager.add(RTMBlockReceiver {
            LastBlockHandler.handle(b.lastUpdated, it)
        })
        broadcastManager.register()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        presenter.onActivityResult(requestCode, resultCode, data)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        presenter.resetAccount()
        presenter.handleExtras(intent)
    }

    private fun checkIsLastActivity(): Boolean {
        return isTaskRoot.let {
            Timber.d("Is task root: %b", it)
            it
        }
    }

    class Builder : ActivityBuilder {
        private var mRawData: String

        constructor(from: Activity, rawData: String) : super(from) {
            mRawData = rawData
        }

        constructor(from: Fragment, rawData: String) : super(from) {
            mRawData = rawData
        }

        constructor(from: Service, rawData: String) : super(from) {
            mRawData = rawData
        }

        override fun getActivityClass(): Class<*> {
            return ExternalTransactionActivity::class.java
        }

        override fun onBeforeStart(intent: Intent) {
            super.onBeforeStart(intent)
            intent.putExtra(EXTRA_RAW_DATA, mRawData)
        }
    }

}