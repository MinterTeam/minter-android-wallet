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

package network.minter.bipwallet.analytics.providers;

import android.os.Bundle;

import com.annimon.stream.Stream;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;

import java.util.Map;

import network.minter.bipwallet.analytics.AnalyticsProvider;
import network.minter.bipwallet.analytics.AppEvent;
import network.minter.bipwallet.internal.helpers.data.CollectionsHelper;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */

public class FabricProvider implements AnalyticsProvider {
    private Answers mProvider;

    public FabricProvider() {
        mProvider = Answers.getInstance();
    }

    @Override
    public void send(AppEvent event) {
        mProvider.logCustom(new CustomEvent(event.getValue()));
    }

    @Override
    public void send(AppEvent event, Map<String, Object> params) {
        final CustomEvent answerEvent = new CustomEvent(event.getValue());
        Stream.of(params.entrySet())
                .forEach(entry -> {
                    answerEvent.putCustomAttribute(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
                });

        mProvider.logCustom(answerEvent);
    }

    @Override
    public void send(AppEvent event, Bundle bundle) {
        send(event, CollectionsHelper.asMap(bundle));
    }

    @Override
    public void send(AppEvent event, Integer itemId) {
        mProvider.logCustom(new CustomEvent(event.getValue()).putCustomAttribute("id", itemId));
    }
}
