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
import java.math.BigDecimal
import java.util.*

typealias RepoDailyRewards = CachedRepository<RewardStatistics, CachedDailyRewardStatisticsRepository>

open class CachedDailyRewardStatisticsRepository(
        protected val storage: KVStorage,
        protected val secretStorage: SecretStorage,
        apiBuilder: ApiService.Builder
) : ExplorerAddressRepository(apiBuilder), CachedEntity<RewardStatistics> {

    companion object {
        private const val KEY_REWARDS = BuildConfig.MINTER_CACHE_VERS + "cached_reward_stats_today_"
    }

    private val cacheKey: String
        get() {
            return KEY_REWARDS + "${secretStorage.mainWallet}"
        }

    override fun getData(): RewardStatistics {
        val empty = RewardStatistics()
        empty.amount = BigDecimal.ZERO
        empty.time = Date()
        return storage[cacheKey, empty]
    }

    override fun getUpdatableData(): Observable<RewardStatistics> {
        val startTime = DateTime()
        val endTime = DateTime().plus(Days.days(1))
        val fmt = DateTimeFormat.forPattern(DateHelper.DATE_FORMAT_SIMPLE).withZone(DateTimeZone.forID("UTC"))
        return getRewardStatistics(
                secretStorage.mainWallet,
                startTime.toString(fmt),
                endTime.toString(fmt)
        )
                .map { res: ExpResult<MutableList<RewardStatistics>?> ->
                    if (res.result == null || !res.isOk || res.result?.isEmpty() == true) {
                        val empty = RewardStatistics()
                        empty.amount = BigDecimal.ZERO
                        empty.time = Date()
                        return@map empty
                    }
                    res.result!![0]
                }
                .subscribeOn(Schedulers.io())
    }

    override fun onAfterUpdate(result: RewardStatistics) {
        storage.putAsync(cacheKey, result)
    }

    override fun onClear() {
        storage.deleteAsync(cacheKey)
    }

    override fun isDataReady(): Boolean {
        return storage.contains(cacheKey)
    }

    override fun getDataKey(): String {
        return javaClass.name + "_daily"
    }

}