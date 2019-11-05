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

import java.util.List;
import java.util.Set;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoSet;
import network.minter.bipwallet.advanced.models.UserAccount;
import network.minter.bipwallet.advanced.repo.AccountStorage;
import network.minter.bipwallet.advanced.repo.SecretStorage;
import network.minter.bipwallet.apis.explorer.CacheTxRepository;
import network.minter.bipwallet.apis.explorer.CacheValidatorsRepository;
import network.minter.bipwallet.internal.auth.AuthSession;
import network.minter.bipwallet.internal.data.CacheManager;
import network.minter.bipwallet.internal.data.CachedRepository;
import network.minter.bipwallet.internal.di.annotations.Cached;
import network.minter.bipwallet.internal.di.annotations.DbCache;
import network.minter.bipwallet.internal.storage.KVStorage;
import network.minter.bipwallet.settings.repo.CacheProfileRepository;
import network.minter.explorer.MinterExplorerApi;
import network.minter.explorer.models.HistoryTransaction;
import network.minter.explorer.models.ValidatorItem;
import network.minter.profile.MinterProfileApi;
import network.minter.profile.models.User;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@Module
public abstract class CacheModule {

    @Provides
    @WalletApp
    public static CacheManager provideCacheManager(@Cached Set<CachedRepository> cachedDependencies) {
        CacheManager cache = new CacheManager();
        cache.addAll(cachedDependencies);
        return cache;
    }

    // Just providing cached repositories
    @Provides
    @WalletApp
    public static CachedRepository<UserAccount, AccountStorage> provideAccountStorage(AccountStorage accountStorage) {
        return new CachedRepository<>(accountStorage);
    }

    @Provides
    @WalletApp
    public static CachedRepository<List<HistoryTransaction>, CacheTxRepository> provideExplorerRepo(@DbCache KVStorage storage, SecretStorage secretStorage, MinterExplorerApi api) {
        return new CachedRepository<>(new CacheTxRepository(storage, secretStorage, api.getApiService()));
    }

    @Provides
    @WalletApp
    public static CachedRepository<User.Data, CacheProfileRepository> provideCachedProfileRepo(KVStorage storage, AuthSession session, MinterProfileApi api) {
        return new CachedRepository<>(new CacheProfileRepository(api.getApiService(), storage, session))
                .setTimeToLive(60);
    }

    @Provides
    @WalletApp
    public static CachedRepository<List<ValidatorItem>, CacheValidatorsRepository> provideCachedValidatorsRepo(@DbCache KVStorage storage, MinterExplorerApi api) {
        return
                new CachedRepository<>(new CacheValidatorsRepository(storage, api.getApiService()))
                        .setTimeToLive(60 * 10);
    }

    // Bindings for CacheManager
    @Binds
    @IntoSet
    @Cached
    @WalletApp
    public abstract CachedRepository provideExplorerRepoForCache(CachedRepository<List<HistoryTransaction>, CacheTxRepository> cache);

    @Binds
    @IntoSet
    @Cached
    @WalletApp
    public abstract CachedRepository provideExplorerValidatorsRepoForCache(CachedRepository<List<ValidatorItem>, CacheValidatorsRepository> cache);

    @Binds
    @IntoSet
    @Cached
    @WalletApp
    public abstract CachedRepository provideMyProfileRepoForCache(CachedRepository<User.Data, CacheProfileRepository> cache);

    @Binds
    @IntoSet
    @Cached
    @WalletApp
    public abstract CachedRepository provideAccountStorageForCache(CachedRepository<UserAccount, AccountStorage> cache);
}
