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

package network.minter.bipwallet.wallets.views;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import javax.inject.Inject;

import moxy.InjectViewState;
import network.minter.bipwallet.R;
import network.minter.bipwallet.advanced.models.AddressListBalancesTotal;
import network.minter.bipwallet.advanced.repo.AccountStorage;
import network.minter.bipwallet.advanced.repo.SecretStorage;
import network.minter.bipwallet.apis.explorer.CacheTxRepository;
import network.minter.bipwallet.apis.explorer.CachedRewardStatisticsRepository;
import network.minter.bipwallet.internal.data.CachedRepository;
import network.minter.bipwallet.internal.dialogs.WalletConfirmDialog;
import network.minter.bipwallet.internal.mvp.MvpBasePresenter;
import network.minter.bipwallet.sending.ui.QRCodeScannerActivity;
import network.minter.bipwallet.wallets.contract.WalletsTabView;
import network.minter.bipwallet.wallets.data.BalanceCurrentState;
import network.minter.bipwallet.wallets.selector.WalletItem;
import network.minter.bipwallet.wallets.ui.WalletsTabFragment;
import network.minter.core.MinterSDK;
import network.minter.core.crypto.MinterAddress;
import network.minter.core.crypto.MinterPublicKey;
import network.minter.core.internal.helpers.StringHelper;
import network.minter.explorer.models.HistoryTransaction;
import network.minter.explorer.models.RewardStatistics;
import network.minter.explorer.repo.ExplorerAddressRepository;
import timber.log.Timber;

import static network.minter.bipwallet.internal.helpers.MathHelper.bdHuman;
import static network.minter.bipwallet.internal.helpers.MathHelper.bdIntHuman;
import static network.minter.bipwallet.internal.helpers.Plurals.bips;
import static network.minter.bipwallet.internal.helpers.Plurals.usd;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@InjectViewState
public class WalletsTabPresenter extends MvpBasePresenter<WalletsTabView> {

    private final BalanceCurrentState mBalanceCurrentState = new BalanceCurrentState();
    @Inject CachedRepository<AddressListBalancesTotal, AccountStorage> accountStorage;
    @Inject CachedRepository<RewardStatistics, CachedRewardStatisticsRepository> rewardsRepo;
    @Inject ExplorerAddressRepository addressRepo;
    @Inject SecretStorage secretStorage;
    @Inject CachedRepository<List<HistoryTransaction>, CacheTxRepository> txRepo;

    @Inject
    public WalletsTabPresenter() {
    }

