/*
 * Copyright (C) 2018 by MinterTeam
 * @link https://github.com/MinterTeam
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

package network.minter.bipwallet.receiving.helpers;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Collections;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import network.minter.mintercore.crypto.MinterAddress;
import timber.log.Timber;

import static network.minter.bipwallet.internal.Wallet.app;

/**
 * MinterWallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class QRAddressGenerator implements ObservableOnSubscribe<QRAddressGenerator.Result> {

    private int mWidthHeight;
    private String mAddress;

    private QRAddressGenerator(int widthHeight, String address) {
        mWidthHeight = widthHeight;
        mAddress = address;
    }

    public static Observable<Result> create(int widthHeight, MinterAddress address) {
        return Observable.create(new QRAddressGenerator(widthHeight, address.toString()));
    }

    public static Observable<Result> create(int widthHeight, String address) {
        return Observable.create(new QRAddressGenerator(widthHeight, address));
    }

    @Override
    public void subscribe(ObservableEmitter<Result> emitter) throws Exception {
        QRCodeWriter writer = new QRCodeWriter();

        final Result out = new Result();
        out.file = new File(app().context().getFilesDir(), "/public/addresses/qr/");
        if (!out.file.exists()) {
            out.file.mkdirs();
        }
        out.file = new File(out.file, mAddress + ".png");
        if (out.file.exists()) {
//            out.file.delete();
            Timber.d("Get existed qr");
            out.bitmap = BitmapFactory.decodeFile(out.file.getPath());
            emitter.onNext(out);
            emitter.onComplete();
            return;
        }
        Timber.d("Creating new qr");

        out.file.createNewFile();

        try {
            BitMatrix bitMatrix = writer.encode(mAddress, BarcodeFormat.QR_CODE, mWidthHeight, mWidthHeight, Collections.singletonMap(EncodeHintType.MARGIN, "0"));
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bmp.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }

            out.bitmap = bmp;


            try (FileOutputStream outputStream = new FileOutputStream(out.file)) {
                bmp.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            } catch (Exception e) {
                Timber.e(e, "Unable to save qr image to file " + out.file.getAbsolutePath());
                emitter.onError(e);
                return;
            }


            emitter.onNext(out);
            emitter.onComplete();

        } catch (WriterException e) {
            emitter.onError(e);
        }
    }

    public static final class Result {
        public Bitmap bitmap;
        public File file;
    }
}
