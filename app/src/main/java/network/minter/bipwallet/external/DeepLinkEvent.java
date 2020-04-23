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

package network.minter.bipwallet.external;

import android.app.Activity;
import android.content.Intent;

import com.airbnb.deeplinkdispatch.DeepLink;
import com.airbnb.deeplinkdispatch.DeepLinkEntry;
import com.airbnb.deeplinkdispatch.DeepLinkHandler;
import com.airbnb.deeplinkdispatch.DeepLinkUri;
import com.annimon.stream.Stream;

import java.util.Collections;

import androidx.annotation.Nullable;

import static network.minter.bipwallet.internal.common.Preconditions.firstNonNull;

/**
 * Minter. 2019
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */

public class DeepLinkEvent {
    private final Intent mIntent;
    private final String mUri;
    private final DeepLinkModuleRegistry mModuleLoader;
    private final DeepLinkEntry mDeepLinkEntry;
    private final DeepLinkModuleRegistryHelper<DeepLinkModuleRegistry> mModuleHelper;

    public DeepLinkEvent(final Intent intent) {
        mIntent = intent;
        mModuleLoader = new DeepLinkModuleRegistry();
        mModuleHelper = new DeepLinkModuleRegistryHelper<>(mModuleLoader);
        final boolean isCorrectIntent = intent.getExtras().getBoolean(DeepLink.IS_DEEP_LINK, false);

        if (isCorrectIntent) {
            mUri = intent.getExtras().getString(DeepLink.URI);
        } else {
            mUri = intent.getExtras().getString(DeepLinkHandler.EXTRA_URI);
        }

        mDeepLinkEntry = mModuleHelper.parseUri(mUri);

        if (mDeepLinkEntry != null && !isCorrectIntent) {
            mIntent.putExtra(DeepLink.IS_DEEP_LINK, true);
            mIntent.putExtra(DeepLink.URI, mUri);

            Stream.of(firstNonNull(mDeepLinkEntry.getParameters(DeepLinkUri.parse(mUri)).entrySet(), Collections.emptyMap().entrySet()))
                    .forEach(entry -> mIntent.putExtra(
                            String.valueOf(entry.getKey()),
                            String.valueOf(entry.getValue())
                    ));
        }
    }

    public String getUri() {
        return mUri;
    }

    @Nullable
    public Class<?> getSupportedActivity() {
        return mDeepLinkEntry == null ? null : mDeepLinkEntry.getActivityClass();
    }

    public boolean isSupportedActivity(Activity handler) {
        return isSupportedActivity(handler.getClass());
    }

    public boolean isSupportedActivity(Class<?> handlerClass) {
        if (mDeepLinkEntry == null) return false;

        return mDeepLinkEntry.getActivityClass().equals(handlerClass);
    }

    public DeepLinkEntry getEntry() {
        return mDeepLinkEntry;
    }

    public Intent getIntent() {
        return mIntent;
    }
}
