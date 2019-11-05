/*
 * Copyright (C) by MinterTeam. 2019
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
package network.minter.bipwallet.exchange.views;

import java.util.List;

import javax.inject.Inject;

import moxy.InjectViewState;
import network.minter.bipwallet.advanced.models.UserAccount;
import network.minter.bipwallet.advanced.repo.AccountStorage;
import network.minter.bipwallet.advanced.repo.SecretStorage;
import network.minter.bipwallet.apis.explorer.CacheTxRepository;
import network.minter.bipwallet.exchange.contract.SpendCoinTabView;
import network.minter.bipwallet.internal.data.CachedRepository;
import network.minter.bipwallet.internal.system.testing.IdlingManager;
import network.minter.blockchain.models.operational.OperationType;
import network.minter.core.MinterSDK;
import network.minter.explorer.models.HistoryTransaction;
import network.minter.explorer.repo.ExplorerCoinsRepository;
import network.minter.explorer.repo.GateEstimateRepository;
import network.minter.explorer.repo.GateGasRepository;
import network.minter.explorer.repo.GateTransactionRepository;

import static network.minter.bipwallet.internal.helpers.MathHelper.bdHuman;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@InjectViewState
public class SpendCoinTabPresenter extends BaseCoinTabPresenter<SpendCoinTabView> {

    @Inject
    public SpendCoinTabPresenter(
            SecretStorage secretStorage,
            CachedRepository<UserAccount, AccountStorage> accountStorage,
            CachedRepository<List<HistoryTransaction>, CacheTxRepository> txRepo,
            ExplorerCoinsRepository explorerCoinsRepository,
            IdlingManager idlingManager,
            GateGasRepository gasRepo,
            GateEstimateRepository estimateRepository,
            GateTransactionRepository gateTransactionRepository) {
        super(secretStorage, accountStorage, txRepo, explorerCoinsRepository, idlingManager, gasRepo, estimateRepository, gateTransactionRepository);
    }

    @Override
    protected boolean isAmountForGetting() {
        return false;
    }

    @Override
    protected void onFirstViewAttach() {
        super.onFirstViewAttach();
        getViewState().setFee(String.format("%s %s", bdHuman(OperationType.SellCoin.getFee()), MinterSDK.DEFAULT_COIN.toUpperCase()));
    }
}
