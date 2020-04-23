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

package network.minter.bipwallet.internal.di;

import dagger.Module;
import dagger.Provides;
import network.minter.bipwallet.apis.gate.TxInitDataRepository;
import network.minter.bipwallet.internal.storage.AccountStorage;
import network.minter.bipwallet.internal.storage.KVStorage;
import network.minter.bipwallet.internal.storage.SecretStorage;
import network.minter.explorer.MinterExplorerApi;
import network.minter.explorer.repo.ExplorerAddressRepository;
import network.minter.explorer.repo.ExplorerCoinsRepository;
import network.minter.explorer.repo.ExplorerTransactionRepository;
import network.minter.explorer.repo.ExplorerValidatorsRepository;
import network.minter.explorer.repo.GateEstimateRepository;
import network.minter.explorer.repo.GateGasRepository;
import network.minter.explorer.repo.GateTransactionRepository;

/**
 * minter-android-wallet. 2017
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@Module
public class RepoModule {

    @Provides
    @WalletApp
    public SecretStorage provideSecretRepository(KVStorage storage) {
        return new SecretStorage(storage);
    }

    @Provides
    @WalletApp
    public AccountStorage provideAccountStorage(KVStorage storage, SecretStorage secretStorage, ExplorerAddressRepository addressRepository) {
        return new AccountStorage(storage, secretStorage, addressRepository);
    }

    @Provides
    @WalletApp
    public MinterExplorerApi provideMinterExplorerApi() {
        final MinterExplorerApi api = MinterExplorerApi.getInstance();
        api.getApiService().setDateFormat("yyyy-MM-dd HH:mm:ssZ");
        api.getApiService().setConnectionTimeout(60).setReadTimeout(60);
        return api;
    }

    @Provides
    @WalletApp
    public ExplorerTransactionRepository provideExplorerTransactionsRepo(MinterExplorerApi api) {
        return api.transactions();
    }

    @Provides
    @WalletApp
    public ExplorerAddressRepository provideExplorerAddressRepository(MinterExplorerApi api) {
        return api.address();
    }

    @Provides
    @WalletApp
    public ExplorerCoinsRepository provideExplorerCoinsRepo(MinterExplorerApi api) {
        return api.coins();
    }

    @Provides
    @WalletApp
    public ExplorerValidatorsRepository provideExplorerValidatorsRepo(MinterExplorerApi api) {
        return api.validators();
    }

    @Provides
    @WalletApp
    public GateGasRepository provideGateGasRepo(MinterExplorerApi api) {
        return api.gas();
    }

    @Provides
    @WalletApp
    public GateEstimateRepository provideGateEstimateRepo(MinterExplorerApi api) {
        return api.estimate();
    }

    @Provides
    @WalletApp
    public GateTransactionRepository provideGateTxRepo(MinterExplorerApi api) {
        return api.transactionsGate();
    }

    @Provides
    @WalletApp
    public TxInitDataRepository provideTxInitDataRepo(GateEstimateRepository estimateRepo, GateGasRepository gasRepo) {
        return new TxInitDataRepository(estimateRepo, gasRepo);
    }
}
