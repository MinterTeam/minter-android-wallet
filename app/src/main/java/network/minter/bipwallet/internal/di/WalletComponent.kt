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
package network.minter.bipwallet.internal.di

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import com.google.gson.GsonBuilder
import dagger.Component
import network.minter.bipwallet.addressbook.db.AddressBookRepository
import network.minter.bipwallet.analytics.AnalyticsManager
import network.minter.bipwallet.apis.explorer.RepoDailyRewards
import network.minter.bipwallet.apis.explorer.RepoMonthlyRewards
import network.minter.bipwallet.apis.explorer.RepoTransactions
import network.minter.bipwallet.apis.explorer.RepoValidators
import network.minter.bipwallet.apis.gate.TxInitDataRepository
import network.minter.bipwallet.coins.CoinMapper
import network.minter.bipwallet.coins.RepoCoins
import network.minter.bipwallet.db.WalletDatabase
import network.minter.bipwallet.internal.Wallet
import network.minter.bipwallet.internal.auth.AuthSession
import network.minter.bipwallet.internal.di.annotations.DbCache
import network.minter.bipwallet.internal.exceptions.ErrorManager
import network.minter.bipwallet.internal.helpers.*
import network.minter.bipwallet.internal.settings.SettingsManager
import network.minter.bipwallet.internal.storage.AccountStorage
import network.minter.bipwallet.internal.storage.KVStorage
import network.minter.bipwallet.internal.storage.RepoAccounts
import network.minter.bipwallet.internal.storage.SecretStorage
import network.minter.bipwallet.internal.system.ForegroundDetector
import network.minter.bipwallet.services.livebalance.notification.BalanceNotificationManager
import network.minter.bipwallet.stories.repo.RepoCachedStories
import network.minter.bipwallet.stories.repo.StoriesRepository
import network.minter.core.internal.api.ApiService
import network.minter.explorer.repo.*
import javax.inject.Named

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
@Component(modules = [
    WalletModule::class,
    HelpersModule::class,
    RepoModule::class,
    DbModule::class,
    InjectorsModule::class,
    CacheModule::class,
    NotificationModule::class,
    AnalyticsModule::class
])
@WalletApp
interface WalletComponent {
    fun inject(app: Wallet)

    // app
    fun context(): Context
    fun res(): Resources
    fun apiBuilder(): ApiService.Builder
    fun session(): AuthSession
    fun storage(): KVStorage

    @DbCache
    fun storageCache(): KVStorage

    @Named("uuid")
    fun uuid(): String

    // helpers
    fun display(): DisplayHelper
    fun network(): NetworkHelper
    fun image(): ImageHelper
    fun prefs(): SharedPreferences
    fun settings(): SettingsManager
    fun gsonBuilder(): GsonBuilder
    fun analytics(): AnalyticsManager
    fun sounds(): SoundManager
    fun fingerprint(): FingerprintHelper
    fun foregroundDetector(): ForegroundDetector
    fun errorManager(): ErrorManager

    // notification
    fun balanceNotifications(): BalanceNotificationManager

    // repositories
    // local
    fun secretStorage(): SecretStorage
    fun accountStorage(): AccountStorage
    fun accountStorageCache(): RepoAccounts
    fun explorerTransactionsRepoCache(): RepoTransactions
    fun validatorsRepoCache(): RepoValidators
    fun rewardsDailyCacheRepo(): RepoDailyRewards
    fun rewardsMonthlyCacheRepo(): RepoMonthlyRewards
    fun coinsCacheRepo(): RepoCoins

    // explorer
    fun explorerTransactionsRepo(): ExplorerTransactionRepository
    fun addressExplorerRepo(): ExplorerAddressRepository
    fun explorerCoinsRepo(): ExplorerCoinsRepository
    fun validatorsRepo(): ExplorerValidatorsRepository
    fun gasRepo(): GateGasRepository
    fun txGateRepo(): GateTransactionRepository
    fun estimateRepo(): GateEstimateRepository
    fun txInitDataRepo(): TxInitDataRepository
    fun coinsGateRepo(): GateCoinRepository
    fun coinMapper(): CoinMapper

    fun storiesRepo(): StoriesRepository
    fun storiesCachedRepo(): RepoCachedStories

    // db
    fun db(): WalletDatabase
    fun addressBookRepo(): AddressBookRepository
}