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

package network.minter.bipwallet.advanced.views;

import android.content.Context;
import android.hardware.usb.UsbManager;
import android.view.View;

import javax.inject.Inject;

import io.reactivex.disposables.Disposable;
import moxy.InjectViewState;
import network.minter.bipwallet.R;
import network.minter.bipwallet.advanced.contract.LedgerActivateView;
import network.minter.bipwallet.advanced.ui.AdvancedLedgerActivity;
import network.minter.bipwallet.internal.Wallet;
import network.minter.bipwallet.internal.auth.AuthSession;
import network.minter.bipwallet.internal.mvp.MvpBasePresenter;
import network.minter.bipwallet.internal.storage.SecretStorage;
import network.minter.bipwallet.internal.storage.models.SecretData;
import network.minter.core.crypto.MinterAddress;
import network.minter.ledger.connector.LedgerNanoS;
import network.minter.ledger.connector.MinterLedger;
import network.minter.ledger.connector.rxjava2.RxMinterLedger;

@InjectViewState
public class AdvancedLedgerPresenter extends MvpBasePresenter<LedgerActivateView> implements LedgerNanoS.DeviceListener {

    @Inject SecretStorage secretStorage;
    @Inject AuthSession session;
    @Inject AdvancedLedgerActivity context;
    @Inject RxMinterLedger ledger;
    private Disposable mActionDisp;
    private MinterAddress mAddress;

    @Inject
    public AdvancedLedgerPresenter() {
    }

    @Override
    public void attachView(LedgerActivateView view) {
        super.attachView(view);
        getViewState().showAddress(false);
        getViewState().setEnableLaunch(false);
        getViewState().setEnableSwitch(false);
        getViewState().setOnSwitchedConfirm((buttonView, isChecked) -> getViewState().setEnableLaunch(isChecked));
        getViewState().setOnActionClick(this::onClickSubmit);

        initDevice();
    }

    @Override
    public void detachView(LedgerActivateView view) {
        super.detachView(view);
        if (mActionDisp != null) {
            mActionDisp.dispose();
            mActionDisp = null;
        }

        ledger.disconnect();
    }

    public void initDevice() {
        ledger = new RxMinterLedger(context, (UsbManager) context.getSystemService(Context.USB_SERVICE));
        ledger.setDeviceListener(this);

        ledger.init();
        if (ledger.isReady()) {
            onDeviceReady();
        }
    }

    @Override
    public void onDeviceReady() {
        getViewState().setSecuredByValue(ledger.getDevice().getManufacturerName() + " " + ledger.getDevice().getProductName());
        getViewState().setEnableSwitch(true);
        mActionDisp = ledger.getAddress()
                .subscribe(res -> {
                    mAddress = res;
                    getViewState().setAddress(mAddress.toString());
                    getViewState().showAddress(true);
                    mActionDisp = null;
                    ledger.destroy();
                }, this::onDeviceResponseError);

    }

    @Override
    public void onDisconnected() {
        if (mAddress != null) return;
        getViewState().setEnableSwitch(false);
        getViewState().setSecuredByValue(Wallet.app().res().getString(R.string.dots));
        getViewState().setProgressText("Device disconnected. Searching...");
        getViewState().showProgress(true);
        getViewState().showAddress(false);
        ledger.init();
    }

    @Override
    public void onError(int code, Throwable t) {
        if (code == MinterLedger.CODE_PERMISSION_DENIED && mAddress == null) {

        }
    }

    private void onClickSubmit(View view) {
        SecretData sd = new SecretData(mAddress);
        secretStorage.add(sd);

        session.login(
                AuthSession.AUTH_TOKEN_ADVANCED,
                AuthSession.AuthType.Hardware
        );

        getViewState().startHome();
    }

    private void onDeviceResponseError(Throwable throwable) {

    }
}
