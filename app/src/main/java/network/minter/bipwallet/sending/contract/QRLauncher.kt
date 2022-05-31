/*
 * Copyright (C) by MinterTeam. 2022
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

package network.minter.bipwallet.sending.contract

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import network.minter.bipwallet.BuildConfig
import network.minter.bipwallet.R
import network.minter.bipwallet.internal.dialogs.ConfirmDialog

class QRLauncher(
        resultCaller: ActivityResultCaller,
        private val getContext: ()->Activity,
        private val onResult: (String?)->Unit
) {
    private var launchQRScannerDeniedDialog: Dialog? = null
    private var launchQRScannerRationaleDialog: Dialog? = null
    private val launchQRScanner = resultCaller.registerForActivityResult(GetQRResultString()) {
        onResult(it)
    }
    private val launchQRScannerPermissions = resultCaller.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            launchQRScanner.launch(Unit)
        } else {
            launchQRScannerDeniedDialog = ConfirmDialog.Builder(getContext(), R.string.dialog_title_camera_permission)
                    .setText(R.string.dialog_text_camera_permission)
                    .setPositiveAction(R.string.btn_open_settings) { d: DialogInterface, _: Int ->
                        val intent = Intent()
                        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        val uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                        intent.data = uri
                        getContext().startActivity(intent)
                        d.dismiss()
                    }
                    .setNegativeAction(R.string.btn_cancel) { d: DialogInterface, _: Int -> d.dismiss() }
                    .create()
                    .also { it.show() }
        }
    }

    fun release() {
        launchQRScannerDeniedDialog?.dismiss().also { launchQRScannerDeniedDialog = null }
        launchQRScannerRationaleDialog?.dismiss().also { launchQRScannerRationaleDialog = null }
    }

    fun launch() {
        val permission = Manifest.permission.CAMERA
        when {
            ContextCompat.checkSelfPermission(getContext(), permission) == PackageManager.PERMISSION_GRANTED -> {
                launchQRScanner.launch(Unit)
            }
            ActivityCompat.shouldShowRequestPermissionRationale(getContext(), permission) -> {
                launchQRScannerRationaleDialog = ConfirmDialog.Builder(getContext(), R.string.dialog_title_camera_permission)
                        .setText(R.string.dialog_text_camera_permission)
                        .setPositiveAction(R.string.btn_ok) { d, _ ->
                            launchQRScannerPermissions.launch(permission)
                            d.dismiss()
                        }
                        .setNegativeAction(R.string.btn_cancel) { d, _ -> d.dismiss() }
                        .create()
                        .also { it.show() }
            }
            else -> {
                launchQRScannerPermissions.launch(permission)
            }
        }
    }


}