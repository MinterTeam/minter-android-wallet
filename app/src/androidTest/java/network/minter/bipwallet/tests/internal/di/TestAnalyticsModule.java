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

package network.minter.bipwallet.tests.internal.di;

import java.util.Collections;
import java.util.Set;

import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoSet;
import network.minter.bipwallet.BuildConfig;
import network.minter.bipwallet.analytics.AnalyticsManager;
import network.minter.bipwallet.analytics.AnalyticsProvider;
import network.minter.bipwallet.analytics.providers.DummyProvider;
import network.minter.bipwallet.internal.di.WalletApp;
import network.minter.bipwallet.internal.di.annotations.AnalyticsProviders;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
@Module
public class TestAnalyticsModule {
    @Provides
    @IntoSet
    @AnalyticsProviders
    @WalletApp
    public AnalyticsProvider provideFabricAnalytics() {
        return new DummyProvider();
    }

    @Provides
    @IntoSet
    @AnalyticsProviders
    @WalletApp
    public AnalyticsProvider provideAppMetricaAnalytics() {
        return new DummyProvider();
    }

    @SuppressWarnings("ConstantConditions")
    @Provides
    @WalletApp
    public AnalyticsManager provideAnalyticsManager(@AnalyticsProviders Set<AnalyticsProvider> providerSet) {
        if (BuildConfig.FLAVOR.equalsIgnoreCase("netTest") || BuildConfig.FLAVOR.equalsIgnoreCase("netMain")) {
            return new AnalyticsManager(providerSet);
        }

        return new AnalyticsManager(Collections.emptySet());
    }
}
