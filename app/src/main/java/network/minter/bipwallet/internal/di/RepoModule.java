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

package network.minter.bipwallet.internal.di;

import dagger.Module;
import dagger.Provides;
import network.minter.bipwallet.advanced.repo.AccountStorage;
import network.minter.bipwallet.advanced.repo.SecretStorage;
import network.minter.bipwallet.internal.auth.AuthSession;
import network.minter.bipwallet.internal.storage.KVStorage;
import network.minter.blockchainapi.MinterBlockChainApi;
import network.minter.blockchainapi.repo.BlockChainAccountRepository;
import network.minter.blockchainapi.repo.BlockChainCoinRepository;
import network.minter.explorerapi.MinterExplorerApi;
import network.minter.explorerapi.repo.ExplorerAddressRepository;
import network.minter.explorerapi.repo.ExplorerTransactionRepository;
import network.minter.my.MyMinterApi;
import network.minter.my.repo.MyAddressRepository;
import network.minter.my.repo.MyAuthRepository;
import network.minter.my.repo.MyInfoRepository;
import network.minter.my.repo.MyProfileRepository;
import timber.log.Timber;

/**
 * Dogsy. 2017
 *
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
    public ExplorerTransactionRepository provideExplorerTransactionsRepo(MinterExplorerApi api) {
        return api.transactions();
    }

    @Provides
    @WalletApp
    public MinterExplorerApi provideMinterExplorerApi() {
        return MinterExplorerApi.getInstance();
    }

    @Provides
    @WalletApp
    public MyAuthRepository provideAuthRepository(MyMinterApi api) {
        return api.auth();
    }

    @Provides
    @WalletApp
    public MyAddressRepository provideAddressRepository(MyMinterApi api) {
        return api.address();
    }

    @Provides
    @WalletApp
    public ExplorerAddressRepository provideExplorerAddressRepository(MinterExplorerApi api) {
        return api.address();
    }

    @Provides
    @WalletApp
    public MyProfileRepository provideProfileRepository(MyMinterApi api) {
        return api.profile();
    }

    @Provides
    @WalletApp
    public MyMinterApi provideMyMinterApi(AuthSession session) {
        MyMinterApi.getInstance().getApiService().setAuthHeaderName("Authorization");
        MyMinterApi.getInstance().getApiService().setTokenGetter(() -> {
            Timber.d("Getting token for MyMinter: %s", "Bearer " + session.getAuthToken());
            return "Bearer " + session.getAuthToken();
        });
        return MyMinterApi.getInstance();
    }

    @Provides
    @WalletApp
    public BlockChainAccountRepository provideBlockChainAccountRepo() {
        return MinterBlockChainApi.getInstance().account();
    }

    @Provides
    @WalletApp
    public BlockChainCoinRepository provideBlockChainCoinRepo() {
        return MinterBlockChainApi.getInstance().coin();
    }

    @Provides
    @WalletApp
    public MyInfoRepository provideInfoRepository(MyMinterApi api) {
        return api.info();
    }
}
