package network.minter.bipwallet.internal.system;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;
import android.support.v4.view.ViewCompat;
import android.view.View;

import com.annimon.stream.Stream;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import timber.log.Timber;

import static network.minter.bipwallet.internal.common.Preconditions.checkNotNull;

/**
 * Wallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */

public abstract class ActivityBuilder {
    private Intent mIntent;
    private Bundle mBundle;
    private WeakReference<Activity> mActivity;
    private WeakReference<Fragment> mFragment;
    private WeakReference<Service> mService;
    private WeakHashMap<String, View> mSharedViews;

    public ActivityBuilder(@NonNull Activity from) {
        mActivity = new WeakReference<>(checkNotNull(from, "Activity can't be null"));
        if (getActivityClass() != null) {
            mIntent = new Intent(from, getActivityClass());
        }
        mSharedViews = new WeakHashMap<>();
    }

    public ActivityBuilder(@NonNull Fragment from) {
        mFragment = new WeakReference<>(checkNotNull(from, "Fragment can't be null"));
        if (getActivityClass() != null) {
            mIntent = new Intent(from.getActivity(), getActivityClass());
        }
        mSharedViews = new WeakHashMap<>();
    }

    public ActivityBuilder(@NonNull Service from) {
        mService = new WeakReference<>(checkNotNull(from, "Service can't be null"));
        if (getActivityClass() != null) {
            mIntent = new Intent(from, getActivityClass());
        }
    }

    public ActivityBuilder addSharedView(View view) {
        return addSharedView(ViewCompat.getTransitionName(view), view);
    }

    public ActivityBuilder addSharedView(String name, View view) {
        if (mActivity == null && mFragment == null) {
            Timber.w("Attaching shared views make sense only from activity or fragment context");
            return this;
        }

        mSharedViews.put(name, view);
        return this;
    }

    public ActivityBuilder addSharedView(View... view) {
        if (mActivity == null && mFragment == null) {
            Timber.w("Attaching shared views make sense only from activity or fragment context");
            return this;
        }

        Stream.of(view)
                .filter(item -> item != null)
                .forEach(item -> mSharedViews.put(ViewCompat.getTransitionName(item), item));

        return this;
    }

    public ActivityBuilder addSharedViews(List<View> views) {
        if (mActivity == null || mFragment == null) {
            Timber.w("Attaching shared views make sense only from activity or fragment context");
            return this;
        }

        Stream.of(views)
                .filter(item -> item != null && ViewCompat.getTransitionName(item) != null)
                .forEach(item -> mSharedViews.put(ViewCompat.getTransitionName(item), item));
        return this;
    }

    public ActivityBuilder addSharedViews(Map<String, View> views) {
        if (mActivity == null || mFragment == null) {
            Timber.w("Attaching shared views make sense only for activity or fragment context");
            return this;
        }
        mSharedViews.putAll(views);
        return this;
    }

    public void start() {
        onBeforeStart(getIntent());
        makeSharedViews();
        if (mActivity != null) {
            if (mBundle != null) {
                mActivity.get().startActivity(getIntent(), mBundle);
            } else {
                mActivity.get().startActivity(getIntent());
            }
            mActivity.clear();
        } else if (mFragment != null) {
            if (mBundle != null) {
                mFragment.get().getActivity().startActivity(getIntent(), mBundle);
            } else {
                mFragment.get().getActivity().startActivity(getIntent());
            }
            mFragment.clear();
        } else if (mService != null) {
            if (mBundle != null) {
                mService.get().startActivity(getIntent(), mBundle);
            } else {
                mService.get().startActivity(getIntent());
            }
            mService.clear();
        }
        onAfterStart();
    }

    public Bundle getBundle() {
        return mBundle;
    }

    public ActivityBuilder setBundle(Bundle bundle) {
        mBundle = bundle;
        return this;
    }

    public void start(int requestCode) {
        onBeforeStart(getIntent());
        makeSharedViews();
        if (mActivity != null && mActivity.get() != null) {
            if (mBundle != null) {
                mActivity.get().startActivityForResult(getIntent(), requestCode, mBundle);
            } else {
                mActivity.get().startActivityForResult(getIntent(), requestCode);
            }
        } else if (mFragment != null && mFragment.get() != null) {
            if (mBundle != null) {
                mFragment.get().startActivityForResult(getIntent(), requestCode, mBundle);
            } else {
                mFragment.get().startActivityForResult(getIntent(), requestCode);
            }
        } else if (mService != null && mService.get() != null) {
            if (mBundle != null) {
                mService.get().startActivity(getIntent(), mBundle);
            } else {
                mService.get().startActivity(getIntent());
            }
        }
        onAfterStart();
    }

    public void startClearTop() {
        getIntent().setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        start();
    }

    public Intent getIntent() {
        makeIntent();
        onBeforeStart(mIntent);
        return mIntent;
    }

    protected Activity getActivity() {
        if (mActivity != null && mActivity.get() != null) {
            return mActivity.get();
        }

        return null;
    }

    protected void onBeforeStart(Intent intent) {
    }

    protected void onAfterStart() {
    }

    /**
     * Activity class to start
     *
     * @return
     */
    protected abstract Class<?> getActivityClass();

    private void makeSharedViews() {
        if (mSharedViews == null || mSharedViews.isEmpty()) {
            return;
        }

        final Activity a;
        if (mActivity != null && mActivity.get() != null) {
            a = mActivity.get();
        } else if (mFragment != null && mFragment.get() != null && mFragment.get().getActivity() != null) {
            a = mFragment.get().getActivity();
        } else {
            throw new IllegalStateException("Activity or fragment is null. Can't start activity with shared views");
        }
        //noinspection unchecked
        final Pair<View, String>[] pairs = (Pair<View, String>[]) new Pair[mSharedViews.size()];
        Stream.of(mSharedViews.entrySet())
                .forEachIndexed((idx, item) -> pairs[idx] = Pair.create(item.getValue(), item.getKey()));

        assert a != null;
        final Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(a, pairs).toBundle();

        if (mBundle != null) {
            mBundle.putAll(bundle);
        } else {
            mBundle = bundle;
        }
    }

    private void makeIntent() {
        if (mIntent != null) return;

        if (mActivity != null && mActivity.get() != null) {
            mIntent = new Intent(mActivity.get(), getActivityClass());
        } else if (mFragment != null && mFragment.get() != null) {
            mIntent = new Intent(mFragment.get().getActivity(), getActivityClass());
        } else if (mService != null && mService.get() != null) {
            mIntent = new Intent(mService.get(), getActivityClass());
        }
    }
}
