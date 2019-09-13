package network.minter.bipwallet.external.ui;

import android.app.Activity;
import android.content.Intent;

import com.airbnb.deeplinkdispatch.DeepLink;
import com.airbnb.deeplinkdispatch.DeepLinkEntry;
import com.airbnb.deeplinkdispatch.DeepLinkHandler;
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
    private final DeepLinkModuleLoader mModuleLoader;
    private final DeepLinkEntry mDeepLinkEntry;

    public DeepLinkEvent(final Intent intent) {
        mIntent = intent;
        mModuleLoader = new DeepLinkModuleLoader();
        final boolean isCorrectIntent = intent.getExtras().getBoolean(DeepLink.IS_DEEP_LINK, false);

        if (isCorrectIntent) {
            mUri = intent.getExtras().getString(DeepLink.URI);
        } else {
            mUri = intent.getExtras().getString(DeepLinkHandler.EXTRA_URI);
        }

        mDeepLinkEntry = mModuleLoader.parseUri(mUri);

        if (mDeepLinkEntry != null && !isCorrectIntent) {
            mIntent.putExtra(DeepLink.IS_DEEP_LINK, true);
            mIntent.putExtra(DeepLink.URI, mUri);

            Stream.of(firstNonNull(mDeepLinkEntry.getParameters(mUri).entrySet(), Collections.emptyMap().entrySet()))
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
