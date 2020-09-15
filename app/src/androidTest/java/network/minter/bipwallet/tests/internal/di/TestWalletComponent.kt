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
package network.minter.bipwallet.tests.internal.di


import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import com.google.gson.GsonBuilder
import dagger.Component
import network.minter.bipwallet.addressbook.db.AddressBookRepository
import network.minter.bipwallet.apis.explorer.RepoDailyRewards
import network.minter.bipwallet.apis.explorer.RepoMonthlyRewards
import network.minter.bipwallet.apis.explorer.RepoTransactions
import network.minter.bipwallet.apis.explorer.RepoValidators
import network.minter.bipwallet.apis.gate.TxInitDataRepository
import network.minter.bipwallet.coins.CoinMapper
import network.minter.bipwallet.coins.RepoCoins
import network.minter.bipwallet.db.WalletDatabase
import network.minter.bipwallet.internal.auth.AuthSession
import network.minter.bipwallet.internal.di.*
import network.minter.bipwallet.internal.di.annotations.DbCache
import network.minter.bipwallet.internal.helpers.*
import network.minter.bipwallet.internal.settings.SettingsManager
import network.minter.bipwallet.internal.storage.AccountStorage
import network.minter.bipwallet.internal.storage.KVStorage
import network.minter.bipwallet.internal.storage.RepoAccounts
import network.minter.bipwallet.internal.storage.SecretStorage
import network.minter.bipwallet.services.livebalance.notification.BalanceNotificationManager
import network.minter.bipwallet.tests.internal.TestWallet
import network.minter.core.internal.api.ApiService
import network.minter.explorer.repo.*
import javax.inject.Named

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock></edward.vstock>@gmail.com>
 */
@Component(modules = [
    TestWalletModule::class,
    HelpersModule::class,
    RepoModule::class,
    DbModule::class,
    InjectorsModule::class,
    CacheModule::class,
    NotificationModule::class,
    TestAnalyticsModule::class
])
@WalletApp
interface TestWalletComponent : WalletComponent {
    fun inject(app: TestWallet)

    // app
    override fun context(): Context
    override fun res(): Resources
    override fun apiBuilder(): ApiService.Builder
    override fun session(): AuthSession
    override fun storage(): KVStorage

    @DbCache
    override fun storageCache(): KVStorage

    @Named("uuid")
    override fun uuid(): String

    // helpers
    override fun display(): DisplayHelper
    override fun network(): NetworkHelper
    override fun image(): ImageHelper
    override fun prefs(): SharedPreferences
    override fun settings(): SettingsManager
    override fun gsonBuilder(): GsonBuilder

    override fun sounds(): SoundManager
    override fun fingerprint(): FingerprintHelper
//    override fun foregroundDetector(): ForegroundDetector

    // notification
    override fun balanceNotifications(): BalanceNotificationManager

    // repositories
    // local
    override fun secretStorage(): SecretStorage
    override fun accountStorage(): AccountStorage
    override fun accountStorageCache(): RepoAccounts
    override fun explorerTransactionsRepoCache(): RepoTransactions
    override fun validatorsRepoCache(): RepoValidators
    override fun rewardsDailyCacheRepo(): RepoDailyRewards
    override fun rewardsMonthlyCacheRepo(): RepoMonthlyRewards
    override fun coinsCacheRepo(): RepoCoins

    // explorer
    override fun explorerTransactionsRepo(): ExplorerTransactionRepository
    override fun addressExplorerRepo(): ExplorerAddressRepository
    override fun explorerCoinsRepo(): ExplorerCoinsRepository
    override fun validatorsRepo(): ExplorerValidatorsRepository
    override fun gasRepo(): GateGasRepository
    override fun txGateRepo(): GateTransactionRepository
    override fun estimateRepo(): GateEstimateRepository
    override fun txInitDataRepo(): TxInitDataRepository
    override fun coinMapper(): CoinMapper

    // db
    override fun db(): WalletDatabase
    override fun addressBookRepo(): AddressBookRepository
}