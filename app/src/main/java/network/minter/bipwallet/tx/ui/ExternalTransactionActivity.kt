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
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import moxy.presenter.InjectPresenter
import moxy.presenter.ProvidePresenter
import network.minter.bipwallet.databinding.ActivityExternalTransactionBinding
import network.minter.bipwallet.external.*
import network.minter.bipwallet.internal.BaseMvpInjectActivity
import network.minter.bipwallet.internal.Wallet
import network.minter.bipwallet.internal.dialogs.WalletDialog
import network.minter.bipwallet.internal.dialogs.WalletDialog.Companion.releaseDialog
import network.minter.bipwallet.internal.dialogs.WalletDialog.Companion.switchDialogWithExecutor
import network.minter.bipwallet.internal.dialogs.WalletProgressDialog
import network.minter.bipwallet.internal.helpers.MathHelper.startsFromNumber
import network.minter.bipwallet.internal.system.ActivityBuilder
import network.minter.bipwallet.internal.system.BroadcastReceiverManager
import network.minter.bipwallet.security.PauseTimer
import network.minter.bipwallet.services.livebalance.RTMService
import network.minter.bipwallet.services.livebalance.ServiceConnector
import network.minter.bipwallet.services.livebalance.broadcast.RTMBlockReceiver
import network.minter.bipwallet.services.livebalance.broadcast.RTMBlockReceiver.Companion.send
import network.minter.bipwallet.tx.contract.ExternalTransactionView
import network.minter.bipwallet.tx.views.ExternalTransactionPresenter
import network.minter.bipwallet.wallets.utils.LastBlockHandler
import network.minter.core.crypto.MinterAddress
import timber.log.Timber
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

    override fun setAdapter(adapter: RecyclerView.Adapter<*>) {
        b.inputList.adapter = adapter
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

    override fun onStart() {
        super.onStart()
        ServiceConnector.bind(this)
        ServiceConnector.onConnected()
                .subscribe { res: RTMService ->
                    res.setOnMessageListener { message: String?, channel: String, address: MinterAddress? ->
                        if (channel == RTMService.CHANNEL_BLOCKS) {
                            send(Wallet.app().context(), message!!)
                        }
                    }
                }
    }

    override fun onStop() {
        super.onStop()
        ServiceConnector.release(this)
        releaseDialog(walletDialog)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityExternalTransactionBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.inputList.layoutManager = LinearLayoutManager(this)

        setupToolbar(b.toolbar)
        presenter.handleExtras(intent)

        LastBlockHandler.handle(b.lastUpdated)
        val broadcastManager = BroadcastReceiverManager(this)
        broadcastManager.add(RTMBlockReceiver {
            LastBlockHandler.handle(b.lastUpdated, it)
        })
        broadcastManager.register()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        presenter.handleExtras(intent)
    }

    private fun checkIsLastActivity(): Boolean {
        return isTaskRoot.let {
            Timber.d("Is task root: %b", it)
            it
        }
//        try {
//            val mngr = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?
//            if (mngr != null) {
//                val taskList = mngr.getRunningTasks(3)
//                return taskList[0].numActivities == 1 && taskList[0].topActivity!!.className == this.javaClass.name
//            }
//        } catch (ignore: Throwable) {
//            Timber.w(ignore, "Unable to detect left activity count")
//            return false
//        }
//        return false
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