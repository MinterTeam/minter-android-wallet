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
package network.minter.bipwallet.settings.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.PromptInfo
import androidx.recyclerview.widget.RecyclerView
import moxy.presenter.InjectPresenter
import moxy.presenter.ProvidePresenter
import network.minter.bipwallet.BuildConfig
import network.minter.bipwallet.R
import network.minter.bipwallet.auth.ui.AuthActivity
import network.minter.bipwallet.databinding.FragmentTabSettingsBinding
import network.minter.bipwallet.home.HomeModule
import network.minter.bipwallet.home.HomeTabFragment
import network.minter.bipwallet.internal.Wallet
import network.minter.bipwallet.internal.dialogs.ConfirmDialog
import network.minter.bipwallet.internal.helpers.ViewExtensions.visibleForTestnet
import network.minter.bipwallet.internal.helpers.ViewHelper
import network.minter.bipwallet.internal.views.SnackbarBuilder
import network.minter.bipwallet.internal.views.list.BorderedItemSeparator
import network.minter.bipwallet.internal.views.list.NonScrollableLinearLayoutManager
import network.minter.bipwallet.internal.views.utils.SingleCallHandler
import network.minter.bipwallet.security.SecurityModule.PinMode
import network.minter.bipwallet.security.ui.PinEnterActivity
import network.minter.bipwallet.settings.contract.SettingsTabView
import network.minter.bipwallet.settings.views.SettingsTabPresenter
import network.minter.bipwallet.wallets.ui.WalletsTopRecolorHelper
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Provider

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock></edward.vstock>@gmail.com>
 */
class SettingsTabFragment : HomeTabFragment(), SettingsTabView {

    @Inject lateinit var presenterProvider: Provider<SettingsTabPresenter>
    @InjectPresenter lateinit var presenter: SettingsTabPresenter

    private lateinit var binding: FragmentTabSettingsBinding

    override fun onAttach(context: Context) {
        HomeModule.getComponent().inject(this)
        super.onAttach(context)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        presenter.onLowMemory()
    }

    override fun onTabSelected() {
        super.onTabSelected()
        if (WalletsTopRecolorHelper.enableRecolorSystemUI()) {
            ViewHelper.setSystemBarsLightness(this, true)
            ViewHelper.setStatusBarColorAnimate(this, -0x1)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentTabSettingsBinding.inflate(inflater, container, false)

        setHasOptionsMenu(true)

        binding.apply {
            testnetWarning.visibleForTestnet()
            activity!!.menuInflater.inflate(R.menu.menu_tab_settings, toolbar.menu)
            toolbar.setOnMenuItemClickListener { item: MenuItem -> onOptionsItemSelected(item) }

            val itemSeparator = BorderedItemSeparator(activity, R.drawable.shape_bottom_separator, false, true)
            itemSeparator.setFirstElementOffset(1)
            itemSeparator.setDividerPadding(Wallet.app().display().dpToPx(16), 0, 0, 0)
            itemSeparator.setLastDividerPadding(0, 0, 0, 0)

            listSecurity.layoutManager = NonScrollableLinearLayoutManager(activity)
            listSecurity.addItemDecoration(itemSeparator)

            listMain.layoutManager = NonScrollableLinearLayoutManager(activity)
            listMain.addItemDecoration(itemSeparator)

            listAdditional.layoutManager = NonScrollableLinearLayoutManager(activity)
            listAdditional.addItemDecoration(itemSeparator)

            if (BuildConfig.DEBUG) {
                appVersion.text = "${BuildConfig.FLAVOR}: ${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})"
            } else {
                appVersion.text = "Version: ${BuildConfig.VERSION_NAME}"
            }
        }

        return binding.root
    }

    override fun setMainAdapter(mainAdapter: RecyclerView.Adapter<*>) {
        binding.listMain.adapter = mainAdapter
    }

    override fun setAdditionalAdapter(additionalAdapter: RecyclerView.Adapter<*>) {
        binding.listAdditional.adapter = additionalAdapter
    }

    override fun setSecurityAdapter(securityAdapter: RecyclerView.Adapter<*>) {
        binding.listSecurity.adapter = securityAdapter
    }

    override fun startAvatarChooser() {}

    override fun showMessage(message: CharSequence) {
        SnackbarBuilder(this)
                .setMessage(message)
                .setDurationShort()
                .show()
    }

    override fun startPinCodeManager(requestCode: Int, mode: PinMode) {
        SingleCallHandler.call("pin-manager"
        ) {
            PinEnterActivity.Builder(activity!!, mode)
                    .start(requestCode)
        }
    }

    override fun startBiometricPrompt(callback: BiometricPrompt.AuthenticationCallback) {
        val info = PromptInfo.Builder()
                .setTitle(getString(R.string.pin_fp_title_enable))
                .setDescription("")
                .setSubtitle("")
                .setNegativeButtonText(getString(R.string.btn_cancel))
                .build()
        val executor: Executor = Executors.newSingleThreadExecutor()
        val prompt = BiometricPrompt(activity!!, executor, callback)
        prompt.authenticate(info)
    }

    override fun startFingerprintEnrollment() {
        var intent: Intent
        intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Intent(Settings.ACTION_FINGERPRINT_ENROLL)
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }
        try {
            startActivity(intent)
        } catch (wtf: ActivityNotFoundException) {
            intent = Intent(Settings.ACTION_SECURITY_SETTINGS)
            startActivity(intent)
        } catch (wtf2: SecurityException) {
            try {
                intent = Intent(Settings.ACTION_SECURITY_SETTINGS)
                startActivity(intent)
            } catch (wtf3: Throwable) {
                startDialog { ctx: Context? ->
                    ConfirmDialog.Builder(ctx!!, R.string.fingerprint_dialog_enroll_title)
                            .setText(R.string.fingerprint_dialog_enroll_text_fix)
                            .setPositiveAction(R.string.btn_ok, null)
                            .create()
                }
            }
        }
    }

    override fun startIntent(intent: Intent) {
        startActivity(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        presenter.onActivityResult(requestCode, resultCode, data)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.logout) {
            presenter.onLogout()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        HomeModule.getComponent().inject(this)
        super.onCreate(savedInstanceState)
    }

    override fun setOnOurChannelClickListener(listener: View.OnClickListener) {
        binding.actionOurChannel.setOnClickListener(listener)
    }

    override fun setOnSupportChatClickListener(listener: View.OnClickListener) {
        binding.actionSupportChat.setOnClickListener(listener)
    }

    override fun startLogin() {
        val intent = Intent(activity, AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        activity!!.startActivity(intent)
        activity!!.finish()
    }

    @ProvidePresenter
    fun providePresenter(): SettingsTabPresenter {
        return presenterProvider.get()
    }
}