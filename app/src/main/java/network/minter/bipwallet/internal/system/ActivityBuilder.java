/*
 * Copyright (C) by MinterTeam. 2022
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

package network.minter.bipwallet.internal.system;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;

import java.lang.ref.WeakReference;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import kotlin.Deprecated;

import static network.minter.bipwallet.internal.common.Preconditions.checkNotNull;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public abstract class ActivityBuilder {
    private Intent mIntent;
    private Bundle mBundle;
    private WeakReference<Activity> mActivity;
    private WeakReference<Fragment> mFragment;
    private WeakReference<Service> mService;

    public ActivityBuilder(@NonNull Activity from) {
        mActivity = new WeakReference<>(checkNotNull(from, "Activity can't be null"));
        if (getActivityClass() != null) {
            mIntent = new Intent(from, getActivityClass());
        }
    }

    public ActivityBuilder(@NonNull Fragment from) {
        mFragment = new WeakReference<>(checkNotNull(from, "Fragment can't be null"));
        if (getActivityClass() != null) {
            mIntent = new Intent(from.getActivity(), getActivityClass());
        }
    }

    public ActivityBuilder(@NonNull Service from) {
        mService = new WeakReference<>(checkNotNull(from, "Service can't be null"));
        if (getActivityClass() != null) {
            mIntent = new Intent(from, getActivityClass());
        }
    }

    public void start() {
        onBeforeStart(getIntent());
        if (mActivity != null) {
            if (mBundle != null) {
                mActivity.get().startActivity(getIntent(), mBundle);
            } else {
                mActivity.get().startActivity(getIntent());
            }
            mActivity.clear();
        } else if (mFragment != null) {
            if (mBundle != null) {
                mFragment.get().startActivity(getIntent(), mBundle);
            } else {
                mFragment.get().startActivity(getIntent());
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

    @Deprecated(message = "Use new ActivityResult API instead")
    public void start(int requestCode) {
        onBeforeStart(getIntent());
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

    protected void onBeforeStart(@NonNull Intent intent) {
    }

    protected void onAfterStart() {
    }

    /**
     * Activity class to start
     * @return
     */
    protected abstract Class<?> getActivityClass();

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
