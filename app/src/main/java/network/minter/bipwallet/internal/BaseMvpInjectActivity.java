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
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.widget.Toolbar;
import dagger.android.AndroidInjection;
import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasAndroidInjector;
import kotlin.jvm.functions.Function1;
import moxy.MvpAppCompatActivity;
import network.minter.bipwallet.BuildConfig;
import network.minter.bipwallet.R;
import network.minter.bipwallet.internal.dialogs.BaseBottomSheetDialogFragment;
import network.minter.bipwallet.internal.dialogs.ConfirmDialog;
import network.minter.bipwallet.internal.dialogs.WalletDialog;
import network.minter.bipwallet.internal.dialogs.WalletDialogFragment;
import network.minter.bipwallet.internal.dialogs.WalletProgressDialog;
import network.minter.bipwallet.internal.mvp.ErrorView;
import network.minter.bipwallet.internal.mvp.ErrorViewWithRetry;
import network.minter.bipwallet.internal.mvp.ProgressTextView;
import network.minter.bipwallet.internal.system.ForegroundDetector;
import network.minter.bipwallet.internal.views.SnackbarBuilder;
import timber.log.Timber;

import static network.minter.bipwallet.internal.common.Preconditions.checkNotNull;


/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@SuppressLint("Registered")
public class BaseMvpInjectActivity extends MvpAppCompatActivity implements HasAndroidInjector, ErrorView, ErrorViewWithRetry, ProgressTextView {

    @Inject DispatchingAndroidInjector<Object> androidInjector;
    private WalletProgressDialog mProgress;
    private BaseBottomSheetDialogFragment mBaseBottomSheetDialogFragment;
    protected WalletDialog walletDialog = null;
    protected WalletDialogFragment walletDialogFragment = null;

    public void startDialog(Function1<Context, WalletDialog> dialogExecutor) {
        if (walletDialogFragment != null && walletDialogFragment.isAdded()) {
            walletDialogFragment.dismiss();
            walletDialogFragment = null;
        }
        walletDialog = WalletDialog.switchDialogWithExecutor(this, walletDialog, dialogExecutor);
    }

    public void startDialogFragment(Function1<Context, WalletDialogFragment> dialogExecutor) {
        if (walletDialog != null && walletDialog.isShowing()) {
            walletDialog.dismiss();
            walletDialog = null;
        }
        walletDialogFragment = WalletDialogFragment.switchDialogWithExecutor(this, walletDialogFragment, dialogExecutor);
    }

    public void startBottomDialog(BaseBottomSheetDialogFragment dialog, String tag) {
        if (mBaseBottomSheetDialogFragment != null) {
            mBaseBottomSheetDialogFragment.dismiss();
            mBaseBottomSheetDialogFragment = null;
        }

        mBaseBottomSheetDialogFragment = dialog;
        mBaseBottomSheetDialogFragment.show(getSupportFragmentManager(), tag);
    }

    @VisibleForTesting
    public void prepareIdlingResources() {
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        getMvpDelegate().onDestroy();
        super.onBackPressed();
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

    public void setupToolbar(@NonNull final Toolbar toolbar) {
        checkNotNull(toolbar, "Toolbar can't be null!");
        setSupportActionBar(toolbar);

        assert (getSupportActionBar() != null);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onError(Throwable t) {
        Timber.e(t);
        if (BuildConfig.DEBUG && t != null) {
            new ConfirmDialog.Builder(this, "Error")
                    .setPositiveAction("Ok")
                    .setText(t)
                    .create()
                    .show();
        }
    }

    @Override
    public void onErrorWithRetry(String errorMessage, View.OnClickListener errorResolver) {
        onErrorWithRetry(errorMessage, getResources().getString(R.string.btn_retry), errorResolver);
    }

    @Override
    public void onError(String err) {
        Timber.e(err);
        runOnUiThread(() -> {
            if (err != null) {
                new ConfirmDialog.Builder(this, "Error")
                        .setPositiveAction("Ok")
                        .setText(err)
                        .create()
                        .show();
            }
        });
    }

    @Override
    public void onErrorWithRetry(String errorMessage, String actionName,
                                 View.OnClickListener errorResolver) {
        runOnUiThread(() -> {
            new SnackbarBuilder(this)
                    .setMessage(errorMessage)
                    .setAction(actionName, errorResolver)
                    .setDurationInfinite()
                    .show();
        });
    }

    @Override
    public void showProgress(CharSequence title, CharSequence message) {
        if (mProgress == null) {
            mProgress = new WalletProgressDialog.Builder(this, title == null ? "Please, wait a few seconds" : title)
                    .setText(message == null ? "working on..." : message)
                    .create();
            mProgress.setCancelable(false);
            mProgress.setCanceledOnTouchOutside(false);
        }

        if (!mProgress.isShowing()) {
            mProgress.show();
        }
    }

    @Override
    public void hideProgress() {
        if (mProgress == null) {
            return;
        }

        mProgress.dismiss();
        mProgress = null;
    }

    @Override
    public AndroidInjector<Object> androidInjector() {
        return androidInjector;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        prepareIdlingResources();
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
