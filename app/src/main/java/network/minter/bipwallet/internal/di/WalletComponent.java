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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;

import com.google.gson.GsonBuilder;

import java.util.List;

import javax.inject.Named;

import dagger.Component;
import network.minter.bipwallet.advanced.models.UserAccount;
import network.minter.bipwallet.advanced.repo.AccountStorage;
import network.minter.bipwallet.advanced.repo.SecretStorage;
import network.minter.bipwallet.apis.explorer.CachedExplorerTransactionRepository;
import network.minter.bipwallet.internal.Wallet;
import network.minter.bipwallet.internal.auth.AuthSession;
import network.minter.bipwallet.internal.auth.SessionStorage;
import network.minter.bipwallet.internal.data.CacheManager;
import network.minter.bipwallet.internal.data.CachedRepository;
import network.minter.bipwallet.internal.helpers.DisplayHelper;
import network.minter.bipwallet.internal.helpers.ImageHelper;
import network.minter.bipwallet.internal.helpers.NetworkHelper;
import network.minter.bipwallet.internal.storage.KVStorage;
import network.minter.blockchainapi.repo.BlockChainAccountRepository;
import network.minter.blockchainapi.repo.BlockChainCoinRepository;
import network.minter.explorerapi.models.HistoryTransaction;
import network.minter.explorerapi.repo.ExplorerAddressRepository;
import network.minter.explorerapi.repo.ExplorerTransactionRepository;
import network.minter.mintercore.internal.api.ApiService;
import network.minter.my.repo.MyAddressRepository;
import network.minter.my.repo.MyAuthRepository;
import network.minter.my.repo.MyInfoRepository;
import network.minter.my.repo.MyProfileRepository;

/**
 * MinterWallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@Component(modules = {
        WalletModule.class,
        HelpersModule.class,
        RepoModule.class,
        InjectorsModule.class,
        CacheModule.class,
})
@WalletApp
public interface WalletComponent {

    void inject(Wallet app);

    // app
    Context context();
    Resources res();

    ApiService.Builder apiBuilder();
    AuthSession session();
    SessionStorage sessionStorage();
    KVStorage storage();

    @Named("uuid")
    String uuid();

    // helpers
    DisplayHelper display();
    NetworkHelper network();
    ImageHelper image();
    SharedPreferences prefs();
    GsonBuilder gsonBuilder();
    CacheManager cache();

    // repositories
    SecretStorage secretStorage();
    AccountStorage accountStorage();
    CachedRepository<UserAccount, AccountStorage> accountStorageCache();
    ExplorerTransactionRepository explorerTransactionsRepo();
    CachedRepository<List<HistoryTransaction>, CachedExplorerTransactionRepository> explorerTransactionsRepoCache();
    MyAuthRepository authRepo();
    MyInfoRepository infoRepo();
    MyAddressRepository addressMyRepo();
    ExplorerAddressRepository addressExplorerRepo();

    MyProfileRepository profileRepo();
    BlockChainAccountRepository accountRepoBlockChain();
    BlockChainCoinRepository coinRepoBlockChain();
}
