/*
 * Copyright (C) by MinterTeam. 2018
 * @link https://github.com/MinterTeam
 * @link https://github.com/edwardstock
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

import com.arellomobile.mvp.InjectViewState;

import java.util.List;

import javax.inject.Inject;

import network.minter.bipwallet.advanced.models.UserAccount;
import network.minter.bipwallet.advanced.repo.AccountStorage;
import network.minter.bipwallet.advanced.repo.SecretStorage;
import network.minter.bipwallet.apis.explorer.CachedExplorerTransactionRepository;
import network.minter.bipwallet.exchange.ExchangeModule.SpendCoinTabView;
import network.minter.bipwallet.internal.data.CachedRepository;
import network.minter.blockchain.repo.BlockChainAccountRepository;
import network.minter.blockchain.repo.BlockChainCoinRepository;
import network.minter.explorer.models.HistoryTransaction;

/**
 * minter-android-wallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@InjectViewState
public class SpendCoinTabPresenter extends BaseCoinTabPresenter<SpendCoinTabView> {

    @Inject
    public SpendCoinTabPresenter(
            SecretStorage secretStorage,
            CachedRepository<UserAccount, AccountStorage> accountStorage,
            CachedRepository<List<HistoryTransaction>, CachedExplorerTransactionRepository> txRepo,
            BlockChainCoinRepository coinRepo,
            BlockChainAccountRepository accountRepo
    ) {
        super(secretStorage, accountStorage, txRepo, coinRepo, accountRepo);

    }

    @Override
    protected boolean isAmountForGetting() {
        return false;
    }
}
