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

package network.minter.bipwallet.share

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.android.support.AndroidSupportInjection
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import network.minter.bipwallet.R
import network.minter.bipwallet.databinding.DialogShareAddressBinding
import network.minter.bipwallet.internal.Wallet.app
import network.minter.bipwallet.internal.dialogs.BaseBottomSheetDialogFragment
import network.minter.bipwallet.internal.helpers.ContextHelper
import network.minter.bipwallet.internal.helpers.TextHelper.humanReadableBytes
import network.minter.bipwallet.internal.helpers.ViewExtensions.visible
import network.minter.bipwallet.internal.storage.SecretStorage
import network.minter.bipwallet.internal.system.WalletFileProvider
import network.minter.bipwallet.internal.views.list.ViewElevationOnScrollNestedScrollView
import network.minter.core.crypto.MinterAddress
import timber.log.Timber
import java.io.File
import javax.inject.Inject


/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
class ShareDialog : BaseBottomSheetDialogFragment() {

    @Inject lateinit var secretStorage: SecretStorage
    private lateinit var address: MinterAddress
    private var outFile: File? = null

    private lateinit var b: DialogShareAddressBinding
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        b = DialogShareAddressBinding.inflate(inflater, container, false)

        address = secretStorage.mainWallet

        b.address.text = address.toString()
        b.address.setOnClickListener(::copyAddress)
        b.actionCopy.setOnClickListener(::copyAddress)
        b.scroll.setOnScrollChangeListener(ViewElevationOnScrollNestedScrollView(b.dialogTop))


        // 74% of screen is qr code image
        val qrWidth = app().display().width.toFloat() * 0.74f

        QRAddressGenerator.create(qrWidth.toInt(), address)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(Consumer { res: QRAddressGenerator.Result ->
                    if (res.bitmap == null) {
                        Timber.e("Unable to create QR: Unknown reason.")
                        return@Consumer
                    }
                    Timber.d("Bitmap size: %s", humanReadableBytes(res.bitmapSize(), true))
                    outFile = res.fileJPG
                    showProgress(false)
                    setQRCode(res.bitmap!!)
                    setOnActionShareQR()
                }, Consumer { t: Throwable ->
                    onError(t)
                    showProgress(false)
                })

        return b.root
    }

    private fun onError(t: Throwable) {
        Timber.e(t)
    }

    private fun setOnActionShareQR() {
        b.actionShare.isEnabled = true

        b.actionShare.setOnClickListener {
            val qrPath = WalletFileProvider.getUriForFile(
                    context!!,
                    app().context().applicationContext.packageName + ".fileprovider",
                    outFile!!
            )

            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_TEXT, address.toString())
            shareIntent.putExtra(Intent.EXTRA_STREAM, qrPath)
            shareIntent.type = "image/jpg"

            WalletFileProvider.grantPermissions(context!!, shareIntent, qrPath, false)

            startActivity(Intent.createChooser(shareIntent, "send"))
        }
    }

    private fun setQRCode(bitmap: Bitmap) {
        b.qr.setImageBitmap(bitmap)
    }

    private fun showProgress(show: Boolean) {
        b.progress.visible = show
    }

    @Suppress("UNUSED_PARAMETER")
    private fun copyAddress(view: View) {
        if (context == null) return
        ContextHelper.copyToClipboardNoAlert(context, address.toString())
        val set = AnimatorInflater.loadAnimator(context, R.animator.fade_in_out) as AnimatorSet
        set.setTarget(b.layoutAddressAlert)
        set.start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidSupportInjection.inject(this)
        super.onCreate(savedInstanceState)
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }
}