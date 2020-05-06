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
package network.minter.bipwallet.apis.explorer

import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import network.minter.bipwallet.BuildConfig
import network.minter.bipwallet.apis.reactive.rxExp
import network.minter.bipwallet.internal.data.CachedEntity
import network.minter.bipwallet.internal.data.CachedRepository
import network.minter.bipwallet.internal.helpers.DateHelper
import network.minter.bipwallet.internal.storage.KVStorage
import network.minter.bipwallet.internal.storage.SecretStorage
import network.minter.core.internal.api.ApiService
import network.minter.explorer.models.ExpResult
import network.minter.explorer.models.RewardStatistics
import network.minter.explorer.repo.ExplorerAddressRepository
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.Days
import org.joda.time.format.DateTimeFormat
import timber.log.Timber
import java.math.BigDecimal
import java.util.*

typealias RepoDailyRewards = CachedRepository<RewardStatistics, CachedDailyRewardStatisticsRepository>

open class CachedDailyRewardStatisticsRepository(
        protected val mStorage: KVStorage,
        protected val mSecretStorage: SecretStorage,
        apiBuilder: ApiService.Builder
) : ExplorerAddressRepository(apiBuilder), CachedEntity<RewardStatistics> {

    override fun getData(): RewardStatistics {
        val empty = RewardStatistics()
        empty.amount = BigDecimal.ZERO
        empty.time = Date()
        Timber.d("DAILY_REWARDS: Get data")
        return mStorage[KEY_REWARDS, empty]
    }

    override fun getUpdatableData(): Observable<RewardStatistics> {
        val startTime = DateTime()
        val endTime = DateTime().plus(Days.days(1))
        val fmt = DateTimeFormat.forPattern(DateHelper.DATE_FORMAT_SIMPLE).withZone(DateTimeZone.forID("UTC"))
        return getRewardStatistics(
                mSecretStorage.mainWallet,
                startTime.toString(fmt),
                endTime.toString(fmt)
        )
                .rxExp()
                .map { res: ExpResult<List<RewardStatistics>> ->
                    Timber.d("DAILY_REWARDS Load remote data")
                    if (!res.isOk || res.result?.isEmpty() == true) {
                        val empty = RewardStatistics()
                        empty.amount = BigDecimal.ZERO
                        empty.time = Date()
                        return@map empty
                    }
                    res.result[0]
                }
                .subscribeOn(Schedulers.io())
    }

    override fun onAfterUpdate(result: RewardStatistics) {
        Timber.d("DAILY_REWARDS: Put data")
        mStorage.putAsync(KEY_REWARDS, result)
    }

    override fun onClear() {
        Timber.d("DAILY_REWARDS: Clear data")
        mStorage.deleteAsync(KEY_REWARDS)
    }

    companion object {
        private const val KEY_REWARDS = BuildConfig.MINTER_STORAGE_VERS + "cached_reward_stats_today"
    }

    override fun isDataReady(): Boolean {
        Timber.d("DAILY_REWARDS: Check data exists: ${mStorage.contains(KEY_REWARDS)}")
        return mStorage.contains(KEY_REWARDS)
    }

    override fun getDataKey(): String {
        return javaClass.name + "_daily"
    }

}