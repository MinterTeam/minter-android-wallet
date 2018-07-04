package network.minter.bipwallet.internal.helpers;

import android.support.annotation.NonNull;
import android.support.v4.util.SimpleArrayMap;

import network.minter.bipwallet.BuildConfig;
import timber.log.Timber;

/**
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class TimeProfiler {

    private static SimpleArrayMap<Integer, Long> profiles = new SimpleArrayMap<>(0);

    public static synchronized void start(@NonNull final Object tag) {
        if (!BuildConfig.DEBUG) {
            return;
        }
        if (profiles.containsKey(tag.hashCode())) {
            return;
        }

        profiles.put(tag.hashCode(), System.nanoTime());
    }

    public static synchronized void end(@NonNull final Object tag) {
        if (!BuildConfig.DEBUG) {
            return;
        }
        if (!profiles.containsKey(tag.hashCode())) {
            return;
        }

        long start = profiles.get(tag.hashCode());
        float end = (System.nanoTime() - start) / 1000000f;
        Timber.tag("PROFILE").i("%s -> Time spent %sms", tag, end);
        profiles.remove(tag.hashCode());

    }
}
