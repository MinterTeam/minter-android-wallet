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

package network.minter.bipwallet.receiving.views;

import android.content.Intent;
import android.view.View;

import java.io.File;

import javax.inject.Inject;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import moxy.InjectViewState;
import network.minter.bipwallet.analytics.AppEvent;
import network.minter.bipwallet.internal.Wallet;
import network.minter.bipwallet.internal.helpers.ContextHelper;
import network.minter.bipwallet.internal.helpers.TextHelper;
import network.minter.bipwallet.internal.mvp.MvpBasePresenter;
import network.minter.bipwallet.internal.storage.SecretStorage;
import network.minter.bipwallet.receiving.contract.ReceiveTabView;
import network.minter.bipwallet.receiving.helpers.QRAddressGenerator;
import network.minter.bipwallet.share.ShareManager;
import network.minter.core.crypto.MinterAddress;
import timber.log.Timber;

/**
 * minter-android-wallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@InjectViewState
public class ReceiveTabPresenter extends MvpBasePresenter<ReceiveTabView> {

    @Inject SecretStorage secretStorage;
    private MinterAddress mAddress;
    private File mOutFile;

    @Inject
    public ReceiveTabPresenter() {

    }

    @Override
    public void attachView(ReceiveTabView view) {
        super.attachView(view);
        if (secretStorage.getAddresses().isEmpty()) {
            Timber.w("No one address stored. How this happened?");
            return;
        }
        mAddress = secretStorage.getAddresses().get(0);
        getViewState().setAddress(mAddress);
        getViewState().setOnClickAddress(this::onClickAddress);

        // 38% of screen is qr code image
        float qrWidth = ((float) Wallet.app().display().getWidth()) * 0.388f;

        getViewState().showQRProgress();
        QRAddressGenerator.create((int) qrWidth, mAddress.toString())
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(res -> {
                    if (res.bitmap == null) {
                        Timber.e("Unable to create QR: Unknown reason.");
                        return;
                    }
                    Timber.d("Bitmap size: %s", TextHelper.humanReadableBytes((res.bitmap.getWidth() * res.bitmap.getHeight()) * 8, true));
                    mOutFile = res.file;
                    getViewState().hideQRProgress();
                    getViewState().setQRCode(res.bitmap);
                    getViewState().setOnActionShareQR(ReceiveTabPresenter.this::onCopyQR);
                    getViewState().setOnActionQR(v -> getViewState().startQRPreview(v, mOutFile.toString()));
                }, t -> {
                    getViewState().onError(t);
                    getViewState().hideQRProgress();
                });
    }

    private void onClickAddress(View view) {
        ContextHelper.copyToClipboard(view.getContext(), mAddress.toString());
    }

    private void onCopyQR(View view) {
        getAnalytics().send(AppEvent.ReceiveShareButton);
//        Uri qrPath = getUriForFile(app().context(), app().context().getApplicationContext().getPackageName() + ".file.provider", mOutFile);

        final Intent intent = new ShareManager.IntentBuilder()
                .setChooserTitle("Share address")
//                .setStream(qrPath)
                .setText(mAddress.toString())
                .setContentType("text/plain")
                .build();

        getViewState().startShare(intent);
    }
}
