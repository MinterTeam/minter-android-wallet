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
package network.minter.bipwallet.internal.di

import dagger.Module
import dagger.Provides
import network.minter.bipwallet.apis.explorer.*
import network.minter.bipwallet.coins.CachedCoinsRepository
import network.minter.bipwallet.coins.CoinMapper
import network.minter.bipwallet.coins.RepoCoins
import network.minter.bipwallet.db.WalletDatabase
import network.minter.bipwallet.internal.data.CachedRepository
import network.minter.bipwallet.internal.di.annotations.DbCache
import network.minter.bipwallet.internal.exceptions.ErrorManager
import network.minter.bipwallet.internal.storage.AccountStorage
import network.minter.bipwallet.internal.storage.KVStorage
import network.minter.bipwallet.internal.storage.RepoAccounts
import network.minter.bipwallet.internal.storage.SecretStorage
import network.minter.bipwallet.stories.repo.RepoCachedStories
import network.minter.bipwallet.stories.repo.StoriesRepository
import network.minter.explorer.MinterExplorerSDK
import network.minter.explorer.repo.GateCoinRepository
import java.util.concurrent.TimeUnit

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
@Module
object CacheModule {

    @JvmStatic
    @Provides
    @WalletApp
    fun provideAccountStorage(@DbCache storage: KVStorage, accountStorage: AccountStorage, em: ErrorManager): RepoAccounts {
        return RepoAccounts(storage, accountStorage).retryWhen(em.retryWhenHandler)
    }

    @JvmStatic
    @Provides
    @WalletApp
    fun provideExplorerRepo(@DbCache storage: KVStorage, secretStorage: SecretStorage, api: MinterExplorerSDK, em: ErrorManager): RepoTransactions {
        return CachedRepository(storage, CacheTxRepository(storage, secretStorage, api.apiService)).retryWhen(em.retryWhenHandler)
    }

    @JvmStatic
    @Provides
    @WalletApp
    fun provideCachedValidatorsRepo(@DbCache storage: KVStorage, api: MinterExplorerSDK, em: ErrorManager): RepoValidators {
        return RepoValidators(storage, CacheValidatorsRepository(storage, api.apiService))
                .setTimeToLive(60 * 20)
                .retryWhen(em.retryWhenHandler)
    }

    @JvmStatic
    @Provides
    @WalletApp
    fun provideCachedCoinsRepo(@DbCache storage: KVStorage, db: WalletDatabase, api: MinterExplorerSDK, em: ErrorManager): RepoCoins {
        return RepoCoins(storage, CachedCoinsRepository(storage, db, api.apiService))
                .setTimeToLive(/*3  minutes */ 60 * 3)
                .retryWhen(em.retryWhenHandler)
    }

    @JvmStatic
    @Provides
    @WalletApp
    fun provideCoinsMapper(repo: RepoCoins, gateRepo: GateCoinRepository): CoinMapper {
        return CoinMapper(repo, gateRepo)
    }

    @JvmStatic
    @Provides
    @WalletApp
    fun provideCachedDailyRewardStatsRepo(@DbCache storage: KVStorage, secretStorage: SecretStorage, api: MinterExplorerSDK): RepoDailyRewards {
        return RepoDailyRewards(storage, CachedDailyRewardStatisticsRepository(storage, secretStorage, api.apiService))
                .setTimeToLive(60 * 10)
    }

    @JvmStatic
    @Provides
    @WalletApp
    fun provideCachedMonthlyRewardStatsRepo(@DbCache storage: KVStorage, secretStorage: SecretStorage, api: MinterExplorerSDK): RepoMonthlyRewards {
        return RepoMonthlyRewards(storage, CachedMonthlyRewardsStatisticsRepository(storage, secretStorage, api.apiService))
                .setTimeToLive(60 * 30)
    }

    @JvmStatic
    @Provides
    @WalletApp
    fun provideCachedStoriesRepo(@DbCache storage: KVStorage, repo: StoriesRepository, em: ErrorManager): RepoCachedStories {
        return RepoCachedStories(storage, repo)
                .setTimeToLive(10, TimeUnit.MINUTES)
                .retryWhen(em.retryWhenHandler)
    }

}