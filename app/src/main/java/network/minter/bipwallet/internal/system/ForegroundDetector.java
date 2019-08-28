package network.minter.bipwallet.internal.system;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentCallbacks2;
import android.content.res.Configuration;
import android.os.Bundle;

import network.minter.bipwallet.security.PauseTimer;

/**
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public final class ForegroundDetector implements Application.ActivityLifecycleCallbacks, ComponentCallbacks2 {
    private ForegroundDelegate mDelegate;
    private boolean mForeground = false;

    public void setListener(ForegroundDelegate lifecycleDelegate) {
        mDelegate = lifecycleDelegate;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {
        if (!mForeground && mDelegate != null) {
            mForeground = true;
            PauseTimer.onResume();
            mDelegate.onAppForegrounded();
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {

    }

    @Override
    public void onActivityStopped(Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }

    @Override
    public void onTrimMemory(int level) {
        if (level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            // lifecycleDelegate instance was passed in on the constructor
            mForeground = false;

            if (mDelegate != null) {
                PauseTimer.onPause(() -> mDelegate.onAppBackgrounded());
            }

        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {

    }

    @Override
    public void onLowMemory() {

    }

    public interface ForegroundDelegate {
        void onAppBackgrounded();
        void onAppForegrounded();
    }
}
