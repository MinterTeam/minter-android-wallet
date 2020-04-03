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

package network.minter.bipwallet.apis.explorer;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.math.BigDecimal;
import java.util.Date;

import javax.annotation.Nonnull;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import network.minter.bipwallet.advanced.repo.SecretStorage;
import network.minter.bipwallet.internal.data.CachedEntity;
import network.minter.bipwallet.internal.helpers.DateHelper;
import network.minter.bipwallet.internal.storage.KVStorage;
import network.minter.core.internal.api.ApiService;
import network.minter.explorer.models.RewardStatistics;
import network.minter.explorer.repo.ExplorerAddressRepository;

import static network.minter.bipwallet.apis.reactive.ReactiveExplorer.rxExp;
import static network.minter.bipwallet.apis.reactive.ReactiveExplorer.toExpError;

public class CachedRewardStatisticsRepository extends ExplorerAddressRepository implements CachedEntity<RewardStatistics> {
    private final static String KEY_REWARDS = "cached_reward_stats_today";
    private KVStorage mStorage;
    private SecretStorage mSecretStorage;

    public CachedRewardStatisticsRepository(KVStorage storage, SecretStorage secretStorage, @Nonnull ApiService.Builder apiBuilder) {
        super(apiBuilder);
        mStorage = storage;
        mSecretStorage = secretStorage;
    }

    @Override
    public RewardStatistics initialData() {
        RewardStatistics empty = new RewardStatistics();
        empty.amount = BigDecimal.ZERO;
        empty.time = new Date();
        return mStorage.get(KEY_REWARDS, empty);
    }

    @Override
    public Observable<RewardStatistics> getUpdatableData() {
        DateTime startTime = new DateTime();

        DateTime endTime = new DateTime().plus(Days.days(1));

        final DateTimeFormatter fmt = DateTimeFormat.forPattern(DateHelper.DATE_FORMAT_SIMPLE).withZone(DateTimeZone.forID("UTC"));

        return
                rxExp(getRewardStatistics(
                        mSecretStorage.getMainWallet(),
                        startTime.toString(fmt),
                        endTime.toString(fmt)
                ))
                        .onErrorResumeNext(toExpError())
                        .map(res -> {
                            if (!res.isOk() || res.result.isEmpty()) {
                                RewardStatistics empty = new RewardStatistics();
                                empty.amount = BigDecimal.ZERO;
                                empty.time = new Date();
                                return empty;
                            }

                            return res.result.get(0);
                        })
                        .subscribeOn(Schedulers.io());
    }

    @Override
    public void onAfterUpdate(RewardStatistics result) {
        mStorage.put(KEY_REWARDS, result);
    }

    @Override
    public void onClear() {
        mStorage.delete(KEY_REWARDS);
    }
}