    @Override
    public void attachView(WalletsTabView view) {
        super.attachView(view);
        getViewState().setMainWallet(WalletItem.create(secretStorage, accountStorage.getEntity().getMainWallet()));
        getViewState().setOnClickWalletListener(this::onWalletSelect);
        getViewState().setOnClickAddWalletListener(this::onWalletAdd);
        getViewState().setOnClickEditWalletListener(this::onWalletEdit);
        getViewState().setOnClickDelegated(this::onClickStartDelegationList);

//        getViewState().setOnRefreshListener(this::onRefresh);

        mBalanceCurrentState.applyTo(getViewState());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mBalanceCurrentState.onSaveInstanceState(outState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mBalanceCurrentState.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == WalletsTabFragment.REQUEST_CODE_QR_SCAN_TX) {
            if (data == null) {
                Timber.w("Something wrong on activity result: req(%d), res(%d), data(null)", requestCode, resultCode);
                return;
            }

            String result = data.getStringExtra(QRCodeScannerActivity.RESULT_TEXT);
            if (result != null) {

                boolean isMxAddress = result.matches(MinterAddress.ADDRESS_PATTERN);
                boolean isMpAddress = result.matches(MinterPublicKey.PUB_KEY_PATTERN);
                if (isMxAddress || isMpAddress) {
                    getViewState().showSendAndSetAddress(result);
                    return;
                }

                try {
                    getViewState().startExternalTransaction(result);
                } catch (Throwable t) {
                    Timber.w(t, "Unable to parse remote transaction: %s", result);
                    getViewState().startDialog(ctx -> new WalletConfirmDialog.Builder(ctx, "Unable to scan QR")
                            .setText("Invalid transaction data: %s", t.getMessage())
                            .setPositiveAction(R.string.btn_close)
                            .create());
                }
            }
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    protected void onFirstViewAttach() {
        super.onFirstViewAttach();
        safeSubscribeIoToUi(rewardsRepo.observe())
                .subscribe(res -> {
                    getViewState().setBalanceRewards(
                            String.format("+ %s %s today", bdHuman(res.amount), MinterSDK.DEFAULT_COIN)
                    );
                }, Timber::w);

        safeSubscribeIoToUi(accountStorage.observe())
                .subscribe(res -> {
                    Timber.d("Update coins list");
                    final StringHelper.DecimalStringFraction availableBIP, totalBIP, totalUSD;

                    getViewState().setWallets(WalletItem.create(secretStorage, res));
                    getViewState().setMainWallet(WalletItem.create(secretStorage, res.getBalance(secretStorage.getMainWallet())));


                    availableBIP = StringHelper.splitDecimalStringFractions(accountStorage.getEntity().getMainWallet().getAvailableBalanceBIP().setScale(4, RoundingMode.DOWN));
                    totalBIP = StringHelper.splitDecimalStringFractions(accountStorage.getEntity().getMainWallet().getTotalBalance().setScale(4, RoundingMode.DOWN));
                    totalUSD = StringHelper.splitDecimalStringFractions(accountStorage.getEntity().getMainWallet().getTotalBalanceUSD().setScale(2, RoundingMode.DOWN));

                    mBalanceCurrentState.setAvailableBIP(bdIntHuman(availableBIP.intPart), availableBIP.fractionalPart, bips(Long.parseLong(availableBIP.intPart)));
                    mBalanceCurrentState.setTotalBIP(bdIntHuman(totalBIP.intPart), totalBIP.fractionalPart, bips(Long.parseLong(totalBIP.intPart)));
                    mBalanceCurrentState.setTotalUSD(usd(bdIntHuman(totalUSD.intPart)), totalUSD.fractionalPart);

                    mBalanceCurrentState.applyTo(getViewState());

                    final BigDecimal delegated = res.find(secretStorage.getMainWallet()).get().delegated;
                    getViewState().setDelegationAmount(String.format("%s %s", bdHuman(delegated), MinterSDK.DEFAULT_COIN));
                }, Timber::w);

        rewardsRepo.update();
        updateDelegation();
        accountStorage.update();
    }

    private void onWalletSelect(WalletItem walletItem) {
        secretStorage.setMain(walletItem.getAddress());
        getViewState().setMainWallet(walletItem);
        forceUpdate();
    }

    private void onWalletEdit(WalletItem walletItem) {
        getViewState().startWalletEdit(walletItem, this::onWalletUpdated);
    }

    private void onWalletUpdated() {
        forceUpdate();
    }

    private void forceUpdate() {
        accountStorage.update(true);
        rewardsRepo.update(true);
        txRepo.update(true);
        updateDelegation();
    }

    private void onWalletAdd() {
        getViewState().startWalletAdd(this::onAddedWallet, null);
    }

    private void onAddedWallet() {
        forceUpdate();
    }

    private void updateDelegation() {
//        safeSubscribeIoToUi(rxExpGate(addressRepo.getDelegations(secretStorage.getMainWallet(), 0))
//                .subscribeOn(Schedulers.io())).subscribe(
//                res -> {
//                    getViewState().setDelegationAmount(String.format("%s %s", bdHuman(res.meta.additional.delegatedAmount), MinterSDK.DEFAULT_COIN));
//
////                    getViewState().setWallets();
//                },
//                Timber::d);
    }

    private void onRefresh() {
        accountStorage.update(true);
        rewardsRepo.update(true);
//        updateDelegation();
    }

    private void onClickStartDelegationList(View view) {
        getViewState().startDelegationList();
    }

}
