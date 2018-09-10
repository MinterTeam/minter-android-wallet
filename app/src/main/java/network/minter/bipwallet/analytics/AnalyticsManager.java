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

package network.minter.bipwallet.analytics;

import android.os.Bundle;

import java.util.Map;
import java.util.Set;

import timber.log.Timber;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class AnalyticsManager implements AnalyticsProvider {

    private final Set<AnalyticsProvider> mProviders;

    public AnalyticsManager(Set<AnalyticsProvider> providers) {
        mProviders = providers;

        if (mProviders.size() == 0) {
            Timber.i("No one analytics provider attached");
        }
    }

    @Override
    public void send(AppEvent event) {
        for (AnalyticsProvider provider : mProviders) {
            if (provider == null) continue;
            try {
                provider.send(event);
            } catch (Throwable t) {
                Timber.w(t, "Unable to send event: %s", event.getValue());
            }
        }
    }

    @Override
    public void send(AppEvent event, Map<String, Object> params) {
        for (AnalyticsProvider provider : mProviders) {
            if (provider == null) continue;
            try {
                provider.send(event, params);
            } catch (Throwable t) {
                Timber.w("Unable to send event: %s", event.getValue());
            }
        }
    }

    @Override
    public void send(AppEvent event, Bundle bundle) {
        for (AnalyticsProvider provider : mProviders) {
            if (provider == null) continue;
            try {
                provider.send(event, bundle);
            } catch (Throwable t) {
                Timber.w("Unable to send event: %s", event.getValue());
            }
        }
    }

    @Override
    public void send(final AppEvent event, final Integer itemId) {
        for (AnalyticsProvider provider : mProviders) {
            if (provider == null) continue;
            try {
                provider.send(event, itemId);
            } catch (Throwable t) {
                Timber.w("Unable to send event: %s", event.getValue());
            }
        }
    }
}
