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

package network.minter.bipwallet.internal;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.LifecycleOwner;
import moxy.MvpAppCompatActivity;
import network.minter.bipwallet.R;
import network.minter.bipwallet.internal.mvp.ErrorView;
import network.minter.bipwallet.internal.mvp.ErrorViewWithRetry;
import network.minter.bipwallet.internal.system.ForegroundDetector;
import timber.log.Timber;

import static network.minter.bipwallet.internal.common.Preconditions.checkNotNull;


/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@SuppressLint("Registered")
public class BaseMvpActivity extends MvpAppCompatActivity implements LifecycleOwner, ErrorView, ErrorViewWithRetry {

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void setupToolbar(@NonNull final Toolbar toolbar, @NonNull final DrawerLayout drawerLayout) {
        checkNotNull(toolbar, "Toolbar can't be null!");
        setSupportActionBar(toolbar);

        assert (getSupportActionBar() != null);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(Gravity.START, true));
    }

    public void setupToolbar(@NonNull final Toolbar toolbar) {
        checkNotNull(toolbar, "Toolbar can't be null!");
        setSupportActionBar(toolbar);

        assert (getSupportActionBar() != null);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public int getStatusBarHeight() {
        // status bar height
        int statusBarHeight = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = getResources().getDimensionPixelSize(resourceId);
        }

        return statusBarHeight;
    }

    public void setActionBarVisible(boolean isVisible) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            if (isVisible) {
                actionBar.show();
            } else {
                actionBar.hide();
            }
        }
    }

    public void startActivityClearTop(Activity from, Intent intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_CLEAR_TASK |
                Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);

        from.startActivity(intent);
        from.finish();
    }

    public void startActivityClearTop(Activity from, Class<?> toClass) {
        Intent intent = new Intent(from, toClass);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_CLEAR_TASK |
                Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);

        from.startActivity(intent);
        from.finish();
    }

    public void startActivityClearTop(Activity from, Class<?> toClass, Bundle options) {
        Intent intent = new Intent(from, toClass);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_CLEAR_TASK |
                Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);

        from.startActivity(intent, options);
        from.finish();
    }

    @Override
    public void onError(Throwable t) {
        Timber.e(t);
    }

    @Override
    public void onError(String err) {
        Timber.e(err);
        runOnUiThread(() -> {
            if (err != null) {
//                if (mStatusView != null) {
//                    mStatusView
//                            .withText(err)
//                            .withoutRetryButton()
//                            .showStatus();
//                } else {
//                    new SnackbarBuilder(this)
//                            .setMessage(err)
//                            .setDurationLong()
//                            .show();
//                }
            }
        });
    }

    @Override
    public void onErrorWithRetry(String errorMessage, View.OnClickListener errorResolver) {
        onErrorWithRetry(errorMessage, getResources().getString(R.string.btn_retry), errorResolver);
    }

    @Override
    public void onErrorWithRetry(String errorMessage, String actionName,
                                 View.OnClickListener errorResolver) {
        runOnUiThread(() -> {
//            if (mStatusView != null) {
//                mStatusView
//                        .withText(errorMessage)
//                        .withRetryButton(actionName, errorResolver)
//                        .showStatus();
//            } else {
//                new SnackbarBuilder(this)
//                        .setMessage(errorMessage)
//                        .setAction(actionName, errorResolver)
//                        .setDurationIndefinite()
//                        .show();
//            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Wallet.app().foregroundDetector().setListener(new ForegroundDetector.ForegroundDelegate() {
            @Override
            public void onAppBackgrounded() {
                Timber.d("Destroy application");
                try {
                    finishAffinity();
                } catch (Throwable ignore) {
                }
            }

            @Override
            public void onAppForegrounded() {

            }
        });
    }
}
