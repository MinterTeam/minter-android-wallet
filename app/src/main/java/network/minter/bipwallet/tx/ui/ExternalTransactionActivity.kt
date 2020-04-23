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
import android.app.ActivityManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextWatcher
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import butterknife.BindView
import butterknife.ButterKnife
import com.google.android.material.textfield.TextInputLayout
import moxy.presenter.InjectPresenter
import moxy.presenter.ProvidePresenter
import network.minter.bipwallet.R
import network.minter.bipwallet.external.*
import network.minter.bipwallet.internal.BaseMvpInjectActivity
import network.minter.bipwallet.internal.Wallet
import network.minter.bipwallet.internal.dialogs.WalletDialog
import network.minter.bipwallet.internal.dialogs.WalletDialog.Companion.releaseDialog
import network.minter.bipwallet.internal.dialogs.WalletDialog.Companion.switchDialogWithExecutor
import network.minter.bipwallet.internal.dialogs.WalletProgressDialog
import network.minter.bipwallet.internal.helpers.MathHelper.startsFromNumber
import network.minter.bipwallet.internal.system.ActivityBuilder
import network.minter.bipwallet.internal.views.widgets.WalletButton
import network.minter.bipwallet.security.PauseTimer
import network.minter.bipwallet.tx.contract.ExternalTransactionView
import network.minter.bipwallet.tx.views.ExternalTransactionPresenter
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
    @JvmField @Inject
    var presenterProvider: Provider<ExternalTransactionPresenter>? = null

    @JvmField @InjectPresenter
    var presenter: ExternalTransactionPresenter? = null

    @JvmField @BindView(R.id.toolbar)
    var toolbar: Toolbar? = null

    @JvmField @BindView(R.id.input_first)
    var inputFirst: AppCompatEditText? = null

    @JvmField @BindView(R.id.layout_input_first)
    var layoutInputFirst: TextInputLayout? = null

    @JvmField @BindView(R.id.input_second)
    var inputSecond: AppCompatEditText? = null

    @JvmField @BindView(R.id.layout_input_second)
    var layoutInputSecond: TextInputLayout? = null

    @JvmField @BindView(R.id.input_payload)
    var inputPayload: AppCompatEditText? = null

    @JvmField @BindView(R.id.fee_value)
    var feeValue: TextView? = null

    @JvmField @BindView(R.id.fee_label)
    var feeLabel: TextView? = null

    @JvmField @BindView(R.id.text_error)
    var textError: TextView? = null

    @JvmField @BindView(R.id.action)
    var action: WalletButton? = null

    @JvmField @BindView(R.id.cancel_action)
    var cancelAction: WalletButton? = null
    private var mCurrentDialog: WalletDialog? = null
    override fun setFirstLabel(label: CharSequence) {
        layoutInputFirst!!.hint = label
    }

    override fun setFirstValue(value: CharSequence) {
        inputFirst!!.setText(value)
    }

    override fun setSecondLabel(label: CharSequence) {
        layoutInputSecond!!.hint = label
    }

    override fun setSecondValue(value: CharSequence) {
        inputSecond!!.setText(value)
    }

    override fun setPayload(payloadString: CharSequence) {
        inputPayload!!.setText(payloadString)
    }

    override fun setCommission(fee: CharSequence) {
        if (fee.isEmpty()) {
            return
        }
        feeValue!!.text = fee
        if (startsFromNumber(fee)) {
            feeLabel!!.visibility = View.VISIBLE
            feeValue!!.textAlignment = View.TEXT_ALIGNMENT_TEXT_END
        } else {
            feeLabel!!.visibility = View.GONE
            feeValue!!.textAlignment = View.TEXT_ALIGNMENT_CENTER
        }
    }

    override fun setFirstVisible(visibility: Int) {
        layoutInputFirst!!.visibility = visibility
    }

    override fun setSecondVisible(visibility: Int) {
        layoutInputSecond!!.visibility = visibility
    }

    override fun startDialog(executor: (Context) -> WalletDialog) {
        mCurrentDialog = switchDialogWithExecutor(this, mCurrentDialog, executor)
    }

    override fun startDialog(cancelable: Boolean, executor: (Context) -> WalletDialog) {
        mCurrentDialog = switchDialogWithExecutor(this, mCurrentDialog, executor)
        mCurrentDialog!!.setCancelable(cancelable)
    }

    override fun setPayloadTextChangedListener(textWatcher: TextWatcher) {
        inputPayload!!.addTextChangedListener(textWatcher)
    }

    override fun setOnConfirmListener(listener: View.OnClickListener) {
        action!!.setOnClickListener(listener)
    }

    override fun setOnCancelListener(listener: View.OnClickListener) {
        cancelAction!!.setOnClickListener(listener)
    }

    override fun hideProgress() {
        releaseDialog(mCurrentDialog)
        mCurrentDialog = null
    }

    override fun showProgress() {
        mCurrentDialog = switchDialogWithExecutor(this, mCurrentDialog, { ctx: Context? ->
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

    override fun startExplorer(txHash: String?) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Wallet.urlExplorerFront() + "/transactions/" + txHash)))
    }

    override fun disableAll() {
        setFirstVisible(View.GONE)
        setSecondVisible(View.GONE)
        action!!.isEnabled = false
        action!!.isClickable = false
        cancelAction!!.isEnabled = true
        cancelAction!!.isClickable = true
    }

    @ProvidePresenter
    fun providePresenter(): ExternalTransactionPresenter {
        return presenterProvider!!.get()
    }

    override fun onStop() {
        super.onStop()
        releaseDialog(mCurrentDialog)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_external_transaction)
        ButterKnife.bind(this)
        setupToolbar(toolbar!!)
        presenter!!.handleExtras(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        presenter!!.handleExtras(intent)
    }

    private fun checkIsLastActivity(): Boolean {
        try {
            val mngr = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?
            if (mngr != null) {
                val taskList = mngr.getRunningTasks(3)
                return taskList[0].numActivities == 1 && taskList[0].topActivity!!.className == this.javaClass.name
            }
        } catch (ignore: Throwable) {
            Timber.w(ignore, "Unable to detect left activity count")
            return false
        }
        return false
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

    companion object {
        const val EXTRA_RAW_DATA = "EXTRA_RAW_TX"
    }
}