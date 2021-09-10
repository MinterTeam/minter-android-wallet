/*
 * Copyright (C) by MinterTeam. 2021
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

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import network.minter.bipwallet.apis.gate.TxInitDataRepository;
import network.minter.bipwallet.internal.di.annotations.DbCache;
import network.minter.bipwallet.internal.storage.AccountStorage;
import network.minter.bipwallet.internal.storage.KVStorage;
import network.minter.bipwallet.internal.storage.SecretStorage;
import network.minter.bipwallet.pools.repo.FarmingRepository;
import network.minter.bipwallet.pools.repo.UserPoolsRepository;
import network.minter.bipwallet.stories.repo.StoriesRepository;
import network.minter.core.internal.api.ApiService;
import network.minter.explorer.MinterExplorerSDK;
import network.minter.explorer.repo.ExplorerAddressRepository;
import network.minter.explorer.repo.ExplorerCoinsRepository;
import network.minter.explorer.repo.ExplorerPoolsRepository;
import network.minter.explorer.repo.ExplorerStatusRepository;
import network.minter.explorer.repo.ExplorerTransactionRepository;
import network.minter.explorer.repo.ExplorerValidatorsRepository;
import network.minter.explorer.repo.GateCoinRepository;
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
    public MinterExplorerSDK provideMinterExplorerSDK() {
        final MinterExplorerSDK api = MinterExplorerSDK.getInstance();
        api.getApiService().setDateFormat("yyyy-MM-dd HH:mm:ssZ");
        api.getApiService().setConnectionTimeout(60).setReadTimeout(60);
        return api;
    }

    @Provides
    @WalletApp
    public ExplorerTransactionRepository provideExplorerTransactionsRepo(MinterExplorerSDK api) {
        return api.transactions();
    }

    @Provides
    @WalletApp
    public ExplorerAddressRepository provideExplorerAddressRepository(MinterExplorerSDK api) {
        return api.address();
    }

    @Provides
    @WalletApp
    public ExplorerCoinsRepository provideExplorerCoinsRepo(MinterExplorerSDK api) {
        return api.coins();
    }

    @Provides
    @WalletApp
    public ExplorerValidatorsRepository provideExplorerValidatorsRepo(MinterExplorerSDK api) {
        return api.validators();
    }

    @Provides
    @WalletApp
    public ExplorerPoolsRepository provideExplorerPoolsRepo(MinterExplorerSDK api) {
        return api.pools();
    }

    @Provides
    @WalletApp
    public ExplorerStatusRepository provideExplorerStatusRepo(MinterExplorerSDK api) {
        return api.status();
    }

    @Provides
    @WalletApp
    public GateGasRepository provideGateGasRepo(MinterExplorerSDK api) {
        return api.gas();
    }

    @Provides
    @WalletApp
    public GateEstimateRepository provideGateEstimateRepo(MinterExplorerSDK api) {
        return api.estimate();
    }

    @Provides
    @WalletApp
    public GateTransactionRepository provideGateTxRepo(MinterExplorerSDK api) {
        return api.transactionsGate();
    }

    @Provides
    @WalletApp
    public GateCoinRepository provideGateCoinRepo(MinterExplorerSDK api) {
        return api.coinsGate();
    }

    @Provides
    @WalletApp
    public TxInitDataRepository provideTxInitDataRepo(GateEstimateRepository estimateRepo, GateGasRepository gasRepo) {
        return new TxInitDataRepository(estimateRepo, gasRepo);
    }

    @Provides
    @WalletApp
    public StoriesRepository provideStoriesRepo(@Named("stories") ApiService.Builder api, @DbCache KVStorage storage, @Named("uuid") String uuid) {
        return new StoriesRepository(api, storage, uuid);
    }

    @Provides
    @WalletApp
    public FarmingRepository provideFarmingRepo(@Named("chainik") ApiService.Builder api, @DbCache KVStorage storage) {
        return new FarmingRepository(api, storage);
    }

    @Provides
    @WalletApp
    public UserPoolsRepository provideUserPools(KVStorage storage, SecretStorage secretStorage, ExplorerPoolsRepository repo) {
        return new UserPoolsRepository(secretStorage, storage, repo);
    }
}
