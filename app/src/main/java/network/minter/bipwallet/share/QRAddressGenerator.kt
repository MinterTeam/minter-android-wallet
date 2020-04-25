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

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.ObservableOnSubscribe
import network.minter.bipwallet.internal.Wallet
import network.minter.core.crypto.MinterAddress
import java.io.File
import java.io.FileOutputStream
import java.util.*

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
class QRAddressGenerator private constructor(private val size: Int, private val address: String) : ObservableOnSubscribe<QRAddressGenerator.Result> {

    @Throws(Exception::class)
    override fun subscribe(emitter: ObservableEmitter<Result>) {
        val writer = QRCodeWriter()

        val root = File(Wallet.app().context().filesDir, "/public/addresses/qr/")
        if (!root.exists()) {
            if (!root.mkdirs()) {
                emitter.onError(IllegalStateException("Unable to create QR root directory"))
                return
            }
        }

        val out = Result(File(root, "$address.png"),
                File(root, "$address.jpg"),
                null
        )

        if (out.filesExist()) {
            out.loadBitmap()
            emitter.onNext(out)
            emitter.onComplete()
            return
        }

        out.createNewFiles()

        try {
            val bitMatrix = writer.encode(address, BarcodeFormat.QR_CODE, size, size, Collections.singletonMap(EncodeHintType.MARGIN, "0"))
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bmp.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            out.bitmap = bmp
            out.bitmap!!.saveAs(out.filePNG, Bitmap.CompressFormat.PNG)
            out.bitmap!!.saveAs(out.fileJPG, Bitmap.CompressFormat.JPEG, 85)

            emitter.onNext(out)
            emitter.onComplete()
        } catch (e: WriterException) {
            emitter.onError(e)
        } catch (t: Throwable) {
            emitter.onError(t)
        }
    }

    internal fun Bitmap.saveAs(file: File, format: Bitmap.CompressFormat, quality: Int = 100) {
        FileOutputStream(file).use {
            compress(format, quality, it)
        }
    }

    data class Result(
            val filePNG: File,
            val fileJPG: File,
            var bitmap: Bitmap? = null
    ) {
        fun filesExist(): Boolean {
            return filePNG.exists() && fileJPG.exists()
        }

        fun bitmapSize(): Long {
            return bitmap!!.width * bitmap!!.height * 8.toLong()
        }

        fun createNewFiles() {
            if (!filePNG.createNewFile()) {
                throw IllegalStateException("Can't create png image file")
            }
            if (!fileJPG.createNewFile()) {
                throw IllegalStateException("Can't create jpg image file")
            }
        }

        fun loadBitmap() {
            bitmap = BitmapFactory.decodeFile(filePNG.path)
        }
    }

    companion object {
        fun create(widthHeight: Int, address: MinterAddress): Observable<Result> {
            return Observable.create(QRAddressGenerator(widthHeight, address.toString()))
        }

        fun create(widthHeight: Int, address: String): Observable<Result> {
            return Observable.create(QRAddressGenerator(widthHeight, address))
        }
    }

}