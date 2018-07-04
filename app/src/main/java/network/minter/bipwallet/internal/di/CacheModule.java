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

import java.util.List;
import java.util.Map;

import dagger.Binds;
import dagger.MapKey;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import network.minter.bipwallet.advanced.models.UserAccount;
import network.minter.bipwallet.advanced.repo.AccountStorage;
import network.minter.bipwallet.advanced.repo.SecretStorage;
import network.minter.bipwallet.apis.explorer.CachedExplorerTransactionRepository;
import network.minter.bipwallet.internal.data.CacheManager;
import network.minter.bipwallet.internal.data.CachedRepository;
import network.minter.bipwallet.internal.storage.KVStorage;
import network.minter.explorerapi.MinterExplorerApi;
import network.minter.explorerapi.models.HistoryTransaction;

/**
 * MinterWallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@Module
public abstract class CacheModule {

    @SuppressWarnings("unchecked")
    @Provides
    @WalletApp
    public static CacheManager provideCacheManager(@CachedMapKey Map<String, CachedRepository> cachedDependencies) {
        CacheManager cache = new CacheManager();
        if (cachedDependencies == null) return cache;

        for (CachedRepository item : cachedDependencies.values()) {
            cache.add(item);
        }

        return cache;
    }

    @Provides
    @WalletApp
    public static CachedRepository<UserAccount, AccountStorage> provideAccountStorage(AccountStorage accountStorage) {
        return new CachedRepository<>(accountStorage);
    }

    @Provides
    @WalletApp
    public static CachedRepository<List<HistoryTransaction>, CachedExplorerTransactionRepository> provideExplorerRepo(KVStorage storage, SecretStorage secretStorage, MinterExplorerApi api) {
        return new CachedRepository<>(new CachedExplorerTransactionRepository(storage, secretStorage, api.getApiService()));
    }

    @Binds
    @IntoMap
    @CachedMapKey
    @WalletApp
    public abstract CachedRepository provideCachedAccountStorage(CachedRepository<UserAccount, AccountStorage> cache);

    @MapKey
    @interface CachedMapKey {
        String value() default "cached";
    }
}
