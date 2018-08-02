/*
 * Copyright (C) by MinterTeam. 2018
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
import network.minter.bipwallet.advanced.repo.AccountStorage;
import network.minter.bipwallet.advanced.repo.SecretStorage;
import network.minter.bipwallet.internal.auth.AuthSession;
import network.minter.bipwallet.internal.storage.KVStorage;
import network.minter.bipwallet.sending.repo.RecipientAutocompleteStorage;
import network.minter.blockchain.MinterBlockChainApi;
import network.minter.blockchain.repo.BlockChainAccountRepository;
import network.minter.blockchain.repo.BlockChainCoinRepository;
import network.minter.explorer.MinterExplorerApi;
import network.minter.explorer.repo.ExplorerAddressRepository;
import network.minter.explorer.repo.ExplorerTransactionRepository;
import network.minter.profile.MinterProfileApi;
import network.minter.profile.repo.ProfileAddressRepository;
import network.minter.profile.repo.ProfileAuthRepository;
import network.minter.profile.repo.ProfileInfoRepository;
import network.minter.profile.repo.ProfileRepository;
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
        final MinterExplorerApi api = MinterExplorerApi.getInstance();
        api.getApiService().setDateFormat("yyyy-MM-dd HH:mm:ssZ");
        return api;
    }

    @Provides
    @WalletApp
    public ProfileAuthRepository provideAuthRepository(MinterProfileApi api) {
        return api.auth();
    }

    @Provides
    @WalletApp
    public ProfileAddressRepository provideAddressRepository(MinterProfileApi api) {
        return api.address();
    }

    @Provides
    @WalletApp
    public ExplorerAddressRepository provideExplorerAddressRepository(MinterExplorerApi api) {
        return api.address();
    }

    @Provides
    @WalletApp
    public ProfileRepository provideProfileRepository(MinterProfileApi api) {
        return api.profile();
    }

    @Provides
    @WalletApp
    public MinterProfileApi provideMinterProfileApi(AuthSession session) {
        MinterProfileApi.getInstance().getApiService().setAuthHeaderName("Authorization");
        MinterProfileApi.getInstance().getApiService().setTokenGetter(() -> {
            Timber.d("Getting token for MyMinter: %s", "Bearer " + session.getAuthToken());
            return "Bearer " + session.getAuthToken();
        });
        MinterProfileApi.getInstance().getApiService().setDateFormat("yyyy-MM-dd HH:mm:ssZ");
        return MinterProfileApi.getInstance();
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
    public RecipientAutocompleteStorage provideRecipientStorage(KVStorage storage) {
        return new RecipientAutocompleteStorage(storage);
    }

    @Provides
    @WalletApp
    public ProfileInfoRepository provideInfoRepository(MinterProfileApi api) {
        return api.info();
    }
}
