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
package network.minter.bipwallet.receiving.ui

import android.app.Activity
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.transition.AutoTransition
import android.widget.ImageView
import androidx.fragment.app.Fragment
import coil.api.load
import network.minter.bipwallet.R
import network.minter.bipwallet.internal.BaseActivity
import network.minter.bipwallet.internal.Wallet
import network.minter.bipwallet.internal.system.ActivityBuilder
import timber.log.Timber
import java.io.File

/**
 * minter-android-wallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
class QRPreviewActivity : BaseActivity() {
    override fun onBackPressed() {
        supportFinishAfterTransition()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportPostponeEnterTransition()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.enterTransition = null
            window.exitTransition = null
            window.sharedElementEnterTransition = AutoTransition()
        }
        setContentView(R.layout.activity_qr_preview)
        val iv = findViewById<ImageView>(R.id.iv)
        iv.setOnClickListener { onBackPressed() }

        iv.load(File(intent.getStringExtra(EXTRA_FILE_PATH)!!)) {
            size(Wallet.app().display().width, Wallet.app().display().width)
            listener(
                    onSuccess = { _, _ ->
                        supportStartPostponedEnterTransition()
                    },
                    onError = { _, _ ->
                        Timber.e("Unable to load image")
                        supportPostponeEnterTransition()
                    })
        }
    }

    class Builder : ActivityBuilder {
        private var mFilePath: String

        constructor(from: Activity, filePath: String) : super(from) {
            mFilePath = filePath
        }

        constructor(from: Fragment, filePath: String) : super(from) {
            mFilePath = filePath
        }

        constructor(from: Service, filePath: String) : super(from) {
            mFilePath = filePath
        }

        override fun onBeforeStart(intent: Intent) {
            super.onBeforeStart(intent)
            intent.putExtra(EXTRA_FILE_PATH, mFilePath)
        }

        override fun getActivityClass(): Class<*> {
            return QRPreviewActivity::class.java
        }
    }

    companion object {
        private const val EXTRA_FILE_PATH = "EXTRA_FILE_PATH"
    }
}