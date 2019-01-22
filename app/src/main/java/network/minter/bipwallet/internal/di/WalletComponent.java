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
import network.minter.bipwallet.analytics.AnalyticsManager;
import network.minter.bipwallet.apis.explorer.CachedExplorerTransactionRepository;
import network.minter.bipwallet.internal.Wallet;
import network.minter.bipwallet.internal.auth.AuthSession;
import network.minter.bipwallet.internal.data.CacheManager;
import network.minter.bipwallet.internal.data.CachedRepository;
import network.minter.bipwallet.internal.helpers.DisplayHelper;
import network.minter.bipwallet.internal.helpers.ImageHelper;
import network.minter.bipwallet.internal.helpers.NetworkHelper;
import network.minter.bipwallet.internal.helpers.SoundManager;
import network.minter.bipwallet.internal.storage.KVStorage;
import network.minter.bipwallet.internal.system.testing.IdlingManager;
import network.minter.bipwallet.sending.repo.RecipientAutocompleteStorage;
import network.minter.bipwallet.services.livebalance.notification.BalanceNotificationManager;
import network.minter.bipwallet.settings.repo.CachedMyProfileRepository;
import network.minter.blockchain.repo.BlockChainAccountRepository;
import network.minter.blockchain.repo.BlockChainBlockRepository;
import network.minter.blockchain.repo.BlockChainCoinRepository;
import network.minter.blockchain.repo.BlockChainTransactionRepository;
import network.minter.core.internal.api.ApiService;
import network.minter.explorer.models.HistoryTransaction;
import network.minter.explorer.repo.ExplorerAddressRepository;
import network.minter.explorer.repo.ExplorerCoinsRepository;
import network.minter.explorer.repo.ExplorerSettingsRepository;
import network.minter.explorer.repo.ExplorerTransactionRepository;
import network.minter.profile.models.User;
import network.minter.profile.repo.ProfileAddressRepository;
import network.minter.profile.repo.ProfileAuthRepository;
import network.minter.profile.repo.ProfileInfoRepository;
import network.minter.profile.repo.ProfileRepository;

/**
 * minter-android-wallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@Component(modules = {
        WalletModule.class,
        HelpersModule.class,
        RepoModule.class,
        InjectorsModule.class,
        CacheModule.class,
        NotificationModule.class,
        AnalyticsModule.class,
})
@WalletApp
public interface WalletComponent {

    void inject(Wallet app);

    // app
    Context context();
    Resources res();

    ApiService.Builder apiBuilder();
    AuthSession session();
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
    AnalyticsManager analytics();
    SoundManager sounds();

    // notification
    BalanceNotificationManager balanceNotifications();

    // repositories
    // local
    SecretStorage secretStorage();
    AccountStorage accountStorage();
    RecipientAutocompleteStorage recipientStorage();
    CachedRepository<UserAccount, AccountStorage> accountStorageCache();
    CachedRepository<List<HistoryTransaction>, CachedExplorerTransactionRepository> explorerTransactionsRepoCache();
    CachedRepository<User.Data, CachedMyProfileRepository> profileCachedRepo();
    // profile
    ProfileAuthRepository authRepo();
    ProfileInfoRepository infoRepo();
    ProfileAddressRepository addressMyRepo();
    ProfileRepository profileRepo();
    // explorer
    ExplorerTransactionRepository explorerTransactionsRepo();
    ExplorerAddressRepository addressExplorerRepo();
    ExplorerCoinsRepository explorerCoinsRepo();
    ExplorerSettingsRepository explorerSettingsRepo();
    // blockchain
    BlockChainAccountRepository accountRepoBlockChain();
    BlockChainCoinRepository coinRepoBlockChain();
    BlockChainTransactionRepository txRepoBlockChain();
    BlockChainBlockRepository bcBlockRepo();

    // test
    IdlingManager idlingManager();
}
