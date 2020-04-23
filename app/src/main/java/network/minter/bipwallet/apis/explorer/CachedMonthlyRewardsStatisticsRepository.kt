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
import network.minter.bipwallet.internal.helpers.DateHelper.day
import network.minter.bipwallet.internal.helpers.DateHelper.days
import network.minter.bipwallet.internal.helpers.DateHelper.fmt
import network.minter.bipwallet.internal.storage.KVStorage
import network.minter.bipwallet.internal.storage.SecretStorage
import network.minter.core.internal.api.ApiService
import network.minter.explorer.models.RewardStatistics
import network.minter.explorer.repo.ExplorerAddressRepository
import org.joda.time.DateTime

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */

typealias RepoMonthlyRewards = CachedRepository<MutableList<RewardStatistics>, CachedMonthlyRewardsStatisticsRepository>

class CachedMonthlyRewardsStatisticsRepository(
        private var storage: KVStorage,
        private var secretStorage: SecretStorage,
        apiBuilder: ApiService.Builder
) : ExplorerAddressRepository(apiBuilder), CachedEntity<MutableList<RewardStatistics>> {

    companion object {
        private const val KEY_REWARDS = BuildConfig.MINTER_STORAGE_VERS + "cached_reward_stats_monthly"
    }

    override fun getData(): MutableList<RewardStatistics> {
        return storage[KEY_REWARDS, ArrayList(0)]
    }

    override fun getUpdatableData(): Observable<MutableList<RewardStatistics>> {
        val startTime = DateTime() - 30.days()
        val endTime = DateTime() + 1.day()

        return getRewardStatistics(
                secretStorage.mainWallet,
                startTime.fmt(DateHelper.DATE_FORMAT_SIMPLE),
                endTime.fmt(DateHelper.DATE_FORMAT_SIMPLE))
                .rxExp()
                .map { it.result }
                .subscribeOn(Schedulers.io())
    }

    override fun onAfterUpdate(result: MutableList<RewardStatistics>) {
        storage.put(KEY_REWARDS, result)
    }

    override fun onClear() {
        storage.delete(KEY_REWARDS)
    }

    override fun isDataReady(): Boolean {
        return storage.contains(KEY_REWARDS)
    }

    override fun getDataKey(): String {
        return javaClass.name + "_monthly"
    }
}